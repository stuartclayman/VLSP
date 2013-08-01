package usr.APcontroller;

import java.util.*;
import usr.router.RouterController;
import usr.globalcontroller.GlobalController;
import usr.router.RouterOptions;
import usr.globalcontroller.ControlOptions;
import usr.logging.*;
import usr.common.Pair;
import java.lang.Math;

/** Implements Random AP Controller */

public class NullAPController implements APController {

    ArrayList<Integer> APGIDs_ = null;  // list of accessPoints

    RouterOptions options_ = null;
    boolean changedNet_ = false;
    boolean changedAPs_ = false;
    ArrayList<Integer> APs_ = null;    // APs indexed by router
    ArrayList<Integer> APCosts_ = null;  // Costs to each AP
    LifeSpanEstimate lse_ = null;

    NullAPController (RouterOptions o) {
        APGIDs_ = new ArrayList<Integer>();
        APs_ = new ArrayList<Integer>();
        APCosts_ = new ArrayList<Integer>();
        APs_.add(0);    // Make arrays offset 1
        APCosts_.add(0);
        lse_ = new LifeSpanEstimate(o);
        options_ = o;
    }

    /** Return number of access points */
    public int getNoAPs() {
        return APGIDs_.size();
    }

    /** Return list of access points */
    public ArrayList<Integer> getAPList() {
        //System.out.println("List is now "+APGIDs_);
        return APGIDs_;
    }

    /** is node an AP */
    public boolean isAP(int gid) {
        Integer AP = APs_.get(gid);

        if (AP == null) {
            //System.err.println("NULL");
            return false;
        }
        boolean truth = ((int)AP == gid);
        //System.err.println(gid+" == "+AP+" "+truth);
        return truth;
    }

    /** Return AP for given gid (or 0 if none) */
    public int getAP(int gid) {
        Integer AP = APs_.get(gid);

        if (AP == null) {
            //System.err.println("NO AP");
            return 0;
        }
        return AP;
    }

    /** Return APCost for given gid (or max dist if none) */
    public int getAPCost(int gid) {
        Integer APCost = APCosts_.get(gid);

        if (APCost == null) {
            //System.err.println("NO AP");
            return options_.getMaxAPWeight();
        }
        return APCost;
    }

    /** Set AP for given gid */
    public void setAP(int gid, int ap, int cost, GlobalController g) {
        Integer thisAP = APs_.get(gid);

        //System.out.println("SETAP CALLED");
        if (thisAP == null) {
            APs_.set(gid, ap);
            APCosts_.set(gid, cost);
            g.setAP(gid, ap);
        } else {

            APCosts_.set(gid, cost);

            //System.out.println("Got here "+thisAP+" "+ap);
            if (thisAP != ap) {
                thisAP = ap;
                APs_.set(gid, ap);
                //System.out.println("Calling g");
                g.setAP(gid, ap);
            }
        }


    }

    /** Router regular AP update action */
    public void routerUpdate(RouterController r) {
        //System.err.println ("Controller called");
    }

    /** Controller regular AP update action */
    public void controllerUpdate(long time, GlobalController g) {
        //System.err.println ("Null Controller called");
        if (!changedNet_ && !changedAPs_) {
            return;
        }

        for (int i : g.getRouterList()) {
            // System.err.println("Find routers for "+i);
            Pair<Integer, Integer> closest = findClosestAP(i, g);

            if (closest == null) {
                addAccessPoint(time, i, g);
            } else {
                setAP(i, closest.getFirst(), closest.getSecond(), g);
            }
        }
        changedNet_ = false;
        changedAPs_ = false;
    }

    /** Use the controller to remove the least efficient AP */
    public void controllerRemove(long time, GlobalController g) {
        System.err.println("To write");
    }

    /** Return a list of potential access points */
    public ArrayList<Integer> nonAPNodes(GlobalController g) {
        ArrayList<Integer> nonAP = new ArrayList<Integer>();

        for (Integer i : g.getRouterList()) {
            if (APGIDs_.indexOf(i) == -1) {
                nonAP.add(i);
            }
        }
        return nonAP;
    }

