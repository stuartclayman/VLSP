package ikms.client;

import ikms.client.utils.Logging;
import ikms.data.EntityRegistrationInformation;
import ikms.data.FlowRegistry;
import ikms.data.IKMSOptimizationGoal.OptimizationRules;
import ikms.data.IKMSOptimizationGoals;
import ikms.data.InformationExchangePolicies;
import ikms.data.InformationFlowRequirementsAndConstraints.Methods;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import demo_usr.ikms.client.utils.Converters;

public class IKMSEnabledEntity implements EntityInterface {
	// The entityid
	protected int entityid;

	// InformationManagementInterface which talks to the IKMS
	protected InformationManagementInterface informationManagementInterface;

	// InformationExchangeInterface which talks to the IKMS
	protected InformationExchangeInterface informationExchangeInterface;

	// the rest listener for the callback facility
	public IKMSClientRestListener restListener;

	// current entity registration information
	protected EntityRegistrationInformation activeEntityRegistration = null;

	// hostname and port for the rest callback facility (used for pub/sub and information exchange policies updates)
	// the enittyPort uses currently the entityid as a port number, if no value is provided
	protected String entityHost="";
	protected int entityPort=0;

	// hostname and port for the IKMS
	protected String ikmsHost="";
	protected int ikmsPort=0;

	// Data structure that keeps local storage (used for pub/sub)
	protected HashMap<String, String> localStorage = new HashMap<String, String>();

	// keeps track of all flow registrations
	protected FlowRegistry flowRegistry = null;

	// if true, compact versions of data structures are used (i.e., relevant optimization goal to save communication overhead/memory storage is set)
	protected boolean compactMode = false;

	// active direct URI (used for direct communication between entities)
	protected String directURI = null;

	// The BasicEntity constructor. Defines the restHost based on the local address 
	public IKMSEnabledEntity () {
		try {
			entityHost = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException uhe) {
			entityHost = "localhost";
		}

		// use the same host as IKMS
		ikmsHost = entityHost;
		// use claydesk1, in case entityHost is in test-bed
		//Logging.Log(entityid, "TRYING TO CONNECT TO IKMS:"+entityHost);
		if (entityHost.startsWith("128.40.39"))
			ikmsHost="128.40.39.166";
		//Logging.Log(entityid, "TRYING TO CONNECT TO IKMS:"+entityHost);

		// use default IKMS port
		ikmsPort = 9900;

		// initialize flow registry
		flowRegistry = new FlowRegistry();
	}

	// The BasicEntity constructor. Defines addresses and ports for the callback facility and IKMS 
	public IKMSEnabledEntity (String entityHost_, int entityPort_, String ikmsHost_, int ikmsPort_) {
		entityHost = entityHost_;
		entityPort = entityPort_;
		ikmsHost = ikmsHost_;
		ikmsPort = ikmsPort_;

		// initialize flow registry
		flowRegistry = new FlowRegistry();
	}

	// Checks compact mode
	public boolean CheckCompactMode () {
		return compactMode;
	}

	public void Shutdown () {
		restListener.stop();
	}

