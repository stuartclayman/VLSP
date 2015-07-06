package ikms.data;

import ikms.IKMS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;

import com.timeindexing.index.IndexView;

/**
 * The DataStoreManager acts as a gateway to managers
 * of different data stores.
 */
public class DataStoreManager {
	// A SINGLETON
	static DataStoreManager singleton  = null;

	// The IKMS this is DataStoreManager for.
	IKMS ikms;

	// The TimeIndexManager which manages timeindexing
	TimeIndexManager tiManager = null;

	// The KeyValueManager manages access to Redis
	KeyValueManager kvManager = null;

	/**
	 * DataStoreManager Constructor.
	 */
	public DataStoreManager(IKMS ikms, String dbHost, String dbPassword) {
		singleton = this;

		this.ikms = ikms;

		tiManager = new TimeIndexManager();
		kvManager = new KeyValueManager(dbHost, dbPassword);
	}


	/**
	 * Get access to a TimeIndex
	 */
	public static IndexView getTimeIndex(String name) {
		DataStoreManager mgr = singleton;

		if (mgr != null) {
			return mgr.tiManager.getIndex(name);
		} else {
			throw new IllegalStateException("DataStoreManager not configured correctly");
		}

	}


	/**
	 * Entry point to list Indexes
	 */
	public Collection<IndexView> listIndexes() {
		return tiManager.listIndexes();
	}



	/**
	 * Get access to a KeyValue store
	 */
	private static Jedis getKeyValueStore() {

		DataStoreManager mgr = singleton;

		Jedis result = null;
		while (result==null) {

			if (mgr != null) {
				try {
					result = mgr.kvManager.getKeyValueStore();
				} catch (Throwable t) {

				}
				// It breaks some times, in that case, try again
				int delay=1000;

				if (result == null) {
					System.out.println ("Problem in getKeyValueStore. Trying again after "+delay+"ms");
					try {
						Thread.sleep(delay);
						// increase delay for next time
						delay = 2 * delay;
					} catch (InterruptedException ie) {
						ie.printStackTrace();
					}					
				} else {
					//System.out.println ("GetKeyValueStore completed.");
					break;
				}
			} else {
				throw new IllegalStateException("DataStoreManager not configured correctly");
			}
		}
		return result;
	}

	public static boolean FlushKeyValueStore () {
		Jedis ikmsdb = DataStoreManager.getKeyValueStore();
		if (ikmsdb==null)
			return false;

		try {
			ikmsdb.flushAll();
			DataStoreManager.releaseKeyValueStore(ikmsdb);

		} finally {
			DataStoreManager.releaseKeyValueStore(ikmsdb);
		}
		return true;
	}

	public static String GetIKMSDBInfo () {
		Jedis ikmsdb = DataStoreManager.getKeyValueStore();
		String output = null;
		try {
			output = ikmsdb.info();
			DataStoreManager.releaseKeyValueStore(ikmsdb);

			return output;
		} finally {
			DataStoreManager.releaseKeyValueStore(ikmsdb);
		}
	}

	// read a value from storage using the corresponding key
	public static String IKMSDBGet (String key) {
		Jedis ikmsdb = DataStoreManager.getKeyValueStore();
		String output = null;
		try {
			output = ikmsdb.get(key);
			DataStoreManager.releaseKeyValueStore(ikmsdb);

			return output;
		} finally {
			DataStoreManager.releaseKeyValueStore(ikmsdb);
		}
	}

	// add a key-value pair to storage
	public static String IKMSDBSet (String key, String value) {
		Jedis ikmsdb = DataStoreManager.getKeyValueStore();
		String output = null;
		try {
			output = ikmsdb.set(key, value);
			DataStoreManager.releaseKeyValueStore(ikmsdb);

			return output;
		} finally {
			DataStoreManager.releaseKeyValueStore(ikmsdb);
		}
	}

	// add multiple key-value pairs to storage
	public static String IKMSDBSet (ArrayList<String> keys, ArrayList<String> values) {
		Jedis ikmsdb = DataStoreManager.getKeyValueStore();
		String output = "";
		try {
			for (String key:keys) {
				output += ikmsdb.set(key, values.get(keys.indexOf(key)));
			}

			DataStoreManager.releaseKeyValueStore(ikmsdb);

			return output;
		} finally {
			DataStoreManager.releaseKeyValueStore(ikmsdb);
		}
	}

	// add multiple keys with the same value to storage
	public static String IKMSDBSet (ArrayList<String> keys, String value) {
		Jedis ikmsdb = DataStoreManager.getKeyValueStore();
		String output = "";
		try {
			for (String key:keys) {
				output += ikmsdb.set(key, value);
			}

			DataStoreManager.releaseKeyValueStore(ikmsdb);

			return output;
		} finally {
			DataStoreManager.releaseKeyValueStore(ikmsdb);
		}
	}

	// remove a key-value pair from storage
	public static long IKMSDBDel (String key) {
		Jedis ikmsdb = DataStoreManager.getKeyValueStore();
		long output = -1;
		try {
			output = ikmsdb.del(key);
			DataStoreManager.releaseKeyValueStore(ikmsdb);

			return output;
		} finally {
			DataStoreManager.releaseKeyValueStore(ikmsdb);
		}
	}

	// remove multiple key-value pairs from storage
	public static long IKMSDBDel (ArrayList<String> keys) {
		Jedis ikmsdb = DataStoreManager.getKeyValueStore();
		long output = -1;
		try {
			for (String key:keys) {
				output = ikmsdb.del(key);
			}
			DataStoreManager.releaseKeyValueStore(ikmsdb);

			return output;
		} finally {
			DataStoreManager.releaseKeyValueStore(ikmsdb);
		}
	}

	public static Set<String> IKMSDBKeys (String key) {
		Jedis ikmsdb = DataStoreManager.getKeyValueStore();
		Set<String> output = null;
		try {
			output = ikmsdb.keys(key);
			DataStoreManager.releaseKeyValueStore(ikmsdb);

			return output;
		} finally {
			DataStoreManager.releaseKeyValueStore(ikmsdb);
		}
	}

	public static Client IKMSDBGetClient() {
		Jedis ikmsdb = DataStoreManager.getKeyValueStore();
		Client output = null;
		try {
			output = ikmsdb.getClient();
			DataStoreManager.releaseKeyValueStore(ikmsdb);

			return output;
		} finally {
			DataStoreManager.releaseKeyValueStore(ikmsdb);
		}
	}

	public static long IKMSDBPublish (String key, String value) {
		Jedis ikmsdb = DataStoreManager.getKeyValueStore();
		long output=0;
		try {
			output = ikmsdb.publish(key, value);
			DataStoreManager.releaseKeyValueStore(ikmsdb);

			return output;
		} finally {
			DataStoreManager.releaseKeyValueStore(ikmsdb);
		}
	}

	/**
	 * Destroy KeyValue pool
	 */
	public static void destroyKeyValueStorePool() {		
		DataStoreManager mgr = singleton;

		if (mgr != null) {
			mgr.kvManager.destroyKeyValueStorePool();
		} else {
			throw new IllegalStateException("DataStoreManager not configured correctly");
		}

	}

	/**
	 * Release a KeyValue store
	 */
	private static void releaseKeyValueStore(Jedis ikmsdb) {
		DataStoreManager mgr = singleton;

		if (mgr != null) {
			mgr.kvManager.releaseKeyValueStore(ikmsdb);
		} else {
			throw new IllegalStateException("DataStoreManager not configured correctly");
		}
	}
}
