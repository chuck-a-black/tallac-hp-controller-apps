package com.tallac.cb.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.of.ctl.ControllerService;
import com.hp.of.ctl.DataPathEvent;
import com.hp.of.ctl.DataPathListener;

import com.hp.of.ctl.QueueEvent;
import com.hp.of.lib.ProtocolVersion;
import com.hp.of.lib.dt.BufferId;
import com.hp.of.lib.dt.DataPathId;
import com.hp.of.lib.instr.ActOutput;
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
import com.hp.util.ip.IpProtocol;
import com.hp.util.ip.PortNumber;

//=================================================================================================
public class SwitchListener implements DataPathListener {

	private static final Logger LOG = LoggerFactory.getLogger( SwitchListener.class );
	
	private volatile ControllerService mControllerService;

	//----------------------------------------------------------------------------------------
	public void init( ControllerService controllerService )
	{
		LOG.info( "MyBlacklist: SwitchListener: init()" );
		mControllerService = controllerService;
	}
	
	//----------------------------------------------------------------------------------------
	public void startup()
	{
		LOG.info( "MyBlacklist: SwitchListener: startup()" );
		mControllerService.addDataPathListener( this );
	}

	//----------------------------------------------------------------------------------------
	public void shutdown()
	{
		LOG.info( "MyBlacklist: SwitchListener: shutdown()" );
		mControllerService.removeDataPathListener( this );
	}

	//----------------------------------------------------------------------------------------
	@Override
	public void event( DataPathEvent dpEvent ) {

		LOG.info( "MyBlacklist: SwitchListener: event(): {}", dpEvent );
		
		switch( dpEvent.type() ) {
		
		case DATAPATH_CONNECTED: 
			setInitialFlows( dpEvent.dpid() );
			break;
			
		case DATAPATH_DISCONNECTED:
			break;
			
		default:
			break;
		}
		
	}

	//----------------------------------------------------------------------------------------
	@Override
	public void queueEvent( QueueEvent queueEvent ) {
		// TODO Auto-generated method stub
		
	}
	
