package usr.test;

import java.util.Scanner;

import usr.logging.Logger;
import usr.logging.USR;
import usr.router.Router;

/**
 * Test ROuter startup.
 */
public class RouterTest1 {
    public static void main(String[] args) {
        Router router = null;

        if (args.length == 0) {
            int port = 15151;
            router = new Router(port, "Router-" + port + "-" + (port+1));
        } else if (args.length == 1) {
            int mPort = 0;
            Scanner sc = new Scanner(args[0]);
            mPort = sc.nextInt();
            sc.close();
            router = new Router(mPort, "Router-" + mPort + "-" + (mPort+1));
        } else if (args.length == 2) {
            int mPort = 0;
            int r2rPort = 0;
            Scanner sc = new Scanner(args[0]);
            mPort = sc.nextInt();
            sc.close();
            sc = new Scanner(args[1]);
            r2rPort = sc.nextInt();
            sc.close();
            router = new Router(mPort, r2rPort, "Router-" + mPort + "-" + r2rPort);
        } else {
            help();
        }

        // start
        if (router.start()) {
        } else {
            router.stop();
        }

    }

    private static void help() {
        Logger.getLogger("log").logln(USR.ERROR, "Test1 [mgt_port [r2r_port]]");
        System.exit(1);
    }

}