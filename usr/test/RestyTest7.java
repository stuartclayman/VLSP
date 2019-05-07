package usr.test;

import us.monoid.json.JSONObject;
import usr.vim.VimClient;

/**
 * Test some calls to GlobalController using Resty
 */
class RestyTest7  {
    public static void main(String[] args) {
        try {
            VimClient test = new VimClient();


            JSONObject rSRC = test.createRouter();
            int routerS = (Integer)rSRC.get("routerID");
            System.out.println("rSRC = " + rSRC);

            JSONObject rDST = test.createRouter();
            int routerD = (Integer)rDST.get("routerID");
            System.out.println("rDST = " + rDST);


            // let the routing tables propogate
            Thread.sleep(10000);


            // on routerD, Egress on port 4000 send to localhost:8856
            JSONObject a1 = test.createApp(routerD, "demo_usr.paths.Egress", "4000 localhost:8856");
            System.out.println("a1 = " + a1);
            Thread.sleep(500);

            // Ingress point - listen on localhost:8855 - send from routerS to router6 on port 4000
            JSONObject a2 = test.createApp(routerS, "demo_usr.paths.Ingress", "8855 " + routerD + ":4000 "); 
            System.out.println("a2 = " + a2);

            /* sleep 60 seconds = 1 minute = 60000 ms */
            Thread.sleep(15000);


            JSONObject rSD = test.deleteRouter(routerS);

            JSONObject rDD = test.deleteRouter(routerD);

        } catch (Exception e) {
        } catch (Error err) {
        }
    }

}
