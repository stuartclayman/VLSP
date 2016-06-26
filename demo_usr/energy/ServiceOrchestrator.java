package demo_usr.energy;

import java.util.HashMap;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import usr.vim.VimClient;
import demo_usr.energy.energymodel.EnergyModelLinear;
import demo_usr.energy.energymodel.EnergyModelLinear.ImpactLevel;

public class ServiceOrchestrator {

	public static void main(String[] args) {
		ServiceOrchestrator serviceOrchestrator =  new ServiceOrchestrator();
		serviceOrchestrator.TFTPTest();;
	}



	public void TFTPTest () {
		try {
			VimClient test = new VimClient();

			JSONObject r1 = test.createRouter();
			int router1 = (Integer)r1.get("routerID");
			System.out.println("r1 = " + r1);

			JSONObject r2 = test.createRouter();
			int router2 = (Integer)r2.get("routerID");
			System.out.println("r2 = " + r2);

			JSONObject l1 = test.createLink(router1, router2, 10);
			int link1 = (Integer)l1.get("linkID");
			System.out.println("l1 = " + l1);


			JSONObject r3 = test.createRouter();
			int router3 = (Integer)r3.get("routerID");
			System.out.println("r3 = " + r3);

			JSONObject l2 = test.createLink(router2, router3, 10);
			int link2 = (Integer)l2.get("linkID");
			System.out.println("l2 = " + l2);


			// let the routing tables propogate
			Thread.sleep(12000);

			// deploy applications in an energy efficient way
			// FTP SERVER has a low impact on processing and memory resources, medium impact on incoming traffic but high impact on outgoing traffic
			int serverVM = DeployEnergyEfficientApplication (test, "plugins_usr.tftp.com.globalros.tftp.server.TFTPServer", "1069", ImpactLevel.Low, ImpactLevel.Low, ImpactLevel.Medium, ImpactLevel.High);

			Thread.sleep(10000);
			
			// FTP CLIENT has a low impact on processing and memory resources, high impact on incoming traffic but medium impact on outgoing traffic
			int clientVM = DeployEnergyEfficientApplication (test, "plugins_usr.tftp.com.globalros.tftp.client.TFTPClient", Integer.toString(serverVM), ImpactLevel.Low, ImpactLevel.Low, ImpactLevel.High, ImpactLevel.Medium);

			
			// previous way to deploy them
			/*
			// on router3, TFTPServer listening on port 1069
			JSONObject a1 = test.createApp(router3, "plugins_usr.tftp.com.globalros.tftp.server.TFTPServer", "1069");
			System.out.println("a1 = " + a1);

			Thread.sleep(10000);

			// on router1, TFTPClient send to @(3)
			JSONObject a2 = test.createApp(router1, "plugins_usr.tftp.com.globalros.tftp.client.TFTPClient", Integer.toString(router3)); 
			System.out.println("a2 = " + a2);*/

			/* sleep 30 seconds = 0.5 minute = 3s0000 ms */
			Thread.sleep(30000);

			JSONObject r1D = test.deleteRouter(router1);

			JSONObject r2D = test.deleteRouter(router2);

			JSONObject r3D = test.deleteRouter(router3);

		} catch (Exception e) {
			e.printStackTrace();
		} catch (Error err) {
			err.printStackTrace();
		}

	}

