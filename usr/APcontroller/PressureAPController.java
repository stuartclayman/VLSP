package usr.APcontroller;

import java.util.*;
import usr.logging.*;
import usr.router.Router;
import usr.globalcontroller.GlobalController;
import usr.router.RouterOptions;

/** Implements PressureAP Controller -- default actions are from NullAPController*/

public class PressureAPController extends NullAPController {

  
    PressureAPController (RouterOptions o) {
        super(o);
        
    } 
        
    /** Router regular AP update action */
    public void routerUpdate(Router r) 
    {
        
    }
    
    /** Controller regular AP update action */
    public void controllerUpdate(GlobalController g)
    {
        super.controllerUpdate(g);
        if (gotMinAPs(g)) {
            if (overMaxAPs(g) && canRemoveAP(g)) {   // Too many APs, remove one
                removeAP(g);
                
            }
            return;
        }
        
        addAP(g);
    }
    
    /** Remove AP using hotSpot */
    
     void removeAP(GlobalController g) {
        ArrayList <Integer> elect= getAPList();
        // No nodes which can be made managers
        int nNodes= elect.size();
        if (nNodes == 0) {
            return;
        }
        int bottomScore= -1;
        int removeNode= 0;
        for (int e: elect) {
            int score= getAPPressure(e,g);
            if (score < bottomScore || bottomScore == -1 && removable(e,g)) {
                bottomScore= score;
                removeNode= e;
            }
        }
        // No node can be removed
        if (removeNode == 0)
            return;
        removeAccessPoint(removeNode);
        Logger.getLogger("log").logln(USR.STDOUT,leadin()+" too many APs remove "+removeNode);
    }
    
    
    
    /** Add new AP usig hotSpot*/
    void addAP(GlobalController g) {
        ArrayList <Integer> elect= nonAPNodes(g);
        // No nodes which can be made managers
        int nNodes= elect.size();
        if (nNodes == 0) {
            return;
        }
        int topScore= -1;
        int electNode= 0;
        for (int e: elect) {
            int score= getPressure(e,g);
            if (score > topScore) {
                topScore= score;
                electNode= e;
            }
        }
        
        addAccessPoint(electNode,g);
        Logger.getLogger("log").logln(USR.STDOUT,leadin()+" too few APs add "+electNode);
    }
 
    /** Pressure score for gid which is not AP */
    int getPressure(int gid, GlobalController g)
    { 
        int pressure= 0;
        ArrayList <Integer> visited= new ArrayList <Integer> ();
        ArrayList <Integer> visitedCost= new ArrayList <Integer> ();
        ArrayList <Integer> toVisit= new ArrayList <Integer> ();
        ArrayList <Integer> toVisitCost= new ArrayList <Integer> ();
        toVisit.add(gid);
        toVisitCost.add(0);
       // Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Start find loop");
        while (toVisit.size() > 0) {
            //Visit closest node first
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
            // Remove from toVisit and add to Visited
            toVisit.remove(smallest);
            toVisitCost.remove(smallest);
            visited.add(smallNode);
            visitedCost.add(smallCost);
            int APcost= getAPCost(smallNode);
            if (APcost <= smallCost) {   // An existing access point is as close
                continue;   // or closer
            }
            pressure+= APcost-smallCost;
            // Now check outlinks 
            //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"picked node"+smallNode+" outlinks "+g.getOutLinks(smallNode));
            for (int l: g.getOutLinks(smallNode)) {
                
                
                //  Is outlink to already visited node
                if (visited.indexOf(l) != -1) 
                    continue;
                int newCost= smallCost+g.getLinkWeight(smallNode,l);
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
        return pressure;
    } 
    
        /** Pressure score for AP -- crude, simply number of nodes controlled*/
    int getAPPressure(int gid, GlobalController g)
    { 
        int pressure= 0;
        ArrayList <Integer> gids= g.getRouterList();
        for (int i: gids) {
            if (getAP(i) == gid) {
                pressure+= 1;
            }
        }
        return pressure;
    }    
}
