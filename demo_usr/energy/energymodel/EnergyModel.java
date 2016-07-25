package demo_usr.energy.energymodel;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

public class EnergyModel {

	// Energy related coefficients for cpu 
	private double a1;
	private double b1;
	private double c1;
	private double r1;

	// Energy related coefficients for memory
	private double a2;
	private double b2;
	private double c2;
	private double r2;

	// Energy related coefficients for network
	private double a3;
	private double b3;
	private double c3;
	private double r3;

	// Energy related coefficients for HD - TBD
	private double a4;
	private double b4;
	private double c4;
	private double r4;

	// Energy coefficient for energy consumption of all server devices, besides network, cpu and memory
	private double c;

	// keep maximum bytes that can be transmitted via the particular physical host (i.e. for normalizing the network utilization measurements)
	long maxNetworkTransmissionBytes;

	// keeps track of cumulative energy consumption of particular server
	private double cumulativeEnergyConsumption;

	// calculate total time (for cumulative energy calculation) - in sampling periods
	private int totalTime;

	// keep track of last cpu loads
	float lastAverageCPULoad;
	float lastAverageIdleCPU;
	float lastLoadAverage;

	// keep track of last memory usage
	float lastMemoryUsed;
	float lastFreeMemory;

	// keep track of last data communicated
	long lastNetworkOutboundBytes;
	long lastNetworkIncomingBytes;

	// keep track of last energy measurement
	double lastEnergyMeasurement; 
	
	// keep track of last normalized values
	float lastNormalizedCPU;
	float lastNormalizedMemory;
	float lastNormalizedNetwork;

	// EnergyModel constructor function
	public EnergyModel (double a1_, double b1_, double c1_, double r1_, double a2_, double b2_, double c2_,double r2_, double a3_, double b3_, double c3_, double r3_, double a4_, double b4_, double c4_, double r4_, double c_, long maxNetworkTransmissionBytes_) {
		// initialise hardware related coefficients
		a1 = a1_;
		b1 = b1_;
		c1 = c1_;
		r1 = r1_;
		
		a2 = a2_;
		b2 = b2_;
		c2 = c2_;
		r2 = r2_;
		
		a3 = a3_;
		b3 = b3_;
		c3 = c3_;
		r3 = r3_;
		
		a4 = a4_;
		b4 = b4_;
		c4 = c4_;
		r4 = r4_;

		// initialise baseline energy consumption 
		c = c_;
		
		maxNetworkTransmissionBytes = maxNetworkTransmissionBytes_;

		InitModel ();
	}

	private void InitModel () {

		// start keeping track of cumulative energy consumption
		cumulativeEnergyConsumption=0.0;

		// initialise total time
		totalTime = 0;

		// initialise last energy measurement
		lastEnergyMeasurement = 0;
		
		// initialize last normalized values
		lastNormalizedCPU = 0;
		lastNormalizedMemory = 0;
		lastNormalizedNetwork = 0;
	}

	public EnergyModel (JSONObject jsonObj) throws JSONException {
		//json string example
		//{"IP":"localhost/127.0.0.1",
		//   "energyFactors":
		//	{"baseLineEnergyConsumption":300,
		//	 "cpuIdleCoefficient":20,
		//	 "cpuLoadCoefficient":50,
		//	 "freeMemoryCoefficient":2,
		//	 "memoryAllocationCoefficient":4,
		//	 "networkIncomingBytesCoefficient":5.0E-4,
		//	 "networkOutboundBytesCoefficient":0.001},
		//    "hostinfo":
		//    {"cpuIdle":0.8119000196456909,
		//     "cpuLoad":0.18790000677108765,
		//     "freeMemory":1.8964844,
		//     "name":"localhost:10000",
		//     "networkIncomingBytes":0,
		//     "networkOutboundBytes":0,
		//     "usedMemory":1.7314453},
		//    "maxRouters":30,
		//    "name":"localhost:10000",
		//    "noRouters":0,
		//    "port":10000,
		//    "usage":0}

		a1 = jsonObj.getDouble("a1");
		b1 = jsonObj.getDouble("b1");
		c1 = jsonObj.getDouble("c1");
		r1 = jsonObj.getDouble("r1");
		
		a2 = jsonObj.getDouble("a2");
		b2 = jsonObj.getDouble("b2");
		c2 = jsonObj.getDouble("c2");
		r2 = jsonObj.getDouble("r2");
		
		a3 = jsonObj.getDouble("a3");
		b3 = jsonObj.getDouble("b3");
		c3 = jsonObj.getDouble("c3");
		r3 = jsonObj.getDouble("r3");
		
		c = jsonObj.getDouble("c");
		
		maxNetworkTransmissionBytes = jsonObj.getLong("maxNetworkTransmissionBytes");

		InitModel ();
	}

	
	// Calculate EnergyConsumption for that particular timeframe - the HostInfo probing period (assuming resource usage is relatively stable)
	// Assuming execution of this function per fixed sampling period
	public double CurrentEnergyConsumption (float averageCPULoad, float averageIdleCPU, float memoryUsed, float freeMemory, long networkOutboundBytes, long networkIncomingBytes, float loadAverage, double extraCPU, double extraMemory) {
		
		// should normalize measurements first
		float normalizedCPU = (loadAverage / 100) + (float) extraCPU;
		if (normalizedCPU>1)
			normalizedCPU=1;

		float normalizedMemory = memoryUsed / (memoryUsed + freeMemory) + (float) extraMemory;
		if (normalizedMemory>1)
			normalizedMemory=1;

		float normalizedNetwork = (networkOutboundBytes + networkIncomingBytes) / maxNetworkTransmissionBytes;

		double currentEnergy = c +  ProcessingConsumptionFunction (normalizedCPU) + MemoryConsumptionFunction (normalizedMemory) + NetworkLoadConsumptionFunction (normalizedNetwork);

		System.out.println ("Energy Consumption: CPU load was:" + (loadAverage / 100) +", it is now "+normalizedCPU);
		System.out.println ("Energy Consumption: Memory was:"+memoryUsed / (memoryUsed + freeMemory)+", it is now "+normalizedMemory);
		System.out.println ("Energy Consumption: baseline " + c+" processing " + ProcessingConsumptionFunction (normalizedCPU)+ " memory " + MemoryConsumptionFunction (normalizedMemory) + " network " + NetworkLoadConsumptionFunction (normalizedNetwork));

		// calculate totalTime (in sampling periods)
		totalTime += 1;

		// calculate cumulativeEnergyConsumption
		cumulativeEnergyConsumption += currentEnergy;

		// keep track of last values
		lastAverageCPULoad = averageCPULoad;
		lastAverageIdleCPU = averageIdleCPU;
		lastMemoryUsed = memoryUsed;
		lastFreeMemory = freeMemory;
        	lastNetworkOutboundBytes = networkOutboundBytes;
        	lastNetworkIncomingBytes = networkIncomingBytes;
        	lastLoadAverage = loadAverage;
        
		lastNormalizedCPU = normalizedCPU;
		lastNormalizedMemory = normalizedMemory;
		lastNormalizedNetwork = normalizedNetwork;

		// keep track of last energy measurement
		lastEnergyMeasurement = currentEnergy;

		return currentEnergy;
	}
	
