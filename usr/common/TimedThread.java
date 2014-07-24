package usr.common;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.management.ThreadInfo;

/**
 * A timed thread keeps its own start time
 * so it is possible to work out the elapsed time
 * a Thread a run for.
 */
public class TimedThread extends Thread {
    // the start time - in millis
    long startTime;

    // ThreadMXBean
    ThreadMXBean mxBean;

    /**
     * Construct a TimedThread
     */
    public TimedThread(Runnable r) {
        super(r);
        startTime = System.currentTimeMillis();

        mxBean = ManagementFactory.getThreadMXBean();
    }

    /**
     * Construct a TimedThread
     */
    public TimedThread() {
        super();
        startTime = System.currentTimeMillis();

        mxBean = ManagementFactory.getThreadMXBean();
    }

    /**
     * Construct a TimedThread
     */
    public TimedThread(Runnable r, String name) {
        super(r, name);
        startTime = System.currentTimeMillis();

        mxBean = ManagementFactory.getThreadMXBean();
    }

    /**
     * Construct a TimedThread
     */
    public TimedThread(ThreadGroup group, Runnable r, String name) {
        super(group, r, name);
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
        long id = getId();

        ThreadInfo info = mxBean.getThreadInfo(id);

        long threadCPU = mxBean.getThreadCpuTime(id);
        long threadUser = mxBean.getThreadUserTime(id);

        long cpu = threadCPU;
        long user = threadUser;
        long sys = (threadCPU - threadUser);

        long mem = 0;

        if (mxBean instanceof com.sun.management.ThreadMXBean) {

            com.sun.management.ThreadMXBean sunMxBean = (com.sun.management.ThreadMXBean)mxBean;
            mem = sunMxBean.getThreadAllocatedBytes(id);
        }
            


        long [] result =  { cpu, user, sys, mem };
        return result;

    }

}
