package demo_usr.ikms;

import java.util.Map;
import java.util.Scanner;

import us.monoid.json.JSONObject;
import usr.applications.Application;
import usr.applications.ApplicationResponse;
import usr.router.AP;
import demo_usr.ikms.TFTP.RestOverTFTPClient;
import demo_usr.ikms.client.IKMSEnabledUSREntity;
import demo_usr.ikms.client.utils.Converters;
import demo_usr.ikms.client.utils.Logging;

// A distributed IKMS client for the virtual infrastructure (i.e., hosted in a virtual router)
public class IKMSForwarder extends IKMSEnabledUSREntity implements Application {

	// TFTP client, i.e., for forwarding REST calls over a virtual path
	RestOverTFTPClient tftpClient;

	public IKMSForwarder () {
		// default entityid value
		entityid = 1002;

		// Initializing TFTP Client
		tftpClient = new RestOverTFTPClient();
	}

	// for a standalone execution, i.e., for testing
	public static void main(String[] args) {
		// Initializing example MA
		IKMSForwarder ma = new IKMSForwarder();       

		// initializes and registers entity
		ma.registerEntity();

		// start entity communication
		ma.run();
	}

	public void registerEntity () {
		// Creating registrationInfo data structure, for this case null
		JSONObject registrationInfo = null;

		// initializes and registers entity
		initializeAndRegister(registrationInfo);
	}

	/**
	 * Initialize with some args
	 */
	@SuppressWarnings("resource")
	public ApplicationResponse init(String[] args) {
		int restOverUSRPort=0;

		if (args.length == 2) {
			// try entityid
			Scanner scanner = new Scanner(args[0]);

			if (scanner.hasNextInt()) {
				entityid = scanner.nextInt();

				scanner = new Scanner(args[1]);

				if (scanner.hasNextInt()) {
					restOverUSRPort = scanner.nextInt();
				} else {
					scanner.close();
					return new ApplicationResponse(false, "Bad knowHost " + args[1]);
				}
			} else {
				scanner.close();
				return new ApplicationResponse(false, "Bad entityid " + args[0]);
			}
			scanner.close();
			// update RestOverUSR port
			restOverUSR.init(restOverUSRPort);
			return new ApplicationResponse(true, "");
		} else {
			return new ApplicationResponse(true, "");
		}

	}

	/**
	 * Start an application.
	 * This is called before run().
	 */
	public ApplicationResponse start() {
		// register entity
		registerEntity ();

		//tftpClient.ApplyRestGetRequest("http://localhost:8080/");
		return new ApplicationResponse(true, "");
	}


	/**
	 * Stop an application.
	 * This is called to implement graceful shut down
	 * and cause run() to end.
	 */
	public ApplicationResponse stop() {
		// stop running entity
		stopRunning=true;
		Logging.Log(entityid, "Stopping running the IKMSForwarder");
		// terminate entity
		shutDown();
		return new ApplicationResponse(true, "");
	}

	public void run() {
		/*System.out.println ("Testing 1st FTP server");
		tftpClient.setHostName("1");
		tftpClient.setPort(1069);
		tftpClient.ApplyRestGetRequest("http://localhost:8080/");

		System.out.println ("Testing 2nd FTP server");
		tftpClient.setHostName("2");
		tftpClient.setPort(1069);
		tftpClient.ApplyRestGetRequest("http://localhost:8080/");*/

		while (stopRunning==false) {
			DelayNoMessage (5000);			
		}
		Logging.Log(entityid, "The IKMSForwarder stopped running");

		running=false;
	}

	@Override
	public void InformationFlowPoliciesUpdatedUSR (JSONObject informationFlowPolicies, String targetURIFileName) {
		try {

			// Should forward informationFlowPolicies to the appropriate Entity
			// Using the TFTPClient and POST method
			Logging.Log(entityid, "informationFlowPolicies:"+informationFlowPolicies+" targetURIFileName:"+targetURIFileName);
			// extract virtual router address from port
			Map<String, String> parameters = Converters.SplitQuery(targetURIFileName);

			//int port = Converters.ExtractPortFromTargetURIFileName (targetURIFileName);
			String hostName = parameters.get("n");
			// TFTP Server port is 2000x
			//port+=10000;
			String port = parameters.get("p");
			tftpClient.setHostName(String.valueOf(hostName));
			tftpClient.setPort(Integer.valueOf(port));
			Logging.Log(entityid, "hostName:"+hostName+" port:"+port);

			//tftpClient
			boolean success = tftpClient.UploadRestPostRequest(targetURIFileName, informationFlowPolicies.toString());
			Logging.Log(entityid, "Forwarded updated informationFlowPolicies, successfullness:"+success);
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	public JSONObject CollectValueUSR (String uri, String targetURIFileName) {
		// get the value through the TFTP
		try {
			// Should forward informationFlowPolicies to the appropriate Entity
			// Using the TFTPClient and GET method
			Logging.Log(entityid, "Collecting information with uri:"+uri+" targetURIFileName:"+targetURIFileName);
			// extract virtual router address from port
			Map<String, String> parameters = Converters.SplitQuery(targetURIFileName);

			//int port = Converters.ExtractPortFromTargetURIFileName (targetURIFileName);
			String hostName = parameters.get("n");
			// TFTP Server port is 2000x
			//port+=10000;
			String port = parameters.get("p");
			tftpClient.setHostName(String.valueOf(hostName));
			tftpClient.setPort(Integer.valueOf(port));
			Logging.Log(entityid, "hostName:"+hostName+" port:"+port);

			//tftpClient
			String result = tftpClient.ApplyRestGetRequest(targetURIFileName+"&u="+uri);
			Logging.Log(entityid, "Retrieved value:"+result);

			JSONObject resultObj = new JSONObject (result);
			return resultObj;
		} catch (Throwable t) {
			t.printStackTrace();
			return null;
		}
	}

	@Override
	public void UpdateValueUSR(String uri, String value, String targetURIFileName) {
		// forwarding update to the corresponding management entity
		// post the value through the TFTP
		try {
			// Using the TFTPClient and POST method
			Logging.Log(entityid, "Notifying ME for information change:"+uri+" targetURI:"+targetURIFileName);
			// extract virtual router address from port
			Map<String, String> parameters = Converters.SplitQuery(targetURIFileName);

			//int port = Converters.ExtractPortFromTargetURIFileName (targetURIFileName);
			String hostName = parameters.get("n");
			// TFTP Server port is 2000x
			//port+=10000;
			String port = parameters.get("p");
			tftpClient.setHostName(String.valueOf(hostName));
			tftpClient.setPort(Integer.valueOf(port));
			Logging.Log(entityid, "hostName:"+hostName+" port:"+port);

			//tftpClient
			Boolean result = tftpClient.ApplyRestPostRequest(targetURIFileName+"&u="+uri, value);
			Logging.Log(entityid, "Information forwarding, result:"+result);
		} catch (Throwable t) {
			t.printStackTrace();
		}	
	}
}

