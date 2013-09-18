package usr.abstractnetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a simple representation of an abstract network
 * Note that nodes are numbered by integer but all calls to
 * add nodes or get out links use the INTERNAL numbering
 */
public class AbstractNetwork {
    // Arrays below are sparse and this approach is for fast access.
    // outLinks_.get(i) returns a primitive array containing all the
    // nodes directly connected from node i
    private ArrayList<int []> outLinks_ = null;

    // numbers of nodes which are connected from
    // a given node
    private ArrayList<int []> linkCosts_ = null;

    // costs of connections in above

    private ArrayList<Integer> nodeList_ = null;

    // List of integers which
    // contains the numbers of nodes present

    private ArrayList<Map<Integer, Integer> > floydwarshall_ = null;

    int noNodes_ = 0;
    int noLinks_ = 0;
    int dmax_ = 0;                // Maximum diameter
    double dbar_ = 0.0;           // mean width

    boolean changed_ = true;

    /**
     * Construct a AbstractNetwork by initialising links
     */
    public AbstractNetwork() {
        outLinks_ = new ArrayList<int[]>();
        linkCosts_ = new ArrayList<int[]>();
        outLinks_.add(new int[0]);
        linkCosts_.add(new int[0]);
        nodeList_ = new ArrayList<Integer>();
    }

    /** Add a new node */
    public void addNode(int rId) {
        while (outLinks_.size() <= rId) {
            outLinks_.add(new int[0]);
            linkCosts_.add(new int[0]);
        }

        nodeList_.add(rId);
        noNodes_++;
    }

    /** Delete a node */
    public void removeNode(int rId) {
        int index = nodeList_.indexOf(rId);

        nodeList_.remove(index);
    }

    /** remove a link from n1 to n2*/
    public void removeLink(int n1, int n2) {
        int [] out;
        int [] out2;
        int [] costs;
        int [] costs2;
        int arrayPos = 0;
        noLinks_--;

        //System.err.println("Remove link from "+n1+" to "+n2);
        out = getOutLinks(n1);
        costs = getLinkCosts(n1);

        for (int i = 0; i < out.length; i++) {
            if (out[i] == n2) {
                arrayPos = i;
                break;
            }
        }

        out2 = new int[out.length - 1];
        costs2 = new int[out.length - 1];
        System.arraycopy(out, 0, out2, 0, out.length - 1);
        System.arraycopy(costs, 0, costs2, 0, out.length - 1);

        //System.err.println("Old array "+out+" new "+out2+" remove
        // "+router1Id+" "+router2Id+" pos "+arrayPos);

        if (arrayPos != out.length - 1) {
            out2[arrayPos] = out[out.length - 1];
            costs2[arrayPos] = costs[out.length - 1];
        }

        setOutLinks(n1, out2);
        setLinkCosts(n1, costs2);

        // remove n1 from outlinks of n2
        out = getOutLinks(n2);
        costs = getLinkCosts(n2);

        for (int i = 0; i < out.length; i++) {
            if (out[i] == n1) {
                arrayPos = i;
                break;
            }
        }

        out2 = new int[out.length - 1];
        costs2 = new int[out.length - 1];
        System.arraycopy(out, 0, out2, 0, out.length - 1);
        System.arraycopy(costs, 0, costs2, 0, out.length - 1);

        if (arrayPos != out.length - 1) {
            out2[arrayPos] = out[out.length - 1];
            costs2[arrayPos] = costs[out.length - 1];
        }

        setOutLinks(n2, out2);
        setLinkCosts(n2, costs2);
    }

    /** Add link between n1 and n2 -- cost is 1*/
    public void addLink(int n1, int n2) {
        noLinks_++;

        //System.err.println("Adding link from "+n1+" to "+n2);
        // Add links in both directions

        // Add link from n1 to n2
        int [] out = getOutLinks(n1);
        int [] out2 = new int[out.length + 1];
        int [] costs = getLinkCosts(n1);
        int [] costs2 = new int[out.length + 1];
        System.arraycopy(out, 0, out2, 0, out.length);
        System.arraycopy(costs, 0, costs2, 0, out.length);
        out2[out.length] = n2;
        costs2[out.length] = 1; // Link cost 1 so far
        setOutLinks(n1, out2);
        setLinkCosts(n1, costs2);

        // Add link from n2 to n1
        out = getOutLinks(n2);
        out2 = new int[out.length + 1];
        costs = getLinkCosts(n2);
        costs2 = new int[out.length + 1];
        System.arraycopy(out, 0, out2, 0, out.length);
        System.arraycopy(costs, 0, costs2, 0, out.length);
        out2[out.length] = n1;
        costs2[out.length] = 1; // Link cost 1 so far
        setOutLinks(n2, out2);
        setLinkCosts(n2, costs2);

        /*System.err.print("Outlinks "+n1+" now ");
         * out2= getOutLinks(n1);
         * for (int i= 0; i < out2.length; i++) {
         *  System.err.print(out2[i]);
         * }
         * System.err.println();
         * out2= getOutLinks(n2);
         * System.err.print("Outlinks "+n2+" now ");
         * for (int i= 0; i < out2.length; i++) {
         *  System.err.print(out2[i]);
         * }
         * System.err.println();*/
    }

