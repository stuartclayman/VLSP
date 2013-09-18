package usr.test;

import us.monoid.json.JSONObject;

/**
 * Test some calls to GlobalController using Resty
 */
class RestAdaptForward extends RestyTest {

    public static void main(String[] args) {
        try {
            RestAdaptForward test = new RestAdaptForward();

            int router1 = 1;

            // start demo_usr.paths.RestAdaptForward 2:24000 2:4000

            String mgmtUsrAddr = args[0];
            String usrAddr = args[1];

            // on router2, Egress on port 4000 send to UDP localhost:8856
            JSONObject a1 = test.createApp(router1, "demo_usr.paths.AdaptForward", mgmtUsrAddr + "+" + usrAddr + "+-v");
            System.out.println("a1 = " + a1);


        } catch (Exception e) {
        }
    }

}
