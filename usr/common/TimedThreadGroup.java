package usr.common;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.management.ThreadInfo;

/**
 * A timed thread group keeps its own start time
 * so it is possible to work out the elapsed time
 * a ThreadGroup a run for.
 */
public class TimedThreadGroup extends ThreadGroup {
    // the start time - in millis
    long startTime;

    // ThreadMXBean
    ThreadMXBean mxBean;

    /**
     * Construct a TimedThreadGroup
     */
    public TimedThreadGroup(String name) {
        super(name);
        startTime = System.currentTimeMillis();

        mxBean = ManagementFactory.getThreadMXBean();

        //System.out.println( "NEW TimedThreadGroup: " + this.getName() + " parent: " + getParent().getName());
    }

    /**
     * Construct a TimedThreadGroup
     */
    public TimedThreadGroup(ThreadGroup parent, String name) {
        super(parent, name);
        startTime = System.currentTimeMillis();

        mxBean = ManagementFactory.getThreadMXBean();
    }

    /**
     * Get the start time.
     * As milliseconds
     */
    public long getStartTime() {
        return startTime;
    }


    /**
     * Get the elapsed time.
     * In milliseconds.
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }


    /**
     * Get cpu usage and memory usage info.
     * Returns  { cpu, user, sys, mem };
     */
    public long[] getUsage() {
        long cpu = 0;
        long user = 0;
        long sys = 0;
        long mem = 0;

        // Get threads in `group'
        Thread[] threads = ThreadTools.getGroupThreadsRecursive(this);
        int numThreads = threads.length;


        //System.out.println( "TimedThreadGroup: " + this.getName() + " threads: " + numThreads + " T[] = " + java.util.Arrays.asList(threads));

        // Enumerate each thread in `group'
        for (int i = 0; i<numThreads; i++) {
            // Get thread
            Thread thread = threads[i];

            long id = thread.getId();

            ThreadInfo info = mxBean.getThreadInfo(id);

            long threadCPU = mxBean.getThreadCpuTime(id);
            long threadUser = mxBean.getThreadUserTime(id);

            cpu += threadCPU;
            user += threadUser;
            sys += (threadCPU - threadUser);

            if (mxBean instanceof com.sun.management.ThreadMXBean) {

                com.sun.management.ThreadMXBean sunMxBean = (com.sun.management.ThreadMXBean)mxBean;
                mem += sunMxBean.getThreadAllocatedBytes(id);
            }
            
        }

        //System.out.println( "TimedThreadGroup: " + this.getName() + " cpu: " + cpu + " user: " + user + " sys: " + sys);

        long [] result =  { cpu, user, sys, mem };

        return result;

    }

}
