package ikms.data;

import ikms.data.IKMSOptimizationGoal.OptimizationRules;

import java.util.ArrayList;

// The IKMSOptimizationGoals object: keeps all available optimization goals
public class IKMSOptimizationGoals {

	// Available optimization goals
	private static ArrayList<IKMSOptimizationGoal> ikmsOptimizationGoals=new ArrayList<IKMSOptimizationGoal>();

	// Initialize all available optimization goals
	private static void InitalizeOptimizationGoals () {
		// Initialize available optimization goals options
		// pull from entity goal - fetches information from source entity first and then checks storage
		IKMSOptimizationGoal pullFromEntityGoal = new IKMSOptimizationGoal(0, "Pull from Entity", "", IKMSOptimizationGoal.EnforcementLevels.High);
		pullFromEntityGoal.AddOptimizationRule(OptimizationRules.FirstFetchThenRetrieveFromStorage);
		IKMSOptimizationGoals.ikmsOptimizationGoals.add(pullFromEntityGoal);

		// pull from storage goal - first checks storage and then fetches information from source entity
		IKMSOptimizationGoal pullFromStorageGoal = new IKMSOptimizationGoal(1, "Pull from Storage", "", IKMSOptimizationGoal.EnforcementLevels.High);
		IKMSOptimizationGoals.ikmsOptimizationGoals.add(pullFromStorageGoal);

		// pub/sub goal
		IKMSOptimizationGoal pubSubGoal = new IKMSOptimizationGoal(2, "Pubsub", "", IKMSOptimizationGoal.EnforcementLevels.High);
		pubSubGoal.AddOptimizationRule(OptimizationRules.PubSub);
		IKMSOptimizationGoals.ikmsOptimizationGoals.add(pubSubGoal);

		// direct entity goal
		IKMSOptimizationGoal directEntityGoal = new IKMSOptimizationGoal(3, "Direct Entity", "", IKMSOptimizationGoal.EnforcementLevels.High);
		directEntityGoal.AddOptimizationRule(OptimizationRules.DirectEntity2EntityCommunication);
		IKMSOptimizationGoals.ikmsOptimizationGoals.add(directEntityGoal);
		
		// pull from entity goal (compact version to save communication cost)
		IKMSOptimizationGoal compactPullFromEntityGoal = new IKMSOptimizationGoal(4, "Pull from Entity (compact)", "", IKMSOptimizationGoal.EnforcementLevels.High);
		compactPullFromEntityGoal.AddOptimizationRule(OptimizationRules.FirstFetchThenRetrieveFromStorage);
		compactPullFromEntityGoal.AddOptimizationRule(OptimizationRules.LightweightDataStructures);
		IKMSOptimizationGoals.ikmsOptimizationGoals.add(compactPullFromEntityGoal);
		
		// pull from storage goal (compact version to save communication cost)
		IKMSOptimizationGoal compactPullFromStorageGoal = new IKMSOptimizationGoal(5, "Pull from Storage (compact)", "", IKMSOptimizationGoal.EnforcementLevels.High);
		compactPullFromStorageGoal.AddOptimizationRule(OptimizationRules.LightweightDataStructures);
		IKMSOptimizationGoals.ikmsOptimizationGoals.add(compactPullFromStorageGoal);

		// pub/sub goal (compact version to save communication cost)
		IKMSOptimizationGoal compactPubSubGoal = new IKMSOptimizationGoal(6, "Pub/sub (compact)", "", IKMSOptimizationGoal.EnforcementLevels.High);
		compactPubSubGoal.AddOptimizationRule(OptimizationRules.PubSub);
		compactPubSubGoal.AddOptimizationRule(OptimizationRules.LightweightDataStructures);
		IKMSOptimizationGoals.ikmsOptimizationGoals.add(compactPubSubGoal);

		// direct entity goal (compact version to save communication cost)
		IKMSOptimizationGoal compactDirectEntityGoal = new IKMSOptimizationGoal(7, "Direct Entity (compact)", "", IKMSOptimizationGoal.EnforcementLevels.High);
		compactDirectEntityGoal.AddOptimizationRule(OptimizationRules.DirectEntity2EntityCommunication);
		compactDirectEntityGoal.AddOptimizationRule(OptimizationRules.LightweightDataStructures);
		IKMSOptimizationGoals.ikmsOptimizationGoals.add(compactDirectEntityGoal);

		
		// Add goal option for reducing response time
		// Reduce Response Time (1) - Communication through IKMS enabled, prioritize IKMS stored values than Entity fetching
		IKMSOptimizationGoal responseTimeGoal = new IKMSOptimizationGoal(8, "Reduce response time", "", IKMSOptimizationGoal.EnforcementLevels.High);
		// Add optimization rules to goal
		responseTimeGoal.AddOptimizationRule(OptimizationRules.LightweightDataStructures);
		responseTimeGoal.AddOptimizationRule(OptimizationRules.MultipleKNOWInstances);
		IKMSOptimizationGoals.ikmsOptimizationGoals.add(responseTimeGoal);

		// Add goal option for reducing processing cost
		// Reduce Processing Cost (2) - Direct entity2entity communication enabled
		IKMSOptimizationGoal reduceProcessingGoal = new IKMSOptimizationGoal(9, "Reduce processing cost", "", IKMSOptimizationGoal.EnforcementLevels.High);
		// Add optimization rules to goal
		reduceProcessingGoal.AddOptimizationRule(OptimizationRules.DirectEntity2EntityCommunication);
		reduceProcessingGoal.AddOptimizationRule(OptimizationRules.DoNotCommunicateMeasurements);
		IKMSOptimizationGoals.ikmsOptimizationGoals.add(reduceProcessingGoal);

		// Add goal option for reducing communication overhead
		// Reduce Communication Overhead (3) - Direct entity2entity communication enabled, lightweight data structures
		IKMSOptimizationGoal reduceCommunicationOverheadGoal = new IKMSOptimizationGoal(10, "Reduce communication overhead", "", IKMSOptimizationGoal.EnforcementLevels.High);
		// Add optimization rules to goal
		reduceCommunicationOverheadGoal.AddOptimizationRule(OptimizationRules.DirectEntity2EntityCommunication);
		reduceCommunicationOverheadGoal.AddOptimizationRule(OptimizationRules.LightweightDataStructures);
		reduceCommunicationOverheadGoal.AddOptimizationRule(OptimizationRules.DoNotCommunicateMeasurements);
		IKMSOptimizationGoals.ikmsOptimizationGoals.add(reduceCommunicationOverheadGoal);

		// Add goal for improving information accuracy
		// Improve Information Accuracy (4) - Communication through IKMS enabled, prioritize fetching than IKMS values
		IKMSOptimizationGoal improveInformationAccuracyGoal = new IKMSOptimizationGoal(11, "Improve information accuracy", "", IKMSOptimizationGoal.EnforcementLevels.High);
		// Add optimization rules to goal
		improveInformationAccuracyGoal.AddOptimizationRule(OptimizationRules.DoNotStoreWithoutANeed);
		improveInformationAccuracyGoal.AddOptimizationRule(OptimizationRules.FirstFetchThenRetrieveFromStorage);

		IKMSOptimizationGoals.ikmsOptimizationGoals.add(improveInformationAccuracyGoal);

		// Add goal for energy efficiency
		// Improve Energy Efficiency (5) - Communication through IKMS enabled, prioritize IKMS stored values than Entity fetching
		IKMSOptimizationGoal improveEnergyEfficiencyGoal = new IKMSOptimizationGoal(12, "Improve energy efficiency", "", IKMSOptimizationGoal.EnforcementLevels.High);
		// Add optimization rules to goal
		improveEnergyEfficiencyGoal.AddOptimizationRule(OptimizationRules.LightweightDataStructures);
		improveEnergyEfficiencyGoal.AddOptimizationRule(OptimizationRules.DoNotCommunicateMeasurements);
		IKMSOptimizationGoals.ikmsOptimizationGoals.add(improveEnergyEfficiencyGoal);

	}

