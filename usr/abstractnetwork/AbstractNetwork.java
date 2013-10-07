package usr.abstractnetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import usr.globalcontroller.GlobalController;
import usr.logging.USR;

import usr.logging.Logger;

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
    private ArrayList<AbstractLink> scheduledLinks_ = null;
    private HashMap<Integer, ArrayList<Integer>> linkFinder_= null;

    // List of integers which
    // contains the numbers of nodes present

    private HashMap <Integer, Map<Integer, Integer> > floydwarshall_ = null;

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
        scheduledLinks_ = new ArrayList<AbstractLink>();
        linkFinder_= new HashMap<Integer, ArrayList<Integer>>();
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


    /**
     * A link will be added from node1 to node2 soon -- schedule this
     * @param node1 first node number
     * @param node2 second node number
     */
    public void scheduleLink(int node1, int node2)
    {
        scheduleLink(new AbstractLink(node1,node2));
    }

    /**
     * Notify abstract network that a link will be added at a later date.
     * @param link
     */
    public void scheduleLink(AbstractLink link)
    {
        scheduledLinks_.add(link);
        int n1= link.getNode1();
        int n2= link.getNode2();
        ArrayList <Integer> lns= linkFinder_.get(n1);
        if (lns == null) {
            lns= new ArrayList <Integer> ();
        }
        lns.add(n2);
        lns= linkFinder_.get(n2);
        if (lns == null) {
            lns= new ArrayList <Integer> ();
        }
        lns.add(n1);
    }

    /**
     * A link from node1 to node2 is removed from the schedule
     * @param node1
     * @param node2
     */
    public void unScheduleLink(int node1, int node2)
    {
        AbstractLink l= new AbstractLink(node1,node2);
        int idx= scheduledLinks_.indexOf(l);
        if (idx < 0) {
            Logger.getLogger("log").logln(USR.ERROR, leadin()+"Attempt to remove link "+l+" already removed");
        }
        scheduledLinks_.remove(idx);
        ArrayList <Integer> lns= linkFinder_.get(node1);
        lns.remove(lns.indexOf(node2));
        lns= linkFinder_.get(node2);
        lns.remove(lns.indexOf(node1));

    }


    /** Check network for isolated nodes and connect them if possible */
    public AbstractLink checkIsolated(long time, GlobalController gc) {
        for (int i : getNodeList()) {
            AbstractLink l = checkIsolated(time, i, gc);
            if (l != null)
                return l;
        }
        return null;
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

    /* Return a list of outlinks from a router including scheduled but not added*/
    public int [] getAllOutLinks(int n1) {
        int [] l= outLinks_.get(n1);
        ArrayList <Integer> exLinks= linkFinder_.get(n1);
        if (exLinks == null || exLinks.size() == 0)
            return l;
        int [] m= new int[l.length + exLinks.size()];
        System.arraycopy(l, 0, m, 0, l.length);

        return l;
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
        floydwarshall_ = new HashMap <Integer, Map<Integer, Integer>  > ();

        for (int i : nodeList_) {
            floydwarshall_.put(i, new HashMap<Integer, Integer>());

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
        	Logger.getLogger("log").logln(USR.ERROR,"null found in FloydWarshall");
            return;
        }

        h.put(j, dist);
    }


    /** Check if given node is isolated return a connecting link if one available */
    public AbstractLink checkIsolated(long time, int gid, GlobalController gc) {
        int [] links = getAllOutLinks(gid);
        int nRouters = getNoNodes();

        if (nRouters == 1) { // One node is allowed to be isolated
            return null;
        }

        if (links.length > 0) {
            return null;
        }

        // Node is isolated.
        while (true) {
            int i = (int)Math.floor(Math.random() * nRouters);
            int dest = getNodeId(i);
            if (dest != gid) {
                return new AbstractLink(dest,gid);
            }
        }
    }

    /** Make sure network is connected*/
    public AbstractLink connectNetwork (long time, GlobalController gc) {
        int nRouters = getNoNodes();
        int largestRouterId = getLargestRouterId();

        if (nRouters <= 1) {
            return null;
        }

        // Boolean arrays are larger than needed but this is fast
        boolean [] visited = new boolean[largestRouterId + 1];
        boolean [] visiting = new boolean[largestRouterId + 1];

        for (int i = 0; i < largestRouterId + 1; i++) {
            visited[i] = true;
            visiting[i] = false;
        }

        for (int i = 0; i < nRouters; i++) {
            visited[getNodeId(i)] = false;
        }

        int [] toVisit = new int[nRouters];
        int toVisitCtr = 1;
        toVisit[0] = getNodeId(0);
        int noVisited = 0;

        while (noVisited < nRouters) {
            //System.err.println("NoVisited = "+noVisited+" /
            // "+nRouters);
            if (toVisitCtr == 0) {
                // Not visited everything so make a new link
                // Choose i1 th visited and i2 th unvisited
                int i1 = (int)Math.floor(Math.random() * noVisited);
                int i2
                    = (int)Math.floor(Math.random()
                                      * (nRouters - noVisited));
                int l1 = -1;
                int l2 = -1;

                for (int i = 0; i < nRouters; i++) {
                    int tmpNode = getNodeId(i);

                    if (visited[tmpNode] && (i1 >= 0)) {
                        if (i1 == 0) {
                            l1 = tmpNode;
                        }

                        i1--;
                    } else if (!visited[tmpNode] && (i2 >= 0)) {
                        if (i2 == 0) {
                            l2 = tmpNode;
                        }

                        i2--;
                    }

                    if ((i1 < 0) && (i2 < 0)) {
                        break;
                    }
                }

                if ((l1 == -1) || (l2 == -1)) {
                    Logger.getLogger("log").logln(USR.ERROR, leadin() + "Error in network connection " + l1 + " " + l2);
                    gc.shutDown();
                    return null;
                }
                return new AbstractLink(l1,l2);
            }

            toVisitCtr--;
            int node = toVisit[toVisitCtr];
            visited[node] = true;
            noVisited++;

            for (int l : getAllOutLinks(node)) {
                if ((visited[l] == false) && (visiting[l] == false)) {
                    toVisit[toVisitCtr] = l;
                    visiting[l] = true;
                    toVisitCtr++;
                }
            }
        }
        return null;
    }

    /** Make sure network is connected from r1 to r2*/
    public AbstractLink connectNetwork(long time, int r1, int r2, GlobalController gc) {
        int nRouters = getNoNodes();
        int maxRouterId = getLargestRouterId();

        if (nRouters <= 1) {
            return null;
        }

        // Boolean arrays are larger than needed but this is fast
        boolean [] visited = new boolean[maxRouterId + 1];
        boolean [] visiting = new boolean[maxRouterId + 1];

        for (int i = 0; i < maxRouterId + 1; i++) {
            visited[i] = true;
            visiting[i] = false;
        }

        for (int i = 0; i < nRouters; i++) {
            int rId = getNodeId(i);
            visited[rId] = false;
        }

        int [] toVisit = new int[nRouters];
        int toVisitCtr = 1;
        toVisit[0] = r1;
        int noVisited = 1;

        while (noVisited < nRouters) {
            if (toVisitCtr == 0) {
                // Not visited everything so make a new link
                // Choose i1 th visited and i2 th unvisited
                int i1 = (int)Math.floor(Math.random() * noVisited);
                int i2
                    = (int)Math.floor(Math.random()
                                      * (nRouters - noVisited));
                int l1 = -1;
                int l2 = -1;

                for (int i = 0; i < nRouters; i++) {
                    int tmpNode = getNodeId(i);

                    if (visited[tmpNode] && (i1 >= 0)) {
                        if (i1 == 0) {
                            l1 = tmpNode;
                        }

                        i1--;
                    } else if (!visited[tmpNode] && (i2 >= 0)) {
                        if (i2 == 0) {
                            l2 = tmpNode;
                        }

                        i2--;
                    }

                    if ((i1 < 0) && (i2 < 0)) {
                        break;
                    }
                }

                if ((l1 == -1) || (l2 == -1)) {
                    Logger.getLogger("log").logln(USR.ERROR, leadin() + "Error in network connection " + l1 + " " + l2);
                    gc.shutDown();
                    return null;
                }
                return new AbstractLink(l1,l2);
            }

            toVisitCtr--;
            int node = toVisit[toVisitCtr];
            visited[node] = true;
            noVisited++;

            for (int l : getOutLinks(node)) {
                if (l == r2) {                                   // We have a connection
                    return null;
                }

                if ((visited[l] == false) && (visiting[l] == false)) {
                    toVisit[toVisitCtr] = l;
                    visiting[l] = true;
                    toVisitCtr++;
                }
            }
        }
        return null;
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

    private String leadin()
    {
        return "AbstractNetwork:";
    }

}