/** Interface choses n links using some rule*/

package usr.engine.linkpicker;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.w3c.dom.Node;

import usr.globalcontroller.GlobalController;

public class PreferentialLinkPicker implements NodeLinkPicker {
    Random rand;
    ArrayList<ArrayList<Integer> > nodesByDegree_;
    int totDegree_ = 0;
    public PreferentialLinkPicker() {
        rand = new Random();
    }

    @Override
    public List<Integer> pickNLinks(List<Integer> nodes, GlobalController g, int noLinks, int node) {
        ArrayList<Integer> picked = new ArrayList<Integer>();
        updateNodes(g, nodes);

        for (int i = 0; i < noLinks; i++) {
            if (totDegree_ <= 0) {
                break;
            }

            int newLink = pickLinkWithoutUpdate();

            if (newLink == -1) {
                break;
            }

            picked.add(newLink);
        }

        return picked;
    }

    /** Count the total degree and create a list of nodes grouped
     * by degree */
    private void updateNodes(GlobalController g, List<Integer> nodes) {
        nodesByDegree_ = new ArrayList<ArrayList<Integer> >();
        totDegree_ = 0;

        for (int node : nodes) {
            int deg = g.getOutLinks(node).size();
            if (deg == 0)
                deg= 1;

            while (deg >= nodesByDegree_.size()) {
                nodesByDegree_.add(new ArrayList<Integer>());
            }

            nodesByDegree_.get(deg).add(node);
            totDegree_ += deg;
        }
    }

    @Override
    public int pickLink(List<Integer> nodes, GlobalController g, int node) {
        updateNodes(g, nodes);
        return pickLinkWithoutUpdate();
    }

    /** Return -1 for no pick */
    public int pickLinkWithoutUpdate() {
        if (totDegree_ <= 0) {
            return -1;
        }

        double weight = rand.nextDouble();
        int chosen = 0;
        double totWeight = 0;
        ArrayList<Integer> tmp;

        for (int i = 1; i < nodesByDegree_.size(); i++) {
            tmp = nodesByDegree_.get(i);
            double thisWeight = (double)i * tmp.size() / totDegree_;
            totWeight += thisWeight;

            if ((totWeight > weight) || (i == nodesByDegree_.size() - 1)) {
                chosen = tmp.remove(rand.nextInt(tmp.size()));
                totDegree_ -= i;
                break;
            }
        }

        return chosen;
    }

    @Override
    public void parseExtraXML(Node linkpicker) {
    }

}