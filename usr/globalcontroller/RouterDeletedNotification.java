package usr.globalcontroller;

/**
 * An interface for objects that need to be notified
 * that a router has been deleted.
 */
public interface RouterDeletedNotification {
    /**
     * The named router has been deleted.
     */
    public void routerDeleted(String routerName);
}