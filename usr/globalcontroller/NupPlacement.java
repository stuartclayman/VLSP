package usr.globalcontroller;

import java.util.Set;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import usr.common.ANSI;
import usr.localcontroller.LocalControllerInfo;
import usr.logging.BitMask;
import usr.logging.Logger;
import usr.logging.USR;


/**
 * The NupPlacement is responsible for determining the placement
 * of a Router across the active resources.
 * <p>
 * It allocates routers in blocks of N
 * It finds the LocalController which has space for another router.
 */
public class NupPlacement implements PlacementEngine {
    // The GlobalController
    GlobalController gc;

    // The no of routers to place on a host
    int count = 5;


    /**
     * Constructor
     */
    public NupPlacement(GlobalController gc) {
        this.gc = gc;

        Logger.getLogger("log").logln(USR.STDOUT, "NupPlacement: localcontrollers = " + getPlacementDestinations());
    }

    /**
     * Get the relevant LocalControllerInfo for a placement of a router with 
     * a specified name and address.
     */
    public LocalControllerInfo routerPlacement(String name, String address) {
        LocalControllerInfo toUse = null;

        int minUse = Integer.MAX_VALUE;
        int thisUsage = 0;

        long elapsedTime = gc.getElapsedTime();

        // now work out placement
        for (LocalControllerInfo localInfo : getPlacementDestinations()) {
            thisUsage = localInfo.getNoRouters();

            if (localInfo.getNoRouters() >= localInfo.getMaxRouters()) {
                // can't use this one
                continue;
            }

            //Logger.getLogger("log").logln(USR.STDOUT, localInfo +" Usage "+thisUsage);

            if (thisUsage % count != 0) { // found a candidate with a free slot
                toUse = localInfo;
                break;
            }

            // now check if there is less usage
            if (thisUsage < minUse) {
                minUse = thisUsage;
                toUse = localInfo;
            }
        }

        Logger.getLogger("log").logln(1<<10, "NupPlacement: end of first loop: toUse = " + toUse + " minUse = " + minUse);

        // check if we didn't chose one

        if (toUse == null) {
            // chhose one with the minimum usage
            for (LocalControllerInfo localInfo : getPlacementDestinations()) {
                thisUsage = localInfo.getNoRouters();
                // choose a random with min use
                if (thisUsage == minUse) {
                    toUse = localInfo;
                }
            }
        }


        // still chose nothing
        if (toUse == null) {
            //so choose a random one
            toUse = getPlacementDestinations().iterator().next();
        }

        // log current values
        Logger.getLogger("log").logln(1<<10, toTable(elapsedTime));

        Logger.getLogger("log").logln(USR.STDOUT, "NupPlacement: choose " + toUse + " use: " + thisUsage);

        Logger.getLogger("log").logln(1<<12, gc.elapsedToString(elapsedTime) + ANSI.CYAN +  " NupPlacement: choose " + toUse + " use: " + thisUsage + " for " + name + "/" + address + ANSI.RESET_COLOUR);

        return toUse;
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
            int free = localInfo.getNoRouters() % count;

            builder.append(localInfo + ": " + localInfo.getNoRouters() + " " + free + " | ");
        }

        return builder.toString();
    }

}
