package com.tallac.nac.packethandler;

import static net.floodlightcontroller.core.IFloodlightProviderService.CONTEXT_PI_PAYLOAD;
import static net.floodlightcontroller.core.IFloodlightProviderService.bcStore;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IListener.Command;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.DHCP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.UDP;

import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tallac.nac.module.ClientMgr;
import com.tallac.nac.module.ClientMgr.Client;
import com.tallac.nac.module.ClientMgr.ClientState;
import com.tallac.nac.module.FlowMgr;

/**
 * The PacketHandler class is responsible for parsing PacketIn messages.
 */
public class PacketHandler
{
    private static final Logger LOG = LoggerFactory.getLogger(PacketHandler.class);
    public static final short HTTP_PORT  = 80;
    public static final short HTTPS_PORT = 443;
    
	private final IOFSwitch         mOfSwitch;
    private final OFPacketIn        mPacketIn;
	private final FloodlightContext mContext;

    public PacketHandler( final IOFSwitch ofSwitch,
                          final OFMessage msg,
                          final FloodlightContext context )
    {
        mOfSwitch = ofSwitch;
        mPacketIn = (OFPacketIn) msg;
        mContext  = context;
    }
    /**
     * Parse message.
     */
    public Command processPacket()
    {
        
        /* Get match object from packetIn */
        final OFMatch ofMatch = new OFMatch();
        ofMatch.loadFromPacket( mPacketIn.getPacketData(), mPacketIn.getInPort() );
        
        // If incoming packet is not IPv4 packet - drop the packet.
        if( ofMatch.getDataLayerType() != Ethernet.TYPE_IPv4 )
        {
            FlowMgr.getInstance().dropPacket(mOfSwitch, mContext, mPacketIn);
            return Command.STOP;
        }
        
        final Ethernet eth = bcStore.get(mContext, CONTEXT_PI_PAYLOAD);
        
        // Parse PacketIn
        if( eth.getPayload() instanceof IPv4 )
        {
            IPv4 ipv4Data = (IPv4) eth.getPayload();
            
        	// Note:  In order for a packet to have made it to the controller, it must have passed through all the 'unauthenticated user - drop' rules.
        	//        Another assumption is that they are coming in on an edge port (not an uplink port)
        	
        	// First thing is to check if we've received a packet from an *already authenticated* user       	
            Client client = ClientMgr.getInstance().getClient( eth.getSourceMAC() );
        	if( client != null )
   			{
        	    // If the client is there, and is authenticated, then we need to add flows to allow access for this user.
        		if( client.getState() == ClientState.AUTHENTICATED  || client.getState() == ClientState.GUEST ) 
    	        {          
    	            FlowMgr.getInstance().removeUnauthFlowsOnAllSwitches( client.getMacAddr() );   // First remove Unauth flows on switches
    	            FlowMgr.getInstance().setAuthFlowsOnAllSwitches( client.getMacAddr() );        // Next, set Auth flows onto switches.
    	        }

        		// If it isn't authenticated, we should send down flow entries to drop their non-DHCP/DNS/HTTP traffic.
        		else {
        			
        		}
        		
        		// return Command.STOP;
        		
     		}
         	
            //---- Handle DHCP packets -----------------------------------------
            if (ipv4Data.getPayload() instanceof UDP)
            {
                UDP udpData = (UDP) ipv4Data.getPayload();
                if (udpData.getPayload() instanceof DHCP) 
                {
                    // Read necessary data from the packet
                    DhcpPacketHandler.handleDhcpRequest( (DHCP) udpData.getPayload(), mOfSwitch, ofMatch.getInputPort() );
                }
                
                // Send packetOut message back to the switch with action NORMAL
                FlowMgr.getInstance().sendPacketOut( mOfSwitch, mContext, mPacketIn, OFPort.OFPP_NORMAL.getValue() );
                
                return Command.STOP;
            }
            
            //---- Handle HTTP request packets ----------------------------------------
            if( ofMatch.getNetworkProtocol() == IPv4.PROTOCOL_TCP && ( ofMatch.getTransportSource() == HTTP_PORT || ofMatch.getTransportSource() == HTTPS_PORT ) )
            {
                LOG.info("HTTP response. {}", ofMatch);
                HttpPacketHandler.getInstance().handleHttpResponse( mOfSwitch, mContext, mPacketIn, ofMatch );
                
                return Command.STOP;
            }
            
            //---- Handle HTTP response packets ----------------------------------------
            if( ofMatch.getNetworkProtocol() == IPv4.PROTOCOL_TCP && ( ofMatch.getTransportDestination() == HTTP_PORT || ofMatch.getTransportDestination() == HTTPS_PORT ) )
            {
                LOG.info("HTTP request. {}", ofMatch);
                HttpPacketHandler.getInstance().handleHttpRequest( mOfSwitch, mContext, mPacketIn, ofMatch );
                
                return Command.STOP;
            }
        }
        
        LOG.debug("Unknown packet, dropping. Match: {}", ofMatch);
        FlowMgr.getInstance().dropPacket(mOfSwitch, mContext, mPacketIn);
        return Command.STOP;
    }

}
