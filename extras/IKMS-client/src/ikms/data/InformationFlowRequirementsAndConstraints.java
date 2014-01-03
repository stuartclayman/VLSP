package ikms.data;

import ikms.data.IKMSOptimizationGoal.OptimizationRules;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.json.JSONWriter;

// Information flow requirements & constraints
public class InformationFlowRequirementsAndConstraints {
	// minimum rate for information requirement 
	private int minimumInformationRetrievalRate=-1;
	// maximum rate for information requirement 
	private int maximumInformationRetrievalRate=-1;
	// minimum rate for information available 
	private int minimumInformationSharingRate=-1;
	// maximum rate for information available 
	private int maximumInformationSharingRate=-1;
	//(-1 not relevant)

	// Available communication methods
	public enum Methods {
		PushPull, PubSub, Entity2Entity, All 
	}

	// Information collection/retrieval method available for particular entity
	// 0 push / pull, 1 pub / sub, 2 direct, 3 all
	private Methods method=Methods.All; // default value 3

	// the rest callback URL to the IKMS Client (i.e., for virtual infrastructure experiments)
	private String ikmsClientURL=null;

	// suggested flow optimization goal (IKMS optimization goal may override it)
	private IKMSOptimizationGoal flowOptimizationGoal=null;

	// retrieves method id
	public int getMethodID () {
		return method.ordinal();
		/*		switch (method) {
		case PushPull: return 0; 
		case PubSub: return 1; 
		case Entity2Entity: return 2; 
		default:return 3; 
		} */
	}

	// sets method from method id
	public void setMethodFromID (int methodID) {
		method = Methods.values()[methodID];
		/*		switch (methodID) {
		case 0: method = Methods.PushPull; break; 
		case 1: method = Methods.PubSub; break; 
		case 2: method = Methods.Entity2Entity; break;
		default: method = Methods.All; break;
		} */
	}

	// returns FlowOptimizationGoal
	public IKMSOptimizationGoal getFlowOptimizationGoal () {
		return flowOptimizationGoal;
	}

	// returns MinimumInformationRetrievalRate
	public int getMinimumInformationRetrievalRate () {
		return minimumInformationRetrievalRate;
	}

	// returns MaximumInformationRetrievalRate
	public int getMaximumInformationRetrievalRate () {
		return maximumInformationRetrievalRate;
	}

	// returns MinimumInformationSharingRate
	public int getMinimumInformationSharingRate () {
		return minimumInformationSharingRate;
	}

	// returns MaximumInformationSharingRate
	public int getMaximumInformationSharingRate () {
		return maximumInformationSharingRate;
	}

	// returns communication method
	public Methods getMethod () {
		return method;
	}

	// returns the IKMS client rest URL, i.e., for distributed deployment over the virtual infrastructure
	public String getIKMSClientURL () {
		return ikmsClientURL;
	}

	// sets FlowOptimizationGoal
	public void setFlowOptimizationGoal (IKMSOptimizationGoal flowOptimizationGoal_) {
		flowOptimizationGoal=flowOptimizationGoal_;
	}

	// sets MinimumInformationRetrievalRate
	public void setMinimumInformationRetrievalRate (int minimumInformationRetrievalRate_) {
		minimumInformationRetrievalRate=minimumInformationRetrievalRate_;
	}

	// sets MaximumInformationRetrievalRate
	public void setMaximumInformationRetrievalRate (int maximumInformationRetrievalRate_) {
		maximumInformationRetrievalRate=maximumInformationRetrievalRate_;
	}

	// sets MinimumInformationSharingRate
	public void setMinimumInformationSharingRate (int minimumInfomationSharingRate_) {
		minimumInformationSharingRate=minimumInfomationSharingRate_;
	}

	// sets MaximumInformationSharingRate
	public void setMaximumInformationSharingRate (int maximumInformationSharingRate_) {
		maximumInformationSharingRate=maximumInformationSharingRate_;
	}

