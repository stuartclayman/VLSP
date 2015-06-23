package demo_usr.ikms;

import ikms.client.IKMSClientRestListener;
import ikms.client.IKMSEnabledEntity;
import ikms.client.InformationExchange;
import ikms.client.InformationManagement;
import ikms.client.utils.Logging;
import ikms.data.IKMSOptimizationGoal;

import java.io.IOException;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

// Example Governance component communication with the IKMS. A new global performance goal is being set.
// This triggers re-negotiations of all active flows. The flows receive asynchronously the new information flow policies.
public class GovernanceLocalApplication extends IKMSEnabledEntity {

	// Basic constructor. For a distributed deployment use:
	// Constructor (entityHost_, entityPort_, ikmsHost_, ikmsPort_) 
	public GovernanceLocalApplication () {
		// entityid should be initialized
		entityid = 31001;
		// initialize
		init ();
	}

	public void init() {
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

	public static void main(String[] args) {
		// Initializing example class, could provide rest callback and IKMS hostnames, ports in the constructor (in case of a distributed deployment)
		// e.g., MA ma = new MA (restHost, restPort, ikmsHost, ikmsPort)
		GovernanceLocalApplication gov = new GovernanceLocalApplication();       

		// initialize gov
		gov.init();

		// start entity communication
		gov.start();

		// unregistering gov
		gov.Unregister ();
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

			// The KnowOptimizationGoal data structure example
			// JSONObject performanceGoal = KnowOptimizationGoals.GetDirectEntityGoal().toJSONString();

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

	public void ApplyGoal (IKMSOptimizationGoal goal) {
		System.out.println("Governance Component Global Performance Goal Updating Example");
		try {
			// Defining a new goal. All goals are defined in the KnowOptimizationGoals data structure
			// can either be defined through the IKMSOptimizationGoal data structure or a JSONObject directly

			JSONObject performanceGoal = new JSONObject(goal.toJSONString());

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

	// Override registration method (i.e., to avoid establishing the rest server, we had some issues with stopping it). 
	@Override
	protected void initializeAndRegister(JSONObject registrationInfo) {

		// if restPort is not set, use entityid as port
		if (entityPort==0)
			entityPort = entityid;

		// Allocate InformationManagementInterface & InformationExchangeInterface
		informationManagementInterface = new InformationManagement(ikmsHost, String.valueOf(ikmsPort));
		informationExchangeInterface = new InformationExchange(ikmsHost, String.valueOf(ikmsPort));

		// Entity Registration example
		while (true) {
			// check if can talk to IKMS
			Logging.Log(entityid, "Registering entityid:"+entityid);
			if (registerWithIKMS(entityid, registrationInfo)) {
				Logging.Log(entityid, "Make connection with IKMS");
				break;
			} else {
				Logging.Log(entityid, "Cannot interact with IKMS- retrying after 5000 ms");
				try {
					Thread.sleep(5000);
				} catch (InterruptedException ie) {
					ie.printStackTrace();
				}
			}
		}
	}

	public void Unregister () {
		System.out.println ("Unregistering GOV component (entityid="+entityid+"):"+UnregisterEntity (entityid).toString());
	}
}