	// Functions that retrieve available goals
	public static IKMSOptimizationGoal GetPullFromEntityGoal () {
		if (ikmsOptimizationGoals.isEmpty()) {
			InitalizeOptimizationGoals();
		}

		return ikmsOptimizationGoals.get(0);
	}
	
	public static IKMSOptimizationGoal GetPullFromStorageGoal () {
		if (ikmsOptimizationGoals.isEmpty()) {
			InitalizeOptimizationGoals();
		}

		return ikmsOptimizationGoals.get(1);
	}
	
	public static IKMSOptimizationGoal GetPubSubGoal () {
		if (ikmsOptimizationGoals.isEmpty()) {
			InitalizeOptimizationGoals();
		}

		return ikmsOptimizationGoals.get(2);
	}
	
	public static IKMSOptimizationGoal GetDirectEntityGoal () {
		if (ikmsOptimizationGoals.isEmpty()) {
			InitalizeOptimizationGoals();
		}

		return ikmsOptimizationGoals.get(3);
	}
	
	public static IKMSOptimizationGoal GetCompactPullFromEntityGoal () {
		if (ikmsOptimizationGoals.isEmpty()) {
			InitalizeOptimizationGoals();
		}

		return ikmsOptimizationGoals.get(4);
	}
	
	public static IKMSOptimizationGoal GetCompactPullFromStorageGoal () {
		if (ikmsOptimizationGoals.isEmpty()) {
			InitalizeOptimizationGoals();
		}

		return ikmsOptimizationGoals.get(5);
	}
	
