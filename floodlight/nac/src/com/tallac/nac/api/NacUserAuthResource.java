/*
 * Copyright (c) 2012, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.nac.api;

import java.net.InetAddress;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tallac.nac.module.ClientMgr;
import com.tallac.nac.module.ClientMgr.Client;
import com.tallac.nac.utils.TallacUtils;

public class NacUserAuthResource extends ServerResource {
	
	private static final Logger LOG = LoggerFactory.getLogger(NacUserAuthResource.class);

    // Note: Inner classes must be static in order to be serialized by JSON
	//---- UserAuthResource class for receiving and parsing REST request
    static class UserAuthResource 
    {
        public String authtype;
        public String username;
        public String password;
        public String ip;

        public UserAuthResource( @JsonProperty("authtype")                                               String iAuthtype,
                                 @JsonProperty("username") @JsonSerialize(include = Inclusion.NON_NULL)  String iUsername, 
                                 @JsonProperty("password") @JsonSerialize(include = Inclusion.NON_NULL)  String iPassword, 
                                 @JsonProperty("ip")       @JsonSerialize(include = Inclusion.NON_NULL)  String iIp )
        {
            username = iUsername;
            password = iPassword;
            authtype = iAuthtype;
            ip       = iIp;
        }

        @Override
        public String toString() {
            return "UserAuthResource [authtype=" + authtype + ", username=" + username + ", password=" + password + ", ip=" + ip + "]";
        }

    }

	@Post
	public void accept(UserAuthResource user) {

		String                ipStr    = null;
		InetAddress           clientIp = null;
        ClientMgr.ClientState clientState;
        
        LOG.debug("Received REST POST request to authenticate user: " + user);
		
		//---- Get client IP address
        if( user.ip != null && !user.ip.isEmpty() ) ipStr = user.ip;
        else                                        ipStr = Request.getCurrent().getClientInfo().getAddress();

        //---- Convert IP address from string to InetAddress
		if( ipStr != null && !ipStr.isEmpty() ) clientIp = TallacUtils.convertIpv4Address(ipStr);

		//---- Return error if unable to get client IP
		if (clientIp == null)
		{
            TallacUtils.setRestApiError( String.format("Unable to parse client IP address [%s].", ipStr));
            return;
		}
		
		//---- Check authentication type
		if(      user.authtype.equalsIgnoreCase("client") ) clientState = ClientMgr.ClientState.AUTHENTICATED; 
		else if( user.authtype.equalsIgnoreCase("guest")  ) clientState = ClientMgr.ClientState.GUEST; 
		else
		{
            TallacUtils.setRestApiError( String.format("Invalid client type [%s].", user.authtype));
            return;
		}
		
        //---- Find Client in client list
        Client client = ClientMgr.getInstance().getClient( clientIp );
        if (client == null)
        {
            TallacUtils.setRestApiError( String.format( "Client with IP [%s] is not registered.", ipStr ) );
            return;
        }
        
		//---- Check user name. Get username from client record if it is not specified in request
		String username = user.username;
		if( username == null || username.isEmpty() )
		{
		    if( clientState != ClientMgr.ClientState.GUEST )  // User name is mandatory for authenticated clients.
		    {
	            TallacUtils.setRestApiError("Username is required");
	            return;
		    }
            username = client.getDetails();
		}
		    
		// Check password if necessary.
		
		//Modify client state, put username in description field
		ClientMgr.getInstance().updateClient( client.getMacAddr(), clientState, username );
		    
		Response.getCurrent().setStatus(Status.SUCCESS_OK);
	}
}
