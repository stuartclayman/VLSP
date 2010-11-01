package usr.router;

/**
 * Some stats for a NetIF.
 */
public class NetStats {
    int [] stats = new int[12];

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
     * To String
     */
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
            OutBytes,
            OutPackets,
            OutErrors,
            OutDropped,
            InQueue,
            BiggestInQueue,
            OutQueue,
            BiggestOutQueue
    }
}
