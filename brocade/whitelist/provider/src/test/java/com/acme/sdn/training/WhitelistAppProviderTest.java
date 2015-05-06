package com.acme.sdn.training;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.After;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;

/**
 * Defines a unit test class which tests the provider.
 * 
 * This class leverages the AbstractDataBrokerTest class which starts a
 * real MD-SAL implementation to use inside of your unit tests.
 * 
 * This is not an exhaustive test, but rather is used to illustrate how one
 * can leverage the AbstractDataBrokerTest to test MD-SAL providers/listeners.
 * 
 */
public class WhitelistAppProviderTest extends AbstractDataBrokerTest{

	ExecutorService threadPool = Executors.newSingleThreadExecutor();
	WhitelistAppProvider provider;
	DataBroker dataBroker;
	
	/**
	 * The @Before annotation is defined in the AbstractDataBrokerTest class. The method
	 * setupWithDataBroker is invoked from inside the @Before method and is used
	 * to initialize the databroker with objects for a test runs.
	 * 
	 * In our case we use this oportunity to create an instance of our provider and
	 * initialize it (which registers it as a listener etc).
	 * 
	 * This method runs before every @Test method below.
	 */
	@Override
	protected void setupWithDataBroker(DataBroker dataBroker) {
		super.setupWithDataBroker(dataBroker);
		
		this.dataBroker = dataBroker;
		
		provider = new WhitelistAppProvider();
		provider.setDataBroker( dataBroker );
		provider.initialize();
	}
	
	/**
	 * Shuts down our provider, testing close code. @After runs after every @Test method below.
	 */
	@After
	public void stop() throws Exception{
		provider.close();
	}
	
}
