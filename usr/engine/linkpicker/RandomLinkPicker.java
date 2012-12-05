/** Interface choses n links using some rule*/

package usr.engine.linkpicker;
import java.util.*;
import usr.globalcontroller.*;
import org.w3c.dom.*;

public class RandomLinkPicker implements NodeLinkPicker {
    Random rand;
    
    public RandomLinkPicker()
    {
        rand= new Random();
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
        if (nodes.size() == 0)
            return -1;
        int newLink= rand.nextInt(nodes.size());
        return nodes.remove(newLink); 
    }
    
    public void parseExtraXML(Node linkpicker)
    {
        
    }
}
