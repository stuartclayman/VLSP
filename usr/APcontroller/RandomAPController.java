package usr.APcontroller;

import java.util.ArrayList;
import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;
import usr.router.RouterController;
import usr.router.RouterOptions;

/** Implements Random AP Controller -- default actions are from NullAPController*/

public class RandomAPController extends NullAPController {

    RandomAPController (RouterOptions o) {
        super(o);
    }

    /** Router regular AP update action */
    public void routerUpdate(long time, RouterController r) {
        //System.err.println ("Controller called");
    }

    /** Controller regular AP update action */
    @Override
	public void controllerUpdate(long time, GlobalController g) {
        super.controllerUpdate(time, g);
        if (gotMinAPs(g)) {
            if (overMaxAPs(g) && canRemoveAP(g)) {   // Too many APs, remove one
                int no = noToRemove(g);
                removeAP(time, g, no);


            }
            return;
        }
        int no = noToAdd(g);

        if (no <= 0) {
            return;
        }
        addAP(time, g, no);

    }

    /** Use the controller to remove one random AP controller */
    @Override
	public void controllerRemove(long time, GlobalController g) {
        System.err.println("To write");
    }

    /** Add no aggregation points chosen at random */
    void addAP(long time, GlobalController g, int no) {
        if (options_ != null && options_.getAPLifeBias() > 0.0) {
            double scores[] = new double[g.getMaxRouterId()+1];
            ArrayList<Integer> nodes = nonAPNodes(g);

            for (Integer node : nodes) {
                scores[node] = 1.0;
            }
            ArrayList<Integer> picked = pickNByScore(no, scores,
                                                          nodes, true, time);

            for (Integer p : picked) {
                addAccessPoint(time, p, g);
            }
            return;
        }

        for (int i = 0; i < no; i++) {
            ArrayList<Integer> elect = nonAPNodes(g);
            Logger.getLogger("log").logln(USR.STDOUT, leadin()+" adding random AP");
            // No nodes which can be made managers
            int nNodes = elect.size();

            if (nNodes == 0) {
                return;
            }
            // Choose a random node to become an AP manager
            int index = (int)Math.floor( Math.random()*nNodes);
            int elected = elect.get(index);
            addAccessPoint(time, elected, g);
        }
    }

    /** Remove no aggregation points chosen at random */
    void removeAP(long time, GlobalController g, int no) {
        if (options_ != null && options_.getAPLifeBias() > 0.0) {
            double scores[] = new double[g.getMaxRouterId()+1];
            ArrayList<Integer> nodes = nonAPNodes(g);

            for (Integer node : nodes) {
                scores[node] = 1.0;
            }
            ArrayList<Integer> picked = pickNByScore(no, scores,
                                                          nodes, false, time);

            for (Integer p : picked) {
                removeAccessPoint(time, p);
            }
            return;
        }

        for (int i = 0; i < no; i++) {
            ArrayList<Integer> APs = new ArrayList<Integer>(getAPList());

            while (APs.size() > 0) {
                int nAPs = APs.size();
                int j = (int)Math.floor( Math.random()*nAPs);
                int rno = APs.get(j);

                if (removable(rno, g)) {
                    Logger.getLogger("log").logln(USR.STDOUT,
                                                  leadin()+" too many APs remove "+rno);
                    removeAccessPoint(time, rno);
                    break;
                }
                APs.remove(j);
            }
        }
    }

    @Override
	String leadin() {
        return ("RandomAPController:");
    }

    /** Create new APInfo */

    @Override
	public APInfo newAPInfo() {
        return new RandomAPInfo();
    }

}
