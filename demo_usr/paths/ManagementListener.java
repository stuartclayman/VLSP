package demo_usr.paths;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;

import us.monoid.json.JSONObject;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Datagram;


/**
 * This class gets data from a queue, probably a ManagementPort, and
 * converts it into a JSONObject then calls the passed in ManagementHandler
 * to process the JSONObject.
 */
public class ManagementListener implements Callable <Object>{
    LinkedBlockingDeque<usr.net.Datagram> queue;
    boolean running = false;

    // keep track of data
    // total no of Datagrams in
    int count = 0;

    // verbose
    int verbose = 0;


    ManagementHandler handler;

    // Latch for end synchronization
    private final CountDownLatch actuallyFinishedLatch;


    public ManagementListener(ManagementHandler handler,  LinkedBlockingDeque<usr.net.Datagram> queue, int verb) throws Exception {
        // The queue to put new Datagrams on
        this.queue = queue;


        // verbose
        this.verbose = verb;

        this.handler = handler;

        actuallyFinishedLatch = new CountDownLatch(1);


        running = true;
    }


    @Override
	public Object call() {
        Datagram inDatagram = null;

        try {

            while (running) {
                // read a USR packet
                //inDatagram = queue.take();

                try {
                    do {
                        inDatagram = queue.take();
                        if (inDatagram == null) {
                            Logger.getLogger("log").logln(USR.ERROR, "ManagementListener IN Datagram is NULL " + count + " at " + System.currentTimeMillis());
                        }
                    } while (inDatagram == null);
                } catch (InterruptedException ie) {
                    //Logger.getLogger("log").logln(USR.ERROR, "ManagementListener interrupted");
                    running = false;
                    break;
                }

                count++;

                // Process the Datagram

                // original data
                byte[] payload = inDatagram.getPayload();
                String data = new String(payload);

                JSONObject jsObject = new JSONObject(data);

                Logger.getLogger("log").logln(USR.ERROR, "ManagementListener received " + jsObject);

                handler.process(jsObject);


            }

        } catch (Exception e) {
            Logger.getLogger("log").log(USR.ERROR, e.getMessage());
            e.printStackTrace();
        }


        actuallyFinishedLatch.countDown();

        // Logger.getLogger("log").logln(USR.ERROR, "ManagementListener: end of call()");

        return null;

    }

    /**
     * Wait for this Forwarder to terminate.
     */
    public void await() {
        try {
            actuallyFinishedLatch.await();
        } catch (InterruptedException ie) {
        }
    }



}
