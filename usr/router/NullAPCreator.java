package usr.router;

import usr.applications.ApplicationResponse;
import usr.logging.Logger;
import usr.logging.USR;

/**
 * The NullAPCreator is an AP that does nothing intersting.
 * It is a plugin that accepts the setAP() method
 * and returns immediately.
 */
public class NullAPCreator implements AP {
    RouterController controller;

    public NullAPCreator(RouterController rc) {
        controller = rc;
    }

    /**
     * Set the AP does nothing
     */
    public int setAP(int gid, int ap, String[] ctxArgs) {
        return 0;
    }

}
