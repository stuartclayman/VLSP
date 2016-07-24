package usr.events;

import usr.engine.EventEngine;
import us.monoid.json.JSONObject;
import us.monoid.json.JSONException;

/** Class represents a global controller event*/
public abstract class AbstractExecutableEvent extends AbstractEvent implements ExecutableEvent {
    /**
     * Important constructor to create Event.
     */
    protected AbstractExecutableEvent(long t, EventEngine eng) {
        super(t, eng);
    }
 
    /**
     * Important constructor to create Event - with extra parameters.
     */
    protected AbstractExecutableEvent(long t, EventEngine eng, String parameters) {
        super(t, eng, parameters);
    }
       
    /** Execute the event, pass in a context object, and return a JSON object with information*/
    @Override
    public  JSONObject execute(EventDelegate ed, Object obj) {
        setContextObject(obj);

        JSONObject jsobj = eventBody(ed);

        //System.out.println("AbstractExecutableEvent" + " result = " + jsobj);

        return jsobj;
    }

    /**
     * The main body of the event.
     * This is called by execute.
     */
    public abstract JSONObject eventBody(EventDelegate ed);

    /**
     * Create a JSON failure msg.
     * Sets success to false
     */
    protected JSONObject fail(String msg) {
        JSONObject json = new JSONObject();

        try {
            json.put("success", false);
            json.put("msg", msg);
        } catch (JSONException e) {
        }
        
        return json;
    }




}
