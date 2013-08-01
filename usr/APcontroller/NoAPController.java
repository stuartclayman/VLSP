package usr.APcontroller;

import java.util.*;
import usr.router.RouterController;
import usr.globalcontroller.GlobalController;
import usr.router.RouterOptions;
import usr.globalcontroller.ControlOptions;
import usr.logging.*;
import usr.common.Pair;
import java.lang.Math;

/** Implements AP controller which does nothing*/

public class NoAPController implements APController {



    LifeSpanEstimate lse_ = null;

    NoAPController () {
        lse_ = new LifeSpanEstimate();
    }

    /** Return number of access points */
    public int getNoAPs() {
        return 0;
    }

    /** Return list of access points */
    public ArrayList<Integer> getAPList() {
        //System.out.println("List is now "+APGIDs_);
        return new ArrayList<Integer>();
    }

    /** is node an AP */
    public boolean isAP(int gid) {
        return false;
    }

    /** Return AP for given gid (or 0 if none) */
    public int getAP(int gid) {
        return 0;
    }


    /** Add warm up (not real) node*/
    public void addWarmUpNode(long time) {
    }

    /** Remove warm up (not real) node */
    public void removeWarmUpNode(long startTime, long endTime) {
    }

    /** Return APCost for given gid (or max dist if none) */
    public int getAPCost(int gid) {
        return 0;
    }

    /** Set AP for given gid */
    public void setAP(int gid, int ap, int cost, GlobalController g) {


    }

    /** No score for this function */
    public int getScore(long tim, int gid, GlobalController g) {
        return 0;
    }

    /** Router regular AP update action */
    public void routerUpdate(RouterController r) {
        //System.err.println ("Controller called");
    }

    /** Controller regular AP update action */
    public void controllerUpdate(long time, GlobalController g) {
        //System.err.println ("Null Controller called");

    }

    /** Use the controller to remove the least efficient AP */
    public void controllerRemove(long time, GlobalController g) {
        //
    }

    /** Return an estimate of traffic for all nodes and APs*/
    public int APTrafficEstimate(GlobalController g) {
        return 0;
    }

    /** Add new access point with ID gid*/
    public void addAccessPoint(long time, int gid, GlobalController g) {
        Logger.getLogger("log").logln(USR.ERROR, "addAccessPoint called in NoAPController");

    }

    /** Remove access point with ID gid */
    public void removeAccessPoint(long time, int gid) {
        Logger.getLogger("log").logln(USR.ERROR, "removeAccessPoint called in NoAPController");
    }

    /** Add node to network */
    public void addNode(long time, int gid) {
        lse_.newNode(time, gid);
    }

    /** Node has been removed from network and hence can no longer be AP --
        note that access points will  */
    public void removeNode(long time, int gid) {
        lse_.nodeDeath(time, gid);

    }

    /** Add link to network */
    public void addLink(long time, int gid1, int gid2) {

    }

    /** Remove link from network */
    public void removeLink(long time, int gid1, int gid2) {

    }

    /** Return the mean life of a node -- this only includes
       nodes which have died*/
    public double meanNodeLife() {
        return lse_.meanNodeLife();
    }

    /** Return the mean life of an AP -- this only includes APs which have
       died*/
    public double meanAPLife() {
        return 0.0;
    }

    /** Return the mean life of an AP -- this only APs which have not died */
    public double meanAPLifeSoFar(long time) {
        return 0.0;
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
