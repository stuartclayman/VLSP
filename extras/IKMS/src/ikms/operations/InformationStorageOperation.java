package ikms.operations;

import ikms.data.DataStoreManager;
import ikms.info.EntitySubscribeListener;
import ikms.info.EntitySubscribeRestListener;
import ikms.util.LoggerFrame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;
import us.monoid.json.JSONObject;

// The collected/shared information from/through the ICD function is optionally stored in the Information Storage. After this 
// stage, the information could be passed back to the ICD function for dissemination. 
// Information could be alternatively stored after the end of an information aggregation or knowledge production operation.
// In case the information is requested through an information retrieval operation, it is fetched from the storage and communicated 
// to the requesting entity through the ICD. 

public class InformationStorageOperation {
	// The DataStoreManager
	//DataStoreManager dataStoreManager;	
	ExecutorService executorService;

	HashMap<String, EntitySubscribeListener> activeSubscribeListeners = new HashMap<String, EntitySubscribeListener>();

	public InformationStorageOperation () {
		//dataStoreManager = dataStoreManager_;
		executorService = Executors.newCachedThreadPool();

	}

	public void ShutdownPubSubThreads () {
		executorService.shutdownNow();
	}

	public String GetInformationFromMainStorage (String uri) {
		String value = DataStoreManager.IKMSDBGet("Storage://"+uri);
		System.out.println ("Fetching uri:"+uri+" from main storage, value:"+value);
		if (value==null) {
			value = SearchBreakingDownUri ("Storage://"+uri);
		}
		return value;
	}

	public String StoreInformationInMainStorage (String uri, JSONObject value) {
		String output = DataStoreManager.IKMSDBSet("Storage://"+uri, value.toString());
		System.out.println ("Storing uri:"+uri+" in main storage, value:"+value+" output:"+output);

		return output;		
	}

	public long PublishInformationToMainStorage (String uri, JSONObject value) {
		long output = DataStoreManager.IKMSDBPublish("Storage://"+uri, value.toString());
		System.out.println ("Publishing uri:"+uri+" in main storage, value:"+value+" output:"+output);
		return output;		
	}

	// subscribe for a uri
	public void SubscribeForAnInformationToMainStorage (final String cburi, final String uri, final int entityid) {
		try {
			//String value = knowdb.get("Storage://"+uri);
			//System.out.println ("Fetched uri:"+uri+" from main storage, value:"+value);

			// we need to find the same address as knowdb
			final Client jedisClient = DataStoreManager.IKMSDBGetClient();

			// check it if is subscribed already
			// get the future object
			EntitySubscribeListener listener = activeSubscribeListeners.get(uri);
			if (listener==null) {
				// creating listener
				final EntitySubscribeListener newListener = new EntitySubscribeRestListener(cburi, entityid);
				// keeping listener in hashmap
				activeSubscribeListeners.put(uri, newListener);
				// if not, subscribe
				System.out.println ("Subscribing listener for uri:"+uri);

				executorService.submit(new Runnable() {
					public void run() {
						Jedis subscriberJedis = new Jedis(jedisClient.getHost(), jedisClient.getPort());
						try {
							subscriberJedis.subscribe(newListener, "Storage://"+ uri);
						} catch (Exception e) {
							e.printStackTrace();
						};
						System.out.println ("Subscribe listener for uri:"+uri + " TERMINATED");
					}
				});

				System.out.println ("Subscribed with listener internal: Storage://"+uri);
			}
		} catch (Exception e) {
			System.out.println ("Subscribing listener for uri:"+uri + " FAILED");
			e.printStackTrace();
		}
	}

