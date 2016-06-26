package usr.globalcontroller;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.common.ANSI;
import usr.localcontroller.LocalControllerInfo;
import usr.logging.BitMask;
import usr.logging.Logger;
import usr.logging.USR;

/**
 * The EnergyEfficientPlacement is responsible for determining the placement
 * of a Router across the active resources.
 * <p>
 * It finds the LocalController where the energy consumption is a minimum.
 */
public class EnergyEfficientPlacement implements PlacementEngine {
    // The GlobalController
    GlobalController gc;

    // keep track on current values
    private float currentCPUUserAndSystem=0;
    private float currentCPUIdle=0;
    private int currentMemoryUsed=0;
    private int currentFreeMemory=0;
    private long currentOutputBytes=0;
    private long currentInputBytes=0;
	private float currentLoadAverage=0;

    /**
     * Constructor
     */
    public EnergyEfficientPlacement(GlobalController gc) {
        this.gc = gc;

        // get logger
        try {
            Logger.getLogger("log").addOutput(new PrintWriter(new FileOutputStream("/tmp/gc-channel12.out")), new BitMask(1<<12));
        } catch (FileNotFoundException fnfe) {
            Logger.getLogger("log").logln(USR.ERROR, fnfe.toString());
        }

        Logger.getLogger("log").logln(USR.STDOUT, "EnergyEfficientPlacement: localcontrollers = " + getPlacementDestinations());
    }

