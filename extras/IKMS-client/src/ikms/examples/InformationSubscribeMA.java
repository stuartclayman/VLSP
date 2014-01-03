package ikms.examples;

import ikms.client.IKMSEnabledEntity;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

public class InformationSubscribeMA extends IKMSEnabledEntity {

	// Basic constructor. For a distributed deployment use:
	// Constructor (entityHost_, entityPort_, knowHost_, knowPort_) 
	public InformationSubscribeMA () {
		// entityid should be initialized
		entityid = 30000;
	}

	public static void main(String[] args) {
		// Initializing example class, could provide rest callback and IKMS hostnames, ports in the constructor (in case of a distributed deployment)
		// e.g., MA ma = new MA (restHost, restPort, ikmsHost, ikmsPort)
		InformationSubscribeMA ma = new InformationSubscribeMA();       

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
		registrationInfo.put("entityname", "ISUB MA");

		// uris that this entity subscribes to follow
		JSONArray requiredArray = new JSONArray();
		requiredArray.put("/BaseStations/Detail/Example2/All");
		registrationInfo.put("urisforsubscribedinformation", requiredArray);

		// setting proposed information flow requirements / constraints (see InformationFlowRequirementsAndConstraints class)
		JSONObject informationflowconstraints = new JSONObject();

		// requesting pub/sub method (should be set, in this case) - see InformationFlowRequirementsAndConstraints class
		informationflowconstraints.put("method", 1);

		// can optionally request a performance goal for direct communication, global goal from a Governance component has usually higher priority
		// the outcome of negotiation is being received asynchronously. See IKMSOptimizationGoal & IKMSOptimizationGoals data structures.
		// the same could be set from performanceGoal = IKMSOptimizationGoals.GetPubSubGoal().toJSONString()
		JSONObject performanceGoal = new JSONObject();
		performanceGoal.put("optGoalId", 2);
		performanceGoal.put("optGoalName", "Pubsub");
		performanceGoal.put("optGoalParameters", "");
		performanceGoal.put("optGoalLevelofEnforcement", "high");
		informationflowconstraints.put("flowOptimizationGoal", performanceGoal);

		// setting the information flow requirements/constraints to registrationInfo
		registrationInfo.put("informationflowconstraints", informationflowconstraints);

		// specifying the information retrieval callback URL (for pub/sub) - using entityid as port
		String irCallBackURL="http://" + entityHost + ":" + entityid + "/update/";
		registrationInfo.put("ircallbackURL", irCallBackURL);

		// setting the callback url (i.e., for information flow negotiation updates - received asynchronously) - using entityid as port
		registrationInfo.put("ifpcallbackURL", "http://" + entityHost + ":" + entityid + "/update/");

		return registrationInfo;
	}
	
	// Start actual communication
	protected void start() {
		System.out.println("SUB MA Information Subscribe Example");

		// Periodically retrieving information from local storage, which keeps recent information pushed from IKMS
		while (true) {
			try {
				// Retrieve information from local storage
				JSONObject result = RetrieveFromLocalStorage ("/BaseStations/Detail/Example2/All");
				System.out.println (result.toString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Wait 5000 ms
			Delay (entityid, 5000);		
		}		

	}
}