	// subscribe for multiple uris
	public void SubscribeForAnInformationToMainStorage (final String cburi, final ArrayList<String> uris, final int entityid) {
		//String value = knowdb.get("Storage://"+uri);
		//System.out.println ("Fetched uri:"+uri+" from main storage, value:"+value);

		// we need to find the same address as knowdb
		final Client jedisClient = DataStoreManager.IKMSDBGetClient();

		for (final String uri:uris) {
			try {

				// check it if is subscribed already
				// get the future object
				EntitySubscribeListener listener = activeSubscribeListeners.get(uri);
				if (listener==null) {
					// creating listener
					final EntitySubscribeListener newListener = new EntitySubscribeRestListener(cburi, entityid);
					// keeping listener in hashmap
					activeSubscribeListeners.put(uri, newListener);
					// if not, subscribe
					System.out.println ("Subscribing listener for uri:"+uri);

					executorService.submit(new Runnable() {
						public void run() {
							Jedis subscriberJedis = new Jedis(jedisClient.getHost(), jedisClient.getPort());
							try {
								subscriberJedis.subscribe(newListener, "Storage://"+ uri);
							} catch (Exception e) {
								e.printStackTrace();
							};
							System.out.println ("Subscribe listener for uri:"+uri + " TERMINATED");
						}
					});

					System.out.println ("Subscribed with listener internal: Storage://"+uri);
				}
			} catch (Exception e) {
				System.out.println ("Subscribing listener for uri:"+uri + " FAILED");
				e.printStackTrace();
			}
		}
	}

	// unsubscribe one uri
	public void UnsubscribeInformationFromMainStorage (final String uri) {
		// unsubscribing in REDIS
		// we need to find the same address as knowdb
		final Client jedisClient = DataStoreManager.IKMSDBGetClient();			
		jedisClient.unsubscribe("Storage://"+ uri);

		try {
			System.out.println ("Unsubscribing listener for uri:"+uri);
			// get existing listener
			EntitySubscribeListener listener = activeSubscribeListeners.get(uri);
			if (listener!=null) {
				// unsubscribe listener
				listener.unsubscribe();
				// remove listener from hashmap
				activeSubscribeListeners.remove(uri);
			}
			System.out.println ("Unsubscribed listener with uri:"+uri);
		} catch (Exception e) {
			System.out.println ("Subscribing listener for uri:"+uri + " FAILED");
			e.printStackTrace();
		}
	}

	// unsubscribe multiple uris
	public void UnsubscribeInformationFromMainStorage (final ArrayList<String> uris) {
		// unsubscribing in REDIS
		// we need to find the same address as knowdb
		final Client jedisClient = DataStoreManager.IKMSDBGetClient();	

		for (String uri:uris) {
			jedisClient.unsubscribe("Storage://"+ uri);

			try {
				System.out.println ("Unsubscribing listener for uri:"+uri);
				// get existing listener
				EntitySubscribeListener listener = activeSubscribeListeners.get(uri);
				if (listener!=null) {
					// unsubscribe listener
					listener.unsubscribe();
					// remove listener from hashmap
					activeSubscribeListeners.remove(uri);
				}
				System.out.println ("Unsubscribed listener with uri:"+uri);
			} catch (Exception e) {
				System.out.println ("Subscribing listener for uri:"+uri + " FAILED");
				e.printStackTrace();
			}
		}
	}

	public void SubscribeForAnInformationSet (int entityid, String cburi, ArrayList<String> uris) {
		@SuppressWarnings("unused")
		Boolean logoutput;

		for (String uri : uris) {

			// Logging for internal IKMS functions workflow diagram
			logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Subscribing for information with uri:"+uri, "ikmsfunctions");				

			SubscribeForAnInformationToMainStorage(cburi, uri, entityid);
		}
	}

	public void UnsubscribeForAnInformationSet (int entityid, ArrayList<String> uris) {
		@SuppressWarnings("unused")
		Boolean logoutput;

		//ArrayList<String> urisToUnsubscribe = new ArrayList<String>();
		for (String uri : uris) {
			// Logging for internal IKMS functions workflow diagram
			logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Unsubscribing information with uri:"+uri, "ikmsfunctions");				
		}
		UnsubscribeInformationFromMainStorage (uris);

	}

	public String StoreInformationSetInMainStorage (ArrayList<String> uris, ArrayList<JSONObject> values) {
		return null;
	}

	public ArrayList<String> GetInformationSetFromStorage (ArrayList<String> uris) {
		return null;
	}

	String SearchBreakingDownUri (String uri) {		
		String[] uris = uri.split("/");
		String result="";
		String value=null;

		for( int i = 0; i <= uris.length - 1; i++)
		{
			if (result=="Storage:")
				result+="/";

			if (!result.equals(""))
				result+="/";

			result+=uris[i];
			//System.out.println("Searching URI:"+result+"/All");
			value = DataStoreManager.IKMSDBGet(result+"/All");
			if (value!=null) {
				return value;
			}
		}
		return null;
	}
}
