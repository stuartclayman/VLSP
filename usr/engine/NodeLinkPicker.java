/** Interface choses n links using some rule*/

package usr.engine;
import java.util.*;
import usr.globalcontroller.*;

public interface NodeLinkPicker{
    public ArrayList <Integer> pickNLinks(ArrayList<Integer> nodes,
        GlobalController g, int noLinks);
    public int pickLink(ArrayList<Integer> nodes, GlobalController g);
}