    /** Return the weight from link1 to link2 or 0 if no link*/
    public int getLinkWeight(int l1, int l2) {
        int [] out = getOutLinks(l1);
        int index = -1;

        for (int i = 0; i < out.length; i++) {
            if (out[i] == l2) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            return 0;
        }

        int [] costs = getLinkCosts(l1);
        return costs[index];
    }

    /* Return a list of outlinks from a router */
    public int [] getOutLinks(int n1) {
        return outLinks_.get(n1);
    }

    /* Return a list of link costs from a router -- must be used in
     *  parallel get getOutLinks to id link nos*/
    public int [] getLinkCosts(int routerId) {
        return linkCosts_.get(routerId);
    }

    /* set list of link costs*/
    public void setLinkCosts(int routerId, int [] costs) {
        linkCosts_.set(routerId, costs);
    }

    public int getdmax() {
        /** get diameter of network */
        performFloydWarshall();
        return dmax_;
    }

    /** Get the average pair-to-pair distance on the network*/
    public double getdbar() {
        performFloydWarshall();
        return dbar_;
    }

    /** Execute the Floyd Warshall algorithm with unit costs assumed */
    private void performFloydWarshall() {
        if (!changed_) {
            return;
        }

        changed_ = false;
        int n = outLinks_.size();
        floydwarshall_ = new ArrayList<Map<Integer, Integer> >(n);

        for (int i : nodeList_) {
            floydwarshall_.set(i, new HashMap<Integer, Integer>());
        }

        for (int k : nodeList_) {
            for (int i : nodeList_) {
                for (int j : nodeList_) {
                    if (j == i) {
                        break;
                    }

                    int newDist = getDist(i, k) + getDist(k, j);

                    if (newDist < getDist(i, j)) {
                        setDist(i, j, newDist);
                    }
                }
            }
        }

        dbar_ = 0.0;
        dmax_ = 0;

        for (int i : nodeList_) {
            for (int j : nodeList_) {
                if (j == i) {
                    break;
                }

                int dist = getDist(i, j);

                if (dist > dmax_) {
                    dmax_ = dist;
                }

                dbar_ += dist;
            }
        }

        dbar_ /= (noNodes_ * (noNodes_ - 1) / 2);
    }

    private void setDist(int i, int j, int dist) {
        if (i > j) {
            setDist(j, i, dist);
        }

        Map<Integer, Integer> h = floydwarshall_.get(i);

        if (h == null) {
            return;
        }

        h.put(j, dist);
    }

    private int getDist(int i, int j) {
        if (i > j) {
            getDist(j, i);
        }

        if (i == j) {
            return 0;
        }

        Map<Integer, Integer> h = floydwarshall_.get(i);

        if (h == null) {
            return Integer.MAX_VALUE;
        }

        Integer d = h.get(j);

        if (d == null) {
            return Integer.MAX_VALUE;
        }

        return d;
    }

    /* set list of outlinks from a router */
    private void setOutLinks(int routerId, int []  out) {
        outLinks_.set(routerId, out);
    }

    public int getNoNodes() {
        return nodeList_.size();
    }

    public int getNoLinks() {
        return noLinks_;
    }

    public int getLargestRouterId() {
        return outLinks_.size();
    }

    /** Accessor function for routerList */
    public ArrayList<Integer> getNodeList() {
        return nodeList_;
    }

    /**
     * Is the node ID valid.
     */
    public boolean nodeExists(int node) {
        int index = nodeList_.indexOf(node);

        if (index >= 0) {
            return true;
        } else {
            return false;
        }
    }

    /** Return id of ith router */
    public int getNodeId(int i) {
        return nodeList_.get(i);
    }

}