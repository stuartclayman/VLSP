package usr.test;

import us.monoid.web.*;
import us.monoid.json.*;
import static us.monoid.web.Resty.*;
import java.io.IOException;
import usr.vim.VimClient;

/**
 * Test some calls to GlobalController using Resty
 */
class TFTP3 {
    public static void main(String[] args) {
        try {
            VimClient test = new VimClient();

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

            JSONObject lSto1 = test.createLink(routerS, router1, 10);
            int linkSto1 = (Integer)lSto1.get("linkID");
            System.out.println("lSto1 = " + lSto1);

            JSONObject lDto6 = test.createLink(routerD, router6, 10);
            int linkDtoS = (Integer)lDto6.get("linkID");
            System.out.println("lDto6 = " + lDto6);


            // let the routing tables propogate
            Thread.sleep(60000);


            // on routerD, TFTPServer listening on port 1069
            JSONObject a1 = test.createApp(routerD, "plugins_usr.tftp.com.globalros.tftp.server.TFTPServer", "1069");
            System.out.println("a1 = " + a1);

            Thread.sleep(10000);

            // on routerS, TFTPClient send to @(3)
            JSONObject a2 = test.createApp(routerS, "plugins_usr.tftp.com.globalros.tftp.client.TFTPClient", "" + routerD); 
            System.out.println("a2 = " + a2);


            /* sleep 60 seconds = 1 minute = 60000 ms */
            Thread.sleep(120000);


            JSONObject r1D = test.deleteRouter(router1);

            JSONObject r2D = test.deleteRouter(router2);

            JSONObject r3D = test.deleteRouter(router3);

            JSONObject r4D = test.deleteRouter(router4);

            JSONObject r5D = test.deleteRouter(router5);

            JSONObject r6D = test.deleteRouter(router6);

            JSONObject rSD = test.deleteRouter(routerS);

            JSONObject rDD = test.deleteRouter(routerD);

        } catch (Exception e) {
        } catch (Error err) {
        }
    }

}
