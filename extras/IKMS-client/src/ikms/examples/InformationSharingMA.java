package ikms.examples;

import ikms.client.IKMSEnabledEntity;

import java.io.IOException;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

//Example Source MA that shares periodically information with the MA (Push/Pull example)
public class InformationSharingMA extends IKMSEnabledEntity {

	// Basic constructor. For a distributed deployment use:
	// Constructor (entityHost_, entityPort_, ikmsHost_, ikmsPort_) 
	public InformationSharingMA () {
		// entityid should be initialized
		entityid = 10000;
	}

	public static void main(String[] args) {
		// Initializing example class, could provide rest callback and IKMS hostnames, ports in the constructor (in case of a distributed deployment)
		// e.g., MA ma = new MA (restHost, restPort, ikmsHost, ikmsPort)
		InformationSharingMA ma = new InformationSharingMA();       

		// Creating registrationInfo data structure
		JSONObject registrationInfo = null;
		try {
			registrationInfo = ma.createRegistrationInfo();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// initializes and registers entity
		ma.initializeAndRegister(registrationInfo);

		// start entity communication
		ma.start();
	}

	protected JSONObject createRegistrationInfo () throws JSONException {
		// creating new MA registration info data structure in JSONObject format
		// a registrationInfo object instance with method .toJSONString() could be used instead
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

		// can optionally request a performance goal for direct communication, global goal from a Governance component has usually higher priority
		// the outcome of negotiation is being received asynchronously. See IKMSOptimizationGoal & IKMSOptimizationGoals data structures.
		// PULL FROM ENTITY GOAL: IKMS Pulls information from the MA Source, if the latter is not available it checks the IKMS storage
		// the same could be obtained from: 
		// performanceGoal = KnowOptimizationGoals.GetPullFromEntityGoal().toJSONString()
		/*JSONObject performanceGoal = new JSONObject();
		performanceGoal.put("optGoalId", 0);
		performanceGoal.put("optGoalName", "Pull from Entity");
		performanceGoal.put("optGoalParameters", "");
		performanceGoal.put("optGoalLevelofEnforcement", "high");
		informationflowconstraints.put("flowOptimizationGoal", performanceGoal);*/

		// PULL FROM Storage GOAL: IKMS retrieves information from the IKMS storage, if it is not available it pulls it from the information source
		// the same could be obtained from: 
		// performanceGoal = KnowOptimizationGoals.GetPullFromStorageGoal().toJSONString()
		JSONObject performanceGoal = new JSONObject();
		performanceGoal.put("optGoalId", 1);
		performanceGoal.put("optGoalName", "Pull from Storage");
		performanceGoal.put("optGoalParameters", "");
		performanceGoal.put("optGoalLevelofEnforcement", "high");
		informationflowconstraints.put("flowOptimizationGoal", performanceGoal);

		// setting the information flow requirements/constraints to registrationInfo
		registrationInfo.put("informationflowconstraints", informationflowconstraints);

		// specifying the information collection callback URL - using entityid as port
		String icCallBackURL="http://" + entityHost + ":" + entityid + "/update/";
		registrationInfo.put("iccallbackURL", icCallBackURL);

		// setting the callback url (i.e., for information flow negotiation updates - received asynchronously) - using entityid as port
		registrationInfo.put("ifpcallbackURL", "http://" + entityHost + ":" + entityid + "/update/");

		return registrationInfo;
	}

	// Start actual communication
	protected void start() {
		System.out.println("IS MA Information Sharing Example");

		// Periodically sharing test information
		while (true) {
			try {
				// Generate and share test information
				JSONObject test = new JSONObject();
				test.put("value", GenerateTestValue("/BaseStations/Detail/Example3/All"));
				System.out.println (informationExchangeInterface.ShareInformation(entityid, "/BaseStations/Detail/All", test));
			} catch (IOException e) {
				// Display connection error and exit
				System.out.println ("Connection error. Probably IKMS stopped running.");
				System.exit(0);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Wait 5000 ms
			Delay (entityid, 5000);
		}
	}

}

