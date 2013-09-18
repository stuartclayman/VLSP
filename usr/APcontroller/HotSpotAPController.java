package usr.APcontroller;

import java.util.ArrayList;
import java.util.List;

import usr.globalcontroller.GlobalController;
import usr.router.Router;
import usr.router.RouterOptions;

/** Implements HotSpotAP Controller -- default actions are from NullAPController*/

public class HotSpotAPController extends NullAPController {



    HotSpotAPController (RouterOptions o) {
        super(o);
    }

    /** Router regular AP update action */
    public void routerUpdate(Router r) {

    }

    /** Controller regular AP update action */
    @Override
	public void controllerUpdate(long time, GlobalController g) {
        super.controllerUpdate(time, g);

        if (gotMinAPs(g)) {
            if (overMaxAPs(g)) {   // Too many APs, remove one
                int noToRemove = noToRemove(g);

                if (noToRemove > 0) {
                    removeAP(time, g, noToRemove);
                }

            }
            return;
        }
        int noToAdd = noToAdd(g);
        addAP(time, g, noToAdd);
    }

    /** Use the controller to remove the least efficient AP using HotSpot alg*/
    @Override
	public void controllerRemove(long time, GlobalController g) {
        System.err.println("To write");
    }

    /** Remove no APs using hotSpot */
    void removeAP(long time, GlobalController g, int no) {
        ArrayList<Integer> elect = new ArrayList<Integer>(getAPList());
        // No nodes which can be made managers
        int nNodes = elect.size();

        if (nNodes == 0) {
            return;
        }
        double [] scores = new double [g.getMaxRouterId()+1];

        for (int e : elect) {
            scores[e] = getHotSpotScore(e, g);
        }
        ArrayList<Integer> picked = pickNByScore(no, scores,
                                                      elect, false, time);

        for (Integer p : picked) {
            removeAccessPoint(time, p);
        }
    }

    /** Add no new APs usig hotSpot*/
    void addAP(long time, GlobalController g, int no) {
        ArrayList<Integer> elect = new ArrayList<Integer>(nonAPNodes(g));
        // No nodes which can be made managers
        int nNodes = elect.size();

        if (nNodes == 0) {
            return;
        }

        double [] scores = new double [g.getMaxRouterId()+1];

        for (int e : elect) {
            scores[e] = getHotSpotScore(e, g);
        }
        ArrayList<Integer> picked = pickNByScore(no, scores,
                                                      elect, true, time);

        for (Integer p : picked) {
            addAccessPoint(time, p, g);
        }
    }

    /** Accessor for hot spot score */
    @Override
	public int getScore(long tim, int gid, GlobalController g) {
        return getHotSpotScore(gid, g);
    }

    /** Hot spot score is # nodes within 1 cubed + # nodes within 2 squared
     + # nodes within 3 */
    int getHotSpotScore(int gid, GlobalController g) {
        List<Integer> rList = g.getRouterList();
        int maxRouterId = g.getMaxRouterId();
        int nRouters = rList.size();

        if (nRouters <=1) {
            return 0;
        }
        int hotSpotMax = 3;
        int [] countArray = new int[hotSpotMax];

        for (int i = 0; i < hotSpotMax; i++) {
            countArray[i] = 0;
        }


        // Boolean arrays are larger than needed but this is fast
        boolean [] visited = new boolean[maxRouterId+1];
        boolean [] visiting = new boolean[maxRouterId+1];
        int [] visitCost = new int[maxRouterId+1];

        for (int i = 0; i < maxRouterId+1; i++) {
            visited[i] = true;
            visiting[i] = false;
        }

        for (int i = 0; i < nRouters; i++) {
            visited[rList.get(i)] = false;
        }
        int [] toVisit = new int[nRouters];
        int toVisitCtr = 1;
        toVisit[0] = gid;
        visitCost[0] = 0;

        while (toVisitCtr > 0) {
            toVisitCtr--;
            int node = toVisit[toVisitCtr];
            visited[node] = true;

            if (visitCost[node] > 0) {
                countArray[visitCost[node]-1]++;
            }

            if (visitCost[node] == hotSpotMax) { //  Only count nodes within number of hops
                continue;
            }

            for (int l : g.getOutLinks(node)) {
                if (visited[l] == false && visiting[l] == false) {
                    toVisit[toVisitCtr] = l;
                    visiting[l] = true;
                    visitCost[l] = visitCost[node]+1;
                    toVisitCtr++;
                }
            }
        }
        int score = 0;

        for (int i = 0; i < hotSpotMax; i++) {
            score += Math.pow(countArray[i], (hotSpotMax-i));
        }
        return score;
    }

}