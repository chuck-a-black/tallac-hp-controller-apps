/*
 * Copyright (c) 2013, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.nac.api;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tallac.nac.module.LogMgr;
import com.tallac.nac.module.LogMgr.LogRecord;
import com.tallac.nac.utils.TallacUtils;

public class NacLogsResource extends ServerResource
{
	private static final Logger LOG = LoggerFactory
			.getLogger(NacLogsResource.class);
	
    // Inner classes should be created as static in order to be serialized by
    // json
    // More details :
    // http://www.cowtowncoder.com/blog/archives/2010/08/entry_411.html
    static class LogRecordResource 
    {
        public String time;
        public String mac;
        public String ip;
        public String state;
        public String details;
        public String event;

        public LogRecordResource(){} 
        public LogRecordResource(@JsonProperty("time")String iTime, 
                    @JsonProperty("mac")String iMac, 
                    @JsonProperty("ip")String iIp, 
                    @JsonProperty("state")String iState, 
                    @JsonProperty("details")String iDetails,
                    @JsonProperty("event")String iEvent)
        {
            time = iTime;
            mac = iMac;
            ip = iIp;
            state = iState;
            details = iDetails;
            event = iEvent;
        }

        @Override
        public String toString() {
            return "LogRecordResource [time=" + time + ", mac=" + mac + ", ip="
                    + ip + ", state=" + state + ", details=" + details
                    + ", event=" + event + "]";
        }

    }
    
    @Get("json")
    public List<LogRecordResource> retrieve()
    {
        LOG.debug("Received REST GET NAC logs request.");

        List<LogRecordResource> retVal = new ArrayList<LogRecordResource>();
       
        for (LogRecord lr : LogMgr.getInstance().getLog())
        {
            retVal.add(new LogRecordResource(
                new SimpleDateFormat("MMM dd yyyy hh:mm:ss").format(lr.getDate()),
                lr.getClient().getMacAddr().toString(),
                TallacUtils.intIpToString(lr.getClient().getIpAddr()), 
                lr.getClient().getState().name(),
                lr.getClient().getDetails(), 
                lr.getEvent()));
        }
        return retVal;
    }
}
