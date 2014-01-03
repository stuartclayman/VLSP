package ikms.operations;

import ikms.functions.InformationStorageAndIndexingFunction;
import ikms.util.LoggerFrame;
import us.monoid.json.JSONObject;

// An information source may share information to the IKMS (or update existing information). The information 
// sharing is triggered from the information sources.

public class InformationSharingOperation {

	// Communicating operations
	InformationAuthorizationOperation informationAuthorizationOperation = null;
	InformationStorageAndIndexingFunction informationStorageAndIndexingFunction = null;

	// The InformationSharingOperation constructor
	public InformationSharingOperation (InformationAuthorizationOperation informationAuthorizationOperation_, InformationStorageAndIndexingFunction informationStorageAndIndexingFunction_) {
		// Initializing communicating operations
		informationAuthorizationOperation = informationAuthorizationOperation_;
		
		// Initializing communicating functions
		informationStorageAndIndexingFunction = informationStorageAndIndexingFunction_;
	}

	// Share an Information value with IKMS
	// passes the id of the entity sharing the information
	public String ShareInformation (int entityid, String uri, JSONObject value) {
		String output = null;
		System.out.println("Information shared, value:"+value.toString());
		// Checking if the particular entity is authorized
		
		// Logging for internal IKMS functions workflow diagram
		//Boolean logoutput = LoggerFrame.testlog(LoggerFrame.ICDFunctionName, LoggerFrame.ISIENTITYStorageName, "Checking Authorization", "black");				
		@SuppressWarnings("unused")
		Boolean logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Checking Authorization", "ikmsfunctions");				
		
		if (informationAuthorizationOperation.CheckAuthorization(entityid, uri, "provided")==false) {
			// Not granted
			System.out.println ("Not Granted.");
			
			// Logging for internal IKMS functions workflow diagram
			//logoutput = LoggerFrame.testlog(LoggerFrame.ISIENTITYStorageName, LoggerFrame.ICDFunctionName, "Access not granted", "black");				
			logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ISIName, LoggerFrame.ICDFunctionName, "Access not granted", "ikmsfunctions");				

			// Logging for internal IKMS functions workflow diagram
			logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationExchangeName, "Access not granted", "ikmsfunctions");				

			// Logging for entity2entity IKMS functions workflow diagram
			logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "Access not granted", "entities2ikms");				
			
			return null;
		}
		// Authorization granted
		System.out.println ("Granted.");
		// Logging for internal IKMS functions workflow diagram
		//logoutput = LoggerFrame.testlog(LoggerFrame.ISIENTITYStorageName, LoggerFrame.ICDFunctionName, "Granted", "black");				
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ISIName, LoggerFrame.ICDFunctionName, "Granted", "ikmsfunctions");				
		
		// Logging for internal IKMS functions workflow diagram
		//logoutput = LoggerFrame.testlog(LoggerFrame.ICDFunctionName, LoggerFrame.ISIInfoStorageName, "Storing Information", "black");				
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Storing Information", "ikmsfunctions");				
		
		// Storing value in main storage
		output = informationStorageAndIndexingFunction.informationStorage.StoreInformationInMainStorage(uri, value);
	
		// Logging for internal IKMS functions workflow diagram
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationExchangeName, "Information stored", "ikmsfunctions");				
		// Logging for internal IKMS functions workflow diagram
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "Information stored", "entities2ikms");				
		
		return output;
	}
	
	// Publishes an Information value to IKMS
	// passes the id of the entity sharing the information
	public long PublishInformation (int entityid, String uri, JSONObject value) {
		long output = 0;
		@SuppressWarnings("unused")
		Boolean logoutput;
		System.out.println("Information published, value:"+value.toString());

		// Checking if the particular Entity is authorized
		if (informationAuthorizationOperation.CheckAuthorization(entityid, uri, "provided")==false) {
			// Not granted
			System.out.println ("Not Granted.");
			
			// Logging for internal IKMS functions workflow diagram
			//logoutput = LoggerFrame.testlog(LoggerFrame.ISIENTITYStorageName, LoggerFrame.ICDFunctionName, "Access not granted", "black");				
			logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ISIName, LoggerFrame.ICDFunctionName, "Access not granted", "ikmsfunctions");				
			
			// Logging for internal IKMS functions workflow diagram
			logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationExchangeName, "Access not granted", "ikmsfunctions");				

			// Logging for internal IKMS functions workflow diagram
			logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "Access not granted", "entities2ikms");				
		
			return 0;
		}
		// Authorization granted
		System.out.println ("Granted.");

		// Logging for internal IKMS functions workflow diagram
		//logoutput = LoggerFrame.testlog(LoggerFrame.ISIENTITYStorageName, LoggerFrame.ICDFunctionName, "Granted", "black");				
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ISIName, LoggerFrame.ICDFunctionName, "Granted", "ikmsfunctions");				
		
		// Logging for internal IKMS functions workflow diagram
		//logoutput = LoggerFrame.testlog(LoggerFrame.ICDFunctionName, LoggerFrame.ISIInfoStorageName, "Publishing Information", "black");				
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.ISIName, "Publishing Information", "ikmsfunctions");				

		// Storing value in main storage
		output = informationStorageAndIndexingFunction.informationStorage.PublishInformationToMainStorage(uri, value);
	
		// Logging for internal IKMS functions workflow diagram
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.ICDFunctionName, LoggerFrame.InformationExchangeName, "Information published", "ikmsfunctions");				
		// Logging for internal IKMS functions workflow diagram
		logoutput = LoggerFrame.workflowvisualisationlog(entityid, LoggerFrame.IKMSName, "", "Information published", "entities2ikms");				
		
		return output;
	}

	// Share an ArrayList of Information values with IKMS
	// passes the id of the Entity sharing the information
	/*public ArrayList ShareInformationSet (int entityid, ArrayList<String> uris, ArrayList<JSONObject> values) {
		// Checks if the Entity is authorized
		if (informationAuthorizationOperation.CheckAuthorization(entityid,uris)==false) {
			// Not granted
			System.out.println ("Not Granted.");
			return null;
		}
		// Authorization granted
		System.out.println ("Granted.");
		
		// Storing values in main storage
		informationStorageAndIndexingFunction.informationStorage.StoreInformationSetInMainStorage(uris, values);
		
		return null;
	}*/
}
