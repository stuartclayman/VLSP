package demo_usr.energy;

import java.util.HashMap;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONObject;
import usr.localcontroller.LocalControllerInfo;
import usr.vim.VimClient;
import demo_usr.energy.energymodel.EnergyModel;

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

			// WE USE THE ENERGYMODEL HERE TO DETERMINE
			// THE APPROPRIATE ROUTING POSITIONS TO DEPLOY THE APPLICATIONS
			
			// A map of LocalController EnergyModels
			HashMap<String, EnergyModel> energyModelsPerLocalController = new HashMap<String, EnergyModel>();
			
			// retrieve local controllers
			JSONArray localControllerIDs = test.listLocalControllers().getJSONArray("list");
			
			JSONArray currentLocalControllerInformation=null;
			EnergyModel currentLocalControllerEnergyModel=null;
			
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
			
			// iterate through all local controllers
			for (int i = 0; i < localControllerIDs.length(); i++) {
				// get information for each localcontroller 
				System.out.println ("Retrieving information for localcontroller:" + localControllerIDs.get(i));
				currentLocalControllerInformation = (JSONArray) test.getLocalControllerInfo(localControllerIDs.get(i).toString()).get("detail");
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
				currentLocalControllerEnergyModel = 	new EnergyModel (cpuLoadCoefficient, cpuIdleCoefficient, memoryAllocationCoefficient, freeMemoryCoefficient, networkOutboundBytesCoefficient, networkIncomingBytesCoefficient, baseLineEnergyConsumption);
				
				// get latest status information for each localcontroller
				currentCPUUserAndSystem = (float) currentLocalControllerInformation.getJSONObject(0).getDouble("cpuLoad");
				currentCPUIdle = (float) currentLocalControllerInformation.getJSONObject(0).getDouble("cpuIdle");
				currentMemoryUsed = currentLocalControllerInformation.getJSONObject(0).getInt("memoryAllocation");
				currentFreeMemory = currentLocalControllerInformation.getJSONObject(0).getInt("freeMemory");
				currentOutputBytes = currentLocalControllerInformation.getJSONObject(0).getLong("networkOutboundBytes");
				currentInputBytes = currentLocalControllerInformation.getJSONObject(0).getLong("networkIncomingBytes");
				
				currentEnergyConsumption = currentLocalControllerEnergyModel.CurrentEnergyConsumption(currentCPUUserAndSystem, currentCPUIdle, currentMemoryUsed, currentFreeMemory, currentOutputBytes, currentInputBytes);

				System.out.println ("Current Energy Consumption of localcontroller:"+localControllerIDs.get(i) + " is "+currentEnergyConsumption+" Watts");
				
				energyModelsPerLocalController.put(localControllerIDs.get(i).toString(), currentLocalControllerEnergyModel);
			}


			// on router3, TFTPServer listening on port 1069
			JSONObject a1 = test.createApp(router3, "plugins_usr.tftp.com.globalros.tftp.server.TFTPServer", "1069");
			System.out.println("a1 = " + a1);

			Thread.sleep(10000);

			// on router1, TFTPClient send to @(3)
			JSONObject a2 = test.createApp(router1, "plugins_usr.tftp.com.globalros.tftp.client.TFTPClient", Integer.toString(router3)); 
			System.out.println("a2 = " + a2);

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
