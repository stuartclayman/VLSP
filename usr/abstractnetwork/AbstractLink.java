package usr.abstractnetwork;

/** Class simply represents a link as
 * two integers -- smallest first -- easy to return as single value */
public class AbstractLink implements Comparable <AbstractLink> {
    int node1_, node2_;



    /**
     * nodes can be in any order
     * @param first node number
     * @param second node number
     */
    public AbstractLink(int n1, int n2) {
        if (n1 < n2) {
            node1_ = n1;
            node2_ = n2;
        } else {
            node1_ = n2;
            node2_ = n1;
        }
    }

    public String toString() {
        String s= node1_+"->"+node2_;
        return s;
    }

    public int compareTo (AbstractLink otherLink)
    {
        if (node1_ < otherLink.getNode1()) {
            return -1;
        } else if (node1_ > otherLink.getNode1()) {
            return 1;
        } else if (node2_ < otherLink.getNode2()) {
            return -1;
        } else if (node2_ > otherLink.getNode2()) {
            return 1;
        }
        return 0;
    }

    /**
     * @return value of first link (smallest)
     */
    public int getNode1() {
        return node1_;
    }

    /**
     * @return value of second link (largest)
     */
    public int getNode2() {
        return node2_;
    }
}
