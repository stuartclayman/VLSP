package ikms.operations;

import static us.monoid.web.Resty.content;
import ikms.data.EntityRegistrationInformation;
import ikms.data.IKMSOptimizationGoal;
import ikms.data.IKMSOptimizationGoals;
import ikms.data.InformationExchangePolicies;
import ikms.data.InformationFlowRequirementsAndConstraints;
import ikms.data.InformationFlowRequirementsAndConstraints.Methods;
import ikms.info.SelectedOptimizationGoal;
import ikms.util.LoggerFrame;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;

//The information quality controller operation is responsible for information flow optimization decisions.  

public class InformationQualityControllerOperation {

	// Communicating operations
	InformationFlowConfigurationAndStatisticsOperation informationFlowConfigurationAndStatisticsOperation = null;
	EntityRegistrationOperation entityRegistrationOperation = null;

	public InformationQualityControllerOperation (InformationFlowConfigurationAndStatisticsOperation informationFlowConfigurationAndStatisticsOperation_, EntityRegistrationOperation entityRegistrationOperation_) {		
		informationFlowConfigurationAndStatisticsOperation = informationFlowConfigurationAndStatisticsOperation_;
		entityRegistrationOperation = entityRegistrationOperation_;
	}

	private static void CommunicateInformationFlowPolicies (int entityID, String callbackURL, InformationExchangePolicies policies) {
		// We  have code here that sends update information to an entity
		// using a REST interface on the entity
		//System.out.println ("communicating policies");
		System.out.println ("Communicating policies:"+policies.toJSONString()+" to url:"+callbackURL+" entityid:"+entityID);

		try {
			// Make a Resty connection
			Resty rest = new Resty();

			//  http://localhost:9110/update/VIM/Removed/
			String callURL = null;
			if (callbackURL.contains("?"))
				callURL = callbackURL+"&register=1";
			else
				callURL = callbackURL+"?register=1";

			// Call the relevant URL where the content is the message (communicate policies using the new goal, i.e., compact if it is needed)

			JSONObject jsobj = rest.json(callURL, content(policies.toJSONString(policies.getFlowOptimizationGoal()))).toObject();

			LoggerFrame.workflowvisualisationlog(entityID, LoggerFrame.IQCName, LoggerFrame.ICDFunctionName, "Enforcing knowledge exchange policies", "ifeofunction");							

			LoggerFrame.workflowvisualisationlog(entityID, LoggerFrame.ICDFunctionName, LoggerFrame.InformationManagementName, "Enforcing knowledge exchange policies", "ikmsfunctions");							

			LoggerFrame.workflowvisualisationlog(entityID, LoggerFrame.IKMSName, "", "Enforcing knowledge exchange policies", "entities2ikms");							


			System.out.println ("New information exchange policies sent: " + jsobj.toString());

		}  catch (IOException ioe) {
			System.out.println ("Cannot Communicate Information Flow. URL:"+callbackURL);
			ioe.printStackTrace();
		} catch (JSONException je) {
			je.printStackTrace();
		}
	}

	public HashMap<ArrayList<String>, EntityRegistrationInformation> EmbeddRegistrationInfosWithURIs (HashMap<ArrayList<String>, EntityRegistrationInformation> otherEndsRegistrationInfos) {
		// keep all augmented registration infos
		HashMap<ArrayList<String>, EntityRegistrationInformation> otherEndsRegistrationInfosWithURIs = new HashMap<ArrayList<String>, EntityRegistrationInformation>();		
		// temporary information flow requirements & constraints variable

		@SuppressWarnings("unused")
		Collection<EntityRegistrationInformation> registrationInfos = otherEndsRegistrationInfos.values();

		// create empty registrationInfo for IKMS registration (useful to carry URIs in the requirements & constraints data structure)
		EntityRegistrationInformation ikmsRegistrationInfo=entityRegistrationOperation.GetIKMSEntityRegistrationInformation();

		// temporary variables for each iteration
		ArrayList<String> uris = null;
		EntityRegistrationInformation currentEntityRegistration = null;

		// create flows with all entity ends
		Iterator<Entry<ArrayList<String>, EntityRegistrationInformation>> it = otherEndsRegistrationInfos.entrySet().iterator();
		while (it.hasNext()) {
			Entry<ArrayList<String>, EntityRegistrationInformation> pairs = (Entry<ArrayList<String>, EntityRegistrationInformation>)it.next();
			uris = pairs.getKey();
			currentEntityRegistration = pairs.getValue();

			// if it has entityid=-1 or is null, imply communication with IKMS
			if (currentEntityRegistration==null) {
				// add empty ikms entity registration
				otherEndsRegistrationInfosWithURIs.put(uris, ikmsRegistrationInfo);
			} else if (currentEntityRegistration.GetEntityId()==-1) {
				// add empty ikms entity registration
				otherEndsRegistrationInfosWithURIs.put(uris, ikmsRegistrationInfo);
			} else {
				// store it in the hashmap
				otherEndsRegistrationInfosWithURIs.put(uris, currentEntityRegistration);
			}
			//it.remove(); // avoids a ConcurrentModificationException
		}

		return otherEndsRegistrationInfosWithURIs;
	}

