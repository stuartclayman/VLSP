package demo_usr.energy;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.vim.VimClient;
import demo_usr.energy.DynamicTopology;
import demo_usr.ikms.eventengine.StaticTopology;

public class IperfExperiment {

	StaticTopology staticTopology;
	DynamicTopology dynamicTopology;

	boolean staticTopologyOption=true;

	static Integer totalTime=20000;
	static Integer nodesNumber=30;
	
	ExecutorService pool;
	Future executorObj;
	
	public IperfExperiment () {

	}

	public static void main(String[] args) {
		IperfExperiment experiment =  new IperfExperiment();

		experiment.config(args);

		// Initializing topology
		experiment.InitializeTopology ();

		// delay experiment / a bit more than totalTime
		experiment.Delay (totalTime + 10000);

		// Cleanup topology
		experiment.CleanUpTopology ();
	}

	private void config(String [] args) {

		switch (args.length) {
			case 3: nodesNumber = Integer.valueOf(args[0]);
                    totalTime = Integer.valueOf(args[1]);
                    staticTopologyOption = Boolean.valueOf(args[2]);
                    break;
			default:
				System.out.println ("Syntax: nodesNumber totalTime staticTopology");
                        	System.exit(0);
				break;
		}

	}



	private void CreateTopology (final int numberOfHosts, final int totalTime) {
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

			CreateTopology (nodesNumber, totalTime);

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

		//stop eventengine
		if (staticTopologyOption) {
			staticTopology.stop();
		} else {
			dynamicTopology.stop();
		}
		executorObj.cancel(false);

		pool.shutdown();

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
