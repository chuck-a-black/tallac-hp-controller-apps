/*
 * Copyright (c) 2013, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.nac.listeners;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.module.FloodlightModuleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tallac.nac.NacModule;
import com.tallac.nac.module.ClientMgr;
import com.tallac.nac.module.FlowMgr;
import com.tallac.nac.module.SwitchMgr;

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
    private SwitchListener()
    {
        // private constructor - prevent external instantiation
    }

    public static SwitchListener getInstance()
    {
        return INSTANCE;
    }

    public void init( final FloodlightModuleContext context )
    {
        LOG.trace("Initialize NAC switch listener.");
        if (mProvider != null)
        {
            throw new RuntimeException("Switch listener already initialized");
        }

        mProvider = context.getServiceImpl(IFloodlightProviderService.class);
    }

    public void startUp()
    {
        LOG.trace("Register NAC OpenFlow switch listener.");
        mProvider.addOFSwitchListener(this);
    }


    @Override
    public void addedSwitch( final IOFSwitch ofSwitch )
    {
        LOG.info( "Set default flows on switch {}", ofSwitch );
        FlowMgr.getInstance().setDefaultFlows( ofSwitch );         // set default flows
        
        LOG.info( "Set client's flows on switch {}", ofSwitch );   // Set flows for authenticated clients
        ClientMgr.getInstance().setClientsFlows( ofSwitch );
        
        LOG.info( "Adding ofSwitch to SwitchMgr: {}", ofSwitch );
        SwitchMgr.getInstance().addSwitch( ofSwitch );
        
    }

    @Override
    public void removedSwitch( final IOFSwitch ofSwitch )
    {
        LOG.debug("Switch {} disconnected", ofSwitch);
        
        LOG.info( "Removing ofSwitch from Switchmgr: {}", ofSwitch );
    }

    @Override
    public void switchPortChanged(Long arg0)
    {
        LOG.debug("Switch {} port changed", arg0);
    }

    @Override
    public String getName()
    {
        return NacModule.NAME;
    }

}
