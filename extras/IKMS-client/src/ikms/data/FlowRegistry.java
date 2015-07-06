package ikms.data;

import ikms.data.IKMSOptimizationGoal.EnforcementLevels;
import ikms.data.IKMSOptimizationGoal.OptimizationRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

// Keeps a registry of the active flows. Used in both IKMS and the Management Applications' sides
public class FlowRegistry {

	/*public interface FlowRegistryListener {
		public void updateFlowRegistery(Collection<InformationExchangePolicies> flows);
	}*/

	// Established flow exchange policies
	ArrayList <InformationExchangePolicies> flowExchangePolicies = null;
	// Index active flow exchange policies by URIs
	HashMap <String, Integer> flowExchangePoliciesIndex = null;

	//private Collection<FlowRegistryListener> listeners = new HashSet<FlowRegistryListener>();
	//private Object listnersLock = new Object();

	/*public void addListener(FlowRegistryListener aListner){
		synchronized (listnersLock) {
			listeners.add(aListner);
		}
	}*/

	/*public void removeListener(FlowRegistryListener aListner){
		synchronized (listnersLock) {
			listeners.remove(aListner);
		}
	}*/

	// flow statistics
	double numberOfFlows = 0;
	double numberOfPubSubFlows = 0;
	double numberOfPushPullFlows = 0;
	double numberOfDirectFlows = 0;

	// last updated flow statistics
	ArrayList<String> lastUpdatedStatistics = new ArrayList<String>();

	// returns number of active flows
	public double GetNumberOfFlows () {
		return numberOfFlows;
	}

	// returns number of active pub/sub flows
	public double GetNumberOfPubSubFlows () {
		return numberOfPubSubFlows;
	}

	// returns number of active push/pull flows
	public double GetNumberOfPushPullFlows () {
		return numberOfPushPullFlows;
	}

	// returns number of direct flows
	public double GetNumberOfDirectFlows () {
		return numberOfDirectFlows;
	}

	// Flow registry constructor
	public FlowRegistry () {
		// initialize flow exchange policies related tables
		flowExchangePolicies = new ArrayList <InformationExchangePolicies>();
		flowExchangePoliciesIndex = new HashMap <String, Integer>();
	}

	public void UpdateFlowRegistration(InformationExchangePolicies policies) {
		// should check whether this flow exists
		int index = FindFlowExchangePolicies (policies);
		// temp variable for old policies
		InformationExchangePolicies oldPolicies=null;
		if (index>-1) {
			// keep old policies in temp variable
			oldPolicies = flowExchangePolicies.get(index);
			// update flow counters for existing flow
			UpdateFlowCounters (oldPolicies, -1);
			// if yes, update it and update index table as well
			flowExchangePolicies.set(index, policies);			
			// update flow index table
			// remove old references
			RemoveOldUriInstancesByIndex (index);
			// add new references
			AddNewUriInstances (policies, index);
			// update flow counters for new flow
			UpdateFlowCounters (policies, 1);
		} else {
			// if not, add new flow and update index table as well
			flowExchangePolicies.add(policies);
			// add references to flow index table
			AddNewUriInstances (policies, flowExchangePolicies.indexOf(policies));
			// update flow counters for new flow
			UpdateFlowCounters (policies, 1);
		}
		//this.updateListners();
		// Dump all active flows
		//System.out.println ("Dumping all active flows.");
		//System.out.println ("(flow changed)");
		//System.out.println ("flowExchangePolicies:"+flowExchangePolicies);
		//System.out.println ("flowExchangePoliciesIndex:"+flowExchangePoliciesIndex);
	}

	/*private void updateListners() {
		synchronized (listnersLock){
			for (FlowRegistryListener listner:listeners){
				listner.updateFlowRegistery(flowExchangePolicies);
			}
		}
	}*/

