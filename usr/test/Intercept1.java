package usr.test;

import us.monoid.json.JSONObject;

/**
 * Test some calls to GlobalController using Resty
 */
class Intercept1 extends RestyTest {

    public static void main(String[] args) {
        try {
            RestyTest1 test = new RestyTest1();

            JSONObject r1 = test.createRouter();
            System.out.println("r1 = " + r1);
            Thread.sleep(500);

            int router1 = r1.getInt("routerID");

            JSONObject r2 = test.createRouter();
            System.out.println("r2 = " + r2);
            Thread.sleep(500);

            int router2 = r2.getInt("routerID");

            JSONObject r3 = test.createRouter();
            System.out.println("r3 = " + r3);
            Thread.sleep(500);

            int router3 = r3.getInt("routerID");

            JSONObject l1 = test.createLink(router1, router2, 1);
            System.out.println("l1 = " + l1);
            Thread.sleep(500);

            int link1 = l1.getInt("linkID");

            JSONObject l2 = test.createLink(router2, router3, 1);
            System.out.println("l2 = " + l2);
            Thread.sleep(500);

            int link2 = l2.getInt("linkID");


            /* sleep 15 seconds */
            Thread.sleep(15000);

            JSONObject a0 = test.createApp(router2, "usr.applications.Echo", "this -is a /path/of/death to http://host:port/path?args=0&other=1");
            System.out.println("a0 = " + a0);
            Thread.sleep(500);

            JSONObject ad1 = test.createApp(router2, "usr.test.InterceptApp", Integer.toString(router1));
            System.out.println("ad1 = " + ad1);
            Thread.sleep(500);

            JSONObject a1 = test.createApp(router3, "usr.applications.Recv", "4000");
            System.out.println("a1 = " + a1);
            Thread.sleep(500);

            JSONObject a2 = test.createApp(router1, "usr.applications.Send", router3 + " 4000 10000 -i 5 -b 20");  // id 4000 count
            System.out.println("a2 = " + a2);


            /* sleep 60 seconds - 1 minute */
            Thread.sleep(60000);

            // delete Links
            JSONObject l1D = test.deleteLink(link1);
            System.out.println("l1D = " + l1D);
            Thread.sleep(1500);

            JSONObject l2D = test.deleteLink(link2);
            System.out.println("l2D = " + l2D);
            Thread.sleep(1500);

            // delete Routers
            JSONObject r1D = test.deleteRouter(router1);
            System.out.println("r1D = " + r1D);
            Thread.sleep(1500);

            JSONObject r2D = test.deleteRouter(router2);
            System.out.println("r2D = " + r2D);
            Thread.sleep(1000);

            JSONObject r3D = test.deleteRouter(router3);
            System.out.println("r3D = " + r3D);
            Thread.sleep(1000);

        } catch (Exception e) {
        }
    }

}
