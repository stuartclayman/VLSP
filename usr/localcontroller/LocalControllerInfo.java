package usr.localcontroller;

import usr.common.LocalHostInfo;
import java.util.List;
import java.util.ArrayList;

/**
 * LocalHostInfo contains basic info about one host in the system
 * It deals with finding out IP addresses, ports and so on
 */
public class LocalControllerInfo extends LocalHostInfo {
    private int maxRouters_ = 100;
    private int currRouters_ = 0;
    private List<Integer> routerList = new ArrayList<Integer>();
    private String remoteLoginUser_ = null;
    private String remoteStartController_ = null;
    private LocalControllerActiveStatus activeStatus_ = LocalControllerActiveStatus.ONLINE; // Set default status as ONLINE

    public enum LocalControllerActiveStatus {
        ONLINE,                                      // A LocalController is ONLINE and usable
        OFFLINE                                      // A LocalController is OFFLINE and not usable
    };



    public LocalControllerInfo(String hostName, int port) throws java.net.UnknownHostException {
        super(hostName, port);
    }

    public LocalControllerInfo(java.net.InetAddress ip, int port)  throws java.net.UnknownHostException {
        super(ip, port);
    }

    public LocalControllerInfo(int port)  throws java.net.UnknownHostException {
        super(port);
    }

    public int getMaxRouters() {
        return maxRouters_;
    }

    public int getNoRouters() {
        return currRouters_;
    }

    public List<Integer> getRouters() {
        return routerList;
    }

    public void addRouter(int id) {
        currRouters_++;
        routerList.add(id);
    }

    public void delRouter(int id) {
        routerList.remove((Integer)id);
        currRouters_--;
    }

    public double getUsage() {
        double usage = (double)currRouters_/maxRouters_;
        //   System.err.println("Port "+getPort()+ " R "+currRouters_+" max "+maxRouters_);
        return usage;
    }

    public void setMaxRouters(int maxR) {
        maxRouters_ = maxR;
    }

    public void setRemoteLoginUser(String u) {
        remoteLoginUser_ = u;
    }

    public String getRemoteLoginUser() {
        return remoteLoginUser_;
    }

    public void setRemoteStartController(String s) {
        remoteStartController_ = s;
    }

    public String getRemoteStartController() {
        return remoteStartController_;
    }

    public void setActiveStatus(LocalControllerActiveStatus status) {
        activeStatus_ = status;
    }

    public LocalControllerActiveStatus getActiveStatus() {
        return activeStatus_;
    }
}
