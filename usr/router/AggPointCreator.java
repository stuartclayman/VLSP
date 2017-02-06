package usr.router;

import usr.applications.ApplicationResponse;
import usr.logging.Logger;
import usr.logging.USR;

/**
 * The AggPointCreator is an AP that starts an Aggregation Point.
 *
 */
public class AggPointCreator implements AP {
    RouterController controller;
    RouterOptions options_;


    int ap_ = 0; // The aggregation point for this node
    String apName_ = null;
    String infoSourceName_ = null;

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
    public int setAP(int gid, int ap, String[] ctxArgs) {
        if (ap == ap_) {  // No change to AP
            return ap;
        }

        return internalSetAP(gid, ap);
    }


    private int internalSetAP(int gid, int ap) {
        stopInfoSource(); // stop previous monitoring generator

        if (gid == ap) {  // If this is becoming an AP then start an AP
            startAP(ap);
        } else if (gid == ap_) { // If this WAS an AP and is no longer then stop an AP
            stopAP();
        }

        // Now start an info source pointing at the new AP.
        startInfoSource(gid, ap);

        ap_ = ap;

        //System.err.println("NEW NAME "+infoSourceName_);
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

        delay(50);

    }

    /** This node stops as an AP*/
    public void stopAP() {
        synchronized (this) {
            System.out.println(leadin()+ap_+" has stopped being an AP");
            controller.appStop(apName_);
            apName_ = null;
        }

        delay(50);
    }


    /**
     * Start an Info Source
     */
    public void startInfoSource(int gid, int ap) {
        String command = new String("plugins_usr.aggregator.appl.InfoSource -o "+ap+
                                    "/3000 -t 1 -d 30");
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
        infoSourceName_ = resp.getMessage();

        delay(50);
    }


    /**
     * Stop an info  source
     */
    public void stopInfoSource() {
        if (infoSourceName_ != null) { 
            //System.err.println("APP STOP");
            controller.appStop(infoSourceName_);

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
        return "AggPointCreator: ";
    }


}
