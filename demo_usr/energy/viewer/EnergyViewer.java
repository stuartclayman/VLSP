package demo_usr.energy.viewer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.HashMap;

import us.monoid.json.JSONObject;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;

import java.net.ConnectException;
import us.monoid.web.Resty;

import usr.common.ANSI;
import demo_usr.energy.energymodel.EnergyModel;


public class EnergyViewer {
    // Controller host
    String host;

    // Controller port
    int port;

    // Current snapshot no
    int count = 0;

    // The timeout for each wait
    int timeout = 2000;

    // A Rest end-point 
    Resty rest;

    // Map of router name to energy consumption
    Map<String, Double>routerEnergy;

    // Total energy usage
    double totalEnergyUsage = 0.0;

    double previousEnergyPerCent = 0.0;
    double alpha = 0.5;

    /**
     * Construct a EnergyViewer.
     * It connects to a Controller on port 8888 of localhost
     */
    public EnergyViewer()  {
        this("localhost", 8888);
    }

    /**
     * Construct a EnergyViewer.
     * It connects to a Controller on port 8888 of a specified host
     */
    public EnergyViewer(String host) {
        this(host, 8888);
    }

    /**
     * Construct a EnergyViewer.
     * It connects to a Controller on the specified port of a host.
     */
    public EnergyViewer(String host, int port) {
        this.host = host;
        this.port = port;

        rest = new Resty(Resty.Option.timeout(1000));

        routerEnergy = new HashMap<String, Double>();
    }


    /**
     * Connect
     */
    protected boolean connect() {
        // clear the screen
        System.out.print(ANSI.CLEAR);

        return true;
    }

    /**
     * Interact with the Controller.
     * This makes a REST call and gets back a graph in 'dot' format.
     * See graphviz for more info.
     */
    protected JSONObject interact(String req ) throws IOException, JSONException {
        String controllerURI = "http://" + host + ":" + Integer.toString(port);

        String uri = controllerURI + req;

        JSONObject response = rest.json(uri).toObject();

        // return the value
        return response;
    }


    /**
     * Collect data from the Controller.
     */
    protected void collectData() {
        // loop forever
        while (true) {

            //System.err.println("Collection: " + count);

            /*
             * Send a version of the network in graphviz dot
             * format to 'dot' and get a version back as xdot.
             */
            try {
                JSONObject localhostInfo = interact("/localcontroller/?detail=all");

                JSONObject routerThreadGroups = interact("/router/?detail=threadgroup");

                presentData(localhostInfo, routerThreadGroups);

            } catch (ConnectException ce) {
                // no Controller - so loop around and try again
                System.err.println("Viewer error: No GlobalController");
            } catch (IOException ioe) {
                System.err.println("Viewer error: " + ioe);
            } catch (JSONException me) {
                System.err.println("Viewer error: " + me);

                // if the Controller has gone away
                // then hang around indefinitely to drive the UI
                    synchronized (this) {
                        try {
                            System.err.println("Waiting.....");
                            wait();
                        } catch (InterruptedException ie) {
                        }
                    }
            }

            count++;

            try {
                Thread.sleep(timeout);
            } catch (InterruptedException ie) {
            }

        }
    }