	public double CurrentEnergyConsumption (float averageCPULoad, float averageIdleCPU, float memoryUsed, float freeMemory, long networkOutboundBytes, long networkIncomingBytes, float loadAverage) {
		return CurrentEnergyConsumption (averageCPULoad, averageIdleCPU, memoryUsed, freeMemory, networkOutboundBytes, networkIncomingBytes, loadAverage, 0, 0);
	}
	
	// return latest measurements
	public double getLastAverageCPULoad () {
		return lastAverageCPULoad;
	}

	public double getLastLoadAverage () {
		return lastLoadAverage;
	}

	public double getLastAverageIdleCPU () {
		return lastAverageIdleCPU;
	}

	public double getLastMemoryUsed () {
		return lastMemoryUsed;
	}

	public double getLastFreeMemory () {
		return lastFreeMemory;
	}

	public double getLastNetworkOutboundBytes () {
		return lastNetworkOutboundBytes;
	}

	public double getLastNetworkIncomingBytes () {
		return lastNetworkIncomingBytes;
	}
	
	public double getLastNormalizedCPULoad () {
		return lastNormalizedCPU;
	}

	public double getLastNormalizedMemoryAllocation () {
		return lastNormalizedMemory;
	}
	
	public double getLastNormalizedNetwork () {
		return lastNormalizedNetwork;
	}
	
	// calculate current energy consumption from relevant jsonobject
	public double CurrentEnergyConsumption (JSONObject jsonObj) throws JSONException {
		// initialize variables
		float averageCPULoad=0;
		float averageIdleCPU=0; 
		float memoryUsed=0;
		float freeMemory=0;
		long networkOutboundBytes=0; 
		long networkIncomingBytes=0;
		float loadAverage=0;

		averageCPULoad=(float) jsonObj.getDouble("cpuLoad");
		averageIdleCPU=(float) jsonObj.getDouble("cpuIdle"); 
		memoryUsed=(float) jsonObj.getDouble("usedMemory");
		freeMemory=(float) jsonObj.getDouble("freeMemory");
		networkOutboundBytes=jsonObj.getLong("networkOutboundBytes");
		networkIncomingBytes=jsonObj.getLong("networkIncomingBytes");
		loadAverage=(float) jsonObj.getDouble("loadAverage");

		return CurrentEnergyConsumption(averageCPULoad, averageIdleCPU, memoryUsed, freeMemory, networkOutboundBytes, networkIncomingBytes, loadAverage);
	}

	// returns cumulative energy consumption of particular physical machine
	public double GetCumulativeEnergyConsumption () {
		return cumulativeEnergyConsumption;
	}

	// returns average energy consumption of particular physical machine
	public double GetAverageEnergyConsumption () {
		return cumulativeEnergyConsumption / totalTime;
	}

	// function that estimates energy consumption based on the averageCPULoad - could be extended to consider more than one cpu,
	// multiple cpu running states etc.
	// returns a value in Watts (at this point we assume to running states, working and idle)
	public double ProcessingConsumptionFunction (double normalizedCPU) {
		// non-linear approach, linear for r1=0
		double val = a1 * normalizedCPU + b1 * Math.pow(normalizedCPU, r1) + c1;
		
		if (val < 0) {
			return 0;
		} else {
			return val;
		}
	}

	// function that estimates energy consumption based on the memory utilisation.
	// returns a value in Watts
	public double MemoryConsumptionFunction (float normalizedMemory) {
		// non-linear approach, linear for r1=0
		double val = a2 * normalizedMemory + b2 * Math.pow(normalizedMemory, r2) + c2;
		
		if (val < 0) {
			return 0;
		} else {
			return val;
		}
	}

	// function that estimates energy consumption based on the network load - could be extended to consider multiple running states
	// of network card, etc.
	// returns a value in Watts - currently assuming two running states: send and receive
	public double NetworkLoadConsumptionFunction (float normalizedNetwork) {

		// non-linear approach, linear for r1=0
		double val = a3 * normalizedNetwork + b3 * Math.pow(normalizedNetwork, r3) + c3;

		if (val < 0) {
			return 0;
		} else {
			return val;
		}
	}
}
