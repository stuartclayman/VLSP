package usr.test;

import us.monoid.json.JSONObject;

/**
 * Test some calls to GlobalController using Resty
 */
class RestyTest2 extends RestyTest {

    public static void main(String[] args) {
        try {
            RestyTest2 test = new RestyTest2();

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

            JSONObject rSRC = test.createRouter();
            int routerS = (Integer)rSRC.get("routerID");
            System.out.println("rSRC = " + rSRC);

            JSONObject rDST = test.createRouter();
            int routerD = (Integer)rDST.get("routerID");
            System.out.println("rDST = " + rDST);




            JSONObject l1 = test.createLink(router1, router2, 10);
            int link1 = (Integer)l1.get("linkID");
            System.out.println("l1 = " + l1);

            JSONObject l2 = test.createLink(router1, router3, 10);
            int link2 = (Integer)l2.get("linkID");
            System.out.println("l2 = " + l2);

            JSONObject l3 = test.createLink(router2, router4, 10);
            l3.get("linkID");
            System.out.println("l3 = " + l3);

            JSONObject l4 = test.createLink(router3, router5, 10);
            l4.get("linkID");
            System.out.println("l4 = " + l4);

            JSONObject l5 = test.createLink(router4, router6, 10);
            l5.get("linkID");
            System.out.println("l5 = " + l5);

            JSONObject l6 = test.createLink(router5, router6, 10);
            l6.get("linkID");
            System.out.println("l6 = " + l6);

            JSONObject lSto1 = test.createLink(routerS, router1, 10);
            lSto1.get("linkID");
            System.out.println("lSto1 = " + lSto1);

            JSONObject lDto6 = test.createLink(routerD, router6, 10);
            lDto6.get("linkID");
            System.out.println("lDto6 = " + lDto6);


            // let the routing tables propogate
            Thread.sleep(66000);


            // on routerD, Recv on port 4000
            JSONObject a1 = test.createApp(routerD, "usr.applications.RecvDataRate", "4000");
            System.out.println("a1 = " + a1);
            Thread.sleep(500);

            // send from routerS to routerD on port 4000
            JSONObject a2 = test.createApp(routerS, "usr.applications.Send", routerD + " 4000 2500000 -s 1024 -i 1");  // id+4000+count
            System.out.println("a2 = " + a2);


            /* sleep 60 seconds = 1 minute = 60000 ms */

            /* After 1 minute set a link weight */
            Thread.sleep(60000);

            JSONObject l1W = test.setLinkWeight(link1, 20);
            System.out.println("l1W = " + l1W);


            /* After 1 more minute reset link weight
               and set a wieght on a different link */
            Thread.sleep(60000);

            JSONObject l2W = test.setLinkWeight(link2, 20);   // now 20
            System.out.println("l2W = " + l2W);
            JSONObject l1WW = test.setLinkWeight(link1, 10);   // back to 10
            System.out.println("l1WW = " + l1WW);



            Thread.sleep(300000);

            test.deleteRouter(router1);

            test.deleteRouter(router2);

            test.deleteRouter(router3);

            test.deleteRouter(router4);

            test.deleteRouter(router5);

            test.deleteRouter(router6);

            test.deleteRouter(routerS);

            test.deleteRouter(routerD);


        } catch (Exception e) {
        }
    }

}
