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
    
    
    public LocalHostInfo (java.net.InetAddress ip, int port) {
      
      port_= port;
      ipAddress_= ip;
      hostName_= ipAddress_.getHostName();
//      System.out.println(hostName_+" "+ipAddress_.getHostAddress());
    }
    
    public LocalHostInfo (String hostName, int port) {
      
      port_= port;
      
      hostName_= hostName;
      try {
         ipAddress_= InetAddress.getByName(hostName);
      } catch (java.net.UnknownHostException e) {
          System.err.println("Cannot resolve host "+hostName);
          System.err.println(e.getMessage());
          System.exit(-1);
      }
//      System.out.println(hostName_+" "+ipAddress_.getHostAddress());
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
    
}
