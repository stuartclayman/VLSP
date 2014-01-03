package ikms.examples;

import ikms.client.IKMSEnabledEntity;
import ikms.client.utils.Logging;
import ikms.data.EntityRegistrationInformation;
import ikms.data.IKMSOptimizationGoal;
import ikms.data.IKMSOptimizationGoals;
import ikms.data.InformationFlowRequirementsAndConstraints;

import java.util.ArrayList;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

public class GenericSinkMA extends IKMSEnabledEntity {

	// default initialization values
	int timePeriod = 5000;

	// total time the flow runs (-1 means forever)
	int totalTime = 60000;

	//String uri = "/BaseStations/Detail/All";
	String uri = "/test/All";

	// PushPull: 0; 
	// PubSub: 1; 
	// Entity2Entity: 2; 
	// all: 3; 
	int method = 3;

	int goalId = 0; // compact version of 0 is 4

	private Thread entityThread;

	boolean monitored = false;

	// Basic constructor. For a distributed deployment use:
	// Constructor (entityHost_, entityPort_, ikmsHost_, ikmsPort_) 
	public GenericSinkMA () {		
		// entityid should be initialized
		entityid = 20300;
	}

	// Constructor used for experiments with many flows
	public GenericSinkMA (int entityid_, int timePeriod_, int totalTime_, String uri_, int method_, int goalId_, boolean monitored_) {
		entityid = entityid_;
		timePeriod = timePeriod_;
		totalTime = totalTime_;
		uri = uri_;
		method = method_;
		goalId = goalId_;
		monitored = monitored_;

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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Initializing example class, could provide rest callback and IKMS hostnames, ports in the constructor (in case of a distributed deployment)
		// e.g., MA ma = new MA (restHost, restPort, ikmsHost, ikmsPort)
		GenericSinkMA ma = new GenericSinkMA ();       

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

		// starts actual communication
		ma.start();
	}

	protected JSONObject createRegistrationInfo () throws JSONException {
		// creating new MA registration info data structure in JSONObject format
		// a registrationInfo object instance with method .toJSONString() could be used instead

		// creating array of required uris
		ArrayList<String> requiredArray = new ArrayList<String>();
		requiredArray.add(uri);

		// creating array of subscribed uris
		ArrayList<String> subscribedArray = new ArrayList<String>();
		subscribedArray.add(uri);

		// setting requested flow optimization goal
		IKMSOptimizationGoal goal = IKMSOptimizationGoals.GetGoalById(goalId);

		// setting information flow requirements and constraints
		InformationFlowRequirementsAndConstraints informationFlowConstraints = new InformationFlowRequirementsAndConstraints();
		informationFlowConstraints.setMethodFromID (method);
		informationFlowConstraints.setFlowOptimizationGoal(goal);
		informationFlowConstraints.setMinimumInformationRetrievalRate (2);
		informationFlowConstraints.setMaximumInformationRetrievalRate (5);

		// specifying the information collection callback URL
		String irCallBackURL="http://" + entityHost + ":" + entityid + "/update/";
		// specifying the information flow establishment callback URL
		String ifpCallBackURL="http://" + entityHost + ":" + entityid + "/update/";

		// creating registration info data structure
		EntityRegistrationInformation infoSpecifications = new EntityRegistrationInformation (entityid, null, null, informationFlowConstraints, null, requiredArray, irCallBackURL, subscribedArray, null, null, null, null, ifpCallBackURL, monitored);

		// converting registration info data structure based on requested goal (i.e., compact version or not)
		JSONObject registrationInfo = new JSONObject (infoSpecifications.toJSONString(goal));

		return registrationInfo;
	}

	protected void start () {

		Logging.Log(entityid, "Generic Sink MA Information Retrieval Example");
		//JSONObject test = new JSONObject();

		Runnable entityRun = new Runnable() {
			public void run() {
				while (entityThread!=null&&((totalTime==-1)||(totalTime>0))) {	
					try {
						Logging.Log(entityid, "Retrieved value:"+RequestInformation (uri).toString());
						Logging.Log(entityid, "Waiting "+timePeriod+" ms");
						Thread.sleep(timePeriod);
						totalTime-=timePeriod;
						if (totalTime<0)
							totalTime=0;
					} catch (java.lang.InterruptedException ex) {
						Logging.Log(entityid, "Stopped!");
					}
				}
				Logging.Log(entityid, "Sink Flow Completed (entityid="+entityid+").");
				
				Logging.Log(entityid, "Unregistering Sink Entity (entityid="+entityid+"):"+UnregisterEntity (entityid).toString());
				
				restListener.stop();
				entityThread=null;
			}
		};
		entityThread = new Thread(entityRun);
		entityThread.start();

		/*while (true) {
			test = RequestInformation (uri);
			System.out.println ("Retrieved value:"+test.toString());
			System.out.println ("Waiting "+timePeriod+" ms");
			try {
				Thread.sleep(timePeriod);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
		}*/
	}
}

