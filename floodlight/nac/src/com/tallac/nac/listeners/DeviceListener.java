package com.tallac.nac.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceListener;
import net.floodlightcontroller.devicemanager.IDeviceService;

public class DeviceListener implements IDeviceListener {

    private static final DeviceListener INSTANCE = new DeviceListener();
    private static final Logger         LOG      = LoggerFactory.getLogger( SwitchListener.class );

    private static IDeviceService             mDeviceService;

    private DeviceListener()                    { /*  private constructor - prevent external instantiation*/  }
    public static DeviceListener getInstance()  { return INSTANCE;  }

    public void init( final FloodlightModuleContext context )
    {
        LOG.trace("Initialize NAC device listener.");
        if( mDeviceService != null ) { throw new RuntimeException("Device listener already initialized"); }

        mDeviceService = context.getServiceImpl( IDeviceService.class );
    }

    public void startUp()
    {
        LOG.trace("Register NAC OpenFlow device listener.");
        mDeviceService.addListener( this );

    }

	@Override
	public void deviceAdded( IDevice device ) {
		LOG.info( "Device added: {}", device );
		
	}

	@Override
	public void deviceIPV4AddrChanged( IDevice device ) {
		LOG.info( "Device IPv4 address changed: {}", device );
		
	}

	@Override
	public void deviceMoved( IDevice device ) {
		LOG.info( "Device location changed: {}", device );
		
	}

	@Override
	public void deviceRemoved( IDevice device ) {
		LOG.info( "Device removed: {}", device );
		
	}

	@Override
	public void deviceVlanChanged( IDevice device ) {
		LOG.info( "Device VLAN changed: {}", device );
		
	}

}
