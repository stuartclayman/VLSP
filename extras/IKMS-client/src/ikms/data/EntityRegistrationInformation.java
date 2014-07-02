package ikms.data;

import ikms.data.IKMSOptimizationGoal.OptimizationRules;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.json.JSONString;
import us.monoid.json.JSONWriter;

// Entity registration information regarding the IKMS
// This information is being stored in the Entity Registration Storage of the IKMS

public class EntityRegistrationInformation implements JSONString {

	// the entity id
	int entityid=0;

	// Entity's short name 
	String entityname="";

	// this information is collected from IKMS
	// uris of available information
	ArrayList<String> urisforavailableinformation=new ArrayList<String>();
	// the callback URL for the information collection
	String iccallbackURL="";

	// this information can be retrieved from the IKMS
	// uris of required information
	ArrayList<String> urisforrequiredinformation=new ArrayList<String>();
	// information flow QoS constraints for information available or required
	InformationFlowRequirementsAndConstraints informationflowconstraints=null;
	// the callback URL for the information retrieval
	String ircallbackURL="";

	// the callback URL for the updated information flow policies (i.e., the outcome of information flow negotiations)
	String ifpcallbackURL="";

	// this information can be subscribed from the IKMS
	// uris of subscribed information
	ArrayList<String> urisforsubscribedinformation =new ArrayList<String>();

	// information processing and knowledge production availability
	// here we register the Entities information processing & knowledge production capabilities
	// uris of knowledge (or processed information) that can be produced - the key values of the indexing storage
	ArrayList<String> urisforknowledge=new ArrayList<String>();
	// urls of equivalent knowledgebuildingrequests
	ArrayList<String> knowledgebuildingrequesturls=new ArrayList<String>();

	// start knowledge production on registration (one boolean per uri) - if false, knowledge starts being built on retrieval of information
	ArrayList<Boolean> knowledgeproductiononregistration=new ArrayList<Boolean>();

	// the callback URL for the information processing and knowledge production
	String ipkpcallbackURL="";

	// whether to take specialized measurements for this entity or not
	boolean monitor=false;

	// Register an Entity with everything besides monitoring parameter
	public EntityRegistrationInformation (int entityid_, String entityname_, ArrayList<String> urisforavailableinformation_, InformationFlowRequirementsAndConstraints informationflowconstraints_, String iccallbackURL_, ArrayList<String> urisforrequiredinformation_, String ircallbackURL_, ArrayList<String> urisforsubscribedinformation_, ArrayList<String> urisforknowledge_, ArrayList<String> knowledgebuildingrequesturls_, ArrayList<Boolean> knowledgeproductiononregistration_, String ipkpcallbackURL_, String ifpcallbackURL_) {
		entityid=entityid_;
		if (entityname_!=null||entityname.equals(""))
			entityname=entityname_;

		if (urisforavailableinformation_!=null)
			urisforavailableinformation=urisforavailableinformation_;
		if (iccallbackURL_!=null)
			iccallbackURL=iccallbackURL_;
		if (urisforrequiredinformation_!=null)
			urisforrequiredinformation=urisforrequiredinformation_;
		if (informationflowconstraints_!=null)
			informationflowconstraints=informationflowconstraints_;
		if (ircallbackURL_!=null)
			ircallbackURL=ircallbackURL_;
		if (urisforsubscribedinformation_!=null)
			urisforsubscribedinformation=urisforsubscribedinformation_;
		if (urisforknowledge_!=null)
			urisforknowledge=urisforknowledge_;
		if (knowledgebuildingrequesturls_!=null)
			knowledgebuildingrequesturls=knowledgebuildingrequesturls_;
		if (knowledgeproductiononregistration_!=null)		
			knowledgeproductiononregistration=knowledgeproductiononregistration_;
		if (ipkpcallbackURL_!=null)
			ipkpcallbackURL=ipkpcallbackURL_;
		if (ifpcallbackURL_!=null)
			ifpcallbackURL=ifpcallbackURL_;		
	}

