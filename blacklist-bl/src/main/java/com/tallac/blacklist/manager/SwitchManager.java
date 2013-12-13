package com.tallac.blacklist.manager;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.of.ctl.ControllerService;
import com.hp.of.lib.OpenflowException;
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
import com.tallac.blacklist.impl.BlacklistComponent;

public class SwitchManager {

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private volatile ControllerService controllerService;
    
    private static final SwitchManager INSTANCE = new SwitchManager();
    
    private static final PortNumber DNS_PORT = PortNumber.valueOf( 53 );

    private static ControllerService mControllerService;

    private static final Logger LOG = LoggerFactory.getLogger( SwitchManager.class );;
    private static final String LOGPREFACE = "[Blacklist: SwitchManager]: ";        


    public static SwitchManager getInstance() { return INSTANCE; } 

    //---------------------------------------------------------------------------------------------
    private SwitchManager() {
    }
    
    //---------------------------------------------------------------------------------------------
    public void init( final ControllerService controllerService )
    {
    	LOG.info( LOGPREFACE + "init(): Creating SwitchManager." );
    	
        mControllerService = controllerService;
    }

    //---------------------------------------------------------------------------------------------
    public void setInitialFlows( DataPathId dpId )
    {
    	LOG.info( LOGPREFACE + "setInitialFlows(): Setting initial flows for dpid: {}", dpId );
    	
    	// Create an OF flow mod message for our initial DNS flow.
    	OfmMutableFlowMod dnsFlowMod = (OfmMutableFlowMod) MessageFactory.create( BlacklistComponent.OF_VERSION, MessageType.FLOW_MOD );
    	
    	// Create the DNS match and add it to the DNS flow.
    	MutableMatch dnsMatch = MatchFactory.createMatch( BlacklistComponent.OF_VERSION )
    			.addField( FieldFactory.createBasicField( ProtocolVersion.V_1_0, OxmBasicFieldType.ETH_TYPE, EthernetType.IPv4 ))
    			.addField( FieldFactory.createBasicField( ProtocolVersion.V_1_0, OxmBasicFieldType.IP_PROTO, IpProtocol.UDP ))
    			.addField( FieldFactory.createBasicField( ProtocolVersion.V_1_0, OxmBasicFieldType.UDP_DST, DNS_PORT ));
    	  	 	
    	dnsFlowMod.match( (Match)dnsMatch.toImmutable() );
    	
    	// Create the forward-to-controller action and add it to the DNS flow.
    	Action dnsAction = ActionFactory.createAction( ProtocolVersion.V_1_0, ActionType.OUTPUT, Port.CONTROLLER, ActOutput.CONTROLLER_NO_BUFFER );
    	dnsFlowMod.addAction( dnsAction );
    	
    	// Add the other fields for the flow mod message.
    	dnsFlowMod.command( FlowModCommand.ADD )
    			  .hardTimeout(0)
    			  .idleTimeout(0)
    			  .priority( BlacklistComponent.PRI_DNS_FLOW )
    			  .bufferId( BufferId.NO_BUFFER );
    	
    	// Now set this flow on the switch
    	try{
        	mControllerService.sendFlowMod( (OfmFlowMod)dnsFlowMod.toImmutable(), dpId );
    	}
    	catch( Exception e ) {
    		LOG.info( LOGPREFACE + "setInitialFlows(): exception: {}", e );
    		LOG.info( LOGPREFACE + "setInitialFlows(): exception: cause: {}", e.getCause() );
    	}
    	
    	// Create an OF flow mod message for our 'otherwise forward IP packets to the controller' flow.
    	OfmMutableFlowMod ipFlowMod = (OfmMutableFlowMod) MessageFactory.create( BlacklistComponent.OF_VERSION, MessageType.FLOW_MOD );
    	
    	// Create the IP match and add it to the IP flow.
    	MutableMatch ipMatch = MatchFactory.createMatch( BlacklistComponent.OF_VERSION )
    			.addField( FieldFactory.createBasicField( ProtocolVersion.V_1_0, OxmBasicFieldType.ETH_TYPE, EthernetType.IPv4 ));
    	ipFlowMod.match( (Match)ipMatch.toImmutable() );
    	
    	// Create the forward-to-controller action and add it to the IP flow.
    	Action ipAction = ActionFactory.createAction( ProtocolVersion.V_1_0, ActionType.OUTPUT, Port.CONTROLLER, ActOutput.CONTROLLER_NO_BUFFER );
    	ipFlowMod.addAction( ipAction );
    	
    	// Add the other fields for the flow mod message.
    	ipFlowMod.command( FlowModCommand.ADD )
    		  	 .hardTimeout(0)
    		  	 .idleTimeout(0)
    		  	 .priority( BlacklistComponent.PRI_IP_DEFAULT_FLOW )
		         .bufferId( BufferId.NO_BUFFER );
    	
    	// Now set this flow on the switch
    	try{
			mControllerService.sendFlowMod( (OfmFlowMod)ipFlowMod.toImmutable(), dpId );
    	}
    	catch( Exception e ) {
    		LOG.info( LOGPREFACE + "setInitialFlows():  exception: {}", e );
    		LOG.info( LOGPREFACE + "setInitialFlows():  exception: cause: {}", e.getCause() );
    	}
    	
    	// Create an OF flow mod message for our ARP flow.
    	OfmMutableFlowMod arpFlowMod = (OfmMutableFlowMod) MessageFactory.create( BlacklistComponent.OF_VERSION, MessageType.FLOW_MOD );
    	
    	// Create the ARP match and add it to the ARP flow.
    	MutableMatch arpMatch = MatchFactory.createMatch( BlacklistComponent.OF_VERSION )
    			.addField( FieldFactory.createBasicField( ProtocolVersion.V_1_0, OxmBasicFieldType.ETH_TYPE, EthernetType.ARP ));
    	arpFlowMod.match( (Match)arpMatch.toImmutable() );
    	
    	// Create the forward-to-controller action and add it to the IP flow.
    	Action arpAction = ActionFactory.createAction( ProtocolVersion.V_1_0, ActionType.OUTPUT, Port.NORMAL  );
    	arpFlowMod.addAction( arpAction );
    	
    	// Add the other fields for the flow mod message.
    	arpFlowMod.command( FlowModCommand.ADD )
    			  .hardTimeout(0)
    			  .idleTimeout(0)
    			  .priority( BlacklistComponent.PRI_ARP_FLOW )
      			  .bufferId( BufferId.NO_BUFFER );
    	
    	// Now set this flow on the switch
    	try{
    		mControllerService.sendFlowMod( (OfmFlowMod)arpFlowMod.toImmutable(), dpId );
    	}
    	catch( Exception e ) {
    		LOG.info( LOGPREFACE + "setInitialFlows():  exception: {}", e );
    		LOG.info( LOGPREFACE + "setInitialFlows():  exception: cause: {}", e.getCause() );
    	}

    }

}
