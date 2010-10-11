package usr.APcontroller;

import java.util.*;
import usr.router.RouterController;
import usr.globalcontroller.GlobalController;
import usr.router.RouterOptions;

/** Implements Random AP Controller -- default actions are from NullAPController*/

public class RandomAPController extends NullAPController {

    RandomAPController (RouterOptions o) {
        super(o);
    } 
        
    /** Router regular AP update action */
    public void routerUpdate(RouterController r) 
    {
        //System.err.println ("Controller called");
    }
    
    /** Controller regular AP update action */
    public void controllerUpdate(GlobalController g)
    {
        if (gotMinAPs(g)) {
            if (overMaxAPs(g)) {   // Too many APs, remove one
                int nAPs= getNoAPs();
                int i= (int)Math.floor( Math.random()*nAPs);
                unElectNode(APGIDs_.get(i),g);
                
            }
            return;
        }
        ArrayList <Integer> elect= nonAPNodes(g);
        
        // No nodes which can be made managers
        int nNodes= elect.size();
        if (nNodes == 0) {
            return;
        }
        // Choose a random node to become an AP manager
        int index= (int)Math.floor( Math.random()*nNodes);
        electNode(elect.get(index),g);
        
    }
    
     /** Create new APInfo */
    
    public APInfo newAPInfo() {
        return new RandomAPInfo();
    }
    
}
