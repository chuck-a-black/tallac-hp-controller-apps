/*
 * Copyright (c) 2013, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.blacklist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tallac.blacklist.api.RestApi;
import com.tallac.blacklist.module.BlacklistMgr;
import com.tallac.blacklist.module.FlowMgr;
import com.tallac.blacklist.module.MessageListener;
import com.tallac.blacklist.module.StatisticsMgr;
import com.tallac.blacklist.module.SwitchListener;

public class BlackListModule implements IFloodlightModule
{
    public static final String NAME = "BlackList";

    private static final Logger LOG = LoggerFactory.getLogger(BlackListModule.class);

    @Override
    public Collection<Class<? extends IFloodlightService>>
    getModuleServices()
    {
        final Collection<Class<? extends IFloodlightService>> list = new ArrayList<Class<? extends IFloodlightService>>();
        return list;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>>
    getModuleDependencies()
    {
        final Collection<Class<? extends IFloodlightService>> dependencies = new ArrayList<Class<? extends IFloodlightService>>();

        dependencies.add(IFloodlightProviderService.class);
        dependencies.add(IRestApiService.class);

        return dependencies;
    }

    @Override
    public void init(final FloodlightModuleContext context)
        throws FloodlightModuleException
    {
        LOG.trace("Init");

        FlowMgr.getInstance().        init(context);  // Initialize all of our Blacklist modules
        SwitchListener.getInstance(). init(context);
        MessageListener.getInstance().init(context);
        BlacklistMgr.getInstance().   init(context);
        StatisticsMgr.getInstance().  init(context);
        RestApi.getInstance().        init(context);

    }

    @Override
    public void startUp(final FloodlightModuleContext context)
    {
        LOG.trace("StartUp");

        SwitchListener.getInstance(). startUp();
        MessageListener.getInstance().startUp();
        RestApi.getInstance().        startUp();
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls()
    {
        final Map<Class<? extends IFloodlightService>, IFloodlightService> map =
            new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();

        return map;
    }
}
