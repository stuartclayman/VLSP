package demo_usr.energy.energymodel;

public class EnergyModel {

	// hardware related coefficient for energy consumption of cpu (user+system mode)
	private double cpuLoadCoefficient;

	// hardware related coefficient for energy consumption of cpu (idle mode)
	private double cpuIdleCoefficient;

	// hardware related coefficient for energy consumption of used memory
	private double memoryAllocationCoefficient;

	// hardware related coefficient for energy consumption of unused memory
	private double freeMemoryCoefficient;

	// hardware related coefficient for energy consumption of network (outbound traffic) - per byte
	private double networkOutboundBytesCoefficient;

	// hardware related coefficient for energy consumption of network (incoming traffic) - per byte
	private double networkIncomingBytesCoefficient;

	// average energy consumption of all server devices, besides newtwork, cpu and memory
	private double baseLineEnergyConsumption;

	// keeps track of cumulative energy consumption of particular server
	private double cumulativeEnergyConsumption;

	// calculate total time (for cumulative energy calculation) - in sampling periods
	private int totalTime;

	// keep track of last cpu loads
	float lastAverageCPULoad;
	float lastAverageIdleCPU;
	
	// keep track of last memory usage
	int lastMemoryUsed;
	int lastFreeMemory;
	
	// keep track of last energy measurement
	double lastEnergyMeasurement; 
		
	// EnergyModel constructor function
	public EnergyModel (double cpuLoadCoefficient_, double cpuIdleCoefficient_, double memoryAllocationCoefficient_, double freeMemoryCoefficient_, double networkOutboundBytesCoefficient_, double networkIncomingBytesCoefficient_, double baseLineEnergyConsumption_) {
		// initialise hardware related coefficients
		cpuLoadCoefficient = cpuLoadCoefficient_;
		cpuIdleCoefficient = cpuIdleCoefficient_;

		memoryAllocationCoefficient = memoryAllocationCoefficient_;
		freeMemoryCoefficient = freeMemoryCoefficient_;
		networkOutboundBytesCoefficient = networkOutboundBytesCoefficient_;
		networkIncomingBytesCoefficient = networkIncomingBytesCoefficient_;

		// initialise baseline energy consumption 
		baseLineEnergyConsumption = baseLineEnergyConsumption_;

		// start keeping track of cumulative energy consumption
		cumulativeEnergyConsumption=0.0;

		// initialise total time
		totalTime = 0;
		
		// initialise last energy measurement
		lastEnergyMeasurement = 0;
	}

	// Calculate EnergyConsumption for that particular timeframe - the HostInfo probing period (assuming resource usage is relatively stable)
	// Assuming execution of this function per fixed sampling period
	public double CurrentEnergyConsumption (float averageCPULoad, float averageIdleCPU, int memoryUsed, int freeMemory, long networkOutboundBytes, long networkIncomingBytes) {
		double currentEnergy = baseLineEnergyConsumption +  ProcessingConsumptionFunction (averageCPULoad, averageIdleCPU) + MemoryConsumptionFunction (memoryUsed, freeMemory) + NetworkLoadConsumptionFunction (networkOutboundBytes, networkIncomingBytes);

		System.out.println ("Energy Consumption: baseline " + baseLineEnergyConsumption+" processing " + ProcessingConsumptionFunction (averageCPULoad, averageIdleCPU)+ " memory " + MemoryConsumptionFunction (memoryUsed, freeMemory) + " network " + NetworkLoadConsumptionFunction (networkOutboundBytes, networkIncomingBytes));
		
		// calculate totalTime (in sampling periods)
		totalTime += 1;

		// calculate cumulativeEnergyConsumption
		cumulativeEnergyConsumption += currentEnergy;

		// keep track of last values
		lastAverageCPULoad = averageCPULoad;
		lastAverageIdleCPU = averageIdleCPU;
		lastMemoryUsed = memoryUsed;
		lastFreeMemory = freeMemory;
		
		// keep track of last energy measurement
		lastEnergyMeasurement = currentEnergy;
		
		return currentEnergy;
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
	private double ProcessingConsumptionFunction (double averageCPULoad, double averageCPUIdle) {
		// start by having a linear approach
		return cpuLoadCoefficient * averageCPULoad + cpuIdleCoefficient * averageCPUIdle;
	}

	// function that estimates energy consumption based on the memory utilisation.
	// returns a value in Watts
	private double MemoryConsumptionFunction (int memoryUsed, int freeMemory) {
		// start by having a linear approach
		return memoryAllocationCoefficient * memoryUsed + freeMemoryCoefficient * freeMemory;
	}

	// function that estimates energy consumption based on the network load - could be extended to consider multiple running states
	// of network card, etc.
	// returns a value in Watts - currently assuming two running states: send and receive
	private double NetworkLoadConsumptionFunction (long networkOutboundBytes, long networkIncomingBytes) {
		
		// start by having a linear approach
		return networkOutboundBytesCoefficient * networkOutboundBytes + networkIncomingBytesCoefficient * networkIncomingBytes;
	}
}
