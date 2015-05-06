/*
 * Copyright (c) 2013, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.blacklist;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.FloodlightModuleLoader;
import net.floodlightcontroller.core.module.IFloodlightModuleContext;
import net.floodlightcontroller.restserver.IRestApiService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class Main
{
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final String CONFIG_FILE = "floodlight.properties";
    private static final String[] MODULES = new String[] {
                                                          "net.floodlightcontroller.core.FloodlightProvider",
                                                          "net.floodlightcontroller.forwarding.Forwarding",
                                                          "net.floodlightcontroller.counter.NullCounterStore",
                                                          "net.floodlightcontroller.perfmon.NullPktInProcessingTime",
                                                          "com.tallac.blacklist.BlackListModule",
    };

    private static final Marker NOTIFY_ADMIN = MarkerFactory.getMarker("NOTIFY_ADMIN");

    /**
     * Load modules for Floodlight.
     *
     * We are not using floodlight's
     * {@linkplain FloodlightModuleLoader#loadModulesFromConfig(String)},
     * since it only accepts an actual configuration file, not a properties
     * file. This way we can run from any directory without command line
     * parameters.
     *
     * @throws FloodlightModuleException
     */
    //---------------------------------------------------------------------------------------------
    //  init:  get configuration properties and load floodlight modules from the MODULES array
    //
    private IFloodlightModuleContext init() throws FloodlightModuleException
    {
        final FloodlightModuleLoader fml        = new FloodlightModuleLoader();
        final Properties             properties = new Properties();

        try
        {

            final InputStream is;  // Load floodlight settings from properties file
            is = getClass().getClassLoader().getResourceAsStream( CONFIG_FILE );
            properties.load( is );
        }
        catch (final IOException e)
        {
            LOG.error( "Unabled to load settings: {}", e.getMessage() );
            System.exit(1);
        }

        return fml.loadModulesFromList( Arrays.asList( MODULES ), properties );
    }

    //---------------------------------------------------------------------------------------------
    //  notifyAdminByMail
    //
    public static void notifyAdminByEmail(String fmt, Object ... args)
    {
        LOG.error( NOTIFY_ADMIN, fmt, args );
    }

    //---------------------------------------------------------------------------------------------
    //  main:  main Blacklist java application
    //

    public static void main(final String[] args) throws FloodlightModuleException
    {
        LOG.info("Tallac BlackList Version: {}", Version.getVersionString());

        // Add logger facade on RESTlet engine
        System.setProperty( "org.restlet.engine.loggerFacadeClass", "org.restlet.ext.slf4j.Slf4jLoggerFacade" );

        // Initialize
        final IFloodlightModuleContext context = new Main().init();

        // Run REST server
        final IRestApiService restApi = context.getServiceImpl(IRestApiService.class);
        restApi.run();

        // Start floodlight controller
        final IFloodlightProviderService controller = context.getServiceImpl(IFloodlightProviderService.class);
        controller.run(); // Note blocking call
    }
}
