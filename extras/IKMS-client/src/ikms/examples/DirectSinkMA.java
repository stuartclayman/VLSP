package ikms.examples;

import ikms.client.IKMSEnabledEntity;

import java.io.IOException;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

// Example Sink MA that communicates directly with a Source MA
// At the beginning, the Sink MA registers itself to IKMS and retrieves communication information for the source MA.
public class DirectSinkMA extends IKMSEnabledEntity {

	// Basic constructor. For a distributed deployment use:
	// Constructor (entityHost_, entityPort_, ikmsHost_, ikmsPort_) 
	public DirectSinkMA () {
		// entityid should be initialized
		entityid = 20100;
	}

	public static void main(String[] args) {
		// Initializing example class, could provide rest callback and IKMS hostnames, ports in the constructor (in case of a distributed deployment)
		// e.g., MA ma = new MA (restHost, restPort, ikmsHost, ikmsPort)
		DirectSinkMA ma = new DirectSinkMA();       

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

		// setting the callback url (i.e., for information flow negotiation updates - received asynchronously) - using entityid as port
		registrationInfo.put("ifpcallbackURL", "http://" + entityHost + ":" + entityid + "/update/");

		return registrationInfo;
	}

	// Start actual communication
	protected void start () {

		System.out.println("DSINK MA Information Retrieval Example");

		// output of information requests
		JSONObject output = new JSONObject();

		while (true) {
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
		}
	}
}

