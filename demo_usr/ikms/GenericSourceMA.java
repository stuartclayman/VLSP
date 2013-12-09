package demo_usr.ikms;

import ikms.data.EntityRegistrationInformation;
import ikms.data.IKMSOptimizationGoal;
import ikms.data.IKMSOptimizationGoals;
import ikms.data.InformationFlowRequirementsAndConstraints;

import java.util.ArrayList;
import java.util.Scanner;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.applications.Application;
import usr.applications.ApplicationResponse;
import demo_usr.ikms.client.IKMSEnabledUSREntity;
import demo_usr.ikms.client.utils.Logging;

public class GenericSourceMA extends IKMSEnabledUSREntity implements Application {

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

	int goalId = 0; //compact version of 0 is 4

	boolean monitored = false;

	// Basic MA constructor. 
	public GenericSourceMA () {
		// default entityid value
		entityid = 10310;
	}

	// Constructor used for experiments with many flows
	public GenericSourceMA (int entityid_, int timePeriod_, int totalTime_, String uri_, int method_, int goalId_) {
		entityid = entityid_;
		timePeriod = timePeriod_;
		totalTime = totalTime_;
		uri = uri_;
		method = method_;
		goalId = goalId_;

		// initializes and registers entity
		registerEntity();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Initializing example MA
		GenericSourceMA ma = new GenericSourceMA();       

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

		// creating array of available uris
		ArrayList<String> availableArray = new ArrayList<String>();
		availableArray.add(uri);

		// setting requested flow optimization goal
		IKMSOptimizationGoal goal = IKMSOptimizationGoals.GetGoalById(goalId);

		// setting information flow requirements and constraints
		InformationFlowRequirementsAndConstraints informationFlowConstraints = new InformationFlowRequirementsAndConstraints();
		informationFlowConstraints.setMethodFromID (method);
		informationFlowConstraints.setFlowOptimizationGoal(goal);
		informationFlowConstraints.setMinimumInformationSharingRate(2);
		informationFlowConstraints.setMaximumInformationSharingRate(5);

		// setting ikmsClientURL (the address of the know forwarder node) - i.e., for distributed virtual infrastructure deployment
		int ikmsForwarderPort=0;
		if (ikmsForwarderHost!=null) {
			ikmsForwarderPort = 10000 + Integer.valueOf(ikmsForwarderHost);
			informationFlowConstraints.setIKMSClientURL("http://" + entityHost + ":"+ikmsForwarderPort+"/update/");
		}

		// specifying the information collection callback URL
		String icCallBackURL="http://" + entityHost + ":" + entityid + "/update/";
		// specifying the information flow establishment callback URL
		String ifpCallBackURL="http://" + entityHost + ":" + entityid + "/update/";

		// creating registration info data structure
		EntityRegistrationInformation infoSpecifications = new EntityRegistrationInformation (entityid, null, availableArray, informationFlowConstraints, icCallBackURL, null, null, null, null, null, null, null, ifpCallBackURL, monitored);

		// converting registration info data structure based on requested goal (i.e., compact version or not)
		JSONObject registrationInfo = new JSONObject (infoSpecifications.toJSONString(goal));

		return registrationInfo;
	}

	/**
	 * Initialize with some args
	 */
	@SuppressWarnings("resource")
	public ApplicationResponse init(String[] args) {
		if (args.length == 2) {
			// try entityid
			Scanner scanner = new Scanner(args[0]);

			if (scanner.hasNextInt()) {
				entityid = scanner.nextInt();

				scanner = new Scanner(args[1]);

				if (scanner.hasNextInt()) {
					ikmsForwarderHost = String.valueOf(scanner.nextInt());
				} else {
					scanner.close();
					return new ApplicationResponse(false, "Bad knowHost " + args[1]);
				}
			} else {
				scanner.close();
				return new ApplicationResponse(false, "Bad entityid " + args[0]);
			}
			scanner.close();
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

		Logging.Log(entityid, "Generic Source MA Information Sharing Example");
		//JSONObject test = null;
		//int timeElapsed = totalTime;

		while (stopRunning==false&&((totalTime==-1)||(totalTime>0))) {	
			try {
				ShareInformation(uri, GenerateTestValue(uri));
				Logging.Log(entityid, "Waiting "+timePeriod+" ms");
				Thread.sleep(timePeriod);
				totalTime-=timePeriod;
				if (totalTime<0)
					totalTime=0;
			} catch (java.lang.InterruptedException ex) {
				Logging.Log(entityid, "Stopped!");
			}
		}
		Logging.Log(entityid, "Source Flow Completed (entityid="+entityid+").");
		Logging.Log(entityid, "Generic Source MA Stopped Running");
		// stopped running
		running = false;
	}
}
