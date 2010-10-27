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
    public void controllerUpdate(long time, GlobalController g)
    {
        super.controllerUpdate(time, g);
        if (gotMinAPs(g)) {
            if (overMaxAPs(g) && canRemoveAP(g)) {   // Too many APs, remove one
                removeAP(time, g);
                
            }
            return;
        }
        
        addAP(time, g);
    }
    
    /** Remove AP using hotSpot */
    
     void removeAP(long time, GlobalController g) {
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
        removeAccessPoint(time, removeNode);
        Logger.getLogger("log").logln(USR.STDOUT,leadin()+" too many APs remove "+removeNode);
    }
    
    
    
    /** Add new AP usig hotSpot*/
    void addAP(long time, GlobalController g) {
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
        
        addAccessPoint(time, electNode,g);
        Logger.getLogger("log").logln(USR.STDOUT,leadin()+" too few APs add "+electNode);
    }
 
    /** Pressure score for gid which is not AP */
    int getPressure(int gid, GlobalController g)
    {         
        List <Integer> rList= g.getRouterList();
        int maxRouterId= g.getMaxRouterId();
        int nRouters= rList.size();
        if (nRouters <=1)
            return 0;
        
        
        
        // Boolean arrays are larger than needed but this is fast
        boolean []visited= new boolean[maxRouterId+1];
        boolean []visiting= new boolean[maxRouterId+1];
        
        for (int i= 0; i < maxRouterId+1; i++) {
            visited[i]= true;
            visiting[i]= false;
        }
        for (int i= 0; i < nRouters; i++) {
            visited[rList.get(i)]= false;
        }
        int []toVisit= new int[nRouters];   // numbers of nodes to visit
        int []visitCost= new int[nRouters]; // costs to get there
        int toVisitCtr= 1;
        int score= 0;
        toVisit[0]= gid;
        visitCost[0]= 0;
        while (toVisitCtr > 0) {
            int smallNode= toVisit[0]; // Find cheapest node to visit next
            int smallCost= visitCost[0];
            int whichNode= 0;
            for (int i= 1; i < toVisitCtr; i++) {
                if (visitCost[i] < smallCost) {
                    smallCost= visitCost[i];
                    smallNode= toVisit[i];
                    whichNode= i;
                }
            }
            toVisitCtr--;
            toVisit[whichNode]= toVisit[toVisitCtr]; // Rearrange to Visit over node just visited
            visitCost[whichNode]= visitCost[toVisitCtr];
            
            visited[smallNode]= true;
            
            int apCost= getAPCost(smallNode);
            if (apCost < smallCost) // Another access point is closer
                continue;
            score+= apCost- smallCost;
             //  No point in looking further as further nodes won't use this AP
            if (smallCost == options_.getMaxAPWeight()) 
                continue;
            List <Integer>out = g.getOutLinks(smallNode);
            List <Integer>costs = g.getLinkCosts(smallNode);
            
            // Add nodes linked from this node
            for (int i= 0; i < out.size();i++ ) {
                int l= out.get(i);
                int cost= costs.get(i);
                if (visited[l] == false && visiting[l] == false && 
                  smallCost+cost < options_.getMaxAPWeight()) {
                    toVisit[toVisitCtr]= l;
                    visiting[l]= true;
                    visitCost[toVisitCtr]= smallCost+cost;
                    toVisitCtr++;
                }
            }
        }


        return score;
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
