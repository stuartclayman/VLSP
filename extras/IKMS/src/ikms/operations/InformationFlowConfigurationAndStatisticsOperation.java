package ikms.operations;

import ikms.client.GlobalControllerClient;
import ikms.data.FlowRegistry;
import ikms.data.IKMSOptimizationGoal;
import ikms.data.InformationExchangePolicies;
import ikms.functions.InformationStorageAndIndexingFunction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

import com.jezhumble.javasysmon.JavaSysMon;

import demo_usr.energy.energymodel.EnergyModel;

public class InformationFlowConfigurationAndStatisticsOperation {

	// keeps track of all flow registrations
	static FlowRegistry flowRegistry = new FlowRegistry();

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

	//static private int countIKMSCPUProcessingRequests = 0;

	static private boolean firstMeasurementReceived = false;
	
	// connectivity information for the global controller
	private String gcHost;
	private String gcPort;
	
	// calculated energy values
	double maxEnergyValue;
	double minEnergyValue;
	double averageEnergyValue;
	double totalEnergyValue;
	
	private GlobalControllerClient gcClient;
	
	static private Map <String, EnergyModel> energyConsumptionPerLocalController = new HashMap <String, EnergyModel> ();
	static private ArrayList<String> localControllers = new ArrayList<String>();
	static private ArrayList<Double> energyConsumedPerLocalController = new ArrayList<Double>();

	static private boolean firstTime=true;
	
	public InformationFlowConfigurationAndStatisticsOperation (InformationStorageAndIndexingFunction informationStorageAndIndexingFunction_, String gcHost_, String gcPort_) {
		informationStorageAndIndexingFunction = informationStorageAndIndexingFunction_;

		// connectivity information for the global controller
		gcHost = gcHost_;
		gcPort = gcPort_;
		
		gcClient = new GlobalControllerClient (gcHost, gcPort);
		
		//RetrieveLocalControllerInformation ();
		//RetrieveLocalControllerInformation ();
		//RetrieveLocalControllerInformation ();

		//System.exit(0);
		
		// reset calculated energy values
		maxEnergyValue=0;
		minEnergyValue=0;
		averageEnergyValue=0;
		totalEnergyValue=0;		
	}

