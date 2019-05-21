/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demo_usr.necos.probes.vlsp;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;
import demo_usr.energy.energymodel.EnergyModelLinear;
/**
 *
 * @author uceeftu
 */

//to be refoctored with an interface

public class VLSPDataCollector {
    String controllerURI;
    String controllerHost;
    int controllerPort;
    
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

    int consolePort = 0;
    
    // show localcontroller info
    //boolean showLocalcontrollerInfo = false;
    
    Resty rest;
    
    private Logger LOGGER = LoggerFactory.getLogger(VLSPDataCollector.class);
    
    
    
    public VLSPDataCollector(String dockerHost, int dockerPort) throws UnknownHostException {
        this.controllerHost = dockerHost;
        this.controllerPort = dockerPort;
        //this.containerId = cId;
        
        routerEnergy = new HashMap<>();
        routerEnergyLastTime = new HashMap<>();
        routerEnergyModel = new HashMap<>();
        localcontrollerEnergy = new HashMap<>();
        localcontrollerEnergyLastTime = new HashMap<>();
        localcontrollerEnergyVM = new HashMap<>();
        
        initialize(InetAddress.getByName(this.controllerHost), this.controllerPort);
    }
    
    private synchronized void initialize(InetAddress addr, int port) {
        this.controllerPort = port;
        this.controllerURI = "http://" + addr.getHostName() + ":" + Integer.toString(port);
        rest = new Resty();
    }
    
    
    protected JSONObject interact(String req) throws IOException, JSONException {
        String uri = controllerURI + req;        
        JSONObject response = rest.json(uri).toObject();        
        return response;
    }
    
    
    protected JSONObject presentData(JSONObject localhostInfo, JSONObject routerThreadGroups) {        
        JSONObject measurements = new JSONObject();

        try {
            /*  get EnergyModel and coefficients per Localcontroller */
            JSONArray hostDetail = localhostInfo.getJSONArray("detail");


            // return if there is nothing to do
            if (hostDetail.length() == 0) {
                LOGGER.info("Waiting for LocalController......");
                return null;
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

            JSONArray localControllers = new JSONArray();
            JSONObject localController = null;
            
            // Skip through all LocalControllers
            for (int local = 0;  local < hostDetail.length(); local++) {
                
                localController = new JSONObject();
                
                localController.put("id", local+1);
                
                JSONObject hostinfo = hostDetail.getJSONObject(local).getJSONObject("hostinfo");

            
                // CPU
                localController.put("cpuLoad", hostinfo.getDouble("cpuLoad") * 100);
                localController.put("cpuIdle", hostinfo.getDouble("cpuIdle") * 100);

                // Memory
                localController.put("usedMemory", hostinfo.getDouble("usedMemory"));
                localController.put("freeMemory", hostinfo.getDouble("freeMemory"));
                
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
                localController.put("energyTot", localcontrollerEnergyTotal);
                localController.put("energyDelta", localcontrollerEnergyNow);
                localController.put("energyNow", localcontrollerEnergyNow);

                localcontrollerEnergyLastTime.put(name, localcontrollerEnergyTotal);
            }
            
            JSONArray routersInfo = new JSONArray();
            
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
                            
                            // save for next time
                            routerEnergyLastTime.put(routerID, routerEnergyConsumption);
                            
                            //System.out.printf("%-16s%-12s%-12s%-12s%-12s%-12s%-12s%-12s", "name", "elapsed s","cpu ms","user ms","sys ms","mem k", "energy Wh", "delta Wh");
                            JSONObject router = new JSONObject();
                            router.put("name", name);
                            router.put("elapsed", elapsed/1000f);
                            router.put("cpu", cpu/1000f);
                            router.put("user", user/1000f);
                            router.put("sys", sys/1000f);
                            router.put("mem", mem);
                            router.put("energyConsumption", routerEnergyConsumption);
                            router.put("energyDelta", energyDelta);
                            
                            routersInfo.put(router);
                        }
                    }
                    
                    
                    
                }
                
                if (localController != null) {
                    localController.put("routers", routersInfo);
                    localControllers.put(localController);
                }
                
                measurements.put("localcontrollers", localControllers);
                
                LOGGER.debug("Printing RAW collected measurements as JSON:");
                LOGGER.debug(measurements.toString(1));
                

            }   
        } catch (JSONException jse) {
            System.err.println("presentData: JSONException " + jse.getMessage());
        }
        
        return measurements;
    }
    
    
    JSONObject collectValues() throws JSONException, IOException {
        JSONObject measurements = null;
        
        try {
            JSONObject localhostInfo = interact("/localcontroller/?detail=all");

            JSONObject routerThreadGroups = interact("/router/?detail=threadgroup");

            measurements = presentData(localhostInfo, routerThreadGroups);

            } catch (ConnectException ce) {
                LOGGER.error("EnergyViewer: No VIM.  Waiting......");
            } catch (IOException ioe) {
                LOGGER.error("Viewer error: " + ioe);
            } catch (JSONException me) {
                LOGGER.error("Viewer error: " + me);

                // if the Controller has gone away
                // then hang around indefinitely to drive the UI
                // no Controller - so loop around and try again
                LOGGER.error("EnergyViewer: No VIM. " + me.getMessage() + " Waiting......");
            }
            return measurements;
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
    
    
    public static void main(String[] args) {
        try {
            VLSPDataCollector c = new VLSPDataCollector("localhost", 8888);
            c.collectValues();
            
        } catch (Exception e) {
            System.err.println("error");
        }
    }
    
}
