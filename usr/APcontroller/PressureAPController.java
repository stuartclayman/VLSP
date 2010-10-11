package usr.APcontroller;

import java.util.*;
import usr.logging.*;
import usr.router.Router;
import usr.globalcontroller.GlobalController;
import usr.router.RouterOptions;

/** Implements HotSpotAP Controller -- default actions are from NullAPController*/

public class PressureAPController extends NullAPController {

    PressureAPController (RouterOptions o) {
        super(o);
    } 
        
    /** Router regular AP update action */
    public void routerUpdate(Router r) 
    {
        
    }
    
    /** Controller regular AP update action */
    public void controllerUpdate(GlobalController g)
    {
    
    }
    
}
