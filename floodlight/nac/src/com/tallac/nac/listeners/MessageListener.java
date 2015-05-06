/*
 * Copyright (c) 2012, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.nac.listeners;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;

import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tallac.nac.NacModule;
import com.tallac.nac.packethandler.PacketHandler;

/**
 * The MessageListener class responsible for registering OpenFlow message
 * listener and receiving PacketIn messages.
 */
public class MessageListener implements IOFMessageListener
{
    private static final MessageListener INSTANCE = new MessageListener();
    private static final Logger LOG =
        LoggerFactory.getLogger(MessageListener.class);

    private static IFloodlightProviderService mProvider;

    private MessageListener()
    {
        // private constructor - prevent external instantiation
    }

    public static MessageListener getInstance()
    {
        return INSTANCE;
    }

    public void init(final FloodlightModuleContext context)
    {
        LOG.debug("Initialize NAC OpenFlow message listener.");
        if (mProvider != null)
        {
            throw new RuntimeException("NAC Message listener already initialized");
        }

        mProvider = context.getServiceImpl(IFloodlightProviderService.class);
    }

    public void startUp()
    {
        LOG.debug("Register NAC OpenFlow PacketIn message listener.");
        mProvider.addOFMessageListener(OFType.PACKET_IN, this);
    }

    @Override
    public String getName()
    {
        return NacModule.NAME;
    }

    @Override
    public boolean isCallbackOrderingPrereq(final OFType type,
                                            final String name)
    {
        return (type.equals(OFType.PACKET_IN) &&
                        (name.equals("topology") ||
                         name.equals("devicemanager")));
    }

    @Override
    public boolean isCallbackOrderingPostreq(final OFType type,
                                             final String name)
    {
        return (type.equals(OFType.PACKET_IN) && name.equals("forwarding"));
    }

    @Override
    public Command receive(final IOFSwitch ofSwitch,
                           final OFMessage msg,
                           final FloodlightContext context)
    {
        switch (msg.getType())
        {
        case PACKET_IN:
           LOG.trace("Received PacketIn {} from switch {}", msg, ofSwitch);
           PacketHandler ph = new PacketHandler( ofSwitch, msg, context );
           return ph.processPacket();

        default:
           LOG.trace("Received msg {} from switch {}", msg, ofSwitch);
           break;
        }

        return Command.CONTINUE;
    }

}

