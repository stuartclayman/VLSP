package usr.common;

/**
 * Link contains basic info about one link / connection in the system
 */
public class LinkInfo {
    private Pair<Integer, Integer> endPoints;      // The end points of a
                                                   // link.  It is a pair
                                                   // of router ids.
    private String linkName;                      // The name of the
                                                  // link
    private int weight;                           // The weight on the
                                                  // link
    private int linkID;                           // The linkID

    /**
     * Construct a LinkInfo.
     */
    public LinkInfo(Pair<Integer,
                         Integer> routers, String name, int weight, int linkID) {
        endPoints = routers;
        linkName = name;
        this.weight = weight;
        this.linkID = linkID;
    }

    /**
     * Get the end point pair
     */
    public Pair<Integer, Integer> getEndPoints() {
        return endPoints;
    }

    /**
     * Get the link name
     */
    public String getLinkName() {
        return linkName;
    }

    /**
     * Get the link weight
     */
    public int getLinkWeight() {
        return weight;
    }

    /**
     * Get the link ID
     */
    public int getLinkID() {
        return linkID;
    }

}