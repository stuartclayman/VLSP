package usr.test;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONObject;

/**
 * Test some calls to GlobalController using Resty
 */
class RestyDelete2 extends RestyTest {

    public static void main(String[] args) {
        try {
            RestyDelete2 test = new RestyDelete2();

            JSONObject jsobj = test.listRouters();
            JSONArray array = jsobj.getJSONArray("list");

            System.err.println("Routers: " + array);

            int id = 3;

            System.err.println("Deleting router: " + id);

            test.deleteRouter(id);

        } catch (Exception e) {
        }
    }

}
