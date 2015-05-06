/*
 * Copyright (c) 2012, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.blacklist.module;

import static net.floodlightcontroller.core.IFloodlightProviderService.CONTEXT_PI_PAYLOAD;
import static net.floodlightcontroller.core.IFloodlightProviderService.bcStore;
import static net.floodlightcontroller.devicemanager.IDeviceService.CONTEXT_DST_DEVICE;
import static net.floodlightcontroller.devicemanager.IDeviceService.CONTEXT_SRC_DEVICE;
import static net.floodlightcontroller.devicemanager.IDeviceService.fcStore;
import static org.openflow.protocol.OFMatch.OFPFW_ALL;
import static org.openflow.protocol.OFMatch.OFPFW_DL_TYPE;
import static org.openflow.protocol.OFMatch.OFPFW_NW_PROTO;
import static org.openflow.protocol.OFMatch.OFPFW_TP_DST;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.SwitchPort;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.topology.ITopologyService;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFPacketOut;
import org.openflow.protocol.OFPort;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;
import org.openflow.util.HexString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The FlowMgr class responsible for creating and sending OpenFlow messages
 * to OpenFlow switches.
 */
public class FlowMgr
{
    private static final FlowMgr INSTANCE = new FlowMgr();
    private static final Logger LOG = LoggerFactory.getLogger(FlowMgr.class);

    private static IFloodlightProviderService mProvider;
    private static ITopologyService mTopology;

    public static final short PRIORITY_NORMAL = 10;
    public static final short PRIORITY_IP_PACKETS = 1000;
    public static final short PRIORITY_DNS_PACKETS = 2000;
    public static final short PRIORITY_IP_FLOWS = 1500;
    public static final short PRIORITY_ARP_PACKETS = 1500;

    public static final short IP_FLOW_IDLE_TIMEOUT = 15;
    public static final short NO_IDLE_TIMEOUT = 0;
    public static final int BUFFER_ID_NONE = 0xffffffff;

    public static final short DNS_QUERY_DEST_PORT = 53;

    //---------------------------------------------------------------------------------------------
    private FlowMgr()
    {
        // private constructor - prevent external instantiation
    }

    //---------------------------------------------------------------------------------------------
    public static FlowMgr getInstance()
    {
        return INSTANCE;
    }

    //---------------------------------------------------------------------------------------------
    public void init( final FloodlightModuleContext context )
    {
        LOG.debug("Initialize BlackList flow manager.");
        if (mProvider != null)
        {
            throw new RuntimeException("BlackList flow manager initialized");
        }

        mProvider = context.getServiceImpl(IFloodlightProviderService.class);
        mTopology = context.getServiceImpl(ITopologyService.class);
    }

    //---------------------------------------------------------------------------------------------
    public void setDefaultFlows(final IOFSwitch ofSwitch)
    {
        setDnsQueryFlow(ofSwitch);
        setIpFlow(ofSwitch);
        setArpFlow(ofSwitch);
        //!!!test
//        setNormalFlow(ofSwitch);
    }
    //---------------------------------------------------------------------------------------------
    public void sendPacketOut( final IOFSwitch         ofSwitch,
                               final FloodlightContext cntx,
                               final OFPacketIn        packetIn,
                               final List<OFAction>    actions)
    {
        if (mProvider == null) LOG.error("FlowMgr is not initialized yet.");

        final OFPacketOut packetOut = (OFPacketOut) mProvider.getOFMessageFactory().getMessage(OFType.PACKET_OUT);
        packetOut.setActions(actions);

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
        for (final OFAction action : actions) { actionsLength += action.getLengthU(); }
        packetOut.setActionsLength((short) actionsLength);

        // set buffer-id, in-port and packet-data based on packet-in
        short poLength = (short) (packetOut.getActionsLength() + OFPacketOut.MINIMUM_LENGTH);

        packetOut.setBufferId(packetIn.getBufferId());
        packetOut.setInPort(packetIn.getInPort());
        if (packetIn.getBufferId() == OFPacketOut.BUFFER_ID_NONE)
        {
            final byte[] packetData = packetIn.getPacketData();
            poLength += packetData.length;
            packetOut.setPacketData(packetData);
        }
        packetOut.setLength(poLength);

        try
        {
            LOG.trace( "Writing PacketOut switch={} packet-out={}", new Object[] { ofSwitch, packetOut } );

            ofSwitch.write(packetOut, cntx);
            ofSwitch.flush();
        }
        catch (final IOException e)
        {
            LOG.error("Failure writing PacketOut switch={} packet-out={}", new Object[] { ofSwitch, packetOut }, e);
        }
    }

