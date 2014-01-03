package ikms.data;

import org.apache.commons.pool.impl.GenericObjectPool;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;


public class KeyValueManager {
	// A Redis key-value database
	JedisPool redis = null;

	String dbHost="localhost";
	String dbPassword="";
		
	public KeyValueManager(String dbHost_, String dbPassword_) {
		JedisPoolConfig config = new JedisPoolConfig();
		dbHost = dbHost_;
		dbPassword = dbPassword_;
		//config.setMaxActive(30000);
		//config.setMaxIdle(-1);
		config.setTestOnBorrow(true);
		//config.setTestOnReturn(true);

		//config.setMinIdle(1);
		//config.setTestWhileIdle(true);
		config.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_GROW);
		//config.setMinIdle(200);
		//config.setMaxIdle(400);
		//config.setMaxWait(1000);
		redis = new JedisPool(config, dbHost);
	}

	/**
	 * Get the KeyValue store.
	 */
	public Jedis getKeyValueStore() {
		try {
			Jedis jedis = redis.getResource();

			if (! dbPassword.equals(""))
				jedis.auth(dbPassword);
	
			return redis.getResource();
		} catch (redis.clients.jedis.exceptions.JedisConnectionException jce) {
			System.out.println(jce.getMessage());
			//System.exit(0);
			return null;
		}
	}
	
	public void releaseKeyValueStore(Jedis knowdb) {
		try {
			redis.returnResource(knowdb);
		} catch (redis.clients.jedis.exceptions.JedisConnectionException jce) {
			//jce.printStackTrace();
			System.out.println(jce.getMessage());
//			System.exit(0);
		}
	}
	
	public void destroyKeyValueStorePool() {
		try {
			redis.destroy();
		} catch (redis.clients.jedis.exceptions.JedisConnectionException jce) {
			//jce.printStackTrace();
			System.out.println(jce.getMessage());
//			System.exit(0);
		}
	}
}
