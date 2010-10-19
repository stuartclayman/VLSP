package usr.globalcontroller;

import usr.logging.*;

public class SimEvent {
    private int eventType_;
    private Object eventData_;
    private long eventTime_;
    
    public static final int EVENT_END_SIMULATION= 1;
    public static final int EVENT_START_SIMULATION= 2;
    public static final int EVENT_START_ROUTER= 3;
    public static final int EVENT_END_ROUTER= 4;
    public static final int EVENT_START_LINK= 5;
    public static final int EVENT_END_LINK= 6;
    public static final int EVENT_AP_CONTROLLER= 7;
    public static final int EVENT_OUTPUT= 8;
    public static final int EVENT_ON_ROUTER = 9;
    
    
    /** Create event -- note that time is time since start of 
       event schedule  */
    public SimEvent(int type, long time, Object data) 
    {
        eventType_= type;
        eventTime_= time;
        eventData_= data;
    }

    public long getTime() {
        return eventTime_;
    }
    
    public int getType() {
        return eventType_;
    }
    
    public Object getData() {
        return eventData_;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(eventTime_);
        builder.append(" ");

        switch (eventType_) {
        case EVENT_END_SIMULATION: 
            builder.append("EVENT_END_SIMULATION");
            break;
        case EVENT_START_SIMULATION:
            builder.append("EVENT_START_SIMULATION");
            break;
        case EVENT_START_ROUTER:
            builder.append("EVENT_START_ROUTER");
            break;
        case EVENT_END_ROUTER: 
            builder.append("EVENT_END_ROUTER");
            break;
        case EVENT_START_LINK:
            builder.append("EVENT_START_LINK");
            break;
        case EVENT_END_LINK: 
            builder.append("EVENT_END_LINK");
            break;    
        case EVENT_AP_CONTROLLER: 
            builder.append("EVENT_AP_CONTROLLER");
            break;
        case EVENT_OUTPUT: 
            builder.append("EVENT_OUTPUT");
            break;
        case EVENT_ON_ROUTER: 
            builder.append("EVENT_ON_ROUTER");
            break;
        }   
        if (eventData_ != null) {
            builder.append(" ");
            if (eventType_ == EVENT_ON_ROUTER) {
                String[] cmdArgs = (String[])eventData_;
                builder.append(java.util.Arrays.asList(cmdArgs));
            } else {
                builder.append(eventData_);
            }
        }

        return builder.toString();
    }
}
