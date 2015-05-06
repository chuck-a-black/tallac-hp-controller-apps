package com.acme.sdn.training;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.yang.gen.v1.brocade.netuser.rev150115.*;
import org.opendaylight.yang.gen.v1.brocade.netuser.rev150115.task.container.*;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Defines a base implementation for your provider. This class extends from a helper class
 * which provides storage for the most commonly used components of the MD-SAL. Additionally the
 * base class provides some basic logging and initialization / clean up methods.
 *
 * To use this, copy and paste (overwrite) the following method into the TestApplicationProviderModule
 * class which is auto generated under src/main/java in this project
 * (created only once during first compilation):
 *
 * <pre>


    </pre>
 */
public class NetUserAppProvider implements AutoCloseable, NetUserAppService, DataChangeListener{

	private List<AutoCloseable> closeList = new LinkedList<AutoCloseable>();
    private final Logger log = LoggerFactory.getLogger( NetUserAppProvider.class );
    private final String appName = "NetUserApp";

    protected DataBroker dataBroker;
    protected NotificationProviderService notificationService;
    protected RpcProviderRegistry rpcRegistry;

    public NetUserAppProvider() {
        this.log.info( "Creating provider for " + appName );
    }

    public void initialize(){
        log.info( "Initializing provider for " + appName );
        //initialization code goes here.
        log.info( "Initialization complete for " + appName );
    }

    protected void initializeChild() {
        //Override if you have custom initialization intelligence
    }

    @Override
    public void close() throws Exception {
        log.info( "Closing provider for " + appName );
        
        synchronized( closeList ){
        	for( AutoCloseable cls : closeList ){
        		cls.close();
        	}
        	closeList.clear();
        }
        
        log.info( "Successfully closed provider for " + appName );
    }

    public void setDataBroker(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        if( log.isDebugEnabled() ){
            log.debug( "DataBroker set to " + (dataBroker==null?"null":"non-null") + "." );
        }
        InstanceIdentifier<TaskContainer> tcId = InstanceIdentifier.builder( TaskContainer.class ).build();
		ListenerRegistration<DataChangeListener> registerDataChangeListener = 
				dataBroker.registerDataChangeListener( LogicalDatastoreType.CONFIGURATION, tcId, this, DataChangeScope.SUBTREE );
		synchronized( closeList ){
			closeList.add( registerDataChangeListener );
		}
    }

    public void setNotificationService(
            NotificationProviderService notificationService) {
        this.notificationService = notificationService;
        if( log.isDebugEnabled() ){
            log.debug( "Notification Service set to " + (notificationService==null?"null":"non-null") + "." );
        }
    }

    public void setRpcRegistry(RpcProviderRegistry rpcRegistry) {
        this.rpcRegistry = rpcRegistry;
        if( log.isDebugEnabled() ){
            log.debug( "RpcRegistry set to " + (rpcRegistry==null?"null":"non-null") + "." );
        }
        final RpcRegistration<NetUserAppService> rpcRegistrion = rpcRegistry
				.addRpcImplementation(NetUserAppService.class, this);
        synchronized( closeList ){
			closeList.add( rpcRegistrion );
		}
    }

	@Override
	public Future<RpcResult<Void>> runTask(RunTaskInput input) {
		
		if( input == null ){
    		//null input, immediately fail since we need a task name in order to run it.
    		return Futures.immediateFuture( RpcResultBuilder.<Void>failed().build() );
    	}
    	
    	final String taskId = input.getTaskId();
    	if( StringUtils.isEmpty( taskId ) ){
    		//null or empty task ID is cause for failrue too.
    		return Futures.immediateFuture( RpcResultBuilder.<Void>failed().build() );
    	}
    	
    	final ReadWriteTransaction readTx = dataBroker.newReadWriteTransaction();
    	
    	TaskKey key = new TaskKey( taskId );
    	final InstanceIdentifier<Task> id = InstanceIdentifier.builder( TaskContainer.class ).child( Task.class, key ).build();
    	
    	CheckedFuture<Optional<Task>, ReadFailedException> readResultFuture = readTx.read( LogicalDatastoreType.OPERATIONAL, id );
    	
    	final SettableFuture<RpcResult<Void>> returnFuture = SettableFuture.create(); 
    	
    	Futures.addCallback( readResultFuture, new FutureCallback<Optional<Task>>() {

			@Override
			public void onFailure(Throwable arg0) {
				log.error( "Exception while running task " + taskId + ".", arg0);
				returnFuture.set( RpcResultBuilder.<Void>failed().withError( ErrorType.RPC, arg0.getMessage(), arg0 ).build() );
			}

			@Override
			public void onSuccess(Optional<Task> arg0) {
				if( arg0.isPresent() ){
					Task curTValue = arg0.get();
					Task t = new TaskBuilder( curTValue ).setRunCount( curTValue.getRunCount() + 1 ).build();
					readTx.merge( LogicalDatastoreType.OPERATIONAL, id, t, false);
					
					CheckedFuture<Void, TransactionCommitFailedException> writeTxSubmit = readTx.submit();
					Futures.addCallback( writeTxSubmit, new FutureCallback<Void>() {

						@Override
						public void onFailure(Throwable arg0) {
							log.error( "Exception while running task " + taskId + ".", arg0);
							returnFuture.set( RpcResultBuilder.<Void>failed().withError( ErrorType.RPC, arg0.getMessage(), arg0 ).build() );
						}

						@Override
						public void onSuccess(Void arg0) {
							returnFuture.set( RpcResultBuilder.<Void>success().build() );
						}
						
					});
					log.info("Running task " + taskId + " with name " + curTValue.getName() );
				} else {
					log.info( "Task " + taskId + " not found." );
					returnFuture.set( RpcResultBuilder.<Void>failed().build() );
				}
			}
		});
    	
    	return returnFuture;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onDataChanged(
			AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> arg0) {
		
		WriteTransaction wt = dataBroker.newWriteOnlyTransaction();
		
		Map<InstanceIdentifier<?>, DataObject> createdData = arg0.getCreatedData();
		for( Entry<InstanceIdentifier<?>, DataObject> entry : createdData.entrySet() ){
			DataObject possibleTask = entry.getValue();
			if( possibleTask instanceof Task ){
				Task t = (Task)possibleTask;
				Task tUpdate = new TaskBuilder( t ).setRunCount( 0 ).build();
				wt.put( LogicalDatastoreType.OPERATIONAL, (InstanceIdentifier<Task>)entry.getKey(), tUpdate, true );
			}
		}
		
		CheckedFuture<Void, TransactionCommitFailedException> submit = wt.submit();
		Futures.addCallback( submit, new FutureCallback<Void>() {

			@Override
			public void onFailure(Throwable arg0) {
				log.warn( "Exception while initializing write.");
			}

			@Override
			public void onSuccess(Void arg0) {
				log.debug( "Successful write");
			}
		} );
		
	}
		
}
