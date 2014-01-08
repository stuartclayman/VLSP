package demo_usr.ikms;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.vim.VimClient;
import demo_usr.ikms.eventengine.StaticTopology;

public class DistributedInformationFlowsExperiment {

	 int warmupTime=10000; // 50000
	 int totalTime=30000; // 80000
	 int nodesNumber=3;
	 int flowsNumber=1;
	 int method=3;
	 int goalId=0;
	 int monitoredFlows=0;
	 int monitoredMethod=3;
	 int monitoredGoalId=0;	
	 int topologyTime = 40;
	 ArrayList<Integer> routerIDs;

	 VimClient vimClient;

	 StaticTopology staticTopology;
	
         ExecutorService pool;
	 Future executorObj;

	public static void main(String[] args) {
            DistributedInformationFlowsExperiment experiment =  new DistributedInformationFlowsExperiment();

            experiment.config(args);

		// Initializing topology
		experiment.InitializeTopology ();

		// let the routing tables propagate
		experiment.Delay (12000);

		// looking up routers
		experiment.LookingUpRouters ();

		System.out.println ("Starting Information Flows");

		// Initializing information flows
		//InitializeInformationFlows (); // takes warmupTime in mss

		// wait for the experiment to finish
		// assume that flows take warmup time to start and add an extra 2000ms to be sure
		experiment.Delay (experiment.totalTime + experiment.warmupTime + 2000);

		// Cleanup topology
		experiment.CleanUpTopology ();
	}

    private void config(String [] args) {
		// initialize routerIDs arraylist
		routerIDs = new ArrayList<Integer>();

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

		// calculating topology duration (it is in seconds)
		topologyTime = 1000 + 12 + (totalTime+warmupTime) / 1000;


    }

	private void LookingUpRouters () {
		System.out.println ("Looking up routers");
		JSONObject routers = null;
		JSONArray routersAr = null;
		try {
			routers = new JSONObject(vimClient.listRouters().toString());
			routersAr = new JSONArray(routers.get("list").toString());
			System.out.println (routersAr.toString());
			// iterate through jsonarray
			for (int i = 0; i < routersAr.length(); i++) {
				routerIDs.add(routersAr.getInt(i));
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void CreateStaticTopology (final int numberOfHosts, final int totalTime) {
                    pool = Executors.newFixedThreadPool(1);

                    executorObj = pool.submit(new Callable(){
			public Object call() {

                            try {
                                staticTopology = new StaticTopology(numberOfHosts, totalTime);
                            } catch (UnknownHostException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }

                            staticTopology.init();

                            staticTopology.start();
		
                            staticTopology.run();
                            System.out.println ("Run stopped.");

                            return new Object();
			}
                    });
	}

	private void InitializeTopology () {
		Integer routerID1=null;
		Integer routerID2=null;
		Integer routerID3=null;

		if (nodesNumber==3) {
			try {

				CreateStaticTopology (3, topologyTime);

				/*JSONObject r1 = vimClient.createRouter();
				routerID1 = (Integer)r1.get("routerID");
				routerIDs.add(routerID1);
				System.out.println("r1 = " + routerID1);

				JSONObject r2 = vimClient.createRouter();
				routerID2 = (Integer)r2.get("routerID");
				routerIDs.add(routerID2);
				System.out.println("r2 = " + routerID2);

				JSONObject r3 = vimClient.createRouter();
				routerID3 = (Integer)r3.get("routerID");
				routerIDs.add(routerID3);
				System.out.println("r3 = " + routerID3);

				JSONObject l1 = vimClient.createLink(routerID1, routerID2, 10);
				int link1 = (Integer)l1.get("linkID");
				System.out.println("l1 = " + l1);

				JSONObject l2 = vimClient.createLink(routerID2, routerID3, 10);
				int link2 = (Integer)l2.get("linkID");
				System.out.println("l2 = " + l2);*/
			} catch (Exception e) {
				e.printStackTrace();
			} catch (Error err) {
				err.printStackTrace();
			}
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
		System.out.println ("STOPPING EVENT ENGINE!!!!!");
		staticTopology.stop();
		
		executorObj.cancel(false);
		System.out.println ("EVENT ENGINE STOPPED!!!!!");


                pool.shutdown();

	}

	private void PlaceIKMSNode () {
		if (nodesNumber==3) {
			try {
				// create sinks
				int entityId = 10000+routerIDs.get(1);
				int entityRestPort = 20000+routerIDs.get(1);

				// add IKMSforwarder to second node
				vimClient.createApp(routerIDs.get(1), "demo_usr.ikms.IKMSForwarder", entityId+" "+entityRestPort);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private  void	InitializeInformationFlows () {
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
				int tempInt = routerIDs.get(0)+i;
				entityId = 2000+tempInt;
				entityRestPort = 3000+tempInt;
				//System.out.println (entityId + " "+entityRestPort+" 2 "+1000+ " " + flowTime + " " + "/test"+i+"/All"+ " " + method + " " + goalId);
				JSONObject appSource=null;

				try {
					// add source apps to first node
					if (i<monitoredFlows) {
						appSource = vimClient.createApp(routerIDs.get(0), "demo_usr.ikms.GenericSourceMA", routerIDs.get(0)+" " + entityId + " " + entityRestPort + " "+routerIDs.get(1)+" " + 1000 + " " + flowTime + " " + "/test"+tempInt+"/All" + " " + monitoredMethod + " " + monitoredGoalId);
					} else {
						appSource = vimClient.createApp(routerIDs.get(0), "demo_usr.ikms.GenericSourceMA", routerIDs.get(0)+" " + entityId + " " + entityRestPort + " "+routerIDs.get(1)+" " +  1000 + " " + flowTime + " " + "/test"+tempInt+"/All"+ " " + method+ " " + goalId);
					}
					System.out.println("appSink = " + appSource);

					// create sinks
					entityId = 4000+routerIDs.get(2)+i;
					entityRestPort = 5000+routerIDs.get(2)+i;
					JSONObject appSink=null;

					// add sink apps to third node
					if (i<monitoredFlows) {
						appSink = vimClient.createApp(routerIDs.get(2), "demo_usr.ikms.GenericSinkMA", routerIDs.get(2)+" " + entityId + " "+ entityRestPort + " "+routerIDs.get(1)+" " +  1000 + " " + flowTime + " " + "/test"+tempInt+"/All"+ " " + monitoredMethod+ " " + monitoredGoalId+ " true");
					} else {
						appSink = vimClient.createApp(routerIDs.get(2), "demo_usr.ikms.GenericSinkMA", routerIDs.get(2)+" " + entityId + " "+ entityRestPort + " "+routerIDs.get(1)+" " +  1000 + " " + flowTime + " " + "/test"+tempInt+"/All"+ " " + method+ " " + goalId + " false");
					}
					System.out.println("appSink = " + appSink);
				} catch (JSONException ex) {
					ex.printStackTrace(); 
				}
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
