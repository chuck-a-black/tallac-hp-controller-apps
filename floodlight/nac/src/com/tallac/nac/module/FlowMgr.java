/*
 * Copyright (c) 2013, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.nac.module;

import static org.openflow.protocol.OFMatch.OFPFW_ALL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.staticflowentry.StaticFlowEntries;
import net.floodlightcontroller.staticflowentry.StaticFlowEntryPusher;
import net.floodlightcontroller.util.MACAddress;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.U16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The FlowMgr class responsible for creating and sending OpenFlow messages
 * to OpenFlow switches.
 */
public class FlowMgr
{
    private static final FlowMgr INSTANCE        = new FlowMgr();
    private static final Logger  LOG             = LoggerFactory.getLogger(FlowMgr.class);
    public static  final short   NO_IDLE_TIMEOUT = 0;

    private IFloodlightProviderService mProvider;

    private ArrayList<OFFlowMod> mDefaultFlows;     // List of default flow. Initializes on init.
    
    private short mAuthFlowPriority;
    private short mUnauthFlowPriority;
    private short mDropUnauthFlowPriority;

    //---- FlowMgr:  private constructor to prevent external instantiation
    private FlowMgr() 
    {
        mDefaultFlows           = new ArrayList<OFFlowMod>();
        mProvider               = null;
        mAuthFlowPriority       = 0;
        mUnauthFlowPriority     = 0;
        mDropUnauthFlowPriority = 0;
    }

    public static FlowMgr getInstance() { return INSTANCE; }
    
    /**
     * Initialize FlowMgr.
     * 
     * @param context - floodlight context
     */
    public void init(final FloodlightModuleContext context)
    {
        LOG.debug("Initialize NAC flow manager.");
        if (mProvider != null) throw new RuntimeException("NAC flow manager not initialized");

        mProvider = context.getServiceImpl(IFloodlightProviderService.class);
        
        //---- Parse configured mandatory flows
        String[] flowList = ConfigMgr.getInstance().getStringArray("nac.mandatoryFlow");
        parseFlowStringArray( flowList );
        
        //---- Parse configured default flows
        flowList = ConfigMgr.getInstance().getStringArray("nac.defaultFlow");
        parseFlowStringArray(flowList);

        //---- Get pre-configured priorities
        mAuthFlowPriority       = ConfigMgr.getInstance().getShort( "nac.authFlowPriority" );
        mUnauthFlowPriority     = ConfigMgr.getInstance().getShort( "nac.unauthFlowPriority" );
        mDropUnauthFlowPriority = ConfigMgr.getInstance().getShort( "nac.dropUnauthFlowPriority" );
    }
    
    /**
     * Set default flows on switch connection
     * 
     * @param ofSwitch - target switch
     */
    //---- setDefaultFlows:  set default flows for the given switch
    public void setDefaultFlows( final IOFSwitch ofSwitch )
    {
        for (OFFlowMod flow : mDefaultFlows) { sendFlowModMessage( ofSwitch, flow );  }  // Iterate through flows, setting each on switch
    }
    
    /**
     * Send packetOut message with empty action list instructing switch to 
     * drop the packet
     * 
     * @param ofSwitch - target switch
     * @param cntx     - Floodlight context
     * @param packetIn - packetIn message containing packet that should be 
     *                   dropped
     */
    public void dropPacket( final IOFSwitch         ofSwitch,
                            final FloodlightContext cntx,
                            OFPacketIn              packetIn )
    {
        LOG.debug("Drop packet");
        
        final List<OFAction> flActions = new ArrayList<OFAction>();
        sendPacketOut( ofSwitch, cntx, packetIn, flActions );
    }

