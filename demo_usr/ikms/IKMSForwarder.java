package demo_usr.ikms;

import java.util.Scanner;

import us.monoid.json.JSONObject;
import usr.applications.Application;
import usr.applications.ApplicationResponse;
import demo_usr.ikms.TFTP.RestOverTFTPClient;
import demo_usr.ikms.client.IKMSEnabledUSREntity;
import demo_usr.ikms.client.utils.Converters;

// A distributed KNOW client for the virtual infrastructure (i.e., hosted in a virtual router)
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
	public ApplicationResponse init(String[] args) {

		if (args.length == 1) {
			// try entityid
			Scanner scanner = new Scanner(args[0]);

			if (scanner.hasNextInt()) {
				entityid = scanner.nextInt();
			} else {
				scanner.close();
				return new ApplicationResponse(false, "Bad entityid " + args[0]);
			}
			scanner.close();
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
		System.out.println ("Stopping running the IKMSForwarder");
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
			Delay (entityid, 5000);			
		}
		System.out.println ("The IKMSForwarder stopped running");

		running=false;
	}

	public void InformationFlowPoliciesUpdatedUSR (JSONObject informationFlowPolicies, String targetURIFileName) {
		// Should forward informationFlowPolicies to the appropriate NEM
		// Using the TFTPClient and POST method

		// extract virtual router address from port
		int port = Converters.ExtractPortFromTargetURIFileName (targetURIFileName);
		int hostName = port-10000;
		// TFTP Server port is 2000x
		port+=10000;
		tftpClient.setHostName(String.valueOf(hostName));
		tftpClient.setPort(port);
		//tftpClient
		boolean success = tftpClient.UploadRestPostRequest(targetURIFileName, informationFlowPolicies.toString());
		System.out.println ("Forwarded updated informationFlowPolicies, successfullness:"+success);
	}
}

