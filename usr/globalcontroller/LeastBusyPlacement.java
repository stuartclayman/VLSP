package usr.globalcontroller;

import usr.localcontroller.LocalControllerInfo;
import usr.common.BasicRouterInfo;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import usr.logging.USR;
import usr.logging.Logger;

/**
 * The LeastBusyPlacement is repsonsible for determining the placement
 * of a Router across the active resources.
 * <p>
 * It finds the LocalController where the traffic volume from all the routers is a minimum.
 */
public class LeastBusyPlacement implements PlacementEngine {
    // The GlobalController
    GlobalController gc;


    /**
     * Constructor
     */
    public LeastBusyPlacement(GlobalController gc) {
        this.gc = gc;

        Logger.getLogger("log").logln(USR.STDOUT, "LeastBusyPlacement: localcontrollers = " + getPlacementDestinations());
    }

    /**
     * Get the relevant LocalControllerInfo for a placement of a router.
     */
    public LocalControllerInfo routerPlacement() {
        LocalControllerInfo leastUsed = null;

        // A map of LocalControllerInfo to the volume of traffic
        HashMap<LocalControllerInfo, Long>lcVolumes = new HashMap<LocalControllerInfo, Long>();

        // a mapping of host to the list of routers on that host.
        HashMap<String, List<BasicRouterInfo> > routerLocations = gc.getRouterLocations();

        // Get the monitoring reporter object that collects link usage data
        TrafficInfo reporter = (TrafficInfo)gc.findByInterface(TrafficInfo.class);

        for (LocalControllerInfo localInfo : getPlacementDestinations()) {
            // get the host for the LocalController
            String host = localInfo.getName();

            // now find all of the routers on that host
            List<BasicRouterInfo> routers = routerLocations.get(host);

            if (routers == null) {
                // no routers in that host
                // therefore zero volume
                lcVolumes.put(localInfo, 0L);

            } else {

                // a running volume
                Long volume = 0L;

                // for each router, find all of the links
                for (BasicRouterInfo router : routers) {
                    int routerID = router.getId();
                    String routerName = router.getName();

                    // get remote routerIDs of links that come out of this router.
                    List<Integer> outDests = gc.getOutLinks(routerID);

                    // for each link
                    for (int otherRouter : outDests) {
                        // convert 
                        String router2Name = gc.findRouterInfo(otherRouter).getName();
                        // get trafffic for link i -> j as routerName => router2Name
                        List<Object> iToj = reporter.getTraffic(routerName, router2Name);

                        if (iToj != null) {             // there is some traffic data for the link
                            // now calculate 
                            // in bytes + out bytes
                            Integer inOut = (Integer)iToj.get(1) + (Integer)iToj.get(5);

                            volume += inOut;
                        }
                    }
                }

                // now visited all routers in this host
                lcVolumes.put(localInfo, volume);

            }
        }


        // at this point we know which host has what volume.
        // now we need to skip through all of them and find the host
        // with the lowest volume
        long lowestVolume = Long.MAX_VALUE;

        for (Map.Entry<LocalControllerInfo, Long> entry : lcVolumes.entrySet()) {
            Long volume = entry.getValue();

            if (volume < lowestVolume) {
                lowestVolume = volume;
                leastUsed = entry.getKey();
            }
        }

        Logger.getLogger("log").logln(USR.STDOUT, "LeastBusyPlacement: choose " + leastUsed + " volume " + lowestVolume);

        // return the leastUsed LocalControllerInfo
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
        return gc.getLocalControllers();
    }
}