	public String TriggerInformationFlowEstablishment (EntityRegistrationInformation entityRegistrationInfo) {		
		// the result of registration
		String registrationResult="";

		// registrationinfos with uris
		HashMap<ArrayList<String>, EntityRegistrationInformation> otherEndsRegistrationInfosWithUris=null;

		// temporary variable for negotiation result
		InformationExchangePolicies negotiationResult=null;

		// temporary variable for otherEnd registration info
		@SuppressWarnings("unused")
		EntityRegistrationInformation otherEndRegistrationInfo=null;

		// get entityID
		int entityID = entityRegistrationInfo.GetEntityId();

		LoggerFrame.workflowvisualisationlog(entityID, LoggerFrame.ICDFunctionName, LoggerFrame.IFEOName, "Initiating Information Flow Negotiation(s)", "ikmsfunctions");	
		LoggerFrame.workflowvisualisationlog(entityID, LoggerFrame.IFCName, LoggerFrame.IQCName, "Initiating Information Flow Negotiation(s)", "ifeofunction");	

		// determine flow ends as information sources
		HashMap<ArrayList<String>, EntityRegistrationInformation> otherEndRegistrationInfos = GetOtherEndOfFlows(entityRegistrationInfo);

		// create temp hashmap with uris as keys and registration infos as values 
		otherEndsRegistrationInfosWithUris = EmbeddRegistrationInfosWithURIs (otherEndRegistrationInfos);

		// temporary variables for each iteration
		ArrayList<String> uris = null;
		EntityRegistrationInformation currentEntityRegistration = null;		

		// create flows with all entity ends
		Iterator<Entry<ArrayList<String>, EntityRegistrationInformation>> it = otherEndsRegistrationInfosWithUris.entrySet().iterator();
		while (it.hasNext()) {
			Entry<ArrayList<String>, EntityRegistrationInformation> pairs = (Entry<ArrayList<String>, EntityRegistrationInformation>)it.next();

			// get values for current iteration
			uris = pairs.getKey();
			currentEntityRegistration = pairs.getValue();

			if (currentEntityRegistration.GetEntityId()==-1) {
				// negotiate flow with IKMS
				// Negotiation takes place here

				try {
					negotiationResult = EstablishInformationFlow(entityRegistrationInfo, null);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					System.out.println ("Negotiation problem.");

					e.printStackTrace();
				}

				// add Uris to negotiation results
				negotiationResult.EmbeddURIs(uris);

				System.out.println ("Negotiation results:"+negotiationResult.toJSONString());

				registrationResult+="Negotiation with IKMS, results:"+negotiationResult.toJSONString()+" - ";

				// finalize information flow establishment
				FinalizeInformationFlowEstablishment (negotiationResult, entityRegistrationInfo, currentEntityRegistration);		

			} else {
				// negotiate flow with entity

				try {
					negotiationResult = EstablishInformationFlow(entityRegistrationInfo, currentEntityRegistration);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					System.out.println ("Negotiation problem.");
					e.printStackTrace();
				}

				// add Uris to negotiation results
				negotiationResult.EmbeddURIs(uris);

				// remove Uris from augmentedEntityRegistrationInfo - so there are not being negotiated in the next negotiation process again
				//augmentedEntityRegistrationInfo.RemoveURIs(uris);

				System.out.println ("Negotiation results:"+negotiationResult.toJSONString());

				registrationResult+="Negotiation with entity:"+pairs.getValue().GetEntityId()+", results:"+negotiationResult.toJSONString()+" - ";

				// finalize information flow establishment
				FinalizeInformationFlowEstablishment (negotiationResult, entityRegistrationInfo, currentEntityRegistration);		

			}
			//it.remove(); // avoids a ConcurrentModificationException
		}

		return registrationResult;
	}