    /**
     * Send packetOut message with provided actions in response for input
     * PacketIn message
     * 
     * @param ofSwitch - target switch
     * @param cntx     - Floodlight context
     * @param packetIn - packetIn message
     * @param actions  - OpenFlow actions
     */
    public void sendPacketOut( final IOFSwitch         ofSwitch,
                               final FloodlightContext cntx,
                               final OFPacketIn        packetIn,
                               final List<OFAction>    actions )
    {
        if (mProvider == null) LOG.error("FlowMgr is not initialized yet.");

        final OFPacketOut packetOut = (OFPacketOut) mProvider.getOFMessageFactory().getMessage(OFType.PACKET_OUT);
        packetOut.setActions(actions);

        /*
         * According to Rob Sherwood comment: https://groups.google.com/a/openflowhub
         * .org/forum/?fromgroups=#!msg/floodlight-dev/VY4JqEm3jxo/VXZwbOrMhbIJ
         * we have to manually calculate and set the length in openflow messages.
         *
         * RS: " The flowMod.length field _really_ should be updated when you
         * call setActions() but it is not. We'll try to fix this (more
         * holistically) soon. It should be fixed in later Floodlight release"
         */
        
        int actionsLength = 0;
        for (final OFAction action : actions) { actionsLength += action.getLengthU(); }
        packetOut.setActionsLength(( short) actionsLength );

        // set buffer-id, in-port and packet-data based on packet-in
        short poLength = (short)( packetOut.getActionsLength() + OFPacketOut.MINIMUM_LENGTH );

        packetOut.setBufferId( packetIn.getBufferId() );
        packetOut.setInPort(   packetIn.getInPort() );
        
        if( packetIn.getBufferId() == OFPacketOut.BUFFER_ID_NONE )
        {
            final byte[] packetData = packetIn.getPacketData();
            poLength += packetData.length;
            packetOut.setPacketData( packetData );
        }
        packetOut.setLength(poLength);

        try
        {
            LOG.trace( "Writing PacketOut switch={} packet-out={}", new Object[] { ofSwitch, packetOut } );
            
            ofSwitch.write( packetOut, cntx );
            ofSwitch.flush();
        }
        catch (final IOException e)
        {
            LOG.error( "Failure writing PacketOut switch={} packet-out={}", new Object[] { ofSwitch, packetOut }, e );
        }
    }

    /**
     * Send packetOut message forwarding packet to provided switch port
     * 
     * @param ofSwitch   - target switch
     * @param context    - FloodLight context
     * @param packetIn   - packetIn message
     * @param outputPort - target output port
     */
    public void sendPacketOut( IOFSwitch         ofSwitch, 
                               FloodlightContext context,
                               OFPacketIn        packetIn,
                               short             outputPort ) 
    {
    	//---- Set the action for sending the packet out the appropriate outputPort
        List<OFAction> ofActions = new ArrayList<OFAction>();
        ofActions.add( new OFActionOutput( outputPort ) );
        
        sendPacketOut(ofSwitch, context, packetIn, ofActions); // Forward packet back to the switch
    }
    
    /**
     * Send packetOut message with action FLOOD in response for input
     * PacketIn message
     */
    public void floodPacket( IOFSwitch         ofSwitch, 
    		                 FloodlightContext context,
                             OFPacketIn        packetIn ) 
    {
        sendPacketOut( ofSwitch, context, packetIn, OFPort.OFPP_FLOOD.getValue() ); // Send packet with FLOOD action
    }
    /**
     * Create bidirectional client's flow on all connected switches.
     *
     * @param  mac - client mac
     */
    public void setAuthFlowsOnAllSwitches( final MACAddress mac ) 
    {
        modifyAuthFlowsOnAllSwitches( OFFlowMod.OFPFC_ADD, mac );
    }
 
    /**
     * Create bidirectional client's flows to allow HTTP traffic on all connected switches.
     *
     * @param  mac - client mac
     */
    public void setUnauthFlowsOnAllSwitches( final MACAddress mac ) 
    {
        modifyUnauthFlowsOnAllSwitches( OFFlowMod.OFPFC_ADD, mac );
    }

    /**
     * Remove bidirectional client's MAC flow on all connected switches.
     *
     * @param  mac - client mac
     */
    public void removeAuthFlowsOnAllSwitches( final MACAddress mac ) 
    {
        modifyAuthFlowsOnAllSwitches( OFFlowMod.OFPFC_DELETE, mac );
    }
    
    public void removeUnauthFlowsOnAllSwitches( final MACAddress mac )
    {
    	modifyUnauthFlowsOnAllSwitches( OFFlowMod.OFPFC_DELETE, mac );
    }
    
