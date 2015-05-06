/*
 * Copyright (c) 2012, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.blacklist.module;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.module.FloodlightModuleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tallac.blacklist.BlackListModule;

/**
 * The SwitchListener class is responsible for registering switch
 * listener, receiving switch related events such as connect, disconnect and
 * setting up default flows on connected switches.
 */
public class SwitchListener implements IOFSwitchListener
{
    private static final SwitchListener INSTANCE = new SwitchListener();
    private static final Logger LOG = LoggerFactory.getLogger(SwitchListener.class);

    private static IFloodlightProviderService mProvider;

    //---------------------------------------------------------------------------------------------
    private SwitchListener() { }  // private constructor - prevent external instantiation

    //---------------------------------------------------------------------------------------------
    public static SwitchListener getInstance() { return INSTANCE; }

    //---------------------------------------------------------------------------------------------
    public void init(final FloodlightModuleContext context)
    {
        LOG.trace("Initialize BlackList switch listener.");

        if (mProvider != null) throw new RuntimeException("Switch listener already initialized");
        else mProvider = context.getServiceImpl(IFloodlightProviderService.class);
    }

    //---------------------------------------------------------------------------------------------
    public void startUp()
    {
        LOG.trace("Register BlackList OpenFlow device listener.");
        mProvider.addOFSwitchListener(this);
    }


    //---------------------------------------------------------------------------------------------
    @Override
    public void addedSwitch(final IOFSwitch ofSwitch)
    {
        LOG.debug("Set default flows on switch {}", ofSwitch);
        FlowMgr.getInstance().setDefaultFlows(ofSwitch);
    }

    //---------------------------------------------------------------------------------------------
    @Override
    public void removedSwitch(final IOFSwitch ofSwitch) { LOG.debug("Switch {} disconnected", ofSwitch); }

    //---------------------------------------------------------------------------------------------
    @Override
    public void switchPortChanged(Long arg0)
    {
        LOG.debug("Switch {} port changed", arg0);
    }

    //---------------------------------------------------------------------------------------------
    @Override
    public String getName() { return BlackListModule.NAME; }

}