	public void FinalizeInformationFlowEstablishment (InformationExchangePolicies negotiationResult, EntityRegistrationInformation sourceEntityRegistration, EntityRegistrationInformation destinationEntityRegistration) {		

		// temp variables
		int entityId = sourceEntityRegistration.GetEntityId();
		int urisNum = negotiationResult.GetUris().size();

		if (destinationEntityRegistration.GetEntityId()==-1) {

			// remove Uris from augmentedEntityRegistrationInfo - so there are not being negotiated in the next negotiation process again
			//augmentedEntityRegistrationInfo.RemoveURIs(uris);

			LoggerFrame.workflowvisualisationlog(entityId, LoggerFrame.IQCName, LoggerFrame.IFCName, "Determining information flow parameters", "ifeofunction");				
			LoggerFrame.workflowvisualisationlog(entityId, LoggerFrame.IQCName, LoggerFrame.IFCName, "Storing information flow parameters", "ifeofunction");				
			LoggerFrame.workflowvisualisationlog(entityId, LoggerFrame.IFEOName, LoggerFrame.ISIName, "Storing information flow parameters", "ikmsfunctions");				

			System.out.println ("Storing InformationExchangePolicies");
			// passing empty callback url				
			InformationFlowConfigurationAndStatisticsOperation.registerFlow(negotiationResult);

			LoggerFrame.workflowvisualisationlog(entityId, LoggerFrame.ICDFunctionName, LoggerFrame.IFEOName, "Resolving Information/Knowledge Dependencies", "ikmsfunctions");	

			// sending the decided information flow policies back
			CommunicateInformationFlowPolicies (entityId, sourceEntityRegistration.GetNextNodeCallBackURL(), negotiationResult);

			LoggerFrame.workflowvisualisationlog(entityId, LoggerFrame.IQCName, LoggerFrame.ICDFunctionName, "Flow Established with IKMS ("+urisNum+" uri(s))", "ifeofunction");	
			LoggerFrame.workflowvisualisationlog(entityId, LoggerFrame.IFEOName, LoggerFrame.ICDFunctionName, "Flow Established with IKMS ("+urisNum+" uri(s))", "ikmsfunctions");	
			LoggerFrame.workflowvisualisationlog(entityId, LoggerFrame.IKMSName, "", "Flow Established with IKMS ("+urisNum+" uri(s))", "entities2ikms");	

		} else {
			LoggerFrame.workflowvisualisationlog(entityId, LoggerFrame.IQCName, LoggerFrame.IFCName, "Determining information flow parameters", "ifeofunction");				
			LoggerFrame.workflowvisualisationlog(entityId, LoggerFrame.IQCName, LoggerFrame.IFCName, "Storing information flow parameters", "ifeofunction");				

			System.out.println ("Storing InformationExchangePolicies");
			// passing empty callback url
			//InformationFlowConfigurationAndStatisticsOperation.registerFlow(currentEntityRegistration.GetEntityId(), "", negotiationResult);
			//InformationFlowConfigurationAndStatisticsOperation.registerFlow(entityID, "", negotiationResult);				
			InformationFlowConfigurationAndStatisticsOperation.registerFlow(negotiationResult);				

			LoggerFrame.workflowvisualisationlog(entityId, LoggerFrame.ICDFunctionName, LoggerFrame.IFEOName, "Resolving Information/Knowledge Dependencies", "ikmsfunctions");	

			// communicating the decided information flow policies
			CommunicateInformationFlowPolicies (destinationEntityRegistration.GetEntityId(), destinationEntityRegistration.GetNextNodeCallBackURL(), negotiationResult);
			CommunicateInformationFlowPolicies (entityId, sourceEntityRegistration.GetNextNodeCallBackURL(), negotiationResult);

			LoggerFrame.workflowvisualisationlog(entityId, LoggerFrame.IQCName, LoggerFrame.ICDFunctionName, "Flow Established - "+ LoggerFrame.getActiveEntityName(entityId)+" with "+destinationEntityRegistration.GetEntityName()+" ("+urisNum+" uri(s))", "ifeofunction");	
			LoggerFrame.workflowvisualisationlog(entityId, LoggerFrame.IFEOName, LoggerFrame.ICDFunctionName, "Flow Established - "+ LoggerFrame.getActiveEntityName(entityId)+" with "+destinationEntityRegistration.GetEntityName()+" ("+urisNum+" uri(s))", "ikmsfunctions");					
			LoggerFrame.workflowvisualisationlog(entityId, destinationEntityRegistration.GetEntityName(), "", "Flow Established - "+ LoggerFrame.getActiveEntityName(entityId)+" with "+destinationEntityRegistration.GetEntityName()+" ("+urisNum+" uri(s))", "entities2ikms");	

		}

	}