    /**
     * Create bidirectional client's flow on target switches.
     *
     * @param  ofSwitch - target switch
     * @param  mac    - target MAC Address
     */
    public void setAuthFlowsOnSwitch( final IOFSwitch  ofSwitch,
                                      final MACAddress mac ) 
    {
        modifyAuthFlowOnSwitch( ofSwitch, OFFlowMod.OFPFC_ADD, mac );
    }
    
    /**
     * Create bidirectional client flows to allow HTTP traffic on target switches.
     *
     * @param  ofSwitch - target switch
     * @param  mac    - target MAC Address
     */
    public void setUnauthFlowsOnSwitch( final IOFSwitch  ofSwitch,
                                        final MACAddress mac ) {
    	
    	modifyUnauthFlowsOnSwitch( ofSwitch, OFFlowMod.OFPFC_ADD, mac );   	
    }

    /**
     * Remove bidirectional client flows to allow HTTP traffic on target switches.
     *
     * @param  ofSwitch - target switch
     * @param  mac    - target MAC Address
     */
    public void removeUnauthFlowsOnSwitch(  final IOFSwitch  ofSwitch, 
    		                                final MACAddress mac       ) {

    	modifyUnauthFlowsOnSwitch( ofSwitch, OFFlowMod.OFPFC_DELETE, mac );   	
    }

    /**
     * Parse string array of flows and add parsed flows to the list of flows
     * that will be set up on the switch connection
     * 
     * @param flowList - String array of flows
     */
    private void parseFlowStringArray(String[] flowList)
    {
        for (String flow : flowList) 
        {
            OFFlowMod ofm = getFlowModFromString( flow );
            if( ofm != null ) mDefaultFlows.add(ofm);
        }
    }

    /**
     * Modify bidirectional client's MAC flow on all connected switches.
     *
     * @param  command - FlowMod command ADD/DELETE
     * @param  mac - target mac
     */
    private void modifyAuthFlowsOnAllSwitches( final short      command,
                                               final MACAddress mac      ) 
    {
        // Loop through connected switches and add bidirectional flow for the client
        Map<Long, IOFSwitch> switches = mProvider.getSwitches();
        
        for( Map.Entry<Long, IOFSwitch> ofSwitchEntry : switches.entrySet() ) {
            IOFSwitch ofSwitch = ofSwitchEntry.getValue();
            modifyAuthFlowOnSwitch( ofSwitch, command, mac );
        }
    }
 
    /**
     * Modify bidirectional client's Allow-HTTP flows on all connected switches.
     *
     * @param  command - FlowMod command ADD/DELETE
     * @param  mac - target mac
     */
    private void modifyUnauthFlowsOnAllSwitches( final short      command,
                                                 final MACAddress mac     ) 
    {
        // Loop through connected switches and add bidirectional flow for the client
        Map<Long, IOFSwitch> switches = mProvider.getSwitches();
        
        for( Map.Entry<Long, IOFSwitch> ofSwitchEntry : switches.entrySet() ) {
            IOFSwitch ofSwitch = ofSwitchEntry.getValue();
            modifyUnauthFlowsOnSwitch( ofSwitch, command, mac );
        }
    }

    /**
     * Set/delete client's mac flow on target switches.
     *
     * @param  ofSwitch - target switch
     * @param  command - FlowMod command ADD/DELETE
     * @param  mac    - target MAC Address
     */
    private void modifyAuthFlowOnSwitch( final IOFSwitch  ofSwitch,
                                         final short      command,
                                         final MACAddress mac )
    {
        
        /* Create output action "NORMAL"*/
//        OFActionOutput ofAction  = new OFActionOutput( OFPort.OFPP_NORMAL.getValue() );
//        List<OFAction> ofActions = new ArrayList<OFAction>();
//        ofActions.add(ofAction);
        
        List<OFAction> ofActions = new ArrayList<OFAction>();
        ofActions.add( new OFActionOutput( OFPort.OFPP_NORMAL.getValue() ));
        
        // Add flow with specified destination MAC address
        OFMatch ofMatch = new OFMatch();
        ofMatch.setWildcards(            allExclude( OFMatch.OFPFW_DL_DST) );
        ofMatch.setDataLayerDestination( mac.toBytes() );
        sendFlowModMessage(              ofSwitch, command, ofMatch, ofActions, mAuthFlowPriority, NO_IDLE_TIMEOUT );

        // Add flow with specified source MAC address
        ofMatch = new OFMatch();
        ofMatch.setWildcards(       allExclude( OFMatch.OFPFW_DL_SRC) );
        ofMatch.setDataLayerSource( mac.toBytes() );
        sendFlowModMessage(         ofSwitch, command, ofMatch, ofActions, mAuthFlowPriority, NO_IDLE_TIMEOUT );
    }
    
