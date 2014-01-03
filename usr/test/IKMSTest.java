package usr.test;

import us.monoid.json.JSONObject;
import usr.vim.VimClient;

/**
 * Test some calls to GlobalController using Resty
 */
class IKMSTest  {
	public static void main(String[] args) {
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

			JSONObject l1 = test.createLink(router1, router2, 10);
			int link1 = (Integer)l1.get("linkID");
			System.out.println("l1 = " + l1);

			JSONObject l2 = test.createLink(router2, router3, 10);
			int link2 = (Integer)l2.get("linkID");
			System.out.println("l2 = " + l2);

			// let the routing tables propogate
			Thread.sleep(12000);

			JSONObject a1 = test.createApp(router2, "demo_usr.ikms.IKMSForwarder", "10002 20002");
			System.out.println("a1 = " + a1);           

			Thread.sleep(10000);
			
			//JSONObject a2 = test.createApp(router1, "demo_usr.ikms.DirectSinkMA", "10001 20001 2");
			//JSONObject a2 = test.createApp(router1, "demo_usr.ikms.InformationRetrievalMA", "10001 20001 2");
			//JSONObject a2 = test.createApp(router1, "demo_usr.ikms.InformationSubscribeMA", "10001 20001 2");
			//JSONObject a2 = test.createApp(router1, "demo_usr.ikms.GenericSinkMA", "10001 20001 2");
			JSONObject a2 = test.createApp(router1, "demo_usr.ikms.GenericSinkMA", "2000 3000 2 1000 130000 /test0/All 3 4");

			System.out.println("a2 = " + a2);

			Thread.sleep(10000);
			
			//JSONObject a3 = test.createApp(router3, "demo_usr.ikms.DirectSourceMA", "10003 20003 2");
			//JSONObject a3 = test.createApp(router3, "demo_usr.ikms.InformationSharingMA", "10003 20003 2");
			//JSONObject a3 = test.createApp(router3, "demo_usr.ikms.InformationPublishMA", "10003 20003 2");
			JSONObject a3 = test.createApp(router3, "demo_usr.ikms.GenericSourceMA", "10003 20003 2");

			System.out.println("a3 = " + a3);
			
			/* sleep 60 seconds = 1 minute = 60000 ms */
			Thread.sleep(300000);

			JSONObject r1D = test.deleteRouter(router1);

			JSONObject r2D = test.deleteRouter(router2);

			JSONObject r3D = test.deleteRouter(router3);


		} catch (Exception e) {
		} catch (Error err) {
		}
	}

}
