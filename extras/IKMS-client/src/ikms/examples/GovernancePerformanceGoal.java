package ikms.examples;

import ikms.client.IKMSEnabledEntity;
import ikms.data.IKMSOptimizationGoals;

import java.io.IOException;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

// Example Governance component communication with the IKMS. A new global performance goal is being set.
// This triggers re-negotiations of all active flows. The flows receive asynchronously the new information flow policies.
public class GovernancePerformanceGoal extends IKMSEnabledEntity {

	// Basic constructor. For a distributed deployment use:
	// Constructor (entityHost_, entityPort_, ikmsHost_, ikmsPort_) 
	public GovernancePerformanceGoal () {
		// entityid should be initialized
		entityid = 10016;
	}

	public static void main(String[] args) {
		// Initializing example class, could provide rest callback and IKMS hostnames, ports in the constructor (in case of a distributed deployment)
		// e.g., MA ma = new MA (restHost, restPort, ikmsHost, ikmsPort)
		GovernancePerformanceGoal gov = new GovernancePerformanceGoal();       

		// Creating registrationInfo data structure
		JSONObject registrationInfo = null;
		try {
			registrationInfo = gov.createRegistrationInfo();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// initializes and registers entity
		gov.initializeAndRegister(registrationInfo);

		// start entity communication
		gov.start();
	}

	protected JSONObject createRegistrationInfo () throws JSONException {
		// creating new MA registration info data structure in JSONObject format
		// a registrationInfo object instance with method .toJSONString() could be used instead
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
		
		return registrationInfo;
	}
	
	// Start actual communication
	protected void start() {			
		System.out.println("Governance Component Global Performance Goal Updating Example");
		try {
			// Defining a new goal. All goals are defined in the KnowOptimizationGoals data structure
			// can either be defined through the IKMSOptimizationGoal data structure or a JSONObject directly
			
			// The JSONObject example
			JSONObject performanceGoal = new JSONObject();
			performanceGoal.put("optGoalId", 3);
			performanceGoal.put("optGoalName", "Direct Entity");
			performanceGoal.put("optGoalParameters", "");
			performanceGoal.put("optGoalLevelofEnforcement", "high");
			
			//JSONObject performanceGoal = new JSONObject();
			//performanceGoal.put("optGoalId", 2);
			//performanceGoal.put("optGoalName", "Pull from Entity");
			//performanceGoal.put("optGoalParameters", "");
			//performanceGoal.put("optGoalLevelofEnforcement", "high");

			// The KnowOptimizationGoal data structure example
			//JSONObject performanceGoal = IKMSOptimizationGoals.GetPullFromEntityGoal().toJSONString();
			
			// Updating global performance goal
			System.out.println (informationManagementInterface.UpdatePerformanceGoal(entityid, performanceGoal));
		} catch (IOException e) {
			// Display connection error and exit
			e.printStackTrace();
			System.out.println ("Connection error. Probably IKMS stopped running.");
			System.exit(0);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
	}

}

