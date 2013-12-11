package usr.test;

import us.monoid.json.JSONObject;
import usr.vim.VimClient;

/**
 * Test some calls to GlobalController using Resty
 */
class Teemu2 extends RestyTest {

    public static void main(String[] args) {
        try {
            VimClient test = new VimClient();

            JSONObject rSRC = test.createRouter();
            int routerS = (Integer)rSRC.get("routerID");
            System.out.println("rSRC = " + rSRC);


            JSONObject r1 = test.createRouter();
            int router1 = (Integer)r1.get("routerID");
            System.out.println("r1 = " + r1);

            JSONObject r2 = test.createRouter();
            int router2 = (Integer)r2.get("routerID");
            System.out.println("r2 = " + r2);

            JSONObject r3 = test.createRouter();
            int router3 = (Integer)r3.get("routerID");
            System.out.println("r3 = " + r3);

            JSONObject r4 = test.createRouter();
            int router4 = (Integer)r4.get("routerID");
            System.out.println("r4 = " + r4);

            JSONObject r5 = test.createRouter();
            int router5 = (Integer)r5.get("routerID");
            System.out.println("r5 = " + r5);

            JSONObject r6 = test.createRouter();
            int router6 = (Integer)r6.get("routerID");
            System.out.println("r6 = " + r6);

            JSONObject r7 = test.createRouter();
            int router7 = (Integer)r7.get("routerID");
            System.out.println("r7 = " + r7);







            JSONObject l1 = test.createLink(router1, router2, 10);
            int link1 = (Integer)l1.get("linkID");
            System.out.println("l1 = " + l1);

            JSONObject l2 = test.createLink(router1, router3, 10);
            int link2 = (Integer)l2.get("linkID");
            System.out.println("l2 = " + l2);

            JSONObject l3 = test.createLink(router2, router4, 10);
            int link3 = (Integer)l3.get("linkID");
            System.out.println("l3 = " + l3);

            JSONObject l4 = test.createLink(router3, router5, 10);
            int link4 = (Integer)l4.get("linkID");
            System.out.println("l4 = " + l4);

            JSONObject l5 = test.createLink(router4, router6, 10);
            int link5 = (Integer)l5.get("linkID");
            System.out.println("l5 = " + l5);

            JSONObject l6 = test.createLink(router5, router6, 10);
            int link6 = (Integer)l6.get("linkID");
            System.out.println("l6 = " + l6);

            JSONObject l7 = test.createLink(router4, router7, 10);
            int link7 = (Integer)l7.get("linkID");
            System.out.println("l7 = " + l7);

            JSONObject l8 = test.createLink(router5, router7, 10);
            int link8 = (Integer)l8.get("linkID");
            System.out.println("l8 = " + l8);

            JSONObject lSto1 = test.createLink(routerS, router1, 10);
            int linkSto1 = (Integer)lSto1.get("linkID");
            System.out.println("lSto1 = " + lSto1);


            // let the routing tables propogate
            Thread.sleep(60000);


            // on router6, Egress on port 4000 send to localhost:8856
            JSONObject a1_1 = test.createApp(router6, "demo_usr.paths.Egress", "4000 195.134.65.214:1234 -v");
            System.out.println("a1_1 = " + a1_1);
            Thread.sleep(500);

            // on router7, Egress on port 4000 send to localhost:8856
            JSONObject a1_2 = test.createApp(router7, "demo_usr.paths.Egress", "4000 195.134.65.214:1235 -v");
            System.out.println("a1_2 = " + a1_2);
            Thread.sleep(500);

            // Ingress point - listen on localhost:8855 - send from routerS to router4 on port 4000
            JSONObject a2_1 = test.createApp(routerS, "demo_usr.paths.Ingress", "8855 " + router4 + ":4000 -b 32 -v"); 
            System.out.println("a2_1 = " + a2_1);

            JSONObject a2_2 = test.createApp(routerS, "demo_usr.paths.Ingress", "8865 " + router5 + ":4000 -b 32 -v"); 
            System.out.println("a2_2 = " + a2_2);


            // Forward - send from router4 to router6 on port 4000
            JSONObject a3_1 = test.createApp(router4, "demo_usr.paths.Forward", "4000 " + router6 + ":4000 -v"); 
            System.out.println("a3_1 = " + a3_1);

            // Forward - send from router5 to router7 on port 4000
            JSONObject a3_2 = test.createApp(router5, "demo_usr.paths.Forward", "4000 " + router7 + ":4000 -v"); 
            System.out.println("a3_2 = " + a3_2);



            /* sleep 60 seconds = 1 minute = 60000 ms */



            Thread.sleep(60000 * 20);

            JSONObject r1D = test.deleteRouter(router1);

            JSONObject r2D = test.deleteRouter(router2);

            JSONObject r3D = test.deleteRouter(router3);

            JSONObject r4D = test.deleteRouter(router4);

            JSONObject r5D = test.deleteRouter(router5);

            JSONObject r6D = test.deleteRouter(router6);

            JSONObject r7D = test.deleteRouter(router7);

            JSONObject rSD = test.deleteRouter(routerS);

        } catch (Exception e) {
        }
    }

}
