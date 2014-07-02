package ikms.interfaces;

import ikms.IKMS;
import ikms.data.EntityRegistrationInformation;
import ikms.data.IKMSOptimizationGoal;
import ikms.data.InformationExchangePolicies;
import ikms.operations.EntityRegistrationOperation;
import ikms.operations.InformationFlowConfigurationAndStatisticsOperation;
import ikms.operations.InformationIndexingOperation;
import ikms.operations.InformationQualityControllerOperation;
import ikms.operations.InformationStorageOperation;
import ikms.util.LoggerFrame;

import java.util.ArrayList;

// This interface is part of IKMS and exposes to the other management entities those IKMS operations 
// that are related to management issues of IKMS. For example, policies, aggregation mechanisms to be used, optimisation 
// goals for the IKMS, and the configuration of the IKMS properties (e.g., to change the information flow optimisation 
// policies, to add new general accuracy objectives for the information filtering etc) can be passed to IKMS via this interface. 

// List of exposed operations:
// - Entity registration
// - Information quality controller

public class InformationManagementInterface {
	// The IKMS itself
	IKMS ikms;

	// Communicating operations
	EntityRegistrationOperation entityRegistrationOperation = null;
	InformationIndexingOperation informationIndexingOperation = null;
	InformationStorageOperation informationStorageOperation = null;
	InformationQualityControllerOperation informationQualityControllerOperation = null;
	InformationFlowConfigurationAndStatisticsOperation informationFlowConfigurationAndStatisticsOperation = null;

	public InformationManagementInterface (IKMS ikms) {
		// keep a handle on the IKMS
		this.ikms = ikms;

		entityRegistrationOperation = ikms.getInformationStorageAndIndexingFunction().entityRegistration;
		informationIndexingOperation = ikms.getInformationStorageAndIndexingFunction().informationIndexing;
		informationStorageOperation = ikms.getInformationStorageAndIndexingFunction().informationStorage;
		informationQualityControllerOperation = ikms.getInformationFlowEstablishmentAndOptimizationFunction().GetInformationQualityControllerOperation();
		informationFlowConfigurationAndStatisticsOperation = ikms.getInformationFlowEstablishmentAndOptimizationFunction().GetInformationFlowConfigurationAndStatisticsOperation();
	}

	public String RegisterEntity (EntityRegistrationInformation entityRegistrationinfo, boolean renegotiationMode) {
		int entityid = entityRegistrationinfo.GetEntityId();
		String entityname = entityRegistrationinfo.GetEntityName();
		InformationExchangePolicies negotiationResult=new InformationExchangePolicies ();

		try {
			// Register new active entity
			LoggerFrame.registerActiveEntity(entityid, entityname);
			// Enable monitoring, if requested
			if (entityRegistrationinfo.CheckIfMonitored())
				InformationFlowConfigurationAndStatisticsOperation.MonitorEntity(entityid);

			// Logging message exchange from entity to IKMS
			@SuppressWarnings("unused")
			Boolean logoutput = null;

			if (renegotiationMode) {
				LoggerFrame.workflowvisualisationlog(entityid, "", LoggerFrame.IKMSName, "Triggering Re-negotiation", "entities2ikms");				
			} else {
				LoggerFrame.workflowvisualisationlog(entityid, "", LoggerFrame.IKMSName, "Registering Entity to IKMS", "entities2ikms");				
			}

			// Logging for internal IKMS functions workflow diagram
			logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.InformationManagementName, LoggerFrame.ICDFunctionName, "Received Entity Registration Information", "ikmsfunctions");				

			// Logging for internal IKMS functions workflow diagram
			//logoutput = LoggerFrame.testlog(LoggerFrame.ICDFunctionName, LoggerFrame.ISIENTITYStorageName, "Checking if entity is pre-registered", "black");	
			logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Checking if entity is already registered", "ikmsfunctions");	

			if (entityRegistrationOperation.CheckIfEntityIsRegistered(entityid)) {
				//logoutput = LoggerFrame.testlog(LoggerFrame.ICDFunctionName, LoggerFrame.ISIENTITYStorageName, "Checking if entity is pre-registered", "black");	
				logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Entity is registered. Updating registration.", "ikmsfunctions");				
			}

			String output=null;
			// Registering the entity to Entity Registration Storage
			output = entityRegistrationOperation.StoreEntityRegistrationInfo (entityRegistrationinfo);

			if (output!=null) {
				// Logging for internal IKMS functions workflow diagram
				//logoutput = LoggerFrame.testlog(LoggerFrame.ICDFunctionName, LoggerFrame.ISIENTITYStorageName, "Storing Entity Instance Description", "black");	
				logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Indexing EntityRegistrationInformation", "ikmsfunctions");	

				// Logging for internal IKMS functions workflow diagram
				logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.IFEOName, "Resolving Information/Knowledge Dependencies", "ikmsfunctions");	

				//logging information flow negotiation workflow
				logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.IFEOName, "Initiating Information Flow Negotiation", "ifeofunction");	