    /**
     * Set/delete client's HTTP flows on target switches.
     *
     * @param  ofSwitch - target switch
     * @param  command - FlowMod command ADD/DELETE
     * @param  mac    - target MAC Address
     */
    private void modifyUnauthFlowsOnSwitch( final IOFSwitch  ofSwitch,
                                            final short      command,
                                            final MACAddress mac ) {
    	
        // Create output action for forward these HTTP flows to the CONTROLLER
//        OFAction ofActionToController         = new OFActionOutput(OFPort.OFPP_CONTROLLER.getValue());
//        List<OFAction> ofActionsToController  = new ArrayList<OFAction>();
//        ofActionsToController.add(ofActionToController);
        
        List<OFAction> ofActionsToController  = new ArrayList<OFAction>();
        ofActionsToController.add( new OFActionOutput( OFPort.OFPP_CONTROLLER.getValue() ) );
 
        // Create output action for forward these HTTP flows to the CONTROLLER
//        OFAction ofActionToController         = new OFActionOutput(OFPort.OFPP_CONTROLLER.getValue());
//        List<OFAction> ofActionsToController  = new ArrayList<OFAction>();
//        ofActionsToController.add(ofActionToController);

        List<OFAction> ofActionsNormal  = new ArrayList<OFAction>();
        ofActionsNormal.add( new OFActionOutput( OFPort.OFPP_NORMAL.getValue() ) );

        
        //---- Construct the flows for HTTP (ports 80, 443, 8080), with source address of the given MAC
        OFMatch ofMatch = new OFMatch();
        ofMatch.setWildcards( allExclude( OFMatch.OFPFW_DL_SRC, OFMatch.OFPFW_DL_TYPE, OFMatch.OFPFW_NW_PROTO, OFMatch.OFPFW_TP_DST) );
        
        ofMatch.setDataLayerSource(      mac.toBytes() );
        ofMatch.setDataLayerType(        Ethernet.TYPE_IPv4 );
        ofMatch.setNetworkProtocol(      IPv4.PROTOCOL_TCP );
        ofMatch.setTransportDestination( (short)80 );
        sendFlowModMessage(              ofSwitch, command, ofMatch, ofActionsToController, mUnauthFlowPriority, NO_IDLE_TIMEOUT );
        
        ofMatch.setTransportDestination( (short)(443) );
        sendFlowModMessage( ofSwitch, command, ofMatch, ofActionsToController, mUnauthFlowPriority, NO_IDLE_TIMEOUT );
        
        ofMatch.setTransportDestination( (short)(8080) );
        sendFlowModMessage( ofSwitch, command, ofMatch, ofActionsNormal, mUnauthFlowPriority, NO_IDLE_TIMEOUT );
        
        //---- Set equivalent destination MAC flows for 80, 443, 8080
        ofMatch = new OFMatch();
		ofMatch.setWildcards( allExclude( OFMatch.OFPFW_DL_DST, OFMatch.OFPFW_DL_TYPE, OFMatch.OFPFW_NW_PROTO, OFMatch.OFPFW_TP_SRC ) );
		
		ofMatch.setDataLayerDestination( mac.toBytes() );
		ofMatch.setDataLayerType(        Ethernet.TYPE_IPv4 );
		ofMatch.setNetworkProtocol(      IPv4.PROTOCOL_TCP );
		ofMatch.setTransportSource(      (short)80 );
		
        sendFlowModMessage( ofSwitch, command, ofMatch, ofActionsToController, mUnauthFlowPriority, NO_IDLE_TIMEOUT );

        ofMatch.setTransportSource( (short)(443) );
        sendFlowModMessage( ofSwitch, command, ofMatch, ofActionsToController, mUnauthFlowPriority, NO_IDLE_TIMEOUT );
        
        ofMatch.setTransportSource( (short)(8080) );
        sendFlowModMessage( ofSwitch, command, ofMatch, ofActionsNormal, mUnauthFlowPriority, NO_IDLE_TIMEOUT );        
         
        //---- Lastly, drop all other traffic for this MAC
        /* Create output action "NORMAL"*/
        List<OFAction> ofActions = new ArrayList<OFAction>();
        
        // Add flow with specified destination MAC address
        ofMatch = new OFMatch();
        ofMatch.setWildcards(            allExclude( OFMatch.OFPFW_DL_DST) );
        ofMatch.setDataLayerDestination( mac.toBytes() );
        sendFlowModMessage(              ofSwitch, command, ofMatch, ofActions, mDropUnauthFlowPriority, NO_IDLE_TIMEOUT );

        // Add flow with specified source MAC address
        ofMatch = new OFMatch();
        ofMatch.setWildcards(       allExclude( OFMatch.OFPFW_DL_SRC) );
        ofMatch.setDataLayerSource( mac.toBytes() );
        sendFlowModMessage(         ofSwitch, command, ofMatch, ofActions, mDropUnauthFlowPriority, NO_IDLE_TIMEOUT );

   }

