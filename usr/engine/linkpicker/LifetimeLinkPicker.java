/** Interface choses n links using some rule*/

package usr.engine.linkpicker;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import org.w3c.dom.Node;

import usr.globalcontroller.GlobalController;
import usr.model.lifeEstimate.NodeAndLifetime;
import usr.model.lifeEstimate.LifetimeEstimate;
/**
 * Class chooses links based upon finding the node with the longest lifetime
 * @author richard
 *
 */
public class LifetimeLinkPicker implements NodeLinkPicker {
    PriorityQueue <NodeAndLifetime> orderNodes_;
    LifetimeEstimate lte_;


    public LifetimeLinkPicker()
    {
        orderNodes_= new PriorityQueue<NodeAndLifetime>();
        lte_= LifetimeEstimate.getLifetimeEstimate();
    }

     /** Return an array of several nodes from a list which will
     * connect to an origin node*/
    @Override
    public List <Integer> pickNLinks(List<Integer> nodes, GlobalController g, int noLinks, int node)
    {
        ArrayList<Integer> picked= new ArrayList<Integer>();
        long time= g.getElapsedTime();
        createQueue(nodes,time);
        for (int i= 0; i < noLinks; i++) {
            if (orderNodes_.size() == 0)
                break;
            int link= orderNodes_.poll().getNode();
            picked.add(link);
        }
        orderNodes_.clear();
        return picked;
    }

    /** As above but a single node -- returns -1 if no node
     * picked*/
    @Override
    public int pickLink(List<Integer> nodes, GlobalController g, int node)
    {
    	long time= g.getElapsedTime();
    	createQueue(nodes, time);
        int thisNode= orderNodes_.poll().getNode();
        orderNodes_.clear();
        return thisNode;
    }


    private void createQueue(List<Integer> nodes, long time)
    {
    	for (int n: nodes) {
    		NodeAndLifetime nl= new NodeAndLifetime(n,lte_.getNodeLife(n,time));
    		orderNodes_.add(nl);
    	}
    }

    /** Parse any extra XML associated with configuration*/
    @Override
    public void parseExtraXML(Node linkpicker)
    {

    }
}
