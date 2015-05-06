package org.opendaylight.yang.gen.v1.brocade.training.whitelistapp.provider.impl.rev140523;

import com.acme.sdn.training.WhitelistAppProvider;

public class WhitelistAppProviderModule extends org.opendaylight.yang.gen.v1.brocade.training.whitelistapp.provider.impl.rev140523.AbstractWhitelistAppProviderModule {
    
	public WhitelistAppProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public WhitelistAppProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.brocade.training.whitelistapp.provider.impl.rev140523.WhitelistAppProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

         final WhitelistAppProvider provider = new WhitelistAppProvider();
         
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
