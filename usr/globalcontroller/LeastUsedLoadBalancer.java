package usr.globalcontroller;

import usr.localcontroller.LocalControllerInfo;

import java.util.Set;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import usr.logging.BitMask;
import usr.logging.USR;
import usr.logging.Logger;
import usr.common.ANSI;

/**
 * The LeastUsedLoadBalancer is responsible for determining the placement
 * of a Router across the active resources.
 * <p>
 * It finds the LocalController with the least no of routers.
 */
public class LeastUsedLoadBalancer implements PlacementEngine {
    // The GlobalController
    GlobalController gc;


    /**
     * Constructor
     */
    public LeastUsedLoadBalancer(GlobalController gc) {
        this.gc = gc;

        // get logger
        try {
            Logger.getLogger("log").addOutput(new PrintWriter(new FileOutputStream("/tmp/gc-channel12.out")), new BitMask(1<<12));
        } catch (FileNotFoundException fnfe) {
            Logger.getLogger("log").logln(USR.ERROR, fnfe.toString());
        }

        Logger.getLogger("log").logln(USR.STDOUT, "LeastUsedLoadBalancer: localcontrollers = " + getPlacementDestinations());
    }

    /**
     * Get the relevant LocalControllerInfo for a placement of a router with 
     * a specified name and address.
     */
    public LocalControllerInfo routerPlacement(String name, String address) {
        LocalControllerInfo leastUsed = null;

        double minUse = 0.0;
        double thisUsage = 0.0;

        long elapsedTime = gc.getElapsedTime();

        // now work out placement
        for (LocalControllerInfo localInfo : getPlacementDestinations()) {
            thisUsage = localInfo.getUsage(); // same as localInfo.getNoRouters() / localInfo.getMaxRouters()

            //Logger.getLogger("log").logln(USR.STDOUT, localInfo +" Usage "+thisUsage);

            if (thisUsage == 0.0) {  // found an empty host
                leastUsed = localInfo;
                break;
            }

            if (thisUsage < minUse || leastUsed == null) {
                minUse = thisUsage;
                leastUsed = localInfo;
            }
        }

        // log current values
        Logger.getLogger("log").logln(1<<10, toTable(elapsedTime));

        if (minUse >= 1.0) {
            Logger.getLogger("log").logln(1<<12, gc.elapsedToString(elapsedTime) + ANSI.RED +  "LeastUsedLoadBalancer: no chose "  + " minUse: " + minUse + ANSI.RESET_COLOUR);


            return null;
        } else {

            Logger.getLogger("log").logln(USR.STDOUT, "LeastUsedLoadBalancer: choose " + leastUsed + " minUse: " + minUse);

            Logger.getLogger("log").logln(1<<12, gc.elapsedToString(elapsedTime) + ANSI.CYAN +  " LeastUsedLoadBalancer: choose " + leastUsed + " minUse: " + minUse + " for " + name + "/" + address + ANSI.RESET_COLOUR);

            return leastUsed;
        }
    }

    /**
     * Get the relevant LocalControllerInfo for a placement of a router with 
     * a specified name and address. This placement method is not using the 
     * extra parameters.
     */
    public LocalControllerInfo routerPlacement(String name, String address, String parameters) {
    		// execute the regular routerPlacement method
    		return routerPlacement(name, address);
    }


    /**
     * Get all the possible placement destinations
     */
    public Set<LocalControllerInfo> getPlacementDestinations() {
        return gc.getLocalControllers();
    }

    /**
     * Get info as a String
     */
    private String toTable(long elapsed) {
        StringBuilder builder = new StringBuilder();

        builder.append(gc.elapsedToString(elapsed) + " ");
        for (LocalControllerInfo localInfo : getPlacementDestinations()) {
            builder.append(localInfo + ": " + localInfo.getNoRouters() + " " + localInfo.getUsage() + " | ");
        }

        return builder.toString();
    }

}
