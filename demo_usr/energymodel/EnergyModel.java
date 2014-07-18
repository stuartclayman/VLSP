package demo_usr.energymodel;

public class EnergyModel {

	// hardware related coefficient for energy consumption of cpu (user+system mode)
	private double cpuLoadC;

	// hardware related coefficient for energy consumption of cpu (idle mode)
	private double cpuIdleC;

	// hardware related coefficient for energy consumption of used memory
	private double memoryAllocationC;

	// hardware related coefficient for energy consumption of unused memory
	private double freeMemoryC;

	// hardware related coefficient for energy consumption of network (outbound traffic) - per byte
	private double networkOutboundBytesC;

	// hardware related coefficient for energy consumption of network (incoming traffic) - per byte
	private double networkIncomingBytesC;

	// average energy consumption of all server devices, besides newtwork, cpu and memory
	private double baseLineEnergyConsumption;

	// keeps track of cumulative energy consumption of particular server
	private double cumulativeEnergyConsumption;

	// calculate total time (for cumulative energy calculation) - in sampling periods
	private int totalTime;

	// EnergyModel constructor function
	public EnergyModel (double cpuLoadC_, double cpuIdleC_, double memoryAllocationC_, double freeMemoryC_, double networkOutboundBytesC_, double networkIncomingBytesC_, double baseLineEnergyConsumption_) {
		// initialise hardware related coefficients
		cpuLoadC = cpuLoadC_;
		cpuIdleC = cpuIdleC_;

		memoryAllocationC = memoryAllocationC_;
		freeMemoryC = freeMemoryC_;
		networkOutboundBytesC = networkOutboundBytesC_;
		networkIncomingBytesC = networkIncomingBytesC_;

		// initialise baseline energy consumption 
		baseLineEnergyConsumption = baseLineEnergyConsumption_;

		// start keeping track of cumulative energy consumption
		cumulativeEnergyConsumption=0.0;

		// initialise total time
		totalTime = 0;
	}

	// Calculate EnergyConsumption for that particular timeframe (assuming resource usage is relatively stable)
	// Assuming execution of this function per fixed sampling period
	public double CurrentEnergyConsumption (float averageCPULoad, float averageIdleCPU, int memoryUsed, int freeMemory, long networkOutboundBytes, long networkIncomingBytes) {
		double currentEnergy = baseLineEnergyConsumption +  ProcessingConsumptionFunction (averageCPULoad, averageIdleCPU) + MemoryConsumptionFunction (memoryUsed, freeMemory) + NetworkLoadConsumptionFunction (networkOutboundBytes, networkIncomingBytes);

		// calculate totalTime (in sampling periods)
		totalTime += 1;

		// calculate cumulativeEnergyConsumption
		cumulativeEnergyConsumption += currentEnergy;

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
		return cpuLoadC * averageCPULoad + cpuIdleC * averageCPUIdle;
	}

	// function that estimates energy consumption based on the memory utilisation.
	// returns a value in Watts
	private double MemoryConsumptionFunction (int memoryUsed, int freeMemory) {
		// start by having a linear approach
		return memoryAllocationC * memoryUsed + freeMemoryC * freeMemory;
	}

	// function that estimates energy consumption based on the network load - could be extended to consider multiple running states
	// of network card, etc.
	// returns a value in Watts - currently assuming two running states: send and receive
	private double NetworkLoadConsumptionFunction (long networkOutboundBytes, long networkIncomingBytes) {
		// start by having a linear approach
		return networkOutboundBytesC * networkOutboundBytes + networkIncomingBytesC * networkIncomingBytes;
	}
}
