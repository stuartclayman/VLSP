package usr.router;

/**
 * Some stats for a NetIF.
 */
public class NetStats implements Cloneable {
    int [] stats = new int[16];

    /**
     * Constructor
     */
    public NetStats() {
    }

    /**
     * Increment a stat
     */
    public NetStats increment(Stat stat) {
        stats[stat.ordinal()]++;
        return this;
    }

    /**
     * Add to a stat
     */
    public NetStats add(Stat stat, int value) {
        stats[stat.ordinal()] += value;
        return this;
    }

    /**
     * Set a value.
     */
    public NetStats setValue(Stat stat, int value) {
        stats[stat.ordinal()] = value;
        return this;
    }

    /**
     * Get a value
     */
    public int getValue(Stat stat) {
        return stats[stat.ordinal()];
    }

    /**
     * Create a copy of a NetStats object.
     */
    @Override
	public Object clone() {
        NetStats theClone = new NetStats();
        System.arraycopy(stats, 0, theClone.stats, 0, 16);

        return theClone;
    }

    /**
     * To String
     */
    @Override
	public String toString() {
        StringBuilder builder = new StringBuilder();

        for (Stat s : Stat.values()) {
            builder.append(" ");
            builder.append(s.toString());
            builder.append("=");
            builder.append(getValue(s));
        }

        return builder.toString();
    }

    public enum Stat {
        InBytes,
        InPackets,
        InErrors,
        InDropped,
        InDataBytes,
        InDataPackets,
        OutBytes,
        OutPackets,
        OutErrors,
        OutDropped,
        OutDataBytes,
        OutDataPackets,
        InQueue,
        BiggestInQueue,
        OutQueue,
        BiggestOutQueue
    }
}