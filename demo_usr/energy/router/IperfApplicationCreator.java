package demo_usr.energy.router;

import usr.applications.ApplicationResponse;
import usr.logging.Logger;
import usr.logging.USR;
import usr.router.AP;
import usr.router.RouterController;
import java.util.Arrays;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

/**
 * The IperfApplicationCreator is an AP that starts an iperf applications
 * communicating.
 *
 */
public class IperfApplicationCreator implements AP {
    RouterController controller;
    // RouterOptions options_;

    int ap_ = 0; // The aggregation point for this node
    String iperfSinkName_ = null;
    String iperfSourceName_ = null;

    // Counts ensure that Info Source and Aggreg Points have unique names
    int isCount_ = 1;
    int apCount_ = 1;


    private final String IPERF_BITS_PER_SECOND = "1M";
    private final String IPERF_TIME = "120";  // seconds


    public IperfApplicationCreator(RouterController rc) {
        controller = rc;
        // options_ = rc.getRouterOptions();
    }

    /**
     * Actually set the AP with a handle back to a RouterController
     */
    public int setAP(int gid, int ap, String[] ctxArgs) {
        if (ap == ap_) { // No change to AP
            return ap;
        }

        //Logger.getLogger("log").logln(USR.STDOUT, leadin() + " received ctxArgs " + Arrays.asList(ctxArgs));

        return internalSetIperfNode(gid, ap, ctxArgs);
    }

    private int internalSetIperfNode(int gid, int ap, String[] ctxArgs) {

        try {
            String jsonData = ctxArgs[0];

            JSONObject jsonObj = new JSONObject(jsonData);

            String gidIP = jsonObj.getString("gidIP");
            String apIP = jsonObj.getString("apIP");


            if (iperfSourceName_ != null) {
                // there is an exisiting iperfSource
                stopIperfSource(); // stop previous iperf generator
            }

            if (gid == ap) { // If this is becoming an AP then start an AP
                startIperfSink(ap);
            } else {
                if (gid == ap_) { // If this WAS an AP and is no longer then stop
                    // an AP
                    stopIperfSink();
                }

                // this is not an iperf sink, so create an iperf source
                // Now start an iperf source pointing at the new AP.
                startIperfSource(gid, ap, gidIP, apIP);
            }
            ap_ = ap;

            Logger.getLogger("log").logln(USR.STDOUT, leadin() + gid + " now hosts an iperf application " + ap);

            return ap;
        } catch (JSONException je) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + je.getMessage());
            return ap;
        }

    }

    /** This node starts as an AP (iperf sink) */
    public void startIperfSink(int gid) {
        synchronized (this) {
            System.out.println(leadin() + gid + " has become an AP (iperf sink)");

            String command = new String("demo_usr.iperf.Iperf -s");

            ApplicationResponse resp = controller.appStart(command);

            apCount_++;
            iperfSinkName_ = resp.getMessage();
            iperfSourceName_ = null;
        }

        delay(50);
    }

    /** This node stops as an AP */
    public void stopIperfSink() {
        synchronized (this) {
            System.out.println(leadin() + ap_ + " has stopped being an AP (iperf sink)");
            controller.appStop(iperfSinkName_);
            iperfSinkName_ = null;
        }

        delay(50);
    }

    /**
     * Start an iperf source application
     */
    public void startIperfSource(int gid, int ap, String gidIP, String apIP) {

        System.out.println(leadin() + gid + " has become an AP (iperf source)");

        // demo_usr.iperf.Iperf -b bits_per_second -c equavalent_ap_ip_address"
        String command = new String("demo_usr.iperf.Iperf " + " -c " + apIP + " -b " + IPERF_BITS_PER_SECOND + " -t " + IPERF_TIME);
        // specify  server  address and  transmission rate

        ApplicationResponse resp = controller.appStart(command);
        isCount_++;
        iperfSourceName_ = resp.getMessage();
        iperfSinkName_ = null;

        System.out.println(leadin() + controller.findAppInfo("demo_usr.iperf.Iperf"));

        delay(50);

    }

    /**
     * Stop an iperf source application
     */
    public void stopIperfSource() {
        if (iperfSourceName_ != null) {
            System.out.println(leadin() + ap_ + " has stopped being an AP (iperf source)");
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
