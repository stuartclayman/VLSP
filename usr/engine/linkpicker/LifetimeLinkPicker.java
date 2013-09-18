/** Interface choses n links using some rule*/

package usr.engine.linkpicker;
import java.util.ArrayList;
import java.util.PriorityQueue;

import org.w3c.dom.Node;

import usr.globalcontroller.GlobalController;
import usr.lifeEstimate.NodeAndLifetime;
import usr.lifeEstimate.LifetimeEstimate;
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

    @Override
	public ArrayList <Integer> pickNLinks(ArrayList<Integer> nodes,
        GlobalController g, int noLinks, int node)
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

    @Override
	public int pickLink(ArrayList<Integer> nodes, GlobalController g,
        int node)
    {
    	long time= g.getElapsedTime();
    	createQueue(nodes, time);
        int thisNode= orderNodes_.poll().getNode();
        orderNodes_.clear();
        return thisNode;
    }


    private void createQueue(ArrayList<Integer> nodes, long time)
    {
    	for (int n: nodes) {
    		NodeAndLifetime nl= new NodeAndLifetime(n,lte_.getNodeLife(n,time));
    		orderNodes_.add(nl);
    	}
    }

    @Override
	public void parseExtraXML(Node linkpicker)
    {

    }
}
