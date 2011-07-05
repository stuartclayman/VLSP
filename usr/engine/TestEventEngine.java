/** Interface for Engine which adds events to the event list
*/

package usr.engine;

import usr.globalcontroller.*;
import usr.common.Pair;
import usr.logging.*;

public class TestEventEngine implements EventEngine {
    int timeToEnd_;

    /** Contructor from Parameter string */
    public TestEventEngine(int time, String parms) 
    {
        timeToEnd_= time*1000;
    }
    
            /** Start up and shut down events */
    public void startStopEvents(EventScheduler s, GlobalController g)
    {
        // simulation start
        SimEvent e;
        e = new SimEvent(SimEvent.EVENT_START_SIMULATION, 0, null,this);
        s.addEvent(e);
        // simulation end
	
        e= new SimEvent(SimEvent.EVENT_END_SIMULATION, timeToEnd_, null,this);
        s.addEvent(e);

    }
    
    /** Initial events to add to schedule */
    public void initialEvents(EventScheduler s, GlobalController g)
    {


        SimEvent e2= new SimEvent(SimEvent.EVENT_START_ROUTER, 
                                      250,null,this);
        
        s.addEvent(e2);                              
        e2= new SimEvent(SimEvent.EVENT_START_ROUTER, 
                                      250,
                                      null,this);
        s.addEvent(e2);
        e2= new SimEvent(SimEvent.EVENT_START_LINK, 250, new Pair<Integer,Integer>(1,2),this);
        s.addEvent(e2);
        e2= new SimEvent(SimEvent.EVENT_END_LINK, 300, new Pair<Integer,Integer>(1,2),this);
        s.addEvent(e2);
        e2= new SimEvent(SimEvent.EVENT_END_ROUTER, 301,2,this);
        s.addEvent(e2);
       /* int mr= 3;
        for (int i= 0; i < mr; i++) {
            SimEvent e2= new SimEvent(SimEvent.EVENT_START_ROUTER, 
                                      250*i,
                                      null);
            s.addEvent(e2);
        }
        for (int i= 0; i < mr-1; i++) {
            SimEvent e2= new SimEvent(SimEvent.EVENT_START_LINK, 
                                      250*(i+mr),
                                      new Pair<Integer,Integer>(i+1,i+2));
            s.addEvent(e2);
        }
        SimEvent e3= new SimEvent (SimEvent.EVENT_START_LINK,250*(2*mr+1),
            new Pair<Integer,Integer>(1,mr));
        s.addEvent(e3);
        e3= new SimEvent (SimEvent.EVENT_END_LINK,250*(2*mr+1)+1,
            new Pair<Integer,Integer>(1,mr));
        s.addEvent(e3);
        e3= new SimEvent (SimEvent.EVENT_START_LINK,250*(2*mr+1)+2,
            new Pair<Integer,Integer>(1,mr));
        s.addEvent(e3);
        e3= new SimEvent (SimEvent.EVENT_END_LINK,timeToEnd_ -1,
            new Pair<Integer,Integer>(1,mr));
        s.addEvent(e3);*/
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
