package demo_usr.nfv;

import java.net.SocketException;
import java.net.NoRouteToHostException;
import java.util.Scanner;
import java.nio.ByteBuffer;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Datagram;
import usr.net.Address;
import usr.net.AddressFactory;
import usr.net.NetworkInterface;
import usr.net.NetworkException;
import usr.applications.*;
import usr.router.Intercept;
import usr.dcap.DcapNetworkInterface;
import usr.protocol.Protocol;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

/**
 * An application for receiving some data and acting as a Network Function.
 * <p>
 * NetFn
 */
public class NetFn implements Application {
    int count = 0;
    boolean running = false;
    CountDownLatch latch = null;

    Map<String, InterceptListener> interceptMap = null;

    // Timer stuff
    Timer timer = null;
    TimerTask netifObserver = null;

    // Default 6: seconds
    int checkInterval = 6;

    // Thread Pool
    ExecutorService executors;

    /**
     * Constructor for SimpleNetFn
     */
    public NetFn() {
    }

    /**
     * Initialisation for NetFn.
     * 
     */
    public ApplicationResponse init(String[] args) {
        latch = new CountDownLatch(1);

        interceptMap = new HashMap<String, InterceptListener>();

        if (args.length == 1) {
            // no args
            return new ApplicationResponse(true, "");

        } else {
            for (int extra = 1; extra < args.length; extra++) {
                String thisArg = args[extra];

                // check if its a flag
                if (thisArg.charAt(0) == '-') {
                    // get option
                    char option = thisArg.charAt(1);

                    // gwet next arg
                    String argValue = args[++extra];

                    switch (option) {
                    case 'i': {
                        try {
                            checkInterval = Integer.parseInt(argValue);
                        } catch (Exception e) {
                            return new ApplicationResponse(false, "Bad checkInterval " + argValue);
                        }

                        break;
                    }
            
                    default:
                        return new ApplicationResponse(false, "Bad option " + option);
                    }
                }

            }
                
            return new ApplicationResponse(true, "");
        }
    }

    /** Start application with argument  */
    public ApplicationResponse start() {
        try {
            // setup a thread pool
            executors = Executors.newCachedThreadPool();

            // set up TimerTask
            netifObserver = new NetIFTimed(this);

            // if there is no timer, start one
            if (timer == null) {
                timer = new Timer();
                // run now and every N seconds
                timer.schedule(netifObserver, 0, checkInterval * 1000);
            }

    
        } catch (Exception e) {
            e.printStackTrace();

            Logger.getLogger("log").logln(USR.ERROR, "Cannot open NetIF " + e.getMessage());
            return new ApplicationResponse(false, "Cannot open NetIF " + e.getMessage());
        }

        running = true;

        return new ApplicationResponse(true, "");
    }

    /** Implement graceful shut down */
    public ApplicationResponse stop() {
        running = false;

        // stop timer
        if (netifObserver != null) {
            netifObserver.cancel();
        }

        // stop InterceptListeners
        if (interceptMap.size() > 0) {
            for (InterceptListener interceptListener : interceptMap.values()) {
                interceptListener.close();
            }
        }


        // reduce latch count by 1
        latch.countDown();

        Logger.getLogger("log").logln(USR.STDOUT, "NetFn stop");

        return new ApplicationResponse(true, "");
    }

    /**
     * Run loop
     */
    public void run() {
        while (running) {
        
            try {
                latch.await();
            } catch (InterruptedException ie) {
            }
        }

    }
    
    /**
     * The callback from the NetworkInterface observer.
     * Tells us that a NetworkInterface is active
     */
    protected boolean addNetworkInterface(NetworkInterface netif)  {
        String name = netif.getName();

        if (interceptMap.containsKey(name)) {
            // already have an Intercept for name
            System.out.println("Already have Intercept for " + name);
            return false;
        } else {
            System.out.println("Create Intercept for " + name);

            try {

                DcapNetworkInterface interceptNIF = DcapNetworkInterface.getIFByName(name);
            
                if (interceptNIF == null) {
                    Logger.getLogger("log").logln(USR.ERROR, "No InterceptNetworkInterface for Name: " + name);
                    return false;
                } else {

                    Intercept intercept = interceptNIF.intercept();
                    
                    // Create InterceptListener
                    InterceptListener interceptListener = new InterceptListener(this, intercept, name);
            
                    // Save in interceptMap
                    interceptMap.put(name, interceptListener);

                    // Now start it
                    // Returns a Future
                    executors.submit((Callable <?>)interceptListener);
            
                    return true;
                }

            } catch (SocketException se) {
                Logger.getLogger("log").logln(USR.ERROR, "SocketException : " + se);
                return false;
            }
        }
    }


