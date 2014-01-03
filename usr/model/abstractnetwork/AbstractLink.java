package usr.model.abstractnetwork;

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + node1_;
        result = prime * result + node2_;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractLink other = (AbstractLink) obj;
        if (node1_ != other.node1_)
            return false;
        if (node2_ != other.node2_)
            return false;
        return true;
    }
    @Override
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
