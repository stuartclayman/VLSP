package usr.test;

import usr.router.Router;
import usr.router.RouterEnv;
import usr.logging.*;
import usr.router.AppSocket;
import usr.net.IPV4Address;
import usr.net.Datagram;
import java.util.Timer;
import java.util.TimerTask;
import java.net.SocketException;

/**
 * Test Router startup and simple AppSocket.
 */
public class RouterApp1SD {
    // the Router
    Router router = null;
    RouterEnv routerEnv = null;

    // the socket
    AppSocket socket;

    // Timer stuff
    Timer timer = null;
    TimerTask timerTask = null;

    // total no of Datagrams in
    int count = 0;
    // last time count
    int lastTimeCount = 0;
    // no per second
    int diffs = 0;

    public RouterApp1SD() {
        try {
            int port = 18191;
            int r2r = 18192;

            routerEnv = new RouterEnv(port, r2r, "Router-2");
            router = routerEnv.getRouter();

            // check
            if (routerEnv.isActive()) {
            } else {
            }

            // set ID
            router.setAddress(new IPV4Address("192.168.7.2")); // WAS new GIDAddress(2));

            // now set up an AppSocket to receive
            socket = new AppSocket(router, 3000);

            // set up timer to count throughput
            timerTask = new TimerTask() {
                boolean running = true;

                public void run() {
                    if (running) {
                        diffs = count - lastTimeCount;
                        Logger.getLogger("log").logln(USR.STDOUT, "Task count: " + count + " diff: "  + diffs);
                        lastTimeCount = count;
                    }
                }

                public boolean cancel() {
                    Logger.getLogger("log").log(USR.STDOUT, "cancel @ " + count);

                    if (running) {
                        running = false;
                    }


                    return running;
                }

                public long scheduledExecutionTime() {
                    Logger.getLogger("log").log(USR.STDOUT, "scheduledExecutionTime:");
                    return 0;
                }

            };

            // if there is no timer, start one
            if (timer == null) {
                timer = new Timer();
                timer.schedule(timerTask, 1000, 1000);
            }



        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "RouterApp1S exception: " + e);
            e.printStackTrace();
        }


    }

    /**
     * Read stuff
     */
    void readALot() {

        try {
            Datagram datagram;

            while ((datagram = socket.receive()) != null) {
                count++;
            }
        } catch (SocketException se) {
            Logger.getLogger("log").logln(USR.ERROR, se.getMessage());
        }

    }

    public static void main(String[] args) {
        RouterApp1SD app1sd = new RouterApp1SD();

        app1sd.readALot();
    }

}
