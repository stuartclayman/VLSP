package usr.APcontroller;

import java.util.*;
import usr.router.Router;
import usr.globalcontroller.GlobalController;
import usr.router.RouterOptions;

/** Implements Random AP Controller */

public class NullAPController implements APController {

    ArrayList<Integer> APGIDs_= null;  // list of accessPoints
    RouterOptions options_= null;

    
    NullAPController (RouterOptions o) {
        APGIDs_= new ArrayList<Integer>();
        options_= o;
    } 
    
    /** Return number of access points */
    public int getNoAPs()
    {
        return APGIDs_.size();
    }  
    
    /** Return list of access points */
    public List<Integer> getAPlist() {
        return APGIDs_;
    }
    
        
    /** Router regular AP update action */
    public void routerUpdate(Router r) 
    {
        
    }
    
    /** Controller regular AP update action */
    public void controllerUpdate(GlobalController g)
    {
    
    }
    
    /** Add new access point with gid G*/
    public void addAccessPoint(int gid)
    {
        if (APGIDs_.indexOf(gid) != -1) {
            System.err.println("AP controller found access point present when adding");
            return;
        }
        APGIDs_.add(gid);
    }
    
    /** Remove access point with gid G*/
    public void removeAccessPoint(int gid)
    {
        int index;
       if ((index= APGIDs_.indexOf(gid)) == -1) {
            System.err.println("AP controller could not find access point when removing");
            return;
        }
        APGIDs_.remove(index);
    }
}
