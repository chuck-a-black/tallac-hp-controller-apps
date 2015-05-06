/*
 * Copyright (c) 2012, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.nac.api;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import net.floodlightcontroller.util.MACAddress;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tallac.nac.module.ClientMgr;
import com.tallac.nac.module.ClientMgr.Client;
import com.tallac.nac.module.ClientMgr.ClientState;
import com.tallac.nac.utils.TallacUtils;

public class NacUserResource extends ServerResource {
	private static final Logger LOG = LoggerFactory.getLogger(NacUserResource.class);

    static class User 
    {
        public String mac;
        public String ip;
        public String state = null;
        public String details = null;
        public String id;
        
        public User(){}
        public User(@JsonProperty("mac")
                    String iMac, 
                    @JsonProperty("ip")
                    String iIp, 
                    @JsonProperty("state")
                    @JsonSerialize(include = Inclusion.NON_NULL)
                    String iState, 
                    @JsonSerialize(include = Inclusion.NON_NULL)
                    @JsonProperty("details")
                    String iDetails)
        {
            id = mac = iMac;
            ip = iIp;
            state = iState;
            details = iDetails;
        }

        @Override
        public String toString() {
            return "User [mac=" + mac + ", ip=" + ip + ", state=" + state + ", details=" + details + "]";
        }

    }

    //---- Put:  accept, change user information
	@Put
	public User accept( User user ) {
		
		ClientState state;
		String      details;

		LOG.debug( "Received REST PUT request to modify user: " + user );
		
		//---- Make sure the user's MAC address was sent to us correctly
		MACAddress macAddr = null;
		try { macAddr = MACAddress.valueOf( user.mac ); }
		catch (IllegalArgumentException e)
		{
            TallacUtils.setRestApiError( String.format("Unable to parse MAC address [%s].", user.mac) );
            return null;
		}
		
		//---- Get the client referenced by the MAC address
		Client client = ClientMgr.getInstance().getClient( macAddr );
		if (client == null)
		{
            TallacUtils.setRestApiError( String.format("Unknown user MAC address [%s].", user.mac) );
            return null;
		}

		//----  Found the client, now set existing values for state and details.
	    state   = client.getState();   
	    details = client.getDetails();
		
		//---- Check if user's new state has been passed to us, if not we have the existing state
		if( user.state != null )
		{
		    state = ClientMgr.ClientState.parseState( user.state );
	        if (state == null)
	        {
	            TallacUtils.setRestApiError( String.format("Unknown state [%s].", user.state) );
	            return null;
	        }
		}
		
		//---- Check if description is modified, if not we have the existing details
        if (user.details != null)
        {
            details = user.details;
        }
		
		//---- Now update the client itself
		ClientMgr.getInstance().updateClient( macAddr, state, details );
        //-- this was a test only:  ClientMgr.getInstance().removeClientFlows( macAddr );
		
		Response.getCurrent().setStatus( Status.SUCCESS_OK );
		return user;
	}

	@Get("json")
	public List<User> retrieve() 
	{
        String macStr = getQuery().getFirstValue("mac");
        String ipStr = getQuery().getFirstValue("ip");
        LOG.debug("Received REST GET clients info request. MAC: {} IP: {}", macStr, ipStr);
        
        if (macStr != null && ipStr != null)
        {
            TallacUtils.setRestApiError( "Parameters [mac] and [ip] are mutually exclusive." );
            return null;
        }
        
        int ipInt = 0;
        if (ipStr != null)
        {
            InetAddress ipAddr = TallacUtils.convertIpv4Address(ipStr);
            if (ipAddr == null)
            {
                TallacUtils.setRestApiError( String.format( "Unable to parse IP address %s.", ipStr ) );
                return null;
            }
            ipInt = TallacUtils.inetAddressToInt(ipAddr);
        }
		
		List<User> retVal = new ArrayList<User>();

		//Add records matching target MAC *AND* IPv4 address in the output list 
		for (Client cl : ClientMgr.getInstance().getClients())
		{
		    if ((macStr == null || cl.getMacAddr().toString().equalsIgnoreCase(macStr)) && 
                (ipInt == 0 || cl.getIpAddr() == ipInt))
		    {
		        retVal.add( new User( cl.getMacAddr().toString(),
		                              TallacUtils.intIpToString(cl.getIpAddr()), 
		                              cl.getState().name(), 
		                              cl.getDetails() ) );
		    }
		}

		return retVal;
	}
}