	public static IKMSOptimizationGoal GetCompactPubSubGoal () {
		if (ikmsOptimizationGoals.isEmpty()) {
			InitalizeOptimizationGoals();
		}

		return ikmsOptimizationGoals.get(6);
	}
	
	public static IKMSOptimizationGoal GetCompactDirectEntityGoal () {
		if (ikmsOptimizationGoals.isEmpty()) {
			InitalizeOptimizationGoals();
		}

		return ikmsOptimizationGoals.get(7);
	}
	
	public static IKMSOptimizationGoal GetResponseTimeGoal () {
		if (ikmsOptimizationGoals.isEmpty()) {
			InitalizeOptimizationGoals();
		}

		return ikmsOptimizationGoals.get(8);
	}

	public static IKMSOptimizationGoal GetProcessingCostGoal () {
		if (ikmsOptimizationGoals.isEmpty())
			InitalizeOptimizationGoals();

		return ikmsOptimizationGoals.get(9);
	}

	public static IKMSOptimizationGoal GetCommunicationOverheadGoal () {
		if (ikmsOptimizationGoals.isEmpty())
			InitalizeOptimizationGoals();

		return ikmsOptimizationGoals.get(10);
	}

	public static IKMSOptimizationGoal GetInformationAccuracyGoal () {
		if (ikmsOptimizationGoals.isEmpty())
			InitalizeOptimizationGoals();

		return ikmsOptimizationGoals.get(11);
	}

	public static IKMSOptimizationGoal GetEnergyEfficiencyGoal () {
		if (ikmsOptimizationGoals.isEmpty())
			InitalizeOptimizationGoals();

		return ikmsOptimizationGoals.get(12);
	}
	
	// Retrieves available goal out of goal id
	public static IKMSOptimizationGoal GetGoalById (int goalId) {
		if (ikmsOptimizationGoals.isEmpty())
			InitalizeOptimizationGoals();

		return ikmsOptimizationGoals.get(goalId);
	}
}
