package ikms.data;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.json.JSONString;
import us.monoid.json.JSONWriter;

// The IKMSOptimizationGoal object
public class IKMSOptimizationGoal implements JSONString {

	// The goal id
	private int optGoalId;
	
	// The goal name
	private String optGoalName;
	
	// The goal parameters
	private String optGoalParameters;
	
	// Available enforcement prioritization levels
	public enum EnforcementLevels {
		Low, Medium, High 
	}

	// Available optimization rules
	public enum OptimizationRules {
		LightweightDataStructures, DoNotStoreWithoutANeed, DirectEntity2EntityCommunication, FirstFetchThenRetrieveFromStorage, MultipleKNOWInstances, PubSub, DoNotCommunicateMeasurements 
	}

	// The optimization rules relevant to goal
	private ArrayList<OptimizationRules> optGoalRules;

	// The enforcement prioritization level of the particular goal
	private EnforcementLevels optGoalLevelofEnforcement;

	// KnowOptimizationGoal object constructor (without optimization rules) - for communication
	public IKMSOptimizationGoal (int optGoalId_, String optGoalName_, String optGoalParameters_, EnforcementLevels optGoalLevelofEnforcement_)
	{
		optGoalId=optGoalId_;
		optGoalName=optGoalName_;
		optGoalParameters=optGoalParameters_;
		optGoalLevelofEnforcement=optGoalLevelofEnforcement_;
		optGoalRules = new ArrayList<OptimizationRules>();
	}

	// IKMSOptimizationGoal object constructor with optimization rules - regular usage
	public IKMSOptimizationGoal (int optGoalId_, String optGoalName_, String optGoalParameters_, EnforcementLevels optGoalLevelofEnforcement_, ArrayList<OptimizationRules> optGoalRules_)
	{
		optGoalId=optGoalId_;
		optGoalName=optGoalName_;
		optGoalParameters=optGoalParameters_;
		optGoalLevelofEnforcement=optGoalLevelofEnforcement_;
		optGoalRules = optGoalRules_;
	}

	// Basic constructor
	public IKMSOptimizationGoal() {
	}

