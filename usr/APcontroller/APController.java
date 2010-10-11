package usr.APcontroller;

import java.util.*;
import usr.logging.*;
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
    
    /** Underlying network adds node */
    public void addNode(int gid);
    
    /** Underlying network adds link */
    public void addLink(int gid1, int gid2);
    
    /** Underlying network removes node */
    public void removeNode(int gid);
    
    /** Underlying network removes link */
    public void removeLink(int gid1,int gid2);
    
}