	// Returns the matching flows for all requested URIs
	public HashMap<ArrayList<String>, EntityRegistrationInformation> GetOtherEndOfFlows (EntityRegistrationInformation entityRegistrationInfo) {
		int entityid = entityRegistrationInfo.GetEntityId();
		HashMap<Integer, ArrayList<String>> sourceEntitiesWithUris;
		ArrayList<Integer> sourceEntities;
		HashMap<Integer, ArrayList<String>> sinkEntitiesWithUris;
		ArrayList<Integer> sinkEntities;

		//logging information flow negotiation workflow
		@SuppressWarnings("unused")
		Boolean logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IQCName, LoggerFrame.IFCName, "Retrieving Information Flow End Options", "ifeofunction");	

		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IFEOName, LoggerFrame.ISIName, "Retrieving Information Flow End Options", "ikmsfunctions");				

		// continue the workflow, if there is a uri match only
		sourceEntitiesWithUris = entityRegistrationOperation.GetEntitiesFromUris(entityRegistrationInfo.GetUrisForRequiredInformation(), "available");

		sourceEntities = new ArrayList<Integer>(sourceEntitiesWithUris.keySet());

		// function output
		HashMap<ArrayList<String>, EntityRegistrationInformation> otherEndsRegistrationInfo=new HashMap<ArrayList<String>, EntityRegistrationInformation>();

		// iterate through all matching sink Entities
		for (Integer entityId : sourceEntities) {
			if (entityId==-1) {
				//flow with IKMS requested
				logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.IQCName, "Preselecting IKMS as Information Sink", "ifeofunction");				
				otherEndsRegistrationInfo.put(sourceEntitiesWithUris.get(entityId), null);
			} else {
				logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.IQCName, "Preselecting "+LoggerFrame.getActiveEntityName(entityId)+ " as Information Source", "ifeofunction");				
				try {
					otherEndsRegistrationInfo.put(sourceEntitiesWithUris.get(entityId), new EntityRegistrationInformation (entityRegistrationOperation.GetEntityRegistrationInfoFromStorage(entityId).toString()));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		// continue the workflow, if there is a uri match only
		sinkEntitiesWithUris = entityRegistrationOperation.GetEntitiesFromUris(entityRegistrationInfo.GetUrisForAvailableInformation(), "required");

		sinkEntities = new ArrayList<Integer>(sinkEntitiesWithUris.keySet());

		// temp ArrayList with URIs
		ArrayList<String> tempUris;		
		// temp EntityRegistration
		@SuppressWarnings("unused")
		EntityRegistrationInformation tempRegistration;

		// iterate through all matching sink Entities
		for (Integer entityId : sinkEntities) {
			// check if this entityId was considered already
			if (sourceEntities.contains(entityId)) {
				// flow exists - update it
				if (entityId==-1) {
					// update flow with IKMS
					logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.IQCName, "Preselecting IKMS as Information Sink", "ifeofunction");				
					// update uris only
					tempUris = sourceEntitiesWithUris.get(entityId);
					// remove previous one
					otherEndsRegistrationInfo.remove(tempUris);
					// add sink uris as well
					tempUris.addAll(sinkEntitiesWithUris.get(entityId));
					// add all uris in a single record
					otherEndsRegistrationInfo.put(tempUris, null);
				} else {
					// update regular flow
					logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.IQCName, "Preselecting Information Sink, entityid:"+entityId, "ifeofunction");				
					tempUris = sourceEntitiesWithUris.get(entityId);
					// remove previous one
					otherEndsRegistrationInfo.remove(tempUris);
					// add sink uris as well
					tempUris.addAll(sinkEntitiesWithUris.get(entityId));
					// add all uris in a single record
					try {
						otherEndsRegistrationInfo.put(tempUris, new EntityRegistrationInformation (entityRegistrationOperation.GetEntityRegistrationInfoFromStorage(entityId).toString()));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			} else {
				// add new flow
				if (entityId==-1) {
					// flow with IKMS requested
					logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.IQCName, "Preselecting IKMS as Information Sink", "ifeofunction");				
					otherEndsRegistrationInfo.put(sinkEntitiesWithUris.get(entityId), null);
				} else {
					// regular flow requested
					logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.IQCName, "Preselecting Information Sink, entityid:"+entityId, "ifeofunction");				
					try {
						otherEndsRegistrationInfo.put(sinkEntitiesWithUris.get(entityId), new EntityRegistrationInformation (entityRegistrationOperation.GetEntityRegistrationInfoFromStorage(entityId).toString()));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}

		return otherEndsRegistrationInfo;
	}

	public InformationExchangePolicies ReEstablishInformationFlow (int sourceEntityId, int destinationEntityId, ArrayList<String> uris) throws Exception {
		// get EntityRegistrationInformation data-structures from ids
		EntityRegistrationInformation sourceEntity = null;
		EntityRegistrationInformation destinationEntity = null;
		InformationExchangePolicies negotiationResult=null;

		LoggerFrame.workflowvisualisationlog(sourceEntityId, LoggerFrame.ICDFunctionName, LoggerFrame.IFEOName, "Re-initiating Information Flow Negotiation(s)", "ikmsfunctions");	
		LoggerFrame.workflowvisualisationlog(sourceEntityId, LoggerFrame.IFCName, LoggerFrame.IQCName, "Re-initiating Information Flow Negotiation(s)", "ifeofunction");	

		LoggerFrame.workflowvisualisationlog(destinationEntityId, LoggerFrame.ICDFunctionName, LoggerFrame.IFEOName, "Re-initiating Information Flow Negotiation(s)", "ikmsfunctions");	
		LoggerFrame.workflowvisualisationlog(destinationEntityId, LoggerFrame.IFCName, LoggerFrame.IQCName, "Re-initiating Information Flow Negotiation(s)", "ifeofunction");	

		String sourceEntityRegistration = null;
		String destinationEntityRegistration = null;

		if (sourceEntityId!=-1)
			sourceEntityRegistration = entityRegistrationOperation.GetEntityRegistrationInfoFromStorage(sourceEntityId);
		if (destinationEntityId!=-1)
			destinationEntityRegistration = entityRegistrationOperation.GetEntityRegistrationInfoFromStorage(destinationEntityId);

		if (sourceEntityRegistration==null) {
			// flow with IKMS
			sourceEntity = null;	
		} else  {
			// retrieve other entity's registration information
			sourceEntity = new EntityRegistrationInformation(sourceEntityRegistration);
		}
		if (destinationEntityRegistration==null) {
			// flow with IKMS
			destinationEntity =null;	
		} else {
			// retrieve other entity's registration information
			destinationEntity = new EntityRegistrationInformation(destinationEntityRegistration);
		}
		if (sourceEntity!=null) {
			negotiationResult = EstablishInformationFlow (sourceEntity, destinationEntity);

			// add Uris to negotiation results
			negotiationResult.EmbeddURIs(uris);
		}

		// Add empty IKMS entity registration info to finalize information flow.
		if (sourceEntity==null)
			sourceEntity = entityRegistrationOperation.GetIKMSEntityRegistrationInformation();

		if (destinationEntity==null)
			destinationEntity = entityRegistrationOperation.GetIKMSEntityRegistrationInformation();

		if (negotiationResult!=null)
			FinalizeInformationFlowEstablishment (negotiationResult, sourceEntity, destinationEntity);		
		return negotiationResult;
	}

	public InformationExchangePolicies EstablishInformationFlow (EntityRegistrationInformation sourceEntity, EntityRegistrationInformation destinationEntity) throws Exception {
		InformationFlowRequirementsAndConstraints sourceConstraints = sourceEntity.GetInformationFlowConstraints();
		InformationFlowRequirementsAndConstraints destinationConstraints = null;
		InformationExchangePolicies output = null;

		if (destinationEntity!=null)
			destinationConstraints = destinationEntity.GetInformationFlowConstraints();

		if (destinationEntity==null) {
			//negotiate information flow with IKMS
			System.out.println ("Negotiate information flow with entityid:"+sourceEntity.GetEntityId()+" with IKMS");

			LoggerFrame.workflowvisualisationlog(sourceEntity.GetEntityId(), LoggerFrame.IQCName, LoggerFrame.IFCName, "Negotiating entity with IKMS", "ifeofunction");				

			output = EstablishInformationFlowWithIKMS (sourceConstraints);
			// default flow end entity id is with IKMS
			output.setSourceEntityId(sourceEntity.GetEntityId());
			output.setDestinationEntityId(-1);

			return output;

		} else {
			// negotiate information flow between two entities
			System.out.println ("Negotiate information flow from entityid:"+sourceEntity.GetEntityId()+" with entity with entityid:"+destinationEntity.GetEntityId());

			LoggerFrame.workflowvisualisationlog(sourceEntity.GetEntityId(), LoggerFrame.IQCName, LoggerFrame.IFCName, "Negotiating entity with "+LoggerFrame.getActiveEntityName(destinationEntity.GetEntityId()), "ifeofunction");				

			output = EstablishInformationFlowBetweenEntities (sourceConstraints, destinationConstraints);

			// set flow ends' entity ids in the InformationExchangePolicies data structure
			output.setSourceEntityId(sourceEntity.GetEntityId());
			output.setDestinationEntityId(destinationEntity.GetEntityId());

			return output;
		}
	}

	private InformationExchangePolicies EstablishInformationFlowBetweenEntities (InformationFlowRequirementsAndConstraints sourceConstraints, InformationFlowRequirementsAndConstraints destinationConstraints) throws Exception {
		InformationExchangePolicies output = new InformationExchangePolicies();
		// set the global performance optimization goal as a default option
		IKMSOptimizationGoal currentGlobalGoal = GetGlobalPerformanceOptimizationGoal ();
		output.setFlowOptimizationGoal(currentGlobalGoal);
		String rateNegotiationRemark="";
		String goalNegotiationRemark="";
		String methodNegotiationRemark="";

		// start negotiation if both constraints are not null
		if (sourceConstraints!=null&&destinationConstraints!=null) {

			// negotiate data rates
			// minimumInformationRetrievalRate from destination
			// maximumInformationRetrievalRate from destination
			// minimumInfomationSharingRate from source
			// maximumInformationSharingRate from source
			// get max from min and min from max
			// negotiating minimum rate
			if (sourceConstraints.getMinimumInformationSharingRate()==-1) {
				// keep minimum rate from destination
				output.setMinimumInformationSharingRate(destinationConstraints.getMinimumInformationRetrievalRate());
				output.setMinimumInformationRetrievalRate(destinationConstraints.getMinimumInformationRetrievalRate());

				// insert negotiation remark
				rateNegotiationRemark+="Source min constraints not set. keeping min rates from destination. ";
			} else {
				if (destinationConstraints.getMinimumInformationRetrievalRate()==-1) {
					// keep minimum rate from source
					output.setMinimumInformationSharingRate(sourceConstraints.getMinimumInformationSharingRate());
					output.setMinimumInformationRetrievalRate(sourceConstraints.getMinimumInformationSharingRate());								

					// insert negotiation remark
					rateNegotiationRemark+="Destination min constraints not set. keeping min rates from source. ";
				} else {
					if (sourceConstraints.getMinimumInformationSharingRate()>destinationConstraints.getMinimumInformationRetrievalRate()) {
						output.setMinimumInformationSharingRate(sourceConstraints.getMinimumInformationSharingRate());
						output.setMinimumInformationRetrievalRate(sourceConstraints.getMinimumInformationSharingRate());														

						// insert negotiation remark
						rateNegotiationRemark+="Min source rates higher. keeping min rates from source. ";
					} else {
						output.setMinimumInformationSharingRate(destinationConstraints.getMinimumInformationRetrievalRate());
						output.setMinimumInformationRetrievalRate(destinationConstraints.getMinimumInformationRetrievalRate());										

						// insert negotiation remark
						rateNegotiationRemark+="Min destination rates higher. keeping min rates from destination. ";
					}
				}
			}

			// negotiating maximum rate
			if (sourceConstraints.getMaximumInformationSharingRate()==-1) {
				// keep minimum rate from destination
				output.setMaximumInformationSharingRate(destinationConstraints.getMaximumInformationRetrievalRate());
				output.setMaximumInformationRetrievalRate(destinationConstraints.getMaximumInformationRetrievalRate());				

				// insert negotiation remark
				rateNegotiationRemark+="Source max constraints not set. keeping max rates from destination. ";
			} else {
				if (destinationConstraints.getMaximumInformationRetrievalRate()==-1) {
					// keep minimum rate from source
					output.setMaximumInformationSharingRate(sourceConstraints.getMaximumInformationSharingRate());
					output.setMaximumInformationRetrievalRate(sourceConstraints.getMaximumInformationSharingRate());								

					// insert negotiation remark
					rateNegotiationRemark+="Destination max constraints not set. keeping max rates from source. ";
				} else {
					if (sourceConstraints.getMaximumInformationSharingRate()<destinationConstraints.getMaximumInformationRetrievalRate()) {
						output.setMaximumInformationSharingRate(sourceConstraints.getMaximumInformationSharingRate());
						output.setMaximumInformationRetrievalRate(sourceConstraints.getMaximumInformationSharingRate());														

						// insert negotiation remark
						rateNegotiationRemark+="Max source rates lower. keeping max rates from source. ";
					} else {
						output.setMaximumInformationSharingRate(destinationConstraints.getMaximumInformationRetrievalRate());
						output.setMaximumInformationRetrievalRate(destinationConstraints.getMaximumInformationRetrievalRate());										

						// insert negotiation remark
						rateNegotiationRemark+="Max destination rates lower. keeping max rates from destination. ";
					}
				}
			}

			// negotiate minimum rate should be less than negotiated maximum rate
			if (output.getMinimumInformationSharingRate()<output.getMaximumInformationSharingRate()) {
				System.out.println ("Rate negotiation successful");

				// insert negotiation remark
				rateNegotiationRemark+="Rate negotiation successful.";
			} else {
				System.out.println ("Rate negotiation unsuccessful. Should reinitiate negotiation with IKMS");
				// insert negotiation remark
				rateNegotiationRemark+="Rate negotiation unsuccessful (negotiated minimum is higher than negotiated maximum).";
			}

			// insert negotiation remark
			output.setExchangeRateNegotiationRemarks(rateNegotiationRemark);

			// negotiate global goal
			// global goal with high priority, overrides source + destination goals
			// in case of conflicting source + destination goals, global goal overrides them
			// source + destination goal is accepted, in the case they have the same goal (or one of those is null) and with priority higher than gobal goal's

			if (sourceConstraints.getFlowOptimizationGoal()!=null) {
				if (destinationConstraints.getFlowOptimizationGoal()!=null) {
					if (sourceConstraints.getFlowOptimizationGoal().getOptGoalId()==destinationConstraints.getFlowOptimizationGoal().getOptGoalId()) {
						output.setFlowOptimizationGoal(NegotiateGoal (sourceConstraints.getFlowOptimizationGoal()));

						// insert negotiation remark
						goalNegotiationRemark+="same goal requested: "+sourceConstraints.getFlowOptimizationGoal().getOptGoalName()+". ";
					} else {
						System.out.println ("There is no agreement for a goal, using global goal instead.");
						// insert negotiation remark
						goalNegotiationRemark+="No agreement for a goal, using global goal instead. ";
					}	
				} else {
					// negotiate source goal (destination is null)
					output.setFlowOptimizationGoal(NegotiateGoal (sourceConstraints.getFlowOptimizationGoal()));

					// insert negotiation remark
					goalNegotiationRemark+="Negotiating source goal (destination is null): "+sourceConstraints.getFlowOptimizationGoal().getOptGoalName()+". ";
				}
			} else {
				if (destinationConstraints.getFlowOptimizationGoal()!=null) {
					// negotiate destination goal (source is null)
					output.setFlowOptimizationGoal(NegotiateGoal (destinationConstraints.getFlowOptimizationGoal()));

					// insert negotiation remark
					goalNegotiationRemark+="Negotiating destination goal (source is null): "+destinationConstraints.getFlowOptimizationGoal().getOptGoalName()+". ";
				}
			}

			// insert negotiation remark
			output.setOptimizationGoalNegotiationRemarks(goalNegotiationRemark);

			// negotiate information exchange method
			// if source supports both
			if (sourceConstraints.getMethod()==Methods.All) {
				// use destination method
				output.setMethod(destinationConstraints.getMethod());

				// insert negotiation remark
				methodNegotiationRemark+="Selecting destination method (source supports all methods). ";

			} else if (destinationConstraints.getMethod()==Methods.All) {
				// use source method
				output.setMethod(sourceConstraints.getMethod());

				// insert negotiation remark
				methodNegotiationRemark+="Selecting source method (destination supports all methods). ";
			} else if (sourceConstraints.getMethod()==destinationConstraints.getMethod()) {
				output.setMethod(sourceConstraints.getMethod());

				// insert negotiation remark
				methodNegotiationRemark+="Same method requested. ";
			} else {
				System.out.println ("Mismatch in the requested knowledge exchange methods. Using push/pull");
				output.setMethod(Methods.PushPull);

				// insert negotiation remark
				methodNegotiationRemark+="Mismatch in the requested knowledge exchange methods. Using push/pull. ";
			}

			// insert negotiation remark
			output.setMethodNegotiationRemarks(methodNegotiationRemark);

		} else {
			// if one of those is null, keep constraints from other
			if (sourceConstraints!=null) {
				output = new InformationExchangePolicies (sourceConstraints);

				output.setExchangeRateNegotiationRemarks("Destination constraints not set. Keeping constraints from source. ");
				output.setMethodNegotiationRemarks("Destination constraints not set. Keeping constraints from source. ");
				output.setOptimizationGoalNegotiationRemarks("Destination constraints not set. Negotiating goal from source "+ output.getFlowOptimizationGoal().getOptGoalName() +". ");

				output.setFlowOptimizationGoal(NegotiateGoal(output.getFlowOptimizationGoal()));

				return output;
			}
			if (destinationConstraints!=null) {
				output = new InformationExchangePolicies (destinationConstraints);

				output.setExchangeRateNegotiationRemarks("Source constraints not set. Keeping constraints from destination. ");
				output.setMethodNegotiationRemarks("Source constraints not set. Keeping constraints from destination. ");
				output.setOptimizationGoalNegotiationRemarks("Source constraints not set. Negotiating goal from destination" + output.getFlowOptimizationGoal().getOptGoalName() + ". ");

				output.setFlowOptimizationGoal(NegotiateGoal(output.getFlowOptimizationGoal()));

				return output;
			}		
		}
		return output;
	}

	private InformationExchangePolicies EstablishInformationFlowWithIKMS (InformationFlowRequirementsAndConstraints sourceConstraints) throws Exception {
		InformationExchangePolicies output = new InformationExchangePolicies();
		// set the global performance optimization goal as a default option
		IKMSOptimizationGoal currentGlobalGoal = GetGlobalPerformanceOptimizationGoal ();
		output.setFlowOptimizationGoal(currentGlobalGoal);

		if (sourceConstraints==null) {
			// no constraints set, return empty result with the global performance optimization goal set only
			System.out.println ("Decided goal:"+output.getFlowOptimizationGoal().getOptGoalName());

			output.setExchangeRateNegotiationRemarks("Constraints not set. Keeping default options. ");
			output.setMethodNegotiationRemarks("Constraints not set. Keeping default options. ");
			output.setOptimizationGoalNegotiationRemarks("Goal not suggested. Keeping global goal. ");

			return output;
		} else {
			// accept all constraints besides the performance goal (since IKMS is the end point)
			output = new InformationExchangePolicies (sourceConstraints);

			output.setExchangeRateNegotiationRemarks("Keeping all constraints from entity. ");
			output.setMethodNegotiationRemarks("Keeping all constraints from entity. ");

			if (output.getFlowOptimizationGoal()==null) {
				// no goal is set, use global goal instead
				output.setOptimizationGoalNegotiationRemarks("No goal suggested. Using global goal. ");
				output.setFlowOptimizationGoal(currentGlobalGoal);
			} else {
				output.setOptimizationGoalNegotiationRemarks("Negotiating suggested goal from entity:"+output.getFlowOptimizationGoal().getOptGoalName()+". ");

				// check requested performance optimization goal
				// if global goal has a high priority, use that
				output.setFlowOptimizationGoal(NegotiateGoal (output.getFlowOptimizationGoal()));
			}
			System.out.println ("Decided goal:"+output.getFlowOptimizationGoal().getOptGoalName());

			return output;
		}			

	}

	// negotiate suggested goal with global established goal
	private IKMSOptimizationGoal NegotiateGoal (IKMSOptimizationGoal suggestedGoal) {
		IKMSOptimizationGoal currentGlobalGoal = GetGlobalPerformanceOptimizationGoal ();

		return informationFlowConfigurationAndStatisticsOperation.NegotiateGoal (currentGlobalGoal, suggestedGoal);
	}

	public void SetGlobalPerformanceOptimizationGoal (IKMSOptimizationGoal ikmsGoal) {
		// retrieve stored data structure (includes optimization rules)
		IKMSOptimizationGoal goal = IKMSOptimizationGoals.GetGoalById(ikmsGoal.getOptGoalId());
		goal.setEnforcementLevel(ikmsGoal.getOptGoalLevelofEnforcement());
		goal.setOptGoalParameters(ikmsGoal.getOptGoalParameters());

		// change it if it is different, only
		if (SelectedOptimizationGoal.GetOptimizationGoal()==null) {
			// use stored data structure (includes optimization rules)
			SelectedOptimizationGoal.SetOptimizationGoal(goal);	
		} else {
			if (ikmsGoal!=SelectedOptimizationGoal.GetOptimizationGoal()) {
				// use stored data structure (includes optimization rules)
				SelectedOptimizationGoal.SetOptimizationGoal(goal);
				// trigger re-negotiation of existing flows - TBD!!!
				RenegotiateExistingFlows ();
			} 
		}
	}

	public IKMSOptimizationGoal GetGlobalPerformanceOptimizationGoal () {
		return SelectedOptimizationGoal.GetOptimizationGoal();
	}

	public void RenegotiateExistingFlows () {
		// get new goal
		IKMSOptimizationGoal newGoal = GetGlobalPerformanceOptimizationGoal();

		// get flows that need updating
		HashMap<ArrayList<String>, HashMap<Integer, Integer>> flowsToReNegotiate = informationFlowConfigurationAndStatisticsOperation.FindFlowsThatNeedRestablishmentDueToNewGlobalGoal(newGoal);

		// temp variables
		int sourceId;
		int destinationId;
		ArrayList<String> uris;

		// trigger flows' re-establishments
		// create flows with all entity ends
		Iterator<Entry<ArrayList<String>, HashMap<Integer, Integer>>> it = flowsToReNegotiate.entrySet().iterator();
		while (it.hasNext()) {
			Entry<ArrayList<String>, HashMap<Integer, Integer>> pairs = (Entry<ArrayList<String>, HashMap<Integer, Integer>>)it.next();

			// get values for current iteration
			uris = pairs.getKey();
			sourceId = (Integer) pairs.getValue().keySet().toArray()[0];
			destinationId = (Integer) pairs.getValue().values().toArray()[0];
			@SuppressWarnings("unused")
			InformationExchangePolicies negotiationResult=null;
			// re-establish those information flows
			try {
				negotiationResult = ReEstablishInformationFlow (sourceId, destinationId, uris);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println ("Re-negotiation problems.");
			}
		}

	}
}

