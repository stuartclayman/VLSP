package ikms.functions;

// The Information Collection and Dissemination (ICD) function is responsible for activities related to information collection, 
// sharing, retrieval and dissemination. 

// The ICD function is the front-end of the IKMS, handling all communication of information between the management 
// entities and the IKMS functional blocks (functions). 

// List of Information Collection and Dissemination operations: 
// - Information collection 
// - Information sharing 
// - Information retrieval
// - Information dissemination

import ikms.IKMS;
import ikms.core.Response;
import ikms.operations.InformationAuthorizationOperation;
import ikms.operations.InformationCollectionOperation;
import ikms.operations.InformationDisseminationOperation;
import ikms.operations.InformationRetrievalOperation;
import ikms.operations.InformationSharingOperation;
import ikms.processor.ProcessorHandle;
import ikms.processor.ProcessorManager;

import java.util.Collection;

import us.monoid.json.JSONObject;

public class InformationCollectionAndDisseminationFunction {
	// The IKMS itself
	IKMS ikms;

	// The ProcessorManager
	ProcessorManager processorManager;

	// Functions it communicates with
	InformationStorageAndIndexingFunction informationStorageAndIndexingFunction = null;

	// Defining ICD operations
	InformationCollectionOperation informationCollection=null;
	InformationDisseminationOperation informationDissemination=null;
	InformationRetrievalOperation informationRetrieval=null;
	InformationSharingOperation informationSharing=null;
	InformationAuthorizationOperation informationAuthorization=null;

	public InformationCollectionAndDisseminationFunction (IKMS ikms) {
		// keep a handle on the IKMS
		this.ikms = ikms;

		// the ProcessorManager
		processorManager = new ProcessorManager(ikms);

		// Functions it communicates with
		informationStorageAndIndexingFunction = ikms.getInformationStorageAndIndexingFunction();

		// Initialize operations
		informationAuthorization = new InformationAuthorizationOperation(informationStorageAndIndexingFunction.entityRegistration);
		informationCollection = new InformationCollectionOperation ();
		informationDissemination = new InformationDisseminationOperation(informationAuthorization);
		informationRetrieval = new InformationRetrievalOperation(informationAuthorization, informationStorageAndIndexingFunction, informationCollection);
		informationSharing = new InformationSharingOperation(informationAuthorization, informationStorageAndIndexingFunction);
	}

	// Request Information value using the Pull Method
	// passes the id of the requesting entity
	public String RequestInformation (int entityid, String uri) {
		return informationRetrieval.RequestInformation(entityid, uri);
	}
	
	// Request an ArrayList of Information values using the Pull Method
	// passes the id of the requesting entity
	//public ArrayList<String> RequestInformationSet (int entityid, ArrayList<String> uris) {
	//	return informationRetrieval.RequestInformationSet(entityid, uris);
	//}

	// Request to Collect an ArrayList of Information values and to apply an Aggregation Function using the Pull Method
	// passes the id of the requesting entity
	//public String RequestAggregatedInformation (int entityid, ArrayList<String> uris, String aggregationFunction) {
	//	return informationRetrieval.RequestAggregatedInformation(entityid, uris, aggregationFunction);
	//}

	// Request to Collect an ArrayList of Information values and to apply a Knowledge Production Algorithm using the Pull Method
	// passes the id of the requesting entity
	//public String RequestKnowledgeProduction (int entityid, ArrayList uris, String knowledgeProductionAlgorithm) {
	//	return informationRetrieval.RequestKnowledgeProduction(entityid, uris, knowledgeProductionAlgorithm);
	//}

	// Share an Information value with IKMS
	// passes the id of the entity sharing the information
	public String ShareInformation (int entityid, String uri, JSONObject value) {
		return informationSharing.ShareInformation(entityid, uri, value);
	}

	// Publish an Information value to IKMS
	// passes the id of the entity sharing the information
	public long PublishInformation (int entityid, String uri, JSONObject value) {
		return informationSharing.PublishInformation(entityid, uri, value);
	}

	// Subscribes for an Information value to IKMS
	// passes the id of the entity sharing the information
	public String SubscribeForInformation (int entityid, String cburl, String uri) {
		return informationRetrieval.SubscribeForInformation(entityid, cburl, uri);
	}

	// Share an ArrayList of Information values with IKMS
	// passes the id of the entity sharing the information
	//public ArrayList<String> ShareInformationSet (int entityid, ArrayList<String> uris, ArrayList<JSONObject> values) {
	//	return informationSharing.ShareInformationSet(entityid, uris, values);
	//}


	/**
	 * Entry point to start an Processor.
	 * Returns an Response with an proc name in it.
	 * The procName is used to stop the proc, and is of the form
	 * /IKMS/Processor/kb.processor.RouterMeasurementProcessor/1
	 */
	public Response startProcessor(String className, String[] args) {
		// args should be class name + args for class

		return processorManager.startProcessor(className, args);
	}

	/**
	 * Entry point to stop an Processor.
	 * Processor name is passed in, in the form
	 * /IKMS/Processor/kb.processor.RouterMeasurementProcessor/1
	 */
	public Response stopProcessor(String procName) {
		return processorManager.stopProcessor(procName);
	}

	/**
	 * Entry point to list Processors.
	 */
	public Collection<ProcessorHandle> listProcessors() {
		return processorManager.listProcessors();
	}
}
