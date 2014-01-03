package ikms.processor;

import ikms.IKMS;
import ikms.core.Response;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cc.clayman.logging.Logger;
import cc.clayman.logging.MASK;

/**
 * The ProcessorManager is responsible for starting and stopping
 * applications.
 */
public class ProcessorManager {
    // The IKMS this is an ProcessorManager for.
    IKMS ikms;

    // A pool 
    ExecutorService pool;

    // A map of all the Processors, name -> ProcessorHandle object
    HashMap<String, ProcessorHandle> procMap;

    // An Proc ID / PID
    int procID = 0;

    /**
     * ProcessorManager Constructor.
     */
    public ProcessorManager(IKMS ikms) {
        // create an pool
        pool = Executors.newCachedThreadPool();

        procMap = new HashMap<String, ProcessorHandle>();
        this.ikms = ikms;
    }

    /**
     * Entry point to start an Processor.
     * Returns an Response with an proc name in it.
     * The procName is used to stop the proc, and is of the form
     * /Router-1/Processor/plugins_usr.aggregator.procl.InfoSource/2.
     */
    public synchronized Response startProcessor(String className, String[] args) {
        // args should be class name + args for class

        Response result = execute(className, args);

        return result;
    }

    /**
     * Entry point to stop an Processor.
     * Processor name is passed in, in the form
     * /Router-1/Processor/plugins_usr.aggregator.appl.InfoSource/2
     */
    public synchronized Response stopProcessor(String procName) {
        return terminate(procName);
    }

    /**
     * Entry point to stop all Processor.
     * Processor name is passed in.
     */
    public synchronized  void stopAll() {
        //System.err.println("Stopping all procs");
        shutdown();
        //System.err.println("Stopped all procs");
    }

    /**
     * Entry point to list Processors.
     */
    public synchronized Collection<ProcessorHandle> listProcessors() {
        return procMap.values();
    }

    /**
     * Get an ProcessorHandle for an Processor.
     */
    public synchronized ProcessorHandle find(String name) {
        return procMap.get(name);
    }

    /**
     * Execute an object with ClassName and args
     */
    private synchronized Response execute(String className, String[] args) {

        System.err.println( leadin() + "execute: " + className + " args: " + Arrays.asList(args));

        // Class
        Class<?> clazz;
        Constructor<? extends Processor> cons0;

        try {
            // get Class object
            clazz = (Class<?>)Class.forName(className);

            // check if the class implements the right interface
            // it is an Processor
            try {
                Class<? extends Processor> procClazz = clazz.asSubclass(Processor.class );
                cons0 = (Constructor<? extends Processor>)procClazz.getDeclaredConstructor();
            } catch (ClassCastException cce) {
                // it is not an Processor, so we cant run it
                Logger.getLogger("log").logln(MASK.ERROR, leadin() + "class " + className + " is not at Processor");
                return new Response(false, "class " + className + " is not at Processor");
            }

            // create an instance of the Processor
            Processor proc =  (Processor)cons0.newInstance();

            procID++;

            // set proc name
            String procName = "/" + "ikms" + "/Processor/" + className + "/" + procID;

            // initialize it
            Response initR;

            synchronized (proc) {
                initR = proc.init(args);
            }

            if (initR == null) {
                Logger.getLogger("log").logln(MASK.ERROR, leadin() + "Processor: " + procName + " failed to init");
                return new Response(false, "class " + className + " Processor failed to init");
            }

            // if init fails, return
            if (!initR.isSuccess()) {
                return initR;
            }

            // otherwise create an ProcessorHandle for the proc
            ProcessorHandle handle = new ProcessorHandle(this, procName, proc, args, procID);

            // try and start the proc
            Response startR;

            synchronized (proc) {
                startR = proc.start();
            }

            //  check if startR is null
            if (startR == null) {
                Logger.getLogger("log").logln(MASK.ERROR, leadin() + "Processor: " + handle + " failed to start");
                return new Response(false, "class " + className + " Processor failed to start");
            }


            // if start succeeded then go onto run()
            if (startR.isSuccess()) {
                // now add details to Processor map
                procMap.put(procName, handle);

                handle.setState(ProcessorHandle.ProcessorState.RUNNING);

                pool.execute(handle);

                //Logger.getLogger("log").logln(MASK.ERROR, leadin() + "pool = " + pool.getActiveCount() + "/" + pool.getTaskCount() + "/" + pool.getPoolSize());

                return new Response(true, procName);

            } else {
                // the proc did not start properly
                handle.setState(ProcessorHandle.ProcessorState.STOPPED);

                return startR;
            }

        } catch (ClassNotFoundException cnfe) {
            Logger.getLogger("log").logln(MASK.ERROR, leadin() + "ClassNotFoundException " + cnfe);
            return new Response(false, "ClassNotFoundException " + cnfe);

        } catch (NoClassDefFoundError ncdfe) {
            Logger.getLogger("log").logln(MASK.ERROR, leadin() + "NoClassDefFoundError " + ncdfe);
            return new Response(false, "NoClassDefFoundError " + ncdfe);

        } catch (NoSuchMethodException nsme) {
            Logger.getLogger("log").logln(MASK.ERROR, leadin() + "NoSuchMethodException " + nsme);
            return new Response(false,  "NoSuchMethodException " + nsme);

        } catch (InstantiationException ie) {
            Logger.getLogger("log").logln(MASK.ERROR, leadin() + "InstantiationException " + ie);
            return new Response(false,  "InstantiationException " + ie);

        } catch (IllegalAccessException iae) {
            Logger.getLogger("log").logln(MASK.ERROR, leadin() + "IllegalAccessException " + iae);
            return new Response(false,  "IllegalAccessException " + iae);

        } catch (InvocationTargetException ite) {
            Logger.getLogger("log").logln(MASK.ERROR, leadin() + "InvocationTargetException " + ite);
            return new Response(false,  "InvocationTargetException " + ite);

        }

    }

