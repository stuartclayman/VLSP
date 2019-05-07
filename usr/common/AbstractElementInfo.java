package usr.common;

import usr.localcontroller.LocalControllerInfo;

/**
 * Class provides simple information that the global and local
 * controllers need about an element.
 */
public class AbstractElementInfo implements ElementInfo {
    protected long time_;
    protected LocalControllerInfo controller_;
    protected int id_;

    /**
     * Get the time
     */
    public long getTime() {
        return time_;
    }

    /**
     * Get the  id
     */
    public int getId() {
        return id_;
    }

    /**
     * Get the LocalController managing the router
     */
    public LocalControllerInfo getLocalControllerInfo() {
        return controller_;
    }

}