	// Initializes and registers entity with the IKMS
	protected void initializeAndRegister(JSONObject registrationInfo) {

		// if restPort is not set, use entityid as port
		if (entityPort==0)
			entityPort = entityid;

		Logging.Log(entityid, "Running rest listener:"+entityPort);
		// sets up Rest Listener for callbacks (i.e., for information subscribe or information flow negotiation updates)
		restListener = new IKMSClientRestListener(this, entityPort);
		restListener.start();

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

	// Flow that unregisters entity from IKMS
	protected JSONObject UnregisterEntity (int entityid) {
		JSONObject jsobj =  null;
		try {
			jsobj = informationManagementInterface.UnregisterEntity(entityid);
			Logging.Log(entityid, "MA unregistered successfully");

		}  catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (JSONException je) {
			je.printStackTrace();
		}
		return jsobj;
	}

	// Use this method in order to pass measurements to IKMS with every information request. The value should have a timestamp (parameter "ts")
	public JSONObject RequestInformationMeasured (int entityid, String key) throws IOException, JSONException {
		long requestTimeInstance = 0;
		long responseTimeInstance = 0;
		long informationRetrievalResponseTime = 0;
		JSONObject output = null;

		requestTimeInstance = Calendar.getInstance().getTimeInMillis();

		output = informationExchangeInterface.RequestInformation(entityid, key);

		responseTimeInstance = Calendar.getInstance().getTimeInMillis();
		informationRetrievalResponseTime = responseTimeInstance-requestTimeInstance;
		Logging.Log(entityid, "Response time:"+String.valueOf(informationRetrievalResponseTime));

		CommunicateStatistics (entityid, informationRetrievalResponseTime, CalculateFreshness (output));

		return output;
	}

	// Use this method in order to pass measurements to IKMS with every direct information request. The value should have a timestamp (parameter "ts")
	public JSONObject RequestDirectInformationMeasured (int entityid, String key, String uri) throws IOException, JSONException {
		long requestTimeInstance = 0;
		long responseTimeInstance = 0;
		long informationRetrievalResponseTime = 0;
		JSONObject output = null;

		// in case of distributed infrastructure
		// check if iccallbackURL passed (i.e., for distributed virtual infrastructure deployment)
		Map<String, String> keys = Converters.SplitQuery(uri);

		if (keys.containsKey("iccallbackURL")) {
			uri = keys.get("iccallbackURL");
		}

		// checking compact version as well
		if (keys.containsKey("iccbu")) {
			uri = keys.get("iccbu");
		}

		requestTimeInstance = Calendar.getInstance().getTimeInMillis();
		output = informationExchangeInterface.RequestDirectInformation(entityid, key, uri);
		responseTimeInstance = Calendar.getInstance().getTimeInMillis();
		informationRetrievalResponseTime = responseTimeInstance-requestTimeInstance;
		Logging.Log(entityid, "Response time:"+String.valueOf(informationRetrievalResponseTime));

		CommunicateStatistics (entityid, informationRetrievalResponseTime, CalculateFreshness (output));

		return output;
	}

	public JSONObject RequestInformation (String key) {
		InformationExchangePolicies activeInformationExchangePolicies = flowRegistry.GetInformationFlowExchangePolicies(key);

		boolean directEntity = false;
		boolean pubsub = false;
		boolean doNotCommunicateMeasurements = false;

		if (activeInformationExchangePolicies!=null) {

			directEntity = activeInformationExchangePolicies.getFlowOptimizationGoal().CheckOptimizationRule(OptimizationRules.DirectEntity2EntityCommunication);
			pubsub = activeInformationExchangePolicies.getFlowOptimizationGoal().CheckOptimizationRule(OptimizationRules.PubSub);
			doNotCommunicateMeasurements = activeInformationExchangePolicies.getFlowOptimizationGoal().CheckOptimizationRule(OptimizationRules.DoNotCommunicateMeasurements);

			if (activeInformationExchangePolicies.getFlowOptimizationGoal()!=null) {
				Logging.Log(entityid, activeInformationExchangePolicies.getFlowOptimizationGoal().getOptGoalName());
			}
		}
		// request information based on the established information flow policies
		JSONObject result = null;
		if (activeInformationExchangePolicies==null) {
			// no policies set: using pull
			Logging.Log(entityid, "USING PULL");
			try {
				if (doNotCommunicateMeasurements)
					result = informationExchangeInterface.RequestInformation(entityid, key);
				else
					result = RequestInformationMeasured(entityid, key);
			} catch (IOException e) {
				// Display connection error and exit
				Logging.Log(entityid, "Connection error. Probably IKMS or Source Entity stopped running.");
				System.exit(0);;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (pubsub==true||(activeInformationExchangePolicies.getMethod()==Methods.PubSub&&directEntity==false)) {
			// using pub/sub method
			// returning local value
			//result = RetrieveLocalValue (key);
			// DO NOTHING
			Logging.Log(entityid, "USING PUB/SUB (DOING NOTHING)");

			long requestTimeInstance = Calendar.getInstance().getTimeInMillis();

			try {
				result = RetrieveFromLocalStorage (key);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			long responseTimeInstance= Calendar.getInstance().getTimeInMillis();

			long informationRetrievalResponseTime = responseTimeInstance-requestTimeInstance;

			long freshness = CalculateFreshness(result);

			Logging.Log(entityid, "Response time:"+String.valueOf(informationRetrievalResponseTime)+" freshness:"+freshness);

			if (doNotCommunicateMeasurements==false)
				CommunicateStatistics (entityid, informationRetrievalResponseTime, freshness);

			//communicate statistics
		} else if (activeInformationExchangePolicies.getMethod()==Methods.Entity2Entity||directEntity==true) {
			// using direct communication method
			Logging.Log(entityid, "USING Entity2Entity");

			// check if flow is not active any more
			if (activeInformationExchangePolicies.getDestinationEntityId()==-1) {
				// flow with IKMS, reset directURI
				Logging.Log(entityid, "Flow is not active anymore, falling back to IKMS-based communication.");
				directURI = null;
			}

			if (directURI==null) {
				try {
					if (doNotCommunicateMeasurements)
						result = informationExchangeInterface.RequestInformation(entityid, key);
					else
						result = RequestInformationMeasured(entityid, key);
				} catch (IOException e) {
					// Display connection error and exit
					Logging.Log(entityid, "Connection error. Probably IKMS or Source Entity stopped running.");
					System.exit(0);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String tempURI=null;
				try {
					if (result!=null)
						if (result.has("url"))
							tempURI = result.getString("url");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (tempURI!=null) {
					directURI=tempURI;
					// url provided, switching to direct mode
					//System.out.println (test.getString("url"));
					try {
						if (doNotCommunicateMeasurements)
							result = informationExchangeInterface.RequestDirectInformation(entityid, key, directURI);
						else
							result = RequestDirectInformationMeasured(entityid, key, directURI);
					} catch (IOException e) {
						// Display connection error and exit
						Logging.Log(entityid, "Connection error. Probably IKMS or Source Entity stopped running.");
						System.exit(0);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//System.out.println ("Retrieved value:"+test.toString());
				}
			} else {
				try {
					if (doNotCommunicateMeasurements)
						result = informationExchangeInterface.RequestDirectInformation(entityid, key, directURI);
					else
						result = RequestDirectInformationMeasured(entityid, key, directURI);
				} catch (IOException e) {
					// Display connection error and exit
					Logging.Log(entityid, "Connection error. Probably IKMS or Source Entity stopped running.");
					System.exit(0);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//System.out.println ("Retrieved value:"+test.toString());				
			}
		} else {
			// using pull method (default mode)
			Logging.Log(entityid, "USING PULL");

			try {
				if (doNotCommunicateMeasurements)
					result = informationExchangeInterface.RequestInformation(entityid, key);
				else
					result = RequestInformationMeasured(entityid, key);
			} catch (IOException e) {
				// Display connection error and exit
				Logging.Log(entityid, "Connection error. Probably IKMS or Source Entity stopped running.");
				System.exit(0);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return result;
	}

	public void ShareInformation (String key, JSONObject value) {
		// share information based on the established information flow policies
		InformationExchangePolicies activeInformationExchangePolicies = flowRegistry.GetInformationFlowExchangePolicies(key);

		@SuppressWarnings("unused")
		JSONObject result = null;

		boolean directEntity = false;
		boolean pubsub = false;
		boolean pullfromentity=false;

		if (activeInformationExchangePolicies!=null) {
			directEntity = activeInformationExchangePolicies.getFlowOptimizationGoal().CheckOptimizationRule(OptimizationRules.DirectEntity2EntityCommunication);
			pubsub = activeInformationExchangePolicies.getFlowOptimizationGoal().CheckOptimizationRule(OptimizationRules.PubSub);
			pullfromentity = activeInformationExchangePolicies.getFlowOptimizationGoal().CheckOptimizationRule(OptimizationRules.FirstFetchThenRetrieveFromStorage);
		}

		if (activeInformationExchangePolicies==null) {
			// no policies set: using push
			Logging.Log(entityid, "USING PUSH");

			try {
				result = informationExchangeInterface.ShareInformation(entityid, key, value);
			} catch (IOException e) {
				// Display connection error and exit
				Logging.Log(entityid, "Connection error. Probably IKMS or Sink Entity stopped running.");
				System.exit(0);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (pubsub==true||(activeInformationExchangePolicies.getMethod()==Methods.PubSub&&directEntity==false)) {
			// using pub/sub method
			// publishing
			Logging.Log(entityid, "USING PUB/SUB (PUBLISHING)");

			try {
				Logging.Log(entityid, informationExchangeInterface.PublishInformation(entityid, key, value).toString());
			} catch (IOException e) {
				// Display connection error and exit
				Logging.Log(entityid, "Connection error. Probably IKMS or Sink Entity stopped running.");
				System.exit(0);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else if (activeInformationExchangePolicies.getMethod()==Methods.Entity2Entity||directEntity==true) {
			// using direct communication method
			// do nothing
			Logging.Log(entityid, "DIRECT ENTITY2ENTITY");

		} else {
			// using push method (default mode)
			// in case of pullfromentity=true, do nothing
			if (pullfromentity) {
				Logging.Log(entityid, "PULL FROM ENTITY MODE ENABLED, DOING NOTHING");

			} else {
				Logging.Log(entityid, "USING PUSH");

				try {
					result = informationExchangeInterface.ShareInformation(entityid, key, value);
				} catch (IOException e) {
					// Display connection error and exit
					Logging.Log(entityid, "Connection error. Probably IKMS or Sink Entity stopped running.");
					System.exit(0);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	// calculates information freshness (input information should have a timestamp parameter - name "ts")
	public long CalculateFreshness (JSONObject output) {
		// return 0, is object is null
		if (output==null)
			return 0;

		if (output.has("result"))
			try {
				output = output.getJSONObject("result");
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		// in case of compact version of data structure
		if (output.has("r"))
			try {
				output = output.getJSONObject("r");
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		long ts = 0;

		// in case no timestamp exists, returns 0
		if (output.has("ts"))
			try {
				ts = output.getLong("ts");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				ts = 0;
			}
		else 
			return 0;

		long currentTimeInstance = Calendar.getInstance().getTimeInMillis();
		return currentTimeInstance - ts;
	}

	// Method that is called whenever a new information flow policy update arrives (called from rest handler)
	public void InformationFlowPoliciesUpdated (JSONObject informationFlowPolicies) {

		// active information exchange policies (updated asynchronously though the rest callback facility data handler)
		InformationExchangePolicies policies=null;
		try {
			policies = new InformationExchangePolicies (informationFlowPolicies.toString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// add new flow to flowRegistry
		// updating goal with complete values (optimization policies are not
		// included in the goal data structure that is being communicated)
		if (policies!=null)
			if (policies.getFlowOptimizationGoal() != null)
				policies.setFlowOptimizationGoal(IKMSOptimizationGoals
						.GetGoalById(policies.getFlowOptimizationGoal()
								.getOptGoalId()));

		if (policies!=null)
			Logging.Log(entityid, "Information flow policies updated:"+informationFlowPolicies
					+" "+policies.getFlowOptimizationGoal().getOptGoalName());

		// update flow registry
		if (policies!=null)
			flowRegistry.UpdateFlowRegistration(policies);
	}

	// Communicating statistical information for each flow to IKMS. Currently, response time and information freshness are supported.
	public void CommunicateStatistics (int entityid, long responseTime, long freshness) {
		Logging.Log(entityid, "Communicating statistics: rt="+responseTime+" fs="+freshness);
		try {
			informationExchangeInterface.CommunicateStatistics(entityid, new JSONObject (GenerateStatisticsJSON(responseTime, freshness)));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Display connection error and exit
			Logging.Log(entityid, "Connection error. Probably IKMS stopped running.");
			System.exit(0);
		}
	}

	// Convert statistical values to equivalent statistics JSONString to be communicated to IKMS
	private String GenerateStatisticsJSON (long responseTime, long freshness) {
		return "{\"rt\":\""+responseTime+"\",\"fs\":\""+freshness+"\"}";
	}

	// Get current timestamp
	public long GetTimeStamp () {
		Calendar calendar = Calendar.getInstance();
		return calendar.getTimeInMillis();
	}

	// Generate a test value with a timestamp (i.e., for example measurements)
	public JSONObject GenerateTestValue (String uri) {
		JSONObject test = new JSONObject();
		// return compact version, if relevant goal is set
		try {
			if (!compactMode)
				test.put("value", "");

			test.put("ts", GetTimeStamp ());
			//get(Calendar.MINUTE)+":"+calendar.get(Calendar.SECOND));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Logging.Log(entityid, "Generating test value:"+test.toString()+" for uri:"+uri+" compactmode:"+compactMode);

		return test;
	}

	// Store value in local storage (for pub/sub implementations)
	public void StoreInLocalStorage (String uri, String value) {
		Logging.Log(entityid, "Store in local storage:"+value);
		localStorage.put(uri, value);
	}

	// resets local storage
	public void ResetLocalStorage () {
		// resetting local storage
		localStorage.clear();
	}

	// Retrieve value from local storage (for pub/sub implementations)
	public JSONObject RetrieveFromLocalStorage (String uri) throws JSONException {
		String value = localStorage.get(uri);

		JSONObject valueObj = null;

		JSONObject jsobj = new JSONObject();

		if (value==null) {
			jsobj.put("message", "no value pushed yet");
			jsobj.put("output", "");
			jsobj.put("ts", GetTimeStamp());
			return jsobj;
		} else {
			valueObj = new JSONObject(value);
			if (compactMode) {
				if (valueObj.has("ts"))
					jsobj.put("ts", valueObj.getString("ts"));
				if (valueObj.has("value"))
					jsobj.put("o", valueObj.getString("value"));
			} else {
				jsobj.put("message", "last value pushed");
				jsobj.put("output", valueObj.getString("value"));
				if (valueObj.has("ts"))
					jsobj.put("ts", valueObj.getString("ts"));
			}
			return jsobj;
		}
	}


	// Register appropriate registrationInfo with the IKMS. RegistrationInfo should be an appropriate JSONObject that follows
	// the structure of the EntityRegistrationInformation. The latter structure could be used and then converted .toJSONString()
	protected boolean registerWithIKMS(int entityid, JSONObject registrationInfo) {
		try {

			JSONObject jsobj = informationManagementInterface.RegisterEntity(registrationInfo);

			try {
				activeEntityRegistration = new EntityRegistrationInformation (registrationInfo.toString());
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			Logging.Log(entityid, "MA registrationInfo " + registrationInfo);
			Logging.Log(entityid, "MA registration result  " + jsobj);

			if (jsobj.get("output").equals("OK"))
				return true;
			return false;                    
		}  catch (IOException ioe) {
			//ioe.printStackTrace();
		} catch (JSONException je) {
			je.printStackTrace();
		}
		return false;
	}

	// Apply a delay (i.e., for experiments or demo)
	public static void Delay (int entityid, int delayTime) {
		try {
			Logging.Log(entityid, "Waiting "+String.valueOf(delayTime)+" ms");
			Thread.sleep(delayTime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// Apply a delay (i.e., for experiments or demo)
	public static void DelayNoMessage (int delayTime) {
		try {
			Thread.sleep(delayTime);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void InformationFlowPoliciesUpdatedUSR(
			JSONObject informationFlowPolicies, String targetURIFileName) {
		// Information flow policies updated
		System.out.println ("InformationFlowPoliciesUpdateUSR method executed (at IKMSEnabledEntity)");
	}

	@Override
	public JSONObject CollectValue(String uri) {
		// return a generated test value
		return GenerateTestValue (uri);
	}

	@Override
	public JSONObject CollectValueUSR(String uri, String targetURIFileName) {
		// return a generated test value
		return GenerateTestValue (uri);
	}

	@Override
	public void UpdateValue(String uri, String value) {
		// store updated value in local storage
		StoreInLocalStorage (uri, value);
	}

	@Override
	public void UpdateValueUSR(String uri, String value, String ircallbackURL) {
		// store updated value in local storage
		StoreInLocalStorage (uri, value);
	}

}
