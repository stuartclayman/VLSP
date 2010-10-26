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
    boolean changedNet_= false;
    boolean changedAPs_= false;
    HashMap<Integer,Integer> APs_= null;    // APs indexed by router
    HashMap<Integer,Integer> APCosts_= null;  // Costs to each AP
    LifeSpanEstimate lse_= null;
    
    NullAPController (RouterOptions o) {
        APGIDs_= new ArrayList<Integer>();
        APs_= new HashMap<Integer,Integer>();
        APCosts_= new HashMap<Integer,Integer>();
        lse_= new LifeSpanEstimate();
        options_= o;
    } 
    
    /** Return number of access points */
    public int getNoAPs()
    {
        return APGIDs_.size();
    }  
    
    /** Return list of access points */
    public ArrayList<Integer> getAPList() {
        //System.out.println("List is now "+APGIDs_);
        return APGIDs_;
    }
    
    /** is node an AP */
    public boolean isAP(int gid) {
        Integer AP= APs_.get(gid);
        if (AP == null) {
              //System.err.println("NULL");
              return false;
        }
        boolean truth= ((int)AP == gid);
        //System.err.println(gid+" == "+AP+" "+truth);
        return truth;
    }
    
    /** Return AP for given gid (or 0 if none) */
    public int getAP(int gid)
    {
        Integer AP= APs_.get(gid);
        if (AP == null) {
            //System.err.println("NO AP");
            return 0;
        }
        return AP;
    }
    
     /** Return APCost for given gid (or max dist if none) */
    public int getAPCost(int gid)
    {
        Integer APCost= APCosts_.get(gid);
        if (APCost == null) {
            //System.err.println("NO AP");
            return options_.getMaxAPWeight();
        }
        return APCost;
    }
    
    /** Set AP for given gid */
    public void setAP(int gid, int ap, int cost, GlobalController g)
    {
        Integer thisAP= APs_.get(gid);
        //System.out.println("SETAP CALLED");
        if (thisAP == null) {
            APs_.put(gid,ap);
            APCosts_.put(gid,cost);
            g.setAP(gid,ap);
        } else {
            
            APCosts_.put(gid,cost);
            //System.out.println("Got here "+thisAP+" "+ap);
            if (thisAP != ap) {
                thisAP= ap;
                APs_.put(gid,ap);
                //System.out.println("Calling g");
                g.setAP(gid,ap);
            }
        }
        
 
    }
        
    /** Router regular AP update action */
    public void routerUpdate(RouterController r) 
    {
        //System.err.println ("Controller called");
    }
    
    /** Controller regular AP update action */
    public void controllerUpdate(long time, GlobalController g)
    {
       //System.err.println ("Null Controller called");
       if (!changedNet_ && !changedAPs_) {
          return;
       }
       for (int i: g.getRouterList()) {
          int myAP= getAP(i);
          Pair <Integer,Integer> closest= findClosestAP(i,g);
          if (closest == null) {
              addAccessPoint(time, i,g);
          } else {
              setAP(i,closest.getFirst(),closest.getSecond(),g);
          }
       }
       changedNet_= false;
       changedAPs_= false;
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
    
    /** Return an estimate of traffic for all nodes and APs*/
    public int APTrafficEstimate(GlobalController g) 
    {
        int traffic= 0;
        for (int i: g.getRouterList()) {
            Integer cost= APCosts_.get(i);
            if (cost == null) {
                traffic+= options_.getMaxAPWeight();
            } else {
                traffic+= cost;
            }
        }
        return traffic;
    }
    
   
    
    /** Add new access point with ID gid*/
    public void addAccessPoint(long time, int gid, GlobalController g)
    {
        Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Node "+gid+" becomes AP");
        if (APGIDs_.indexOf(gid) != -1) {
            Logger.getLogger("log").logln(USR.ERROR, "AP controller found access point present when adding");
            return;
        }
        lse_.newAP(time,gid);
        APGIDs_.add(gid);
        setAP(gid,gid,0,g);
    }
    
    
    
    /** Remove access point with ID gid */
    public void removeAccessPoint(long time, int gid)
    {
        int index;
        
        if ((index= APGIDs_.indexOf(gid)) == -1) { 
            Logger.getLogger("log").logln(USR.ERROR, "AP controller could not find access point when removing");
            return;
        }
        Logger.getLogger("log").logln(USR.STDOUT, leadin()+" removing access point "+gid);
        lse_.APDeath(time,gid);
        APGIDs_.remove(index);
        APs_.remove(gid);
        APCosts_.remove(gid);
        changedAPs_= true;
        
    }
    
    /** Return the gid and cost of the closest AP or null if there is no such AP*/
    public Pair<Integer,Integer> findClosestAP(int gid, GlobalController g)
    {
        // Array lists are of costs and gids
        ArrayList <Integer> visited= new ArrayList <Integer> ();
        ArrayList <Integer> visitedCost= new ArrayList <Integer> ();
        ArrayList <Integer> toVisit= new ArrayList <Integer> ();
        ArrayList <Integer> toVisitCost= new ArrayList <Integer> ();
        toVisit.add(gid);
        toVisitCost.add(0);
       // Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Start find loop");
        while (toVisit.size() > 0) {
            //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"toVisit"+toVisit);
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
           
            // Have we found AP?
            if (APGIDs_.indexOf(smallNode) != -1) {
                //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"for "+gid+" new AP is "+smallNode+" "+" cost "+smallCost);
                return new Pair<Integer,Integer>(smallNode, smallCost);
            }
            // Remove from toVisit and add to Visited
            toVisit.remove(smallest);
            toVisitCost.remove(smallest);
            visited.add(smallNode);
            visitedCost.add(smallCost);
            // Now check outlinks 
            List <Integer> outLinks= g.getOutLinks(smallNode);
            List <Integer> costs= g.getLinkCosts(smallNode);
            //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"picked node"+smallNode+" outlinks "+g.getOutLinks(smallNode));
            for (int i= 0; i < outLinks.size(); i++) {
                int l= outLinks.get(i);
                int linkCost= costs.get(i);
                //  Is outlink to already visited node
                if (visited.indexOf(l) != -1) 
                    continue;
                int newCost= smallCost+linkCost;
                // Is outlink cheaper way to get to node we already have on to visit list
                int index= toVisit.indexOf(l);
                if (index != -1) {
                    toVisitCost.set(index,Math.min(toVisitCost.get(index),newCost));
                } else if (options_.getMaxAPWeight() == 0 || newCost <= options_.getMaxAPWeight()) {
                    toVisit.add(l); 
                    toVisitCost.add(newCost);
                }
            }
            
        }
        return null;
    }
    
    
    /** Add node to network */
    public void addNode(long time, int gid)
    { 
        lse_.newNode(time,gid);
        changedNet_= true;
    }
    
    /** Node has been removed from network and hence can no longer be AP --
        note that access points will  */
    public void removeNode(long time, int gid)
    {
        lse_.nodeDeath(time,gid);
        changedNet_= true;
        
        int index= APGIDs_.indexOf(gid);
        if (index == -1) {
            return;
        }
        removeAccessPoint(time, gid);
    }
        
    /** Can AP be removed giving a new AP */
    boolean removable(int gid, GlobalController g)
    {
        ArrayList <Integer> toVisit = new ArrayList<Integer> ();
        ArrayList <Integer> visited = new ArrayList<Integer> ();
        toVisit.add(gid);
        while (toVisit.size() > 0) {
            int newNode= toVisit.get(0);
            toVisit.remove(0);
            // If this node is an AP we win
            if (newNode != gid && isAP(newNode))
                return true;
            visited.add(newNode);
            for (int l: g.getOutLinks(newNode)) {
                if (toVisit.indexOf(l) == -1 && visited.indexOf(l) == -1) {
                    toVisit.add(l);
                }
            }
        }
        return false;
    }
   
    
    /** Add link to network */
    public void addLink(long time, int gid1, int gid2) {
        changedNet_= true;
    }
    
    /** Remove link from network */
    public void removeLink(long time,int gid1, int gid2) {
        changedNet_= true;
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
    
    /** Number of routers to add to make minimum requirements */
    int noToAdd(GlobalController g) {
        int noAPs= getNoAPs();
        int noRouters= g.getNoRouters();
        int add1= (int)Math.ceil(options_.getMinPropAP()*noRouters)-noAPs;
        int add2= options_.getMinAPs()-noAPs;
        return Math.max(add1,add2);
    }
    
    /** Number of routers to remove to make maximum requirements */
    int noToRemove(GlobalController g) {
        int noAPs= getNoAPs();
        int noRouters= g.getNoRouters();
        int remove1= noAPs - (int)Math.floor(options_.getMaxPropAP()*noRouters);
        int remove2= noAPs - options_.getMaxAPs();
        return Math.max(remove1,remove2);
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
    
    /** Return true if we can remove a single AP  -- do we still
    have minimum number if we remove AP*/
    boolean canRemoveAP(GlobalController g) {
       int noAPs= getNoAPs()-1;
       int noRouters= g.getNoRouters();
       if (noAPs >= options_.getMinAPs() && 
          (double)noAPs/noRouters >= options_.getMinPropAP())
            return true;
       return false;
        
    } 
    
    /** Return the mean life of a node -- this only includes
     nodes which have died*/
    public double meanNodeLife()
    {
        return lse_.meanNodeLife();
    }
    
    /** Return the mean life of an AP -- this only includes APs which have
    died*/
    public double meanAPLife() {
        return lse_.meanAPLife();
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
