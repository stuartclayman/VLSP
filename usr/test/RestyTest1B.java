package usr.test;

import us.monoid.json.JSONObject;

/**
 * Test some calls to GlobalController using Resty
 */
class RestyTest1B extends RestyTest {

    public static void main(String[] args) {
        try {
            RestyTest1B test = new RestyTest1B();

            JSONObject r1 = test.createRouter();
            System.out.println("r1 = " + r1);
            Thread.sleep(500);

            int router1 = (Integer)r1.get("routerID");

            JSONObject r2 = test.createRouter();
            System.out.println("r2 = " + r2);
            Thread.sleep(500);

            int router2 = (Integer)r2.get("routerID");

            JSONObject l1 = test.createLink(router1, router2, 1);
            System.out.println("l1 = " + l1);
            Thread.sleep(500);

            int link1 = (Integer)l1.get("linkID");


            /* sleep 60 seconds - 1 minute */
            Thread.sleep(60000);

            JSONObject a0 = test.createApp(router2, "usr.applications.Echo", "this -is a /path/of/death to http://host:port/path?args=0&other=1");
            System.out.println("a0 = " + a0);
            Thread.sleep(500);

            JSONObject a1 = test.createApp(router2, "usr.applications.Recv", "4000");
            System.out.println("a1 = " + a1);
            Thread.sleep(500);

            JSONObject a2 = test.createApp(router1, "usr.applications.Send", router2 + " 4000 1000 -i 1 -b 20");  // id 4000 count
            System.out.println("a2 = " + a2);

            Thread.sleep(60000);

            JSONObject a3 = test.createApp(router1, "usr.applications.Recv", "4000");
            System.out.println("a3 = " + a3);
            Thread.sleep(500);

            JSONObject a4 = test.createApp(router2, "usr.applications.Send", router1 + " 4000 1000 -i 1 -b 20");  // id 4000 count
            System.out.println("a4 = " + a4);


            /* sleep 60 seconds - 1 minute */
            Thread.sleep(60000);


            JSONObject l1D = test.deleteLink(link1);
            System.out.println("l1D = " + l1D);
            Thread.sleep(1500);

            JSONObject r2D = test.deleteRouter(router2);
            System.out.println("r2D = " + r2D);
            Thread.sleep(1000);

            JSONObject r1D = test.deleteRouter(router1);
            System.out.println("r1D = " + r1D);
            Thread.sleep(1500);

        } catch (Exception e) {
        }
    }

}
