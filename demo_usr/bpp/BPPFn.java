package demo_usr.bpp;

import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.util.Timer;
import java.util.TimerTask;

import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Datagram;
import usr.net.DatagramFactory;
import usr.protocol.Protocol;
import usr.common.ANSI;
import usr.applications.ApplicationResponse;
import demo_usr.nfv.NetFn;

import us.monoid.json.JSONObject;

/**
 * A NetFn that does some BPP processing
 */
public class BPPFn extends NetFn  {

    boolean adaptivePPS = false;
    
    // in bits
    int bandwidthBits = 1024 * 1024;   // default: 1 Mb
    int bandwidth = 0;
    int packetsPerSecond = 100;  // default: 100
    
    // Input / Output counts
    int count = 0;
    int countLastSecond = 0;
    long volumeIn = 0;
    long volumeOut = 0;
    long volumeInLastSecond = 0;
    long volumeOutLastSecond = 0;

    
    // The BPPUnpacker
    BPPUnpack unpack = null;

    // Timer stuff
    Timer volumeTimer = null;
    TimerTask volumeTask = null;


    // I/O
    String file = "/tmp/namedPipe0";
    PrintStream outStream;
        

    /**
     * Constructor for BPPFn
     */
    public BPPFn() {
        this.bandwidth = bandwidthBits >> 3;
        
        try { 
            outStream = new PrintStream(file);
        } catch (FileNotFoundException fe) {
            outStream = System.err;
        }
    }

    /**
     * Constructor for BPPFn, pass in bandwidth
     */
    public BPPFn(int bandwidthBits) {
        this.bandwidthBits = bandwidthBits;
        this.bandwidth = bandwidthBits >> 3;

        try { 
            outStream = new PrintStream(new FileOutputStream(file), true);
        } catch (FileNotFoundException fe) {
            outStream = System.err;
        }            
    }


    /**
     * Get the bandwidth available (in bits)
     */
    public int getBandwidth() {
        return bandwidthBits; 
    }
    
    /**
     * Set the available bandwidth (in bits)
     */
    public void setBandwidth(int b) {
        bandwidthBits = b;
    }
    
    /**
     * Process the args
     * -m port_no  -- the port number to listen for management Reconfigure updates.
     * -r rate     -- default packets per second expected to arrive
     * -a          -- do adaptive packets per second, by counting actual flow
     * -b bandwidth -- the estimated bandwidth of the output link
     * -v | -vv    -- for different verbose levels
     * Return null on success
     * Return ApplicationResponse if there is an error
     */
    protected ApplicationResponse processArgs(String[] args) {
        for (int extra = 0; extra < args.length; extra++) {
            String thisArg = args[extra].trim();

            // check if its a flag
            if (thisArg.charAt(0) == '-') {
                // get option
                char option = thisArg.charAt(1);

                switch (option) {

                case 'm': {   /* -m port -- the port number to listen for management Reconfigure updates.
                                 -- By default there is no reconfiguration. */
                    // get next arg
                    String portValue = args[++extra];

                    try {
                        setManagementPortNumber(Integer.parseInt(portValue));
                    } catch (Exception e) {
                        return applicationError("Bad managementPortNumber " + portValue);
                    }
                    break;
                }

                case 'a': {
                    adaptivePPS = true;
                    break;
                }

                case 'r': {   /* -r rate -- the no of packets per second */
                    // get next arg
                    String countValue = args[++extra];

                    try {
                        setPacketsPerSecond(Integer.parseInt(countValue));
                    } catch (Exception e) {
                        return applicationError("Bad packets per second " + countValue);
                    }
                    break;
                }

                case 'b': {   /* -b bandwidthBits -- the estimated bandwidth (in Mbits) e.g 0.8 or 1.2 */
                    // get next arg
                    String countValue = args[++extra];

                    try {
                        setBandwidth(Float.parseFloat(countValue));
                    } catch (Exception e) {
                        return applicationError("Bad packets per second " + countValue);
                    }
                    break;
                }

                case 'v': {  /* -v or -vv for different verbose levels */
                    setVerbose(1);
                    if (thisArg.length() == 3 && thisArg.charAt(2) == 'v') {
                        setVerbose(2);
                    }
                    break;
                }


                        
                default:
                    return applicationError("Bad option " + option);
                }
            }

        }

        // all ok
        return null;
    }


    /**
     * Extra startup - useful in subclasses
     * Return null on success
     * Return ApplicationResponse if there is an error
     */
    protected ApplicationResponse startHook() {
        // Start the BPPUnpack
        unpack = new BPPUnpack(bandwidthBits, packetsPerSecond, outStream);

        // set up TimerTask
        volumeTask = new TimedVolume(this);

        // if there is no timer, start one
        if (volumeTimer == null) {
            volumeTimer = new Timer();
            // run now and every 1 second
            volumeTimer.schedule(volumeTask, 0, 1000);
        }

        return null;
    }
    
