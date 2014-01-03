package ikms.interfaces;

import ikms.IKMS;
import ikms.functions.InformationCollectionAndDisseminationFunction;
import ikms.operations.InformationFlowConfigurationAndStatisticsOperation;
import ikms.util.LoggerFrame;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

// This interface is part of IKMS and is responsible for exposing IKMS operations which are related to information 
// exchange to the management entities. 

// List of exposed operations:
// - Information Collection
// - Information Sharing 
// - Information Retrieval
// - Information Dissemination

public class InformationExchangeInterface {
	// The KnowledgeBlock itself
	IKMS ikms;

	// Communicates with InformationCollectionAndDisseminationFunction
	InformationCollectionAndDisseminationFunction informationCollectionAndDissemination = null;

	InformationFlowConfigurationAndStatisticsOperation informationFlowConfigurationAndStatisticsOperation = null;

	// KnowledgeExchangeInterface Constructor
	public InformationExchangeInterface (IKMS ikms) {
		// keep a handle on the KnowledgeBlock
		this.ikms = ikms;

		informationCollectionAndDissemination=ikms.getInformationCollectionAndDisseminationFunction();
		informationFlowConfigurationAndStatisticsOperation = ikms.getInformationFlowEstablishmentAndOptimizationFunction().GetInformationFlowConfigurationAndStatisticsOperation();
	}

	// Request Information value using the Pull Method
	// passes the id of the requesting entity
	public String RequestInformation (int entityid, String uri) {
		// Check if entityid is already registered
		if (LoggerFrame.getActiveEntityName(entityid)==null) {
			// entity is not registered
			System.out.println ("Entity is not registered.");
			return "";
		}

		// Logging message exchange from Entity to IKMS
		@SuppressWarnings("unused")
		Boolean logoutput = LoggerFrame.workflowvisualisationlog(entityid, "", LoggerFrame.IKMSName, "Requesting information: UMFInfoSpecification with uri "+uri, "entities2ikms");				
		// Logging for internal IKMS functions workflow diagram
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.InformationExchangeName, LoggerFrame.ICDFunctionName, "Information Request", "ikmsfunctions");				

