package com.acme.sdn.training;

import org.opendaylight.yang.gen.v1.brocade.netuser.rev150115.Netuser;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.brocade.whitelist.rev150115.Whitelist;
import org.opendaylight.yang.gen.v1.brocade.whitelist.rev150115.whitelist.WhitelistEntry;
import org.opendaylight.yang.gen.v1.brocade.netuser.rev150115.netuser.NetuserEntry;
//import org.opendaylight.yang.gen.v1.brocade.sample.rev150115.*;
import org.opendaylight.yang.gen.v1.urn.com.brocade.apps.pathexplorer.rev140626.Paths;
import org.opendaylight.yang.gen.v1.urn.com.brocade.apps.pathexplorer.rev140626.paths.Path;
import org.opendaylight.yang.gen.v1.urn.com.brocade.apps.pathexplorer.rev140626.paths.PathBuilder;
import org.opendaylight.yang.gen.v1.urn.com.brocade.apps.pathexplorer.rev140626.paths.PathKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

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
public class WhitelistAppProvider implements AutoCloseable, DataChangeListener{

	private List<AutoCloseable> closeList = new LinkedList<AutoCloseable>();
	
    private final Logger log = LoggerFactory.getLogger( WhitelistAppProvider.class );
    private final String appName = "WhitelistApp";

    // Important services: data broker, notification service, and rpc registry
    protected DataBroker                  dataBroker;
    protected NotificationProviderService notificationService;
    protected RpcProviderRegistry         rpcRegistry;

	// Cache the current map of whitelist and netuser entries
	private volatile HashMap<Ipv4Address, Ipv4Address> whitelistMap;
	private volatile HashMap<Ipv4Address, Ipv4Address> netuserMap;
	
	// Get the instance IDs for Whitelist and Netuser
	private static final InstanceIdentifier<Whitelist> WHITELIST_IID = InstanceIdentifier.builder(Whitelist.class).build();
	private static final InstanceIdentifier<Netuser>   NETUSER_IID   = InstanceIdentifier.builder(Netuser.class).build();

	//---------------------------------------------------------------------------------------------
    public WhitelistAppProvider() {
    	
        this.log.info( "Creating provider for " + appName );
        
		whitelistMap = new HashMap<Ipv4Address, Ipv4Address>(10);
		netuserMap   = new HashMap<Ipv4Address, Ipv4Address>(10);

    }

	//---------------------------------------------------------------------------------------------
    public void initialize(){
    	
        log.info( "Initializing provider for " + appName );
        //initialization code goes here.
        log.info( "Initialization complete for " + appName );
    }

	//---------------------------------------------------------------------------------------------
    protected void initializeChild() {
        //Override if you have custom initialization intelligence
    }

	//---------------------------------------------------------------------------------------------
    @Override
    public void close() throws Exception {
        log.info( "Closing provider for " + appName );
        
        synchronized( closeList ){
        	for( AutoCloseable cls : closeList ) {
        		cls.close();
        	}
        	closeList.clear();
        }
        
        log.info( "Successfully closed provider for " + appName );
    }

	//---------------------------------------------------------------------------------------------
    public void setDataBroker( DataBroker dataBroker ) {
        
    	this.dataBroker = dataBroker;
    	
        if( log.isDebugEnabled() ){
            log.debug( "DataBroker set to " + (dataBroker==null?"null":"non-null") + "." );
        }
        
		ListenerRegistration<DataChangeListener> whitelistDataChangeListener = 
				dataBroker.registerDataChangeListener( LogicalDatastoreType.CONFIGURATION, WHITELIST_IID, this, DataChangeScope.SUBTREE );
		ListenerRegistration<DataChangeListener> netuserDataChangeListener = 
				dataBroker.registerDataChangeListener( LogicalDatastoreType.CONFIGURATION, NETUSER_IID, this, DataChangeScope.SUBTREE );
		
		synchronized( closeList ){
			closeList.add( whitelistDataChangeListener );
			closeList.add( netuserDataChangeListener );
		}
    }

	//---------------------------------------------------------------------------------------------
    public void setNotificationService( NotificationProviderService notificationService ) {
    	
        this.notificationService = notificationService;
        if( log.isDebugEnabled() ){
            log.debug( "Notification Service set to " + (notificationService==null?"null":"non-null") + "." );
        }
        
    }

	//---------------------------------------------------------------------------------------------
    public void setRpcRegistry(RpcProviderRegistry rpcRegistry) {
    	
        this.rpcRegistry = rpcRegistry;
        
        if( log.isDebugEnabled() ){
            log.debug( "RpcRegistry set to " + (rpcRegistry==null?"null":"non-null") + "." );
        }
       
        synchronized( closeList ){
		}
    }

