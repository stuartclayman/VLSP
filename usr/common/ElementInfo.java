package usr.common;

import usr.localcontroller.LocalControllerInfo;

/**
 * Interface provides simple information that the global and local
 * controllers need about a virtual element.
 */
public interface ElementInfo {
    /**
     * Get the start time
     */
    public long getTime();

    /**
     * Get the id
     */
    public int getId();
    
    /**
     * Get the LocalController managing the element
     */
    public LocalControllerInfo getLocalControllerInfo();

}
