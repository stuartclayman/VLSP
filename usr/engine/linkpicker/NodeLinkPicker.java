/** Interface choses n links using some rule*/

package usr.engine.linkpicker;
import java.util.*;
import usr.globalcontroller.*;
import org.w3c.dom.*;

public interface NodeLinkPicker {
    /** Return an array of several nodes from a list which will
     * connect to an origin node*/
    public ArrayList<Integer> pickNLinks(ArrayList<Integer> nodes, GlobalController g, int noLinks, int node);

    /** As above but a single node -- returns -1 if no node
     * picked*/
    public int pickLink(ArrayList<Integer> nodes, GlobalController g, int node);

    /** Parse any extra XML associated with configuration*/
    public void parseExtraXML(Node linkpicker);
}