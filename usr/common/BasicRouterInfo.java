package usr.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import usr.localcontroller.LocalControllerInfo;

/**
 * Class provides simple information that the global and local
 * controllers need about a router
 */
public class BasicRouterInfo {
    private long time_;
    private LocalControllerInfo controller_;
    private int managementPort_;
    private int router2routerPort_;
    private int routerId_;
    // the address of the router
    private String address;
    // the name of the router
    private String name;

    // A map of ID to Name for apps
    private HashMap<Integer, String> appIDs;

    // the Map of applications running on this router
    // It holds data like
    // 00:14:52 AID | StartTime | State | ClassName | Args | Name |
    // 00:14:52 1 | 1331119233159 | RUNNING | usr.applications.Send | [4, 3000, 250000, -d, 250, -i, 10] |
    // /R1/App/usr.applications.Send/1 |
    // So /R1/App/usr.applications.Send/1  -> ["id": 46346535, "time" : 00:14:52, "aid" : 1, "startime" : 1331119233159, "state":
    // "RUNNING", "classname" : "usr.applications.Send", "args" : "[4, 3000, 250000, -d, 250, -i, 10]" ]

    private HashMap<String, Map<String, Object> > localApplications;

    /**
     * BasicRouterInfo with router id and time only.
     */
    public BasicRouterInfo(int id, long time) {
        routerId_ = id;
        time_ = time;
        controller_ = null;
        managementPort_ = 0;
        router2routerPort_ = 0;
        appIDs = null;
        localApplications = null;
    }

    /**
     * BasicRouterInfo with router id, start time, the LocalController, and
     * the managementPort
     */
    public BasicRouterInfo(int id, long time, LocalControllerInfo lc, int port1) {
        this(id, time, lc, port1, port1+1);
    }

    /**
     * BasicRouterInfo with router id, start time, the LocalController,
     * the managementPort, and the router-to-router port.
     */
    public BasicRouterInfo(int id, long time, LocalControllerInfo lc, int port1, int port2) {
        routerId_ = id;
        time_ = time;
        controller_ = lc;
        managementPort_ = port1;
        router2routerPort_ = port2;
        appIDs = new HashMap<Integer, String>();
        localApplications = new HashMap<String, Map<String, Object> >();
    }

    /**
     * Get the router time
     */
    public long getTime() {
        return time_;
    }

    /**
     * Get the router id
     */
    public int getId() {
        return routerId_;
    }

    /**
     * Get the router management port
     */
    public int getManagementPort() {
        return managementPort_;
    }

    /**
     * Get the router router-to-router port
     */
    public int getRoutingPort() {
        return router2routerPort_;
    }

    /**
     * Get the LocalController managing the router
     */
    public LocalControllerInfo getLocalControllerInfo() {
        return controller_;
    }

    /**
     * Get the hostname of the LocalController managing the router
     */
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
     * Add an application to the router.
     */
    public void addApplication(Integer id, String name) {
        appIDs.put(id, name);
        localApplications.put(name, null);
    }

    // FIXME: GT - I could not find the globalcontroller command where to invoke application shutdown
    /**
     * Remove an application to the router.
     */
    public void removeApplication(Integer id, String name) {
        appIDs.remove(id);
        localApplications.remove(name);
    }

    /**
     * List all application to the router.
     */
    public Set<String> getApplications() {
        return localApplications.keySet();
    }

    /**
     * List all application IDs to the router.
     */
    public Set<Integer> getApplicationIDs() {
        return appIDs.keySet();
    }

    /**
     * Get App Name given an ID
     */
    public String getAppName(Integer id) {
        return appIDs.get(id);
    }

    /**
     * Get the data held for an application
     * So /R1/App/usr.applications.Send/1  -> ["time" : 00:14:52, "id" : 1, "startime" : 1331119233159, "state": "RUNNING",
     *"classname" : "usr.applications.Send", "args" : "[4, 3000, 250000, -d, 250, -i, 10]" ]
     */
    public Map<String, Object> getApplicationData(String appName) {
        return localApplications.get(appName);
    }

    /**
     * Set the data held for an application
     * So /R1/App/usr.applications.Send/1  -> ["time" : 00:14:52, "id" : 1, "startime" : 1331119233159, "state": "RUNNING",
     *"classname" : "usr.applications.Send", "args" : "[4, 3000, 250000, -d, 250, -i, 10]" ]
     */
    public void setApplicationData(String appName, Map<String, Object> data) {
        localApplications.put(appName, data);

        //System.out.println("BasicRouterInfo: " + name + ". setApplicationData " + appName + " -> " + data);
    }

    /**
     * Check if this is equal to another BasicRouterInfo
     */
    @Override
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
     * hashCode for BasicRouterInfo
     */
    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    /**
     * To string
     */
    @Override
    public String toString() {
        return getHost() + ":" + getManagementPort() + " % " + getId() +
            " -> " + getName() + "/" + getAddress();
    }

}