	// returns the chosen router
	private int DeployEnergyEfficientApplication (VimClient vim, String applicationName, String applicationPort, ImpactLevel processingImpact, ImpactLevel memoryImpact, ImpactLevel incomingTrafficImpact, ImpactLevel outgoingTrafficImpact) throws JSONException {
		// WE USE THE ENERGYMODEL HERE TO DETERMINE
		// THE APPROPRIATE ROUTING POSITIONS TO DEPLOY THE APPLICATIONS

		// A map of LocalController EnergyModels
		HashMap<String, EnergyModelLinear> energyModelsPerLocalController = new HashMap<String, EnergyModelLinear>();

		// retrieve local controllers
		JSONArray localControllerIDs = vim.listLocalControllers().getJSONArray("list");

		JSONArray currentLocalControllerInformation=null;
		EnergyModelLinear currentLocalControllerEnergyModel=null;

		// Coefficients per energy model
		// hardware related coefficient for energy consumption of cpu (user+system mode)
		double cpuLoadCoefficient=0;
		// hardware related coefficient for energy consumption of cpu (idle mode)
		double cpuIdleCoefficient=0;
		// hardware related coefficient for energy consumption of used memory
		double memoryAllocationCoefficient=0;
		// hardware related coefficient for energy consumption of unused memory
		double freeMemoryCoefficient=0;
		// hardware related coefficient for energy consumption of network (outbound traffic) - per byte
		double networkOutboundBytesCoefficient=0;
		// hardware related coefficient for energy consumption of network (incoming traffic) - per byte
		double networkIncomingBytesCoefficient=0;
		// average energy consumption of all server devices, besides network, cpu and memory
		double baseLineEnergyConsumption=0;

		// current localcontroller status
		float currentCPUUserAndSystem=0;
		float currentCPUIdle=0;
		int currentMemoryUsed=0;
		int currentFreeMemory=0;
		long currentOutputBytes=0;
		long currentInputBytes=0;

		// current energy consumption
		double currentEnergyConsumption=0;
		
		// minimum energy consumption
		double minimumEnergyConsumption = Double.MAX_VALUE;
		// localcontroller with minimum energy consumption
		String bestLocalController="";
		
		// iterate through all local controllers
		for (int i = 0; i < localControllerIDs.length(); i++) {
			// get information for each localcontroller 
			System.out.println ("Retrieving information for localcontroller:" + localControllerIDs.get(i));
			currentLocalControllerInformation = (JSONArray) vim.getLocalControllerInfo(localControllerIDs.get(i).toString()).get("detail");
			System.out.println (currentLocalControllerInformation);
			// retrieve all coefficients
			cpuLoadCoefficient=currentLocalControllerInformation.getJSONObject(0).getDouble("cpuLoadCoefficient");
			cpuIdleCoefficient=currentLocalControllerInformation.getJSONObject(0).getDouble("cpuIdleCoefficient");
			memoryAllocationCoefficient=currentLocalControllerInformation.getJSONObject(0).getDouble("memoryAllocationCoefficient");
			freeMemoryCoefficient=currentLocalControllerInformation.getJSONObject(0).getDouble("freeMemoryCoefficient");
			networkOutboundBytesCoefficient=currentLocalControllerInformation.getJSONObject(0).getDouble("networkOutboundBytesCoefficient");
			networkIncomingBytesCoefficient=currentLocalControllerInformation.getJSONObject(0).getDouble("networkIncomingBytesCoefficient");
			baseLineEnergyConsumption = currentLocalControllerInformation.getJSONObject(0).getDouble("baseLineEnergyConsumption");

			// add one energy model per localcontroller
			currentLocalControllerEnergyModel = 	new EnergyModelLinear (cpuLoadCoefficient, cpuIdleCoefficient, memoryAllocationCoefficient, freeMemoryCoefficient, networkOutboundBytesCoefficient, networkIncomingBytesCoefficient, baseLineEnergyConsumption);

			// get latest status information for each localcontroller
			currentCPUUserAndSystem = (float) currentLocalControllerInformation.getJSONObject(0).getDouble("cpuLoad");
			currentCPUIdle = (float) currentLocalControllerInformation.getJSONObject(0).getDouble("cpuIdle");
			currentMemoryUsed = currentLocalControllerInformation.getJSONObject(0).getInt("memoryAllocation");
			currentFreeMemory = currentLocalControllerInformation.getJSONObject(0).getInt("freeMemory");
			currentOutputBytes = currentLocalControllerInformation.getJSONObject(0).getLong("networkOutboundBytes");
			currentInputBytes = currentLocalControllerInformation.getJSONObject(0).getLong("networkIncomingBytes");

			currentEnergyConsumption = currentLocalControllerEnergyModel.CurrentEnergyConsumption(currentCPUUserAndSystem, currentCPUIdle, currentMemoryUsed, currentFreeMemory, currentOutputBytes, currentInputBytes);

			System.out.println ("Current Energy Consumption of localcontroller:"+localControllerIDs.get(i) + " is "+currentEnergyConsumption+" Watts");

			// NOW THE GOAL IS TO PREDICT THE IMPACT ON THE ENERGY CONSUMPTION OF EACH PHYSICAL SERVER
			// AFTER THE APPLICATION DEPLOYMENT
			// FOR EACH APPLICATION WE ASSUME THE LEVEL OF IMPACT ON PROCESSING, MEMORY AND NETWORK RESOURCES (LOW, MEDIUM, HIGH).
			// OR WE KEEP HISTORICAL INFORMATION OF AVERAGE PROCESSING, MEMORY AND NETWORK RESOURCES UTILISATION
			
			// reset application coefficients - adding some indicative default values
			float HighProcessingImpactValueCoefficient = 0.10F;
			float MediumProcessingImpactValueCoefficient = 0.05F;
			float LowProcessingImpactValueCoefficient = 0.01F;
			int HighMemoryImpactValueCoefficient = 1000; //1GB
			int MediumMemoryImpactValueCoefficient = 500; //500MB
			int LowMemoryImpactValueCoefficient = 50; //50MB
			long HighOutgoingTrafficValueCoefficient = 1000000000; //100MB
			long MediumOutgoingTrafficValueCoefficient = 10000000; //10MB
			long LowOutgoingTrafficValueCoefficient = 100000; //100KB
			long HighIncomingTrafficValueCoefficient = 1000000000; //100MB
			long MediumIncomingTrafficValueCoefficient = 10000000; //10MB
			long LowIncomingTrafficValueCoefficient = 100000; //100KB
			
			// configure application coefficients in the energy model
			currentLocalControllerEnergyModel.ConfigureApplicationCoefficients(HighProcessingImpactValueCoefficient, MediumProcessingImpactValueCoefficient, LowProcessingImpactValueCoefficient, HighMemoryImpactValueCoefficient, MediumMemoryImpactValueCoefficient, LowMemoryImpactValueCoefficient, HighOutgoingTrafficValueCoefficient, MediumOutgoingTrafficValueCoefficient, LowOutgoingTrafficValueCoefficient, HighIncomingTrafficValueCoefficient, MediumIncomingTrafficValueCoefficient, LowIncomingTrafficValueCoefficient);
			
			// calculate predicted energy consumption for the specific application
			double currentPredictedEnergyConsumption = currentLocalControllerEnergyModel.PredictEnergyConsumptionAfterApplicationDeploymentFromLevelOfImpact(processingImpact, memoryImpact, outgoingTrafficImpact, incomingTrafficImpact);
			
			System.out.println ("Predicted Energy Consumption of localcontroller after application deployment:"+localControllerIDs.get(i) + " is "+currentPredictedEnergyConsumption+" Watts");
			
			if (minimumEnergyConsumption > currentPredictedEnergyConsumption) {
				minimumEnergyConsumption = currentPredictedEnergyConsumption;
				bestLocalController = localControllerIDs.getString(i);
			}
			
			energyModelsPerLocalController.put(localControllerIDs.get(i).toString(), currentLocalControllerEnergyModel);
		}

		System.out.println ("Best localcontroller for application is:"+bestLocalController+" consuming energy:"+minimumEnergyConsumption+" Watts");
		// deploy application and return the chosen router id
		return DeployApplicationInLocalController (vim, applicationName, applicationPort, bestLocalController);
	}

