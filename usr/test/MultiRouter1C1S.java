package usr.test;

import usr.router.Router;
import usr.interactor.RouterInteractor;
import usr.logging.*;
import usr.net.*;
import java.util.Scanner;
import java.io.IOException;

/**
 * Test Router startup and simple AppSocket.
 */
public class MultiRouter1C1S {
    // the Routers
    Router router1 = null;
    Router router2 = null;
    RouterInteractor router1Interactor = null;
    RouterInteractor router2Interactor = null;

    public MultiRouter1C1S() {
        try {
            AddressFactory.setClassForAddress("usr.net.IPV4Address");
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          "MultiRouter1C1S exception: " + e);
            e.printStackTrace();
        }
    }

    void setup() {
        try {
            // Router 1
            router1 = new Router(18181, 19181, "Router-1");

            if (router1.start()) {
                // set up address
                router1.setAddress(new IPV4Address("192.168.7.1"));

                // talk to router1 ManagementConsole
                router1Interactor = new RouterInteractor("localhost",
                                                         18181);
            } else {
                router1.stop();
                throw new Exception(
                          "router1 will not start");
            }

            // Router 2
            router2 = new Router(18182, 19182, "Router-2");

            if (router2.start()) {
                // set up address
                router2.setAddress(new IPV4Address("192.168.7.2"));

                // talk to router2 ManagementConsole
                router2Interactor = new RouterInteractor("localhost",
                                                         18182);
            } else {
                router2.stop();
                throw new Exception(
                          "router2 will not start");
            }
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          "MultiRouter1C1S exception: " + e);
            e.printStackTrace();
        }
    }

    void connect() {
        try {
            // connect router1 to router2

            // then set up Router-to-Router data connection, weight 1
            router1Interactor.createConnection(
                "localhost:" + router2.getManagementConsolePort(),
                1);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          "MultiRouter1C1S exception: " + e);
            e.printStackTrace();
        }
    }

    void go() {
        try {
            // list on router2
            String[] args2 = {
                "3000"
            };
            router2Interactor.appStart("usr.applications.Recv", args2);

            String[] args1 = {
                "192.168.7.2", "3000", "100"
            };
            router1Interactor.appStart("usr.applications.Send", args1);
        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR,
                                          "MultiRouter1C1S exception: " + e);
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
            Logger.getLogger("log").logln(USR.ERROR,
                                          "MultiRouter1C1S exception: " + e);
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