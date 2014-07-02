package ikms.info;

import static us.monoid.web.Resty.content;
import ikms.util.LoggerFrame;

import java.io.IOException;
import java.util.Arrays;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;
// Pub/sub listener

/**
 * A custom listener which calls a remote Entity with a value
 */
public class EntitySubscribeRestListener extends EntitySubscribeListener {
    // The URL to callback to when Redis does a publish
    String callbackURL;

    /**
     * Construct an EntitySubscribeRestListener given a callback URL.
     */
    public EntitySubscribeRestListener(String url, int entityid) {
        System.out.println("EntitySubscribeRestListener: URL = " + url);
        callbackURL = url;
        entityID = entityid;
    }
    
    public EntitySubscribeRestListener(String url) {
        System.out.println("EntitySubscribeRestListener: URL = " + url);
        callbackURL = url;
    }

    public void onMessage(String channel, String message) {
    		//Boolean logoutput;
    	
    		// Channel == Storage:///VIM/Removed/
        String uri = channel.replaceFirst("Storage://", "");
    		
        System.out.println ("EntitySubscribeRestListener onMessage channel:"+channel+" message "+message+" url:"+callbackURL+uri);

        // Logging for internal IKMS functions workflow diagram
        //logoutput = LoggerFrame.testlog(LoggerFrame.ISIInfoStorageName, LoggerFrame.ICDFunctionName, "Updated subscribed information, uri:"+uri+" changed", "black");
        LoggerFrame.workflowvisualisationlog(entityID, LoggerFrame.ISIName, LoggerFrame.ICDFunctionName, "Subscribed information with uri:"+uri+" changed", "ikmsfunctions");
  
        // Logging for internal IKMS functions workflow diagram
        LoggerFrame.workflowvisualisationlog(entityID, LoggerFrame.ICDFunctionName, LoggerFrame.InformationExchangeName, "Notifying subscribed entity", "ikmsfunctions");
 
        // Logging for internal IKMS functions workflow diagram
        LoggerFrame.workflowvisualisationlog(entityID, LoggerFrame.IKMSName, "", "Updated information received", "entities2ikms");
        
        // We  have code here that sends update information to an entity
        // using a REST interface on the entity
        try {
            // Make a Resty connection
            Resty rest = new Resty();

            //  http://localhost:9110/update/VIM/Removed/
            String callURL = callbackURL + "&u="+uri;

            System.out.println ("url:"+callURL);
            
            // Call the relevant URL where the content is the message
            JSONObject jsobj = rest.json(callURL, content(message)).toObject();

            System.out.println ("EntitySubscribeRestListener received: " + jsobj.toString());
          
        }  catch (IOException ioe) {
            //ioe.printStackTrace();
        		System.out.println ("Cannot notificate for information change, probably target ME stopped running.");
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    public void onSubscribe(String channel, int subscribedChannels) {
        System.out.println ("EntitySubscribeRestListener onSubscribe channel:"+channel+" subscribedChannels "+subscribedChannels);
    }

    public void onUnsubscribe(String channel, int subscribedChannels) {
        System.out.println ("EntitySubscribeRestListener onUnsubscribe channel:"+channel+" subscribedChannels "+subscribedChannels);
    }

    public void onPSubscribe(String pattern, int subscribedChannels) {
        System.out.println ("EntitySubscribeRestListener onPSubscribe pattern:"+pattern+" subscribedChannels "+subscribedChannels);
    }

    public void onPUnsubscribe(String pattern, int subscribedChannels) {
        System.out.println ("EntitySubscribeRestListener onPUnsubscribe pattern:"+pattern+" subscribedChannels "+subscribedChannels);
    }

    public void onPMessage(String pattern, String channel, String message) {
        System.out.println ("EntitySubscribeRestListener onPMessage pattern:"+pattern +" channel: " + channel+" message "+message);
    }

    public void unsubscribe() {
        System.out.println ("EntitySubscribeRestListener unsubscribe");
        super.unsubscribe();
    }

    public void unsubscribe(String args) {
        System.out.println ("EntitySubscribeRestListener unsubscribe" + args);
        super.unsubscribe(args);
    }

    public void subscribe(String args) {
        System.out.println ("EntitySubscribeRestListener subscribe" + args);
        super.subscribe(args);
    }

 
    public void psubscribe(String args) {
        System.out.println ("EntitySubscribeRestListener psubscribe" + args);
        super.psubscribe(args);
    }

 
    public void punsubscribe() {
        System.out.println ("EntitySubscribeRestListener punsubscribe");
        super.punsubscribe();
    }


    public void punsubscribe(String args) {
        System.out.println ("EntitySubscribeRestListener punsubscribe" + args);
        super.punsubscribe(args);
    }

 
    public boolean isSubscribed() {
        System.out.println ("EntitySubscribeRestListener isSubscribed");
        boolean result = super.isSubscribed();
        return result;
    }


    public void proceedWithPatterns(redis.clients.jedis.Client client, String args) {
        System.out.println ("EntitySubscribeRestListener proceedWithPatterns" + client + " and " + args);
        super.proceedWithPatterns(client, args);
    }

 
    public void proceed(redis.clients.jedis.Client client, String args) {
        System.out.println ("EntitySubscribeRestListener proceed " + client + " and " + Arrays.asList(args));
        super.proceed(client, args);

        System.out.println ("EntitySubscribeRestListener proceed RETURN");
    }

 
    public int getSubscribedChannels() {
        System.out.println ("EntitySubscribeRestListener getSubscribedChannels");
        return super.getSubscribedChannels();
    }

}
