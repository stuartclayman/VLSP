/** Interface choses n links using some rule*/

package usr.engine;
import java.util.*;
import usr.globalcontroller.*;

public class RandomLinkPicker implements NodeLinkPicker {
    Random rand;
    
    public RandomLinkPicker()
    {
        rand= new Random();
    }
    
    public ArrayList <Integer> pickNLinks(ArrayList<Integer> nodes,
        GlobalController g, int noLinks)
    {
        ArrayList<Integer> picked= new ArrayList<Integer>();
        for (int i= 0; i < noLinks; i++) {
            if (nodes.size() == 0)
                break;
            int got= pickLink(nodes,g);
            picked.add(got); 
        }
        return picked;
    }
    
    public int pickLink(ArrayList<Integer> nodes, GlobalController g)
    {
        int newLink= rand.nextInt(nodes.size());
        return nodes.remove(newLink); 
    }
}
