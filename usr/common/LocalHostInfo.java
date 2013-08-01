package usr.common;

import java.net.InetAddress;


/**
 * LocalHostInfo contains basic info about one host in the system
 * It deals with finding out IP addresses, ports and so on
 */
public class LocalHostInfo {
    private String hostName_;                 // Name of host -- should be resolvable
    private int port_;                        // Port host listens on
    private java.net.InetAddress ipAddress_;  // IP address of host
    private int lowPort_ = 0;                 // Lowest number port available (if localcontroller)
    private int highPort_ = 0;                // Highest number port available (if localcontroller)

    /** initMyInfo -- assumes that the current local host is the
     * host in question and finds out the correct info for host name
     * and ip address
     */
    public LocalHostInfo (int port) throws java.net.UnknownHostException {

        port_ = port;
        ipAddress_ = java.net.InetAddress.getLocalHost();

        hostName_ = ipAddress_.getHostName();
//      Logger.getLogger("log").logln(USR.STDOUT, hostName_+" "+ipAddress_.getHostAddress());
    }

    public LocalHostInfo (String hostPort) throws java.net.UnknownHostException
    // Construct LocalHostInfo from host and port
    {
        int port = 0;
        String [] args = hostPort.split(":");

        if (args.length != 2) {
            throw new java.net.UnknownHostException("LocalHostInfo constructor expects string host:port");
        }
        try {
            port = Integer.parseInt(args[1]);
        } catch (java.lang.NumberFormatException e) {
            throw new java.net.UnknownHostException("LocalHostInfo constructor expects string host:port "+e.getMessage());

        }

        init(args[0], port);
    }

    public LocalHostInfo (java.net.InetAddress ip, int port) {

        port_ = port;
        ipAddress_ = ip;
        hostName_ = ipAddress_.getHostName();
//      Logger.getLogger("log").logln(USR.STDOUT, hostName_+" "+ipAddress_.getHostAddress());
    }

    public LocalHostInfo (String hostName, int port) throws java.net.UnknownHostException {

        init(hostName, port);
//      Logger.getLogger("log").logln(USR.STDOUT, hostName_+" "+ipAddress_.getHostAddress());
    }

    public void init(String hostName, int port) throws java.net.UnknownHostException {
        port_ = port;

        hostName_ = hostName;

        ipAddress_ = InetAddress.getByName(hostName);

    }

    /** Accessor function for port_
     */
    public int getPort() {
        return port_;
    }

    /**Accessor function for IP address
     */
    public java.net.InetAddress getIp() {
        return ipAddress_;
    }

    /**Accessor function for name
     */
    public String getName() {
        return hostName_;
    }

    public int getLowPort() {
        return lowPort_;
    }

    public int getHighPort() {
        return highPort_;
    }

    public void setLowPort(int low) {
        lowPort_ = low;
    }

    public void setHighPort(int high) {
        highPort_ = high;
    }

    public String toString() {
        return hostName_ + ":" + port_;
    }

    /**
     * Check if this is equal to another LocalHostInfo
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LocalHostInfo) {
            LocalHostInfo other = (LocalHostInfo)obj;

            if (other.ipAddress_ == this.ipAddress_ &&
                other.port_ == this.port_) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

}
