/** This class deals with the scheduling of events
  it keeps a time ordered list of events and returns the first
*/
package usr.globalcontroller;
import java.util.*;

class EventScheduler {
    private ArrayList <SimEvent> schedule_= null;
    
  
    public EventScheduler() {
        schedule_= new ArrayList <SimEvent>();
        
    }

    /** Return first event from schedule
    */
    public SimEvent getFirstEvent() {
        if (schedule_.size() == 0) {
            return null;
        }
        return schedule_.remove(0);
    }
    
    public static long afterPause(long pause) {
        long time= System.currentTimeMillis();
        return time+pause;
    }
    
    /** Adds an event to the schedule in time order
    */
    public void addEvent(SimEvent e) {
        long time= e.getTime();
        for (int i= 0; i < schedule_.size(); i++) {
            if (schedule_.get(i).getTime() > time) {  // Add at given 
                schedule_.add(i,e);         // position in list
                return;
            }
        }
        schedule_.add(e);       // Add at end of list
    }
}