    /**
     * Print the data
     */
    protected void presentData(JSONObject localhostInfo, JSONObject routerThreadGroups) {
        double energyTotalThisTime = 0.0;
        double energyDeltaThisTime = 0.0;


        try {
            System.out.print(ANSI.POS(0,0));


            // get EnergyModel
            // {  "baseLineEnergyConsumption": 20,  "cpuIdleCoefficient": 0.2,  "cpuLoadCoefficient": 50,  "freeMemoryCoefficient": 0.02,  "memoryAllocationCoefficient": 0.4,  "networkIncomingBytesCoefficient": 0.0002,  "networkOutboundBytesCoefficient": 0.0002 }
             JSONObject energyFactors = localhostInfo.getJSONArray("detail").getJSONObject(0).getJSONObject("energyFactors");
             
             double baseLineEnergyConsumption = energyFactors.getDouble("baseLineEnergyConsumption");
             double cpuIdleCoefficient = energyFactors.getDouble("cpuIdleCoefficient");
             double cpuLoadCoefficient = energyFactors.getDouble("cpuLoadCoefficient");
             double freeMemoryCoefficient = energyFactors.getDouble("freeMemoryCoefficient");
             double memoryAllocationCoefficient = energyFactors.getDouble("memoryAllocationCoefficient");
             double networkIncomingBytesCoefficient = energyFactors.getDouble("networkIncomingBytesCoefficient");
             double networkOutboundBytesCoefficient = energyFactors.getDouble("networkOutboundBytesCoefficient");
             
             EnergyModel energyModel = new EnergyModel (cpuLoadCoefficient, cpuIdleCoefficient, memoryAllocationCoefficient, freeMemoryCoefficient, networkOutboundBytesCoefficient, networkIncomingBytesCoefficient,  baseLineEnergyConsumption);

            // get hostinfo
            // .detail[0].hostinfo
            // {"cpuIdle":0.6848000288009644,"cpuLoad":0.3149999976158142,"freeMemory":5.7851562,"name":"localhost:10000","networkIncomingBytes":536,"networkOutboundBytes":2075,"usedMemory":10.208984}

            JSONObject hostinfo = localhostInfo.getJSONArray("detail").getJSONObject(0).getJSONObject("hostinfo");

            

            System.out.print("CPU Usage: ");
            System.out.printf("%2.2f%% used", hostinfo.getDouble("cpuLoad") * 100);
            System.out.printf("  %2.2f%% idle", hostinfo.getDouble("cpuIdle") * 100);

            System.out.print("        Mem Usage: ");
            System.out.printf("%2.2f Gb used", hostinfo.getDouble("usedMemory") );
            System.out.printf("  %2.2f Gb free", hostinfo.getDouble("freeMemory"));


            System.out.print(ANSI.CLEAR_EOL);
            System.out.print(ANSI.COLUMN(0));
            System.out.print(ANSI.DOWN(1));


            double energyUsage = calculateEnergy(energyModel, hostinfo);
            totalEnergyUsage += energyUsage;

            System.out.print("Energy Usage: ");
            System.out.printf("%7.2f", totalEnergyUsage);
            System.out.print(" Total (Watts)");
            System.out.printf("   %5.2f", energyUsage);
            System.out.print(" Delta (Watts)");

            System.out.print(ANSI.CLEAR_EOL);
            System.out.print(ANSI.COLUMN(0));
            System.out.print(ANSI.DOWN(2));


            System.out.printf("%-16s%-12s%-12s%-12s%-12s%-12s%-12s%-12s", "name", "elapsed (s)","cpu (ms)","user (ms)","sys (ms)","mem (k)", "energy (W)", "delta (W)");
            System.out.print(ANSI.COLUMN(0));
            System.out.print(ANSI.DOWN(1));

            // get thread info:
            // {"address":"6","linkIDs":[458788,2293796,1835044,917540],"links":[1,5,4,2],"mgmtPort":11010,"name":"Router-6","r2rPort":11011,"routerID":6,"threadgroup":[{...}]}
            JSONArray routerDetail = routerThreadGroups.getJSONArray("detail");

            for (int detail = 0;  detail < routerDetail.length(); detail++) {
                // info per router
                JSONObject routerInfo = routerDetail.getJSONObject(detail);

                String name = routerInfo.getString("name");

                JSONArray threadGroups = routerInfo.getJSONArray("threadgroup");


                // skip through thread info to find the TOTALs
                for (int thread = 0;  thread < threadGroups.length(); thread++) {
                    // info per thread
                    // {"cpu":527235,"elapsed":"[40.277]","mem":16528,"name":"TOTAL","starttime":"[2014/11/25 17:26:54.869]","system":44603,"user":482632}
                    JSONObject threadInfo = threadGroups.getJSONObject(thread);

                    String threadName = threadInfo.getString("name");

                    if (threadName.equals("TOTAL")) {

                        long elapsed = threadInfo.getLong("elapsed");
                        long cpu = threadInfo.getLong("cpu");
                        long user = threadInfo.getLong("user");
                        long sys = threadInfo.getLong("system");
                        long mem = threadInfo.getLong("mem");


                        // try energy
                        double routerEnergyConsumption = energyModel.ProcessingConsumptionFunction((cpu + user + sys)/1000000f, 0) + energyModel.MemoryConsumptionFunction (mem/1000f, 0);

                        double energyDelta = 0.0;

                        Double routerLastEnergyDelta = routerEnergy.get(name);

                        if (routerLastEnergyDelta == null) {
                            // never seen this router
                        } else {
                            energyDelta = routerEnergyConsumption - routerLastEnergyDelta;

                            // the following case should not occur, but it does
                            // need to look into it further.
                            if (energyDelta < 0) {
                                energyDelta = 0;
                            }
                        }


                        // ruuning totals
                        energyTotalThisTime += routerEnergyConsumption;
                        energyDeltaThisTime += energyDelta;


                        System.out.printf("%-16s%-12.2f%-12.3f%-12.3f%-12.3f%-12s%-12.3f%-12.3f", name, elapsed/1000f, cpu/1000f, user/1000f, sys/1000f, mem, routerEnergyConsumption, energyDelta);

                        System.out.print(ANSI.CLEAR_EOL);
                        System.out.print(ANSI.COLUMN(0));

                        System.out.print(ANSI.DOWN(1));

                        routerEnergy.put(name, routerEnergyConsumption);

                    }
                }
            }


            System.out.printf("%-16s%-12s%-12s%-12s%-12s%-12s%-12s%-10s", "", "","","","","", "----------", "--------");
            System.out.print(ANSI.CLEAR_EOL);
            System.out.print(ANSI.COLUMN(0));

            System.out.print(ANSI.DOWN(1));

            double energyPerCent = (energyDeltaThisTime * 100) / energyUsage;

            // cleanup energyPerCent
            // sometimes this case happens
            if (energyPerCent > 100.0) {
                energyPerCent = 100.0;
            }

            // setup previousEnergyPerCent
            if (previousEnergyPerCent == 0.0) {
                previousEnergyPerCent = energyPerCent;
            }

            // smooth the percent age
            double smooth_value  = (1 - alpha) *  previousEnergyPerCent + alpha * energyPerCent;

            System.out.printf("%-16s%-12s%-12s%-12s%-12s%-12s%-12.3f%-10.3f%-4.2f%% %-4.2f%%", "", "","","","","", energyTotalThisTime, energyDeltaThisTime, energyPerCent, smooth_value);

            previousEnergyPerCent = smooth_value;

            System.out.print(ANSI.CLEAR_EOS);

                
        } catch (JSONException jse) {
        }
    }


