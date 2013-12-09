/** This class deals with the scheduling of events
 * it keeps a time ordered list of events and returns the first
 */
package usr.events;

import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
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

    private EventDelegate delegate_;   // Hook back to gc

    public SimpleEventScheduler(boolean isSimulation, EventDelegate gc) {
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
        delegate_ = gc;
    }

    /** Used if we are in emulation mode
     * Strips off an event, sends it to the  delegate for execution */
    @Override
    public void run() {
        while (true) {
            Event ev = getFirstEvent();
            long expectedStart = 0;

            if (ev == null) {
                Logger.getLogger("log").logln(USR.ERROR, "Run out of events to process");
                delegate_.deactivate();
                waitForever();
            } else {
                expectedStart = ev.getTime() + simulationStartTime_;
                waitUntil(expectedStart);
            }

            if (!delegate_.isActive()) {
                break;
            }



            long lag = System.currentTimeMillis() - expectedStart;
            Logger.getLogger("log").logln(USR.STDOUT,
                                          leadin()+"Lag is "+lag+" starting event "+ev);

            if (lag > delegate_.getMaximumLag()) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              leadin()+ delegate_.getName() + "  problem -- "
                                              + "lag is greater than allowed in options "
                                              + " allowed: " + delegate_.getMaximumLag() + " saw " +
                                              lag);
                delegate_.deactivate();
            }

            try {
                JSONObject js= delegate_.executeEvent(ev);
                Boolean success= (Boolean)js.get("success");
                if (! success) {
                    Logger.getLogger("log").logln(USR.ERROR, "Event "+ev+" failed");
                    delegate_.deactivate();
                }
            } catch (InstantiationException ine) {
                Logger.getLogger("log").logln(USR.ERROR,
                                              "Unexpected error in scheduled operation: "
                                              + ine.getMessage() + "\nEvent: "+ ev.toString());
                ine.printStackTrace();
                delegate_.deactivate();
            } catch (InterruptedException ie) {
                Logger.getLogger("log").logln(USR.ERROR, delegate_.getName() + " problem -- scheduler interrupted");
                delegate_.deactivate();
            } catch (TimeoutException te) {
                Logger.getLogger("log").logln(USR.ERROR, delegate_.getName() + " must be lagging, cannot interrupt "
                                              + "scheduler in time");
                delegate_.deactivate();
            } catch (JSONException e) {
                Logger.getLogger("log").logln(USR.ERROR, "Event "+ev+" failed to return success in JSON");
                delegate_.deactivate();
            }

            lag = System.currentTimeMillis() - expectedStart;
            Logger.getLogger("log").logln(USR.STDOUT, leadin()+"Lag is "+lag+" finishing event "+ev);
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
            if (delegate_.isActive()) {
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
            if (delegate_.isActive()) {
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

    /* (non-Javadoc)
     * return list of events
     */
    public ArrayList <Event> getEvents()
    {
    	return schedule_;
    }

    /**
     * Create the String to print out before a message
     */
    String leadin() {
        return "SEvSch: ";
    }


}
