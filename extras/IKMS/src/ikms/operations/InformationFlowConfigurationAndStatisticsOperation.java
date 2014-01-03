package ikms.operations;

import ikms.data.FlowRegistry;
import ikms.data.IKMSOptimizationGoal;
import ikms.data.InformationExchangePolicies;
import ikms.functions.InformationStorageAndIndexingFunction;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import com.jezhumble.javasysmon.JavaSysMon;

public class InformationFlowConfigurationAndStatisticsOperation {

	// keeps track of all flow registrations
	static FlowRegistry flowRegistry = null;

	// monitored entities
	static ArrayList<Integer> monitoredEntities = new ArrayList<Integer> ();

	// Defining communicating functions
	static InformationStorageAndIndexingFunction informationStorageAndIndexingFunction = null;

	// measurements for all flows
	static private long responseTimeRequestsInPeriod = 0;
	static private long totalResponseTimesInPeriod = 0;
	static private long freshnessRequestsInPeriod = 0;
	static private long totalFreshness = 0;

	// measurements for monitored flows
	static private long monitoredEntitiesResponseTimeRequestsInPeriod = 0;
	static private long totalResponseTimesInPeriodForMonitoredEntities = 0;
	static private long monitoredEntitiesFreshnessRequestsInPeriod = 0;
	static private long totalFreshnessForMonitoredEntities = 0;

	//static private double lastAverageResponseTime = 0;
	//static private double lastAverageFreshness = 0;

	static private int countIKMSCPUProcessingRequests = 0;

	public InformationFlowConfigurationAndStatisticsOperation (InformationStorageAndIndexingFunction informationStorageAndIndexingFunction_) {
		informationStorageAndIndexingFunction = informationStorageAndIndexingFunction_;

		flowRegistry = new FlowRegistry();
	}

	public static void MonitorEntity (int entityid) {
		monitoredEntities.add(entityid);
	}

	private static boolean CheckIfMonitored (int entityid) {
		if (monitoredEntities.contains(entityid))
			return true;
		else 
			return false;
	}

	public static void registerFlow (InformationExchangePolicies policies) {
		// update flow registry
		flowRegistry.UpdateFlowRegistration(policies);
	}

	public IKMSOptimizationGoal NegotiateGoal (IKMSOptimizationGoal currentGlobalGoal, IKMSOptimizationGoal suggestedGoal) {
		return flowRegistry.NegotiateGoal(currentGlobalGoal, suggestedGoal);
	}
	
	public HashMap<ArrayList<String>, HashMap<Integer, Integer>> FindFlowsThatNeedRestablishmentDueToNewGlobalGoal (IKMSOptimizationGoal newGoal) {
		return flowRegistry.FindFlowsThatNeedRestablishmentDueToNewGlobalGoal(newGoal);
	}
	
	// Retrieve relevant flows to entity
	public static ArrayList<InformationExchangePolicies> GetRelevantInformationFlows (int entityId) {
		return flowRegistry.GetRelevantInformationFlows(entityId);
	}

	// Unregister flows relevant to entity
	public static void UnRegisterFlows (int entityid) {
		// get relevant flows

		ArrayList<InformationExchangePolicies> relevantFlows = GetRelevantInformationFlows (entityid);
		for (InformationExchangePolicies currentFlow : relevantFlows) {
			flowRegistry.RemoveFlowRegistration(currentFlow);
		}
	}

	// Retrieves flow registration
	public static InformationExchangePolicies getFlowRegistration (String uri) {
		return flowRegistry.GetInformationFlowExchangePolicies(uri);
	}

	

	/*public static ArrayList<Integer> getAllEntityIDs () {
		Set<Integer> keys = flowRegistry.keySet();

		// reverse list of entity ids (so re-negotiation follows the same order)
		ArrayList<Integer> reverseKeys = new ArrayList<Integer>(keys);
		Collections.reverse(reverseKeys);

		return reverseKeys;
	}*/

	public static int GetStorageMemoryUsed () {
		return informationStorageAndIndexingFunction.GetStorageMemoryUsed();
	}

	/*	public static double GetStorageSystemCPUUsed () {
		return informationStorageAndIndexingFunction.GetSystemCPUUsed();
	}*/

	// Returns number of active flows
	public static double GetNumberOfFlows () {
		return flowRegistry.GetNumberOfFlows();
	}

	// Returns number of active direct flows
	public static double GetNumberOfDirectFlows () {
		return flowRegistry.GetNumberOfDirectFlows();
	}

	// Returns number of active pub/sub flows
	public static double GetNumberOfPubSubFlows () {
		return flowRegistry.GetNumberOfPubSubFlows();
	}

	// Returns number of active pub/sub flows
	public static double GetNumberOfPushPullFlows () {
		return flowRegistry.GetNumberOfPushPullFlows();
	}

