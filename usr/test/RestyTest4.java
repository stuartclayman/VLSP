package usr.test;

import us.monoid.json.JSONObject;

/**
 * Test some calls to GlobalController using Resty
 */
class RestyTest4 extends RestyTest {

    public static void main(String[] args) {
        try {
            RestyTest4 test = new RestyTest4();

            JSONObject r1 = test.createRouter();
            int router1 = (Integer)r1.get("routerID");
            System.out.println("r1 = " + r1);

            JSONObject r2 = test.createRouter();
            int router2 = (Integer)r2.get("routerID");
            System.out.println("r2 = " + r2);




            JSONObject l1 = test.createLink(router1, router2, 10);
            l1.get("linkID");
            System.out.println("l1 = " + l1);

            // let the routing tables propogate
            Thread.sleep(1000);


            // on router2, Egress on port 4000 send to UDP localhost:8856
            JSONObject a1 = test.createApp(router2, "demo_usr.paths.Egress", "4000 localhost:8856 -v");
            System.out.println("a1 = " + a1);
            Thread.sleep(500);

            // listen on UDP port 8855
            // send from router1 to router2 on port 4000
            JSONObject a2 = test.createApp(router1, "demo_usr.paths.Ingress", "8855 " + router2 + ":4000 -b 64 -v"); 
            System.out.println("a2 = " + a2);




            /*
            Thread.sleep(300000);

            JSONObject r1D = test.deleteRouter(router1);

            JSONObject r2D = test.deleteRouter(router2);
            */

        } catch (Exception e) {
        }
    }

}
