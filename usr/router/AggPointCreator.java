package usr.router;

import usr.applications.ApplicationResponse;
import usr.logging.Logger;
import usr.logging.USR;

public class AggPointCreator implements AP {
    RouterController controller;
    RouterOptions options_;


    int ap_ = 0; // The aggregation point for this node
    String apName_ = null;
    String monGenName_ = null;

    // Counts ensure that Info Source and Aggreg Points have unique names
    int isCount_ = 1;
    int apCount_ = 1;




    public AggPointCreator(RouterController rc) {
        controller = rc;
        options_ = controller.getRouterOptions();
    }

    /**
     * Actually set the AP 
     * with a handle back to a RouterController
     */
    public int setAP(int gid, int ap) {
        if (ap == ap_) {  // No change to AP
            return ap;
        }

        int result = internalSetAP(gid, ap);
        return result;
    }


    private int internalSetAP(int gid, int ap) {
        if (monGenName_ != null) { // stop previous monitoring generator
            //System.err.println("APP STOP");
            controller.appStop(monGenName_);

        }

        if (gid == ap) {  // If this is becoming an AP then start an AP
            startAP(ap);
        } else if (ap_ == gid) { // If this WAS an AP and is no longer then stop an AP
            stopAP();
        }
        ap_ = ap;

        // Now start an info source pointing at the new AP.
        String command = new String("plugins_usr.aggregator.appl.InfoSource -o "+ap+
                                    "/3000 -t 1 -d 3");
        command += (" -p "+options_.getMonType());    // What type of data do we monitor
        //command+= (" -n info-source-"+gid+"-"+isCount_);  // Make source name unique
        command += (" -n info-source-"+gid);  // Make source name

        if (options_.getAPFilter() != null) {
            command += (" -f "+options_.getAPFilter());             // Filter output
        }

        if (options_.getAPOutputPath() != null) {
            command += " -l "+ options_.getAPOutputPath();
        }
        ApplicationResponse resp = controller.appStart(command);
        // WAS "/3000 -p rt -t 1 -d 3 -n info-source-"+gid+"-"+isCount_);
        isCount_++;
        monGenName_ = resp.getMessage();
        //System.err.println("NEW NAME "+monGenName_);
        Logger.getLogger("log").logln(USR.STDOUT, leadin()+ gid+ " now has aggregation point "+ap);

        return ap;

    }

    /** This node starts as an AP */
    public void startAP(int gid) {
        synchronized (this) {
            System.out.println(leadin()+ gid+" has become an AP");
            String command = new String("plugins_usr.aggregator.appl.AggPoint -i 0/3000 -t 5 -a average");
            command += (" -n agg-point-"+gid+"-"+apCount_);

            if (options_.getAPOutputPath() != null) {
                command += " -l "+ options_.getAPOutputPath();
            }
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
     * Create the String to print out before a message
     */
    String leadin() {
        return "AggPointCreator: ";
    }


}