				// Establish information flow
				informationQualityControllerOperation.TriggerInformationFlowEstablishment(entityRegistrationinfo);

				// Indexing information availability 
				String indexingoutput = informationIndexingOperation.IndexArrayList(entityRegistrationinfo.GetEntityId(), entityRegistrationinfo.GetUrisForAvailableInformation(), entityRegistrationinfo.GetNextNodeICCallBackURL()); 
				if (indexingoutput!=null) {
					// Logging for internal IKMS functions workflow diagram
					//logoutput = LoggerFrame.testlog(LoggerFrame.ICDFunctionName, LoggerFrame.ISIIndexingStorageName, "Indexing Information Availability", "black");	
					logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Indexing available uris", "ikmsfunctions");	
				}
				// Subscribe for relevant information
				if (entityRegistrationinfo.GetUrisForSubscribedInformation() != null &&
						entityRegistrationinfo.GetUrisForSubscribedInformation().size() > 0) {

					// Logging for internal IKMS functions workflow diagram
					//logoutput = LoggerFrame.testlog(LoggerFrame.ISIENTITYStorageName, LoggerFrame.ICDFunctionName, "Subscribing for external inputs", "black");	
					logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ISIName, LoggerFrame.ICDFunctionName, "Subscribing for external inputs", "ikmsfunctions");	

