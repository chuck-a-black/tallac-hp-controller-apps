/*
 * Copyright (c) 2013, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.nac;

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

import com.tallac.nac.api.RestApi;
import com.tallac.nac.listeners.DeviceListener;
import com.tallac.nac.listeners.MessageListener;
import com.tallac.nac.listeners.SwitchListener;
import com.tallac.nac.module.ClientMgr;
import com.tallac.nac.module.ConfigMgr;
import com.tallac.nac.module.FlowMgr;
import com.tallac.nac.packethandler.HttpPacketHandler;

public class NacModule implements IFloodlightModule
{
    public static final String NAME = "Nac";

    private static final Logger LOG = LoggerFactory.getLogger(NacModule.class);

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
        
        ConfigMgr.getInstance().        init();
        FlowMgr.getInstance().          init(context);
        SwitchListener.getInstance().   init(context);
        MessageListener.getInstance().  init(context);
        DeviceListener.getInstance().   init(context);
        ClientMgr.getInstance().        init();
        HttpPacketHandler.getInstance().init();
        RestApi.getInstance().          init(context);
   }

    @Override
    public void startUp(final FloodlightModuleContext context)
    {
        LOG.trace("StartUp");
        
        SwitchListener.getInstance(). startUp();
        MessageListener.getInstance().startUp();
        DeviceListener.getInstance(). startUp();
        RestApi.getInstance().        startUp();
        ClientMgr.getInstance().      startUp();
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService>
    getServiceImpls()
    {
        final Map<Class<? extends IFloodlightService>, IFloodlightService> map = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
        return map;
    }
}
