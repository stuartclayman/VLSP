package usr.globalcontroller;

import usr.localcontroller.LocalControllerInfo;
import java.util.Set;

/**
 * A PlacementEngine is repsonsible for determining the placement
 * of a Router across the active resources.
 */
public interface PlacementEngine {

    /**
     * Get the relevant LocalControllerInfo for a placement of a router with 
     * a specified name and address.
     */
    public LocalControllerInfo routerPlacement(String name, String address);

    /**
     * Get the relevant LocalControllerInfo for a placement of a router with 
     * a specified name and address, but also supplying extra parameters. It 
     * is used for prediction of future load.
     */
    public LocalControllerInfo routerPlacement(String name, String address, String parameters);
    
    /**
     * Get all the possible placement destinations
     */
    public Set<LocalControllerInfo> getPlacementDestinations();
}
