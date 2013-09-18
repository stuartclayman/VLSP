package usr.test;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONObject;

/**
 * Test some calls to GlobalController using Resty
 */
class RestyDelete extends RestyTest {

    public static void main(String[] args) {
        try {
            RestyDelete test = new RestyDelete();

            JSONObject jsobj = test.listRouters();
            JSONArray array = jsobj.getJSONArray("list");

            int length = array.length();

            for (int off=0; off < length; off++) {
                int id = array.getInt(off);

                System.err.println("Deleting router: " + id);

                test.deleteRouter(id);
            }
        } catch (Exception e) {
        }
    }

}
