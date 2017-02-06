package demo_usr.ikms;

import usr.applications.ApplicationResponse;
import usr.logging.Logger;
import usr.logging.USR;
import usr.router.AP;
import usr.router.RouterController;
import usr.router.RouterOptions;

/**
 * The KnowledgeForwarderCreator is an AP that starts an IKMS forwarder.
 *
 */
public class IKMSForwarderCreator implements AP {
    RouterController controller;
   // RouterOptions options_;


    int ap_ = 0; // The aggregation point for this node
    String apName_ = null;
    String infoSourceName_ = null;

    // Counts ensure that Info Source and Aggreg Points have unique names
    int isCount_ = 1;
    int apCount_ = 1;

    public IKMSForwarderCreator(RouterController rc) {
        controller = rc;
        //options_ = rc.getRouterOptions();
    }

    /**
     * Actually set the AP 
     * with a handle back to a RouterController
     */
    public int setAP(int gid, int ap, String[] ctxArgs) {
        if (ap == ap_) {  // No change to AP
            return ap;
        }

        return internalSetAP(gid, ap);
    }


    private int internalSetAP(int gid, int ap) {
        if (gid == ap) {  // If this is becoming an AP then start an AP
            startAP(ap);
        } else if (gid == ap_) { // If this WAS an AP and is no longer then stop an AP
            stopAP();
        }

        ap_ = ap;

        //System.err.println("NEW NAME "+infoSourceName_);
        Logger.getLogger("log").logln(USR.STDOUT, leadin()+ gid+ " now hosts an IKMS forwarder "+ap);

        return ap;

    }

    /** This node starts as an AP */
    public void startAP(int gid) {
        synchronized (this) {
            System.out.println(leadin()+ gid+" has become an IKMSForwarder");
            
            // define IKMSForwarder parameters
			int entityId = 27000+gid;
			int entityRestPort = 28000+gid;
            
            String command = new String("demo_usr.ikms.IKMSForwarder "+entityId+" "+entityRestPort);
            //command += (" -n agg-point-"+gid+"-"+apCount_);

            //if (options_.getAPOutputPath() != null) {
            //    command += " -l "+ options_.getAPOutputPath();
            //}
            ApplicationResponse resp = controller.appStart(command);

            // WAS " -t 5 -a average -n agg-point-"+gid+"-"+apCount_);
            apCount_++;
            apName_ = resp.getMessage();
        }
    }

    /** This node stops as an AP*/
    public void stopAP() {
        synchronized (this) {
            System.out.println(leadin()+ap_+" has stopped being an AP");
            controller.appStop(apName_);
            apName_ = null;
        }
    }


    /**
     * Start an Info Source
     */
    public void startInfoSource(int gid, int ap) {
        // TBD for dynamic scenarios
    }


    /**
     * Stop an info  source
     */
    public void stopInfoSource() {
        if (infoSourceName_ != null) { 
            //System.err.println("APP STOP");
            controller.appStop(infoSourceName_);
        }
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        return "IKMSForwarderCreator: ";
    }


}
