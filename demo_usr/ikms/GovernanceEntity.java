package demo_usr.ikms;

import java.io.IOException;
import java.util.Scanner;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.applications.Application;
import usr.applications.ApplicationResponse;
import demo_usr.ikms.client.IKMSEnabledUSREntity;
import demo_usr.ikms.client.utils.Logging;

// Example Governance component communication with the IKMS. A new global performance goal is being set.
// This triggers re-negotiations of all active flows. The flows receive asynchronously the new information flow policies.
public class GovernanceEntity extends IKMSEnabledUSREntity implements Application {

	// Basic MA constructor. 
	public GovernanceEntity () {
		// default entityid value
		entityid = 10016;
	}

	public static void main(String[] args) {
		// Initializing example Governance component
		GovernanceEntity gov = new GovernanceEntity();       

		// initializes and registers entity
		gov.registerEntity();

		// start entity communication
		gov.run();
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
		// In this example:
		// EntityRegistrationInformation infoSpecifications = new EntityRegistrationInformation (entityid, "GOV", null, null, null, null, null, null, null, null, null, null, ifpCallBackURL, false);
		// registrationInfo = new JSONObject (infoSpecifications.toJSONString());

		JSONObject registrationInfo = new JSONObject();

		// setting entityid
		registrationInfo.put("entityid", entityid);

		// entityname is being used for visualization purposes
		registrationInfo.put("entityname", "GOV");

		// setting the callback url (i.e., for information flow negotiation updates - received asynchronously) - using entityid as port
		registrationInfo.put("ifpcallbackURL", "http://" + entityHost + ":" + entityid + "/update/"); 

		// setting proposed information flow requirements / constraints (see InformationFlowRequirementsAndConstraints class)
		JSONObject informationflowconstraints = new JSONObject();

		// setting ikmsClientURL (the address of the know forwarder node) - i.e., for distributed virtual infrastructure deployment
		int ikmsForwarderPort=0;
		if (ikmsForwarderHost!=null) {
			ikmsForwarderPort = 10000 + Integer.valueOf(ikmsForwarderHost);
			informationflowconstraints.put("ikmsClientURL", "http://" + entityHost + ":"+ikmsForwarderPort+"/update/");
		}

		// setting the information flow requirements/constraints to registrationInfo
		registrationInfo.put("informationflowconstraints", informationflowconstraints);


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
		//stopRunning=true;

		// shut down rest server
		shutDown();

		return new ApplicationResponse(true, "");
	}



	public void run() {			
		Logging.Log(entityid, "Waiting for registration to finish.");

		// enforce a delay
		Delay(entityid, 5000);
		
		Logging.Log(entityid, "Governance Component Global Performance Goal Updating Example");
		
		try {
			// Defining a new goal. All goals are defined in the KnowOptimizationGoals data structure
			// can either be defined through the IKMSOptimizationGoal data structure or a JSONObject directly
			
			// The JSONObject example
			JSONObject performanceGoal = new JSONObject();
			performanceGoal.put("optGoalId", 3);
			performanceGoal.put("optGoalName", "Direct Entity");
			performanceGoal.put("optGoalParameters", "");
			performanceGoal.put("optGoalLevelofEnforcement", "high");
			
			// The KnowOptimizationGoal data structure example
			// JSONObject performanceGoal = KnowOptimizationGoals.GetDirectEntityGoal().toJSONString();
			
			// Updating global performance goal
			Logging.Log(entityid, informationManagementInterface.UpdatePerformanceGoal(entityid, performanceGoal).toString());
		} catch (IOException e) {
			// Display connection error and exit
			Logging.Log(entityid, "Connection error. Probably IKMS stopped running.");
			System.exit(0);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