	// sets the IKMS client url
	public void setIKMSClientURL (String url) {
		ikmsClientURL = url;
	}

	// sets communication method
	public void setMethod (Methods method_) {
		method=method_;
	}

	// Construct InformationFlowRequirementsAndConstraints object from JSONString
	public InformationFlowRequirementsAndConstraints (String jsonString) throws Exception {
		try {
			JSONObject jsObject = new JSONObject(jsonString);
			if (GetJsonObject (jsObject,"minimumInformationRetrievalRate") != null) {
				minimumInformationRetrievalRate = (Integer)GetJsonObject (jsObject,"minimumInformationRetrievalRate");
			} 

			if (GetJsonObject (jsObject,"maximumInformationRetrievalRate") != null) {
				maximumInformationRetrievalRate = (Integer)GetJsonObject (jsObject,"maximumInformationRetrievalRate");
			} 

			if (GetJsonObject (jsObject,"minimumInformationSharingRate") != null) {
				minimumInformationSharingRate = (Integer)GetJsonObject (jsObject,"minimumInformationSharingRate");
			} 

			if (GetJsonObject (jsObject,"maximumInformationSharingRate") != null) {
				maximumInformationSharingRate = (Integer)GetJsonObject (jsObject,"maximumInformationSharingRate");
			} 

			if (GetJsonObject (jsObject,"method") != null) {
				setMethodFromID ((Integer)GetJsonObject (jsObject,"method"));
			} 

			if (GetJsonObject (jsObject,"ikmsClientURL") != null) {
				ikmsClientURL = (String)GetJsonObject (jsObject,"ikmsClientURL");
			} 

			if (GetJsonObject (jsObject,"flowOptimizationGoal") != null) {
				flowOptimizationGoal = new IKMSOptimizationGoal (GetJsonObject (jsObject,"flowOptimizationGoal").toString());
			} 

			// checking compact versions of fields
			if (GetJsonObject (jsObject,"mirr") != null) {
				minimumInformationRetrievalRate = (Integer)GetJsonObject (jsObject,"mirr");
			} 

			if (GetJsonObject (jsObject,"mxirr") != null) {
				maximumInformationRetrievalRate = (Integer)GetJsonObject (jsObject,"mxirr");
			} 

			if (GetJsonObject (jsObject,"misr") != null) {
				minimumInformationSharingRate = (Integer)GetJsonObject (jsObject,"misr");
			} 

			if (GetJsonObject (jsObject,"mxisr") != null) {
				maximumInformationSharingRate = (Integer)GetJsonObject (jsObject,"mxisr");
			} 

			if (GetJsonObject (jsObject,"md") != null) {
				setMethodFromID ((Integer)GetJsonObject (jsObject,"md"));
			} 

			if (GetJsonObject (jsObject,"icurl") != null) {
				ikmsClientURL = (String)GetJsonObject (jsObject,"icurl");
			} 

			if (GetJsonObject (jsObject,"fog") != null) {
				flowOptimizationGoal = new IKMSOptimizationGoal (GetJsonObject (jsObject,"fog").toString());
			} 

			//else {
			//	throw new Exception("InformationFlowRequirementsAndConstraints no flowOptimizationGoal specified");
			//}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new Exception("Invalid InformationFlowRequirementsAndConstraints data structure provided.");
		}
	}

	// Default constructor
	public InformationFlowRequirementsAndConstraints ()  {
	}

