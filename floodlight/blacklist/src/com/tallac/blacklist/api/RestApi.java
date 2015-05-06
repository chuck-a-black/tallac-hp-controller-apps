/*
 * Copyright (c) 2012, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.blacklist.api;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.restserver.IRestApiService;


public class RestApi
{
    private static final RestApi INSTANCE = new RestApi();

    private static IRestApiService mRestApi;

    private RestApi()
    {
        // private constructor - prevent external instantiation
    }

    public static RestApi getInstance()
    {
        return INSTANCE;
    }

    public void init(final FloodlightModuleContext context)
    {
        if (mRestApi != null)
        {
            throw new RuntimeException("Flow Manager already initialized");
        }

        mRestApi = context.getServiceImpl(IRestApiService.class);
    }

    public void startUp()
    {
	// Rest API /tallac/api/...
        mRestApi.addRestletRoutable(new RestRoutable("blacklist/stats/details",
                                    BlacklistStatsDetailsResource.class));
        mRestApi.addRestletRoutable(new RestRoutable("blacklist/stats",
                                    BlacklistStatsResource.class));

        mRestApi.addRestletRoutable(new RestRoutable("blacklist/sites/dns/{id}",
                                    BlacklistDnsConfigResource.class));
        mRestApi.addRestletRoutable(new RestRoutable("blacklist/sites/dns",
                                                     BlacklistDnsConfigResource.class));

        mRestApi.addRestletRoutable(new RestRoutable("blacklist/sites/ip/{id}",
                                                     BlacklistIpv4ConfigResource.class));
        mRestApi.addRestletRoutable(new RestRoutable("blacklist/sites/ip",
                                    BlacklistIpv4ConfigResource.class));

	// Web UI routable is under /tallac/ui/...
        mRestApi.addRestletRoutable(new WebUiRoutable());
    }

}