	//---------------------------------------------------------------------------------------------
	@Override
	public void onDataChanged( AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent ) {
		
		log.info("onDataChanged() called. " + dataChangeEvent);

		// Use updated subtree to distinguish a whitelist/netuser change
		DataObject origSubTree    = dataChangeEvent.getOriginalSubtree();
		DataObject updatedSubTree = dataChangeEvent.getUpdatedSubtree();

		// If there is no updatedSubTree then the entire subtree was deleted.
		if ( updatedSubTree == null ) {
			
			if (      origSubTree instanceof Whitelist ) { handleWhitelistSubtreeRemoved(); } 
			else if ( origSubTree instanceof Netuser   ) { handleNetuserSubtreeRemoved() ; 	}
			
		} else {
			
			if (      updatedSubTree instanceof Whitelist) { handleWhitelistDataChanged(dataChangeEvent); } 
			else if ( updatedSubTree instanceof Netuser)   { handleNetuserDataChanged(dataChangeEvent);   }
			
		}
	}

	//---------------------------------------------------------------------------------------------
	private synchronized void handleWhitelistSubtreeRemoved() {
		
		final WriteTransaction deletePaths = dataBroker.newReadWriteTransaction();

		for ( Ipv4Address ipAddr : whitelistMap.keySet() ) { deletePathsForDestination( deletePaths, ipAddr ); }
        submitTransaction( deletePaths );
		
		whitelistMap = new HashMap<Ipv4Address, Ipv4Address>(10);
	}
	
	//---------------------------------------------------------------------------------------------
	private synchronized void handleNetuserSubtreeRemoved() {
		
		final WriteTransaction deletePaths = dataBroker.newReadWriteTransaction();

		for ( Ipv4Address ipAddr : netuserMap.keySet() ) {
			deletePathsForUser( deletePaths, ipAddr );
		}
        submitTransaction( deletePaths );

		netuserMap = new HashMap<Ipv4Address, Ipv4Address>(10);
	}
		
	//---------------------------------------------------------------------------------------------
	private synchronized void handleNetuserDataChanged( AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent ) {
		
		Map<InstanceIdentifier<?>, DataObject> createdObjectMap = dataChangeEvent.getCreatedData();
		Set<InstanceIdentifier<?>>             removedPathsMap  = dataChangeEvent.getRemovedPaths();
		
		log.info("Netuser changed ... ");

		//---- Handle new objects created in the tree.
		if ( createdObjectMap.size() > 0 ) {
			
			final WriteTransaction createPaths= dataBroker.newReadWriteTransaction();

			for ( InstanceIdentifier<?> objectKey : createdObjectMap.keySet() ) {
				
				DataObject obj = createdObjectMap.get(objectKey);

				if ( obj instanceof NetuserEntry ) {

					NetuserEntry entry = (NetuserEntry) obj;
					netuserMap.put( entry.getIpAddr(), entry.getIpAddr() );
					addPathsForUser( createPaths, entry.getIpAddr() );

				}
			}
	        submitTransaction( createPaths );

		}
		
		//---- Handle objects that have been removed from the tree.
		if ( removedPathsMap.size() > 0 ) {
			
			final WriteTransaction deletePaths = dataBroker.newReadWriteTransaction();
			
			Map<InstanceIdentifier<?>, DataObject> originalData = dataChangeEvent.getOriginalData();
			for ( InstanceIdentifier<?> object : removedPathsMap ) {
				
				NetuserEntry entry = (NetuserEntry) originalData.get(object);
				
				if ( entry != null ) {
					whitelistMap.remove( entry.getIpAddr() );
					deletePathsForUser( deletePaths, entry.getIpAddr() );
				}
			}
	        submitTransaction( deletePaths );
		}
	}

	
	//---------------------------------------------------------------------------------------------
	private synchronized void handleWhitelistDataChanged( AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dataChangeEvent) {
		
		Map<InstanceIdentifier<?>, DataObject> createdObjectMap = dataChangeEvent.getCreatedData();
		Set<InstanceIdentifier<?>>             removedPathsMap  = dataChangeEvent.getRemovedPaths();

		//---- Handle new objects created in the tree.
		if (createdObjectMap.size() > 0) {
			
			final WriteTransaction createPaths = dataBroker.newReadWriteTransaction();

			for ( InstanceIdentifier<?> objectKey : createdObjectMap.keySet() ) {
				DataObject obj = createdObjectMap.get(objectKey);

				if (obj instanceof WhitelistEntry) {

					WhitelistEntry entry = (WhitelistEntry) obj;
					
					whitelistMap.put( entry.getIpAddr(), entry.getIpAddr() );
					addPathsForDestination( createPaths, entry.getIpAddr() );

				}
			}
	        submitTransaction(createPaths);
	        
		}
		
		//---- Handle objects that have been removed from the tree.
		if (removedPathsMap.size() > 0) {
			
			Map<InstanceIdentifier<?>, DataObject> originalData = dataChangeEvent.getOriginalData();
			final WriteTransaction                 deletePaths  = dataBroker.newReadWriteTransaction();

			for ( InstanceIdentifier<?> object : removedPathsMap ) {
				
				WhitelistEntry entry = (WhitelistEntry) originalData.get(object);
				
				if ( entry != null ) {
					whitelistMap.remove(entry.getIpAddr());
					deletePathsForDestination(deletePaths, entry.getIpAddr());
				}

			}

	        submitTransaction(deletePaths);
		}
	}

