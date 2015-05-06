/*
 * Copyright (c) 2012, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.blacklist.api;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tallac.blacklist.module.StatisticsMgr;
import com.tallac.blacklist.module.StatisticsMgr.StatisticsTotal;


public class BlacklistStatsResource extends ServerResource
{
    private static final Logger LOG =
        LoggerFactory.getLogger(BlacklistStatsResource.class);

    static class BlacklistStatistics
    {
        public long   ipv4Count;
        public String ipv4LastMatch;
        public long ipv4LastMatchTimestamp;
        public long   dnsCount;
        public String dnsLastMatch;
        public long dnsLastMatchTimestamp;

        public BlacklistStatistics() {}
        public BlacklistStatistics(StatisticsTotal ipv4Stats,
                                   StatisticsTotal dnsStats)
        {
            ipv4Count = ipv4Stats.getMatchCounter();
            ipv4LastMatch = "";
            ipv4LastMatchTimestamp = 0;
            if (ipv4Count > 0)
            {
                ipv4LastMatch = ipv4Stats.getMatch();
                ipv4LastMatchTimestamp = ipv4Stats.getLastMatchTime().getTime();
            }

            dnsCount = dnsStats.getMatchCounter();
            dnsLastMatch = "";
            dnsLastMatchTimestamp = 0;
            if (dnsCount > 0)
            {
                dnsLastMatch = dnsStats.getMatch();
                dnsLastMatchTimestamp = dnsStats.getLastMatchTime().getTime();
            }
        }
    }

    @Get("json")
    public BlacklistStatistics retrieve()
    {
        LOG.debug("Received REST GET blacklist stats request.");

        StatisticsMgr statsMgr = StatisticsMgr.getInstance();
        return new BlacklistStatistics(statsMgr.getIpv4Stats(),
                                       statsMgr.getDnsStats());
    }
}
