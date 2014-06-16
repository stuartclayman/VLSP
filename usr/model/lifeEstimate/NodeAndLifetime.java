package usr.model.lifeEstimate;


/** Class represents a nodes internal number and its lifeTime estimate
 *
 */
public class NodeAndLifetime implements Comparable <NodeAndLifetime>{

	int nodeNo_;
	long lifetime_;

	public NodeAndLifetime(int n, long l)
	{
		nodeNo_= n;
		lifetime_= l;
	}

	public int compareTo(NodeAndLifetime nl)
	{
		if (nl.getLifetime() < lifetime_)
			return -1;
		if (nl.getLifetime() > lifetime_)
			return 1;
		return 0;
	}

	public int getNode()
	{
		return nodeNo_;
	}

	public long getLifetime()
	{
		return lifetime_;
	}
}
