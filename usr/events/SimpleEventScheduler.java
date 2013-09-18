/** This class deals with the scheduling of events
 * it keeps a time ordered list of events and returns the first
 */
package usr.events;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import usr.globalcontroller.GlobalController;
import usr.logging.Logger;
import usr.logging.USR;

public class SimpleEventScheduler implements EventScheduler, Runnable {
    private ArrayList<Event> schedule_ = null;
    private long simulationTime_;         // Current time in simulation
    private long simulationStartTime_;    // time at which simulation
                                          // started  (assuming
                                          // realtime)
    private long lastEventTime_;
    private long lastEventLength_;        // length of time previous
                                          // event
                                          // took
    private Object waitCounter_;          //  Just used in a wait loop
    boolean isSimulation_ = true;

    private GlobalController controller_;   // Hook back to gc

    public SimpleEventScheduler(boolean isSimulation, GlobalController gc) {
        isSimulation_ = isSimulation;
        schedule_ = new ArrayList<Event>();

        if (isSimulation) {
            simulationStartTime_ = 0;
        } else {
            simulationStartTime_ = System.currentTimeMillis();
        }

        lastEventTime_ = 0;
        simulationTime_ = simulationStartTime_;
        waitCounter_ = new Object();
        controller_ = gc;
    }

    /** Used if we are in emulation mode
     * Strips off an event, sends it to the global controller for
     * execution */
    @Override
	public void run() {
        while (true) {
            Event ev = getFirstEvent();
            long expectedStart = 0;

            if (ev == null) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              "Run out of events to process");
                controller_.deactivate();
                waitForever();
            } else {
                expectedStart = ev.getTime() + simulationStartTime_;
                waitUntil(expectedStart);
            }

            if (!controller_.isActive()) {
                break;
            }

            

            long lag = System.currentTimeMillis() - expectedStart;
            Logger.getLogger("log").logln(USR.STDOUT,
                        leadin()+"Lag is "+lag+" starting event "+ev);

            if (lag > controller_.getMaximumLag()) {
                Logger.getLogger("log").logln(USR.ERROR,
                        leadin()+"Global Controller problem -- "
                        + "lag is greater than allowed in options "
                        + " allowed: " + controller_.getMaximumLag() + " saw " +
                        lag);
                controller_.deactivate();
            }

            try {
                controller_.executeEvent(ev);
            } catch (InstantiationException ine) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              "Unexpected error in scheduled operation: "
                                              + ine.getMessage());
                controller_.deactivate();
            } catch (InterruptedException ie) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              "Global Controller problem -- scheduler interrupted");
                controller_.deactivate();
            } catch (TimeoutException te) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              "Global Controller must be lagging, cannot interrupt "
                                              + "scheduler in time");
                controller_.deactivate();
            }
            lag = System.currentTimeMillis() - expectedStart;
            Logger.getLogger("log").logln(USR.STDOUT,
                        leadin()+"Lag is "+lag+" finishing event "+ev);
        }
    }

    /** Return the time since the start of the simulation*/
    @Override
	public long getElapsedTime() {
        if (isSimulation_) {
            return lastEventTime_ - simulationStartTime_;
        }

        return simulationTime_ - simulationStartTime_;
    }

    /** Return start time */
    @Override
	public long getStartTime() {
        return simulationStartTime_;
    }

    /** Get the current time into the simulation.
     * It is important to note that this can be called between events,
     * therefore simulationTime_ needs to be updated as needed.
     */
    @Override
	public long getSimulationTime() {
        long current = System.currentTimeMillis();

        if (current - simulationTime_ > 1000)  {   // more than 1 second out
            simulationTime_ = current;
        }


        return simulationTime_;
    }

    /** Return first event from schedule
     */
    @Override
	public Event getFirstEvent() {
        if (schedule_.size() == 0) {
            return null;
        }

        Event ev = schedule_.remove(0);
        lastEventTime_ = ev.getTime();
        return ev;
    }

    /**
     * Wait until a specified absolute time is milliseconds.
     */
    private void waitUntil(long time) {
        long now = System.currentTimeMillis();

        if (time <= now) {
            return;
        }

        try {
            long timeout = time - now;
            Logger.getLogger("log").logln(USR.STDOUT,
                "EVENT: " + "<"
                + lastEventLength_ + "> " + (now - simulationStartTime_) + " @ "
                + now + " waiting " + timeout);
            synchronized (waitCounter_) {
                waitCounter_.wait(timeout);
            }
            lastEventLength_ = System.currentTimeMillis() - now;
        } catch (InterruptedException e) {
            if (controller_.isActive()) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              "Scheduler interrupted without reason");
            }
        }
    }

    /**
     * Wait forever -- no events in scheduler
     */
    private void waitForever() {
        try {
            synchronized (waitCounter_) {
                waitCounter_.wait();
            }
        } catch (InterruptedException e) {
            if (controller_.isActive()) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              "Scheduler interrupted without reason");
            }
        }
    }

    /** Interrupt above wait*/
    @Override
	public void wakeWait() {
        synchronized (waitCounter_) {
            waitCounter_.notify();
        }
    }

    /** Adds an event to the schedule in time order
     */
    @Override
	public void addEvent(Event e) {
        Logger.getLogger("log").logln(USR.STDOUT, leadin() + 
            "Adding Event at time: " + e.getTime() + " Event " + e );

        long time = e.getTime();

        for (int i = 0; i < schedule_.size(); i++) {
            if (schedule_.get(i).getTime() > time) {
                // Add at given position in list
                schedule_.add(i, e);
                //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Event position " +  i);
                return;
            }
        }

        schedule_.add(e);   // Add at end of list
        //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Event position " +  "END");
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        return "SEvSch: ";
    }


}
