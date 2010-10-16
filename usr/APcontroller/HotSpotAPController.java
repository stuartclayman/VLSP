package usr.APcontroller;

import java.util.*;
import usr.logging.*;
import usr.router.Router;
import usr.globalcontroller.GlobalController;
import usr.router.RouterOptions;

/** Implements HotSpotAP Controller -- default actions are from NullAPController*/

public class HotSpotAPController extends NullAPController {

    

    HotSpotAPController (RouterOptions o) {
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
            int score= getHotSpotScore(e,g);
            if (score < bottomScore || bottomScore == -1) {
                bottomScore= score;
                removeNode= e;
            }
        }
        removeAccessPoint(removeNode);
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
            int score= getHotSpotScore(e,g);
            if (score > topScore) {
                topScore= score;
                electNode= e;
            }
        }
        addAccessPoint(electNode);
    }
 
    /** Hot spot score is # nodes within 1 cubed + # nodes within 2 squared
      + # nodes within 3 */
    int getHotSpotScore(int gid, GlobalController g)
    { 
        ArrayList <Integer> visited= new ArrayList <Integer> ();
        ArrayList <Integer> visitedCost= new ArrayList <Integer> ();
        ArrayList <Integer> toVisit= new ArrayList <Integer> ();
        ArrayList <Integer> toVisitCost= new ArrayList <Integer> ();
        toVisit.add(gid);
        toVisitCost.add(0);
        int singles= 0;
        int doubles= 0;
        int triples= 0;
       // Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Start find loop");
        while (toVisit.size() > 0) {
            //The first node will always be the smallest cost one
            int smallCost= toVisitCost.get(0);
            int smallNode= toVisit.get(0);
            if (smallCost == 1) {
                singles++;
            } else if (smallCost == 2) {
                doubles++;
            } else if (smallCost == 3) {
                triples++;
            }
            // Remove from toVisit and add to Visited
            toVisit.remove(0);
            toVisitCost.remove(0);
            visited.add(smallNode);
            visitedCost.add(smallCost);
            // Now check outlinks 
            //Logger.getLogger("log").logln(USR.STDOUT, leadin()+"picked node"+smallNode+" outlinks "+g.getOutLinks(smallNode));
            for (int l: g.getOutLinks(smallNode)) {
                
                //  Is outlink to already visited node
                if (visited.indexOf(l) != -1) 
                    continue;
                // Is outlink cheaper way to get to node we do not yet have
                int index= toVisit.indexOf(l);
                if (index != -1) {
                    toVisitCost.set(index,Math.min(toVisitCost.get(index), smallCost+1));
                    continue;
                }
                if (smallCost+1 < 4) {
                    toVisit.add(l);
                    toVisitCost.add(smallCost+1);
                }
            }
            
        }
        return (singles*singles*singles+doubles*doubles+singles);
    }   
}