    /**
     * Create OFFlowMod object and send FlowMod message to specified device.
     * 
     * @param ofSwitch     - target siwtch
     * @param command      - FlowMod comand (ADD/DELETE)
     * @param ofMatch      - flow match
     * @param actions      - flow actions
     * @param priority     - flow priority
     * @param idleTimeout  - flow idle timeout
     */
    private void sendFlowModMessage( final IOFSwitch      ofSwitch,
                                     final short          command,
                                     final OFMatch        ofMatch,
                                     final List<OFAction> actions,
                                     final short          priority,
                                     final short          idleTimeout )
    {
        if (mProvider == null) LOG.error("FlowMgr is not initialized yet."); 

        final OFFlowMod ofm = (OFFlowMod) mProvider.getOFMessageFactory().getMessage( OFType.FLOW_MOD );
        ofm.setBufferId( OFPacketOut.BUFFER_ID_NONE );
        
        ofm.setCommand(command).setIdleTimeout(idleTimeout).setPriority(priority).setMatch(ofMatch.clone()).setOutPort(OFPort.OFPP_NONE).setActions(actions);
        
        /* If application need to be notified about removed/expired flows, for
         * example to collect flow statistics, then setup SEND_FLOW_REM flag.setFlags(OFFlowMod.OFPFF_SEND_FLOW_REM)
         */
        
        sendFlowModMessage( ofSwitch, ofm );
    }

    /**
     * Send FlowMod message to specified switch.
     * 
     * @param ofSwitch - target switch
     * @param ofm      - OFFlowMod object representing flow
     */
    private void sendFlowModMessage( final IOFSwitch ofSwitch,
                                     final OFFlowMod ofm )
    {       
        // Set transaction ID
        ofm.setXid(ofSwitch.getNextTransactionId());
        
        /*
         * According to Rob Sherwood comment:
         * https://groups.google.com/a/openflowhub
         * .org/forum/?fromgroups=#!msg/floodlight-dev/VY4JqEm3jxo/VXZwbOrMhbIJ
         * we have to manually calculate and set the length in openflow
         * messages.
         *
         * RS: " The flowMod.length field _really_ should be updated when you
         * call setActions() but it is not. We'll try to fix this (more
         * holistically) soon. It should be fixed in later Floodlight release"
         */
        int actionsLength = 0;
        for (final OFAction action : ofm.getActions()) { actionsLength += action.getLengthU(); }
        ofm.setLengthU( OFFlowMod.MINIMUM_LENGTH + actionsLength );

        try
        {
            ofSwitch.write(ofm, null);
            ofSwitch.flush();
        }
        catch (final IOException e)
        {
            LOG.error("Unable to set flow " + ofm + " on switch {} err: {}", ofSwitch.getId(), e);
        }
    }
    
