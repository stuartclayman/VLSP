package ikms.operations;

import ikms.data.IKMSOptimizationGoal;
import ikms.data.IKMSOptimizationGoal.OptimizationRules;
import ikms.data.IKMSOptimizationGoals;
import ikms.data.InformationExchangePolicies;
import ikms.data.InformationFlowRequirementsAndConstraints.Methods;
import ikms.functions.InformationStorageAndIndexingFunction;
import ikms.util.LoggerFrame;

import java.util.ArrayList;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

// Information can be queried from the Knowledge Building Entities during their knowledge building process and both information 
// & knowledge can be queried from the entities that perform optimization or configuration changes. These interactions are 
// handled through the Information Exchange Interface. The information retrieval operation may use the same methods with the 
// information collection operation.

// A retrieval operation is triggered from an entity

public class InformationRetrievalOperation {
	// Communicating functions
	InformationStorageAndIndexingFunction informationStorageAndIndexingFunction = null;

	// Communicating operations
	InformationCollectionOperation informationCollectionOperation = null;
	InformationAuthorizationOperation informationAuthorizationOperation = null;

	public InformationRetrievalOperation (InformationAuthorizationOperation informationAuthorizationOperation_, InformationStorageAndIndexingFunction informationStorageAndIndexingFunction_, InformationCollectionOperation informationCollectionOperation_) {
		// Initializing communicating functions
		informationStorageAndIndexingFunction = informationStorageAndIndexingFunction_;
		// Initializing communicating operations
		informationCollectionOperation = informationCollectionOperation_;
		informationAuthorizationOperation = informationAuthorizationOperation_;
	}

	// Request Information value using the Pull Method
	// passes the id of the requesting ENTITY
	public String RequestInformation (int entityid, String uri) {
		String output=null;

		// Logging for internal IKMS functions workflow diagram
		//Boolean logoutput = LoggerFrame.testlog(LoggerFrame.ICDFunctionName, LoggerFrame.ISIENTITYStorageName, "Checking Authorization", "black");				
		@SuppressWarnings("unused")
		Boolean logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Checking Authorization", "ikmsfunctions");				

		// Checking if the particular entity is authorized
		if (informationAuthorizationOperation.CheckAuthorization(entityid, uri, "required")==false) {
			// Not granted
			System.out.println ("Not Granted.");

			// Logging for internal IKMS functions workflow diagram
			//logoutput = LoggerFrame.testlog(LoggerFrame.ISIENTITYStorageName, LoggerFrame.ICDFunctionName, "Access not granted", "black");				
			logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ISIName, LoggerFrame.ICDFunctionName, "Access not granted", "ikmsfunctions");				

			// Logging for internal IKMS functions workflow diagram
			logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.IKMSName, "Access not granted", "ikmsfunctions");				

