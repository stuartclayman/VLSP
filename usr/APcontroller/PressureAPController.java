package usr.APcontroller;

import java.util.ArrayList;
import java.util.List;

import usr.globalcontroller.GlobalController;
import usr.model.abstractnetwork.AbstractNetwork;
import usr.router.Router;
import usr.router.RouterOptions;

/** Implements PressureAP Controller -- default actions are from NullAPController*/

public class PressureAPController extends NullAPController {


    public PressureAPController (RouterOptions o) {
        super(o);

    }

    /** Router regular AP update action */
    public void routerUpdate(Router r) {

    }

    /** Controller regular AP update action */
    @Override
    public void controllerUpdate(long time, GlobalController g) {
        // calls super class controllerUpdate()
        // which does the setAP()
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
        //System.err.println("Adding "+noToAdd);
        addAP(time, g, noToAdd);
    }

    /** Use the controller to remove the least efficient AP using Pressure alg*/
    @Override
    public void controllerRemove(long time, GlobalController g) {
        System.err.println("To write");
    }

    /** Remove no APs using pressure algorithm */
    void removeAP(long time, GlobalController g, int no) {
        ArrayList<Integer> elect = new ArrayList<Integer>(getAPList());

        double scores[] = new double[g.getMaxRouterId()+1];

        for (int e : elect) {
            scores[e] = getPressure(e, g);
        }
        ArrayList<Integer> picked = pickNByScore(no, scores,
                                                 elect, false, time);

        for (Integer p : picked) {
            removeAccessPoint(time, p);
        }
    }

    /** Add new AP usig hotSpot*/
    void addAP(long time, GlobalController g, int no) {
        // System.err.println("At "+time+" adding "+no);
        ArrayList<Integer> elect = new ArrayList<Integer>(nonAPNodes(g));
        // No nodes which can be made managers
        double scores[] = new double[g.getMaxRouterId()+1];

        for (int e : elect) {
            scores[e] = getPressure(e, g);
        }
        ArrayList<Integer> picked = pickNByScore(no, scores,
                                                 elect, true, time);

        for (Integer p : picked) {
            addAccessPoint(time, p, g);
        }
    }

    /** No score for this function */
    @Override
    public int getScore(long time, int gid, GlobalController g) {
        if (isAP(gid)) {
            return getAPPressure(gid, g);
        } else {
            return getPressure(gid, g);
        }
    }

    /** Pressure score for gid which is not AP */
    int getPressure(int gid, GlobalController g) {
        // Array lists are of costs and gids

        int [] permCost = new int[g.getMaxRouterId()+1];
        int [] tempCost = new int[g.getMaxRouterId()+1];
        List<Integer> routers = g.getRouterList();

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
        int score = 0;

        // Dijkstra time -- everyone loves Dijkstra
        while (tempLen > 0) {
            // Find cheapest temp node
            int cheapest = 0;
            int cheapCost = tempCost[temporary[0]];

            for (int i = 1; i < tempLen; i++) {
                if (tempCost[temporary[0]] < cheapCost) {
                    cheapCost = tempCost[temporary[i]];
                    cheapest = i;
                }
            }
            int cheapNode = temporary[cheapest];

            permCost[cheapNode] = cheapCost;
            score += getAPCost(cheapNode)-cheapCost;
            tempLen--;
            temporary[cheapest] = temporary[tempLen];

            if (cheapCost == maxCost) {
                continue;
            }
            
            AbstractNetwork network = g.getAbstractNetwork();
            int [] out = network.getOutLinks(cheapNode);
            int [] outCost = network.getLinkCosts(cheapNode);
            
            int link;

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

                if (newCost >= getAPCost(link)) { // Pricer than other AP
                    continue;
                }

                if (tempCost[link] >= 0) { // Already visiting
                    if (tempCost[link] > newCost) {
                        tempCost[link] = newCost;
                        // Found cheaper route
                    }
                    continue;
                }
                // Add to temporary list
                tempCost[link] = newCost;
                temporary[tempLen] = link;
                tempLen++;
            }

        }


        return score;
    }

    /** Pressure score for AP -- crude, simply number of nodes controlled*/
    int getAPPressure(int gid, GlobalController g) {
        int pressure = 0;
        List<Integer> gids = g.getRouterList();

        for (int i : gids) {
            if (getAP(i) == gid) {
                pressure += 1;
            }
        }
        return pressure;
    }

}
