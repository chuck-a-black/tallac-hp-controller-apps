//------------------------------------------------------------------------------
//  (c) Copyright 2013 Hewlett-Packard Development Company, L.P.
//
//  Confidential computer software. Valid license from HP required for 
//  possession, use or copying. 
//
//  Consistent with FAR 12.211 and 12.212, Commercial Computer Software,
//  Computer Software Documentation, and Technical Data for Commercial Items
//  are licensed to the U.S. Government under vendor's standard commercial 
//  license.
//------------------------------------------------------------------------------
package com.tallac.blacklist.impl;


import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.of.ctl.ControllerService;
import com.hp.of.lib.ProtocolVersion;

import com.tallac.blacklist.api.BlacklistService;
import com.tallac.blacklist.listener.CtlMessageListener;
import com.tallac.blacklist.listener.PacketListener;
import com.tallac.blacklist.listener.SwitchListener;
import com.tallac.blacklist.manager.BlacklistedHostsManager;
import com.tallac.blacklist.manager.SwitchManager;

/**
 * OSGi component used to publish {@link OpenFlowMonitor} so it is available
 * to be consumed by other components.
 */
@Component
@Service
public class BlacklistComponent implements BlacklistService {

    public static final ProtocolVersion OF_VERSION = ProtocolVersion.V_1_0;
    
    public static final int  PRI_ARP_FLOW        = 32200;
    public static final int  PRI_DNS_FLOW        = 32200;
    public static final int  PRI_IP_FLOW         = 31200;
    public static final int  PRI_IP_DEFAULT_FLOW = 31100;

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private volatile ControllerService controllerService;

    // Listeners
    private PacketListener      packetListener;
    private CtlMessageListener  messageListener;
    private SwitchListener      switchListener;
    
    private volatile boolean blacklistEnabled = false;
    
    private static final Logger LOG = LoggerFactory.getLogger( BlacklistComponent.class );
    private static final String LOGPREFACE = "[Blacklist: BlacklistComponent]: ";

    @Activate
    protected void activate() {
    	
        //---- Listeners ------------------------------------------------------
        
        // Create, initialize, and do some startup for our switch listener.
        switchListener = new SwitchListener();
        switchListener.init( controllerService );
        switchListener.startUp();
        LOG.info( LOGPREFACE + "activate(): Switch Listener created" );
        
        // Create, initialize, and do some startup for our message listener.
        messageListener = new CtlMessageListener();
        messageListener.init( controllerService );
        messageListener.startUp();
		LOG.info( LOGPREFACE + "activate(): Message Listener created" );
        
        // Create, initialize, and do some startup for our packet listener.
        packetListener = new PacketListener();
        packetListener.init( controllerService );
        packetListener.startUp();        
		LOG.info( LOGPREFACE + "activate(): Packet Listener created" );
		
		//---- Managers -------------------------------------------------------
		
		SwitchManager.getInstance().init( controllerService );
		LOG.info( LOGPREFACE + "activate(): Switch Manager created." );
		
		BlacklistedHostsManager.getInstance().init();
		LOG.info( LOGPREFACE + "activate(): Blacklisted Hosts Manager created." );
        
    }

    @Deactivate
    protected void deactivate() {
    	LOG.info( LOGPREFACE + "deactivate(): shutting down all listeners and setting enabled flag to false.");
    	
    	packetListener.shutDown();
    	messageListener.shutDown();
    	switchListener.shutDown();
    	
        blacklistEnabled = false;
    }


    @Override
    public void enableBlacklist() {
    	blacklistEnabled = true;
    }

    @Override
    public boolean isBlacklistEnabled() {
        return blacklistEnabled;
    }

    @Override
    public void disableBlacklist() {
    }
    
}
