/*
 * Copyright (c) 2012, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.nac.packethandler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFSwitch;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionNetworkLayerDestination;
import org.openflow.protocol.action.OFActionNetworkLayerSource;
import org.openflow.protocol.action.OFActionOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tallac.nac.module.ConfigMgr;
import com.tallac.nac.module.FlowMgr;

public class HttpPacketHandler {

    private static final HttpPacketHandler INSTANCE = new HttpPacketHandler();
    private static final Logger            LOG = LoggerFactory.getLogger(HttpPacketHandler.class);

    public static class HttpRequestDestData {

        private int   mDestIpAddr;
        private short mInputPort;

        public HttpRequestDestData(OFMatch ofMatch) {
            mDestIpAddr = ofMatch.getNetworkDestination();
            mInputPort  = ofMatch.getInputPort();
        }

        public int getIp() { return mDestIpAddr; }

        public short getInputPort() { return mInputPort; }

        @Override
        public String toString() {
            return "HttpRequestDestData [mDestIpAddr=" + mDestIpAddr + ", mInputPort=" + mInputPort + "]";
        }

    }

    public static int TALLAC_WEB_IP = 0;

    Cache<OFMatch, HttpRequestDestData> mHeaderList;

    //---- HttpPacketHandler: private constructor to prevent external instantiation
    private HttpPacketHandler() {
        mHeaderList = CacheBuilder.newBuilder().maximumSize(1000).expireAfterWrite( 1, TimeUnit.MINUTES ).build();  // Initialize header list
    }

    //---- init:  get address and URL for redirection web page
    public void init() {
    	
        LOG.debug("Initialize NAC HTTP packet manager.");
        
        try {
            InetAddress ipAddr;

            // Get IPv4 address of WEB server using to redirect requests
            ipAddr = InetAddress.getByName( ConfigMgr.getInstance().getString("nac.httpRedirectionWebServer"));
            TALLAC_WEB_IP = ByteBuffer.wrap(ipAddr.getAddress()).getInt();
            
        } catch (UnknownHostException e) {
            LOG.error("Unable to configure HTTP packet handler {}", e);
        }
    }

    public static HttpPacketHandler getInstance() { return INSTANCE; }

    //---- handleHttpRequest:  process incoming HTTP requests and change destination to our registration server.
    public void handleHttpRequest( final IOFSwitch         ofSwitch,
                                   final FloodlightContext context, 
                                   final OFPacketIn        packetIn,
                                   final OFMatch           ofMatch ) {
        
        if (! isHttpForwardingReady()) // HTTP forwarding is not ready. Drop the packet.
        {
            FlowMgr.getInstance().dropPacket(ofSwitch, context, packetIn);
            return;
        }
        
        // Construct key for the header list
        OFMatch keyMatch = makeKey( ofMatch );

        // Search for header with the same key in the header list
        HttpRequestDestData reqDest = mHeaderList.getIfPresent(keyMatch);
        if (reqDest == null) 
        {
            reqDest = new HttpRequestDestData(ofMatch);
            mHeaderList.put( keyMatch, reqDest );           // Add header to the table
        }

        // Create list of actions
        final List<OFAction> actions = new ArrayList<OFAction>();
        
        actions.add( new OFActionNetworkLayerDestination( TALLAC_WEB_IP ) );
        actions.add( new OFActionOutput( OFPort.OFPP_NORMAL.getValue() ) );

        // Forward packet to the Tallac WEB Server
        FlowMgr.getInstance().sendPacketOut( ofSwitch, context, packetIn, actions );

        return;
    }

    //---- handleHttpResponse:  process HTTP response packets, by changing the IP source address to the original value
    public void handleHttpResponse( final IOFSwitch         ofSwitch,
                                    final FloodlightContext context, 
                                    final OFPacketIn        packetIn,
                                    final OFMatch           ofMatch) {

        // Check if NAC module knows IPv4 addresses of Tallac WEB server and authentication server
        if( !isHttpForwardingReady() )
        {
            // HTTP forwarding is not ready. Drop the packet.
            FlowMgr.getInstance().dropPacket( ofSwitch, context, packetIn );
            return;
        }
        
        // Reverse header, recreate original record key
        OFMatch origKeyMatch = makeKey( reverseMatch( ofMatch ) );

        // Search for the recreated key in the header list
        HttpRequestDestData reqDest = mHeaderList.getIfPresent( origKeyMatch );
        if (reqDest == null) 
        {
            // Record with original header is not found in the list.  HTTP forwarding is not ready. Drop the packet.
            FlowMgr.getInstance().dropPacket( ofSwitch, context, packetIn );
            return;
        }

        // Create list of the action, send packet back to the client
        final List<OFAction> actions = new ArrayList<OFAction>();
        
        // Key thing here is to spoof the IP address and port to appear like the original request
        actions.add( new OFActionNetworkLayerSource( reqDest.getIp() ) );   
        actions.add( new OFActionOutput(             reqDest.getInputPort() ) );
        
        FlowMgr.getInstance().sendPacketOut( ofSwitch, context, packetIn, actions );
    }
    
    //---- isHttpForwardingRead:  make sure we've initialized our redirection server value
    private boolean isHttpForwardingReady()
    {
        if( TALLAC_WEB_IP == 0 )   // Check if we successfully obtained IP address of WEB server using to redirect HTTP(s) traffic
        {
            LOG.error("Unknown IPv4 address of redirection WEB server.");
            return false;
        }
        
        return true;
    }

    //---- reverseMatch:  reverse the MAC, IP, and TCP ports, for assisting with bi-directional mapping
    private OFMatch reverseMatch(final OFMatch ofMatch) {
        OFMatch origMatch = ofMatch.clone();

        // Reverse MAC addresses
        origMatch.setDataLayerSource(ofMatch.getDataLayerDestination());
        origMatch.setDataLayerDestination(ofMatch.getDataLayerSource());

        // reverse IP addresses
        origMatch.setNetworkSource(ofMatch.getNetworkDestination());
        origMatch.setNetworkDestination(ofMatch.getNetworkSource());

        // reverse ports
        origMatch.setTransportSource(ofMatch.getTransportDestination());
        origMatch.setTransportDestination(ofMatch.getTransportSource());

        return origMatch;
    }

    //---- makeKey:  create the key for storing HTTP request info
    private OFMatch makeKey( OFMatch ofMatch ) {
    	
        OFMatch keyMatch = ofMatch.clone();

        keyMatch.setDataLayerDestination( new byte[6] );
        keyMatch.setInputPort( (short) 0 );
        keyMatch.setNetworkDestination( 0 );
        
        return keyMatch;
    }

}
