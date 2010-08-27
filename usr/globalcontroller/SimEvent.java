package usr.globalcontroller;

class SimEvent {
    private int eventType_;
    private Object eventData_;
    private long eventTime_;
    
    public static final int EVENT_END_SIMULATION= 1;
    public static final int EVENT_START_SIMULATION= 2;
    public static final int EVENT_START_ROUTER= 3;
    public static final int EVENT_END_ROUTER= 4;
    public static final int EVENT_START_LINK= 5;
    public static final int EVENT_END_LINK= 6;
    
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
}
