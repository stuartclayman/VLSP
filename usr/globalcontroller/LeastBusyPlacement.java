package usr.globalcontroller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import usr.common.ANSI;
import usr.common.BasicRouterInfo;
import usr.localcontroller.LocalControllerInfo;
import usr.logging.BitMask;
import usr.logging.Logger;
import usr.logging.USR;
import usr.common.ANSI;

/**
 * The LeastBusyPlacement is responsible for determining the placement
 * of a Router across the active resources.
 * <p>
 * It finds the LocalController where the traffic volume from all the routers is a minimum.
 */
public class LeastBusyPlacement implements PlacementEngine {
    // The GlobalController
    GlobalController gc;

    // Previous volumes
    HashMap<LocalControllerInfo, Long>oldVolumes;

    /**
     * Constructor
     */
    public LeastBusyPlacement(GlobalController gc) {
        this.gc = gc;

        oldVolumes = new HashMap<LocalControllerInfo, Long>();

        // get logger
        try {
            Logger.getLogger("log").addOutput(new PrintWriter(new FileOutputStream("/tmp/gc-channel12.out")), new BitMask(1<<12));
        } catch (FileNotFoundException fnfe) {
            Logger.getLogger("log").logln(USR.ERROR, fnfe.toString());
        }
        
        Logger.getLogger("log").logln(USR.STDOUT, "LeastBusyPlacement: localcontrollers = " + getPlacementDestinations());
    }

    /**
     * Get the relevant LocalControllerInfo for a placement of a router with 
     * a specified name and address.
     */
    public LocalControllerInfo routerPlacement(String name, String address) {
        LocalControllerInfo leastUsed = null;

        long elapsedTime = gc.getElapsedTime();

        // A map of LocalControllerInfo to the volume of traffic
        HashMap<LocalControllerInfo, Long>lcVolumes = new HashMap<LocalControllerInfo, Long>();

        // a mapping of host to the list of routers on that host.
        HashMap<LocalControllerInfo, List<BasicRouterInfo> > routerLocations = gc.getRouterLocations();

        // Get the monitoring reporter object that collects link usage data
        TrafficInfo reporter = (TrafficInfo)gc.findByInterface(TrafficInfo.class);

        for (LocalControllerInfo localInfo : getPlacementDestinations()) {
            // now find all of the routers on that host
            List<BasicRouterInfo> routers = routerLocations.get(localInfo);

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
        // this is done by subracting the oldvolume from the latest volume
        long lowestVolume = Long.MAX_VALUE;

        for (Map.Entry<LocalControllerInfo, Long> entry : lcVolumes.entrySet()) {
            LocalControllerInfo localInfo = entry.getKey();
            Long newVolume = entry.getValue();
            Long oldVolume = oldVolumes.get(localInfo);
            long volume = 0;

            if (oldVolume == null) { // the oldVolumes didnt have an entry for this LocalControllerInfo
                volume = 0;
            } else if (newVolume < oldVolume) {
                volume = 0;
            } else {
                volume = newVolume - oldVolume;
            }

            if (volume < lowestVolume) {
                lowestVolume = volume;
                leastUsed = entry.getKey();
            }
        }


        // log current values
        Logger.getLogger("log").logln(1<<10, toTable(elapsedTime, lcVolumes));


        Logger.getLogger("log").logln(USR.STDOUT, "LeastBusyPlacement: choose " + leastUsed + " volume " + lowestVolume);


        Logger.getLogger("log").logln(1<<12, gc.elapsedToString(elapsedTime) + ANSI.CYAN +  " LeastBusyPlacement: choose " + leastUsed + " lowestVolume: " + lowestVolume + " for " + name + "/" + address + ANSI.RESET_COLOUR);

        // save volumes
        oldVolumes = lcVolumes;

        // return the leastUsed LocalControllerInfo
        return leastUsed;

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
    private String toTable(long elapsed, HashMap<LocalControllerInfo, Long>lcVolumes) {
        StringBuilder builder = new StringBuilder();


        builder.append(gc.elapsedToString(elapsed) + " ");

        for (Map.Entry<LocalControllerInfo, Long> entry : lcVolumes.entrySet()) {
            LocalControllerInfo localInfo = entry.getKey();
            Long newVolume = entry.getValue();
            Long oldVolume = oldVolumes.get(localInfo);

            long volume = 0;

            if (oldVolume == null) { // the oldVolumes didnt have an entry for this LocalControllerInfo
                volume = 0;
            } else if (newVolume < oldVolume) {
                volume = 0;
            } else {
                volume = newVolume - oldVolume;
            }

            builder.append(localInfo + ": " + localInfo.getNoRouters() + " "  + volume + " | ");
        }

        return builder.toString();
    }

}
