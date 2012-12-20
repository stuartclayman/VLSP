/** Interface choses n links using some rule*/

package usr.engine.linkpicker;
import java.util.*;
import usr.common.*;
import usr.globalcontroller.*;
import org.w3c.dom.*;

public class LifetimeLinkPicker implements NodeLinkPicker {
    Random rand_;
    ArrayList <Pair<Integer,Integer>> lifetimes_;
    
    public LifetimeLinkPicker()
    {
        rand_= new Random();
    }
    
    public ArrayList <Integer> pickNLinks(ArrayList<Integer> nodes,
        GlobalController g, int noLinks, int node)
    {
        ArrayList<Integer> picked= new ArrayList<Integer>();
        for (int i= 0; i < noLinks; i++) {
            if (nodes.size() == 0)
                break;
            int got= pickLink(nodes,g, node);
            picked.add(got); 
        }
        return picked;
    }
    
    public int pickLink(ArrayList<Integer> nodes, GlobalController g,
        int node)
    {
        return 0;
    }
    
    public void sortLifetimes(ArrayList <Integer> nodes)
    {
        
        Collections.sort(lifetimes_);
    }
    
    public void parseExtraXML(Node linkpicker)
    {
        
    }
}
