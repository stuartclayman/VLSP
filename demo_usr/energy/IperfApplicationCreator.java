package demo_usr.energy;

import usr.applications.ApplicationResponse;
import usr.logging.Logger;
import usr.logging.USR;
import usr.router.AP;
import usr.router.RouterController;

/**
 * The IperfApplicationCreator is an AP that starts an iperf applications
 * communicating.
 *
 */
public class IperfApplicationCreator implements AP {
	RouterController controller;
	// RouterOptions options_;

	int ap_ = 0; // The aggregation point for this node
	String apName_ = null;
	String iperfSourceName_ = null;

	// Counts ensure that Info Source and Aggreg Points have unique names
	int isCount_ = 1;
	int apCount_ = 1;

	public IperfApplicationCreator(RouterController rc) {
		controller = rc;
		// options_ = rc.getRouterOptions();
	}

	/**
	 * Actually set the AP with a handle back to a RouterController
	 */
	public int setAP(int gid, int ap) {
		if (ap == ap_) { // No change to AP
			return ap;
		}

		return internalSetAP(gid, ap);
	}

	private int internalSetAP(int gid, int ap) {
		stopIperfSource(); // stop previous monitoring generator

		if (gid == ap) { // If this is becoming an AP then start an AP
			startAP(ap);
		} else {
			if (gid == ap_) { // If this WAS an AP and is no longer then stop
								// an AP
				stopAP();
			}
			// this is not an iperf sink, so create an iperf source
			 // Now start an iperf source pointing at the new AP.
	        startIperfSource(gid, ap);
		}
		ap_ = ap;

		Logger.getLogger("log").logln(USR.STDOUT,
				leadin() + gid + " now hosts an iperf application " + ap);

		return ap;

	}

	/** This node starts as an AP (iperf sink) */
	public void startAP(int gid) {
		synchronized (this) {
			System.out.println(leadin() + gid + " has become an iperf sink");

			String command = new String("demo_usr.iperf.Iperf -s");

			ApplicationResponse resp = controller.appStart(command);

			apCount_++;
			apName_ = resp.getMessage();
		}

		delay(50);
	}

	/** This node stops as an AP */
	public void stopAP() {
		synchronized (this) {
			System.out.println(leadin() + ap_
					+ " has stopped being an AP (iperf sink)");
			controller.appStop(apName_);
			apName_ = null;
		}

		delay(50);
	}

	/**
	 * Start an iperf source application
	 */
	public void startIperfSource(int gid, int ap) {

		String command = new String("demo_usr.iperf.Iperf -s"); // specify
																// server
																// address and
																// transmission
																// rate

		ApplicationResponse resp = controller.appStart(command);
		System.out.println(leadin() + controller.findAppInfo("demo_usr.iperf.Iperf"));
		isCount_++;
		iperfSourceName_ = resp.getMessage();

		delay(50);

	}

	/**
	 * Stop an iperf source application
	 */
	public void stopIperfSource() {
		if (iperfSourceName_ != null) {
			// System.err.println("APP STOP");
			controller.appStop(iperfSourceName_);

			delay(250);
		}
	}

	private void delay(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException ie) {
		}
	}

	/**
	 * Create the String to print out before a message
	 */
	String leadin() {
		return "IperfApplicationCreator: ";
	}

}
