/** Interface choses n links using some rule*/

package usr.engine;
import java.util.*;
import usr.globalcontroller.*;

public class PreferentialLinkPicker implements NodeLinkPicker {
    Random rand;
    ArrayList<ArrayList<Integer>> nodesByDegree_;
    int totDegree_= 0;
    public PreferentialLinkPicker()
    {
        rand= new Random();
    }
    
    public ArrayList <Integer> pickNLinks(ArrayList<Integer> nodes,
        GlobalController g, int noLinks)
    {
        ArrayList<Integer> picked= new ArrayList<Integer>();
        updateNodes(g,nodes);
        for (int i= 0; i < noLinks; i++) {
            if (totDegree_ <= 0)
                break;
            int newLink= pickLinkWithoutUpdate();
            if (newLink == -1)
                break;
            picked.add(newLink); 
        }
        return picked;
    }
    
    /** Count the total degree and create a list of nodes grouped
     * by degree */
    private void updateNodes(GlobalController g,
        ArrayList<Integer> nodes)
    {
        nodesByDegree_= new ArrayList<ArrayList<Integer>>();
        totDegree_= 0;
        for (int node: nodes) {
            int deg= g.getOutLinks(node).length;
            while (deg >= nodesByDegree_.size()) {
                nodesByDegree_.add(new ArrayList<Integer>());
            }
            nodesByDegree_.get(deg).add(node);
            totDegree_+= deg;
        }
    }
    
    public int pickLink(ArrayList<Integer> nodes, GlobalController g)
    {
        updateNodes(g,nodes);
        return pickLinkWithoutUpdate();
    }
    
    /** Return -1 for no pick */    
    public int pickLinkWithoutUpdate()
    {
        if (totDegree_ <= 0) {
            return -1;
        }
        double weight= rand.nextDouble();
        int chosen= 0;
        double totWeight= 0;
        ArrayList <Integer>tmp;
        for (int i= 1; i <  nodesByDegree_.size(); i++) {
            tmp= nodesByDegree_.get(i);
            double thisWeight= (double)i*tmp.size()/totDegree_;
            totWeight+= thisWeight;
            if (totWeight > weight || i == nodesByDegree_.size()-1) {
                chosen= tmp.remove(rand.nextInt(tmp.size()));
                totDegree_-=i;
                break;
            }
        }
        return chosen;
    }
}