    /**
     * Get the relevant LocalControllerInfo for a placement of a router with 
     * a specified name and address.
     */
    public LocalControllerInfo routerPlacement(String name, String address) {
        LocalControllerInfo leastUsed = null;

        long elapsedTime = gc.getElapsedTime();

        // A map of LocalControllerInfo to the energy consumption
        HashMap<LocalControllerInfo, Long>lcEnergyVolumes = new HashMap<LocalControllerInfo, Long>();

        HostInfoReporter hostInfoReporter = (HostInfoReporter) gc.findByMeasurementType("HostInfo");

        JSONObject currentMeasurement=null;
        /**
         * Each measurement has the following structure:
         * ProbeValues
         * 0: Name: STRING: name
         * 1: cpu-user: FLOAT: percent
         * 2: cpu-sys: FLOAT: percent
         * 3: cpu-idle: FLOAT: percent
         * 4: mem-used: INTEGER: Mb
         * 5: mem-free: INTEGER: Mb
         * 6: mem-total: INTEGER: Mb
         * 7: in-packets: LONG: n
         * 8: in-bytes: LONG: n
         * 9: out-packets: LONG: n
         * 10: out-bytes: LONG: n
         * 
         * HostInfo attributes: [0: STRING LocalController:10000, 1: FLOAT 7.72, 2: FLOAT 14.7, 3: FLOAT 77.57, 4: INTEGER 15964, 5: INTEGER 412, 6: INTEGER 16376, 7: LONG 50728177, 8: LONG 43021697138, 9: LONG 40879848, 10: LONG 7519963728]
         */

        Double currentEnergyVolume=0.0;

        String localControllerName="";

        // iterate through all potential placement destinations and calculate energy consumption
        for (LocalControllerInfo localInfo : getPlacementDestinations()) {
            // get measurement from hostInfoReporter for particular localcontroller
            //System.out.println (localInfo.getName());

            localControllerName = localInfo.getName() + ":" + localInfo.getPort();
            //currentMeasurement = hostInfoReporter.getData(localControllerName); 
            currentMeasurement = hostInfoReporter.getProcessedData(localControllerName); 

            //System.out.println ("From localcontroller name:"+localControllerName);
            System.out.println ("Fetching HostInfo Probe:"+currentMeasurement+" from "+localControllerName);
		
<<<<<<< .mine
			if (currentMeasurement!=null) {
				// extracted required measurements for the energy model
				try {
					currentCPUUserAndSystem = (float) currentMeasurement.getDouble("cpuLoad");
					currentCPUIdle = (float) currentMeasurement.getDouble("cpuIdle");
					currentMemoryUsed = currentMeasurement.getInt("usedMemory");
					currentFreeMemory = currentMeasurement.getInt("freeMemory");
					currentOutputBytes = currentMeasurement.getLong("networkOutboundBytes");
					currentInputBytes = currentMeasurement.getLong("networkIncomingBytes");
					currentLoadAverage = (float) currentMeasurement.getDouble("loadAverage");
					//Logger.getLogger("log").logln(USR.STDOUT, "EnergyEfficientPlacement: log-values " + localInfo.getName() + ":" + currentCPUUserAndSystem + " " + currentCPUIdle + " " + currentMemoryUsed + " " + currentFreeMemory + " " + currentOutputBytes + " " + currentInputBytes);

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
=======
            if (currentMeasurement!=null) {
                // extracted required measurements for the energy model
                try {
                    currentCPUUserAndSystem = (float) currentMeasurement.getDouble("cpuLoad");
                    currentCPUIdle = (float) currentMeasurement.getDouble("cpuIdle");
                    currentMemoryUsed = currentMeasurement.getInt("usedMemory");
                    currentFreeMemory = currentMeasurement.getInt("freeMemory");
                    currentOutputBytes = currentMeasurement.getLong("networkOutboundBytes");
                    currentInputBytes = currentMeasurement.getLong("networkIncomingBytes");
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
>>>>>>> .r1333
				
<<<<<<< .mine
				// calculate current energy consumption of particular physical server 
				currentEnergyVolume = localInfo.GetCurrentEnergyConsumption(currentCPUUserAndSystem, currentCPUIdle, currentMemoryUsed, currentFreeMemory, currentOutputBytes, currentInputBytes, currentLoadAverage);
=======
                // calculate current energy consumption of particular physical server 
                currentEnergyVolume = localInfo.GetCurrentEnergyConsumption(currentCPUUserAndSystem, currentCPUIdle, currentMemoryUsed, currentFreeMemory, currentOutputBytes, currentInputBytes);
>>>>>>> .r1333

                // convert double to long
                lcEnergyVolumes.put(localInfo, currentEnergyVolume.longValue());
                System.out.println ("Current Energy Volume:"+currentEnergyVolume.longValue());
            } else {
                lcEnergyVolumes.put(localInfo, 0L);
            }
        }

        // at this point we know which host has what volume.
        // now we need to skip through all of them and find the host
        // with the lowest volume
        // this is done by subtracting the oldvolume from the latest volume
        long lowestEnergyVolume = Long.MAX_VALUE;

        for (Map.Entry<LocalControllerInfo, Long> entry : lcEnergyVolumes.entrySet()) {
            LocalControllerInfo localInfo = entry.getKey();
            Long chosenEnergyVolume = entry.getValue();

             Logger.getLogger("log").logln(1<<12, gc.elapsedToString(elapsedTime) + ANSI.YELLOW +  " EnergyEfficientPlacement: LocalControllerInfo " + localInfo + " EnergyVolume: " + chosenEnergyVolume + ANSI.RESET_COLOUR);
            

            if (chosenEnergyVolume < lowestEnergyVolume) {
                lowestEnergyVolume = chosenEnergyVolume;
                leastUsed = entry.getKey();
            }
        }


        // log current values
        Logger.getLogger("log").logln(1<<10, toTable(elapsedTime, lcEnergyVolumes));

        Logger.getLogger("log").logln(USR.STDOUT, "EnergyEfficientPlacement: choose " + leastUsed + " lowestEnergyVolume " + lowestEnergyVolume+" Watts - " + currentCPUUserAndSystem + " " + currentCPUIdle + " " + currentMemoryUsed + " " + currentFreeMemory + " " + currentOutputBytes + " " + currentInputBytes);

        Logger.getLogger("log").logln(1<<12, gc.elapsedToString(elapsedTime) + ANSI.CYAN +  " EnergyEfficientPlacement: choose " + leastUsed + " lowestEnergyVolume: " + lowestEnergyVolume + " for " + name + "/" + address + ANSI.RESET_COLOUR);

        // return the most energy efficient LocalControllerInfo
        return leastUsed;
    }


    /**
     * Get all the possible placement destinations
     */
    public Set<LocalControllerInfo> getPlacementDestinations() {
        return gc.getLocalControllers();
    }


    /**
     * Get info as a String
     */
    private String toTable(long elapsed, HashMap<LocalControllerInfo, Long>lcVolumes) {
        StringBuilder builder = new StringBuilder();


        builder.append(gc.elapsedToString(elapsed) + " ");

        for (Map.Entry<LocalControllerInfo, Long> entry : lcVolumes.entrySet()) {
            LocalControllerInfo localInfo = entry.getKey();
            Long volume = entry.getValue();

            builder.append(localInfo + ": " + localInfo.getNoRouters() + " "  + volume + " | ");
        }

        return builder.toString();
    }

}