	// Register an Entity with everything, including monitor parameter
	public EntityRegistrationInformation (int entityid_, String entityname_, ArrayList<String> urisforavailableinformation_, InformationFlowRequirementsAndConstraints informationflowconstraints_, String iccallbackURL_, ArrayList<String> urisforrequiredinformation_, String ircallbackURL_, ArrayList<String> urisforsubscribedinformation_, ArrayList<String> urisforknowledge_, ArrayList<String> knowledgebuildingrequesturls_, ArrayList<Boolean> knowledgeproductiononregistration_, String ipkpcallbackURL_, String ifpcallbackURL_, boolean monitor_) {
		this (entityid_, entityname_, urisforavailableinformation_, informationflowconstraints_, iccallbackURL_, urisforrequiredinformation_, ircallbackURL_, urisforsubscribedinformation_, urisforknowledge_, knowledgebuildingrequesturls_, knowledgeproductiononregistration_, ipkpcallbackURL_, ifpcallbackURL_);

		monitor=monitor_;
	}

	// Register an entity with everything using a JSON string
	public EntityRegistrationInformation (String jsonString) throws Exception {
		try {
			JSONObject jsObject = new JSONObject(jsonString);

			if (GetJsonObject (jsObject,"entityid")!=null) 
				entityid = (Integer)GetJsonObject (jsObject,"entityid");
			else 
				if (GetJsonObject (jsObject,"entityid")!=null)  // for backward compatibility
					entityid = (Integer)GetJsonObject (jsObject,"entityid");
				else 
					if (GetJsonObject (jsObject,"eid")==null)
						throw new Exception("EntityRegistrationInformation: no entityid specified");

			if (GetJsonObject (jsObject,"entityname")!=null) {
				entityname = (String)GetJsonObject (jsObject,"entityname");
			} else {
				if (GetJsonObject (jsObject,"en")==null)
					//throw new Exception("UMFInformationSpecifications no entityname specified (use GOV, COORD for core services)");
					entityname = "ENTITY"+entityid;
			}

			if (GetJsonArray(jsObject,"urisforavailableinformation")!=null) 
				urisforavailableinformation = JSONArrayToStringArrayList (GetJsonArray (jsObject,"urisforavailableinformation"));

			if (GetJsonObject (jsObject,"iccallbackURL")!=null) 
				iccallbackURL=(String)GetJsonObject (jsObject,"iccallbackURL");

			if (GetJsonArray (jsObject, "urisforrequiredinformation")!=null)
				urisforrequiredinformation = JSONArrayToStringArrayList (GetJsonArray (jsObject, "urisforrequiredinformation"));

			if (GetJsonObject (jsObject, "informationflowconstraints")!=null) {
				informationflowconstraints = new InformationFlowRequirementsAndConstraints (GetJsonObject (jsObject, "informationflowconstraints").toString());
			}

			if (GetJsonObject (jsObject,"ircallbackURL")!=null)
				ircallbackURL=(String)GetJsonObject (jsObject,"ircallbackURL");

			if (GetJsonArray (jsObject, "urisforsubscribedinformation")!=null)
				urisforsubscribedinformation = JSONArrayToStringArrayList (GetJsonArray (jsObject, "urisforsubscribedinformation"));

			if (GetJsonArray (jsObject, "urisforknowledge")!=null)
				urisforknowledge = JSONArrayToStringArrayList (GetJsonArray (jsObject, "urisforknowledge"));

			if (GetJsonArray (jsObject,"knowledgebuildingrequesturls")!=null)
				knowledgebuildingrequesturls = JSONArrayToStringArrayList (GetJsonArray (jsObject,"knowledgebuildingrequesturls"));			

			if (GetJsonArray (jsObject,"knowledgeproductiononregistration")!=null)
				knowledgeproductiononregistration = JSONArrayToBooleanArrayList (GetJsonArray (jsObject,"knowledgeproductiononregistration"));

			if (GetJsonObject (jsObject,"ipkpcallbackURL")!=null)
				ipkpcallbackURL=(String)GetJsonObject (jsObject,"ipkpcallbackURL");

			if (GetJsonObject (jsObject,"ifpcallbackURL")!=null)
				ifpcallbackURL=(String)GetJsonObject (jsObject,"ifpcallbackURL");

			if (GetJsonObject (jsObject,"monitor")!=null) {
				if ((Integer)GetJsonObject (jsObject,"monitor")==1)
					monitor=true;
				else
					monitor=false;
			}

			// checking compact version of parameters
			if (GetJsonObject (jsObject,"eid")!=null) {
				entityid = (Integer)GetJsonObject (jsObject,"eid");
			} 

			if (GetJsonObject (jsObject,"en")!=null) {
				entityname = (String)GetJsonObject (jsObject,"en");
			} 

			if (GetJsonArray(jsObject,"ufai")!=null)
				urisforavailableinformation = JSONArrayToStringArrayList (GetJsonArray (jsObject,"ufai"));

			if (GetJsonObject (jsObject,"iccbu")!=null)
				iccallbackURL=(String)GetJsonObject (jsObject,"iccbu");

			if (GetJsonArray (jsObject, "ufri")!=null)
				urisforrequiredinformation = JSONArrayToStringArrayList (GetJsonArray (jsObject, "ufri"));

			if (GetJsonObject (jsObject, "ifc")!=null) {
				informationflowconstraints = new InformationFlowRequirementsAndConstraints (GetJsonObject (jsObject, "ifc").toString());
			}

			if (GetJsonObject (jsObject,"ircbu")!=null)
				ircallbackURL=(String)GetJsonObject (jsObject,"ircbu");

			if (GetJsonArray (jsObject, "ufsi")!=null)
				urisforsubscribedinformation = JSONArrayToStringArrayList (GetJsonArray (jsObject, "ufsi"));

			if (GetJsonArray (jsObject, "ufk")!=null)
				urisforknowledge = JSONArrayToStringArrayList (GetJsonArray (jsObject, "ufk"));			

			if (GetJsonArray (jsObject,"kbru")!=null)
				knowledgebuildingrequesturls = JSONArrayToStringArrayList (GetJsonArray (jsObject,"kbru"));			

			if (GetJsonArray (jsObject,"kpor")!=null)
				knowledgeproductiononregistration = JSONArrayToBooleanArrayList (GetJsonArray (jsObject,"kpor"));

			if (GetJsonObject (jsObject,"ipkpcbu")!=null)
				ipkpcallbackURL=(String)GetJsonObject (jsObject,"ipkpcbu");

			if (GetJsonObject (jsObject,"ifpcbu")!=null)
				ifpcallbackURL=(String)GetJsonObject (jsObject,"ifpcbu");

			if (GetJsonObject (jsObject,"m")!=null) {
				if ((Integer)GetJsonObject (jsObject,"m")==1)
					monitor=true;
				else
					monitor=false;
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new Exception("Invalid Entity Information Specifications data structure provided.");
		}
	}

	// Serialize object to JSON String
	public String toJSONString() {

		Writer writer = new StringWriter();
		JSONWriter jsWriter=new JSONWriter(writer);

		try {
			jsWriter.object();

			// if (SelectedOptimizationGoal.GetOptimizationGoal().CheckOptimizationRule(OptimizationRules.LightweightDataStructures)) {

			// regular version of data structure

			jsWriter.key("entityid");
			jsWriter.value(entityid);

			if (entityname!=null) {
				jsWriter.key("entityname");
				jsWriter.value(entityname);
			}

			if (urisforavailableinformation.size()>0) {
				jsWriter.key("urisforavailableinformation");
				jsWriter.value(urisforavailableinformation);
			}

			if (iccallbackURL!=null) {
				jsWriter.key("iccallbackURL");
				jsWriter.value(iccallbackURL);
			}

			if (urisforrequiredinformation.size()>0) {
				jsWriter.key("urisforrequiredinformation");
				jsWriter.value(urisforrequiredinformation);
			}

			if (informationflowconstraints!=null) {
				jsWriter.key("informationflowconstraints");
				jsWriter.value(informationflowconstraints.toJSONString());
			}

			if (ircallbackURL!=null) {
				jsWriter.key("ircallbackURL");
				jsWriter.value(ircallbackURL);
			}

			if (urisforsubscribedinformation.size()>0) {
				jsWriter.key("urisforsubscribedinformation");
				jsWriter.value(urisforsubscribedinformation);
			}

			if (urisforknowledge.size()>0) {
				jsWriter.key("urisforknowledge");
				jsWriter.value(urisforknowledge);
			}

			if (knowledgebuildingrequesturls.size()>0) {
				jsWriter.key("knowledgebuildingrequesturls");
				jsWriter.value(knowledgebuildingrequesturls);
			}

			if (knowledgeproductiononregistration.size()>0) {
				jsWriter.key("knowledgeproductiononregistration");
				jsWriter.value(knowledgeproductiononregistration);
			}

			if (ipkpcallbackURL!=null) {
				jsWriter.key("ipkpcallbackURL");
				jsWriter.value(ipkpcallbackURL);
			}

			if (ifpcallbackURL!=null) {
				jsWriter.key("ifpcallbackURL");
				jsWriter.value(ifpcallbackURL);
			}

			if (monitor) {
				jsWriter.key("monitor");
				jsWriter.value(1);		
			}
			//}
			jsWriter.endObject();

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return String.valueOf(writer);
	}

	// Serialize object to JSON String (compact version to save overhead)
	public String toCompactJSONString() {

		Writer writer = new StringWriter();
		JSONWriter jsWriter=new JSONWriter(writer);

		try {
			jsWriter.object();

			// compact version of data structure
			jsWriter.key("eid");
			jsWriter.value(entityid);

			if (entityname!=null) {
				jsWriter.key("en");
				jsWriter.value(entityname);
			}

			if (urisforavailableinformation.size()>0) {
				jsWriter.key("ufai");
				jsWriter.value(urisforavailableinformation);
			}

			if (!iccallbackURL.equals("")) {
				jsWriter.key("iccbu");
				jsWriter.value(iccallbackURL);
			}

			if (urisforrequiredinformation.size()>0) {
				jsWriter.key("ufri");
				jsWriter.value(urisforrequiredinformation);
			}

			if (informationflowconstraints!=null) {
				jsWriter.key("ifc");
				jsWriter.value(informationflowconstraints.toCompactJSONString());
			}

			if (!ircallbackURL.equals("")) {
				jsWriter.key("ircbu");
				jsWriter.value(ircallbackURL);
			}

			if (urisforsubscribedinformation.size()>0) {
				jsWriter.key("ufsi");
				jsWriter.value(urisforsubscribedinformation);
			}

			if (urisforknowledge.size()>0) {
				jsWriter.key("ufk");
				jsWriter.value(urisforknowledge);
			}

			if (knowledgebuildingrequesturls.size()>0) {
				jsWriter.key("kbru");
				jsWriter.value(knowledgebuildingrequesturls);
			}

			if (knowledgeproductiononregistration.size()>0) {
				jsWriter.key("kpor");
				jsWriter.value(knowledgeproductiononregistration);
			}

			if (!ipkpcallbackURL.equals("")) {
				jsWriter.key("ipkpcbu");
				jsWriter.value(ipkpcallbackURL);
			}

			if (!ifpcallbackURL.equals("")) {
				jsWriter.key("ifpcbu");
				jsWriter.value(ifpcallbackURL);
			}

			if (monitor) {
				jsWriter.key("m");
				jsWriter.value(1);		
			}

			jsWriter.endObject();

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return String.valueOf(writer);
	}

	// Returns entityid of current instance
	public int GetEntityId () {
		return entityid;
	}

	// Returns entityname of current instance
	public String GetEntityName () {
		return entityname;
	}

	// Returns urisforavailableinformation of current instance
	public ArrayList<String> GetUrisForAvailableInformation () {
		return urisforavailableinformation;
	}

	// Returns iccallbackURL of current instance
	public String GetIcCallbackURL () {
		return iccallbackURL;
	}

	// Sets iccallbackURL of current instance
	public void SetIcCallbackURL (String iccallbackURL_) {
		iccallbackURL=iccallbackURL_;
	}

	// Returns urisforrequiredinformation of current instance
	public ArrayList<String> GetUrisForRequiredInformation () {
		return urisforrequiredinformation;
	}

	// Returns informationflowconstraints of current instance
	public InformationFlowRequirementsAndConstraints GetInformationFlowConstraints () {
		return informationflowconstraints;
	}

	// Embedd informationflowconstraints in current instance
	public void EmbeddInformationFlowConstraints (InformationFlowRequirementsAndConstraints constraints) {
		informationflowconstraints = constraints;
	}

	// Returns ircallbackURL of current instance
	public String GetIrCallbackURL () {
		return ircallbackURL;
	}

	// Sets ircallbackURL of current instance
	public void SetIrCallbackURL (String ircallbackURL_) {
		ircallbackURL = ircallbackURL_;
	}

	// Returns urisforsubscribedinformation of current instance
	public ArrayList<String> GetUrisForSubscribedInformation () {
		return urisforsubscribedinformation;
	}

	// Returns urisforknowledge of current instance
	public ArrayList<String> GetUrisForKnowledge () {
		return urisforknowledge;
	}

	// Returns knowledgebuildingrequesturls of current instance
	public ArrayList<String> GetKnowledgeBuildingRequestURLs () {
		return knowledgebuildingrequesturls;
	}

	// Returns knowledgeproductiononregistration of current instance
	public ArrayList<Boolean> GetKnowledgeProductionOnRegistration () {
		return knowledgeproductiononregistration;
	}

	// Returns ipkpcallbackURL of current instance
	public String GetIPKPCallBackURL () {
		return ipkpcallbackURL;
	}

	// Returns ifpcallbackURL of current instance
	public String GetIFCCallBackURL() {
		// in case of a distributed virtual infrastructure deployment
		// return a 
		return ifpcallbackURL;
	}

	// Sets ipkpcallbackURL of current instance
	public void SetIPKPCallBackURL (String ipkpcallbackURL_) {
		ipkpcallbackURL = ipkpcallbackURL_;
	}

	// Sets ifpcallbackURL of current instance
	public void SetIFCCallBackURL(String ifpcallbackURL_) {
		// in case of a distributed virtual infrastructure deployment
		// return a 
		ifpcallbackURL = ifpcallbackURL_;
	}

	// Returns ifpcallbackURL or knowClientURL of current instance, i.e., in case of a distributed virtual infrastructure deployment
	public String GetNextNodeCallBackURL() {
		if (informationflowconstraints==null) {
			return ifpcallbackURL;
		} else {
			if (informationflowconstraints.getIKMSClientURL()==null) {
				return ifpcallbackURL;
			} else {
				try {
					return informationflowconstraints.getIKMSClientURL()+"?ifpcallbackURL="+URLEncoder.encode(ifpcallbackURL, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return null;
				}
			}
		}
	}

	// Returns iccallbackURL or ikmsClientURL of current instance, i.e., in case of a distributed virtual infrastructure deployment
	public String GetNextNodeICCallBackURL() {
		if (informationflowconstraints==null) {
			return iccallbackURL;
		} else {
			if (informationflowconstraints.getIKMSClientURL()==null) {
				return iccallbackURL;
			} else {
				try {
					return informationflowconstraints.getIKMSClientURL()+"?iccallbackURL="+URLEncoder.encode(iccallbackURL, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return null;
				}
			}
		}
	}

	// Returns ircallbackURL or ikmsClientURL of current instance, i.e., in case of a distributed virtual infrastructure deployment
	public String GetNextNodeIRCallBackURL() {
		if (informationflowconstraints==null) {
			return ircallbackURL;
		} else {
			if (informationflowconstraints.getIKMSClientURL()==null) {
				return ircallbackURL;
			} else {
				try {
					return informationflowconstraints.getIKMSClientURL()+"?ircallbackURL="+URLEncoder.encode(ircallbackURL, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return null;
				}
			}
		}
	}

	// Check if entity is being monitored
	public boolean CheckIfMonitored () {
		return monitor;
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
	public ArrayList<String> JSONArrayToStringArrayList (JSONArray jsArray) throws JSONException {
		ArrayList<String> list = new ArrayList<String>();     
		if (jsArray != null) { 
			int len = jsArray.length();
			for (int i=0;i<len;i++){ 
				list.add((String) jsArray.get(i));
			} 
		} 
		return list;
	}

	public ArrayList<Boolean> JSONArrayToBooleanArrayList (JSONArray jsArray) throws JSONException {
		ArrayList<Boolean> list = new ArrayList<Boolean>();     
		if (jsArray != null) { 
			int len = jsArray.length();
			for (int i=0;i<len;i++){ 
				list.add((Boolean) jsArray.get(i));
			} 
		} 
		return list;
	}

	// JSON related support function
	Object GetJsonObject (JSONObject jsObject, String key) throws JSONException{
		return jsObject.opt(key);
	}

	// JSON related support function
	JSONArray GetJsonArray (JSONObject jsObject, String key) throws JSONException{
		return jsObject.optJSONArray(key);
	}
}