    //---------------------------------------------------------------------------------------------
    public void dropPacket( final IOFSwitch         ofSwitch,
                            final FloodlightContext cntx,
                                  OFPacketIn        packetIn)
    {
        LOG.debug("Drop packet");

        final List<OFAction> flActions = new ArrayList<OFAction>();
        sendPacketOut( ofSwitch, cntx, packetIn, flActions );
    }

    //---------------------------------------------------------------------------------------------
    public void createDataStreamFlow( final IOFSwitch         ofSwitch,
                                      final FloodlightContext context,
                                      final OFPacketIn        packetIn,
                                             List<OFAction>   actions)
    {
        final OFMatch match = new OFMatch();
        match.loadFromPacket(packetIn.getPacketData(), packetIn.getInPort());

        //---- Ignore packet if it is an ARP, or has not source/dest, or is not IPv4.
        if (match.getDataLayerType()      == Ethernet.TYPE_ARP)   return;
        if( match.getNetworkDestination() == 0 )                  return;
        if( match.getNetworkSource()      == 0 )                  return;
        if( match.getDataLayerType()      != Ethernet.TYPE_IPv4 ) return;

        //---- Send the flow modifications for this specific IP destination address
        match.setWildcards(allExclude( OFMatch.OFPFW_NW_DST_MASK, OFMatch.OFPFW_DL_TYPE) );
        sendFlowModMessage( ofSwitch, OFFlowMod.OFPFC_ADD, match, actions,
                            PRIORITY_IP_FLOWS, IP_FLOW_IDLE_TIMEOUT, packetIn.getBufferId() );

    }

    //---------------------------------------------------------------------------------------------
    public void deleteIPFlowOnAllConnectedSwitches(InetAddress ipAddr)
    {
        final OFMatch match = new OFMatch();

        match.setWildcards( allExclude( OFMatch.OFPFW_NW_DST_MASK, OFMatch.OFPFW_DL_TYPE ) )
                           .setDataLayerType(Ethernet.TYPE_IPv4)
                           .setNetworkDestination( ByteBuffer.wrap( ipAddr.getAddress() ).getInt() );

        Map<Long,IOFSwitch> switches = mProvider.getSwitches();
        for( Map.Entry<Long,IOFSwitch> ofSwitchEntry : switches.entrySet() )
        {
            IOFSwitch ofSwitch = ofSwitchEntry.getValue();
            deleteFlow(ofSwitch, match);
        }

    }


    //---------------------------------------------------------------------------------------------
    private void deleteFlow( final IOFSwitch ofSwitch,
                             final OFMatch   match )
    {

        final List<OFAction> actions = new ArrayList<OFAction>();

        sendFlowModMessage( ofSwitch, OFFlowMod.OFPFC_DELETE, match, actions,
                            PRIORITY_IP_FLOWS, IP_FLOW_IDLE_TIMEOUT, BUFFER_ID_NONE);
    }



    //---------------------------------------------------------------------------------------------
   /**
     * Creates a OFPacketOut with the OFPacketIn data that is flooded on all
     * ports unless the port is blocked, in which case the packet will be
     * dropped.
     * @param ofSwitch
     *            Openflow switch context
     * @param context
     *            The FloodlightContext associated with this OFPacketIn
     * @param packetIn
     *            The OFPacketIn that came to the switch
     */
    public void floodPacket(final IOFSwitch ofSwitch,
                            final FloodlightContext context,
                            final OFPacketIn packetIn)
    {
        // Create action flood/all
        final List<OFAction> actions = new ArrayList<OFAction>();

        if (mTopology.isIncomingBroadcastAllowed(ofSwitch.getId(),
                                                 packetIn.getInPort()) == false)
        {
            LOG.debug("Drop broadcast packet, packetIn={}, " +
                      "from a blocked port, srcSwitch=[{},{}], linkInfo={}",
                      new Object[] { packetIn, ofSwitch.getId(),
                                     packetIn.getInPort() });
            // Drop the packet
            dropPacket(ofSwitch, context, packetIn);
            return;
        }

        if (ofSwitch.hasAttribute(IOFSwitch.PROP_SUPPORTS_OFPP_FLOOD))
        {
            actions.add(new OFActionOutput(OFPort.OFPP_FLOOD.getValue()));
        }
        else
        {
            actions.add(new OFActionOutput(OFPort.OFPP_ALL.getValue()));
        }

        sendPacketOut(ofSwitch, context, packetIn, actions);
    }

