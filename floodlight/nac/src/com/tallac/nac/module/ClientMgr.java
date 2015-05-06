/*
 * Copyright (c) 2012, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.nac.module;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.util.MACAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tallac.nac.utils.TallacUtils;

public class ClientMgr {

    private static final ClientMgr INSTANCE = new ClientMgr();
    private static final Logger    LOG      = LoggerFactory.getLogger(ClientMgr.class);

    public static enum ClientState {
        GUEST, AUTHENTICATED, UNAUTHENTICATED;

        public static ClientState parseState(String state) 
        {
            for( ClientState clientState:ClientState.values() ) if( clientState.name().equalsIgnoreCase(state.trim()) ) return clientState;
            return null;
        }
    };

    //---- Inner class 'Client', holds all information about a single client
    public static class Client 
    {
        private MACAddress  mMac;
        private int         mIpAddr;
        private ClientState mClientState;
        private String      mDetails;
        
        private long        mSwitchId;
        private short       mSwitchPort;
        
        //---- Client:  Constructors
        public Client( MACAddress mac, InetAddress inetIpAddr, ClientState clientState, String details, long ofSwitchId, short ofSwitchPort ) 
        {
        	this( mac, TallacUtils.inetAddressToInt(inetIpAddr), clientState, details, ofSwitchId, ofSwitchPort ); 
        }

        public Client( MACAddress mac, int ip, ClientState clientState, String details ) 
        {
        	this( mac, ip, clientState, details, (long)(-1), (short)(-1) );
        }
        
        public Client( MACAddress mac, int ip, ClientState clientState, String details, long ofSwitchId, short ofSwitchPort ) 
        {
            mMac         = mac;
            mIpAddr      = ip;
            mClientState = clientState;
            
            if( mDetails == null || mDetails.isEmpty() ) mDetails = "unknown";
            mDetails     = details;
            
            mSwitchId   = ofSwitchId;
            mSwitchPort = ofSwitchPort;
        }
       
        //---- Update client
        public void update( int ip, ClientState clientState, String details, long ofSwitchId, short ofSwitchPort )
        {
        	mSwitchId   = ofSwitchId;
        	mSwitchPort = ofSwitchPort;
        	
        	update( ip, clientState, details );
        }
        
       //---- Update client
        public void update( int ip, ClientState clientState, String details )
        {
        	if( ip != 0 ) mIpAddr = ip;  // Update the ip address only if the value passed is non-zero
        	
        	mClientState = clientState;
        	mDetails     = details;
        	
        	String ipAddrString = TallacUtils.intIpToString( ip );
        	LogMgr.getInstance().log( this, String.format( "Client updated:  ip:[%s], state:[%s], details:[%s]", ipAddrString, clientState, details ) );
        }

        public void setIpAddr(int ipAddr)  { mIpAddr = ipAddr; }
        public int  getIpAddr()            { return mIpAddr; }
        
        public void        setState(ClientState clientState) { mClientState = clientState; }
        public ClientState getState()                        { return mClientState; }
        
        public void setDetails(String details)        { mDetails = details; }
        public String getDetails()                    { return mDetails; }
        
        public MACAddress getMacAddr()                { return mMac; }
        
        public void setSwitchId(   long ofSwitchId )    { mSwitchId   = ofSwitchId; }
        public void setSwitchPort( short ofSwitchPort ) { mSwitchPort = ofSwitchPort; }
        public long  getSwitchId()                      { return mSwitchId; }
        public short getSwitchPort()                    { return mSwitchPort; }
        
        @Override
        public String toString() {
            return "Client [mMac=" + mMac + ", mIpAddr=" + TallacUtils.intIpToString(mIpAddr) + ", mClientState=" + mClientState + ", mDetails=" + mDetails + "]";
        }
    }

    private ConcurrentHashMap<MACAddress, Client> mClients;

    //---- Constructor:  ClientMgr private constructor to prevent external instantiation.
    private ClientMgr() { 
    	mClients = new ConcurrentHashMap<MACAddress, Client>();
    }

    //---- getInstance: Normal getInstance method to return reference to this singleton
    public static ClientMgr getInstance() { return INSTANCE; }

    //---- init: Nothing going on here, maybe remove?
    public void init()  
    {
    }

    //---- startUp:  Nothing going on here, maybe remove?
    public void startUp() 
    {
    }
           
    //---- getClients:  Returns a list of all the client objects.
    public ArrayList<Client> getClients() 
    {
        return new ArrayList<Client>( mClients.values() );
    }

    //---- getClient:  Returns the client given the MAC address if found, null otherwise.
    public Client getClient( MACAddress mac )
    {
    	return mClients.get( mac );    	
    }

    //---- getClient:  Returns the client given the IP address if found, null otherwise.
    public Client getClient( InetAddress clientIp )
    {
        for( Client client : mClients.values() ) if( client.getIpAddr() == TallacUtils.inetAddressToInt( clientIp) ) return client; 
        return null;
    }
    
    //---- createClient:  create Client object and add it to the list of Clients
    public Client createClient( MACAddress   mac, 
    		                    int          ipAddr, 
    		                    ClientState  clientState, 
    		                    String       details, 
    		                    long         ofSwitchId,
    		                    short        ofSwitchPort ) 
    {
    	if ( getClient( mac ) != null ) return null;
    	
    	Client client = new Client( mac, ipAddr, clientState, details, ofSwitchId, ofSwitchPort );
    	mClients.put( mac, client );

    	LogMgr.getInstance().log( client, String.format( "Client created:  mac:[%s], ip:[%d]", mac.toString(), ipAddr ) );
    	
    	return client;
    }
    
    /**
     * Add or replace client's data in the list of clients
     * @param mac     - MAC address
     * @param state   - client state
     * @param details - client description
     */
    public Client updateClient( MACAddress  mac, 
    		                    ClientState state, 
    		                    String      details ) 
    {
        Client client = mClients.get(mac);  // Find the client
        
        if( client == null )  // If the client is not there, log error and return.
        {
            LOG.info("updateClient: cannot find Client {}", mac.toString() );
            return null;
        }
        
        //---- Handle state changes
        ClientState prevState = client.getState();
        if( state != prevState )  // If the state is changing, log the information
        {                
            client.setState(state);       // Change client state to the new value
            LogMgr.getInstance().log(client, String.format( "Client state changed from [%s] to [%s].", prevState, state ) );
        }
        
        //---- Handle details changes
        String prevDetails = client.getDetails();
        if( !details.equals( prevDetails ) )
        {                
            client.setDetails(details);                // set new client description
            LogMgr.getInstance().log(client, String.format( "Client details changed from [%s] to [%s].", 
                            (prevDetails == null ? "" : prevDetails), (details == null ? "" : details)));
        }
        
        //---- Handle setting up the flows for allowed client
        if( client.getState() == ClientState.AUTHENTICATED || client.getState() == ClientState.GUEST )  // If user is now allowed onto network...
        {          
            FlowMgr.getInstance().removeUnauthFlowsOnAllSwitches( client.getMacAddr() );   // First remove Unauth flows on switches
            FlowMgr.getInstance().setAuthFlowsOnAllSwitches( client.getMacAddr() );        // Next, set Auth flows onto switches.
         }
        
        //---- ... else handle setting up the flows for unauthenticated client
        else if( client.getState() == ClientState.UNAUTHENTICATED )
        {
            FlowMgr.getInstance().removeAuthFlowsOnAllSwitches( client.getMacAddr() );  // First remove Auth flows on switches
            FlowMgr.getInstance().setUnauthFlowsOnAllSwitches( mac );                   // Next, set Unauth flows on switches
        }

        String sIpAddr = TallacUtils.intIpToString( client.getIpAddr() );
    	LogMgr.getInstance().log( client, String.format( "Client updated:  mac:[%s], ip:[%s], state:[%s], details:[%s]", mac.toString(), sIpAddr, state, details ) );
    	
        return client;    
    }

    //---- removeClientFlows:  remove the authentication flows that exist for this client (helps with testing)
    public void removeClientFlows( MACAddress  mac ) 
    {
    	Client client = mClients.get(mac);  // Find the client

    	if( client == null )  // If the client is not there, log error and return.
    	{
    		LOG.info("removeClientFlows: cannot find Client {}", mac.toString() );
    		return;
    	}

   		FlowMgr.getInstance().removeAuthFlowsOnAllSwitches( client.getMacAddr() );  // Remove Auth flows on switches
    }

    
    
    
    /**
     * Set up clients' flows on target switch
     * @param ofSwitch - target switch
     */
    public void setClientsFlows( IOFSwitch ofSwitch ) 
    {
        //---- Go through all clients and add appropriate flows depending on the client's state.  Only relevant if we are pushing all client flows to all switches.
        for( Client client : mClients.values() )
        {
        	switch( client.getState() )
        	{
        		case GUEST:
        		case AUTHENTICATED: {
        			FlowMgr.getInstance().setAuthFlowsOnSwitch( ofSwitch, client.getMacAddr() );
        			return;
        		}
        		case UNAUTHENTICATED: {
                	FlowMgr.getInstance().setUnauthFlowsOnSwitch( ofSwitch, client.getMacAddr() );
        			return;
        		}
        	}
        }
    }
}