		return informationCollectionAndDissemination.RequestInformation(entityid, uri);
	}

	// request information & provide statistical information (i.e., response time)
	@SuppressWarnings("static-access")
	public String RequestInformation (int entityid, String uri, String stats) {
		// updating statistics
		informationFlowConfigurationAndStatisticsOperation.UpdateResponseTime(entityid, Long.valueOf(stats));
		// requesting information
		return RequestInformation (entityid, uri);
	}

	@SuppressWarnings("static-access")
	public void CommunicateStatistics (int entityid, JSONObject stats) {
		// updating statistics
		String responsetime=null;
		String freshness=null;
		try {
			responsetime = stats.getString("rt");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			freshness = stats.getString("fs");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (responsetime!=null)
			informationFlowConfigurationAndStatisticsOperation.UpdateResponseTime(entityid, Long.valueOf(responsetime));
		if (freshness!=null)
			informationFlowConfigurationAndStatisticsOperation.UpdateFreshness(entityid, Long.valueOf(freshness));

	}

	// Request an ArrayList of Information values using the Pull Method
	// passes the id of the requesting Entity
	/*public ArrayList RequestInformationSet (int entityid, ArrayList uris) {
		// Logging message exchange from Entity to IKMS
		Boolean logoutput = LoggerFrame.workflowvisualisationlog(entityid, "", LoggerFrame.IKMSName, "Requesting information with a set of UMFInfoSpecification", "entities2ikms");				
		// Logging for internal Entity functions workflow diagram
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.InformationExchangeName, LoggerFrame.ICDFunctionName, "Information Request", "ikmsfunctions");				

		return informationCollectionAndDissemination.RequestInformationSet(entityid, uris);
	}*/

	// Request to Collect an ArrayList of Information values and to apply an Aggregation Function using the Pull Method
	// passes the id of the requesting Entity
	/*public String RequestAggregatedInformation (int entityid, ArrayList uris, String aggregationFunction) {
		// Logging message exchange from Entity to IKMS
		Boolean logoutput = LoggerFrame.workflowvisualisationlog(entityid, "", LoggerFrame.IKMSName, "Requesting aggregated information", "entities2ikms");				
		// Logging for internal IKMS functions workflow diagram
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.InformationExchangeName, LoggerFrame.ICDFunctionName, "Information Aggregation Request", "ikmsfunctions");				

		return informationCollectionAndDissemination.RequestAggregatedInformation(entityid, uris, aggregationFunction);
	}*/

	// Request to Collect an ArrayList of Information values and to apply a Knowledge Production Algorithm using the Pull Method
	// passes the id of the requesting Entity
	/*public String RequestKnowledgeProduction (int entityid, ArrayList uris, String knowledgeProductionAlgorithm) {
		// Logging message exchange from Entity to IKMS
		Boolean logoutput = LoggerFrame.workflowvisualisationlog(entityid, "", LoggerFrame.IKMSName, "Requesting knowledge production", "entities2ikms");				
		// Logging for internal IKMS functions workflow diagram
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.InformationExchangeName, LoggerFrame.ICDFunctionName, "Knowledge Production Request", "ikmsfunctions");				

		return informationCollectionAndDissemination.RequestKnowledgeProduction(entityid, uris, knowledgeProductionAlgorithm);
	}*/

	// Share an Information value with KNOW
	// passes the id of the Entity sharing the information
	public String ShareInformation (int entityid, String uri, JSONObject value) {
		// Check if entityid is already registered
		if (LoggerFrame.getActiveEntityName(entityid)==null) {
			// entity is not registered
			System.out.println ("Entity is not registered.");
			return "";
		}
		// Logging message exchange from entity to IKMS
		@SuppressWarnings("unused")
		Boolean logoutput = LoggerFrame.workflowvisualisationlog(entityid, "", LoggerFrame.IKMSName, "Sharing information with uri:"+uri+" value "+value, "entities2ikms");				
		// Logging for internal IKMS functions workflow diagram
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.InformationExchangeName, LoggerFrame.ICDFunctionName, "Information Sharing Request", "ikmsfunctions");				
		return informationCollectionAndDissemination.ShareInformation(entityid, uri, value);
	}

	// Publish an Information value to IKMS
	// passes the id of the entity sharing the information
	public long PublishInformation (int entityid, String uri, JSONObject value) {
		// Logging message exchange from entity to IKMS
		@SuppressWarnings("unused")
		Boolean logoutput = LoggerFrame.workflowvisualisationlog(entityid, "", LoggerFrame.IKMSName, "Publishing information with uri:"+uri, "entities2ikms");				
		// Logging for internal IKMS functions workflow diagram
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.InformationExchangeName, LoggerFrame.ICDFunctionName, "Information Publishing Request", "ikmsfunctions");				

		return informationCollectionAndDissemination.PublishInformation(entityid, uri, value);
	}

	// An entity subscribes for information to be sent out at a later date
	public String SubscribeForInformation (int entityid, String cburl, String uri) {
		// Logging message exchange from entity to IKMS
		@SuppressWarnings("unused")
		Boolean logoutput = LoggerFrame.workflowvisualisationlog(entityid, "", LoggerFrame.IKMSName, "Subscribing for information with uri:"+uri, "entities2ikms");				
		// Logging for internal IKMS functions workflow diagram
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.InformationExchangeName, LoggerFrame.ICDFunctionName, "Information Subscription Request", "ikmsfunctions");				

		return informationCollectionAndDissemination.SubscribeForInformation(entityid, cburl, uri);
	}
	
	// Share an ArrayList of Information values with IKMS
	// passes the id of the entity sharing the information
	/*public ArrayList ShareInformationSet (int entityid, ArrayList uris, ArrayList<JSONObject> values) {
		// Logging message exchange from entity to IKMS
		Boolean logoutput = LoggerFrame.workflowvisualisationlog(entityid, "", LoggerFrame.IKMSName, "Sharing information with set of uris", "entities2ikms");				
		// Logging for internal IKMS functions workflow diagram
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.InformationExchangeName, LoggerFrame.ICDFunctionName, "Information Sharing Request", "ikmsfunctions");				

		return informationCollectionAndDissemination.ShareInformationSet(entityid, uris, values);
	}*/

}