    //---------------------------------------------------------------------------------------------
    // Simplified/Modified version of method doForwardFlow in
    // net.floodlightcontroller.forwarding.Forwarding used to
    // create output action
    /**
     * Get output port
     *
     * @param ofSwitch
     *            Openflow switch context
     * @param context
     *            The FloodlightContext associated with this OFPacketIn
     * @param packetIn
     *            The OFPacketIn that came to the switch
     * @return output port
     */
    public short getOutputPort( final IOFSwitch ofSwitch,
                                final FloodlightContext context,
                                final OFPacketIn packetIn )
    {
        LOG.trace("Process packet {}", packetIn);

        final Ethernet eth = bcStore.get(context, CONTEXT_PI_PAYLOAD);
        if (eth.isBroadcast() || eth.isMulticast())
        {
            return OFPort.OFPP_FLOOD.getValue(); // For now we treat multicast as broadcast... Flood the packet
        }

        // Check if we have the location of the destination
        final IDevice dstDevice = fcStore.get(context, CONTEXT_DST_DEVICE);
        if (dstDevice == null)
        {
            // Flood the packet
            return OFPort.OFPP_FLOOD.getValue();
        }

        final IDevice srcDevice = fcStore.get(context, CONTEXT_SRC_DEVICE);
        if (srcDevice == null)
        {
            LOG.error("No device entry found for source device");
            return OFPort.OFPP_FLOOD.getValue();  // Flood the packet
        }

        final Long srcIsland = mTopology.getL2DomainId(ofSwitch.getId());
        if (srcIsland == null)
        {
            LOG.error("No openflow island found for source {}/{}",
                      HexString.toHexString(ofSwitch.getId()), packetIn.getInPort());
            return OFPort.OFPP_NONE.getValue();   // Drop the packet

        }

        // Validate that we have a destination known on the same island
        // Validate that the source and destination are not on the same switch port
        boolean sameIsland = false;
        boolean sameInterface = false;
        short dstPort = 0;
        for (final SwitchPort dstDap : dstDevice.getAttachmentPoints())
        {
            final long dstSwDpid = dstDap.getSwitchDPID();
            final Long dstIsland = mTopology.getL2DomainId( dstSwDpid );
            if( (dstIsland != null) && dstIsland.equals( srcIsland ) )
            {
                sameIsland = true;
                if( ofSwitch.getId() == dstSwDpid )
                {
                    if( packetIn.getInPort() == dstDap.getPort() ) sameInterface = true;
                    dstPort = (short) dstDap.getPort();
                }
                break;
            }
        }
        if( !sameIsland )
        {
            LOG.trace( "No first hop island found for destination " + "device {}, Action = flooding", dstDevice );
            return OFPort.OFPP_FLOOD.getValue(); // Flood since we don't know the dst device
        }
        if( sameInterface )
        {
            LOG.trace( "Both source and destination are on the same " +
                       "switch/port {}/{}, Action = NOP", ofSwitch.toString(), packetIn.getInPort() );
            return OFPort.OFPP_NONE.getValue();  // Drop the packet
        }

        if( dstPort == 0 )
        {
            return OFPort.OFPP_NONE.getValue();  // Flood the packet
        }

        return dstPort;
    }

    //---------------------------------------------------------------------------------------------
    private static int allExclude(final int... flags)
    {
        int wc = OFPFW_ALL;
        for( final int f : flags ) { wc &= ~f; }

        return wc;
    }

    //---------------------------------------------------------------------------------------------
    private void sendFlowModMessage( final IOFSwitch      ofSwitch,
                                     final short          command,
                                     final OFMatch        ofMatch,
                                     final List<OFAction> actions,
                                     final short          priority,
                                     final short          idleTimeout,
                                     final int            bufferId )
    {
        if (mProvider == null)
        {
            LOG.error("FlowMgr is not initialized yet.");
            return;
        }

        final OFFlowMod ofm = (OFFlowMod) mProvider.getOFMessageFactory().getMessage(OFType.FLOW_MOD);
        ofm.setCommand(     command)
           .setIdleTimeout( idleTimeout )
           .setPriority(    priority )
           .setMatch(       ofMatch.clone() )
           .setBufferId(    bufferId )
           .setOutPort(     OFPort.OFPP_NONE )
           .setActions(     actions )
           .setXid(         ofSwitch.getNextTransactionId() );

        /* If application need to be notified about removed/expired flows, for
         * example to collect flow statistics, then setup SEND_FLOW_REM flag.
         * .setFlags(OFFlowMod.OFPFF_SEND_FLOW_REM)
         */

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
        for( final OFAction action : actions ) { actionsLength += action.getLengthU(); }

        ofm.setLengthU(OFFlowMod.MINIMUM_LENGTH + actionsLength);

        try
        {
            ofSwitch.write(ofm, null);
            ofSwitch.flush();

            LOG.info("Flow {} is set on switch {}", ofm, ofSwitch.getId());
        }
        catch (final IOException e)
        {
            LOG.error("Unable to set flow " + ofm + " on switch {} err: {}", ofSwitch.getId(), e);
        }
    }

