package usr.test;

import usr.router.Router;
import usr.router.RouterEnv;
import usr.logging.*;
import java.util.Scanner;

/**
 * Test ROuter startup.
 */
public class RouterTest1 {
    public static void main(String[] args) {
        RouterEnv routerEnv = null;
        @SuppressWarnings("unused")
        Router router;

        if (args.length == 0) {
            int port = 15151;
            routerEnv = new RouterEnv(port, "Router-" + port + "-" + (port+1));
        } else if (args.length == 1) {
            int mPort = 0;
            Scanner sc = new Scanner(args[0]);
            mPort = sc.nextInt();
            sc.close();
            routerEnv = new RouterEnv(mPort, "Router-" + mPort + "-" + (mPort+1));
        } else if (args.length == 2) {
            int mPort = 0;
            int r2rPort = 0;
            Scanner sc = new Scanner(args[0]);
            mPort = sc.nextInt();
            sc.close();
            sc = new Scanner(args[1]);
            r2rPort = sc.nextInt();
            sc.close();
            routerEnv = new RouterEnv(mPort, r2rPort, "Router-" + mPort + "-" + r2rPort);
        } else {
            help();
        }

        // check
        if (routerEnv.isActive()) {
        } else {
        }

    }

    private static void help() {
        Logger.getLogger("log").logln(USR.ERROR, "Test1 [mgt_port [r2r_port]]");
        System.exit(1);
    }

}