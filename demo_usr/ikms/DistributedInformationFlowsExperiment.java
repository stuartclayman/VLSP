package demo_usr.ikms;

import ikms.data.IKMSOptimizationGoals;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.vim.VimClient;
import demo_usr.energy.DynamicTopology;
import demo_usr.ikms.eventengine.StaticTopology;

public class DistributedInformationFlowsExperiment {

	int warmupTime=100000; // 50000
	int totalTime=150000; // 80000 
	int nodesNumber=3;
	int informationSourcesNumber=1;
	int urisPerInformationSourceNumber=1;
	int method=3;
	int goalId=0;
	int monitoredFlows=0;
	int monitoredMethod=3;
	int monitoredGoalId=0;	
	int topologyTime = 40;
	int newGoal=-1; // no new goal set (i.e., for experiments that change the global goal after some time)
	int newGoalDelay=0; // waiting time before applying the new goal
	
	ArrayList<Integer> routerIDs; // arraylist with existing routerIDs
	ArrayList<Integer> ikmsForwarderPerRouter; // arraylist that keeps IKMS forwarders assigned to existing routers
	VimClient vimClient;

	StaticTopology staticTopology;
	DynamicTopology dynamicTopology;

	ExecutorService pool;
	Future executorObj;

	int sourceStartingPeriod;
	int flowStartingPeriod;

	boolean staticTopologyOption=true;

	public DistributedInformationFlowsExperiment () {

	}

	public static void main(String[] args) {
		DistributedInformationFlowsExperiment experiment =  new DistributedInformationFlowsExperiment();

		experiment.config(args);

		// Initializing topology
		experiment.InitializeTopology ();

		// let the routing tables propagate
		experiment.Delay (20000);

		// place ikmsforwarder manually
		//experiment.PlaceIKMSNode ();

		// looking up routers
		experiment.LookingUpRouters (); // for dynamic placement

		System.out.println ("Starting Information Flows");

		// calculate flow periods
		experiment.calculateFlowPeriods();

		// Initializing information flows
		experiment.InitializeInformationFlows (); // takes warmupTime in mss

		// wait for the experiment to finish
		// assume that flows take warmup time to start and warmup time to stop
		// if a goal is set, apply the goal and then wait again
		experiment.NewGoalDelay (experiment.totalTime + experiment.warmupTime);

		// Cleanup topology
		experiment.CleanUpTopology ();
	}

	private void calculateFlowPeriods () {
		sourceStartingPeriod = warmupTime / informationSourcesNumber;
		flowStartingPeriod = sourceStartingPeriod / urisPerInformationSourceNumber;
	}

