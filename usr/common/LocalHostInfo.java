/**
  * LocalHostInfo contains basic info about one host in the system
  * It deals with finding out IP addresses, ports and so on
*/

package usr.common;
import java.net.InetAddress;


public class LocalHostInfo  {
    private String hostName_;      // Name of host -- should be resolvable
    private int port_;          // Port host listens on
    private java.net.InetAddress ipAddress_;  // IP address of host
    private int lowPort_= 0;       // Lowest number port available (if localcontroller)
    private int highPort_= 0;      // Highest number port available (if localcontroller)
    
    /** initMyInfo -- assumes that the current local host is the
    host in question and finds out the correct info for host name
    and ip address */
    public LocalHostInfo (int port) {
      
      port_= port;
      try {
        ipAddress_= java.net.InetAddress.getLocalHost();
      } 
      catch (java.net.UnknownHostException e) {
        System.err.println("Cannot find hostname "+
          e.getMessage());
      }
      hostName_= ipAddress_.getHostName();
//      System.out.println(hostName_+" "+ipAddress_.getHostAddress());
    }
    
    
    public LocalHostInfo (String hostPort) 
    // Construct LocalHostInfo from host and port
    {
        int port= 0;
        String []args= hostPort.split(":");
        if (args.length != 2) {
            System.err.println("LocalHostInfo constructor expects string host:port");
            System.exit(-1);
        }
        try {
            port= Integer.parseInt(args[1]);
        }
        catch (java.lang.NumberFormatException e) {
            System.err.println("LocalHostInfo constructor expects string host:port");
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        init(args[0],port);
    }
    
    public LocalHostInfo (java.net.InetAddress ip, int port) {
      
      port_= port;
      ipAddress_= ip;
      hostName_= ipAddress_.getHostName();
//      System.out.println(hostName_+" "+ipAddress_.getHostAddress());
    }
    
    public LocalHostInfo (String hostName, int port) {
      
      init(hostName,port);
//      System.out.println(hostName_+" "+ipAddress_.getHostAddress());
    }
    
    public void init(String hostName, int port) {
      port_= port;
      
      hostName_= hostName;
      try {
         ipAddress_= InetAddress.getByName(hostName);
      } catch (java.net.UnknownHostException e) {
          System.err.println("Cannot resolve host "+hostName);
          System.err.println(e.getMessage());
          System.exit(-1);
      }
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
      lowPort_= low;
    }
    
    public void setHighPort(int high) {
       highPort_= high;
    }
    
    public String toString() {
        return hostName_ + ":" + port_;
    }
}
