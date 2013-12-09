package usr.globalcontroller;

import usr.localcontroller.LocalControllerInfo;
import java.util.Set;
import usr.logging.USR;
import usr.logging.Logger;

/**
 * A LoadBalanacer is repsonsible for determining the placement
 * of a Router across the active resources.
 */
public class LeastUsedLoadBalancer implements PlacementEngine {
    Set<LocalControllerInfo> localcontrollers;

    /**
     * Constructor
     */
    public LeastUsedLoadBalancer(Set<LocalControllerInfo> localcontrollers) {
        this.localcontrollers = localcontrollers;

        Logger.getLogger("log").logln(USR.STDOUT, "LeastUsedLoadBalancer: localcontrollers = " + localcontrollers);
    }

    /**
     * Get the relevant LocalControllerInfo for a placement of a router.
     */
    public LocalControllerInfo routerPlacement() {
        LocalControllerInfo leastUsed = null;

        double minUse = 0.0;
        double thisUsage = 0.0;

        for (LocalControllerInfo localInfo : localcontrollers) {
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

        if (minUse >= 1.0) {
            return null;
        }

        return leastUsed;
    }


    /**
     * Get the relevant LocalControllerInfo for a placement of a router with a specific address.
     */
    public LocalControllerInfo routerPlacement(String address) {
        // this LoadBalancer doesn't care about the address.
        return routerPlacement();
    }

    /**
     * Get all the possible placement destinations
     */
    public Set<LocalControllerInfo> getPlacementDestinations() {
        return localcontrollers;
    }
}
