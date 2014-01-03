package ikms.examples;

public class LocalInformationFlowsExperiment {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int warmupTime=50000;
		int totalTime=80000;
		int flowsNumber=1;
		int method=3;
		int goalId=0;
		int monitoredFlows=0;
		int monitoredMethod=3;
		int monitoredGoalId=0;	

		if (args.length==6) {
			flowsNumber = Integer.valueOf(args[0]);
			method = Integer.valueOf(args[1]);
			goalId = Integer.valueOf(args[2]);
			monitoredFlows = Integer.valueOf(args[3]);
			monitoredMethod = Integer.valueOf(args[4]);
			monitoredGoalId = Integer.valueOf(args[5]);	
		} else {
			if (args.length==3) {
				flowsNumber = Integer.valueOf(args[0]);
				method = Integer.valueOf(args[1]);
				goalId = Integer.valueOf(args[2]);
			} else {
				System.out.println ("Syntax: flowsNumber method goalId monitoredFlows monitoredMethod monitoredGoalId");
				System.exit(0);
			}
		}
		
		int startingPeriod = warmupTime / flowsNumber;
		int flowTime=0;

		GenericSourceMA[] sources = new GenericSourceMA[flowsNumber];
		GenericSinkMA[] sinks = new GenericSinkMA[flowsNumber];

		// initialize and run sources + sinks
		for (int i=0;i<flowsNumber;i++) {
			
			flowTime = totalTime + (flowsNumber - i) * startingPeriod;
			
			if (i<monitoredFlows)
				sources[i] = new GenericSourceMA (2000+i, 1000, flowTime, "/test"+i+"/All", monitoredMethod, monitoredGoalId);
			else
				sources[i] = new GenericSourceMA (2000+i, 1000, flowTime, "/test"+i+"/All", method, goalId);

			if (i<monitoredFlows)
				sinks[i] = new GenericSinkMA (4000+i, 1000, flowTime, "/test"+i+"/All", monitoredMethod, monitoredGoalId, true);
			else
				sinks[i] = new GenericSinkMA (4000+i, 1000, flowTime, "/test"+i+"/All", method, goalId, false);

			sources[i].start();
			sinks[i].start();

			Delay (startingPeriod);
		}
	}

	private static void Delay (int delayTime) {
		try {
			System.out.println ("Starting next flow in "+delayTime+" ms");
			Thread.sleep(delayTime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
