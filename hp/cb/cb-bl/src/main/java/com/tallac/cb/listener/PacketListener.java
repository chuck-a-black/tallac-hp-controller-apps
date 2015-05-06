package com.tallac.cb.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.of.ctl.ControllerService;
import com.hp.of.ctl.ErrorEvent;
import com.hp.of.ctl.pkt.MessageContext;
import com.hp.of.ctl.pkt.PacketListenerRole;
import com.hp.of.ctl.pkt.SequencedPacketListener;
import com.hp.of.lib.msg.OfmPacketIn;
import com.hp.util.pkt.Codec;
import com.hp.util.pkt.Packet;
import com.hp.util.pkt.ProtocolId;
import com.tallac.cb.handler.DnsPacketHandler;
import com.tallac.cb.handler.IpPacketHandler;

//=================================================================================================
public class PacketListener implements SequencedPacketListener{

	private static final Logger LOG = LoggerFactory.getLogger( PacketListener.class );
	
	private volatile ControllerService mControllerService;

	//----------------------------------------------------------------------------------------
	public void init( ControllerService controllerService ) {

		LOG.info( "MyBlacklist: PacketListener: init()" );
		mControllerService = controllerService;
		
	}

	//----------------------------------------------------------------------------------------
	public void startup() {
		
		LOG.info( "MyBlacklist: PacketListener: startup()" );
		mControllerService.addPacketListener( this, PacketListenerRole.DIRECTOR, 60000 );
	}

	//----------------------------------------------------------------------------------------
	public void shutdown() {
		
		LOG.info( "MyBlacklist: PacketListener: shutdown()" );
		mControllerService.removePacketListener( this );
	}

	//----------------------------------------------------------------------------------------
	@Override
	public boolean event( MessageContext messageContext ) {

//		LOG.info( "MyBlacklist: PacketListener: event(): messageContext={}", messageContext );
		
		OfmPacketIn ofPacketIn = (OfmPacketIn)messageContext.srcEvent().msg();
		Packet      packetInData = Codec.decodeEthernet( ofPacketIn.getData() );

		LOG.info( "MyBlacklist: PacketListener: event(): ofPacketIn={}", ofPacketIn );
//		LOG.info( "MyBlacklist: PacketListener: event(): packetInData={}", packetInData );
		
		if( packetInData.has( ProtocolId.ARP ) ) {
//			LOG.info( "MyBlacklist: PacketListener: event(): received ARP" );
		}
//		else if( packetInData.has( ProtocolId.ICMP ) ) {
//			LOG.info( "MyBlacklist: PacketListener: event(): received ICMP" );
//		}
		else if( packetInData.has( ProtocolId.DNS ) ) {
			LOG.info( "MyBlacklist: PacketListener: event(): received DNS" );
			
			DnsPacketHandler dnsPacketHandler = new DnsPacketHandler();
			boolean handled = dnsPacketHandler.handle( messageContext, ofPacketIn );
			
			return handled;
		}
		else if( packetInData.has( ProtocolId.IP ) ) {
			LOG.info( "MyBlacklist: PacketListener: event(): received IP" );
			
			IpPacketHandler ipPacketHandler = new IpPacketHandler( mControllerService );
			boolean handled = ipPacketHandler.handle( messageContext, packetInData );
			
			return handled;
		}

		return false;
	}

	//----------------------------------------------------------------------------------------
	@Override
	public void errorEvent(ErrorEvent arg0) {
		
	}

}
