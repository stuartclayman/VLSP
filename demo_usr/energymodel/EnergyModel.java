package demo_usr.energymodel;

public class EnergyModel {

	// hardware related coefficient for energy consumption of cpu
	private double cpuLoadC;
	
	// hardware related coefficient for energy consumption of memory
	private double memoryAllocationC;
	
	// hardware related coefficient for energy consumption of network
	private double networkLoadC;
	
	// total memory of particular physical server
	long totalMemory;
	
	//maximum network load of particular physical server;
	double maximumNetworkLoad;
	
	// average energy consumption of all server devices, besides newtwork, cpu and memory
	private double baseLineEnergyConsumption;
	
	// keeps track of cumulative energy consumption of particular server
	private double cumulativeEnergyConsumption;
	
	// calculate total time (for cumulative energy calculation) - in sampling periods
	private int totalTime;
	
	// EnergyModel constructor function
	public EnergyModel (double cpuLoadC_, double memoryAllocationC_, double networkLoadC_, double baseLineEnergyConsumption_, long totalMemory_, double maximumNetworkLoad_) {
		// initialise hardware related coefficients
		cpuLoadC = cpuLoadC_;
		memoryAllocationC = memoryAllocationC_;
		networkLoadC = networkLoadC_;
		
		// initialise baseline energy consumption 
		baseLineEnergyConsumption = baseLineEnergyConsumption_;
		
		// start keeping track of cumulative energy consumption
		cumulativeEnergyConsumption=0.0;
		
		// initialise total memory and maximum network load variables
		totalMemory = totalMemory_;
		maximumNetworkLoad = maximumNetworkLoad_;
		
		// initialise total time
		totalTime = 0;
	}
	
	// Calculate EnergyConsumption for that particular timeframe (assuming resource usage is relatively stable)
	// Assuming execution of this function per fixed sampling period
	public double CurrentEnergyConsumption (double averageCPULoad, long freeMemory, double networkLoad) {
		double currentEnergy = baseLineEnergyConsumption +  ProcessingConsumptionFunction (averageCPULoad) + MemoryConsumptionFunction (freeMemory) + NetworkLoadConsumptionFunction (networkLoad);
		
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
	// returns a value in Watts
	private double ProcessingConsumptionFunction (double averageCPULoad) {
		// start by having a linear approach
		return cpuLoadC * averageCPULoad;
	}

	// function that estimates energy consumption based on the memory utilisation.
	// returns a value in Watts
	private double MemoryConsumptionFunction (long freeMemory) {
		// start by having a linear approach
		return memoryAllocationC * (freeMemory / totalMemory);
	}
	
	// function that estimates energy consumption based on the network load - could be extended to consider multiple running states
	// of network card, etc.
	// returns a value in Watts
	private double NetworkLoadConsumptionFunction (double networkLoad) {
		// start by having a linear approach
		return networkLoadC * (networkLoad / maximumNetworkLoad);
	}
}