	// Construct object out of a JSON string
	public IKMSOptimizationGoal (String jsonString) throws Exception {
		try {
			JSONObject jsObject = new JSONObject(jsonString);
			if (GetJsonObject (jsObject,"optGoalId") != null) {
				optGoalId = (Integer)GetJsonObject (jsObject,"optGoalId");
			} else {
				if (GetJsonObject (jsObject,"gid")==null)
					throw new Exception("IKMSOptimizationGoal no optGoalId specified");
			}

			if (GetJsonObject (jsObject,"optGoalName") != null) {
				optGoalName = (String)GetJsonObject (jsObject,"optGoalName");
			} else {
				if (GetJsonObject (jsObject,"gn")==null)
					throw new Exception("IKMSOptimizationGoal no optGoalName specified");
			}

			if (GetJsonObject (jsObject,"optGoalParameters") != null)  {
				optGoalParameters=(String)GetJsonObject (jsObject,"optGoalParameters");
			}

			if (GetJsonObject (jsObject,"optGoalLevelofEnforcement") != null) {
				String enforcementLevel=(String)GetJsonObject (jsObject,"optGoalLevelofEnforcement");
				if (enforcementLevel.toUpperCase().equals("HIGH"))
					optGoalLevelofEnforcement=EnforcementLevels.High;
				if (enforcementLevel.toUpperCase().equals("MEDIUM"))
					optGoalLevelofEnforcement=EnforcementLevels.Medium;
				if (enforcementLevel.toUpperCase().equals("LOW"))
					optGoalLevelofEnforcement=EnforcementLevels.Low;
			}

			// check compact version fields, i.e., to save communication overhead
			if (GetJsonObject (jsObject,"gid") != null) {
				optGoalId = (Integer)GetJsonObject (jsObject,"gid");
			}

			if (GetJsonObject (jsObject,"gn") != null) {
				optGoalName = (String)GetJsonObject (jsObject,"gn");
			} 

			if (GetJsonObject (jsObject,"gp") != null) {
				optGoalParameters=(String)GetJsonObject (jsObject,"gp");
			}

			if (GetJsonObject (jsObject,"gel") != null) {
				String enforcementLevel=(String)GetJsonObject (jsObject,"gel");
				if (enforcementLevel.toUpperCase().equals("H"))
					optGoalLevelofEnforcement=EnforcementLevels.High;
				if (enforcementLevel.toUpperCase().equals("M"))
					optGoalLevelofEnforcement=EnforcementLevels.Medium;
				if (enforcementLevel.toUpperCase().equals("L"))
					optGoalLevelofEnforcement=EnforcementLevels.Low;
			}


		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// Add optimization rule to particular goal
	public void AddOptimizationRule (OptimizationRules rule) {
		optGoalRules.add(rule);
	}

	// Remove optimization rule from particular goal
	public void RemoveOptimizationRule (OptimizationRules rule) {
		optGoalRules.remove(rule);
	}

	// Check if particular goal includes the "rule" optimization rule
	public Boolean CheckOptimizationRule (OptimizationRules rule) {		
		if (optGoalRules.contains(rule))
			return true;
		else
			return false;
	}

	// Get all optimization rules assigned to particular goal
	public ArrayList<OptimizationRules> GetOptimizationRules () {
		return optGoalRules;
	}

	// Serialize object to JSON String
	public String toJSONString() {

		Writer writer = new StringWriter();
		JSONWriter jsWriter=new JSONWriter(writer);

		try {
			jsWriter.object();

			//		if (SelectedOptimizationGoal.GetOptimizationGoal().CheckOptimizationRule(OptimizationRules.LightweightDataStructures)) {

			// regular version of data structure
			jsWriter.key("optGoalId");
			jsWriter.value(optGoalId);

			jsWriter.key("optGoalName");
			jsWriter.value(optGoalName);

			jsWriter.key("optGoalParameters");
			jsWriter.value(optGoalParameters);

			if (optGoalLevelofEnforcement==EnforcementLevels.High) {
				jsWriter.key("optGoalLevelofEnforcement");
				jsWriter.value("High");
			}

			if (optGoalLevelofEnforcement==EnforcementLevels.Medium) {
				jsWriter.key("optGoalLevelofEnforcement");
				jsWriter.value("Medium");
			}

			if (optGoalLevelofEnforcement==EnforcementLevels.Low) {
				jsWriter.key("optGoalLevelofEnforcement");
				jsWriter.value("Low");
			}

			jsWriter.endObject();

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return String.valueOf(writer);
	}

	// Serialize object to Compact JSON String, i.e., to save communication overhead
	public String toCompactJSONString() {

		Writer writer = new StringWriter();
		JSONWriter jsWriter=new JSONWriter(writer);

		try {
			jsWriter.object();

			// compact version of data structure
			jsWriter.key("gid");
			jsWriter.value(optGoalId);

			if (optGoalName!=null) {
				jsWriter.key("gn");
				jsWriter.value(optGoalName);
			}

			if (optGoalParameters!=null) {
				jsWriter.key("gp");
				jsWriter.value(optGoalParameters);
			}

			if (optGoalLevelofEnforcement==EnforcementLevels.High) {
				jsWriter.key("gel");
				jsWriter.value("H");
			}

			if (optGoalLevelofEnforcement==EnforcementLevels.Medium) {
				jsWriter.key("gel");
				jsWriter.value("M");
			}

			if (optGoalLevelofEnforcement==EnforcementLevels.Low) {
				jsWriter.key("gel");
				jsWriter.value("L");
			}

			jsWriter.endObject();

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return String.valueOf(writer);
	}

	// returns optimization goal id
	public int getOptGoalId () { 
		return optGoalId;
	}

	// returns optimization goal name
	public String getOptGoalName () {
		return optGoalName;
	}

	// returns optimization goal parameters
	public String getOptGoalParameters () {
		return optGoalParameters;
	}

	// returns level of enforcement of particular goal
	public EnforcementLevels getOptGoalLevelofEnforcement () {
		return optGoalLevelofEnforcement;
	}

	// sets optimization goal parameters
	public void setOptGoalParameters (String parameters) {
		optGoalParameters=parameters;
	}

	// sets level of enforcement
	public void setEnforcementLevel (EnforcementLevels enforcementLevel) {
		optGoalLevelofEnforcement=enforcementLevel;
	}

	// Serialize based on goal object
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

	// JSONObject related support function
	Object GetJsonObject (JSONObject jsObject, String key) throws JSONException{
		return jsObject.opt(key);
	}
}

