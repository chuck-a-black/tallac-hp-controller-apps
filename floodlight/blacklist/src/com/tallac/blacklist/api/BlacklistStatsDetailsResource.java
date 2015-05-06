/*
 * Copyright (c) 2012, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.blacklist.api;

import java.util.List;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tallac.blacklist.module.StatisticsMgr;
import com.tallac.blacklist.module.StatisticsMgr.StatisticsDetails;


public class BlacklistStatsDetailsResource extends ServerResource
{
    private static final Logger LOG =
        LoggerFactory.getLogger(BlacklistStatsDetailsResource.class);


    @Get("json")
    public List<StatisticsDetails> retrieve()
    {
        LOG.debug("Received REST GET blacklist stats details request.");

        StatisticsMgr statsMgr = StatisticsMgr.getInstance();
        List<StatisticsDetails> retVal = statsMgr.getDetails();

        return retVal;
    }
}