	private void config(String [] args) {
		// initialize routerIDs arraylist
		routerIDs = new ArrayList<Integer>();
		// initialize ikmsForwarderPerRouter map
		ikmsForwarderPerRouter = new ArrayList<Integer>();

		switch (args.length) {
			case 9: nodesNumber = Integer.valueOf(args[0]);
                        	informationSourcesNumber = Integer.valueOf(args[1]);
                        	urisPerInformationSourceNumber = Integer.valueOf(args[2]);
                        	totalTime = Integer.valueOf(args[3]);
                        	method = Integer.valueOf(args[4]);
                        	goalId = Integer.valueOf(args[5]);
                        	monitoredFlows = Integer.valueOf(args[6]);
                        	monitoredMethod = Integer.valueOf(args[7]);
                        	monitoredGoalId = Integer.valueOf(args[8]);
				break;
			case 11: nodesNumber = Integer.valueOf(args[0]);
                                 informationSourcesNumber = Integer.valueOf(args[1]);
                                 urisPerInformationSourceNumber = Integer.valueOf(args[2]);
                                 totalTime = Integer.valueOf(args[3]);
                                 method = Integer.valueOf(args[4]);
                                 goalId = Integer.valueOf(args[5]);
                                 monitoredFlows = Integer.valueOf(args[6]);
                                 monitoredMethod = Integer.valueOf(args[7]);
                                 monitoredGoalId = Integer.valueOf(args[8]);
                                 newGoal = Integer.valueOf(args[9]);
				 break;
			case 6: nodesNumber = Integer.valueOf(args[0]);
                                informationSourcesNumber = Integer.valueOf(args[1]);
                                urisPerInformationSourceNumber = Integer.valueOf(args[2]);
                                totalTime = Integer.valueOf(args[3]);
                                method = Integer.valueOf(args[4]);
                                goalId = Integer.valueOf(args[5]);
				break;
			case 7: nodesNumber = Integer.valueOf(args[0]);
                                informationSourcesNumber = Integer.valueOf(args[1]);
                                urisPerInformationSourceNumber = Integer.valueOf(args[2]);
                                totalTime = Integer.valueOf(args[3]);
                                method = Integer.valueOf(args[4]);
                                goalId = Integer.valueOf(args[5]);
				staticTopologyOption = Boolean.valueOf(args[6]);
                                break;
			case 8: nodesNumber = Integer.valueOf(args[0]);
                                informationSourcesNumber = Integer.valueOf(args[1]);
                                urisPerInformationSourceNumber = Integer.valueOf(args[2]);
                                totalTime = Integer.valueOf(args[3]);
                                method = Integer.valueOf(args[4]);
                                goalId = Integer.valueOf(args[5]);
                                newGoal = Integer.valueOf(args[6]);
                                newGoalDelay = Integer.valueOf(args[7]);
				break;
			default:
				System.out.println ("Syntax: nodesNumber informationSourcesNumber urisPerInformationSourceNumber totalTime method goalId monitoredFlows monitoredMethod monitoredGoalId newGoal newGoalDelay");
                        	System.exit(0);
				break;
		}

		vimClient=null;
		try {
			vimClient = new VimClient();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
			
		try {
			JSONObject json=vimClient.listApps(200);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		// calculating topology duration (it is in seconds) - set something big
		topologyTime = 150000; //1000 + 12 + (totalTime+warmupTime) / 1000;
	}

	private void LookingUpRouters () {
		System.out.println ("Looking up routers");
		JSONObject routers = null;
		JSONArray routersAr = null;

		// do that until topology appears & all routers has been assigned to ikmsForwarders
		try {
			int routersNotAssigned=-1;
			boolean topologyFinished = false;
			while (routersNotAssigned!=0) {
				if (!topologyFinished) {
					// check how many routers are there
					routers = new JSONObject(vimClient.listRouters().toString());
					routersAr = new JSONArray(routers.get("list").toString());

					if (routersAr.length()<nodesNumber) {
						// wait a bit
						System.out.println ("Waiting for topology to appear. Currently "+routersAr.length()+ " routers out of "+nodesNumber+".");
						Delay (10000);
					} else {
						System.out.println ("Topology ready. Waiting for ikms placement to complete.");
						topologyFinished=true;
					}
				} else {
					routersNotAssigned=0;
					routerIDs.clear();
					ikmsForwarderPerRouter.clear();
					// iterate through jsonarray
					int currentid=0;
					int currentforwarder=0;
					for (int i = 0; i < routersAr.length(); i++) {
						currentid = routersAr.getInt(i);
						routerIDs.add(currentid);
						currentforwarder = vimClient.getAggPointInfo(currentid).getInt("ap");
						System.out.println ("Router :"+currentid+" has been assigned to ikmsForwarder:"+currentforwarder);
						ikmsForwarderPerRouter.add(currentforwarder);
						if (currentforwarder==0)
							routersNotAssigned++;
					}
					System.out.println ("Number of routers:"+routerIDs.size()+" out of "+nodesNumber);
					if (routersNotAssigned!=0) {
						// wait a bit
						System.out.println ("Waiting for imksFowarder assignments to complete. Currently, "+routersNotAssigned+" routers not assigned yet.");
						Delay (10000);
					}	
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void CreateStaticTopology (final int numberOfHosts, final int totalTime) {
		pool = Executors.newFixedThreadPool(1);

		executorObj = pool.submit(new Callable<Object>(){
			public Object call() {

				try {
					if (staticTopologyOption) {
						staticTopology = new StaticTopology(numberOfHosts, totalTime);
					} else {
						dynamicTopology = new DynamicTopology(numberOfHosts, totalTime);
					}
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (staticTopologyOption) {
					staticTopology.init();

					staticTopology.start();

					staticTopology.run();
				} else {
					dynamicTopology.init();

                                        dynamicTopology.start();

                                        dynamicTopology.run();
				}

				System.out.println ("Run stopped.");

				return new Object();
			}
		});
	}

	private void InitializeTopology () {
		try {

			CreateStaticTopology (nodesNumber, topologyTime);

		} catch (Exception e) {
			e.printStackTrace();
		} catch (Error err) {
			err.printStackTrace();
		}
	}

	private  void CleanUpTopology () {
		// doing my own cleaning up
		/*if (nodesNumber==3) {
			try {
				for (Integer routerID : routerIDs) {
					// delete all routers
					vimClient.deleteRouter(routerID);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} catch (Error err) {
				err.printStackTrace();
			}
			// cleanup routerIDs arraylist
			routerIDs.clear();
		}*/

		// cleanup routerIDs arraylist		
		routerIDs.clear();

		//stop eventengine
		if (staticTopologyOption) {
			staticTopology.stop();
		} else {
			dynamicTopology.stop();
		}
		executorObj.cancel(false);

		pool.shutdown();

	}

	private void PlaceIKMSNode () {
		if (nodesNumber==3) {
			try {
				// create ikmsforwarder in first node
				int entityId = 27000+1;
				int entityRestPort = 28000+1;

				// add IKMSforwarder to second node
				vimClient.createApp(1, "demo_usr.ikms.IKMSForwarder", entityId+" "+entityRestPort);

				// assign this IKMSforwarder to all three nodes
				ikmsForwarderPerRouter.add(1);
				ikmsForwarderPerRouter.add(1);
				ikmsForwarderPerRouter.add(1);

				// fill in the available routers data structure
				JSONObject routers = null;
				JSONArray routersAr = null;
				routers = new JSONObject(vimClient.listRouters().toString());
				routersAr = new JSONArray(routers.get("list").toString());
				System.out.println (routersAr.toString());

				// iterate through jsonarray
				int currentid=0;
				int currentforwarder=0;
				for (int i = 0; i < routersAr.length(); i++) {
					currentid = routersAr.getInt(i);
					routerIDs.add(currentid);
					System.out.println ("Router :"+currentid+" has been assigned to ikmsForwarder:1");
					ikmsForwarderPerRouter.add(currentforwarder);
				}

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void InitializeInformationFlows () {
		int flowTime=0;

		// manual IKMS placement
		//PlaceIKMSNode ();

		// select "informationSourcesNumber" sources out of "nodesNumber" nodes
		// shuffle routerIDs data structure
		ArrayList<Integer> shuffledRouterIDs = routerIDs;
		Collections.shuffle (shuffledRouterIDs);

		// remove IKMS nodes from collection
		for (int i:ikmsForwarderPerRouter) {
			if (shuffledRouterIDs.contains(i))
				shuffledRouterIDs.remove(shuffledRouterIDs.indexOf(i));
		}

		int entityId;
		int entityRestPort;

		// for every information source + sink pair
		// information node pairs are a double number compared to sources
		int informationNodes=informationSourcesNumber * 2;
		
		// doubling monitored flows as well (i.e., because step has ++2)
		int doubleMonitoredFlows=monitoredFlows * 2;
		
		for (int k=0;k<informationNodes;k=k+2) {

			System.out.println ("selecting router:"+shuffledRouterIDs.get(k)+" for source and router:"+shuffledRouterIDs.get(k+1)+" for sink.");
			// initialize and run sources + sinks

			for (int i=0;i<urisPerInformationSourceNumber;i++) {
				// calculate flow time (stop at the same time)
				//flowTime = totalTime + (informationSourcesNumber-(k/2)-1) * sourceStartingPeriod + (urisPerInformationSourceNumber - i) * flowStartingPeriod;
				flowTime = totalTime + flowStartingPeriod;
				System.out.println ("i:"+i+" k:"+k+" sourceStartingPeriod:"+sourceStartingPeriod+" flowStartingPeriod:"+flowStartingPeriod);
				System.out.println ("New flow duration:"+flowTime);
				// create sinks
				int tempInt = shuffledRouterIDs.get(k)+i;
				entityId = 2000+tempInt;
				entityRestPort = 3000+tempInt;
				//System.out.println (entityId + " "+entityRestPort+" 2 "+1000+ " " + flowTime + " " + "/test"+i+"/All"+ " " + method + " " + goalId);
				JSONObject appSource=null;
		
				try {
					// add source apps to first node
					if (k<doubleMonitoredFlows) {
						appSource = vimClient.createApp(shuffledRouterIDs.get(k), "demo_usr.ikms.GenericSourceMA", shuffledRouterIDs.get(k)+" " + entityId + " " + entityRestPort + " "+ikmsForwarderPerRouter.get(routerIDs.indexOf(shuffledRouterIDs.get(k)))+" " + 1000 + " " + flowTime + " " + "/test"+tempInt+"/All" + " " + monitoredMethod + " " + monitoredGoalId);
					} else {
						appSource = vimClient.createApp(shuffledRouterIDs.get(k), "demo_usr.ikms.GenericSourceMA", shuffledRouterIDs.get(k)+" " + entityId + " " + entityRestPort + " "+ikmsForwarderPerRouter.get(routerIDs.indexOf(shuffledRouterIDs.get(k)))+" " +  1000 + " " + flowTime + " " + "/test"+tempInt+"/All"+ " " + method+ " " + goalId);
					}
					System.out.println("appSource = " + appSource);

					// create sinks
					entityId = 4000+shuffledRouterIDs.get(k+1)+i;
					entityRestPort = 5000+shuffledRouterIDs.get(k+1)+i;
					JSONObject appSink=null;

					// add sink apps to third node
					if (k<doubleMonitoredFlows) {
						appSink = vimClient.createApp(shuffledRouterIDs.get(k+1), "demo_usr.ikms.GenericSinkMA", shuffledRouterIDs.get(k+1)+" " + entityId + " "+ entityRestPort + " "+ikmsForwarderPerRouter.get(routerIDs.indexOf(shuffledRouterIDs.get(k+1)))+" " +  1000 + " " + flowTime + " " + "/test"+tempInt+"/All"+ " " + monitoredMethod+ " " + monitoredGoalId+ " true");
					} else {
						appSink = vimClient.createApp(shuffledRouterIDs.get(k+1), "demo_usr.ikms.GenericSinkMA", shuffledRouterIDs.get(k+1)+" " + entityId + " "+ entityRestPort + " "+ikmsForwarderPerRouter.get(routerIDs.indexOf(shuffledRouterIDs.get(k+1)))+" " +  1000 + " " + flowTime + " " + "/test"+tempInt+"/All"+ " " + method+ " " + goalId + " false");
					}
					System.out.println("appSink = " + appSink);
				} catch (JSONException ex) {
					ex.printStackTrace(); 
				}
				Delay (flowStartingPeriod);
			}
		}

	}


	private void Delay (int delayTime) {
		try {
			System.out.println ("Waiting "+delayTime+" ms");
			Thread.sleep(delayTime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void NewGoalDelay (int delayTime) {
		try {
			// if a goal is set, apply the goal and then wait again
			if (newGoal>=0) {
				int newDelay = delayTime - newGoalDelay;
				System.out.println ("Waiting "+newGoalDelay+" ms before applying the new goal:"+newGoal);
				Thread.sleep(newGoalDelay);				
				// applying new goal
				GovernanceLocalApplication gov = new GovernanceLocalApplication();
				gov.ApplyGoal(IKMSOptimizationGoals.GetGoalById(newGoal));
				// unregistering entity
				gov.Unregister();
				// waiting the remaining time

				System.out.println ("Waiting the remaining time:"+newDelay+" ms");
				Thread.sleep(newDelay);
			} else {
				System.out.println ("Waiting "+delayTime+" ms (no new goal specified)");				
				Thread.sleep(delayTime);				
			}
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
