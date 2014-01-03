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

//Example Source MA that shares periodically information with the IKMS (Push/Pull example)
public class InformationSharingMA extends IKMSEnabledUSREntity implements Application {

	// Basic constructor. 
	public InformationSharingMA () {
		// default entityid
		entityid = 10001;
	}

	public static void main(String[] args) {
		// Initializing example MA
		InformationSharingMA ma = new InformationSharingMA();       

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
		// creating new EntityRegistrationInformation data structure in JSONObject format
		// an EntityRegistrationInformation object instance with method .toJSONString() could be used instead
		JSONObject registrationInfo = new JSONObject();

		// setting entityid
		registrationInfo.put("entityid", entityid);

		// entityname is being used for visualization purposes
		registrationInfo.put("entityname", "IS MA");

		// uris that are available from this entity
		JSONArray availableArray = new JSONArray();
		availableArray.put("/BaseStations/Detail/Example3/All");
		registrationInfo.put("urisforavailableinformation", availableArray);

		// setting proposed information flow requirements / constraints (see InformationFlowRequirementsAndConstraints class)
		JSONObject informationflowconstraints = new JSONObject();

		// can optionally request a performance goal for direct communication, global goal from Governance component has usually higher priority
		// the outcome of negotiation is being received asynchronously. See IKMSOptimizationGoal & IKMSOptimizationGoals data structures.
		// PULL FROM ENTITY GOAL: IKMS Pulls information from the MA Source, if the latter is not available it checks the IKMS storage
		// the same could be obtained from: 
		// performanceGoal = IKMSOptimizationGoals.GetPullFromEntityGoal().toJSONString()
		/*JSONObject performanceGoal = new JSONObject();
		performanceGoal.put("optGoalId", 0);
		performanceGoal.put("optGoalName", "Pull from Entity");
		performanceGoal.put("optGoalParameters", "");
		performanceGoal.put("optGoalLevelofEnforcement", "high");
		informationflowconstraints.put("flowOptimizationGoal", performanceGoal);*/

		// PULL FROM Storage GOAL: IKMS retrieves information from the IKMS storage, if it is not available it pulls it from the information source
		// the same could be obtained from: 
		// performanceGoal = IKMSOptimizationGoals.GetPullFromStorageGoal().toJSONString()
		JSONObject performanceGoal = new JSONObject();
		performanceGoal.put("optGoalId", 1);
		performanceGoal.put("optGoalName", "Pull from Storage");
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

		// specifying the information collection callback URL - using entityid as port
		String icCallBackURL="http://" + entityHost + ":" + entityid + "/update/";
		registrationInfo.put("iccallbackURL", icCallBackURL);

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
		
		Logging.Log(entityid, "IS MA Information Sharing Example");

		// Periodically sharing test information
		while (stopRunning==false) {
			try {
				// Generate and share test information
				JSONObject test = new JSONObject();
				test.put("value", GenerateTestValue("/BaseStations/Detail/Example3/All"));
				Logging.Log(entityid, informationExchangeInterface.ShareInformation(entityid, "/BaseStations/Detail/All", test).toString());
			} catch (IOException e) {
				// Display connection error and exit
				Logging.Log(entityid, "Connection error. Probably IKMS stopped running.");
				System.exit(0);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Wait 5000 ms
			Delay (entityid, 5000);
		}
		Logging.Log(entityid, "IS MA Stopped Running");
		// stopped running
		running = false;
	}

}

