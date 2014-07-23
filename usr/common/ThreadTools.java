package usr.common;

import usr.logging.Logger;
import usr.logging.USR;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.management.ThreadInfo;

public class ThreadTools {
    // can turn on printing with:
    // ThreadTools.on = true
    public static boolean on = true;


    /**
     * Get the Threads in a ThreadGroup
     */
    public static Thread[] getGroupThreads( final ThreadGroup group ) {
        return getGroupThreads(group, false);
    }

    /**
     * Get the Threads in a ThreadGroup recursively
     */
    public static Thread[] getGroupThreadsRecursive( final ThreadGroup group ) {
        return getGroupThreads(group, true);
    }

    /**
     * Get the Threads in a ThreadGroup.
     * Specify if to do it recursively.
     */
    private static Thread[] getGroupThreads( ThreadGroup group, final boolean recursively ) {
        if ( group == null )
            throw new NullPointerException( "Null thread group" );

        // Get threads in `group'
        int numThreads = group.activeCount();
        Thread[] threads = new Thread[numThreads*2];
        numThreads = group.enumerate(threads, recursively);

        return java.util.Arrays.copyOf( threads, numThreads );
    }

    /**
     * Get the ThreadGroups in a ThreadGroup
     */
    public static ThreadGroup[] getGroupThreadGroups( final ThreadGroup group ) {
        return getGroupThreadGroups(group, false);
    }

    /**
     * Get the ThreadGroups in a ThreadGroup recursively
     */
    public static ThreadGroup[] getGroupThreadGroupsRecursive( final ThreadGroup group ) {
        return getGroupThreadGroups(group, true);
    }

    /**
     * Get the ThreadGroups in a ThreadGroup.
     * Specify if to do it recursively.
     */
    private static ThreadGroup[] getGroupThreadGroups( ThreadGroup group, final boolean recursively) {
        if ( group == null )
            throw new NullPointerException( "Null thread group" );

        /*
        while (group.getParent() != null) {
            group = group.getParent();
        }
        */

        // Get thread groups in `group'
        int numGroups = group.activeGroupCount();
        ThreadGroup[] groups = new ThreadGroup[numGroups*2];
        numGroups = group.enumerate(groups, recursively);

        return java.util.Arrays.copyOf( groups, numGroups );
    }

    /**
     * Accumulate all the cpu data for a list of threads.
     * Returns a 3-tuple of { cpu, user, sys }
     */
    public static long[] visitThreadsAccumulate(Thread[] threads) {
        long cpu = 0;
        long user = 0;
        long sys = 0;

        ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();

        // List every thread in the group
        for (int i=0; i<threads.length; i++) {
            Thread t = threads[i];

            long id = t.getId();

            ThreadInfo info = mxBean.getThreadInfo(id);

            long threadCPU = mxBean.getThreadCpuTime(id);
            long threadUser = mxBean.getThreadUserTime(id);

            cpu += threadCPU;
            user += threadUser;
            sys += (threadCPU - threadUser);
        }

        long [] result =  { cpu, user, sys };
        return result;

        //System.out.println(" " + time + " Thread " + name + "." + " cpu: " + (cpu/1000000) + " user: " + (user/1000000) + " system: " + (sys/1000000));
    }


    /**
     * Find all the Threads and recursively print out a tree
     */
    public static void findAllThreads(String leadin) {
        if (!on) {
            return;
        } else {
            // Find the root thread group
            ThreadGroup root = Thread.currentThread().getThreadGroup().getParent();

            while (root.getParent() != null) {
                root = root.getParent();
            }

            // Visit each thread group
            visit(root, 0, leadin);
        }
    }

    // This method recursively visits all thread groups under `group'.
    public static void visit(ThreadGroup group, int level, String leadin) {
        Thread[] threads = getGroupThreads(group);
        int numThreads = threads.length;

        // group name
        if (on) {
            for (int sp = 0; sp < level; sp++) {
                System.err.print("  ");
            }
            Logger.getLogger("log").logln(USR.ERROR, leadin + " " + group.getName());
        }

        // Enumerate each thread in `group'
        for (int i = 0; i<numThreads; i++) {
            // Get thread
            Thread thread = threads[i];

            for (int sp = 0; sp < level; sp++) {
                if (on) {
                    System.err.print("  ");
                }
            }

            if (on) {
                Logger.getLogger("log").logln(USR.ERROR, leadin + " " + thread);
            }
        }

        // Get thread subgroups of `group'
        ThreadGroup[] groups = getGroupThreadGroups(group);
        int numGroups = groups.length;

        // Recursively visit each subgroup
        for (int i = 0; i<numGroups; i++) {
            visit(groups[i], level+1, leadin);
        }
    }

}
