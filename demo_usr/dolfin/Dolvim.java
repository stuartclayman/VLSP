package demo_usr.dolfin;

import usr.globalcontroller.GlobalController;
import usr.globalcontroller.HostInfoReporter;
import usr.globalcontroller.ThreadGroupListReporter;
import usr.localcontroller.LocalControllerInfo;
import usr.common.BasicRouterInfo;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.json.JSONArray;
import usr.common.ANSI;
import usr.logging.Logger;
import usr.logging.USR;
import eu.reservoir.monitoring.core.Measurement;
import eu.reservoir.monitoring.core.ProbeValue;
import eu.reservoir.monitoring.core.table.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import demo_usr.energy.energymodel.EnergyModelLinear;

import java.io.IOException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.AMQP.BasicProperties;

import demo_usr.dolfin.siemens.*;


public class Dolvim extends GlobalController {

    private AmqpPublisher amqpPublisher;

    private AmqpConnection amqpConnection;

    static final String TOPIC = "vim";
    static final String VM_MANAGER_RK = "key.manager";       
       
    static final String VM_RK = "key.vm";
       
    static final String HOST_RK = "key.host";

    static final String GREEN_RK = "key.host.green";
    

    // Map of router name to energy consumption
    Map<String, Map<String, Object>>routerEnergyData;

    // no of times the runLoopHook is called
    int runLoopCount = 0;
    
    /**
     * Construct a DOLFIN Virtual Infrastructure Manager
     */
    public Dolvim() {
        super();

        routerEnergyData = new  HashMap<String, Map<String, Object>>();
        
    }


