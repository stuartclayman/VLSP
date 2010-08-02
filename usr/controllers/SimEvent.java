class SimEvent {
    private int eventType_;
    private Object eventData_;
    private long eventTime_;
    
    public static final int EVENT_END_SIMULATION= 1;
    public static final int EVENT_START_SIMULATION= 2;
    
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
