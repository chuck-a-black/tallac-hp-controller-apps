/*
 * Copyright (c) 2012, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.blacklist.api;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;

import org.codehaus.jackson.annotate.JsonProperty;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tallac.blacklist.module.BlacklistMgr;

public class BlacklistIpv4ConfigResource extends ServerResource
{
    private static final Logger LOG = LoggerFactory.getLogger(BlacklistIpv4ConfigResource.class);

    static class Ipv4BlacklistRecord {
        private final String record;
        private final String id;

        public Ipv4BlacklistRecord( @JsonProperty( "record" ) String rec ) {
            id = record = rec;
        }

        /**
         * @return configuration record
         */
        @JsonProperty( "record" )
        public String getRecord() { return record; }

        @JsonProperty( "id" )
        public String getId() { return id; }
    }

    @Post
    public Ipv4BlacklistRecord accept(Ipv4BlacklistRecord cfgRecord)
    {
        LOG.debug("Received REST POST request to modify configuration: " +
                  cfgRecord.getRecord());

        String record = cfgRecord.getRecord();

        // Check record
        if (record.isEmpty())
        {
            setError("Record string is empty.");
            return null;
        }

        // Convert string representation of IPv4 address to InetAddress
        InetAddress ipAddr = BlacklistMgr.convertIpv4Address(record);
        if (ipAddr == null)
        {
            setError("Unable to parse IPv4 address: " + record);
            return null;
        }

        // Check if record already exist in the blacklist
        boolean isRecordFound = BlacklistMgr.getInstance()
                                            .checkIpv4Blacklist(ipAddr);
        if (isRecordFound)
        {
            setError("Unable to add record. Record [" + record +
                     "] already exists in IPv4 blacklist.");
            return null;
        }
        BlacklistMgr.getInstance().addIpv4Record(ipAddr);
        // save modification in IPv4 configuration file
        BlacklistMgr.getInstance().saveIpv4Blacklist();

        Response.getCurrent().setStatus(Status.SUCCESS_OK);
        return cfgRecord;
    }

    private void setError(String errStr)
    {
        Response.getCurrent()
                .setStatus(Status.CLIENT_ERROR_BAD_REQUEST, errStr);
    }

    @Delete
    public void removeEntry()
    {
        String id = (String) getRequest().getAttributes().get("id");
        if( id != null ) {
            BlacklistMgr.getInstance().removeIpv4Record( BlacklistMgr.convertIpv4Address( id ) );
            BlacklistMgr.getInstance().saveIpv4Blacklist();
        }

        Response.getCurrent().setStatus(Status.SUCCESS_OK);
    }

    @Get("json")
    public Collection<Ipv4BlacklistRecord> retrieve()
    {
        Collection<Ipv4BlacklistRecord> records = new ArrayList<Ipv4BlacklistRecord>();
        LOG.debug("Received REST GET IP blacklist config.");
        for( String s : BlacklistMgr.getInstance().getIpv4BlacklistConfig() ) {
            records.add( new Ipv4BlacklistRecord( s ) );
        }
        return records;
    }
}