	//----------------------------------------------------------------------------------------
	private void setInitialFlows( DataPathId dpId ) {

		LOG.info( "MyBlacklist: SwitchListener: setInitialFlows(): dpid={}", dpId );

    	OfmMutableFlowMod dnsFlowMod = (OfmMutableFlowMod)MessageFactory.create( ProtocolVersion.V_1_0, MessageType.FLOW_MOD );

    	// Create the DNS match and add it to the DNS flow.
    	MutableMatch dnsMatch = MatchFactory.createMatch( ProtocolVersion.V_1_0 )
    			.addField( FieldFactory.createBasicField( ProtocolVersion.V_1_0, OxmBasicFieldType.ETH_TYPE, EthernetType.IPv4 ))
    			.addField( FieldFactory.createBasicField( ProtocolVersion.V_1_0, OxmBasicFieldType.IP_PROTO, IpProtocol.UDP ))
    			.addField( FieldFactory.createBasicField( ProtocolVersion.V_1_0, OxmBasicFieldType.UDP_DST,  PortNumber.valueOf(53) ));
    	dnsFlowMod.match( (Match)dnsMatch.toImmutable() );
    	
    	// Create the forward-to-controller action and add it to the DNS flow.
    	Action dnsAction = ActionFactory.createAction( ProtocolVersion.V_1_0, ActionType.OUTPUT, Port.CONTROLLER, ActOutput.CONTROLLER_NO_BUFFER );
    	dnsFlowMod.addAction( dnsAction );

    	// Add the other fields for the flow mod message.
    	dnsFlowMod.command( FlowModCommand.ADD )
    			  .hardTimeout(0)
    			  .idleTimeout(0)
    			  .priority( 40000 )
    			  .bufferId( BufferId.NO_BUFFER );

    	// Send the flow modification message to the switch
    	try{
        	mControllerService.sendFlowMod( (OfmFlowMod)dnsFlowMod.toImmutable(), dpId );
    		LOG.info( "MyBlacklist: SwitchListener: set initial flows successfully" );
    	}
    	catch( Exception e ) {
    		LOG.info( "MyBlacklist: SwitchListener: setInitialFlows() exception: {}", e );
    	}

    	OfmMutableFlowMod ipFlowMod = (OfmMutableFlowMod)MessageFactory.create( ProtocolVersion.V_1_0, MessageType.FLOW_MOD );
    	
    	// Create the IP match and add it to the IP flow.
    	MutableMatch ipMatch = MatchFactory.createMatch(  ProtocolVersion.V_1_0 )
    			.addField( FieldFactory.createBasicField( ProtocolVersion.V_1_0, OxmBasicFieldType.ETH_TYPE, EthernetType.IPv4 ));
    	ipFlowMod.match( (Match)ipMatch.toImmutable() );
    	
    	// Create the forward-to-controller action and add it to the IP flow.
    	Action ipAction = ActionFactory.createAction( ProtocolVersion.V_1_0, ActionType.OUTPUT, Port.CONTROLLER, ActOutput.CONTROLLER_NO_BUFFER );
    	ipFlowMod.addAction( ipAction );
    	
    	// Add the other fields for the flow mod message.
    	ipFlowMod.command( FlowModCommand.ADD )
    		  	 .hardTimeout(0)
    		  	 .idleTimeout(0)
    		  	 .priority( 30000 )
		         .bufferId( BufferId.NO_BUFFER );
    	
    	// Now set this flow on the switch
    	try{
			mControllerService.sendFlowMod( (OfmFlowMod)ipFlowMod.toImmutable(), dpId );
    		LOG.info( "MyBlacklist: SwitchListener: set initial flows successfully" );
    	}
    	catch( Exception e ) {
    		LOG.info( "MyBlacklist: SwitchListener: setInitialFlows():  exception: {}", e );
    		LOG.info( "MyBlacklist: SwitchListener: setInitialFlows():  exception: cause: {}", e.getCause() );
    	}
    	
    	// Create an OF flow mod message for our ARP flow.
    	OfmMutableFlowMod arpFlowMod = (OfmMutableFlowMod) MessageFactory.create( ProtocolVersion.V_1_0, MessageType.FLOW_MOD );
    	
    	// Create the ARP match and add it to the ARP flow.
    	MutableMatch arpMatch = MatchFactory.createMatch( ProtocolVersion.V_1_0 )
    			.addField( FieldFactory.createBasicField( ProtocolVersion.V_1_0, OxmBasicFieldType.ETH_TYPE, EthernetType.ARP ));
    	arpFlowMod.match( (Match)arpMatch.toImmutable() );
    	
    	// Create the forward-to-controller action and add it to the IP flow.
    	Action arpAction = ActionFactory.createAction( ProtocolVersion.V_1_0, ActionType.OUTPUT, Port.NORMAL  );
    	arpFlowMod.addAction( arpAction );
    	
    	// Add the other fields for the flow mod message.
    	arpFlowMod.command( FlowModCommand.ADD )
    			  .hardTimeout(0)
    			  .idleTimeout(0)
    			  .priority( 40000 )
      			  .bufferId( BufferId.NO_BUFFER );
    	
    	// Now set this flow on the switch
    	try{
    		mControllerService.sendFlowMod( (OfmFlowMod)arpFlowMod.toImmutable(), dpId );
    		LOG.info( "MyBlacklist: SwitchListener: set initial flows successfully" );
    	}
    	catch( Exception e ) {
    		LOG.info( "MyBlacklist: SwitchListener: setInitialFlows():  exception: {}", e );
    		LOG.info( "MyBlacklist: SwitchListener: setInitialFlows():  exception: cause: {}", e.getCause() );
    	}
    	
	}
}
