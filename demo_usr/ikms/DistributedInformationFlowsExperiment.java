package demo_usr.ikms;

import java.io.IOException;
import java.net.UnknownHostException;

import us.monoid.json.JSONObject;
import usr.vim.VimClient;

public class DistributedInformationFlowsExperiment {

	static int warmupTime=10000; // 50000
	static int totalTime=30000; // 80000
	static int nodesNumber=3;
	static int flowsNumber=1;
	static int method=3;
	static int goalId=0;
	static int monitoredFlows=0;
	static int monitoredMethod=3;
	static int monitoredGoalId=0;	

	static VimClient vimClient;

	public static void main(String[] args) {

		if (args.length==7) {
			nodesNumber = Integer.valueOf(args[0]);
			flowsNumber = Integer.valueOf(args[1]);
			method = Integer.valueOf(args[2]);
			goalId = Integer.valueOf(args[3]);
			monitoredFlows = Integer.valueOf(args[4]);
			monitoredMethod = Integer.valueOf(args[5]);
			monitoredGoalId = Integer.valueOf(args[6]);	
		} else {
			if (args.length==4) {
				nodesNumber = Integer.valueOf(args[0]);
				flowsNumber = Integer.valueOf(args[1]);
				method = Integer.valueOf(args[2]);
				goalId = Integer.valueOf(args[3]);
			} else {
				System.out.println ("Syntax: nodesNumber flowsNumber method goalId monitoredFlows monitoredMethod monitoredGoalId");
				System.exit(0);
			}
		}

		vimClient=null;
		try {
			vimClient = new VimClient();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Initializing topology
		InitializeTopology ();

		// let the routing tables propagate
		Delay (12000);

		// Initializing information flows
		InitializeInformationFlows (); 
	}

	private static void InitializeTopology () {
		if (nodesNumber==3) {
			try {
				JSONObject r1 = vimClient.createRouter();
				int router1 = (Integer)r1.get("routerID");
				System.out.println("r1 = " + r1);

				JSONObject r2 = vimClient.createRouter();
				int router2 = (Integer)r2.get("routerID");
				System.out.println("r2 = " + r2);

				JSONObject r3 = vimClient.createRouter();
				int router3 = (Integer)r3.get("routerID");
				System.out.println("r3 = " + r3);

				JSONObject l1 = vimClient.createLink(router1, router2, 10);
				int link1 = (Integer)l1.get("linkID");
				System.out.println("l1 = " + l1);

				JSONObject l2 = vimClient.createLink(router2, router3, 10);
				int link2 = (Integer)l2.get("linkID");
				System.out.println("l2 = " + l2);
			} catch (Exception e) {
				e.printStackTrace();
			} catch (Error err) {
				err.printStackTrace();
			}
		}
	}

	private static void PlaceIKMSNode () {
		if (nodesNumber==3) {
			vimClient.createApp(2, "demo_usr.ikms.IKMSForwarder", "10002 20002");
		}
	}

	private static void	InitializeInformationFlows () {
		int startingPeriod = warmupTime / flowsNumber;
		int flowTime=0;

		// place IKMS node
		PlaceIKMSNode ();

		int entityId;
		int entityRestPort;
		if (nodesNumber==3) {
			// initialize and run sources + sinks
			for (int i=0;i<flowsNumber;i++) {

				// calculate flow time
				flowTime = totalTime + (flowsNumber - i) * startingPeriod;
				
				// create sinks
				entityId = 2000+i;
				entityRestPort = 3000+i;
				//System.out.println (entityId + " "+entityRestPort+" 2 "+1000+ " " + flowTime + " " + "/test"+i+"/All"+ " " + method + " " + goalId);
				JSONObject appSource=null;

				if (i<monitoredFlows) {
					appSource = vimClient.createApp(1, "demo_usr.ikms.GenericSourceMA", "1 " + entityId + " " + entityRestPort + " 2 " + 1000 + " " + flowTime + " " + "/test"+i+"/All" + " " + monitoredMethod + " " + monitoredGoalId);
				} else {
					appSource = vimClient.createApp(1, "demo_usr.ikms.GenericSourceMA", "1 " + entityId + " " + entityRestPort + " 2 " + 1000 + " " + flowTime + " " + "/test"+i+"/All"+ " " + method+ " " + goalId);
				}
				System.out.println("appSink = " + appSource);

				// create sinks
				entityId = 4000+i;
				entityRestPort = 5000+i;
				JSONObject appSink=null;

				if (i<monitoredFlows) {
					appSink = vimClient.createApp(3, "demo_usr.ikms.GenericSinkMA", "3 " + entityId + " "+ entityRestPort + " 2 " + 1000 + " " + flowTime + " " + "/test"+i+"/All"+ " " + monitoredMethod+ " " + monitoredGoalId+ " true");
				} else {
					appSink = vimClient.createApp(3, "demo_usr.ikms.GenericSinkMA", "3 " + entityId + " "+ entityRestPort + " 2 " + 1000 + " " + flowTime + " " + "/test"+i+"/All"+ " " + method+ " " + goalId + " false");
				}
				System.out.println("appSink = " + appSink);

				Delay (startingPeriod);
			}
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