	//---------------------------------------------------------------------------------------------
	/**
	 * Submits the async transaction and registers a simple callback for the CheckedFuture
	 * which logs success or failure.
	 * 
	 * The application specific semantics of a retry are beyond the scope of this lab.
	 * 
	 * @param deletePaths
	 */
	private void submitTransaction(final WriteTransaction writeTransaction ) {
		
		Futures.addCallback( writeTransaction.submit(), new FutureCallback<Void>() {
			
		   public void onSuccess( Void result ) {
		       log.info("BvcWhitelist transaction succeeded.");
		   }

		   public void onFailure( Throwable t ) {
			   
		      if( t instanceof OptimisticLockFailedException ) {
		    	  log.info("update paths transaction failed due to OptimisticLockFailedException.  No retries coded.");
		      } 
		      else {
		    	  log.info("update paths transaction failed due to another type of TransactionCommitFailedException.\n{}", t ) ;
		      }
		  }
      } ) ;
		
	}

	//---------------------------------------------------------------------------------------------
	private void deletePathsForDestination(final WriteTransaction writeTransaction,  final Ipv4Address destIp) {

		try {
			
			for (Ipv4Address sourceIp : netuserMap.keySet()) {
				
				Path path = buildPath(sourceIp, destIp);
				log.info("delete src={}, dest={}",sourceIp,destIp);
				
				InstanceIdentifier<Path> pathIID = 
							InstanceIdentifier.builder(Paths.class).child(Path.class, path.getKey()).toInstance();
				
				writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, pathIID);
			}
			
		} catch (Exception e) {
			log.error("Caught exception trying to put path entries {} ", e);
		}
	}

	//---------------------------------------------------------------------------------------------
	private void deletePathsForUser(final WriteTransaction writeTransaction, final Ipv4Address sourceIp) {

		try {
			
			for (Ipv4Address destIp : whitelistMap.keySet()) {
				
				Path path = buildPath(sourceIp, destIp);
				log.info("delete src={}, dest={}",sourceIp,destIp);
				
				InstanceIdentifier<Path> pathIID = 
							InstanceIdentifier.builder(Paths.class).child(Path.class, path.getKey()).toInstance();
				
				writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, pathIID);
			}
			
		} catch (Exception e) {
			log.error("Caught exception trying to put path entries {} ", e);
		}
	}

	//---------------------------------------------------------------------------------------------
	private void addPathsForUser( final WriteTransaction writeTransaction, 
			                      final Ipv4Address      source) {
		
		try {
			
			for( Ipv4Address destination : whitelistMap.keySet() ) {
				
				// Create bi-directional paths.
				Path pathSourceDest = buildPath( source, destination );
				Path pathDestSource = buildPath( destination, source );

				addPaths( writeTransaction, pathSourceDest, pathDestSource );
			}
			
		} catch (Exception e) {
			log.error("Caught exception trying to put path entries {} ", e);
		}
	}

	//---------------------------------------------------------------------------------------------
	private void addPathsForDestination( final WriteTransaction writeTransaction, 
			                             final Ipv4Address      destination) {
		
		try {
			
			for( Ipv4Address source : netuserMap.keySet() ) {
				
				// Create bi-directional paths.
				Path pathSourceDest = buildPath( source, destination );
				Path pathDestSource = buildPath( destination, source );

				addPaths( writeTransaction, pathSourceDest, pathDestSource );
 			}
			
		} catch (Exception e) {
			log.error("Caught exception  trying to put path entries {} ", e);
		}
	}

	//---------------------------------------------------------------------------------------------
	private Path buildPath(final Ipv4Address source, final Ipv4Address destination) {

		PathBuilder pathBuilder = new PathBuilder();
		
		PathKey pathKey = new PathKey( source.getValue() + "-" + destination.getValue() );
		pathBuilder.setKey(pathKey).setSourceAddr(source).setDestinationAddr(destination);

		return pathBuilder.build();
	}
	
	//---------------------------------------------------------------------------------------------
	private void addPaths ( final WriteTransaction writeTransaction, Path pathSourceDest, Path pathDestSource ) {
		
		// Add two path entries to path explorer model
		InstanceIdentifier<Path> pathIID;
		
		pathIID = InstanceIdentifier.builder(Paths.class).child(Path.class, pathSourceDest.getKey()).toInstance();
		writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, pathIID, pathSourceDest, true);
		
		pathIID = InstanceIdentifier.builder(Paths.class).child(Path.class, pathDestSource.getKey()).toInstance();
		writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, pathIID, pathDestSource, true);

	}
}
