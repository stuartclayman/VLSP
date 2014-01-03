package ikms.info;

import ikms.data.IKMSOptimizationGoal;

public class SelectedOptimizationGoal {

	private static IKMSOptimizationGoal selectedOptimizationGoal = null;

	public static void SetOptimizationGoal (IKMSOptimizationGoal knowGoal) {
		selectedOptimizationGoal = knowGoal;
	}
	
	public static IKMSOptimizationGoal GetOptimizationGoal () {
		return selectedOptimizationGoal;
	}
	
}