    /**
     * The callback from the NetworkInterface observer.
     * Tells us that a NetworkInterface is gone.
     */
    protected boolean removeNetworkInterface(String name)  {

        if (interceptMap.containsKey(name)) {
            // already have an Intercept for name
            System.out.println("Remove Intercept for " + name);

            InterceptListener interceptListener = interceptMap.get(name);

            interceptListener.close();

            // Remove from interceptMap
            interceptMap.remove(name);


            return true;
        } else {
            // Dont have the name in our Map
            return false;
        }
    }

    /**
     * The callback for when a Datagram is received by an Intercepter.
     * Return true to forward Datagram, Return false to throw it away.
     */
    public boolean datagramProcess(InterceptListener intercepter, Datagram datagram) {
            count++;

            // send onwards
            return true;
    }

    /**
     * The callback for when a Datagram is to be sent an Intercepter.
     */
    public boolean datagramSend(InterceptListener intercepter, Datagram datagram) {
        return intercepter.send(datagram);
    }


    //------------------------------------------------------------------------//
    
    /**
     * Class to check NetworkInterfaces on a regular basis
     */
    private class NetIFTimed extends TimerTask {
        NetFn netfn;
        boolean running = false;

        
        public NetIFTimed(NetFn netfn) {
            this.netfn = netfn;
            running = true;
        }

    
        public void run() {
            if (running) {

                Set<String> existing = new HashSet<String>(netfn.interceptMap.keySet());
                
                try {
                    // get list of all netif
                    List<NetworkInterface> netifs = NetworkInterface.getNetworkInterfaces();

                    for (NetworkInterface netif : netifs) {

                        String name = netif.getName();
                        
                        if (! existing.contains(name)) {
                            // Tell the NetFn that a NetworkInterface is active
                            netfn.addNetworkInterface(netif);
                        }

                        existing.remove(name);
                    }
                } catch (SocketException se) {
                    Logger.getLogger("log").log(USR.ERROR, "NetIFTimed SocketException " + se.getMessage());
                }


                // if existing still has values, they need to be removed
                if (existing.size() > 0) {
                    for (String name : existing) {
                        netfn.removeNetworkInterface(name);
                    }
                }
            }
        }

        public boolean cancel() {
            if (running) {
                running = false;
            }

            return running;
        }
    }


    //------------------------------------------------------------------------//
    

    /** 
     * An Intercept listener.
     * Wraps up an Intercept, reads from Intercept, and passes Datagram onto NetFn
     * for checking and processing.
     */
    public class InterceptListener implements Callable<Object> {
        boolean running = false;

        NetFn netfn;
        Intercept intercept;
        String name;

        Thread myThread;

        public InterceptListener(NetFn netfn, Intercept intercept, String name) {
            this.netfn = netfn;
            this.intercept = intercept;
            this.name = name;
        }

        
        /**
         * Main Loop listens for Datagrams
         */
        public Object call() {
            running = true;
            
            Datagram datagram;

            myThread = Thread.currentThread();
            
            try {
                while (running) {

                    if ((datagram = intercept.receive()) != null) {
                        boolean forward = netfn.datagramProcess(this, datagram);

                        if (forward) {
                            netfn.datagramSend(this, datagram);
                        }
                    }

                    
                }
            } catch (NetworkException se) {
                Logger.getLogger("log").log(USR.ERROR, "InterceptListener NetworkException on " + name + " " + se.getMessage());
            }

            Logger.getLogger("log").log(USR.STDOUT, "End of InterceptListener for " + name);

            return null;
        }

        /**
         * Send Datagrams onwards.
         */
        public boolean send(Datagram datagram) {
            try {
                // send onwards
                intercept.send(datagram);

                return true;
            } catch (NoRouteToHostException ne) {
                Logger.getLogger("log").log(USR.ERROR, ne.getMessage());

                return false;
            }
        }


        /**
         * Close.
         */
        public boolean close() {
            running = false;

            myThread.interrupt();

            intercept.close();

            return true;
        }


        /**
         * Get the name 
         */
        public String getName() {
            return name;
        }

    }


}

