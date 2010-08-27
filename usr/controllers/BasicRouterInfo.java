package usr.controllers;

/**
Class provides simple information that the global and local
controllers need about a router
*/
class BasicRouterInfo {
    private int startTime_;
    private LocalControllerInfo controller_;
    private int managementPort_;
    private int routingPort_;
    private int routerId_;
    
    BasicRouterInfo(int id, int time, LocalControllerInfo lc, int port1) {
         this(id,time,lc,port1,port1+1);
    }
    BasicRouterInfo(int id, int time, LocalControllerInfo lc, int port1, 
      int port2) {
        startTime_= time;
        controller_= lc;
        managementPort_= port1;
        routingPort_= port2;
    }
    
    public int getId() {
        return routerId_;
    }
}