    /*
     * Intialisation for the Dolvim
     * Call global controller init() first
     */
    @Override
    public boolean postInitHook() {
        try {

            // properties holds connection details
            String propertiesLocation = "amqpConfig.properties";
            // properties file location
            PropertiesLoader.setPropertiesLocation(propertiesLocation);
            PropertiesLoader propertiesLoader = PropertiesLoader.getInstanceFromFile();



            // AMQP Connection

            Logger.getLogger("log").logln(USR.STDOUT, "Initializing the AMQP connection ... ");						
            Logger.getLogger("log").logln(USR.STDOUT, "Creating AMQP connection ... DONE ");

            amqpConnection = new AmqpConnection();   // creating the AMQP connection

            Logger.getLogger("log").logln(USR.STDOUT, "Creating Amqp client ... ");

            amqpPublisher = new AmqpPublisher(amqpConnection);           // creating a publisher
                
            Logger.getLogger("log").logln(USR.STDOUT, "Creating Amqp client ... DONE ");

                
            /* start done - send START_VM_MANAGER msg 
               and one for each LocalController */

            try {
                // {
                //     message: "event_notification"
                //     timestamp: blah
                //     type: VM_MANAGER
                //     payload: {
                //        ....
                //     }
                // }


                // String eventType
                // String id

                JSONObject ev = new JSONObject();

                ev.put("message", "event_notification");
                ev.put("timestamp", System.currentTimeMillis());                

                JSONObject jsobj = new JSONObject();

                jsobj.put("type", "START_VM_MANAGER");
                jsobj.put("id", getName());

                ev.put("payload", jsobj);
                

                String amqpMessage = ev.toString();

                amqpPublisher.publish(TOPIC, VM_MANAGER_RK, amqpMessage);

                for (LocalControllerInfo lcInfo :  getLocalControllers() ) {
                    ev = new JSONObject();

                    ev.put("message", "event_notification");
                    ev.put("timestamp", System.currentTimeMillis());                

                    jsobj = new JSONObject();

                    jsobj.put("type", "START_HOST");
                    jsobj.put("id", lcInfo.toString());
                
                    ev.put("payload", jsobj);

                    amqpMessage = ev.toString();

                    amqpPublisher.publish(TOPIC, VM_MANAGER_RK, amqpMessage);

                }

                /* send whether host is on a green server  */

                for (LocalControllerInfo lcInfo :  getLocalControllers() ) {
                    ev = new JSONObject();

                    ev.put("message", "event_notification");
                    ev.put("timestamp", System.currentTimeMillis());                

                    jsobj = new JSONObject();

                    jsobj.put("hostId", lcInfo.toString());

                    // we decide if a host is green if the port number is odd
                    if (lcInfo.getPort() % 2 == 1) {
                        jsobj.put("status", "GREEN");
                    } else {
                        jsobj.put("status", "NORMAL");
                    }   

                
                    ev.put("payload", jsobj);

                    amqpMessage = ev.toString();

                    amqpPublisher.publish(TOPIC, GREEN_RK, amqpMessage);

                }

                

            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean postLocalControllerStopHook(LocalControllerInfo lcInfo) {
        /* LocalController started - send a msg */

        try {
            JSONObject ev = new JSONObject();

            ev.put("message", "event_notification");
            ev.put("timestamp", System.currentTimeMillis());                

            JSONObject jsobj = new JSONObject();

            jsobj.put("eventType", "STOP_HOST");
            jsobj.put("id", lcInfo.toString());
                
            ev.put("payload", jsobj);
                

            String amqpMessage = ev.toString();

            PropertiesLoader propertiesLoader = PropertiesLoader.getInstance();
            
            amqpPublisher.publish(TOPIC, VM_MANAGER_RK, amqpMessage);

        } catch (Exception ioe) {
            ioe.printStackTrace();
        }

        return true;
    }

    /*
     * Shutdown hook
     */
    public boolean postShutdownHook() {
        try {
            JSONObject ev = new JSONObject();

            ev.put("message", "event_notification");
            ev.put("timestamp", System.currentTimeMillis());                

            JSONObject jsobj = new JSONObject();

            jsobj.put("eventType", "STOP_VM_MANAGER");
            jsobj.put("id", getName());
                
            ev.put("payload", jsobj);

            String amqpMessage = ev.toString();

            PropertiesLoader propertiesLoader = PropertiesLoader.getInstance();
            
            amqpPublisher.publish(TOPIC, VM_MANAGER_RK, amqpMessage);

        } catch (Exception ioe) {
            ioe.printStackTrace();
        }

        // show down stuff
        amqpConnection.closeConnection();		// closing AMQP connection

        return true;
    }


    /**
     * Called after a router is started.
     */
    public boolean routerStartedHook(int id) {
        try {
            BasicRouterInfo br = findRouterInfo(id);

            JSONObject ev = new JSONObject();

            ev.put("message", "event_notification");
            ev.put("timestamp", System.currentTimeMillis());                

            JSONObject jsobj = new JSONObject();

            jsobj.put("eventType", "START_VM");
            jsobj.put("id", id);
            jsobj.put("name", br.getName());

            ev.put("payload", jsobj);

            String amqpMessage = ev.toString();

            PropertiesLoader propertiesLoader = PropertiesLoader.getInstance();
            
            amqpPublisher.publish(TOPIC, VM_MANAGER_RK, amqpMessage);

        } catch (Exception ioe) {
            ioe.printStackTrace();
        }

        return true;
    }

    /**
     * Called after a router is ended.
     */
    public boolean routerEndedHook(int id) {
        try {
            JSONObject ev = new JSONObject();

            ev.put("message", "event_notification");
            ev.put("timestamp", System.currentTimeMillis());                

            JSONObject jsobj = new JSONObject();

            // A hack to make every id divible by 7 to be
            // marked as a crash
            if (id % 7 == 0) {
                jsobj.put("eventType", "STOP_VM_CRASH");
            } else {
                jsobj.put("eventType", "STOP_VM");
            }
            jsobj.put("id", id);

            ev.put("payload", jsobj);

            String amqpMessage = ev.toString();

            PropertiesLoader propertiesLoader = PropertiesLoader.getInstance();
            
            amqpPublisher.publish(TOPIC, VM_MANAGER_RK, amqpMessage);

        } catch (Exception ioe) {
            ioe.printStackTrace();
        }

        return true;
    }

    public boolean runLoopHook() {
        // retrieve current status information

        if (runLoopCount % 10 == 0) {
            // do LocalController info every 10 seconds

            HostInfoReporter hostInfoReporter = (HostInfoReporter) findByMeasurementType("HostInfo");

            for (LocalControllerInfo lci :  getLocalControllers() ) {
            
                String localControllerName=lci.getName() + ":" + lci.getPort();

                try {
                
                    Measurement measurement = hostInfoReporter.getData(localControllerName);
                    Measurement prevM = hostInfoReporter.getPreviousData(localControllerName);

                    JSONObject measurementJsobj = getProcessedHostData(localControllerName, measurement, prevM);
            
                    if (measurementJsobj != null) {
                        JSONObject ev = new JSONObject();

                        ev.put("message", "event_notification");
                        ev.put("timestamp", System.currentTimeMillis());                

                        ev.put("payload", measurementJsobj);

                        String amqpMessage = ev.toString();

                        amqpPublisher.publish(TOPIC, HOST_RK, amqpMessage);
                    }
                    
                } catch (Exception ioe) {
                    ioe.printStackTrace();
                }

            }
        }
        

        if (runLoopCount % 5 == 0) {
            // do Router info every 5 seconds

            ThreadGroupListReporter threadGroupListReporter = (ThreadGroupListReporter) findByMeasurementType("ThreadGroupList");


            for (BasicRouterInfo brInfo : getAllRouterInfo()) {
                int routerID = brInfo.getId();
                String routerName = brInfo.getName();

                // need LocalControllerInfo for this router to work out energy data
                LocalControllerInfo lcInfo = brInfo.getLocalControllerInfo();
            
                try {
                
                    Measurement measurement = threadGroupListReporter.getData(routerName);
                    Measurement prevM = threadGroupListReporter.getPreviousData(routerName);
            
                    JSONObject measurementJsobj = getProcessedRouterData(routerID, routerName, lcInfo, measurement, prevM);
                
                    if (measurementJsobj != null) {

                        JSONObject ev = new JSONObject();

                        ev.put("message", "event_notification");
                        ev.put("timestamp", System.currentTimeMillis());                

                        ev.put("payload", measurementJsobj);

                        String amqpMessage = ev.toString();

                        amqpPublisher.publish(TOPIC, VM_RK, amqpMessage);
                    }
                    
                } catch (Exception ioe) {
                    ioe.printStackTrace();
                }


            }
        }

        
        runLoopCount++;
        
        return true;
    }
 

    // this method returns a JSONObject with the difference in inbound/outbound traffic between the latest two probes
    // String hostId;
    // float cpuUser;  // cpu percentage consumed by the user's app
    // float cpuSys; // cpu percentage consumed by the operating system
    // float cpuIdle; // cpu percentage consumed in idle mode
    // int usedMemory;  
    // int freeMemory;  
    // int totalMemory;
    // long inPackets;
    // long inBytes;
    // long outPackets;
    // long outBytes;
    public JSONObject getProcessedHostData (String localControllerName, Measurement m, Measurement prevM) {
        if (m == null) {
            return null;

        } else {
                        
            List<ProbeValue> currentProbeValue = m.getValues();

            List<ProbeValue> previousProbeValue = null;

            if (prevM != null) previousProbeValue = prevM.getValues();

            JSONObject jsobj = new JSONObject();

            try {
                jsobj.put("hostId", localControllerName);

                jsobj.put("cpuIdle", ((Float) currentProbeValue.get(3).getValue()) ); // percentage
                jsobj.put("cpuUser", ((Float) currentProbeValue.get(1).getValue()) );
                jsobj.put("cpuSys", ((Float) currentProbeValue.get(2).getValue()));

                
                jsobj.put("freeMemory", ((Integer)currentProbeValue.get(5).getValue())); // in MBs
                jsobj.put("usedMemory", ((Integer)currentProbeValue.get(4).getValue())); // in MBs

                if (previousProbeValue==null) {
                    // starts with zero bytes
                    jsobj.put("inBytes", 0);
                    jsobj.put("inBytes", 0);
                    jsobj.put("outPackets", 0);
                    jsobj.put("outPackets", 0);
                } else {
                    // subtract from previous probe
                    jsobj.put("inBytes", (Long) currentProbeValue.get(8).getValue() - (Long) previousProbeValue.get(8).getValue());
                    jsobj.put("outBytes", (Long) currentProbeValue.get(10).getValue() - (Long) previousProbeValue.get(10).getValue());
                    jsobj.put("inPackets", (Long) currentProbeValue.get(7).getValue() - (Long) previousProbeValue.get(7).getValue());
                    jsobj.put("outPackets", (Long) currentProbeValue.get(9).getValue() - (Long) previousProbeValue.get(9).getValue());
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return jsobj;
        }
    }


    // Output:
    // String vmId;
    // String vmName;
    // double elapsedTime; // functioning time from the  beginning
    // float cpuUser;
    // float cpuSys;
    // int memory;  // energy used in k
    // double energy;  // energy consumed by the vm from start in kWh
    //
    // Measurements:
    
    public JSONObject getProcessedRouterData(int routerID, String routerName, LocalControllerInfo lcInfo, Measurement m, Measurement prevM) {
        if (m == null) {
            return null;
            
        } else {
            // setup routerEnergyData
            Map <String, Object> prevValues = routerEnergyData.get(routerName);

            if (prevValues == null) {
                // we havent seen this router before
                prevValues = new HashMap<String, Object>();
                prevValues.put("cpuTotal", 0L);
                prevValues.put("cpuUser", 0L);
                prevValues.put("cpuSys", 0L);
                prevValues.put("energy", 0.0D);

                routerEnergyData.put(routerName, prevValues);
            }
            
 
            List<ProbeValue> previousProbeValue = null;

            if (prevM != null) previousProbeValue = prevM.getValues();

            // running totals
            long startTimeR = 0;
            long elapsedTimeR = 0;
            long cpuT = 0;  // in milliseconds
            long userT = 0;  // in milliseconds
            long sysT = 0;  // in milliseconds
            long memT = 0;  // in Kb
        

            List<ProbeValue> values = m.getValues();

            // ProbeValue 0 is the router name
            //ProbeValue pv0 = values.get(0);
            //String routerName = (String)pv0.getValue();

            // ProbeValue 1 is the Table
            ProbeValue pv1 = values.get(1);
            Table table = (Table)pv1.getValue();

            // visit all rows
            List<TableRow> rows = table.toList();

            // result object
            JSONObject jsobj = new JSONObject();

            // create result
            try {
                // add up values for all thread groups
                for (TableRow row : rows) {
                    String name = (String)row.get(0).getValue();
                    Long time = (Long)row.get(1).getValue();
                    Long elapsed = (Long)row.get(2).getValue();
                    Long cpu = (Long)row.get(3).getValue();
                    Long user = (Long)row.get(4).getValue();
                    Long sys = (Long)row.get(5).getValue();
                    Long mem = (Long)row.get(6).getValue();


                    // add up
                    cpuT += (cpu / 1000);
                    userT += (user / 1000);
                    sysT += (sys / 1000);
                    memT += (mem / 1000);


                    // the first time through we set the startTime and the elapsedTime
                    // the first row has the first ThreadGroup of a router
                    // this data is what we need
                    if (startTimeR == 0 && elapsedTimeR == 0) {
                        startTimeR = time;
                        elapsedTimeR = elapsed;
                    }
                }


                // we now have totals
                // so work out diff since last time
               
                Long prevCpuT =  (Long)prevValues.get("cpuTotal");
                Long prevUserT =  (Long)prevValues.get("cpuUser");
                Long prevSysT =  (Long)prevValues.get("cpuSys");
                Double prevEnergy =  (Double)prevValues.get("energy");


                // key.vm	{"vmName":"Router-50", "starttime":"[2015/07/30 10:59:48.192]", "elapsedTime":40282, "cpuSys":59606, "cpuTotal":728630, "cpuUser":669024,"memory":25184}
                jsobj.put("vmId", routerID);
                jsobj.put("vmName", routerName);
                jsobj.put("starttime", startTimeR);
                jsobj.put("elapsedTime", elapsedTimeR);
                jsobj.put("cpuTotal",  (cpuT - prevCpuT)/1000f);
                jsobj.put("cpuUser",  (userT - prevUserT)/1000f);
                jsobj.put("cpuSys", (sysT - prevSysT)/1000f);
                jsobj.put("memory", memT);


                String localControllerName=lcInfo.getName() + ":" + lcInfo.getPort();
                jsobj.put("hostId", localControllerName);
                

                // now work out energy values
                double baseLineEnergyConsumption = lcInfo.GetBaseLineEnergyConsumption();
                double cpuIdleCoefficient = lcInfo.GetCPULoadCoefficient();
                double cpuLoadCoefficient = lcInfo.GetCPULoadCoefficient();
                double freeMemoryCoefficient = lcInfo.GetFreeMemoryCoefficient();
                double memoryAllocationCoefficient = lcInfo.GetMemoryAllocationCoefficient();
                double networkIncomingBytesCoefficient = lcInfo.GetNetworkIncomingBytesCoefficient();
                double networkOutboundBytesCoefficient = lcInfo.GetNetworkOutboundBytesCoefficient();
             
                EnergyModelLinear energyModel = new EnergyModelLinear (cpuLoadCoefficient, cpuIdleCoefficient, memoryAllocationCoefficient, freeMemoryCoefficient, networkOutboundBytesCoefficient, networkIncomingBytesCoefficient,  baseLineEnergyConsumption);

                // Calculate energy consumption
                double routerEnergyConsumption = energyModel.ProcessingConsumptionFunction((cpuT + userT + sysT)/1000000f, 0) + energyModel.MemoryConsumptionFunction (memT/1000f, 0);

                jsobj.put("energyTotal", routerEnergyConsumption);
                jsobj.put("energy", routerEnergyConsumption - prevEnergy);
                

                // save data for next time
                prevValues.put("cpuTotal", cpuT);
                prevValues.put("cpuUser", userT);
                prevValues.put("cpuSys", sysT);
                prevValues.put("energy", routerEnergyConsumption);


            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return jsobj;


        }

    }
    
    
    /**
     * Create the String to print out before a message
     */
    String leadin() {
        final String VN = "DOL VIM: ";

        return getName() + " " + VN;
    }


    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Command line must specify XML file to read.");
            System.exit(-1);
        }

        try {
            Dolvim vControl = new Dolvim();

            if (args.length > 1) {
                vControl.setStartupFile(args[1]);
                vControl.init();
            } else {
                vControl.setStartupFile(args[0]);
                vControl.init();
            }

            vControl.start();


            System.out.println("Vim complete");

        } catch (Throwable t) {
            System.exit(1);
        }

    }

}