    /** Return an estimate of traffic for all nodes and APs*/
    public int APTrafficEstimate(GlobalController g) {
        int traffic = 0;

        for (int i : g.getRouterList()) {
            Integer cost = APCosts_.get(i);

            if (cost == null) {
                traffic += options_.getMaxAPWeight();
            } else {
                traffic += cost;
            }
        }
        return traffic;
    }

    /** Add new access point with ID gid*/
    public void addAccessPoint(long time, int gid, GlobalController g) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Node "+gid+" becomes AP");

        if (APGIDs_.indexOf(gid) != -1) {
            Logger.getLogger("log").logln(USR.ERROR, "AP controller found access point present when adding");
            return;
        }
        lse_.newAP(time, gid);
        APGIDs_.add(gid);
        setAP(gid, gid, 0, g);
    }

    /** Remove access point with ID gid */
    public void removeAccessPoint(long time, int gid) {
        int index;

        if ((index = APGIDs_.indexOf(gid)) == -1) {
            Logger.getLogger("log").logln(USR.ERROR, "AP controller could not find access point when removing");
            return;
        }
        Logger.getLogger("log").logln(USR.STDOUT, leadin()+" removing access point "+gid);
        lse_.APDeath(time, gid);
        APGIDs_.remove(index);
        changedAPs_ = true;

    }

    /** Return the gid and cost of the closest AP or null if there is no such AP*/
    public Pair<Integer, Integer> findClosestAP(int gid, GlobalController g) {
        // Dijkstra algorithm with temp and perm costs

        int [] permCost = new int[g.getMaxRouterId()+1];
        int [] tempCost = new int[g.getMaxRouterId()+1];
        ArrayList<Integer> routers = g.getRouterList();

        for (int i : routers) {
            permCost[i] = -1;
            tempCost[i] = -1;
        }
        int maxCost = options_.getMaxAPWeight();

        if (maxCost == 0) {
            maxCost = g.getMaxRouterId();
        }
        int [] temporary = new int[routers.size()]; // Unordered list of
        // nodes on temporary list
        temporary[0] = gid;
        tempCost[gid] = 0;
        int tempLen = 1;

        // Dijkstra time -- everyone loves Dijkstra
        //System.err.println("Starting work "+gid);
        while (tempLen > 0) {
            // Find cheapest temp node
            int cheapest = 0;
            int cheapCost = tempCost[temporary[0]];

            for (int i = 1; i < tempLen; i++) {
                if (tempCost[temporary[i]] < cheapCost) {
                    cheapCost = tempCost[temporary[i]];
                    cheapest = i;
                }
            }
            int cheapNode = temporary[cheapest];

            //System.err.println("Considering node "+cheapNode+" cost "+cheapCost);
            if (isAP(cheapNode)) {  // Found cheapest AP]
                //System.err.println("Found "+cheapNode+" cost "+cheapCost);
                return new Pair<Integer, Integer>(cheapNode, cheapCost);
            }
            permCost[cheapNode] = cheapCost;
            tempLen--;
            temporary[cheapest] = temporary[tempLen];

            if (cheapCost == maxCost) {
                continue;
            }
            int [] out = g.getOutLinks(cheapNode);
            int [] outCost = g.getLinkCosts(cheapNode);
            int link;

            //System.err.println("Adding links from "+cheapNode+" cost "+cheapCost);
            // Consider adding links from new node
            for (int i = 0; i < out.length; i++) {
                link = out[i];

                if (permCost[link] >= 0) { // Already visited
                    continue;
                }
                int newCost = cheapCost+outCost[i];

                if (newCost > maxCost) { // Too pricey
                    continue;
                }

                if (tempCost[link] >= 0) { // Already visiting
                    if (tempCost[link] > newCost) {
                        tempCost[link] = newCost;
                        // Found cheaper route
                    }
                    continue;
                }
                //System.err.println("Add "+link+" at pos "+tempLen+" Link cost ="+newCost+ " out cost was "+outCost[i]);
                // Add to temporary list
                tempCost[link] = newCost;
                temporary[tempLen] = link;
                tempLen++;
            }

        }
        return null;
    }

    /** Add node to network */
    public void addNode(long time, int gid) {
        while (APs_.size() <= gid) {
            APs_.add(0);
            APCosts_.add(options_.getMaxAPWeight());
        }

        lse_.newNode(time, gid);
        changedNet_ = true;
    }

    /** Node has been removed from network and hence can no longer be AP --
        note that access points will  */
    public void removeNode(long time, int gid) {
        lse_.nodeDeath(time, gid);
        changedNet_ = true;

        int index = APGIDs_.indexOf(gid);

        if (index == -1) {
            return;
        }
        removeAccessPoint(time, gid);
    }


    /** Add warm up (not real) node*/
    public void addWarmUpNode(long time) {
        lse_.addWarmUpNode(time);
    }

    /** Remove warm up (not real) node */
    public void removeWarmUpNode(long startTime, long endTime) {
        lse_.removeWarmUpNode(startTime, endTime);
    }


    /** Can AP be removed giving a new AP */
    boolean removable(int gid, GlobalController g) {
        ArrayList<Integer> toVisit = new ArrayList<Integer> ();
        ArrayList<Integer> visited = new ArrayList<Integer> ();
        toVisit.add(gid);

        while (toVisit.size() > 0) {
            int newNode = toVisit.get(0);
            toVisit.remove(0);

            // If this node is an AP we win
            if (newNode != gid && isAP(newNode)) {
                return true;
            }
            visited.add(newNode);

            for (int l : g.getOutLinks(newNode)) {
                if (toVisit.indexOf(l) == -1 && visited.indexOf(l) == -1) {
                    toVisit.add(l);
                }
            }
        }
        return false;
    }

    /** Add link to network */
    public void addLink(long time, int gid1, int gid2) {
        changedNet_ = true;
    }

    /** Remove link from network */
    public void removeLink(long time, int gid1, int gid2) {
        changedNet_ = true;
    }

    /** No score for this function */
    public int getScore(long tim, int gid, GlobalController g) {
        return 0;
    }

    /** Return true if we have minimum number of APs or more */
    boolean gotMinAPs(GlobalController g) {
        if (getNoAPs() >= getMinNoAPs(g)) {
            return true;
        }
        return false;

    }

    /** Number of routers to add to make minimum requirements */
    int noToAdd(GlobalController g) {
        return (getMinNoAPs(g) - getNoAPs());
    }

    /** Number of routers to remove to make maximum requirements */
    int noToRemove(GlobalController g) {
        return (getNoAPs()-getMaxNoAPs(g));
    }

    /** Return true if we have max number of APs or more */
    boolean overMaxAPs(GlobalController g) {
        if (getNoAPs() > getMaxNoAPs(g)) {
            return true;
        }
        return false;

    }

    /** Return maximum no APs or tot no of routers if no max */
    int getMaxNoAPs(GlobalController g) {
        int noRouters = g.getNoRouters();
        int m1 = noRouters;
        int m2 = noRouters;

        if (options_.getMaxPropAP() != 0) {
            m1 = (int)Math.floor(options_.getMaxPropAP()*noRouters);
        }

        if (options_.getMaxAPs() != 0) {
            m2 = options_.getMaxAPs();
        }
        return Math.min(m1, m2);

    }

    /** Return min no APs or 0 if no min */
    int getMinNoAPs(GlobalController g) {
        int noRouters = g.getNoRouters();

        int m1 = 0;
        int m2 = 0;

        if (options_.getMinPropAP() != 0) {
            m1 = (int)Math.ceil(options_.getMinPropAP()*noRouters);
        }

        if (options_.getMinAPs() != 0) {
            m2 = options_.getMinAPs();
        }
        return Math.max(m1, m2);

    }

    /** Return true if we can remove a single AP  -- do we still
       have minimum number if we remove AP*/
    boolean canRemoveAP(GlobalController g) {
        if (getNoAPs()-1 >= getMinNoAPs(g)) {
            return true;
        }
        return false;

    }

    /** Return the mean life of a node -- this only includes
       nodes which have died*/
    public double meanNodeLife() {
        return lse_.meanNodeLife();
    }

    /** Return the mean life of an AP -- this only includes APs which have
       died*/
    public double meanAPLife() {
        return lse_.meanAPLife();
    }

    /** Return the mean life of an AP -- includes all*/
    public double meanAPLifeSoFar(long time) {
        return lse_.meanAPLifeSoFar(time);
    }

    String leadin() {
        return ("NullAPController:");
    }

    /**
       /** Create new APInfo */

    public APInfo newAPInfo() {
        return new NullAPInfo();
    }

}