	public void RemoveFlowRegistration (InformationExchangePolicies policies) {
		// should check whether this flow exists
		int index = FindFlowExchangePolicies (policies);
		if (index>-1) {
			// flow exists, remove it - leave it as empty data structure so reindexing is not required
			flowExchangePolicies.set(index, new InformationExchangePolicies());
			// update flow counters
			UpdateFlowCounters (policies, -1);
			// remove references to index table
			// iterate through all table
			Iterator<Entry<String, Integer>> it = flowExchangePoliciesIndex.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, Integer> pairs = (Entry<String, Integer>)it.next();

				// if index is found remove record
				if (pairs.getValue()==index)
					it.remove();
			}
		}
		//this.updateListners();
	}

	// Passes basic information of active flows
	// (only once per change)
	public ArrayList<String> GetActiveFlowBasicInformation () {
		ArrayList<String> result = new ArrayList<String>();
		String tempFlowInfo;
		int tempUrisSize;
		for (InformationExchangePolicies flow : flowExchangePolicies) {
			tempUrisSize = flow.GetUris().size();
			if (tempUrisSize>0) {
				tempFlowInfo = flow.getSourceEntityId()+":"+flow.getDestinationEntityId()+":"+flow.GetUris().size()+":"+flow.getMethodID();
				result.add(tempFlowInfo);
			}
		}

		//System.out.println (result+" : "+lastUpdatedStatistics+" : "+result.equals(lastUpdatedStatistics));

		// avoid flickering of workflow window - refresh once per change
		if (result.equals(lastUpdatedStatistics))
			return null;

		lastUpdatedStatistics = result;
		return result;
	}


	private void UpdateFlowCounters (InformationExchangePolicies policies, double step) {
		// Retrieve goal with embedded rules
		IKMSOptimizationGoal ruledGoal = IKMSOptimizationGoals.GetGoalById(policies.getFlowOptimizationGoal().getOptGoalId());

		// check if it is a direct flow
		boolean directEntity = ruledGoal.CheckOptimizationRule(OptimizationRules.DirectEntity2EntityCommunication);
		// check if it is a pub/sub
		boolean pubsub = ruledGoal.CheckOptimizationRule(OptimizationRules.PubSub);
		// int finalstep
		double finalstep = step;

		if (directEntity)
			numberOfDirectFlows=numberOfDirectFlows + finalstep;
		else if (pubsub)
			numberOfPubSubFlows=numberOfPubSubFlows + finalstep;
		else 
			numberOfPushPullFlows=numberOfPushPullFlows + finalstep;

		numberOfFlows = numberOfFlows + finalstep;
	}

	private void RemoveOldUriInstancesByIndex (int index) {
		// remove references to index
		// iterate through all table
		Iterator<Entry<String, Integer>> it = flowExchangePoliciesIndex.entrySet().iterator();

		while (it.hasNext()) {
			Entry<String, Integer> pairs = (Entry<String, Integer>)it.next();
			// if index is found remove record
			if (pairs.getValue()==index) {
				it.remove();
			}
		}
	}

	private void AddNewUriInstances (InformationExchangePolicies policies, int index) {
		// add new references to index
		// of available information
		ArrayList<String> uris = policies.GetUris();
		// temp variable for old InformationExchangePolicies
		InformationExchangePolicies oldPolicies = null;
		// temp variable for uri count
		Integer uricount=null;
		// temp variable for old uri index
		Integer oldIndex=null;

		for (String uri : uris) {
			// check if uri exists
			oldIndex = flowExchangePoliciesIndex.get(uri);
			if (oldIndex!=null) {
				// uri exists, so update uris in old information flow
				oldPolicies = flowExchangePolicies.get(oldIndex);
				uricount = oldPolicies.RemoveUri(uri);
				// remove old flow - leave it as empty data structure so reindexing is not required
				//System.out.println ("Checking if old flow needs removal, index:"+index+" uricount:"+uricount);
				flowExchangePolicies.set(oldIndex, new InformationExchangePolicies());

				if (uricount==0) {
					// do not add it again (no uris left)
					// update flow counters
					UpdateFlowCounters (policies, -1);
					//System.out.println ("Removing old flow:"+oldPolicies.toJSONString());
				} else {
					// readd it there
					flowExchangePolicies.set(oldIndex, oldPolicies);
				}

			}
			// add new records
			flowExchangePoliciesIndex.put(uri, index);
		}
	}	
	/*
	private void UpdateFlowExchangeIndex (InformationExchangePolicies policies, int index) {
		// remove references to index
		// iterate through all table
		Iterator<Entry<String, Integer>> it = flowExchangePoliciesIndex.entrySet().iterator();
		// temp variable for uri count
		Integer uricount=null;
		// temp variable for old InformationExchangePolicies
		InformationExchangePolicies oldPolicies = null;

		while (it.hasNext()) {
			Entry<String, Integer> pairs = (Entry<String, Integer>)it.next();
			System.out.println ("INDEX TABLE Checking uri:"+pairs.getKey()+" index:"+pairs.getValue()+" index to check:"+index);
			// if index is found remove record
			if (pairs.getValue()==index) {
				// update uris in old information flow
				oldPolicies = flowExchangePolicies.get(index);
				uricount = oldPolicies.RemoveUri(pairs.getKey());
				// remove old flow
				System.out.println ("Removing old flow, index:"+index+" uricount:"+uricount);
				flowExchangePolicies.remove(index);

				if (uricount==0) {
					// do not add it again (no uris left)
					// update flow counters
					UpdateFlowCounters (policies, -1);
				} else {
					// readd it there
					flowExchangePolicies.add(index, oldPolicies);
				}

				it.remove();
			}
		}

		// add new references to index
		// of available information
		ArrayList<String> uris = policies.GetUris();
		for (String uri : uris) {
			flowExchangePoliciesIndex.put(uri, index);
		}
	}
	 */
	private int FindFlowExchangePolicies (InformationExchangePolicies policies) {
		// search active flow policies arraylist
		for (InformationExchangePolicies currentPolicies : flowExchangePolicies) {
			// search for match for a sourceId and DestinationId
			if ((currentPolicies.getSourceEntityId()==policies.getSourceEntityId()&&currentPolicies.getDestinationEntityId()==policies.getDestinationEntityId())||(currentPolicies.getSourceEntityId()==policies.getDestinationEntityId()&&currentPolicies.getDestinationEntityId()==policies.getSourceEntityId())) {
				// there is a match, return index
				return flowExchangePolicies.indexOf(currentPolicies);
			}
		}
		// no match, return -1
		return -1;
	}

	public HashMap<ArrayList<String>, HashMap<Integer, Integer>> FindFlowsThatNeedRestablishmentDueToNewGlobalGoal (IKMSOptimizationGoal newGoal) {
		System.out.println ("Finding flows that need restablishment dut to new global goal.");

		// source and destination ids of flows that need re-establishment (uris as the key)
		HashMap<ArrayList<String>, HashMap<Integer, Integer>> result = new HashMap<ArrayList<String>, HashMap<Integer, Integer>>();
		HashMap<Integer, Integer> partialResult = null;
		// temp variables for goals
		IKMSOptimizationGoal oldGoal = null;
		IKMSOptimizationGoal negotiatedGoal = null;

		// search active flow policies 
		for (InformationExchangePolicies currentPolicies : flowExchangePolicies) {
			// ignore removed flows
			if (!(currentPolicies.getSourceEntityId()==-1&&currentPolicies.getDestinationEntityId()==-1)) {
				// get old goal
				oldGoal = currentPolicies.getFlowOptimizationGoal();
				// do a test goal negotiation to see if it changes
				negotiatedGoal = NegotiateGoal (newGoal, oldGoal);

				if (oldGoal!=null)
					System.out.println ("Checking flow with sourceid:"+currentPolicies.getSourceEntityId()+" destinationid:"+currentPolicies.getDestinationEntityId()+" oldGoal:"+oldGoal.getOptGoalName()+" negotiatedGoal:"+negotiatedGoal.getOptGoalName());

				// check if oldGoal needs updating
				// if negotiated goal is different, add flow to result hashmap
				// MAY HAVE A BUG, NOT SURE WHAT I DID HERE
				partialResult = new HashMap<Integer, Integer>();
				if (oldGoal!=null) {
					partialResult.put(currentPolicies.getSourceEntityId(), currentPolicies.getDestinationEntityId());
					result.put(currentPolicies.GetUris(), partialResult);
				// COMMENTED OUT THE FIXBUGS ISSUE
					//} else if (negotiatedGoal.getOptGoalId()!=oldGoal.getOptGoalId()) {
				//	partialResult.put(currentPolicies.getSourceEntityId(), currentPolicies.getDestinationEntityId());
				//	result.put(currentPolicies.GetUris(), partialResult);
				}
			}
		}
		// return result hashmap
		return result;
	}

	public IKMSOptimizationGoal NegotiateGoal (IKMSOptimizationGoal globalGoal, IKMSOptimizationGoal localGoal) {

		if (localGoal==null) {
			// no goal suggested, keeping global goal
			return globalGoal;
		} else {
			// check requested performance optimization goal
			// if global goal has a high priority, use that
			if (globalGoal.getOptGoalLevelofEnforcement()==EnforcementLevels.High) {
				return globalGoal;
			} 
			// if global goal has a medium priority, override source entity's priority if it is Low or Medium
			if (globalGoal.getOptGoalLevelofEnforcement()==EnforcementLevels.Medium) {
				if (localGoal.getOptGoalLevelofEnforcement()==EnforcementLevels.Low||localGoal.getOptGoalLevelofEnforcement()==EnforcementLevels.Medium) {
					return globalGoal;
				}
			}
			// if global goal has a low priority, override source entity's priority if it is low only
			if (globalGoal.getOptGoalLevelofEnforcement()==EnforcementLevels.Low) {
				if (localGoal.getOptGoalLevelofEnforcement()==EnforcementLevels.Low) {
					return globalGoal;
				}
			}

			return localGoal;
		}
	}

	// returns all flows that are relevant to a specific entity
	public ArrayList<InformationExchangePolicies> GetRelevantInformationFlows (int entityId) {

		ArrayList<InformationExchangePolicies> result = new ArrayList<InformationExchangePolicies>();
		for (InformationExchangePolicies policies : flowExchangePolicies) {
			if (policies!=null) {
				if (policies.getSourceEntityId()==entityId||policies.getDestinationEntityId()==entityId) {
					// flow is relevant to the entity
					result.add(policies);
				}
			}
		}

		return result;
	}

	// Find active flow based on a uri
	public InformationExchangePolicies GetInformationFlowExchangePolicies (String uri) {
		// retrieve index
		//System.out.println ("Searching uri:"+uri+" in table:"+flowExchangePoliciesIndex);
		Integer index = flowExchangePoliciesIndex.get(uri);

		// return null if it is not found
		if (index==null)
			return null;
		else
			return flowExchangePolicies.get(index);
	}
}
