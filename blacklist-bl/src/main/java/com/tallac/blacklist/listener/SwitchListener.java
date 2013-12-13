package com.tallac.blacklist.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.of.ctl.ControllerService;
import com.hp.of.ctl.DataPathEvent;
import com.hp.of.ctl.DataPathListener;
import com.hp.of.ctl.QueueEvent;
import com.tallac.blacklist.manager.SwitchManager;

public class SwitchListener implements DataPathListener {

    private static ControllerService mControllerService;

    private static final Logger LOG = LoggerFactory.getLogger( SwitchListener.class );
    private static final String LOGPREFACE = "[Blacklist: SwitchListener]: ";        

    //---------------------------------------------------------------------------------------------
    public void init( final ControllerService controllerService )
    {
    	LOG.info( LOGPREFACE + "init(): Initialization." );
        
        mControllerService = controllerService;       
    }

    //---------------------------------------------------------------------------------------------
    public void startUp()
    {
        mControllerService.addDataPathListener( this );
        LOG.info( LOGPREFACE + "startup(): OpenFlow switch listener registered." );
    }

    //---------------------------------------------------------------------------------------------
    public void shutDown()
    {
        mControllerService.removeDataPathListener( this );
        LOG.info( LOGPREFACE + "shutDown(): OpenFlow switch listener unregistered." );
    }


    //---------------------------------------------------------------------------------------------
	@Override
	public void event( DataPathEvent dpEvent ) {
		
		switch( dpEvent.type() ) {
		
		case DATAPATH_CONNECTED:
			LOG.info( LOGPREFACE + "event(): Received datapath-connected event." );
			SwitchManager.getInstance().setInitialFlows( dpEvent.dpid() );
			break;
			
		case DATAPATH_DISCONNECTED:
			LOG.info( LOGPREFACE + "event(): Received datapath-disconnected event." );
			break;
			
		default:
			LOG.info( LOGPREFACE + "event(): Received some other datapath event." );
			break;			
			
		}
		
	}

	@Override
	public void queueEvent(QueueEvent arg0) {
		// TODO Auto-generated method stub
		
	}


}
