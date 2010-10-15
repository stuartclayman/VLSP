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

    ArrayList<Integer> APGIDs_= null;  // list of accessPoints
    RouterOptions options_= null;

    
    NullAPController (RouterOptions o) {
        APGIDs_= new ArrayList<Integer>();
        options_= o;
    } 
    
    /** Return number of access points */
    public int getNoAPs()
    {
        return APGIDs_.size();
    }  
    
    /** Return list of access points */
    public List<Integer> getAPlist() {
        return APGIDs_;
    }
    
        
    /** Router regular AP update action */
    public void routerUpdate(RouterController r) 
    {
        //System.err.println ("Controller called");
    }
    
    /** Controller regular AP update action */
    public void controllerUpdate(GlobalController g)
    {
       // System.err.println ("Controller called");
    }
  
    /** Return a list of potential access points */
    public ArrayList<Integer> nonAPNodes(GlobalController g)
    {
        ArrayList <Integer> nonAP= new ArrayList<Integer>();
        for (Integer i: g.getRouterList()) {
            if (APGIDs_.indexOf(i) == -1) {
                nonAP.add(i);
            }
        }
        return nonAP;
    }
    
    /** Node elected to be AP, add it to data structures and if
    not simulating then inform it */
    public void electNode(int gid, GlobalController g) {
        if (!g.getOptions().isSimulation()) {
            g.nodeAPStart(gid);
        }
        addAccessPoint(gid);
    }
    
     /** Node stopped from being AP, remove it from data structures
     and if not simulating then inform it */
     public void unElectNode(int gid, GlobalController g) {
        if (!g.getOptions().isSimulation()) {
            g.nodeAPStop (gid);
        }
        removeAccessPoint(gid);
    }
    
    /** Add new access point with ID gid*/
    public void addAccessPoint(int gid)
    {
        if (APGIDs_.indexOf(gid) != -1) {
            Logger.getLogger("log").logln(USR.ERROR, "AP controller found access point present when adding");
            return;
        }
        APGIDs_.add(gid);
    }
    
    
    
    /** Remove access point with ID gid */
    public void removeAccessPoint(int gid)
    {
        int index;
        if ((index= APGIDs_.indexOf(gid)) == -1) { 
            Logger.getLogger("log").logln(USR.ERROR, "AP controller could not find access point when removing");
            return;
        }
        APGIDs_.remove(index);
    }
    
    /** Return the gid of the closest AP or 0 if there is no such AP*/
    public int findClosestAP(int gid, GlobalController g)
    {
        // Array lists are of costs and gids
        ArrayList <Integer> visited= new ArrayList <Integer> ();
        ArrayList <Integer> visitedCost= new ArrayList <Integer> ();
        ArrayList <Integer> toVisit= new ArrayList <Integer> ();
        ArrayList <Integer> toVisitCost= new ArrayList <Integer> ();
        toVisit.add(gid);
        toVisitCost.add(0);
        while (toVisit.size() > 0) {
            int smallest= 0;
            int smallCost= toVisitCost.get(0);
            int smallPos= 0;
            for (int i= 1; i < toVisit.size(); i++) {
                int newCost= toVisitCost.get(i);
                if (newCost < smallCost) {
                    smallCost= newCost;
                    smallest= i;
                }
            }
            int smallNode= toVisit.get(smallest);
            // Have we actually got AP in range
            if (options_.getMaxAPWeight() > 0 && smallCost > options_.getMaxAPWeight())
                return 0;
            // Have we found AP?
            if (APGIDs_.indexOf(smallNode) != -1)
                return smallest;
                
            // Remove from toVisit and add to Visited
            toVisit.remove(smallest);
            toVisitCost.remove(smallest);
            visited.add(smallNode);
            visitedCost.add(smallCost);
            // Now check outlinks 
            for (int l: g.getOutLinks(smallNode)) {
                //  Is outlink to already visited node
                if (visited.indexOf(l) != -1) 
                    continue;
                // Is outlink cheaper way to get to node we do not yet have
                int index= toVisit.indexOf(l);
                if (index != -1) {
                    toVisitCost.set(index,Math.min(toVisitCost.get(index), smallCost));
                    continue;
                }
                toVisit.add(l);
                toVisitCost.add(smallCost+g.getLinkWeight(smallNode,l));
            }
            
        }
        return 0;
    }
    
    /** Node has been removed from network and hence can no longer be AP */
    public void removeNode(int gid)
    {
        int index= APGIDs_.indexOf(gid);
        if (index == -1) {
            return;
        }
        APGIDs_.remove(index);
    }
        
    /** Return true if we have minimum number of APs or more */
    boolean gotMinAPs(GlobalController g) {
       int noAPs= getNoAPs();
       int noRouters= g.getNoRouters();
       if (noAPs >= options_.getMinAPs() && 
          (double)noAPs/noRouters >= options_.getMinPropAP())
            return true;
       return false;
        
    }    
    
    /** Return true if we have max number of APs or more */
    boolean overMaxAPs(GlobalController g) {
       int noAPs= getNoAPs();
       int noRouters= g.getNoRouters();
       if (noAPs > options_.getMaxAPs() || 
          (double)noAPs/noRouters > options_.getMaxPropAP())
            return true;
       return false;
        
    }   
        
    /** 
    /** Create new APInfo */
    
    public APInfo newAPInfo() {
        return new NullAPInfo();
    }
    
}
