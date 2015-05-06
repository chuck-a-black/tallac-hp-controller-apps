/*
 * Copyright (c) 2012, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.nac.api;

import net.floodlightcontroller.restserver.RestletRoutable;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Router;

public class RestRoutable implements RestletRoutable
{
    private final Class<? extends ServerResource> mServerResource;
    private final String mBasePath;

    public RestRoutable(final String basePath,
                        final Class<? extends ServerResource> serverResource)
    {
        mServerResource = serverResource;
        mBasePath = "/tallac/api/" + basePath;
    }

    @Override
    public String basePath()
    {
        return mBasePath;
    }

    @Override
    public Restlet getRestlet(final Context context)
    {
        final Router router = new Router(context);

        // NOTE BW: Using emtpy string as the pathtemplate. We're only using the
        //          basepath, since basepaths need to be unique. What we're
        //          doing here doesn't seem to be correct/clean, but it works
        //          for now ...
        router.attach("", mServerResource);

        return router;
    }
}
