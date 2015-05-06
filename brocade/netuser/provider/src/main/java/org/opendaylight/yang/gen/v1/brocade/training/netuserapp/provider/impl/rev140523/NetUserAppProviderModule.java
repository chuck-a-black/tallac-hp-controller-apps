package org.opendaylight.yang.gen.v1.brocade.training.netuserapp.provider.impl.rev140523;

import com.acme.sdn.training.NetUserAppProvider;

public class NetUserAppProviderModule extends org.opendaylight.yang.gen.v1.brocade.training.netuserapp.provider.impl.rev140523.AbstractNetUserAppProviderModule {
    public NetUserAppProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetUserAppProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.brocade.training.netuserapp.provider.impl.rev140523.NetUserAppProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

         final NetUserAppProvider provider = new NetUserAppProvider();
         provider.setDataBroker( getDataBrokerDependency() );
         provider.setNotificationService( getNotificationServiceDependency() );
         provider.setRpcRegistry( getRpcRegistryDependency() );
         provider.initialize();
         return new AutoCloseable() {

            @Override
            public void close() throws Exception {
                //TODO: CLOSE ANY REGISTRATION OBJECTS CREATED USING ABOVE BROKER/NOTIFICATION
                //SERVIE/RPC REGISTRY
                provider.close();
            }
        };
    }

}
