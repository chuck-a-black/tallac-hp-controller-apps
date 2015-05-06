/*
 * Copyright (c) 2013, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.nac.api;

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
            throw new RuntimeException("REST API already initialized");
        }

        mRestApi = context.getServiceImpl(IRestApiService.class);
    }

    public void startUp()
    {
	// Rest API /tallac/api/...
        mRestApi.addRestletRoutable(new RestRoutable("nac/users/auth", NacUserAuthResource.class));
        mRestApi.addRestletRoutable(new RestRoutable("nac/users/{id}", NacUserResource.class));
        mRestApi.addRestletRoutable(new RestRoutable("nac/users", NacUserResource.class));
        mRestApi.addRestletRoutable(new RestRoutable("nac/logs", NacLogsResource.class));

	// Web UI routable is under /tallac/ui/...
        mRestApi.addRestletRoutable(new WebUiRoutable());
    }

}
