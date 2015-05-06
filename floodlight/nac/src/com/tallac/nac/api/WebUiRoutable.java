/*
 * Copyright (c) 2012, Elbrys Networks
 * All Rights Reserved.
 */

package com.tallac.nac.api;

import net.floodlightcontroller.restserver.RestletRoutable;

import org.restlet.Client;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.resource.Directory;
import org.restlet.routing.Router;

public class WebUiRoutable implements RestletRoutable
{
    @Override
    public Restlet getRestlet(final Context context)
    {
        final Router router = new Router(context);
        router.attach("", new Directory(context, "clap://classloader/web/"));
        context.setClientDispatcher(new Client(context, Protocol.CLAP));
        return router;
    }

    @Override
    public String basePath()
    {
        // FIXME: See if we can use (base) path from RestRoutable
        return "/nac/";
    }
}
