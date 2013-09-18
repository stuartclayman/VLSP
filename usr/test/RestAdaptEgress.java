package usr.test;

import us.monoid.json.JSONObject;

/**
 * Test some calls to GlobalController using Resty
 */
class RestAdaptEgress extends RestyTest {

    public static void main(String[] args) {
        try {
            RestAdaptEgress test = new RestAdaptEgress();

            int router1 = 1;

            // start demo_usr.paths.AdaptEgress 2:14000 localhost:6655

            String usrAddr = args[0];
            String udpAddr = args[1];

            // on router2, Egress on port 4000 send to UDP localhost:8856
            JSONObject a1 = test.createApp(router1, "demo_usr.paths.AdaptEgress", usrAddr + "+" + udpAddr + "+-v");
            System.out.println("a1 = " + a1);


        } catch (Exception e) {
        }
    }

}
