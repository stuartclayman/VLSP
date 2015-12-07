package demo_usr.stress;

import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;


import usr.applications.Application;
import usr.applications.ApplicationResponse;
import usr.logging.Logger;
import usr.logging.USR;
import usr.common.ProcessWrapper;


/**
 * An application for starting a resource stressing application
 */
public class Stress implements Application {

    List<String> stressArgs;

    Process child;

    ProcessWrapper wrapper;

    boolean running = false;

    /**
     * Constructor for Stress
     */
    public Stress() {
        stressArgs = new ArrayList<String>();
    }


    /**
     * Initialisation for Recv.
     * Recv port
     */
    @Override
    public ApplicationResponse init(String[] args) {
        if (args.length == 0) {
            return new ApplicationResponse(false, "Usage: Stress args");

        } else {
            // stressArgs[0] = "stress"
            stressArgs.add("stress");

            // copy args
            for (String arg : args) {
                stressArgs.add(arg);
            }

            return new ApplicationResponse(true, "");

        }
    }

    /** Start application with argument  */
    @Override
    public ApplicationResponse start() {
        try {
            // start the ProcessBuilder
            ProcessBuilder pb = new ProcessBuilder(stressArgs);

            child = pb.start();

            wrapper = new ProcessWrapper(child, "stress");

        } catch (Exception e) {
            Logger.getLogger("log").logln(USR.ERROR, "Cannot start process " + e.getMessage());
            return new ApplicationResponse(false, "Cannot start process " + e.getMessage());
        }

        running = true;

        return new ApplicationResponse(true, "");
    }

    /** Implement graceful shut down */
    @Override
    public ApplicationResponse stop() {
        try {
            running = false;

            wrapper.stop();
        
            Logger.getLogger("log").logln(USR.STDOUT, "stop() Stress stop");

            return new ApplicationResponse(true, "");
        } catch (Exception e) {
            return new ApplicationResponse(false, "Process waitFor Exception: " + e.getMessage());
        }
    }

    /** Run the application */
    @Override
    public void run() {
        try {
            child.waitFor();
            
        } catch (InterruptedException ie) {
            System.out.println("Process waitFor interrupted: " + ie.getMessage());
        }

        System.out.println("run() Process waitFor completed");
    }

}

