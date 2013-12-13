package com.tallac.blacklist.handler;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.of.ctl.ControllerService;
import com.hp.of.ctl.pkt.MessageContext;
import com.hp.of.lib.ProtocolVersion;
import com.hp.of.lib.dt.BufferId;
import com.hp.of.lib.dt.DataPathId;
import com.hp.of.lib.instr.Action;
import com.hp.of.lib.instr.ActionFactory;
import com.hp.of.lib.instr.ActionType;
import com.hp.of.lib.match.FieldFactory;
import com.hp.of.lib.match.Match;
import com.hp.of.lib.match.MatchFactory;
import com.hp.of.lib.match.MutableMatch;
import com.hp.of.lib.match.OxmBasicFieldType;
import com.hp.of.lib.msg.FlowModCommand;
import com.hp.of.lib.msg.MessageFactory;
import com.hp.of.lib.msg.MessageType;
import com.hp.of.lib.msg.OfmFlowMod;
import com.hp.of.lib.msg.OfmMutableFlowMod;
import com.hp.of.lib.msg.Port;
import com.hp.util.ip.EthernetType;
import com.hp.util.ip.IpAddress;
import com.hp.util.pkt.Ip;
import com.hp.util.pkt.Packet;
import com.hp.util.pkt.ProtocolId;
import com.tallac.blacklist.impl.BlacklistComponent;
import com.tallac.blacklist.manager.BlacklistedHostsManager;

public class IpPacketHandler {

    private static final Logger LOG = LoggerFactory.getLogger( IpPacketHandler.class );
    private static final String LOGPREFACE = "[Blacklist: IpPacketHandler]: "; 
    
    private static final short IP_FLOW_TIMEOUT = 20;
    
    private IpAddress mDestIpAddress;
    
    private ControllerService mControllerService;

	//-------------------------------------------------------------------------
    public IpPacketHandler( ControllerService controllerService )
    {
    	mControllerService = controllerService;
    }
    
	//-------------------------------------------------------------------------
	public boolean allowIpDestination(  MessageContext messageContext,
            					        Packet         packetInData )
	{
		// Extract the destination IP address from the packet data.
		Ip ipData = packetInData.get( ProtocolId.IP );
		mDestIpAddress = ipData.dstAddr();
		
		InetAddress ipAddr = mDestIpAddress.toInetAddress();

		// Check the destination IP address against the list of known bad IP addresses.
		if( BlacklistedHostsManager.getInstance().checkIpv4Blacklist( ipAddr ) ) {
			
			LOG.info( LOGPREFACE + "allowIpDestination(): IP request dropped, bad destination address: {}", mDestIpAddress.toString() );
			return false;
		}
		else {
			LOG.trace( LOGPREFACE + "allowIpDestination(): IP request allowed" );
			return true;			
		}

	}

	//-------------------------------------------------------------------------
	public void installIpFlowsForDestination( DataPathId dpId )
	{
    	// Create an OF flow mod message for our initial IP destination address flow.
    	OfmMutableFlowMod ipDestAllowFlowMod = (OfmMutableFlowMod) MessageFactory.create( BlacklistComponent.OF_VERSION, MessageType.FLOW_MOD );
    	
    	// Create the IP destination address match and add it to the flow.
    	MutableMatch match = MatchFactory.createMatch( BlacklistComponent.OF_VERSION )
    			.addField( FieldFactory.createBasicField( ProtocolVersion.V_1_0, OxmBasicFieldType.ETH_TYPE, EthernetType.IPv4 ))
    			.addField( FieldFactory.createBasicField( ProtocolVersion.V_1_0, OxmBasicFieldType.IPV4_DST, mDestIpAddress ));
    	  	 	
    	ipDestAllowFlowMod.match( (Match)match.toImmutable() );
    	
    	// Create the forward-normal action and add it to the flow.
    	Action action = ActionFactory.createAction( ProtocolVersion.V_1_0, ActionType.OUTPUT, Port.NORMAL  );
    	ipDestAllowFlowMod.addAction( action );
    	
    	// Add the other fields for the flow mod message.
    	ipDestAllowFlowMod.command( FlowModCommand.ADD )
    			  .hardTimeout(0)
    			  .idleTimeout( IP_FLOW_TIMEOUT )
    			  .priority( BlacklistComponent.PRI_IP_FLOW )
    			  .bufferId( BufferId.NO_BUFFER );
    	
    	// Now set this flow on the switch
    	try{
    		LOG.trace( LOGPREFACE + "installIpFlowsForDestination(): setting flow: {}", ipDestAllowFlowMod );
        	mControllerService.sendFlowMod( (OfmFlowMod)ipDestAllowFlowMod.toImmutable(), dpId );
    	}
    	catch( Exception e ) {
    		LOG.info( LOGPREFACE + "setInitialFlows(): exception: {}", e );
    		LOG.info( LOGPREFACE + "setInitialFlows(): exception: cause: {}", e.getCause() );
    	}
	}
}