	public void RetrieveLocalControllerInformation () {
		try {
			JSONObject result = gcClient.retrieveLocalControllerInfo();
			JSONArray listArray = null;

			if (!localControllers.isEmpty()) {
				// it is not executed for a first time, so the objects do not need to be initialized
				firstTime=false;
			}
			// get local controller information (do that once)
			if (result.getJSONArray("list")!=null&&firstTime) {
				listArray = result.getJSONArray("list");
				final int n = listArray.length();
			    for (int i = 0; i < n; ++i) {
			      String currentDetail = listArray.getString(i);
				  System.out.println ("Adding localcontroller information to IKMS:"+currentDetail);
				  // add all localcontroller names to the arraylist
				  localControllers.add(i, currentDetail);
				}
			}
			
			JSONArray detailArray = null;
			JSONObject coefficientsArray = null;
			double currentEnergy = 0;
			
			// initialize EnergyModel Objects
			if (result.getJSONArray("detail")!=null) {
				detailArray = result.getJSONArray("detail");
				
				final int n = detailArray.length();
			    for (int i = 0; i < n; ++i) {
		    		  JSONObject currentDetail = detailArray.getJSONObject(i);

			      // create a new EnergyModel per localcontroller (do that once)
			    	  if (firstTime) {
			    		  //System.out.println (currentDetail.toString());
			    		  coefficientsArray = currentDetail.getJSONObject("energyFactors");
			    		  energyConsumptionPerLocalController.put(localControllers.get(i), new EnergyModel (coefficientsArray));
			    	  }
				  // update energy values
				  EnergyModel model = energyConsumptionPerLocalController.get(localControllers.get(i));
				  currentEnergy = model.CurrentEnergyConsumption (currentDetail.getJSONObject("hostinfo"));
				  System.out.println ("Current energy consumption is:"+i+", "+model.getLastAverageCPULoad()+", " + model.getLastAverageIdleCPU()+", "+model.getLastMemoryUsed()+", "+model.getLastFreeMemory()+", "+model.getLastNetworkIncomingBytes()+", "+model.getLastNetworkOutboundBytes()+", " + currentEnergy+" "+firstTime);
				  if (!firstTime)
					  energyConsumedPerLocalController.remove(i);
				  energyConsumedPerLocalController.add(i, currentEnergy);
			    }
			}
			
			//System.out.println (result.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//calculate max energy value
		System.out.println ("Maximum energy value is:"+CalculateMaxValueFromArrayList (energyConsumedPerLocalController));
		
		//calculate min energy value
		System.out.println ("Minimum energy value is:"+CalculateMinValueFromArrayList (energyConsumedPerLocalController));

		//calculate average energy value
		System.out.println ("Average energy value is:"+CalculateAverageValueFromArrayList (energyConsumedPerLocalController));

	}
	
	private double CalculateMaxValueFromArrayList (ArrayList arrayList) {
		maxEnergyValue=(double) Collections.max(arrayList);

		return maxEnergyValue;
	}
	
	private double CalculateMinValueFromArrayList (ArrayList<Double> arrayList) {
		minEnergyValue=(double) Collections.min(arrayList);

		return minEnergyValue;
	}
	
	public double CalculateAverageValueFromArrayList (ArrayList<Double> arrayList) {
	    // 'average' is undefined if there are no elements in the list.
	    if (arrayList == null || arrayList.isEmpty())
	        return 0.0;
	    // Calculate the summation of the elements in the list
	    double sum = 0;
	    int n = arrayList.size();
	    // Iterating manually is faster than using an enhanced for loop.
	    for (int i = 0; i < n; i++)
	        sum += arrayList.get(i);
	    // We don't want to perform an integer division, so the cast is mandatory.
		totalEnergyValue=sum;
		
		averageEnergyValue=((double) sum) / n;
		
	    return averageEnergyValue;
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
	
	// Returns the maximum energy consumed in a host
	public double GetMaxEnergyValue () {
		return maxEnergyValue;
	}
	
	// Returns the minimum energy consumed in a host
	public double GetMinEnergyValue () {
		return minEnergyValue;
	}
	
	// Returns the average energy consumed in the hosts
	public double GetAverageEnergyValue () {
		return averageEnergyValue;
	}

	// Returns the total energy consumed in the hosts
	public double GetTotalEnergyValue () {
		return totalEnergyValue;
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

	public static boolean IsFirstMessageReceived () {
		return firstMeasurementReceived;
	}

	public static void ResetFirstMeasurementReceived () {
		firstMeasurementReceived = false;
	}

	public static void UpdateResponseTime (int entityid, long responseTime) {
		// set that a first measurement has been received
		if (!firstMeasurementReceived)
			firstMeasurementReceived = true;

		// for all flows
		//responseTimeRequestsInPeriod++;
		//totalResponseTimesInPeriod+=responseTime;

		if (CheckIfMonitored (entityid)) {
			monitoredEntitiesResponseTimeRequestsInPeriod++;
			totalResponseTimesInPeriodForMonitoredEntities+=responseTime;
		} else {
			// other flows (without the selected)
			responseTimeRequestsInPeriod++;
			totalResponseTimesInPeriod+=responseTime;
		}
	}

	public static void UpdateFreshness (int entityid, long freshness) {
		// for all flows
		//freshnessRequestsInPeriod++;
		//totalFreshness+=freshness;

		if (CheckIfMonitored (entityid)) {
			monitoredEntitiesFreshnessRequestsInPeriod++;
			totalFreshnessForMonitoredEntities+=freshness;
		} else {
			freshnessRequestsInPeriod++;
			totalFreshness+=freshness;
		}

	}

	public static double GetIKMSSystemCPUUsed () {
		double output = 0.0;
		try {

			JavaSysMon monitor =   new JavaSysMon();
			int currentPID = monitor.currentPid();

			output = CPULoadFromPSCommand (currentPID);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println ("Bad CPU value. Returning last valid one.");
			return 0.0;
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
		//countIKMSCPUProcessingRequests++;
		// do not show first 10 values
		//if (countIKMSCPUProcessingRequests>10) {

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
		//}
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
