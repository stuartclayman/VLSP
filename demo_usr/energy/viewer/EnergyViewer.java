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
import demo_usr.energy.energymodel.EnergyModelLinear;

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

    // Map of router ID to energy consumption
    Map<Integer, Double>routerEnergy;
    // Map of router ID to energy consumption
    Map<Integer, Double>routerEnergyLastTime;
    // Map of router ID to energy model to use to calculate energy 
    Map<Integer, EnergyModelLinear>routerEnergyModel;
    // Map of localcontroller name to total energy used
    Map<String, Double>localcontrollerEnergy;
    // Map of localcontroller name to total energy used
    Map<String, Double>localcontrollerEnergyLastTime;
    // Map of localcontroller name to total energy used by VMs
    Map<String, Double>localcontrollerEnergyVM;

    // Total energy usage
    double totalEnergyUsage = 0.0;

    double previousEnergyPerCent = 0.0;
    double alpha = 0.5;

    // Are we showing energy prices
    boolean showEnergyPrices = false;

    // the energy price
    double energyPrice = 0.0;
    
    // A console to accept energy prices requests
    EnergyViewerConsole console;

    int consolePort = 0;
    
    // show localcontroller info
    boolean showLocalcontrollerInfo = false;
    
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

        //rest = new Resty(Resty.Option.timeout(1000));
        rest = new Resty();

        routerEnergy = new HashMap<Integer, Double>();
        routerEnergyLastTime = new HashMap<Integer, Double>();
        routerEnergyModel = new HashMap<Integer, EnergyModelLinear>();
        localcontrollerEnergy = new HashMap<String, Double>();
        localcontrollerEnergyLastTime = new HashMap<String, Double>();
        localcontrollerEnergyVM = new HashMap<String, Double>();
    }
    

    /**
     * Connect
     */
    protected boolean connect() {
        // clear the screen
        System.out.print(ANSI.CLEAR);

        if (showEnergyPrices) {
            System.out.print("EnergyViewer: starting EnergyViewerConsole on port " + consolePort);

            console = new EnergyViewerConsole(this, consolePort);
            console.start();
        }

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
             * Ger detail on localcontrollers and routers
             */
            try {
                JSONObject localhostInfo = interact("/localcontroller/?detail=all");

                JSONObject routerThreadGroups = interact("/router/?detail=threadgroup");

                presentData(localhostInfo, routerThreadGroups);

            } catch (ConnectException ce) {
                // no Controller - so loop around and try again
                System.out.print(ANSI.POS(0,0));
                System.out.print(ANSI.CLEAR_EOS);
                System.out.print("EnergyViewer: No VIM.  Waiting......");
            } catch (IOException ioe) {
                System.err.println("Viewer error: " + ioe);
            } catch (JSONException me) {
                System.err.println("Viewer error: " + me);

                // if the Controller has gone away
                // then hang around indefinitely to drive the UI
                // no Controller - so loop around and try again
                System.out.print(ANSI.POS(0,0));
                System.out.print(ANSI.CLEAR_EOS);
                System.out.print("EnergyViewer: No VIM. " + me.getMessage() + " Waiting......");
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

            JSONArray localcontrollers = localhostInfo.getJSONArray("list");
            int localcontrollerCount = localcontrollers.length();


            /*  get EnergyModel and coefficients per Localcontroller */
            
            // {  "baseLineEnergyConsumption": 20,  "cpuIdleCoefficient": 0.2,  "cpuLoadCoefficient": 50,  "freeMemoryCoefficient": 0.02,  "memoryAllocationCoefficient": 0.4,  "networkIncomingBytesCoefficient": 0.0002,  "networkOutboundBytesCoefficient": 0.0002 }
            JSONArray hostDetail = localhostInfo.getJSONArray("detail");


            // return if there is nothing to do
            if (hostDetail.length() == 0) {
                System.out.print(ANSI.CLEAR_EOS);
                System.out.print("Waiting for LocalController......");
                return;
            }


            // Skip through all LocalControllers
            for (int local = 0;  local < hostDetail.length(); local++) {

                // first we work out the energy coefficients / factors
                JSONObject energyFactors = hostDetail.getJSONObject(local).getJSONObject("energyFactors");
             
                double baseLineEnergyConsumption = energyFactors.getDouble("baseLineEnergyConsumption");
                double cpuIdleCoefficient = energyFactors.getDouble("cpuIdleCoefficient");
                double cpuLoadCoefficient = energyFactors.getDouble("cpuLoadCoefficient");
                double freeMemoryCoefficient = energyFactors.getDouble("freeMemoryCoefficient");
                double memoryAllocationCoefficient = energyFactors.getDouble("memoryAllocationCoefficient");
                double networkIncomingBytesCoefficient = energyFactors.getDouble("networkIncomingBytesCoefficient");
                double networkOutboundBytesCoefficient = energyFactors.getDouble("networkOutboundBytesCoefficient");
             
                EnergyModelLinear energyModel = new EnergyModelLinear (cpuLoadCoefficient, cpuIdleCoefficient, memoryAllocationCoefficient, freeMemoryCoefficient, networkOutboundBytesCoefficient, networkIncomingBytesCoefficient,  baseLineEnergyConsumption);

                // save energy model per router
                JSONArray routers = hostDetail.getJSONObject(local).getJSONArray("routers");

                for (int r=0; r < routers.length(); r++ ) {
                    int routerID = routers.getInt(r);

                    routerEnergyModel.put(routerID, energyModel);
                }



                /* get hostinfo */
             
                // .detail[0].hostinfo
                // {"cpuIdle":0.6848000288009644,"cpuLoad":0.3149999976158142,"freeMemory":5.7851562,"name":"localhost:10000","networkIncomingBytes":536,"networkOutboundBytes":2075,"usedMemory":10.208984}

                JSONObject hostinfo = hostDetail.getJSONObject(local).getJSONObject("hostinfo");

            
                // Calculate Energy Usage
                String name = hostinfo.getString("name");
                double energyUsage = calculateEnergy(energyModel, hostinfo) / 60;

                localcontrollerEnergy.put(name, energyUsage);

                // keep running total
                totalEnergyUsage += energyUsage;
            }

            /*  get router thread energy info */
            // {"address":"6","linkIDs":[458788,2293796,1835044,917540],"links":[1,5,4,2],"mgmtPort":11010,"name":"Router-6","r2rPort":11011,"routerID":6,"threadgroup":[{...}]}
            JSONArray routerDetail = routerThreadGroups.getJSONArray("detail");

            // "list":[31,22,27,13,34,33,35,30,16], "type": "router"
            JSONArray routerDetailList = routerThreadGroups.getJSONArray("list");

            
            // Skip through all LocalControllers
            for (int local = 0;  local < hostDetail.length(); local++) {

                JSONObject hostinfo = hostDetail.getJSONObject(local).getJSONObject("hostinfo");
                String hostname = hostinfo.getString("name");

                // get list of  routers per LocalController
                JSONArray routers = hostDetail.getJSONObject(local).getJSONArray("routers");

                // skip through every router in this LocalController
                for (int r=0; r < routers.length(); r++ ) {
                    int routerID = routers.getInt(r);

                    // now find the offset of router ID in the detail list
                    int detail = 0;

                    while (routerDetailList.getInt(detail) != routerID) detail++;

                    // info per router
                    JSONObject routerInfo = routerDetail.getJSONObject(detail);

                    // WAS for (int detail = 0;  detail < routerDetail.length(); detail++) {

                    //String name = routerInfo.getString("name");
                    //Integer routerID = routerInfo.getInt("routerID");

                    JSONArray threadGroups = routerInfo.getJSONArray("threadgroup");


                    // skip through thread info to find the TOTALs
                    for (int thread = 0;  thread < threadGroups.length(); thread++) {
                        // info per thread
                        // {"cpu":527235,"elapsed":"[40.277]","mem":16528,"name":"TOTAL","starttime":"[2014/11/25 17:26:54.869]","system":44603,"user":482632}
                        JSONObject threadInfo = threadGroups.getJSONObject(thread);

                        String threadName = threadInfo.getString("name");

                        // we've found the TOTAL row
                        if (threadName.equals("TOTAL")) {

                            long elapsed = threadInfo.getLong("elapsed");
                            long cpu = threadInfo.getLong("cpu");
                            long user = threadInfo.getLong("user");
                            long sys = threadInfo.getLong("system");
                            long mem = threadInfo.getLong("mem");


                            // Calculate energy consumption
                            EnergyModelLinear energyModel = routerEnergyModel.get(routerID);

                            double routerEnergyConsumption = (energyModel.ProcessingConsumptionFunction((cpu + user + sys)/1000000f, 0) + energyModel.MemoryConsumptionFunction (mem/1000f, 0)) / 60;

                            // save router energy comsumption
                            routerEnergy.put(routerID, routerEnergyConsumption);

                            Double routerLastEnergyDelta = routerEnergyLastTime.get(routerID);
                            Double energyThisTime = 0.0;
                            
                            if (routerLastEnergyDelta == null) {
                                // never seen this router
                                energyThisTime = routerEnergyConsumption;
                            } else {
                                energyThisTime = routerEnergyConsumption - routerLastEnergyDelta;

                                // the following case should not occur, but it does
                                // need to look into it further.
                                if (energyThisTime < 0) {
                                    energyThisTime = 0.0;
                                }
                            }



                            // add router energy comsumption for this LocalController
                            if (localcontrollerEnergyVM.containsKey(hostname)) {
                                localcontrollerEnergyVM.put(hostname, localcontrollerEnergyVM.get(hostname) + energyThisTime);
                            } else {
                                localcontrollerEnergyVM.put(hostname, energyThisTime);
                            }
                        }
                    }
                }
            }

            /* now do output */

            if (showLocalcontrollerInfo) {
                System.out.printf("%-6s", "LC");
            }

            System.out.printf("%-20s%-20s%-36s", "CPU", "Memory","Energy");

            if (showEnergyPrices) {
                System.out.printf("%-24s", "Price");
            }

            System.out.print(ANSI.CLEAR_EOL);
            System.out.print(ANSI.COLUMN(0));
            System.out.print(ANSI.DOWN(1));

            if (showLocalcontrollerInfo) {
                System.out.printf("%-6s", "");
            }

            System.out.printf("%-10s%-10s%-10s%-10s%-12s%-12s%-12s", "used %", "idle %","used Gb","free Gb", "total Wh","delta Wh", "virtual Wh");

            if (showEnergyPrices) {
                System.out.printf("%-12s%-12s%-6s", "total", "delta", "unit");
            }

            System.out.print(ANSI.CLEAR_EOL);
            System.out.print(ANSI.COLUMN(0));
            System.out.print(ANSI.DOWN(1));


            // Skip through all LocalControllers
            for (int local = 0;  local < hostDetail.length(); local++) {

                if (showLocalcontrollerInfo) {
                    System.out.printf("%-6s", local+1);
                }

                /* get hostinfo */
             
                // .detail[0].hostinfo
                // {"cpuIdle":0.6848000288009644,"cpuLoad":0.3149999976158142,"freeMemory":5.7851562,"name":"localhost:10000","networkIncomingBytes":536,"networkOutboundBytes":2075,"usedMemory":10.208984}

                JSONObject hostinfo = hostDetail.getJSONObject(local).getJSONObject("hostinfo");

            
                // CPU
                System.out.printf("%-10.2f", hostinfo.getDouble("cpuLoad") * 100);
                System.out.printf("%-10.2f", hostinfo.getDouble("cpuIdle") * 100);

                // Memory
                System.out.printf("%-10.2f", hostinfo.getDouble("usedMemory") );
                System.out.printf("%-10.2f", hostinfo.getDouble("freeMemory"));


                // net
                //System.out.printf("%-10.2f", hostinfo.getDouble("networkIncomingBytes") / 100);
                //System.out.printf("%-10.2f", hostinfo.getDouble("networkOutboundBytes") / 100);


                // Calculate Energy Usage
                String name = hostinfo.getString("name");

                double localcontrollerEnergyLastTimeValue = 0.0;
                double localcontrollerEnergyVMNow = 0.0;
                
                if (localcontrollerEnergyLastTime.containsKey(name)) {
                    localcontrollerEnergyLastTimeValue = localcontrollerEnergyLastTime.get(name);
                }

                if (localcontrollerEnergyVM.containsKey(name)) {
                    localcontrollerEnergyVMNow = localcontrollerEnergyVM.get(name);
                }

                double localcontrollerEnergyNow = localcontrollerEnergy.get(name);

                double localcontrollerEnergyTotal = localcontrollerEnergyLastTimeValue + localcontrollerEnergyNow;


                // Energy Usage
                System.out.printf("%-12.2f", localcontrollerEnergyTotal);
                System.out.printf("%-12.2f", localcontrollerEnergyNow);
                System.out.printf("%-12.2f", localcontrollerEnergyVMNow);

                if (showEnergyPrices) {

                    // Energy price
                    System.out.printf("%-12.2f", localcontrollerEnergyTotal * energyPrice);
                    System.out.printf("%-12.2f", localcontrollerEnergyNow * energyPrice);
                    System.out.printf("%-6.1f", energyPrice);
                }

                localcontrollerEnergyLastTime.put(name, localcontrollerEnergyTotal);

                
                System.out.print(ANSI.CLEAR_EOL);
                System.out.print(ANSI.COLUMN(0));
                System.out.print(ANSI.DOWN(1));

            }
            
            System.out.print(ANSI.CLEAR_EOL);
            System.out.print(ANSI.COLUMN(0));
            System.out.print(ANSI.DOWN(2));

            if (showLocalcontrollerInfo) {
                System.out.printf("%-6s", "LC");
            }

            // Now output info for each router
            System.out.printf("%-16s%-12s%-12s%-12s%-12s%-12s%-12s%-12s", "name", "elapsed s","cpu ms","user ms","sys ms","mem k", "energy Wh", "delta Wh");

            if (showEnergyPrices) {
                System.out.printf("%-12s%-12s", "total E", "delta E)");
            }

            System.out.print(ANSI.CLEAR_EOL);
            System.out.print(ANSI.COLUMN(0));
            System.out.print(ANSI.DOWN(1));

            
            // Skip through all LocalControllers
            for (int local = 0;  local < hostDetail.length(); local++) {

                // get list of  routers per LocalController
                JSONArray routers = hostDetail.getJSONObject(local).getJSONArray("routers");

                // skip through every router in this LocalController
                for (int r=0; r < routers.length(); r++ ) {
                    int routerID = routers.getInt(r);

                    // now find the offset of router ID in the detail list
                    int detail = 0;

                    while (routerDetailList.getInt(detail) != routerID) detail++;

                    // info per router
                    JSONObject routerInfo = routerDetail.getJSONObject(detail);

                    String name = routerInfo.getString("name");
                    Integer detailRouterID = routerInfo.getInt("routerID");


                    if (showLocalcontrollerInfo) {
                        System.out.printf("%-6s", local+1);
                    }



                    JSONArray threadGroups = routerInfo.getJSONArray("threadgroup");

                    // skip through thread info to find the TOTALs
                    for (int thread = 0;  thread < threadGroups.length(); thread++) {
                        // info per thread
                        // {"cpu":527235,"elapsed":"[40.277]","mem":16528,"name":"TOTAL","starttime":"[2014/11/25 17:26:54.869]","system":44603,"user":482632}
                        JSONObject threadInfo = threadGroups.getJSONObject(thread);

                        String threadName = threadInfo.getString("name");

                        // we've found the TOTAL row
                        if (threadName.equals("TOTAL")) {

                            long elapsed = threadInfo.getLong("elapsed");
                            long cpu = threadInfo.getLong("cpu");
                            long user = threadInfo.getLong("user");
                            long sys = threadInfo.getLong("system");
                            long mem = threadInfo.getLong("mem");


                            double energyDelta = 0.0;

                            Double routerLastEnergyDelta = routerEnergyLastTime.get(routerID);
                            Double routerEnergyConsumption = routerEnergy.get(routerID);

                            if (routerLastEnergyDelta == null) {
                                // never seen this router
                                energyDelta = routerEnergyConsumption;
                            } else {
                                energyDelta = routerEnergyConsumption - routerLastEnergyDelta;

                                // the following case should not occur, but it does
                                // need to look into it further.
                                if (energyDelta < 0) {
                                    energyDelta = 0.0;
                                }
                            }


                            // ruuning totals
                            energyTotalThisTime += routerEnergyConsumption;
                            energyDeltaThisTime += energyDelta;


                            // output info for this router
                            System.out.printf("%-16s%-12.2f%-12.3f%-12.3f%-12.3f%-12s%-12.3f%-12.3f", name, elapsed/1000f, cpu/1000f, user/1000f, sys/1000f, mem, routerEnergyConsumption, energyDelta);

                            if (showEnergyPrices) {
                                System.out.printf("%-12.3f%-12.3f", routerEnergyConsumption * energyPrice, energyDelta * energyPrice);
                            }

                            System.out.print(ANSI.CLEAR_EOL);
                            System.out.print(ANSI.COLUMN(0));
                            System.out.print(ANSI.DOWN(1));

                            // save for next time
                            routerEnergyLastTime.put(routerID, routerEnergyConsumption);

                        }
                    }
                }

            }



            /*  output summary line */
            
            if (showLocalcontrollerInfo) {
                System.out.printf("%-6s", "");
            }

            System.out.printf("%-16s%-12s%-12s%-12s%-12s%-12s%-12s%-10s", "", "","","","","", "----------", "--------");

            System.out.print(ANSI.CLEAR_EOL);
            System.out.print(ANSI.COLUMN(0));
            System.out.print(ANSI.DOWN(1));

            double energyPerCent = (energyDeltaThisTime * 100) / energyTotalThisTime;  // WAS / energyUsage;

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

            if (showLocalcontrollerInfo) {
                System.out.printf("%-6s", "");
            }

            System.out.printf("%-16s%-12s%-12s%-12s%-12s%-12s%-12.3f%-10.3f%-4.2f%% %-4.2f%%", "", "","","","","", energyTotalThisTime, energyDeltaThisTime, energyPerCent, smooth_value);

            previousEnergyPerCent = smooth_value;

            System.out.print(ANSI.CLEAR_EOS);

                
        } catch (JSONException jse) {
            System.err.println("presentData: JSONException " + jse.getMessage());
        }
    }

    
    protected double calculateEnergy(EnergyModelLinear energyModel, JSONObject measurementJsobj) {
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
     * The handler calls this to set the energy price
     */
    protected void setEnergyPrice(Double price) {
        System.err.println("EnergyViewer price set to: " + price);
        energyPrice = price;
    }

    /**
     * Get the energy usage
     */
    protected double getEnergyUsage() {
        return totalEnergyUsage;
    }
    
    /**
     * The main entry point
     * EnergyViewer args:
     * -t timeout, timeout between wakeup (default: 2 secs)
     * -g gc_host, the host of the global controller (default: localhost)
     * -p gc_port, the port of the global controller (default: 8888)
     * -e, deal with energy prices POSTed via REST interface (default: false)
     * -c console_port, the port the energy console listens on (default: 9180)
     * -l, show separate LocalController info (default:false)
     * -h, help
     */
    public static void main(String[] args) {
        String gcHost = "localhost";
        int gcPort = 8888;
        int consolePort = 9180;
        int timeout = 2000;
        boolean prices = false;
        boolean localcontrollerInfo = false;

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


                case 'e': {
                    prices = true;
                    break;
                }

                case 'c': {
                    // gwet next arg
                    String argValue = args[++arg];

                    try {
                        consolePort = Integer.parseInt(argValue);
                    } catch (Exception e) {
                        System.err.println("Error: " + e);
                        System.exit(1);
                    }
                    break;
                }

                    

                case 'l': {
                    localcontrollerInfo = true;
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
            viewer.timeout = timeout;

            viewer.showEnergyPrices = prices;
            viewer.consolePort = consolePort;
            viewer.showLocalcontrollerInfo = localcontrollerInfo;
            

            // connect to the Controller
            boolean connected;

            System.err.print("Connecting to " + viewer.host + "/" + viewer.port + " ");

            // sit an wait until can connect
            viewer.connect();

            // now start collecting data
            viewer.collectData();
        } catch (Exception e) {
            System.err.println("EnergyViewer Exception: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void help() {
        System.err.println("EnergyViewer [-t timeout] [-g gc_host]  [-p gc_port] [-c console_port] [-e] [-l]");
        System.exit(1);
    }

}
