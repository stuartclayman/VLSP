package usr.common;

/**
 * Link contains basic info about one link / connection in the system
 */
public class LinkInfo {
    private Pair<Integer, Integer> endPoints;   // The end points of a link.  It is a pair of router ids.
    private String linkName;                    // The name of the link
    private int weight;                         // The weight on the link
    private int linkID;                         // The linkID
    private long time;                          // The time the link was created

    /**
     * Construct a LinkInfo.
     */
    public LinkInfo(Pair<Integer, Integer> routers, String name, int weight, int linkID, long time) {
        endPoints = routers;
        linkName = name;
        this.weight = weight;
        this.linkID = linkID;
        this.time = time;
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
     * Set the link weight
     */
    public void setLinkWeight(int w) {
        weight = w;
    }

    /**
     * Get the link ID
     */
    public int getLinkID() {
        return linkID;
    }

    /**
     * Get the time the link was created
     */
    public long getTime() {
        return time;
    }

}
