package ikms.examples;

import ikms.client.IKMSEnabledEntity;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

// Example Source MA that communicates with a Sink MA directly 
// At the beginning, the Source MA registers itself to IKMS and waits for information retrieval requests from the source MA.
public class DirectSourceMA extends IKMSEnabledEntity {

	// Basic constructor. For a distributed deployment use:
	// Constructor (entityHost_, entityPort_, ikmsHost_, ikmsPort_) 
	public DirectSourceMA () {
		// entityid should be initialized
		entityid = 10210;
	}

	public static void main(String[] args) {
		// Initializing example class, could provide rest callback and IKMS hostnames, ports in the constructor (in case of a distributed deployment)
		// e.g., MA ma = new MA (restHost, restPort, ikmsHost, ikmsPort)
		DirectSourceMA ma = new DirectSourceMA();       

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
		registrationInfo.put("entityname", "DS MA");

		// uris that are available from this entity
		JSONArray availableArray = new JSONArray();
		availableArray.put("/BaseStations/Detail/Example1/All");
		registrationInfo.put("urisforavailableinformation", availableArray);

		// setting proposed information flow requirements / constraints (see InformationFlowRequirementsAndConstraints class)
		JSONObject informationflowconstraints = new JSONObject();

		// suggesting minimum and maximum information sharing rates
		informationflowconstraints.put("minimumInformationSharingRate", 2);
		informationflowconstraints.put("maximumInformationSharingRate", 5);

		// requesting direct Entity2Entity method (should be set, in this case) - see InformationFlowRequirementsAndConstraints class
		informationflowconstraints.put("method", 2);

		// can optionally request a performance goal for direct communication, global goal from a Governance component has usually higher priority
		// the outcome of negotiation is being received asynchronously. See IKMSOptimizationGoal & IKMSOptimizationGoals data structures.
		// the same could be set from performanceGoal = IKMSOptimizationGoals.GetDirectEntityGoal().toJSONString()
		JSONObject performanceGoal = new JSONObject();
		performanceGoal.put("optGoalId", 3);
		performanceGoal.put("optGoalName", "Direct Entity");
		performanceGoal.put("optGoalParameters", "");
		performanceGoal.put("optGoalLevelofEnforcement", "high");
		informationflowconstraints.put("flowOptimizationGoal", performanceGoal);

		// setting the information flow requirements/constraints to registrationInfo
		registrationInfo.put("informationflowconstraints", informationflowconstraints);

		// specifying the information collection callback URL (the sink Entity communicates directly through this URL) - using entityid as port
		String icCallBackURL="http://" + entityHost + ":" + entityid + "/update/";
		registrationInfo.put("iccallbackURL", icCallBackURL);

		// setting the callback url (i.e., for information flow negotiation updates - received asynchronously) - using entityid as port
		registrationInfo.put("ifpcallbackURL", "http://" + entityHost + ":" + entityid + "/update/");

		return registrationInfo;
	}

	// Start actual communication
	protected void start() {
		System.out.println("DS MA Information Retrieval Example");

		// just wait for direct requests from DSINK MA
	}

}


