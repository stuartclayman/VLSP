package usr.APcontroller;

import java.util.*;
import usr.router.Router;

/** This interface is used to provide a router with the extra
   information required for a particular AP Controller */

public interface APInfo {

    /** Returns true if this router is an AP Controller */
    public boolean isAPController();

    /** Sets whether this router is an AP Controller */
    public void setAPController(boolean controller);

}