    /**
     * Extra stop - useful in subclasses
     * Return null on success
     * Return ApplicationResponse if there is an error
     */
    protected ApplicationResponse stopHook() {
        // if there is a timer, stop it
        if (volumeTimer != null) {
            volumeTask.cancel();
        }
        
        return null;
    }

    /**
     * Process the recevied Datagram
     */
    
    /**
     * The callback for when a Datagram is received by an Intercepter.
     * Return a Datagram to forward Datagram, Return null to throw it away.
     */
    public Datagram datagramProcess(InterceptListener intercepter, Datagram datagram) {
        if (datagram.getProtocol() == Protocol.CONTROL) {
            Logger.getLogger("log").log(USR.STDOUT, "INTERCEPT: " + "CONTROL" + ". \n");
            return null;
            
        } else {

            count++;
            countLastSecond++;

            try {
                // increase volumeIn
                volumeIn += datagram.getPayload().length;
                volumeInLastSecond += datagram.getPayload().length;
                
                // do the conversion
                byte[] result = unpack.convert(count, datagram);

                // now patch up the datagram to have modified contents

                if (result == null) {
                    // nothnig to do but forward the datagram

                    // increase volumeOut
                    volumeOut += datagram.getPayload().length;
                    volumeOutLastSecond += datagram.getPayload().length;

                    // Get the network function to forward the packet
                    return datagram;

                } else {
                    // we have new payload
                    outStream.println("datagram length = " + datagram.getPayload().length + " newPayload length = " + result.length);
                    
                    Datagram newDatagram = DatagramFactory.newDatagram(result);
                    newDatagram.setDstAddress(datagram.getDstAddress());
                    newDatagram.setDstPort(datagram.getDstPort());

                    // increase volumeOut
                    volumeOut += newDatagram.getPayload().length;
                    volumeOutLastSecond += newDatagram.getPayload().length;

                    // DO NOT get the network function to forward the packet
                    // send our one
                    return newDatagram;
                }                
            } catch (Exception e) {
                System.err.println(e.getClass() + ": " + e.getMessage());
                return null;
            }
        }
    }

    /**
     * The callback for each time the timer goes off
     */
    private void timerCallback(int seconds) {
        printVolume(seconds, outStream);
        countLastSecond = 0;
        volumeInLastSecond = 0;
        volumeOutLastSecond = 0;

        if (adaptivePPS) {
            // tell BPPUnpack about the no of packets per second
            if (countLastSecond > 0) {
                unpack.setPacketsPerSecond(countLastSecond);
            }
        }
            
    }
    
    /**
     * Set the packets per second
     */
    protected void setPacketsPerSecond(int c) {
        packetsPerSecond = c;
    }

    /**
     * Set the bandwidth (as Mbits)
     */
    protected void setBandwidth(float b) {
        bandwidthBits = (int)(b * 1024 * 1024);
        bandwidth = bandwidthBits >> 3;
        System.out.println("BPPFn: bandwidthBits = " + bandwidthBits + " bandwidth = " + bandwidth);
    }

    /**
     * Print out some data
     */
    private void printVolume(int seconds, PrintStream outStream) {
        outStream.printf("VOLUME @ %-4d: in:%-9d bits:%-10d out:%-9d bits:%-10d\n", seconds, volumeIn, volumeIn * 8, volumeOut, volumeOut * 8);

        String colour = "";
        String end = "";

        if (volumeInLastSecond > bandwidth) {
            colour = ANSI.RED_BG;
            end = ANSI.RESET_COLOUR;
        }

        outStream.printf("SECOND @ %-4d: count: %-4d in:%s%-9d%s bits:%-10d out:%-9d bits:%-10d diff: %-8d\n",  seconds, countLastSecond, colour, volumeInLastSecond, end, volumeInLastSecond * 8, volumeOutLastSecond, volumeOutLastSecond * 8, volumeInLastSecond - volumeOutLastSecond);
    }
    
    /**
     * Process a reconfiguration JSONObject
     */
    @Override
    public Object process(JSONObject jsobj) {
        System.out.println("BPPFn: Reconfiguration with: " + jsobj);
        return null;
    }


    //------------------------------------------------------------------------//
    
    /**
     * Class to check input and output data volume on a regular basis - every second
     */
    private class TimedVolume extends TimerTask {
        BPPFn bppfn;
        boolean running = false;
        // no of seconds
        int secs = 0;

        public TimedVolume(BPPFn bppfn) {
            this.bppfn = bppfn;
            running = true;
        }

    
        public void run() {
            if (running) {
                secs++;
                bppfn.timerCallback(secs); 
            }
        }

        public boolean cancel() {
            if (running) {
                running = false;
            }

            return running;
        }
    }

    
    
}