			// Logging for internal IKMS functions workflow diagram
			logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "Access not granted", "entities2ikms");				

			return null;
		}
		// Authorization granted
		System.out.println ("Granted.");
		// Logging for internal IKMS functions workflow diagram
		//logoutput = LoggerFrame.testlog(LoggerFrame.ISIENTITYStorageName, LoggerFrame.ICDFunctionName, "Granted", "black");				
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ISIName, LoggerFrame.ICDFunctionName, "Granted", "ikmsfunctions");				

		// Retrieving active flow information
		InformationExchangePolicies activeFlow = InformationFlowConfigurationAndStatisticsOperation.getFlowRegistration(uri);

		// retrieve complete data structure of stored IKMS optimization goal (includes optimization rules that are not being communicated)
		IKMSOptimizationGoal activeGoal = IKMSOptimizationGoals.GetGoalById(activeFlow.getFlowOptimizationGoal().getOptGoalId());
		System.out.println ("Active flow has goal name:"+activeGoal.getOptGoalName()+" id:"+activeGoal.getOptGoalId()+" rules:"+activeGoal.GetOptimizationRules().toString()+" decided method:"+activeFlow.getMethod());

		// in case of direct entity2entity communication
		if (activeGoal.CheckOptimizationRule(OptimizationRules.DirectEntity2EntityCommunication)||activeFlow.getMethod()==Methods.Entity2Entity) {
			// Direct entity2entity communication (passing back the REST URI instead of the value)

			// Checks information retrievability (i.e., if information is indexed)
			// Logging for internal IKMS functions workflow diagram
			//logoutput = LoggerFrame.testlog(LoggerFrame.ICDFunctionName, LoggerFrame.ISIIndexingStorageName, "Checking information retrievability", "black");				
			logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Checking information retrievability", "ikmsfunctions");				

			String locationURL =informationStorageAndIndexingFunction.informationIndexing.GetInformationIndex(uri);
			if (locationURL==null || locationURL.equals("")) {
				// Logging for internal IKMS functions workflow diagram
				//logoutput = LoggerFrame.testlog(LoggerFrame.ISIIndexingStorageName, LoggerFrame.ICDFunctionName, "Information cannot retrieved", "black");				
				logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ISIName, LoggerFrame.ICDFunctionName, "Information is not indexed", "ikmsfunctions");				

				// Logging for internal IKMS functions workflow diagram
				logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationExchangeName, "Information is not indexed", "ikmsfunctions");				

				// Logging for internal IKMS functions workflow diagram
				logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "Information is not indexed", "entities2ikms");				

				System.out.println ("Information is not indexed");
				return null;
			} else {
				// Collecting information for uri:(uri) from location:(locationURL)
				// Uses REST call to retrieve a single value from a remote entity

				// Logging for internal IKMS functions workflow diagram
				logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationExchangeName, "Passing information retrieval information (direct entity2entity communication mode)", "ikmsfunctions");				
				logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "Returning information retrieval information (direct entity2entity communication mode)", "entities2ikms");				
				
				String result = "{\"url\" : "+"\""+locationURL+"\"}";
				// return the rest url in JSON format 
				return CheckIfNeedsCompacting (activeGoal, result);
			}
		} else {		
			if (activeGoal.CheckOptimizationRule(OptimizationRules.FirstFetchThenRetrieveFromStorage)) {
				// Communication through IKMS enabled, prioritize entities fetching than IKMS stored values
				// Checks information availability
				// Logging for internal IKMS functions workflow diagram
				//logoutput = LoggerFrame.testlog(LoggerFrame.ICDFunctionName, LoggerFrame.ISIInfoStorageName, "Checking information availability", "black");				
				logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Checking information availability", "ikmsfunctions");				

				String locationURL =informationStorageAndIndexingFunction.informationIndexing.GetInformationIndex(uri);
				if (locationURL==null || locationURL.equals("")) {

					// Logging for internal IKMS functions workflow diagram
					//logoutput = LoggerFrame.testlog(LoggerFrame.ISIIndexingStorageName, LoggerFrame.ICDFunctionName, "Information cannot retrieved", "black");				
					logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ISIName, LoggerFrame.ICDFunctionName, "Information cannot retrieved", "ikmsfunctions");				

					// Logging for internal IKMS functions workflow diagram
					logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationExchangeName, "Information is not indexed", "ikmsfunctions");				

					System.out.println ("Information is not indexed");		
				} else {
					// Collecting information for uri:(uri) from location:(locationURL)
					// Uses REST call to retrieve a single value from a remote Entity

					// Logging for internal IKMS functions workflow diagram
					logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationExchangeName, "Retrieve Information", "ikmsfunctions");				

					output = informationCollectionOperation.CollectInformationUsingPull (entityid, locationURL, uri);

					if (output==null) {
						// Logging for internal IKMS functions workflow diagram
						logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationExchangeName, "Collection incapability", "ikmsfunctions");				

					} else {
						// store value in the local storage, if it is needed
						if (! activeGoal.CheckOptimizationRule(OptimizationRules.DoNotStoreWithoutANeed)) {
							// Logging for internal IKMS functions workflow diagram
							//logoutput = LoggerFrame.testlog(LoggerFrame.ICDFunctionName, LoggerFrame.ISIInfoStorageName, "Storing info (if it is required)", "black");				
							logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Storing information", "ikmsfunctions");				
							// Storing value in main storage
							try {
								informationStorageAndIndexingFunction.informationStorage.StoreInformationInMainStorage(uri, new JSONObject(output));
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}

				if (locationURL==null || locationURL.equals("") || output==null) {

					// information is not indexed or cannot be collected, checking storage
					output=informationStorageAndIndexingFunction.informationStorage.GetInformationFromMainStorage(uri);
					if (output==null) {
						// Information not available in storage
						// Logging for internal IKMS functions workflow diagram
						//logoutput = LoggerFrame.testlog(LoggerFrame.ISIInfoStorageName, LoggerFrame.ICDFunctionName, "Information not available", "black");				
						logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ISIName, LoggerFrame.ICDFunctionName, "Information not available in the storage", "ikmsfunctions");				

					} else {

						// Logging for internal IKMS functions workflow diagram
						//logoutput = LoggerFrame.testlog(LoggerFrame.ISIInfoStorageName, LoggerFrame.ICDFunctionName, "Information retrieved", "black");				
						logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ISIName, LoggerFrame.ICDFunctionName, "Information retrieved from storage", "ikmsfunctions");				

						// do not need to store again
						/*	if (! activeGoal.CheckOptimizationRule(OptimizationRules.DoNotStoreWithoutANeed)) {
							// Logging for internal IKMS functions workflow diagram
							//logoutput = LoggerFrame.testlog(LoggerFrame.ICDFunctionName, LoggerFrame.ISIInfoStorageName, "Storing info (if it is required)", "black");				
							logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Storing information", "ikmsfunctions");				
							// Storing value in main storage
							try {
								informationStorageAndIndexingFunction.informationStorage.StoreInformationInMainStorage(uri, new JSONObject(output));
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}*/

						// Logging for internal IKMS functions workflow diagram
						logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationExchangeName, "Information passed to entity", "ikmsfunctions");				
						// Logging for internal IKMS functions workflow diagram
						logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "Information received", "entities2ikms");				

						System.out.println ("Information retrieved from storage.");
						return CheckIfNeedsCompacting (activeGoal, output);
					}			
				} else {
					// Logging for internal IKMS functions workflow diagram
					logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationExchangeName, "Passing collected information", "ikmsfunctions");				

					// Logging for internal IKMS functions workflow diagram
					logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "Information received", "entities2ikms");				
					return CheckIfNeedsCompacting (activeGoal, output);
				}
				return null;

			} else {
				// Communication through IKMS enabled, prioritize IKMS stored values than entity fetching
				// Checks information availability
				// Logging for internal IKMS functions workflow diagram
				logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Checking information availability", "ikmsfunctions");				

				output=informationStorageAndIndexingFunction.informationStorage.GetInformationFromMainStorage(uri);
				if (output==null) {
					// Information not available in storage
					// Logging for internal IKMS functions workflow diagram
					//logoutput = LoggerFrame.testlog(LoggerFrame.ISIInfoStorageName, LoggerFrame.ICDFunctionName, "Information not available", "black");				
					logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ISIName, LoggerFrame.ICDFunctionName, "Information not available", "ikmsfunctions");				

					// Checks information retrievability (i.e., if information is indexed)
					// Logging for internal IKMS functions workflow diagram
					//logoutput = LoggerFrame.testlog(LoggerFrame.ICDFunctionName, LoggerFrame.ISIIndexingStorageName, "Checking information retrievability", "black");				
					logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Checking information retrievability", "ikmsfunctions");				

					String locationURL = informationStorageAndIndexingFunction.informationIndexing.GetInformationIndex(uri);

					System.out.println ("locationURL:"+locationURL);
					if (locationURL==null || locationURL.equals("")) {
						// Logging for internal IKMS functions workflow diagram
						//logoutput = LoggerFrame.testlog(LoggerFrame.ISIIndexingStorageName, LoggerFrame.ICDFunctionName, "Information cannot retrieved", "black");				
						logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ISIName, LoggerFrame.ICDFunctionName, "Information cannot retrieved", "ikmsfunctions");				

						// Logging for internal IKMS functions workflow diagram
						logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationExchangeName, "Information is not stored or indexed", "ikmsfunctions");				

						// Logging for internal IKMS functions workflow diagram
						logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "Information is not stored or indexed", "entities2ikms");				

						System.out.println ("Information not in the storage or indexed");
						return null;
					} else {
						// Collecting information for uri:(uri) from location:(locationURL)
						// Uses REST call to retrieve a single value from a remote Entity

						// Logging for internal IKMS functions workflow diagram
						logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationExchangeName, "Retrieve Information", "ikmsfunctions");				

						output = informationCollectionOperation.CollectInformationUsingPull (entityid, locationURL, uri);

						if (output==null) {
							// Logging for internal IKMS functions workflow diagram
							logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationExchangeName, "Notifying for collection incapability", "ikmsfunctions");				

							// Logging for internal IKMS functions workflow diagram
							logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "Cannot collect information", "entities2ikms");				

						} else {
							if (! activeGoal.CheckOptimizationRule(OptimizationRules.DoNotStoreWithoutANeed)) {
								// Logging for internal IKMS functions workflow diagram
								logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Storing information", "ikmsfunctions");				
								// Storing value in main storage
								try {
									informationStorageAndIndexingFunction.informationStorage.StoreInformationInMainStorage(uri, new JSONObject(output));
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}

							// Logging for internal IKMS functions workflow diagram
							logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationExchangeName, "Passing collected information", "ikmsfunctions");				

							// Logging for internal IKMS functions workflow diagram
							logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "Information received", "entities2ikms");				
						}
						return CheckIfNeedsCompacting (activeGoal, output);				
					}

				} else {

					// Logging for internal IKMS functions workflow diagram
					logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ISIName, LoggerFrame.ICDFunctionName, "Information retrieved from storage", "ikmsfunctions");				

					// Logging for internal IKMS functions workflow diagram
					logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationExchangeName, "Information passed to Entity", "ikmsfunctions");				
					// Logging for internal IKMS functions workflow diagram
					logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "Information received", "entities2ikms");				

					System.out.println ("Information retrieved from storage.");
					return CheckIfNeedsCompacting (activeGoal, output);
				}			
			}
		}
	}

	private String CheckIfNeedsCompacting (IKMSOptimizationGoal goal, String result) {
		if (goal.CheckOptimizationRule(OptimizationRules.LightweightDataStructures))
			return result+"=compact";

		return result;
	}

	// Subscribe to an Information value (pub/sub)
	// passes the id of the requesting Entity
	public String SubscribeForInformation (int entityid, String cburl, String uri) {
		@SuppressWarnings("unused")
		String output;

		// Checking if the particular Entity is authorized
		if (informationAuthorizationOperation.CheckAuthorization(entityid, uri, "required")==false) {
			// Not granted
			System.out.println ("Not Granted.");
			return null;
		}
		// Authorization granted
		System.out.println ("Granted.");

		output = informationCollectionOperation.CollectInformationUsingSubscribe(entityid, uri);

		// Subscribes to the particular information
		informationStorageAndIndexingFunction.informationStorage.SubscribeForAnInformationToMainStorage(cburl, uri, entityid);
		return "OK";
	}

	// Request an ArrayList of Information values using the Pull Method
	// passes the id of the requesting Entity
	/*public ArrayList RequestInformationSet (int entityid, ArrayList<String> uris) {
		// Checks if the Entity is authorized
		if (informationAuthorizationOperation.CheckAuthorization(entityid,uris)==false) {
			// Not granted
			System.out.println ("Not Granted.");
			return null;
		}
		// Authorization granted
		System.out.println ("Granted.");

		return null;
	}*/

	// Request IKMS to Collect an ArrayList of Information values and to apply an Aggregation Function using the Pull Method
	// passes the id of the requesting Entity
	public String RequestAggregatedInformation (int entityid, ArrayList<String> uris, String aggregationFunction) {
		// Checks if the Entity is authorized
		if (informationAuthorizationOperation.CheckAggregationAuthorization(entityid,uris,aggregationFunction)==false) {
			// Not granted
			return null;
		}
		// Authorization granted

		return null;
	}

	// Request IKMS to Collect an ArrayList of Information values and to apply a Knowledge Production Algorithm using the Pull Method
	// passes the id of the requesting Entity
	public String RequestKnowledgeProduction (int entityid, ArrayList<String> uris, String knowledgeProductionAlgorithm) {
		// Checks if the Entity is authorized
		if (informationAuthorizationOperation.CheckKnowledgeProductionAuthorization(entityid, uris, knowledgeProductionAlgorithm)==false) {
			// Not granted
			return null;
		}
		// Authorization granted
		return null;
	}
}
