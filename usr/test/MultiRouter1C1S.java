package usr.test;

import usr.router.Router;
import usr.router.RouterEnv;
import usr.interactor.RouterInteractor;
import usr.logging.*;
import usr.net.*;

/**
 * Test Router startup and simple AppSocket.
 */
public class MultiRouter1C1S {
    // the Routers
    Router router1 = null;
    Router router2 = null;
    RouterEnv router1Env = null;
    RouterEnv router2Env = null;
    RouterInteractor router1Interactor = null;
    RouterInteractor router2Interactor = null;

    public MultiRouter1C1S() {
        try {
            AddressFactory.setClassForAddress("usr.net.IPV4Address");

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "MultiRouter1C1S exception: " + e);
            e.printStackTrace();
        }
    }

    void setup() {
        try {
            // Router 1
            router1Env = new RouterEnv(18181, 19181, "Router-1");
            router1 = router1Env.getRouter();

            if (router1Env.isActive()) {
                // set up address
                router1.setAddress(new IPV4Address("192.168.7.1"));

                // talk to router1 ManagementConsole
                router1Interactor = router1Env.getRouterInteractor();
            } else {
                router1Env.stop();
                throw new Exception("router1 will not start");
            }

            // Router 2
            router2Env = new RouterEnv(18182, 19182, "Router-2");
            router2 = router2Env.getRouter();

            if (router2Env.isActive()) {
                // set up address
                router2.setAddress(new IPV4Address("192.168.7.2"));

                // talk to router2 ManagementConsole
                router2Interactor = router2Env.getRouterInteractor();
            } else {
                router2Env.stop();
                throw new Exception("router2 will not start");
            }

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "MultiRouter1C1S exception: " + e);
            e.printStackTrace();
        }
    }

    void connect() {
        try {
            // connect router1 to router2

            // then set up Router-to-Router data connection, weight 1
            router1Interactor.createConnection("localhost:" + router2.getManagementConsolePort(), 1);

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "MultiRouter1C1S exception: " + e);
            e.printStackTrace();
        }
    }

    void go() {
        try {
            // list on router2
            String[] args2 = { "3000" };
            router2Interactor.appStart("usr.applications.Recv", args2);

            String[] args1 = { "192.168.7.2", "3000", "100" };
            router1Interactor.appStart("usr.applications.Send", args1);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "MultiRouter1C1S exception: " + e);
            e.printStackTrace();
        }
    }

    void end() {
        try {
            Thread.sleep(5000);

            router1Interactor.quit();
            router2Interactor.quit();

            router1.shutDown();
            router2.shutDown();
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "MultiRouter1C1S exception: " + e);
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        MultiRouter1C1S mr = new MultiRouter1C1S();

        mr.setup();
        mr.connect();
        mr.go();
        mr.end();
    }

}