	// deploy application on specific localcontroller (choose a VM randomly)
	private int DeployApplicationInLocalController (VimClient vim, String applicationName, String applicationPort, String localControllerName) {
		// retrieve existing VMs
		
		// select one virtual machine randomly
		
		// deploy application and return the chosen router id
		return 0;
	}
	
	public void RestyTest () {
		try {

			VimClient test = new VimClient();

			JSONObject r1 = test.createRouter();
			int router1 = (Integer)r1.get("routerID");
			System.out.println("r1 = " + r1);

			JSONObject r2 = test.createRouter();
			int router2 = (Integer)r2.get("routerID");
			System.out.println("r2 = " + r2);

			JSONObject r3 = test.createRouter();
			int router3 = (Integer)r3.get("routerID");
			System.out.println("r3 = " + r3);

			JSONObject r4 = test.createRouter();
			int router4 = (Integer)r4.get("routerID");
			System.out.println("r4 = " + r4);

			JSONObject r5 = test.createRouter();
			int router5 = (Integer)r5.get("routerID");
			System.out.println("r5 = " + r5);

			JSONObject r6 = test.createRouter();
			int router6 = (Integer)r6.get("routerID");
			System.out.println("r6 = " + r6);

			JSONObject rSRC = test.createRouter();
			int routerS = (Integer)rSRC.get("routerID");
			System.out.println("rSRC = " + rSRC);

			JSONObject rDST = test.createRouter();
			int routerD = (Integer)rDST.get("routerID");
			System.out.println("rDST = " + rDST);


			JSONObject l1 = test.createLink(router1, router2, 10);
			int link1 = (Integer)l1.get("linkID");
			System.out.println("l1 = " + l1);

			JSONObject l2 = test.createLink(router1, router3, 10);
			int link2 = (Integer)l2.get("linkID");
			System.out.println("l2 = " + l2);

			JSONObject l3 = test.createLink(router2, router4, 10);
			l3.get("linkID");
			System.out.println("l3 = " + l3);

			JSONObject l4 = test.createLink(router3, router5, 10);
			l4.get("linkID");
			System.out.println("l4 = " + l4);

			JSONObject l5 = test.createLink(router4, router6, 10);
			l5.get("linkID");
			System.out.println("l5 = " + l5);

			JSONObject l6 = test.createLink(router5, router6, 10);
			l6.get("linkID");
			System.out.println("l6 = " + l6);

			JSONObject lSto1 = test.createLink(routerS, router1, 10);
			lSto1.get("linkID");
			System.out.println("lSto1 = " + lSto1);

			JSONObject lDto6 = test.createLink(routerD, router6, 10);
			lDto6.get("linkID");
			System.out.println("lDto6 = " + lDto6);


			// let the routing tables propogate
			Thread.sleep(60000);

			// on routerD, Recv on port 4000
			JSONObject a1 = test.createApp(routerD, "usr.applications.RecvDataRate", "4000");
			System.out.println("a1 = " + a1);
			Thread.sleep(500);

			// send from routerS to routerD on port 4000
			JSONObject a2 = test.createApp(routerS, "usr.applications.Send", routerD + " 4000 2500000 -s 1024 -i 1");  // id+4000+count
			System.out.println("a2 = " + a2);


			/* sleep 60 seconds = 1 minute = 60000 ms */

			/* After 1 minute set a link weight */
			Thread.sleep(60000);

			JSONObject l1W = test.setLinkWeight(link1, 20);
			System.out.println("l1W = " + l1W);


			/* After 1 more minute reset link weight
           and set a wieght on a different link */
			Thread.sleep(60000);

			JSONObject l2W = test.setLinkWeight(link2, 20);   // now 20
			System.out.println("l2W = " + l2W);
			JSONObject l1WW = test.setLinkWeight(link1, 10);   // back to 10
			System.out.println("l1WW = " + l1WW);



			Thread.sleep(300000);

			test.deleteRouter(router1);

			test.deleteRouter(router2);

			test.deleteRouter(router3);

			test.deleteRouter(router4);

			test.deleteRouter(router5);

			test.deleteRouter(router6);

			test.deleteRouter(routerS);

			test.deleteRouter(routerD);
		} catch (Exception e) {
		} catch (Error err) {
		}

	}

}
