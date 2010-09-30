package usr.APcontroller;

import java.util.*;
import usr.router.Router;
import usr.globalcontroller.GlobalController;

/** Interface for aggregation point controller types */

public interface APController {
    
    /** Return number of access points allocated */
    public int getNoAPs();  
    
    /** Return list of access points */
    public List<Integer> getAPlist();  
    
    /** Router regular AP update action */
    public void routerUpdate(Router r);
    
    /** Controller regular AP update action */
    public void controllerUpdate(GlobalController g);
    
    /** Add new access point with gid G*/
    public void addAccessPoint(int gid);
    
    /** Remove access point with gid G*/
    public void removeAccessPoint(int gid);
}
