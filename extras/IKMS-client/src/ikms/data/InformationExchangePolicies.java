package ikms.data;

import ikms.data.IKMSOptimizationGoal.OptimizationRules;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.json.JSONWriter;

// InformationExchangePolicies class (i.e., the outcome of information flow establishment)
public class InformationExchangePolicies extends InformationFlowRequirementsAndConstraints {

	// Comments for the exchange rate negotiation result
	private String exchangeRateNegotiationRemarks="";

	// Comments for the method negotiation result
	private String methodNegotiationRemarks="";

	// Comments for the optimization goal negotiation result
	private String optimizationGoalNegotiationRemarks="";

	// Decided flow ends' ids
	private int sourceEntityId=-1; 
	private int destinationEntityId=-1; // -1 for IKMS

	// uris this flow is responsible for
	private ArrayList<String> uris=new ArrayList<String>();

	// returns the uris this flow is responsible for
	public ArrayList<String> GetUris () {
		return uris;
	}

	// sets the uris this flow is responsible for
	public void SetUris (ArrayList<String> uris) {
		this.uris=uris;
	}

	// add a new available information uri - if does not exist
	public void AddNewUri (String uri) {
		if (!uris.contains(uri))
			uris.add(uri);
	}

	// Removes information uri and returns number of uris after that
	public int RemoveUri (String uri) {
		if (uris.contains(uri))
			uris.remove(uri);
		
		return uris.size();
	}

	// check if information uri exists
	public boolean CheckUri (String uri) {
		return uris.contains(uri);
	}

	// returns ExchangeRateNegotiationRemarks
	public String getExchangeRateNegotiationRemarks () {
		return exchangeRateNegotiationRemarks;
	}

	// returns MethodNegotiationRemarks
	public String getMethodNegotiationRemarks () {
		return methodNegotiationRemarks;
	}

	// returns OptimizationGoalRemarks
	public String getOptimizationGoalNegotiationRemarks () {
		return optimizationGoalNegotiationRemarks;
	}

	// returns SourceEntityId
	public int getSourceEntityId () {
		return sourceEntityId;
	}

	// returns DestinationEntityId
	public int getDestinationEntityId () {
		return destinationEntityId;
	}

	// sets ExchangeRateNegotiationRemarks
	public void setExchangeRateNegotiationRemarks (String remarks) {
		exchangeRateNegotiationRemarks=remarks;
	}

	// sets MethodNegotiationRemarks
	public void setMethodNegotiationRemarks (String remarks) {
		methodNegotiationRemarks=remarks;
	}

	// sets OptimizationGoalNegotiationRemarks
	public void setOptimizationGoalNegotiationRemarks (String remarks) {
		optimizationGoalNegotiationRemarks=remarks;
	}

	// sets SourceEntityId
	public void setSourceEntityId (int entityid) {
		sourceEntityId=entityid;
	}

	// sets DestinationEntityId
	public void setDestinationEntityId (int entityid) {
		destinationEntityId=entityid;
	}

	// Override toString() Method - Useful for debugging
	@Override
	public String toString() {
		return toJSONString();
	}
	
	// embedd URIs in the information exchange policies - used from the RetrieveAugmentedRegistrationInfos method
	public void EmbeddURIs (ArrayList<String> uris) {
		// should determine if uris are required or provided
		/*for (String uri : uris) {
			if (uri.startsWith("EntityProvidingInfo://")) {
				// available uri
				addAvailableInformationUri (StripURI (uri));
			} else if (uri.startsWith("EntityRequiringInfo://")) {
				//required uri
				addRequiredInformationUri (StripURI (uri));
			} else {
				// unacceptable URI
				System.out.println ("Unacceptable URI."+uri);
				System.exit(0);
			}
		}*/

		// do not differentiate URIs at this level
		for (String uri : uris) {
			AddNewUri (StripURI (uri));
		}
	}

	// Strip URI from unnecessary staff
	private String StripURI (String sourceUri) {
		String result = sourceUri.replace("EntityProvidingInfo://available/","");
		result = result.replace("EntityRequiringInfo://required/","");

		// remove /All suffix
		//if (result.endsWith("/All"))
		//	result = result.substring(0, result.lastIndexOf("/All"));

		return result;
	}