    //---------------------------------------------------------------------------------------------
    /**
     * Set up DNS query flow on provided device.
     * The flow forwards DNS query packets to the controller.
     *
     * @param  ofSwitch - OpenFlow switch
     */
    private void setDnsQueryFlow(final IOFSwitch ofSwitch)
    {
        /* fill in match object */
        OFMatch ofMatch = new OFMatch();
        ofMatch.setWildcards( allExclude( OFPFW_TP_DST, OFPFW_NW_PROTO, OFPFW_DL_TYPE ) )
               .setDataLayerType(        Ethernet.TYPE_IPv4 )
               .setNetworkProtocol(      IPv4.PROTOCOL_UDP )
               .setTransportDestination( DNS_QUERY_DEST_PORT );

        //---- Create output action to send packets to the controller
        OFActionOutput ofAction  = new OFActionOutput(OFPort.OFPP_CONTROLLER.getValue(), (short) 65535);
        List<OFAction> ofActions = new ArrayList<OFAction>();
        ofActions.add(ofAction);

        getInstance().sendFlowModMessage( ofSwitch, OFFlowMod.OFPFC_ADD, ofMatch, ofActions,
                                          PRIORITY_DNS_PACKETS, NO_IDLE_TIMEOUT, BUFFER_ID_NONE );
    }

    //---------------------------------------------------------------------------------------------
    /**
     * Set up IP flow on provided device.
     * The flow forwards all IP packets to the controller.
     *
     * @param  ofSwitch - OpenFlow switch
     */
    private void setIpFlow(final IOFSwitch ofSwitch)
    {
        //---- Create match object to only match all IPv4 packets
        OFMatch ofMatch = new OFMatch();
        ofMatch.setWildcards( allExclude( OFPFW_DL_TYPE ) )
               .setDataLayerType( Ethernet.TYPE_IPv4 );

        //---- Create output action "controller"
        OFActionOutput ofAction  = new OFActionOutput(OFPort.OFPP_CONTROLLER.getValue(), (short) 65535);
        List<OFAction> ofActions = new ArrayList<OFAction>();
        ofActions.add(ofAction);

        sendFlowModMessage( ofSwitch, OFFlowMod.OFPFC_ADD, ofMatch, ofActions,
                            PRIORITY_IP_PACKETS, NO_IDLE_TIMEOUT, BUFFER_ID_NONE );
    }

    //---------------------------------------------------------------------------------------------
    private void setNormalFlow(final IOFSwitch ofSwitch)
    {
        //---- Create match object to match everything
        OFMatch ofMatch = new OFMatch();
        ofMatch.setWildcards(allExclude());

        //---- Create output action to forward normally
        OFActionOutput ofAction  = new OFActionOutput(OFPort.OFPP_NORMAL.getValue(), (short) 65535);
        List<OFAction> ofActions = new ArrayList<OFAction>();
        ofActions.add(ofAction);

        sendFlowModMessage( ofSwitch, OFFlowMod.OFPFC_ADD, ofMatch, ofActions, PRIORITY_NORMAL, NO_IDLE_TIMEOUT, BUFFER_ID_NONE );
    }

    //---------------------------------------------------------------------------------------------
    private void setArpFlow(final IOFSwitch ofSwitch)
    {
        //---- Create match object match arp packets
        OFMatch ofMatch = new OFMatch();
        ofMatch.setWildcards( allExclude(OFPFW_DL_TYPE) ).setDataLayerType( Ethernet.TYPE_ARP );

        //---- Create output action to forward normally
        OFActionOutput ofAction  = new OFActionOutput(OFPort.OFPP_NORMAL.getValue(), (short) 65535);
        List<OFAction> ofActions = new ArrayList<OFAction>();
        ofActions.add(ofAction);

        sendFlowModMessage( ofSwitch, OFFlowMod.OFPFC_ADD, ofMatch, ofActions, PRIORITY_ARP_PACKETS, NO_IDLE_TIMEOUT, BUFFER_ID_NONE );
    }

}
