/** This class deals with the scheduling of events
 * it keeps a time ordered list of events and returns the first
 */
package usr.events;

import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import usr.logging.Logger;
import usr.logging.USR;
import usr.output.OutputType;

/**
 * A SimpleEventScheduler is here to implement an EventScheduler.
 */
public class SimpleEventScheduler extends AbstractEventScheduler implements EventScheduler, Runnable {
    boolean isSimulation_ = true;


    public SimpleEventScheduler(EventDelegate ed) {
        this(false, ed);
    }


    public SimpleEventScheduler(boolean isSimulation, EventDelegate gc) {
        isSimulation_ = isSimulation;
        schedule_ = new ArrayList<Event>();

        if (isSimulation) {
            runStartTime_ = 0;
        } else {
            runStartTime_ = System.currentTimeMillis();
        }

        lastEventTime_ = 0;
        waitCounter_ = new Object();
        delegate_ = gc;
    }



    /** Return the time since the start of the run */
    @Override
    public long getElapsedTime() {
        if (isSimulation_) {
            return lastEventTime_ - getStartTime();
        } else {
            return System.currentTimeMillis() - getStartTime();
        }
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        return "EventScheduler: ";
    }


}
