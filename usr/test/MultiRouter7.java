package usr.test;

import usr.router.Router;
import usr.interactor.RouterInteractor;
import usr.logging.*;
import usr.net.*;
import java.util.*;
import java.io.IOException;

/**
 * Test Router startup and simple AppSocket.
 */
public class MultiRouter7 {
    // the Routers
    List<Router> routers;
    List<RouterInteractor> routerInteractors;

    public MultiRouter7() {
        try {
            AddressFactory.setClassForAddress("usr.net.IPV4Address");

            routers = new ArrayList<Router>(8);
            routerInteractors = new ArrayList<RouterInteractor>(8);

            routers.add(null);
            routerInteractors.add(null);

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "MultiRouter1C1S exception: " + e);
            e.printStackTrace();
        }
    }

    void setup() {
        try {
            Thread.sleep(5000);

            for (int r=1; r<=7; r++) {
                // Router r
                Router router = new Router(18180+r, 19180+r, "Router-"+r);

                if (router.start()) {
                    // set up address
                    router.setAddress(new IPV4Address("192.168.7."+r)); 

                    // talk to router1 ManagementConsole
                    RouterInteractor routerInteractor = new RouterInteractor("localhost", 18180+r);
                    routers.add(router);
                    routerInteractors.add(routerInteractor);
                } else {
                    router.stop();
                    throw new Exception("router1 will not start");
                }

                Thread.sleep(600);
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
            routerInteractors.get(3).createConnection("localhost:" + routers.get(4).getManagementConsolePort(), 1);
            Thread.sleep(200);
            routerInteractors.get(3).createConnection("localhost:" + routers.get(5).getManagementConsolePort(), 1);
            Thread.sleep(200);
            routerInteractors.get(3).createConnection("localhost:" + routers.get(7).getManagementConsolePort(), 1);
            Thread.sleep(200);
            routerInteractors.get(1).createConnection("localhost:" + routers.get(3).getManagementConsolePort(), 1);
            Thread.sleep(200);
            routerInteractors.get(2).createConnection("localhost:" + routers.get(3).getManagementConsolePort(), 1);
            Thread.sleep(200);
            routerInteractors.get(6).createConnection("localhost:" + routers.get(3).getManagementConsolePort(), 1);

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "MultiRouter7 exception: " + e);
            e.printStackTrace();
        }
    }

    void go() {
        try {
            // listen on 4, 5, 7
            String[] argsRecv =  {"3000"};

            // allow time for routing table propogation
            Thread.sleep(7000);

            routerInteractors.get(4).appStart("usr.applications.RecvDataRate", argsRecv);
            Thread.sleep(100);
            routerInteractors.get(5).appStart("usr.applications.RecvDataRate", argsRecv);
            Thread.sleep(100);
            routerInteractors.get(7).appStart("usr.applications.RecvDataRate", argsRecv);

            // send on 1, 2, 6
            Thread.sleep(1000);
            String[] args1 = {"192.168.7.4", "3000", "250000"};
            routerInteractors.get(1).appStart("usr.applications.Send", args1);

            Thread.sleep(200);
            String[] args2 = {"192.168.7.5", "3000", "250000"};
            routerInteractors.get(2).appStart("usr.applications.Send", args2);

            Thread.sleep(200);
            String[] args6 = {"192.168.7.7", "3000", "250000"};
            routerInteractors.get(6).appStart("usr.applications.Send", args6);

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "MultiRouter7 exception: " + e);
            e.printStackTrace();
        }
    }

    void end() {
        try {
            Thread.sleep(18000);

            for (int r=1; r<=7; r++) {
                routerInteractors.get(r).quit();
            }

            for (int r=1; r<=7; r++) {
                routers.get(r).shutDown();
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
