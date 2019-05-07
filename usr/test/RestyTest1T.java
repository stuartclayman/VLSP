package usr.test;

import us.monoid.json.JSONObject;

/**
 * Test some calls to GlobalController using Resty
 * Try simple TCPClient and Server
 */
class RestyTest1T extends RestyTest {

    public static void main(String[] args) {
        try {
            RestyTest1T test = new RestyTest1T();

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


            /* sleep 10 seconds */
            Thread.sleep(10000);

            JSONObject a1 = test.createApp(router2, "usr.applications.TCPServer", "6789");
            System.out.println("a1 = " + a1);
            Thread.sleep(500);

            JSONObject a2 = test.createApp(router1, "usr.applications.TCPClient", router2 + " " + "6789");  // id 6789
            System.out.println("a2 = " + a2);


            /* sleep 60 seconds */
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
