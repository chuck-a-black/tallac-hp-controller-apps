//  (c) Copyright 2013 Hewlett-Packard Development Company, L.P.
//  Autogenerated

package com.tallac.blacklist.rs;

import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;

import com.hp.sdn.rs.misc.ServiceLocator;

/**
 * Component for tracking availability of various services for the benefit of
 * of the web-layer REST resources.
 * <p>
 * The class acts as point of contact for the OSGi declarative services and is
 * notified when services that it is interested in become available or go away.
 * It uses these notifications to track availability and for providing service
 * references to the REST resources.
 */
@Component(immediate=true, specVersion="1.1")
@References(value={
    @Reference(name="BlacklistService", 
               referenceInterface=com.tallac.blacklist.api.BlacklistService.class,
               policy=ReferencePolicy.DYNAMIC, 
               cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE)
    })
public class ServiceAssistant {
    
    private ServiceLocator sl = ServiceLocator.INSTANCE;

    /**
     * Hook for registering BlacklistService implementation via declarative services.
     *
     * @param s newly advertised service to register
     * @param properties the properties associated with the service
     */
    protected void bindBlacklistService(com.tallac.blacklist.api.BlacklistService s, Map<String, Object> properties) {
        sl.register(com.tallac.blacklist.api.BlacklistService.class, s, properties);
    }
    
    /**
     * Hook for unregistering deactivated SystemInformationService via declarative services.
     *
     * @param s deactivated service to unregister
     */
    protected void unbindBlacklistService(com.tallac.blacklist.api.BlacklistService s) {
        sl.unregister(com.tallac.blacklist.api.BlacklistService.class, s);
    }

}
