package ikms.util;

import us.monoid.json.*;

/**
 * SOme JSONData
 */
public class JSONData extends JSONObject implements JSONString {

    public JSONData() throws JSONException {
        super();
    }

    public JSONData(String s) throws JSONException {
        super(s);
    }

    public JSONData(JSONObject js) throws JSONException {
        super(js, JSONObject.getNames(js));
        
    }


    public String toJSONString() {
        return super.toString();
    }
}
