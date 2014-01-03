package demo_usr.ikms;

import java.io.IOException;
import java.util.Scanner;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.applications.Application;
import usr.applications.ApplicationResponse;
import demo_usr.ikms.client.IKMSEnabledUSREntity;
import demo_usr.ikms.client.utils.Logging;

// Example Sink Entity that communicates directly with a Source Entity
// At the beginning, the Sink Entity registers itself to the IKMS and retrieves communication information for the source Entity.
public class DirectSinkMA extends IKMSEnabledUSREntity implements Application  {

	// Source Entity's rest URI for direct communication is being kept here
	String directURI = null;

	// Basic entity constructor. 
	public DirectSinkMA () {
		// default entityid value
		entityid = 20100;		
	}

	public static void main(String[] args) {
		// Initializing example class, could provide rest callback and IKMS hostnames, ports in the constructor (in case of a distributed deployment)
		// e.g., MA ma = new MA (restHost, restPort, ikmsHost, ikmsPort)
		DirectSinkMA ma = new DirectSinkMA();       

		// initializes and registers entity
		ma.registerEntity();

		// start entity communication
		ma.run();
	}

	public void registerEntity () {
		// Creating registrationInfo data structure
		JSONObject registrationInfo = null;
		try {
			registrationInfo = createRegistrationInfo();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// initializes and registers entity
		initializeAndRegister(registrationInfo);
	}

	protected JSONObject createRegistrationInfo () throws JSONException {
		// creating new EntityRegistartionInformation data structure in JSONObject format
		// an EntityRegistartionInformation object instance with method .toJSONString() could be used instead
		JSONObject registrationInfo = new JSONObject();

		// setting entityid
		registrationInfo.put("entityid", entityid);

		// entityname is being used for visualization purposes
		registrationInfo.put("entityname", "DSINK MA");

		// uris that are required from this entity
		JSONArray requiredArray = new JSONArray();
		requiredArray.put("/BaseStations/Detail/Example1/All");
		registrationInfo.put("urisforrequiredinformation", requiredArray);

		// setting proposed information flow requirements / constraints (see InformationFlowRequirementsAndConstraints class)
		JSONObject informationflowconstraints = new JSONObject();

		// suggesting minimum and maximum information retrieval rates
		informationflowconstraints.put("minimumInformationRetrievalRate", 2);
		informationflowconstraints.put("maximumInformationRetrievalRate", 5);

		// requesting direct Entity2Entity method (should be set, in this case) - see InformationFlowRequirementsAndConstraints class
		informationflowconstraints.put("method", 2);

		// can optionally request a performance goal for direct communication, global goal from Governance component has usually higher priority
		// the outcome of negotiation is being received asynchronously. See IKMSOptimizationGoal & IKMSOptimizationGoals data structures.
		// the same could be set from performanceGoal = IKMSOptimizationGoals.GetDirectEntityGoal().toJSONString()
		JSONObject performanceGoal = new JSONObject();
		performanceGoal.put("optGoalId", 3);
		performanceGoal.put("optGoalName", "Direct Entity");
		performanceGoal.put("optGoalParameters", "");
		performanceGoal.put("optGoalLevelofEnforcement", "high");
		informationflowconstraints.put("flowOptimizationGoal", performanceGoal);

		// setting ikmsClientURL (the address of the know forwarder node) - i.e., for distributed virtual infrastructure deployment
		int ikmsForwarderPort=0;
		if (ikmsForwarderHost!=null) {
			ikmsForwarderPort = 10000 + Integer.valueOf(ikmsForwarderHost);
			informationflowconstraints.put("ikmsClientURL", "http://" + entityHost + ":"+ikmsForwarderPort+"/update/");
		}

		// setting the information flow requirements/constraints to registrationInfo
		registrationInfo.put("informationflowconstraints", informationflowconstraints);

		// setting the callback url (i.e., for information flow negotiation updates - received asynchronously) - using entityid as port
		registrationInfo.put("ifpcallbackURL", "http://" + entityHost + ":" + entityid + "/update/");

		return registrationInfo;
	}

	/**
	 * Initialize with some args
	 */
	@SuppressWarnings("resource")
	public ApplicationResponse init(String[] args) {
		int restOverUSRPort=0;

		if (args.length == 3) {
			// try entityid
			Scanner scanner = new Scanner(args[0]);

			if (scanner.hasNextInt()) {
				entityid = scanner.nextInt();

				scanner = new Scanner(args[1]);

				if (scanner.hasNextInt()) {
					restOverUSRPort = scanner.nextInt();

					scanner = new Scanner(args[2]);

					if (scanner.hasNextInt()) {
						ikmsForwarderHost = String.valueOf(scanner.nextInt());						
					} else {
						scanner.close();
						return new ApplicationResponse(false, "Bad ikmsForwarderHost: " + args[1]);
					}
				} else {
					scanner.close();
					return new ApplicationResponse(false, "Bad restOverUSRPort: " + args[1]);
				}
			} else {
				scanner.close();
				return new ApplicationResponse(false, "Bad entityid: " + args[0]);
			}
			scanner.close();
			// update RestOverUSR port
			restOverUSR.init(restOverUSRPort);
			return new ApplicationResponse(true, "");
		} else {
			return new ApplicationResponse(true, "");
		}
	}

	/**
	 * Start an application.
	 * This is called before run().
	 */
	public ApplicationResponse start() {
		// register entity
		registerEntity ();

		return new ApplicationResponse(true, "");
	}


	/**
	 * Stop an application.
	 * This is called to implement graceful shut down
	 * and cause run() to end.
	 */
	public ApplicationResponse stop() {
		// stop running
		stopRunning=true;

		// shut down rest server
		shutDown();

		return new ApplicationResponse(true, "");
	}


	public void run() {
		
		Logging.Log(entityid, "Waiting for registration to finish.");

		// enforce a delay
		Delay(entityid, 5000);
		
		System.out.println("DSINK MA Information Retrieval Example");
		
		// output of information requests
		JSONObject output = new JSONObject();

		while (stopRunning==false) {
			try {
				// at the beginning, IKMS communicates the source entities direct communication rest URL
				// then the two MAs start communicating directly
				if (directURI==null) {
					output = informationExchangeInterface.RequestInformation(entityid, "/BaseStations/Detail/Example1/All");
					System.out.println ("Retrieved value:"+output.toString());
					if (output.has("url")) {
						directURI=output.getString("url");
						// url provided, switching to direct mode
						output = informationExchangeInterface.RequestDirectInformation(entityid, "/BaseStations/Detail/Example1/All", directURI);
						System.out.println ("Retrieved value:"+output.toString());
					} else {
						System.out.println ("No appropriate information source found:"+output.toString());
					}
				} else {
					output = informationExchangeInterface.RequestDirectInformation(entityid, "/BaseStations/Detail/Example1/All", directURI);
					System.out.println ("Retrieved value:"+output.toString());				
				}
				// enforce a delay
				Delay(entityid, 5000);		

			} catch (IOException e) {
				// Display connection error and exit
				System.out.println ("Connection error. Probably IKMS or Source MA stopped running.");
				System.exit(0);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println ("DSINK MA Stopped Running");
			// stopped running
			running = false;
		}
	}		
}

