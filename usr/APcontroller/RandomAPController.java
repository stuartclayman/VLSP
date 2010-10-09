package usr.APcontroller;

import java.util.*;
import usr.logging.*;
import usr.router.Router;
import usr.globalcontroller.GlobalController;
import usr.router.RouterOptions;

/** Implements Random AP Controller -- default actions are from NullAPController*/

public class RandomAPController extends NullAPController {

    RandomAPController (RouterOptions o) {
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
