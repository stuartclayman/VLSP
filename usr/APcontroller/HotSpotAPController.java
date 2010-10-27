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
    public void controllerUpdate(long time, GlobalController g)
    {
        super.controllerUpdate(time, g);
        if (gotMinAPs(g)) {
            if (overMaxAPs(g)) {   // Too many APs, remove one
                int noToRemove= noToRemove(g);
                if (noToRemove > 0)
                    removeAP(time, g, noToRemove);
                
            }
            return;
        }
        int noToAdd= noToAdd(g);
        addAP(time, g, noToAdd);
    }
    
    /** Remove no APs using hotSpot */ 
     void removeAP(long time, GlobalController g, int no) {
        ArrayList <Integer> elect= new ArrayList<Integer>(getAPList());
        // No nodes which can be made managers
        int nNodes= elect.size();
        if (nNodes == 0) {
            return;
        }
        int []scores= new int [g.getMaxRouterId()+1];
        for (int e: elect) {
            scores[e]= getHotSpotScore(e,g);
        }
        for (int i= 0; i < no; i++) {
            int bottomScore= -1;
            int removeNode= 0;
            for (int e: elect) {
                int score= scores[e];
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
            int index= elect.indexOf(removeNode);
            elect.remove(index);
            if(elect.size() == 0)
                break;
        }
    }
    
    
    
    /** Add no new APs usig hotSpot*/
    void addAP(long time, GlobalController g, int no) {
        ArrayList <Integer> elect= new ArrayList<Integer>(nonAPNodes(g));
        // No nodes which can be made managers
        int nNodes= elect.size();
        if (nNodes == 0) {
            return;
        }
        
        int []scores= new int [g.getMaxRouterId()+1];
        for (int e: elect) {
           scores[e]= getHotSpotScore(e,g);
        }
        for (int i= 0; i < no; i++) {
            int topScore= -1;
            int electNode= 0;
            for (int e: elect) {
              
                int score= scores[e];
                if (score > topScore) {
                    topScore= score;
                    electNode= e;
                }
            }
            addAccessPoint(time, electNode,g);
            Logger.getLogger("log").logln(USR.STDOUT,leadin()+" too few APs add "+electNode);
            int index= elect.indexOf(electNode);
            elect.remove(index);
            if(elect.size() == 0)
                break;
        }
    }
 
    /** Hot spot score is # nodes within 1 cubed + # nodes within 2 squared
      + # nodes within 3 */
    int getHotSpotScore(int gid, GlobalController g)
    { 
        List <Integer> rList= g.getRouterList();
        int maxRouterId= g.getMaxRouterId();
        int nRouters= rList.size();
        if (nRouters <=1)
            return 0;
        int hotSpotMax= 3;
        int []countArray= new int[hotSpotMax];
        for (int i= 0; i < hotSpotMax; i++) {
            countArray[i]= 0;
        }
        
        
        // Boolean arrays are larger than needed but this is fast
        boolean []visited= new boolean[maxRouterId+1];
        boolean []visiting= new boolean[maxRouterId+1];
        int []visitCost= new int[maxRouterId+1];
        for (int i= 0; i < maxRouterId+1; i++) {
            visited[i]= true;
            visiting[i]= false;
        }
        for (int i= 0; i < nRouters; i++) {
            visited[rList.get(i)]= false;
        }
        int []toVisit= new int[nRouters];
        int toVisitCtr= 1;
        toVisit[0]= gid;
        visitCost[toVisit[0]]= 0;
        while (toVisitCtr > 0) {
            toVisitCtr--;
            int node= toVisit[toVisitCtr];
            visited[node]= true;
            if (visitCost[node] > 0) {
                countArray[visitCost[node]-1]++;
            }
            if (visitCost[node] == hotSpotMax)  //  Only count nodes within number of hops
                continue;
            
            for (int l: g.getOutLinks(node)) {
                if (visited[l] == false && visiting[l] == false) {
                    toVisit[toVisitCtr]= l;
                    visiting[l]= true;
                    visitCost[l]= visitCost[node]+1;
                    toVisitCtr++;
                }
            }
        }
        int score= 0;
        for (int i=0; i < hotSpotMax; i++) {
            score+=Math.pow(countArray[i],(hotSpotMax-i));
        }
        return score;
    }   
}
