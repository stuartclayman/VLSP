package usr.router;

/**
 * An interface for implementors of AP functionality.
 */
public interface AP {
    /**
     * Actually set the AP 
     * with a handle back to a RouterController
     */
    int setAP(int gid, int ap);
}