					informationStorageOperation.SubscribeForAnInformationSet(entityid, entityRegistrationinfo.GetNextNodeIRCallBackURL(), entityRegistrationinfo.GetUrisForSubscribedInformation());
				}

				// Logging for internal IKMS functions workflow diagram
				logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationManagementName, "Entity registered to IKMS successfully", "ikmsfunctions");	
				// Logging for internal IKMS functions workflow diagram
				logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "Entity registered to IKMS successfully", "entities2ikms");	
			} else {

				// Logging for internal IKMS functions workflow diagram
				logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationManagementName, "Entity cannot be registered to IKMS", "ikmsfunctions");	
				// Logging for internal IKMS functions workflow diagram
				logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "Entity cannot be registered to IKMS", "entities2ikms");				
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}

		return negotiationResult.toJSONString();
	}

	public String UnregisterEntity (int entityid) {		
		try {
			// get registration info for entity
			String entityRegistrationStr = entityRegistrationOperation.GetEntityRegistrationInfoFromStorage(entityid);
			EntityRegistrationInformation entityRegistration;
			try {
				entityRegistration = new EntityRegistrationInformation (entityRegistrationStr);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();

				LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationManagementName, "Entity unregistered from IKMS unsuccessfully", "ikmsfunctions");	
				LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "Entity unregistered from IKMS unsuccessfully", "entities2ikms");	

				return "entity unregistration failed.";
			}

			LoggerFrame.workflowvisualisationlog(entityid, "", LoggerFrame.IKMSName, "Unregistering Entity from IKMS", "entities2ikms");				

			LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.InformationManagementName, LoggerFrame.ICDFunctionName, "Received registration removal request", "ikmsfunctions");				

			LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Checking if entity is registered", "ikmsfunctions");	

			if (entityRegistrationOperation.CheckIfEntityIsRegistered(entityid)) {
				LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ISIName, LoggerFrame.ICDFunctionName, "Entity is registered. Proceeding unregistration.", "ikmsfunctions");				
			}

			entityRegistrationOperation.RemoveEntityRegistrationInfo(entityRegistration);

			LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Removing Entity Information indices", "ikmsfunctions");	

			informationIndexingOperation.RemoveIndexArrayList(entityid, entityRegistration.GetUrisForAvailableInformation()); 

			LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Removing Subscriptions for external inputs", "ikmsfunctions");	
			ArrayList<String> urisToRemove = entityRegistration.GetUrisForSubscribedInformation();
			urisToRemove.addAll(entityRegistration.GetUrisForRequiredInformation());
			informationStorageOperation.UnsubscribeForAnInformationSet(entityid, urisToRemove);

			LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.IFEOName, "Resolving Information/Knowledge Dependencies", "ikmsfunctions");	

			// get relevant flows to the entity
			ArrayList<InformationExchangePolicies> relevantFlows = InformationFlowConfigurationAndStatisticsOperation.GetRelevantInformationFlows (entityid);

			//System.out.println ("Relevant flows:"+relevantFlows);

			// remove flows from IKMS, including updating IKMS UI
			InformationFlowConfigurationAndStatisticsOperation.UnRegisterFlows (entityid);

			// trigger re-negotiations, if needed
			//System.out.println ("Checking if re-negotiation is needed.");
			for (InformationExchangePolicies policies : relevantFlows) {
				// get id of other end of flow
				int otherEntityid;
				if (policies.getSourceEntityId()==entityid)
					otherEntityid=policies.getDestinationEntityId();
				else
					otherEntityid=policies.getSourceEntityId();

				//System.out.println ("id of other flow:"+otherEntityid);


				// the other end should re-negotiate itself (if it exists)
				if (otherEntityid!=-1) {
					String otherEndRegistration = entityRegistrationOperation.GetEntityRegistrationInfoFromStorage(otherEntityid);
					try {
						RegisterEntity (new EntityRegistrationInformation(otherEndRegistration), true);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();

						LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationManagementName, "Entity unregistered from IKMS unsuccessfully", "ikmsfunctions");	
						LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "Entity unregistered from IKMS unsuccessfully", "entities2ikms");	

						return "Renegotiation failed.";
					}
				}
			}
			// remove entity from knowlogger
			LoggerFrame.unregisterActiveEntity (entityid);

			LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationManagementName, "Entity unregistered from IKMS successfully", "ikmsfunctions");	
			LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "Entity unregistered from IKMS successfully", "entities2ikms");	
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return "OK";
	}

	public String GetEntityRegistrationInfo (int entityid) {
		@SuppressWarnings("unused")
		Boolean logoutput = LoggerFrame.workflowvisualisationlog(entityid, "", LoggerFrame.IKMSName, "Requesting Entity registration info from IKMS", "entities2ikms");
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.InformationManagementName, LoggerFrame.ICDFunctionName, "Received entity registration info request", "ikmsfunctions");				
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Checking if entity is registered", "ikmsfunctions");	
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Retrieving registration information","ikmsfunctions");	
		LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "registration information retrieved from IKMS", "entities2ikms");

		return entityRegistrationOperation.GetEntityRegistrationInfoFromStorage(entityid);
	}

	/**
	 * New global performance optimization goal received from GOV
	 * Provides a JSONObject which contains the performance goal details.
	 * @return The result of the REST call as a JSONObject
	 */
	public String UpdatePerformanceGoal(int entityid, IKMSOptimizationGoal knowOptimizationGoal) {
		System.out.println ("Updating Global performance optimization goal");
		@SuppressWarnings("unused")
		Boolean logoutput = LoggerFrame.workflowvisualisationlog(entityid, "", LoggerFrame.IKMSName, "Updating global optimization goal", "entities2ikms");
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.InformationManagementName, LoggerFrame.ICDFunctionName, "Received global optimization goal update request", "ikmsfunctions");				
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.IQCName, "Checking if request comes from a governance component", "ikmsfunctions");	
		if (LoggerFrame.getActiveEntityName(entityid).equals("GOV")) {
			logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.IQCName, "Updating global optimization goal","ikmsfunctions");	
			LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "Global optimization goal updated", "entities2ikms");
			informationQualityControllerOperation.SetGlobalPerformanceOptimizationGoal (knowOptimizationGoal);
			return "Goal set succesfully";
		} else {
			logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.IQCName, "Global optimization goal update request should come from GOV","ikmsfunctions");	
			LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "Global optimization goal update request should come from GOV", "entities2ikms");			
			return "Goal set unsuccesfully, only GOV can update global goal";
		}
	}

	/**
	 * New global performance optimization goal received from GOV
	 * Provides a JSONObject which contains the performance goal details.
	 * @return The result of the REST call as a JSONObject
	 */
	public IKMSOptimizationGoal GetPerformanceGoal(int entityid) {
		System.out.println ("Retrieving Global performance optimization goal");
		return informationQualityControllerOperation.GetGlobalPerformanceOptimizationGoal();
	}
}