	// Serialize object to JSON String
	public String toJSONString() {

		Writer writer = new StringWriter();
		JSONWriter jsWriter=new JSONWriter(writer);

		try {
			jsWriter.object();

			//if (SelectedOptimizationGoal.GetOptimizationGoal().CheckOptimizationRule(OptimizationRules.LightweightDataStructures)) {

			// creating regular version of data structure
			if (minimumInformationRetrievalRate!=-1) {
				jsWriter.key("minimumInformationRetrievalRate");
				jsWriter.value(minimumInformationRetrievalRate);
			}

			if (maximumInformationRetrievalRate!=-1) {
				jsWriter.key("maximumInformationRetrievalRate");
				jsWriter.value(maximumInformationRetrievalRate);
			}

			if (minimumInformationSharingRate!=-1) {
				jsWriter.key("minimumInformationSharingRate");
				jsWriter.value(minimumInformationSharingRate);
			}

			if (maximumInformationSharingRate!=-1) {
				jsWriter.key("maximumInformationSharingRate");
				jsWriter.value(maximumInformationSharingRate);
			}

			if (getMethodID ()<3) {
				jsWriter.key("method");
				jsWriter.value(getMethodID ());
			}

			if (ikmsClientURL!=null) {
				jsWriter.key("ikmsClientURL");
				jsWriter.value(ikmsClientURL);
			}

			if (flowOptimizationGoal!=null) {
				jsWriter.key("flowOptimizationGoal");
				jsWriter.value(flowOptimizationGoal.toJSONString());
			}

			jsWriter.endObject();

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return String.valueOf(writer);
	}

	// Serialize object to JSON String
	public String toCompactJSONString() {

		Writer writer = new StringWriter();
		JSONWriter jsWriter=new JSONWriter(writer);

		try {
			jsWriter.object();

			//if (SelectedOptimizationGoal.GetOptimizationGoal().CheckOptimizationRule(OptimizationRules.LightweightDataStructures)) {
			// creating compact version of data structure
			if (minimumInformationRetrievalRate!=-1) {
				jsWriter.key("mirr");
				jsWriter.value(minimumInformationRetrievalRate);
			}

			if (maximumInformationRetrievalRate!=-1) {
				jsWriter.key("mxirr");
				jsWriter.value(maximumInformationRetrievalRate);
			}

			if (minimumInformationSharingRate!=-1) {
				jsWriter.key("misr");
				jsWriter.value(minimumInformationSharingRate);
			}

			if (maximumInformationSharingRate!=-1) {
				jsWriter.key("mxisr");
				jsWriter.value(maximumInformationSharingRate);
			}

			int methodId = getMethodID();
			if (methodId<3) {
				jsWriter.key("md");
				jsWriter.value(methodId);
			}

			if (ikmsClientURL!=null) {
				jsWriter.key("icurl");
				jsWriter.value(ikmsClientURL);
			}

			if (flowOptimizationGoal!=null) {
				jsWriter.key("fog");
				jsWriter.value(flowOptimizationGoal.toCompactJSONString());
			}

			jsWriter.endObject();

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return String.valueOf(writer);
	}

	// serialize based on goal
	public String toJSONString (IKMSOptimizationGoal currentGoal) {
		if (currentGoal==null)
			return toJSONString();

		if (currentGoal.GetOptimizationRules()==null) {
			//retrieve full goal (i.e., with embedded optimization rules)
			currentGoal = IKMSOptimizationGoals.GetGoalById(currentGoal.getOptGoalId());
		}

		if (currentGoal.CheckOptimizationRule(OptimizationRules.LightweightDataStructures)) 
			return toCompactJSONString();
		else
			return toJSONString();
	}

	// JSON related support function
	Object GetJsonObject (JSONObject jsObject, String key) throws JSONException{
		return jsObject.opt(key);
	}

	// JSON related support function
	JSONArray GetJsonArray (JSONObject jsObject, String key) throws JSONException{
		return jsObject.optJSONArray(key);
	}

	// JSON related support function
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ArrayList JSONArrayToArrayList (JSONArray jsArray) throws JSONException {
		ArrayList list = new ArrayList<String>();     
		if (jsArray != null) { 
			int len = jsArray.length();
			for (int i=0;i<len;i++){ 
				list.add(jsArray.get(i));
			} 
		} 
		return list;
	}
}
