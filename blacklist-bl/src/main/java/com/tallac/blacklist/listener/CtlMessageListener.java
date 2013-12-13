package com.tallac.blacklist.listener;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.of.ctl.ControllerService;
import com.hp.of.ctl.MessageEvent;
import com.hp.of.ctl.MessageListener;
import com.hp.of.ctl.QueueEvent;
import com.hp.of.lib.msg.MessageType;

public class CtlMessageListener implements MessageListener {

    private static ControllerService mControllerService;

    private static final Logger LOG = LoggerFactory.getLogger( CtlMessageListener.class );
    private static final String LOGPREFACE = "[Blacklist: MessageListener]: ";        

    private static final Set<MessageType> MESSAGE_TYPES = new HashSet<MessageType>();
    static {
        MESSAGE_TYPES.add(MessageType.PORT_MOD );
//        MESSAGE_TYPES.getClass().getEnumConstants();
    }

    //---------------------------------------------------------------------------------------------
    public void init( final ControllerService controllerService )
    {
    	LOG.info( LOGPREFACE + "init(): Initialization." );

        mControllerService = controllerService;        
    }

    //---------------------------------------------------------------------------------------------
    public void startUp()
    {
        mControllerService.addMessageListener( this, MESSAGE_TYPES );
        LOG.info( LOGPREFACE + "startUp(): OpenFlow message listener registered. ");
    }

    //---------------------------------------------------------------------------------------------
    public void shutDown()
    {
        mControllerService.addMessageListener( this, MESSAGE_TYPES );
        LOG.info( LOGPREFACE + "shutDown(): OpenFlow message listener unregistered. ");
    }

	@Override
	public void event(MessageEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void queueEvent(QueueEvent arg0) {
		// TODO Auto-generated method stub
		
	}


}
