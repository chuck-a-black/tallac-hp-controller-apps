/*
 * Copyright (c) 2012, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.nac.packethandler;

import java.net.InetAddress;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.DHCP;
import net.floodlightcontroller.util.MACAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tallac.nac.module.ClientMgr;
import com.tallac.nac.module.ClientMgr.Client;
import com.tallac.nac.module.FlowMgr;
import com.tallac.nac.module.SwitchMgr;
import com.tallac.nac.module.ClientMgr.ClientState;
import com.tallac.nac.module.SwitchMgr.NacSwitch;
import com.tallac.nac.utils.TallacUtils;

/**
 * Class responsible for handling DHSP responses 
 */
public class DhcpPacketHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DhcpPacketHandler.class);

    /**
     * Parse DHCP packet, add information about client in the list of clients
     * @param dhcp - dhcp packet
     */
    public static void handleDhcpRequest( DHCP      dhcp, 
    		                              IOFSwitch ofSwitch, 
    		                              short     ofIngressPort ) 
    {
    	if(      dhcp.getHardwareType()  != DHCP.HWTYPE_ETHERNET ) return;  // If it isn't an Ethernet packet, do nothing.
     
        MACAddress  mac    = MACAddress.valueOf(dhcp.getClientHardwareAddress());

    	if( dhcp.getOpCode() == DHCP.OPCODE_REPLY ) {

    		int         ip     = dhcp.getYourIPAddress();
            InetAddress ipAddr = TallacUtils.intToInetAddress(ip);
            
            if( ipAddr == null ) 
            {
                LOG.error("Unable to parse DHCP packet {}.", dhcp);
                return;
            }
            
    		Client client = ClientMgr.getInstance().getClient( mac );
     		if( client == null ) {
     			LOG.info( "Ignoring DHCP responses for clients that we have not heard of before" );
     			return;
     			//client =  ClientMgr.getInstance().createClient( mac, ip, ClientState.UNAUTHENTICATED, "MAC:" + mac.toString(), ofSwitch.getId(), ofIngressPort );
     		}

    		// Check the location of the client for whom this DHCP reply is destined; if it is not from the user's edge device, ignore it.
    		if( client.getSwitchId() != ofSwitch.getId() ) {
    			LOG.info( "Ignoring DHCP responses from clients not attached to this switch" );
    			return;
    		}
            
    		if( client.getState() == ClientState.AUTHENTICATED ) {
    			LOG.info( "Ignoring DHCP responses from clients that are already authenticated" );
    		}
    		else {
    			client.update( ip, ClientState.UNAUTHENTICATED, "MAC:" + mac.toString() );
    			FlowMgr.  getInstance().setUnauthFlowsOnSwitch( ofSwitch, mac );  //  Add flows to allow this user (MAC) to do DHCP, DNS, or HTTP, and prohibit other packets.
    		}
        }
    	
        else if( dhcp.getOpCode() == DHCP.OPCODE_REQUEST ) {
        	
        	//  If this is a request on a non-uplink port, then we know the switch that the user is attached to, so create user
        	NacSwitch nacSwitch = SwitchMgr.getInstance().getSwitch( ofSwitch.getId() );
        	if( nacSwitch == null ) {
        		LOG.info( "DhcpPacketHandler handleDhcpRequest: Unknown switch: {}", ofSwitch );
        	}
        	
        	//---- Only care about DHCP requests that come in from an edge port
        	else if( ofIngressPort != nacSwitch.getUplinkPortNumber() ) {
        		
        		// TODO:  also check for inter-switch links?
        		
        		Client client = ClientMgr.getInstance().getClient( mac );
        		if( client == null ) ClientMgr.getInstance().createClient( mac, 0, ClientState.UNAUTHENTICATED, "MAC:" + mac.toString(), ofSwitch.getId(), ofIngressPort );
        		else 
        		{
        			if( client.getState() == ClientState.AUTHENTICATED ) {
        				LOG.info( "Ignoring DHCP requests for clients that are already authenticated" );
        			}
        			else
	        		{
	        			client.update( 0, ClientState.UNAUTHENTICATED, "MAC:" + mac.toString(), ofSwitch.getId(), ofIngressPort );
	        		}
        		}
        	}
        	//---- Ignore DHCP requests coming in on uplink port
        	else {
        		LOG.info( "Ignoring DHCP requests coming in on the uplink port" );
        		return;
        	}  
        	
        }
    }

}