    protected double calculateEnergy(EnergyModel energyModel, JSONObject measurementJsobj) {
        // current status of localcontroller
        float currentCPUUserAndSystem=0;
        float currentCPUIdle=0;
        float currentMemoryUsed=0;
        float currentFreeMemory=0;
        long currentOutputBytes=0;
        long currentInputBytes=0;

        if (measurementJsobj!=null) {
            // extracted required measurements for the energy model
            try {
                currentCPUUserAndSystem = (float) measurementJsobj.getDouble("cpuLoad");
                currentCPUIdle = (float) measurementJsobj.getDouble("cpuIdle");
                currentMemoryUsed = measurementJsobj.getInt("usedMemory");
                currentFreeMemory = measurementJsobj.getInt("freeMemory");
                currentOutputBytes = measurementJsobj.getLong("networkOutboundBytes");
                currentInputBytes = measurementJsobj.getLong("networkIncomingBytes");
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }


            // Get energy usage
            double energyConsumption = energyModel.CurrentEnergyConsumption(currentCPUUserAndSystem, currentCPUIdle, currentMemoryUsed, currentFreeMemory, currentOutputBytes, currentInputBytes);

            return energyConsumption;
        } else {
            return 0.0;
        }
    }


    /**
     * Set the timeout
     */
    void setTimeout(int t) {
        timeout = t;
    }

    
    /**
     * The main entry point
     * EnergyViewer args:
     * -t timeout, timeout between wakeup (default: 2 secs)
     * -g gc_host, the host of the global controller (default: localhost)
     * -p gc_port, the port of the global controller (default: 8888)
     * -h, help
     */
    public static void main(String[] args) {
        String gcHost = "localhost";
        int gcPort = 8888;
        int timeout = 2000;

        EnergyViewer viewer = null;

        // process args
        int argc = args.length;

        for (int arg=0; arg < argc; arg++) {
            String thisArg = args[arg];

            // check if its a flag
            if (thisArg.charAt(0) == '-') {
                // get option
                char option = thisArg.charAt(1);

                switch (option) {

                case 'h': {
                    help();
                    System.exit(0);
                    break;
                }

                case 'g': {
                    // gwet next arg
                    String argValue = args[++arg];

                    gcHost = argValue;
                    break;
                }

                case 'p': {
                    // gwet next arg
                    String argValue = args[++arg];

                    try {
                        gcPort = Integer.parseInt(argValue);
                    } catch (Exception e) {
                        System.err.println("Error: " + e);
                        System.exit(1);
                    }
                    break;
                }

                case 't': {
                    // gwet next arg
                    String argValue = args[++arg];

                    try {
                        int secs = Integer.parseInt(argValue);
                        timeout = secs * 1000;
                    } catch (Exception e) {
                        System.err.println("Error: " + e);
                        System.exit(1);
                    }
                    break;
                }

                default:
                    System.err.println("EnergyViewer: unknown option " + option);
                    break;

                }
            }
        }

        try {
            // start a EnergyViewer
            viewer = new EnergyViewer(gcHost, gcPort);
            // set the timeout
            viewer.setTimeout(timeout);

            // connect to the Controller
            boolean connected;

            System.err.print("Connecting to " + viewer.host + "/" + viewer.port + " ");

            // sit an wait until can connect
            viewer.connect();

            // now start collecting data
            viewer.collectData();
        } catch (Exception e) {
            System.err.println("EnergyViewer Exception: " + e);
            System.exit(1);
        }
    }

    private static void help() {
        System.err.println("EnergyViewer [-t timeout] [-h gc_host]  [-p gc_port] ");
        System.exit(1);
    }

}
