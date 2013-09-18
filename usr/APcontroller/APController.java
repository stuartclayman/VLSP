package usr.APcontroller;

import java.util.List;

import usr.globalcontroller.GlobalController;
import usr.router.RouterController;

/** Interface for aggregation point controller types */

public interface APController {

    /** Return number of access points allocated */
    public int getNoAPs();

    /** Return list of access points */
    public List<Integer> getAPList();

    /** Is node with GID an AP*/
    public boolean isAP(int gid);

    /** get AP for given GID */
    public int getAP(int gid);

    /** get AP for given GID */
    public int getAPCost(int gid);

    /** Router regular AP update action */
    public void routerUpdate(RouterController r);

    /** Controller regular AP update action */
    public void controllerUpdate(long time, GlobalController g);

    /** Controller regular AP remove then add action */
    public void controllerRemove(long time, GlobalController g);

    /** Calculate AP traffic estimate */
    public int APTrafficEstimate(GlobalController g);

    /** Add new access point with gid G*/
    public void addAccessPoint(long time, int gid, GlobalController g);

    /** Remove access point with gid G*/
    public void removeAccessPoint(long time, int gid);

    /** Add node to network */
    public void addNode(long time, int gid);

    /** Add warm up (not real) node*/
    public void addWarmUpNode(long time);

    /** Remove warm up (not real) node */
    public void removeWarmUpNode(long startTime, long endTime);

    /** Remove node and hence possibly AP from network*/
    public void removeNode(long time, int gid);

    /** Add link to network */
    public void addLink(long time, int gid1, int gid2);

    /** Remove link from network */
    public void removeLink(long time, int gid1, int gid2);

    /** Return the mean life of a node -- this only includes
       nodes which have died*/
    public double meanNodeLife();

    /** Return the mean life of an AP -- this only includes APs which have
       died*/
    public double meanAPLife();


    /** Return the mean life of an AP -- includes all  APs*/
    public double meanAPLifeSoFar(long time);

    /** Return score at given time */
    public int getScore(long time, int gid, GlobalController g);

    /** Return APInfo appropriate for this controller */
    public APInfo newAPInfo();

}
