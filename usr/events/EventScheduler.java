/** This class deals with the scheduling of events
 * it keeps a time ordered list of events and returns the first
 */
package usr.events;

import usr.logging.*;
import java.util.*;
import java.util.concurrent.*;
import usr.globalcontroller.*;

public class EventScheduler implements Runnable
{
private ArrayList <Event> schedule_ = null;
private long simulationTime_;            // Current time in simulation
private long simulationStartTime_;  // time at which simulation
                                   // started  (assuming realtime)
private long lastEventTime_;
private long lastEventLength_;      // length of time previous event
                                    // took
private Object waitCounter_;        //  Just used in a wait loop
boolean isSimulation_= true;

private GlobalController controller_;    // Hook back to gc

public EventScheduler(boolean isSimulation, GlobalController gc){
    isSimulation_= isSimulation;
    schedule_ = new ArrayList <Event>();
    if (isSimulation) {
        simulationStartTime_= 0;
    } else {
        simulationStartTime_= System.currentTimeMillis();
    }
    lastEventTime_= 0;
    simulationTime_= simulationStartTime_;
    waitCounter_= new Object();
    controller_= gc;
}

/** Used if we are in emulation mode
 * Strips off an event, sends it to the global controller for
 * execution */
public void run() 
{
    while (true) {
        Event ev= getFirstEvent();
        waitUntil(ev.getTime());
        if (!controller_.isActive())
            break;
        // Fix me -- detect large lag
        try {
            controller_.executeEvent(ev);
        } catch (InstantiationException ine) {
            Logger.getLogger("log").logln(USR.ERROR,
                "Unexpected error in scheduled operation: "+
                    ine.getMessage());
            controller_.deactivate();
        } catch (InterruptedException ie) {
            Logger.getLogger("log").logln(USR.ERROR,
                "Global Controller problem -- scheduler interrupted");
            controller_.deactivate();
        } catch (TimeoutException te) {
            Logger.getLogger("log").logln(USR.ERROR,
                "Global Controller must be lagging, cannot interrupt "+
                "scheduler in time");
            controller_.deactivate();
        }
    }
}

/** Return the time since the start of the simulation*/
public long getElapsedTime()
{
    if (isSimulation_) {
        return lastEventTime_- simulationStartTime_;
    }
    return System.currentTimeMillis()-simulationStartTime_;
}

/** Return start time */
public long getStartTime()
{
    return simulationStartTime_;
}

/** Return first event from schedule
 */
public Event getFirstEvent(){
    if (schedule_.size() == 0)
        return null;
    Event ev= schedule_.remove(0);
    lastEventTime_= ev.getTime();
    return ev;
}



/**
 * Wait until a specified absolute time is milliseconds.
 */
private void waitUntil(long time){
    long now = System.currentTimeMillis();

    if (time <= now)
        return;
    try {
        long timeout = time - now;
        Logger.getLogger("log").logln(USR.STDOUT, "EVENT: " + "<" + 
        lastEventLength_ + "> " + (now - simulationStartTime_) + " @ " +
            now + " waiting " + timeout);
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

/** Interrupt above wait*/
public void wakeWait(){
    synchronized (waitCounter_) {
        waitCounter_.notify();
    }
}

/** Adds an event to the schedule in time order
 */
public void addEvent(Event e){
    long time = e.getTime();

    for (int i = 0; i < schedule_.size(); i++) {
        if (schedule_.get(i).getTime() > time) {                
            // Add at given position in list
            schedule_.add(i, e);
            return;
        }
    }
    schedule_.add(e);           // Add at end of list
}

}
