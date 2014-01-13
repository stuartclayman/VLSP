package demo_usr.ikms;

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
import demo_usr.ikms.eventengine.StaticTopology;

public class DistributedInformationFlowsExperiment {

	int warmupTime=30000; // 50000
	int totalTime=90000; // 80000
	int nodesNumber=3;
	int informationSourcesNumber=1;
	int urisPerInformationSourceNumber=1;
	int method=3;
	int goalId=0;
	int monitoredFlows=0;
	int monitoredMethod=3;
	int monitoredGoalId=0;	
	int topologyTime = 40;
	ArrayList<Integer> routerIDs; // arraylist with existing routerIDs
	ArrayList<Integer> ikmsForwarderPerRouter; // arraylist that keeps IKMS forwarders assigned to existing routers
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
		experiment.Delay (20000);

		// looking up routers
		experiment.LookingUpRouters ();

		System.out.println ("Starting Information Flows");

		// Initializing information flows
		experiment.InitializeInformationFlows (); // takes warmupTime in mss

		// wait for the experiment to finish
		// assume that flows take warmup time to start and stop, while add an extra 5000ms to be on the safe side
		experiment.Delay (experiment.totalTime + experiment.warmupTime + 5000);

		// Cleanup topology
		experiment.CleanUpTopology ();
	}

	private void config(String [] args) {
		// initialize routerIDs arraylist
		routerIDs = new ArrayList<Integer>();
		// initialize ikmsForwarderPerRouter map
		ikmsForwarderPerRouter = new ArrayList<Integer>();

		if (args.length==9) {
			nodesNumber = Integer.valueOf(args[0]);
			informationSourcesNumber = Integer.valueOf(args[1]);
			urisPerInformationSourceNumber = Integer.valueOf(args[2]);
			totalTime = Integer.valueOf(args[3]);
			method = Integer.valueOf(args[4]);
			goalId = Integer.valueOf(args[5]);
			monitoredFlows = Integer.valueOf(args[6]);
			monitoredMethod = Integer.valueOf(args[7]);
			monitoredGoalId = Integer.valueOf(args[8]);	
		} else {
			if (args.length==6) {
				nodesNumber = Integer.valueOf(args[0]);
				informationSourcesNumber = Integer.valueOf(args[1]);
				urisPerInformationSourceNumber = Integer.valueOf(args[2]);
				totalTime = Integer.valueOf(args[3]);
				method = Integer.valueOf(args[4]);
				goalId = Integer.valueOf(args[5]);
			} else {
				System.out.println ("Syntax: nodesNumber informationSourcesNumber urisPerInformationSourceNumber totalTime method goalId monitoredFlows monitoredMethod monitoredGoalId");
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

		// calculating topology duration (it is in seconds) - set something big
		topologyTime = 100000; //1000 + 12 + (totalTime+warmupTime) / 1000;
	}

	private void LookingUpRouters () {
		System.out.println ("Looking up routers");
		JSONObject routers = null;
		JSONArray routersAr = null;

		// do that until topology appears & all nodes has been assigned to 
		try {
			int routersNotAssigned=-1;
			while (routersNotAssigned!=0) {
				routerIDs.clear();
				ikmsForwarderPerRouter.clear();
				routersNotAssigned = 0;
				while (routerIDs.size()<nodesNumber) {
					routers = new JSONObject(vimClient.listRouters().toString());
					routersAr = new JSONArray(routers.get("list").toString());
					System.out.println (routersAr.toString());

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
					if (routerIDs.size()<nodesNumber||routersNotAssigned!=0) {
						// wait a bit
						System.out.println ("Waiting for topology to appear or imksFowarder assignments to complete.");
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
		staticTopology.stop();

		executorObj.cancel(false);

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

	private void InitializeInformationFlows () {
		int sourceStartingPeriod = warmupTime / informationSourcesNumber;
		int flowStartingPeriod = sourceStartingPeriod / urisPerInformationSourceNumber;
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

		for (int k=0;k<informationNodes;k=k+2) {
			System.out.println ("selecting router:"+shuffledRouterIDs.get(k)+" for source and router:"+shuffledRouterIDs.get(k+1)+" for sink.");
			// initialize and run sources + sinks
			
			for (int i=0;i<urisPerInformationSourceNumber;i++) {
				// calculate flow time
				flowTime = totalTime + (informationSourcesNumber-(k/2)-1) * sourceStartingPeriod + (urisPerInformationSourceNumber - i) * flowStartingPeriod;
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
					if (i<monitoredFlows) {
						appSource = vimClient.createApp(shuffledRouterIDs.get(k), "demo_usr.ikms.GenericSourceMA", shuffledRouterIDs.get(k)+" " + entityId + " " + entityRestPort + " "+ikmsForwarderPerRouter.get(routerIDs.indexOf(shuffledRouterIDs.get(k)))+" " + 1000 + " " + flowTime + " " + "/test"+tempInt+"/All" + " " + monitoredMethod + " " + monitoredGoalId);
					} else {
						appSource = vimClient.createApp(shuffledRouterIDs.get(k), "demo_usr.ikms.GenericSourceMA", shuffledRouterIDs.get(k)+" " + entityId + " " + entityRestPort + " "+ikmsForwarderPerRouter.get(routerIDs.indexOf(shuffledRouterIDs.get(k)))+" " +  1000 + " " + flowTime + " " + "/test"+tempInt+"/All"+ " " + method+ " " + goalId);
					}
					System.out.println("appSink = " + appSource);

					// create sinks
					entityId = 4000+shuffledRouterIDs.get(k+1)+i;
					entityRestPort = 5000+shuffledRouterIDs.get(k+1)+i;
					JSONObject appSink=null;

					// add sink apps to third node
					if (i<monitoredFlows) {
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

}
