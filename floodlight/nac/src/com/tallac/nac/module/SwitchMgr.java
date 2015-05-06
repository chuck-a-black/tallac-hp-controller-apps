/*
 * Copyright (c) 2013, Tallac Networks
 * All Rights Reserved.
 */

package com.tallac.nac.module;

import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.OFSwitchImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tallac.nac.utils.TallacUtils;

//---- SwitchMgr class, singleton that holds map of all discovered switches, each with their attributes (e.g. uplink port)
public class SwitchMgr {
    
    private static final SwitchMgr INSTANCE = new SwitchMgr();
    private static final Logger    LOG      = LoggerFactory.getLogger( SwitchMgr.class );

    //---- Inner Switch class that holds info about a switch
    public static class NacSwitch {
    	
    	private long      mSwitchId;
    	private int       mSwitchIp;
    	private short     mUplinkPortNumber;
    	
    	public NacSwitch( IOFSwitch ofSwitch ) {
    		
    		mSwitchId  = ofSwitch.getId();
    		InetSocketAddress  socket = (InetSocketAddress)((OFSwitchImpl)ofSwitch).getInetAddress();
    		InetAddress   inetAddress = socket.getAddress();
    		
    		mSwitchIp  = TallacUtils.inetAddressToInt( inetAddress );
    		
    		LOG.info( "creating Switch:  {}", ofSwitch );
    		
    		// Read in the uplink port number from the properties file.
    		mUplinkPortNumber = ConfigMgr.getInstance().getShort( "nac.uplinkPort." + TallacUtils.intIpToString( mSwitchIp ) );
    		
    		LOG.info( "creating Switch: setting uplink port={}", mUplinkPortNumber );
    		
     	}
    	
    	public short getUplinkPortNumber()                   { return mUplinkPortNumber; }
    	public void  setUPlinkPortNumber( short uplinkPort ) { mUplinkPortNumber = uplinkPort; }
    	
    	public boolean isInfrastructurePort( short port ) { return false; }
    	public boolean isEdgePort(           short port ) { return true; }
    	
    	public long  getSwitchId() { return mSwitchId; }
    }

    //---- SwitchMgr main data structure - a hashmap of switches, indexed by switch IP address
    private ConcurrentHashMap< Long, NacSwitch > mSwitchMap;

    //---- Private constructor for SwitchMgr, prevents external instantiation
    private SwitchMgr() 
    {
        mSwitchMap = new ConcurrentHashMap< Long, NacSwitch >();
    }

    public static SwitchMgr getInstance() { return INSTANCE; } 

    //---- Add a switch to our hashmap of all known switches
    public void addSwitch( IOFSwitch ofSwitch ) {
    	
    	if( mSwitchMap.get( ofSwitch.getId() ) != null ) {
    		LOG.info( "SwitchMgr addSwitch: Switch already exists: {}", ofSwitch );
    		return;
    	}
    	
    	mSwitchMap.put( ofSwitch.getId(), new NacSwitch( ofSwitch ) );
    	LOG.info( "SwitchMgr addSwitch: Added switch {}", ofSwitch );
    }
 
    //---- Remove the switch from our hashmap of all switches
    public void removeSwitch( IOFSwitch ofSwitch ) {
    	
    	if( mSwitchMap.remove( ofSwitch.getId() ) == null ) LOG.info( "SwitchMgr removeSwitch: Switch doesn't exist: {}", ofSwitch );
    	else                                                LOG.info( "SwitchMgr removeSwitch: Removed switch {}", ofSwitch );

    }

    //---- Return the NacSwitch object that matches the given ID
    public NacSwitch getSwitch( Long dpId ) { return mSwitchMap.get( dpId ); }
}
