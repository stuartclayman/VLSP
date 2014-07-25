package usr.globalcontroller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import usr.common.ANSI;
import usr.localcontroller.LocalControllerInfo;
import usr.logging.Logger;
import usr.logging.USR;
import eu.reservoir.monitoring.core.Measurement;
import eu.reservoir.monitoring.core.ProbeValue;

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

	// keep track on last inbound and outbound traffic
	long lastNetworkOutboundBytes;
	long lastNetworkIncomingBytes;

	// variables to calculate difference in communicated bytes
	long differenceInOutBoundBytes;
	long differenceInIncomingBytes;

	/**
	 * Constructor
	 */
	public EnergyEfficientPlacement(GlobalController gc) {
		this.gc = gc;

		// initialize last incoming and outgoing bytes
		lastNetworkOutboundBytes=0;
		lastNetworkIncomingBytes=0;

		// initialize variables to calculate difference in communicated bytes
		differenceInOutBoundBytes=0;
		differenceInIncomingBytes=0;
		
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

		Measurement currentMeasurement=null;
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
			currentMeasurement = hostInfoReporter.getData(localControllerName); 

			System.out.println ("Fetching HostInfo Probe:"+currentMeasurement);
			if (currentMeasurement!=null) {
				List<ProbeValue> values = currentMeasurement.getValues();
				// extracted required measurements for the energy model
				currentCPUUserAndSystem = ((Float)values.get(1).getValue() + (Float)values.get(2).getValue()) / 100; // percentage
				currentCPUIdle = ((Float) values.get(3).getValue()) / 100; // percentage
				currentMemoryUsed = (Integer) values.get(4).getValue() / 1024; // in GBs
				currentFreeMemory = (Integer) values.get(5).getValue() / 1024; // in GBs
				currentOutputBytes = (Long) values.get(10).getValue();
				currentInputBytes = (Long) values.get(8).getValue();
				// subtract last incoming & outgoing bytes
				if (lastNetworkOutboundBytes==0) 
					differenceInOutBoundBytes=0;
				else
					differenceInOutBoundBytes = currentOutputBytes - lastNetworkOutboundBytes;
				lastNetworkOutboundBytes = currentOutputBytes;

				if (lastNetworkIncomingBytes==0) 
					differenceInIncomingBytes=0;
				else
					differenceInIncomingBytes = currentInputBytes - lastNetworkIncomingBytes;
				lastNetworkIncomingBytes = currentInputBytes;

				// calculate current energy consumption of particular physical server 
				currentEnergyVolume = localInfo.GetCurrentEnergyConsumption(currentCPUUserAndSystem, currentCPUIdle, currentMemoryUsed, currentFreeMemory, differenceInOutBoundBytes, differenceInIncomingBytes);

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
		long lowestVolume = Long.MAX_VALUE;

		for (Map.Entry<LocalControllerInfo, Long> entry : lcEnergyVolumes.entrySet()) {
			LocalControllerInfo localInfo = entry.getKey();
			Long currentVolume = entry.getValue();

			if (currentVolume < lowestVolume) {
				lowestVolume = currentVolume;
				leastUsed = entry.getKey();
			}
		}


		// log current values
		Logger.getLogger("log").logln(1<<10, toTable(elapsedTime, lcEnergyVolumes));

		Logger.getLogger("log").logln(USR.STDOUT, "EnergyEfficientPlacement: choose " + leastUsed + " volume " + lowestVolume+" Watts - " + currentCPUUserAndSystem + " " + currentCPUIdle + " " + currentMemoryUsed + " " + currentFreeMemory + " " + differenceInOutBoundBytes + " " + differenceInIncomingBytes);

		Logger.getLogger("log").logln(1<<10, gc.elapsedToString(elapsedTime) + ANSI.CYAN +  " EnergyEfficientPlacement: choose " + leastUsed + " lowestVolume: " + lowestVolume + " for " + name + "/" + address + ANSI.RESET_COLOUR);

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