	// create InformationExchangePolicies class from JSONString
	@SuppressWarnings("unchecked")
	public InformationExchangePolicies (String jsonString) throws Exception {		
		// call parent constructor
		super (jsonString);

		try {	
			JSONObject jsObject = new JSONObject(jsonString);

			if (GetJsonObject (jsObject,"exchangeRateNegotiationRemarks") != null) {
				exchangeRateNegotiationRemarks = (String)GetJsonObject (jsObject,"exchangeRateNegotiationRemarks");
			} 

			if (GetJsonObject (jsObject,"methodNegotiationRemarks") != null) {
				methodNegotiationRemarks = (String)GetJsonObject (jsObject,"methodNegotiationRemarks");
			} 

			if (GetJsonObject (jsObject,"optimizationGoalNegotiationRemarks") != null) {
				optimizationGoalNegotiationRemarks = (String)GetJsonObject (jsObject,"optimizationGoalNegotiationRemarks");
			} 

			if (GetJsonObject (jsObject,"sourceEntityId") != null) {
				sourceEntityId = (Integer)GetJsonObject (jsObject,"sourceEntityId");
			} 

			if (GetJsonObject (jsObject,"destinationEntityId") != null) {
				destinationEntityId = (Integer)GetJsonObject (jsObject,"destinationEntityId");
			} 

			// check compact version parameters
			if (GetJsonObject (jsObject,"sid") != null) {
				sourceEntityId = (Integer)GetJsonObject (jsObject,"sid");
			} 

			if (GetJsonObject (jsObject,"did") != null) {
				destinationEntityId = (Integer)GetJsonObject (jsObject,"did");
			} 

			if (GetJsonArray(jsObject,"uris")!=null) {
				uris = JSONArrayToArrayList (GetJsonArray (jsObject,"uris"));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new Exception("Invalid InformationExchangePolicies data structure provided.");
		}
	}

	// Serialize object to JSON String
	public String toJSONString() {

		Writer writer = new StringWriter();
		JSONWriter jsWriter=new JSONWriter(writer);

		try {
			jsWriter.object();

			// prepare regular data structure
			if (getMinimumInformationRetrievalRate()!=-1){
				jsWriter.key("minimumInformationRetrievalRate");
				jsWriter.value(getMinimumInformationRetrievalRate());
			}

			if (getMaximumInformationRetrievalRate()!=-1) {
				jsWriter.key("maximumInformationRetrievalRate");
				jsWriter.value(getMaximumInformationRetrievalRate());
			}

			if (getMinimumInformationSharingRate()!=-1) {
				jsWriter.key("minimumInformationSharingRate");
				jsWriter.value(getMinimumInformationSharingRate());
			}

			if (getMaximumInformationSharingRate()!=-1) {
				jsWriter.key("maximumInformationSharingRate");
				jsWriter.value(getMaximumInformationSharingRate());
			}

			if (getMethodID ()<3) {
				jsWriter.key("method");
				jsWriter.value(getMethodID ());
			}

			if (getIKMSClientURL()!=null) {
				jsWriter.key("ikmsClientURL");
				jsWriter.value(getIKMSClientURL());
			}

			if (getFlowOptimizationGoal()!=null) {
				jsWriter.key("flowOptimizationGoal");
				jsWriter.value(getFlowOptimizationGoal().toJSONString());
			}

			if (!exchangeRateNegotiationRemarks.equals("")) {
				jsWriter.key("exchangeRateNegotiationRemarks");
				jsWriter.value(exchangeRateNegotiationRemarks);				
			}

			if (!methodNegotiationRemarks.equals("")) {
				jsWriter.key("methodNegotiationRemarks");
				jsWriter.value(methodNegotiationRemarks);

			}

			if (!optimizationGoalNegotiationRemarks.equals("")) {
				jsWriter.key("optimizationGoalNegotiationRemarks");
				jsWriter.value(optimizationGoalNegotiationRemarks);
			}


			if (sourceEntityId!=-1) {
				jsWriter.key("sourceEntityId");
				jsWriter.value(sourceEntityId);				
			}

			if (destinationEntityId!=-1) {
				jsWriter.key("destinationEntityId");
				jsWriter.value(destinationEntityId);				
			}

			if (uris.size()>0) {
				jsWriter.key("uris");
				jsWriter.value(uris);
			}

			jsWriter.endObject();

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return String.valueOf(writer);
	}

	// Serialize object to compact JSON String, i.e., to save communication overhead
	public String toCompactJSONString() {

		Writer writer = new StringWriter();
		JSONWriter jsWriter=new JSONWriter(writer);

		try {
			jsWriter.object();

			//if (currentGoal.CheckOptimizationRule(OptimizationRules.LightweightDataStructures)) {
			// prepare lightweight data structure

			if (getMinimumInformationRetrievalRate()!=-1) {
				jsWriter.key("mirr");
				jsWriter.value(getMinimumInformationRetrievalRate());
			}

			if (getMaximumInformationRetrievalRate()!=-1) {
				jsWriter.key("mxirr");
				jsWriter.value(getMaximumInformationRetrievalRate());
			}

			if (getMinimumInformationSharingRate()!=-1) {
				jsWriter.key("misr");
				jsWriter.value(getMinimumInformationSharingRate());
			}

			if (getMaximumInformationSharingRate()!=-1) {
				jsWriter.key("mxisr");
				jsWriter.value(getMaximumInformationSharingRate());
			}

			if (getMethodID ()<3) {
				jsWriter.key("md");
				jsWriter.value(getMethodID ());
			}

			if (getIKMSClientURL()!=null) {
				jsWriter.key("icurl");
				jsWriter.value(getIKMSClientURL());
			}

			if (getFlowOptimizationGoal()!=null) {
				jsWriter.key("fog");
				jsWriter.value(getFlowOptimizationGoal().toCompactJSONString());
			}

			// leaving out all remarks

			if (sourceEntityId!=-1) {
				jsWriter.key("sid");
				jsWriter.value(sourceEntityId);				
			}

			if (destinationEntityId!=-1) {
				jsWriter.key("did");
				jsWriter.value(destinationEntityId);				
			}

			if (uris.size()>0) {
				jsWriter.key("uris");
				jsWriter.value(uris);
			}

			jsWriter.endObject();

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return String.valueOf(writer);
	}

	// Serialize based on goal
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

	// Empty constructor
	public InformationExchangePolicies () {

	}

	// Create InformationExchangePolicies object out from an InformationFlowRequirementsAndConstraints object
	public InformationExchangePolicies (InformationFlowRequirementsAndConstraints constraintsObject) throws Exception {
		super (constraintsObject.toJSONString());
	}
}
