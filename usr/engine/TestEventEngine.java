/** Interface for Engine which adds events to the event list
*/

package usr.engine;

import usr.globalcontroller.*;
import usr.common.Pair;

public class TestEventEngine implements EventEngine {
    int timeToEnd_;

    /** Contructor from Parameter string */
    public TestEventEngine(int time, String parms) 
    {
        timeToEnd_= time;
    }
    
    /** Initial events to add to schedule */
    public void initialEvents(EventScheduler s, GlobalController g)
    {
                // simulation start
        SimEvent e0 = new SimEvent(SimEvent.EVENT_START_SIMULATION, 0, null);
        s.addEvent(e0);

        // simulation end
        SimEvent e= new SimEvent(SimEvent.EVENT_END_SIMULATION, timeToEnd_, null);
        s.addEvent(e);

        int mr= 5;
        for (int i= 0; i < mr; i++) {
            SimEvent e2= new SimEvent(SimEvent.EVENT_START_ROUTER, 
                                      timeToEnd_/3-1000,
                                      null);
            s.addEvent(e2);
        }
        for (int i= 0; i < mr-1; i++) {
            SimEvent e2= new SimEvent(SimEvent.EVENT_START_LINK, 
                                      timeToEnd_/2+1000,
                                      new Pair<Integer,Integer>(i+1,i+2));
            s.addEvent(e2);
        }
    }
    
    /** Add or remove events following a simulation event */
    public void preceedEvent(SimEvent e, EventScheduler s,  GlobalController g) 
    {
    
    }
    
    /** Add or remove events following a simulation event */
    public void followEvent(SimEvent e, EventScheduler s,  GlobalController g,
      Object o)
    {
    
    }

}
