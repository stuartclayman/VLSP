package usr.test;

import java.util.ArrayList;
import java.util.List;

import usr.interactor.RouterInteractor;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.AddressFactory;
import usr.net.IPV4Address;
import usr.router.RouterEnv;

/**
 * Test Router startup and simple AppSocket.
 */
public class MultiRouter7 {
    // the Routers
    List<RouterInteractor> routerInteractors;

    String options =
        "<RouterOptions>" +
        "  <RoutingParameters>" +
        "    <MaxCheckTime>60000</MaxCheckTime>" +
        "    <MinNetIFUpdateTime>5000</MinNetIFUpdateTime>" + 
        "    <MaxNetIFUpdateTime>10000</MaxNetIFUpdateTime>" +
        "  </RoutingParameters>" +
        "</RouterOptions>";

    public MultiRouter7() {
        try {
            AddressFactory.setClassForAddress("usr.net.IPV4Address");

            routerInteractors = new ArrayList<RouterInteractor>(8);

            // put null in 0th entries
            routerInteractors.add(null);

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "MultiRouter7 exception: " + e);
            e.printStackTrace();
        }
    }

    void setup() {
        try {
            //Thread.sleep(1000);

            for (int r = 1; r<=7; r++) {
                // Create a Router Environment
                RouterEnv env = new RouterEnv(18180+r, 19180+r, "Router-"+r);

                if (env.isActive()) {

                    // talk to router ManagementConsole
                    // so get a RouterInteractor
                    RouterInteractor routerInteractor = env.getRouterInteractor();

                    // set up address
                    routerInteractor.setAddress(new IPV4Address("192.168.7."+r));

                    // set options
                    routerInteractor.setConfigString(options);


                    // save details
                    routerInteractors.add(routerInteractor);
                } else {
                    env.stop();
                    throw new Exception("router " +  (18180+r) + " / " +  (19180+r) +
                                        " / " + ("Router-"+r)  + " will not start");
                }

                //Thread.sleep(300);
            }

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "MultiRouter7 exception: " + e);
            e.printStackTrace();
        }
    }

    void connect() {
        try {
            // connect routers
            Thread.sleep(1000);

            // then set up Router-to-Router data connection, weight 1
            routerInteractors.get(3).createConnection(routerInteractors.get(4).getManagementConsoleAddress(), 1);

            routerInteractors.get(3).createConnection(routerInteractors.get(5).getManagementConsoleAddress(), 1);

            routerInteractors.get(3).createConnection(routerInteractors.get(7).getManagementConsoleAddress(), 1);

            routerInteractors.get(1).createConnection(routerInteractors.get(3).getManagementConsoleAddress(), 1);

            routerInteractors.get(2).createConnection(routerInteractors.get(3).getManagementConsoleAddress(), 1);

            routerInteractors.get(6).createConnection(routerInteractors.get(3).getManagementConsoleAddress(), 1);

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "MultiRouter7 exception: " + e);
            e.printStackTrace();
        }
    }

    void go() {
        try {
            // listen on 4, 5, 7
            String[] argsRecv = { "3000" };

            // allow time for routing table propogation
            Thread.sleep(10000);

            // recv on 4, 5, 7
            routerInteractors.get(4).appStart("usr.applications.RecvDataRate", argsRecv);

            routerInteractors.get(5).appStart("usr.applications.RecvDataRate", argsRecv);

            routerInteractors.get(7).appStart("usr.applications.RecvDataRate", argsRecv);

            // send on 1, 2, 6
            // to 4, 5, 7
            Thread.sleep(2000);
            String[] args1 = { "192.168.7.4", "3000", "250000" };
            routerInteractors.get(1).appStart("usr.applications.Send", args1);

            String[] args2 = { "192.168.7.5", "3000", "250000" };
            routerInteractors.get(2).appStart("usr.applications.Send", args2);

            String[] args6 = { "192.168.7.7", "3000", "250000" };
            routerInteractors.get(6).appStart("usr.applications.Send", args6);

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "MultiRouter7 exception: " + e);
            e.printStackTrace();
        }
    }

    void end() {
        try {
            Thread.sleep(20000);

            // then remove Router-to-Router data connections
            routerInteractors.get(3).endLink(routerInteractors.get(4).getAddress().toString());

            routerInteractors.get(3).endLink(routerInteractors.get(5).getAddress().toString());

            routerInteractors.get(3).endLink(routerInteractors.get(7).getAddress().toString());

            routerInteractors.get(1).endLink(routerInteractors.get(3).getAddress().toString());

            routerInteractors.get(2).endLink(routerInteractors.get(3).getAddress().toString());

            routerInteractors.get(6).endLink(routerInteractors.get(3).getAddress().toString());

            // shut down all routers
            for (int r = 1; r<=7; r++) {
                routerInteractors.get(r).shutDown();
            }

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "MultiRouter7 exception: " + e);
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        MultiRouter7 mr = new MultiRouter7();

        mr.setup();
        mr.connect();
        mr.go();
        mr.end();
    }

}