    /**
     * Stop an Processor
     */
    private Response terminate(String procName) {
        ProcessorHandle procH = procMap.get(procName);
        if (procH == null) {
            // no proc with that name
            return new Response(false, "No Processor called " + procName);
        } else {
            synchronized (procH) {  // This must not be called twice for the same procH simultaneously


                if (procH.getState() == ProcessorHandle.ProcessorState.STOPPED) {

                    // wait for the thread to actually end
                    //pool.waitFor(procH);

                    // and remove from the proc map
                    procMap.remove(procName);

                    return new Response(false, "Processor called " + procName + " already stopped");
                } else {
                    // NOT ProcessorHandle.ProcessorState.STOPPED
                    try {

                        Processor proc = procH.getProcessor();
                        // try and stop the proc
                        Response stopR = null;

                        System.err.println( leadin() + "stopping " + procName);


                        if (procH.getState() == ProcessorHandle.ProcessorState.RUNNING) {
                            procH.setState(ProcessorHandle.ProcessorState.STOPPING);
                            synchronized (proc) {
                                stopR = proc.stop();
                            }


                            // wait for the thread to actually end
                            // TODO: work out how to do this with cache pool
                            //pool.waitFor(procH);
                        } else if (procH.getState() == ProcessorHandle.ProcessorState.PROC_POST_RUN) {
                            // the proc had already exited the run loop
                            System.err.println( leadin() + "Cleanup proc after exiting run() " + procName);
                            synchronized (proc) {
                                stopR = proc.stop();
                            }

                        }


                        // stop state
                        procH.setState(ProcessorHandle.ProcessorState.STOPPED);

                        // and remove from the proc map
                        procMap.remove(procName);


                        //  check if stopR is null
                        if (stopR == null) {
                            Logger.getLogger("log").logln(MASK.ERROR, leadin() + "Processor: " + procH + " failed to stop");
                            return new Response(false, "Processor called " + procName + " failed to stop - returned null.");
                        } else {
                            return new Response(true, "");
                        }

                    } catch (Exception e) {
                        return new Response(false, "Processor called " + procName + " failed to stop with Exception " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Shutdown the ProcessorManager
     */
    private synchronized void shutdown() {
        System.err.println( leadin() + "shutdown ");

        Collection<ProcessorHandle> procs = new java.util.LinkedList<ProcessorHandle>(procMap.values());

        for (ProcessorHandle procH : procs) {
            System.err.println( leadin() + "Attempting to terminate "+procH);
            terminate(procH.getName());
        }

        System.err.println( leadin() + "pool shutdown ");
        //pool.shutdown();
    }

    /**
     * Get the IKMS this is an ProcessorManager for.
     */
    public IKMS getIKMS() {
        return ikms;
    }

    private String leadin() {
        return "ProcessorManager: ";
    }

}