    /**
     * Create OFFlowMod object from provided string representation of the flow
     * 
     * @param flowString - string containing flow description
     * 
     * @return - OFFLowMod object or null if any errors
     */
    private OFFlowMod getFlowModFromString( String flowString ) {
    	
        final OFFlowMod ofm = (OFFlowMod) mProvider.getOFMessageFactory().getMessage(OFType.FLOW_MOD);
        
        ofm.setBufferId( OFPacketOut.BUFFER_ID_NONE );
        ofm.setIdleTimeout( ( short ) 0 );
        
        StringBuffer matchString = new StringBuffer();
        
        String[] tokens = flowString.split(",");
        for (String token : tokens) 
        {
            String[] parameters = token.split("=");
            if (parameters.length > 2 || parameters.length < 1) 
            {
                LOG.error("Unable to parse flow {}. Parameter {}", flowString, token);
                return null;
            }
            String key = parameters[0];
            if (parameters.length == 2) 
            {
                String value = parameters[1];

                if (key.equals(StaticFlowEntryPusher.COLUMN_IDLE_TIMEOUT)) 
                {
                    ofm.setIdleTimeout(U16.t(Integer.valueOf(value)));
                    continue;
                }
                if (key.equals(StaticFlowEntryPusher.COLUMN_HARD_TIMEOUT)) 
                {
                    ofm.setHardTimeout(U16.t(Integer.valueOf(value)));
                    continue;
                }
                if (key.equals(StaticFlowEntryPusher.COLUMN_PRIORITY))
                {
                    ofm.setPriority(U16.t(Integer.valueOf(value)));
                    continue;
                }
                if (key.equals(StaticFlowEntryPusher.COLUMN_ACTIONS))
                {
                    // Create empty list of actions if action field contains
                    // word "drop". Empty list of actions tells switch to drop
                    // the packet. Otherwise let Floodlight parse action field
                    if (value.contains("drop"))
                    {
                        final List<OFAction> action = new ArrayList<OFAction>();
                        ofm.setActions(action);
                    }
                    else
                    {
                        value = value.replaceAll(":", "=");
                        StaticFlowEntries.parseActionString(ofm, value, LOG);
                    }
                    continue;
                }
            }
            if (parameters.length == 1)
            {
                String matchStr = null;
                if( key.equalsIgnoreCase("ip") )   matchStr = "dl_type=0x0800";
                if( key.equalsIgnoreCase("tcp") )  matchStr = "dl_type=0x0800,nw_proto=6";
                if( key.equalsIgnoreCase("udp") )  matchStr = "dl_type=0x0800,nw_proto=17";
                if( key.equalsIgnoreCase("icmp") ) matchStr = "dl_type=0x0800,nw_proto=1";
                if( key.equalsIgnoreCase("arp") )  matchStr = "dl_type=0x0806";
                
                if (matchStr == null)
                {
                    LOG.error( "Ignoring flow entry {} with unknown token: " + key, flowString );
                    return null;
                }
                
                if( matchString.length() > 0 ) matchString.append(",");
                matchString.append( matchStr );
                continue;
            }

            if( matchString.length() > 0 ) matchString.append(",");
            matchString.append( token );
        }

        OFMatch ofMatch = new OFMatch();
        String  match   = matchString.toString();
        
        try {
            ofMatch.fromString( match );
            
        } catch( IllegalArgumentException e ) {
            LOG.error( "Ignoring flow entry {} with illegal OFMatch() key: " + match, flowString );
            return null;
        }
        
        ofm.setMatch(ofMatch);

        return ofm;
    }

    /**
     * Return a selective wildcard with indicated flags excluded from all.
     */
    private static int allExclude(final int... flags)
    {
        int wc = OFPFW_ALL;
        for( final int f : flags ) wc &= ~f;

        return wc;
    }

}
