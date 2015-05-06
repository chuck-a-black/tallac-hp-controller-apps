/*
 * Copyright (c) 2012, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.blacklist.api;

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

public class BlacklistDnsConfigResource extends ServerResource
{
    private static final Logger LOG = LoggerFactory.getLogger(BlacklistDnsConfigResource.class);

    static class DnsBlacklistRecord {
        private final String record;
        private final String id;

        public DnsBlacklistRecord( @JsonProperty( "record" ) String rec ) {
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

    @Post("json")
    public DnsBlacklistRecord accept(DnsBlacklistRecord cfgRecord)
    {
        LOG.debug("Received REST POST request to add domain name: " +
                  cfgRecord.getRecord());

        String record = cfgRecord.getRecord();
        // Get and check record
        if (record.isEmpty())
        {
            setError("Record string is empty.");
            return null;
        }

        // Check if record already exist in the blacklist
        boolean isRecordFound = BlacklistMgr.getInstance()
                                            .checkDnsBlacklist(record);

        // If command is add and record does not exist - add record
        if (isRecordFound)
        {
            setError("Unable to add record. Record [" + record +
                     "] already exists in DNS blacklist.");
            return null;
        }

        BlacklistMgr.getInstance().addDnsRecord(record);

        // save modification in DNS configuration file
        BlacklistMgr.getInstance().saveDnsBlacklist();

        Response.getCurrent().setStatus(Status.SUCCESS_OK);
        return cfgRecord;
    }

    @Delete
    public void removeEntry() {

        String id = (String) getRequest().getAttributes().get("id");
        if( id != null ) {
            BlacklistMgr.getInstance().removeDnsRecord( id );
            BlacklistMgr.getInstance().saveDnsBlacklist();
        }

        Response.getCurrent().setStatus(Status.SUCCESS_OK);
    }

    @Get("json")
    public Collection<DnsBlacklistRecord> retrieve()
    {
        Collection<DnsBlacklistRecord> records = new ArrayList<DnsBlacklistRecord>();
        LOG.debug("Received REST GET DNS blacklist config.");
        for( String s : BlacklistMgr.getInstance().getDnsBlacklistConfig() ) {
            records.add( new DnsBlacklistRecord( s ) );
        }
        return records;
    }

    private void setError(String errStr)
    {
        Response.getCurrent()
                .setStatus(Status.CLIENT_ERROR_BAD_REQUEST, errStr);
    }

}
