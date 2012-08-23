package usr.events;

import usr.logging.*;
import java.lang.*;
import usr.globalcontroller.*;
import usr.engine.*;
import us.monoid.json.*;

/** Class represents a global controller event*/
public class StartRouterEvent extends Event {

    String address_= null;
    String name_= null;


    public StartRouterEvent (long time, EventEngine eng) {
        time_= time;
        engine_= eng;
    }

    public StartRouterEvent (long time, EventEngine eng,
         String address, String name) throws InstantiationException {
        time_= time;
        engine_= eng;
        name_= name;
        address_= address;
    }

    public String toString()
    {
        String str;
        str= "StartRouter: "+time_+" "+nameString();
        return str;
    }
    
    private String nameString()
    {
        String str="";
        if (name_ != null) {
            str+=" "+name_;
        }
        if (address_ != null) {
            str+=" "+address_;
        }
        return str;
    }

    public JSONObject execute(GlobalController gc) throws InstantiationException
    {
        int rNo= gc.startRouter(time_, address_, name_);
        JSONObject json= new JSONObject();
        try {
            if (rNo < 0) {
                json.put("success",false);
                json.put("msg","Could not create router");
            } else {
                json.put("success",true);
                json.put("router",(Integer)rNo);
                json.put("msg","Created router "+rNo+" "+nameString());
                if (name_ != null) {
                    json.put("name",name_);
                }                
                if (address_ != null) {
                    json.put("address",address_);
                }
            }
        } catch (JSONException je) {
            Logger.getLogger("log").logln(USR.ERROR,
                "JSONException in StartLinkEvent should not occur");           
        }
        return json;
    }
    
}
