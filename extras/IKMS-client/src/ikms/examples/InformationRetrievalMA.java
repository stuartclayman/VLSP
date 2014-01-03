package ikms.examples;

import ikms.client.IKMSEnabledEntity;

import java.io.IOException;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

//Example Sink Entity that retrieves periodically information from IKMS (Push/Pull example)
public class InformationRetrievalMA extends IKMSEnabledEntity {


	// Basic constructor. For a distributed deployment use:
	// Constructor (entityHost_, entityPort_, knowHost_, knowPort_) 
	public InformationRetrievalMA () {
		// entityid should be initialized
		entityid = 20000;
	}

	public static void main(String[] args) {
		// Initializing example class, could provide rest callback and IKMS hostnames, ports in the constructor (in case of a distributed deployment)
		// e.g., MA ma = new MA (restHost, restPort, ikmsHost, ikmsPort)
		InformationRetrievalMA ma = new InformationRetrievalMA();       

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
		registrationInfo.put("entityname", "IR MA");

		// uris that are required from this entity
		JSONArray requiredArray = new JSONArray();
		requiredArray.put("/BaseStations/Detail/Example3/All");
		registrationInfo.put("urisforrequiredinformation", requiredArray);

		// setting proposed information flow requirements / constraints (see InformationFlowRequirementsAndConstraints class)
		JSONObject informationflowconstraints = new JSONObject();

		// can optionally request a performance goal for direct communication, global goal from a Governance component has usually higher priority
		// the outcome of negotiation is being received asynchronously. See IKMSOptimizationGoal & IKMSOptimizationGoals data structures.
		// PULL FROM ENTITY GOAL: IKMS Pulls information from the MA Source, if the latter is not available it checks the IKMS storage
		// the same gould be obtained from: 
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

		// setting the information flow requirements/constraints to registrationInfo
		registrationInfo.put("informationflowconstraints", informationflowconstraints);

		// setting the callback url (i.e., for information flow negotiation updates - received asynchronously) - using entityid as port
		registrationInfo.put("ifpcallbackURL", "http://" + entityHost + ":" + entityid + "/update/");

		return registrationInfo;
	}

	// Start actual communication
	protected void start () {

		System.out.println("IS MA Information Retrieval Example");
		JSONObject test = new JSONObject();

		// Periodically requesting test information
		while (true) {
			try {	
				// request test information
				test = informationExchangeInterface.RequestInformation(entityid, "/BaseStations/Detail/Example3/All");
				System.out.println ("Retrieved value:"+test.toString());
				System.out.println ("Waiting 5s");
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