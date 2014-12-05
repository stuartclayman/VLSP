package usr.common;

import java.util.concurrent.ThreadFactory;

/**
 * Generate a Thread in a specified ThreadGroup.
 */
public class SimpleThreadFactory implements ThreadFactory {
    String name;
    ThreadGroup group;

    /**
     * Construct a ThreadFactory with a name
     */
    public SimpleThreadFactory(String name) {
        this.name = name;
        this.group = new TimedThreadGroup(name);
    }

    /**
     * Construct a ThreadFactory with a name
     */
    public SimpleThreadFactory(ThreadGroup parent, String name) {
        this.name = name;
        this.group = parent; // new TimedThreadGroup(parent, parent.getName());
    }

    /**
     * Create a new Thread in a specific ThreadGroup
     */
    @Override
    public Thread newThread(Runnable r) {
        return new TimedThread(group, r, name);
    }

}

