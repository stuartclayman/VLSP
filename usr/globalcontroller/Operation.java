package usr.globalcontroller;

import java.util.concurrent.Callable;

import us.monoid.json.JSONObject;

/**
 * An Operation that has a call() method
 * and returns a JSONObject.
 */
public interface Operation extends Callable<JSONObject> {

}
