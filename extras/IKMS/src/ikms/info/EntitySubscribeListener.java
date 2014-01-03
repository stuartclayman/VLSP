package ikms.info;

import redis.clients.jedis.JedisPubSub;

// Pub/sub listener
// We could create custom listeners doing more sophisticated subscriptions (e.g., whenever a value exceeds a threshold etc). 

public class EntitySubscribeListener extends JedisPubSub {

	int entityID;
	
	public void onMessage(String channel, String message) {
		System.out.println ("Information received uri:"+channel+" value "+message);
	}

    public void onSubscribe(String channel, int subscribedChannels) {
    }

    public void onUnsubscribe(String channel, int subscribedChannels) {
    }

    public void onPSubscribe(String pattern, int subscribedChannels) {
    }

    public void onPUnsubscribe(String pattern, int subscribedChannels) {
    }

    public void onPMessage(String pattern, String channel,
        String message) {
    }
    
    // Returns the entityID the listener is associated to
    public int getEntityID () {
    		return entityID;
    }
}
