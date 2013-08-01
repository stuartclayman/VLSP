package usr.console;

import cc.clayman.console.ManagementConsole;

/**
   import usr.logging.*;
 * A ComponentController is the controller for a component
 * in the UserSpaceRouting system.
 * It interacts with a ManagementConsole.
 */
public interface ComponentController {
    /**
     * Get the ManagementConsole this ComponentController interacts with.
     */
    public ManagementConsole getManagementConsole();

    /**
     * Get the name of the component.
     */
    public String getName();
}