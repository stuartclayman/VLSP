package usr.globalcontroller;

import usr.localcontroller.LocalControllerInfo;
import java.util.Set;

/**
 * A PlacementEngine is repsonsible for determining the placement
 * of a Router across the active resources.
 */
public interface PlacementEngine {

    /**
     * Get the relevant LocalControllerInfo for a placement of a router.
     */
    public LocalControllerInfo routerPlacement();

    /**
     * Get the relevant LocalControllerInfo for a placement of a router with a specific address.
     */
    public LocalControllerInfo routerPlacement(String address);

    /**
     * Get all the possible placement destinations
     */
    public Set<LocalControllerInfo> getPlacementDestinations();
}
