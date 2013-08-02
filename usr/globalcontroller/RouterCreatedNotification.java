package usr.globalcontroller;

/**
 * An interface for objects that need to be notified
 * that a router has been created.
 */
public interface RouterCreatedNotification {
    /**
     * The named router has been created
     */
    public void routerCreated(String routerName);
}