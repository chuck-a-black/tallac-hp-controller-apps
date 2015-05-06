package com.tallac.blacklist.listener;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.of.ctl.ControllerService;
import com.hp.of.ctl.ErrorEvent;
import com.hp.of.ctl.pkt.MessageContext;
import com.hp.of.ctl.pkt.PacketListenerRole;
import com.hp.of.ctl.pkt.SequencedPacketListener;
import com.hp.of.lib.ProtocolVersion;
import com.hp.of.lib.instr.Action;
import com.hp.of.lib.instr.ActionFactory;
import com.hp.of.lib.instr.ActionType;
import com.hp.of.lib.msg.OfmPacketIn;
import com.hp.of.lib.msg.Port;
import com.hp.util.pkt.Codec;
import com.hp.util.pkt.Packet;
import com.hp.util.pkt.ProtocolId;
import com.tallac.blacklist.handler.DnsPacketHandler;
import com.tallac.blacklist.handler.IpPacketHandler;

public class PacketListener implements SequencedPacketListener {
	
	private static final int ALTITUDE = 10000;
    private static final Set<ProtocolId> PROTOCOLS = new HashSet<ProtocolId>();
    static {
        PROTOCOLS.add(ProtocolId.IP);
    }
    
    private static ControllerService mControllerService;

    private static final Logger LOG = LoggerFactory.getLogger( PacketListener.class );
    private static final String LOGPREFACE = "[Blacklist: PacketListener]: ";        

    //---------------------------------------------------------------------------------------------
    public void init( final ControllerService controllerService )
    {
        mControllerService = controllerService;
        LOG.info( LOGPREFACE + "init():  BlackList packet listener initialized.");       
  }

    //---------------------------------------------------------------------------------------------
    public void startUp()
    {
    	mControllerService.addPacketListener( this, PacketListenerRole.DIRECTOR, ALTITUDE, PROTOCOLS );
    	LOG.info( LOGPREFACE + "startUp(): OpenFlow packet listener registered." );
    }

    //---------------------------------------------------------------------------------------------
    public void shutDown()
    {
    	mControllerService.removePacketListener( this );
    	LOG.info( LOGPREFACE + "shutDown(): OpenFlow packet listener unregistered." );
    }

    //---------------------------------------------------------------------------------------------
    @Override
    public boolean event( MessageContext messageContext ) {
    	
        // Get incoming packet from switch, and the packet data as well
        OfmPacketIn packetIn = (OfmPacketIn)messageContext.srcEvent().msg();
        Packet packetInData  = Codec.decodeEthernet( packetIn.getData() );

        // Exclude any non-IPv4 packets
        if( !packetInData.has( ProtocolId.IP ) ) {
        	LOG.info( LOGPREFACE + "event(): Received non-IP packet, ignoring." );
        	return false;
        }
        
        //---- Handle DNS request here --------------------     
        if( packetInData.has( ProtocolId.DNS ) ) {

        	DnsPacketHandler dnsPacketHandler = new DnsPacketHandler(); // Create handler for this DNS request.
        	
        	// Invoke packet handler to determine if this DNS request should be allowed.
        	if( dnsPacketHandler.allowDnsRequest( messageContext, packetIn ) ) {

        		LOG.trace( LOGPREFACE + "event(): allowing DNS request to be forwarded" );
        		
        		// If allowed, add a 'forward normal' action to the outgoing packet.
            	Action normalAction = ActionFactory.createAction( ProtocolVersion.V_1_0, ActionType.OUTPUT, Port.NORMAL  );        		
            	messageContext.packetOut().addAction( normalAction );
        	}
        	else {
        		// Otherwise, we just don't add an action - this will cause it to be dropped.
        		LOG.info( LOGPREFACE + "event(): bad DNS requested, dropping packet." );        		
        	}

        	return true;
        }
        
        //---- Handle regular IP requests here ------------
        else {

        	IpPacketHandler ipPacketHandler = new IpPacketHandler( mControllerService );
        	
        	// Invoke packet handler to determine if this IP destination address should be allowed.
        	if( ipPacketHandler.allowIpDestination( messageContext, packetInData ) ) {

        		LOG.trace( LOGPREFACE + "event(): allowing IP request to be forwarded" );
        		
        		// If allowed, ad a 'forward normal' action to the outgoing packet.
            	Action normalAction = ActionFactory.createAction( ProtocolVersion.V_1_0, ActionType.OUTPUT, Port.NORMAL  );
            	messageContext.packetOut().addAction( normalAction );
 
            	// Tell the packet handler to install appropriate flows.
            	ipPacketHandler.installIpFlowsForDestination( messageContext.srcEvent().dpid() );
            	
        	}
        	else {
        		// Otherwise, we drop the packet by adding no action
        		LOG.info( LOGPREFACE + "event(): bad IP destination address, dropping packet." );
        	}

        	return true;
        }
/*
        // Inspect TCP port... We are interested in default HTTP port 80

        // only
        Tcp tcp = (Tcp) piData.get(ProtocolId.TCP);
        if (tcp.dstPort().getNumber() == 80) {
            // We have HTTP data!
            if (piData.size() >= 4) {
                String http = ByteUtils
                    .getStringFromRawBytes(((UnknownProtocol) piData
                    .get(3)).bytes());
                // Post alert
//                post(http, piData);
                // Allows downstream path-planner to push reactive flow
                return false;
            }
            // Push PACKET_OUT to destination switch, by-passing
            // downstream PathDaemon.
            return packetOut(piData, context);
        }

        // Any non-HTTP protocol, allow path-planner to take care of
        return false;
 */
 }
    
    @Override
    public void errorEvent(ErrorEvent event) {
    	
        LOG.error( LOGPREFACE + "errorEvent(): Received error event: " + event.text() );
        
    }

/*
    // Push PACKET_OUT
    private boolean packetOut(Packet piData, MessageContext context) {
        BigPortNumber outPort = null;

        // Get destination dpid from destination host's mac
        Ethernet eth = (Ethernet) piData.get(ProtocolId.ETHERNET);
        NetworkNode dstHost = nodeService.getNetworkNode(eth.dstAddr(),
                                                VId.valueOf(eth.vlanId()));
        DataPathId dstDpid = dstHost.connectedSwitchDpid();

        // Get source dpid
        DataPathId srcDpid = context.srcEvent().dpid();

        // Are we on the same datapath?
        if (srcDpid.equals(dstDpid)) {
            // Output to port that destination host is connected to
            outPort = dstHost.connectedPort();
        } else {
            // Output to port that is connected to destination dpid
            L2Path shortestPath = topologyService.path(srcDpid, dstDpid);
            Link link = nextHop(shortestPath, srcDpid);
            outPort = link.src().port();
        }

        // Push this packet_out now!
        context.packetOut()
            .addAction(ActionFactory.createAction(context.getVersion(),
                                                  ActionType.OUTPUT,
                                                  outPort));

        logger.info("packet_out on {} port {} ", srcDpid, outPort.toLong());
        return true;
    }

    // Get the next hop in the given path, starting with the srcDpid
    private Link nextHop(L2Path path, DataPathId srcDpid) {
        for (Link link : path.path())
            if (link.src().device().equals(srcDpid))
                return link;
        return null;
    }

*/
    
}