	public static double GetAverageResponseTime () {
		double output = 0.0;
		if (responseTimeRequestsInPeriod>0)
			output = totalResponseTimesInPeriod / responseTimeRequestsInPeriod;

		// start a new period
		totalResponseTimesInPeriod = 0;
		responseTimeRequestsInPeriod = 0;

		// if output = 0, return previous calculated response time
		/*if (responseTimeRequestsInPeriod==0) 
			return lastAverageResponseTime;
		else
			lastAverageResponseTime = output;*/

		return output;
	}

	public static double GetAverageFreshness () {
		double output = 0.0;
		if (freshnessRequestsInPeriod>0)
			output = totalFreshness / freshnessRequestsInPeriod;

		// start a new period
		totalFreshness = 0;
		freshnessRequestsInPeriod = 0;

		// if output = 0, return previous calculated response time
		/*if (freshnessRequestsInPeriod==0) 
			return lastAverageFreshness;
		else
			lastAverageFreshness = output;*/

		return output;
	}

	public static double GetAverageResponseTimeForMonitoredEntities () {
		double output = 0.0;
		if (monitoredEntitiesResponseTimeRequestsInPeriod>0)
			output = totalResponseTimesInPeriodForMonitoredEntities / monitoredEntitiesResponseTimeRequestsInPeriod;

		// start a new measurement period
		totalResponseTimesInPeriodForMonitoredEntities = 0;
		monitoredEntitiesResponseTimeRequestsInPeriod = 0;

		return output;
	}

	public static double GetAverageFreshnessForMonitoredEntities () {
		double output = 0.0;
		if (monitoredEntitiesFreshnessRequestsInPeriod>0)
			output = totalFreshnessForMonitoredEntities / monitoredEntitiesFreshnessRequestsInPeriod;

		// start a new measurement period
		totalFreshnessForMonitoredEntities = 0;
		monitoredEntitiesFreshnessRequestsInPeriod = 0;

		return output;
	}

	public static void UpdateResponseTime (int entityid, long responseTime) {
		responseTimeRequestsInPeriod++;
		totalResponseTimesInPeriod+=responseTime;

		if (CheckIfMonitored (entityid)) {
			monitoredEntitiesResponseTimeRequestsInPeriod++;
			totalResponseTimesInPeriodForMonitoredEntities+=responseTime;
		}
	}

	public static void UpdateFreshness (int entityid, long freshness) {
		freshnessRequestsInPeriod++;
		totalFreshness+=freshness;

		if (CheckIfMonitored (entityid)) {
			monitoredEntitiesFreshnessRequestsInPeriod++;
			totalFreshnessForMonitoredEntities+=freshness;
		}

	}

	public static double GetIKMSSystemCPUUsed () {
		JavaSysMon monitor =   new JavaSysMon();
		int currentPID = monitor.currentPid();
		double output = 0.0;

		try {
			output = CPULoadFromPSCommand (currentPID);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println ("process load:"+output);

		return output;

		/*long totalCPUTime = monitor.cpuTimes().getTotalMillis() / monitor.numCpus();
		long processUserCPUTime = monitor.processTree().find(monitor.currentPid()).processInfo().getUserMillis();
		long processKernelCPUTime = monitor.processTree().find(monitor.currentPid()).processInfo().getSystemMillis();
		long processTotalCPUTime = processUserCPUTime + processKernelCPUTime;
		double output = 0.0;

		double CPUTimeDifference = totalCPUTime - lastCPUTime;
		double processCPUTimeDifference = processTotalCPUTime - lastProcessCPUTime;

		if (lastProcessCPUTime!=0) {			
			double processLoad = processCPUTimeDifference * 100 / CPUTimeDifference;

			System.out.println ("pid:"+monitor.currentPid() +" total cpu time:"+ CPUTimeDifference +" total Process CPU:"+processCPUTimeDifference);
			System.out.println ("pid:"+monitor.currentPid() +" load:"+processLoad);	
			output = processLoad;
		}
		lastProcessCPUTime = processTotalCPUTime;
		lastCPUTime = totalCPUTime;

		System.out.println (mbean.getProcessCpuTime());

		return output;*/
	}

	public static double CPULoadFromPSCommand (int currentPID) throws Exception {
		countIKMSCPUProcessingRequests++;
		// do not show first 10 values
		if (countIKMSCPUProcessingRequests>10) {

			Process p = Runtime.getRuntime().exec("/bin/ps -p "+currentPID+" -o %cpu");
			p.waitFor();

			BufferedReader reader = 
					new BufferedReader(new InputStreamReader(
							p.getInputStream()));
			String line = reader.readLine();
			while (line != null) {
				line = reader.readLine();
				if (line!=null)
					return Double.valueOf(line);
			}
		}
		return 0.0;
	}
	
	/*public static ArrayList<Integer> getAllEntityIDs () {
		Set<Integer> keys = flowRegistry.keySet();

		// reverse list of entity ids (so re-negotiation follows the same order)
		ArrayList<Integer> reverseKeys = new ArrayList<Integer>(keys);
		Collections.reverse(reverseKeys);

		return reverseKeys;
	}*/
}
