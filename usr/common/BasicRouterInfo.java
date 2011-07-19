package usr.common;

import usr.localcontroller.LocalControllerInfo;
import usr.logging.*;

/**
   Class provides simple information that the global and local
   controllers need about a router
 */
public class BasicRouterInfo {
    private long startTime_;
    private LocalControllerInfo controller_;
    private int managementPort_;
    private int router2routerPort_;
    private int routerId_;
    // the address of the router
    private String address;
    // the name of the router
    private String name;

    public BasicRouterInfo(int id, long time, LocalControllerInfo lc, int port1) {
        this(id,time,lc,port1,port1+1);
    }
    public BasicRouterInfo(int id, long time, LocalControllerInfo lc, int port1,
                           int port2) {
        startTime_= time;
        controller_= lc;
        managementPort_= port1;
        router2routerPort_= port2;
        routerId_ = id;
    }

    public int getId() {
        return routerId_;
    }

    public int getManagementPort() {
        return managementPort_;
    }

    public int getRoutingPort() {
        return router2routerPort_;
    }

    public LocalControllerInfo getLocalControllerInfo() {
        return controller_;
    }

    public String getHost() {
        return controller_.getName();
    }

    /**
     * Get the address of the router.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Set the address of the router.
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Get the name of the router.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the router.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Check if this is equal to another BasicRouterInfo
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BasicRouterInfo) {
            BasicRouterInfo other = (BasicRouterInfo)obj;

            if (other.routerId_ == this.routerId_ &&
                other.managementPort_ == this.managementPort_ &&
                other.router2routerPort_ == this.router2routerPort_) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }


    /**
     * To string
     */
    public String toString() {
        return getHost() + ":" + getManagementPort() + "@" + getId();
    }
}
