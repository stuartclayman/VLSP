package usr.common;

import java.util.Enumeration;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.net.NetworkInterface;
import usr.logging.BitMask;
import usr.logging.Logger;
import usr.logging.USR;


/**
 * A class for dealing with IP addresses.
 * It tries to determine whether IPv4 or IPv6 addresses are being used,
 * and also tries to map localhost to a network visibile address, if possible.
 */

public class IPAddress {

    public static String getLocalHost() {
        String gcAddress = "::1";  // was localhost
        String hostAddr = null;

        try {

            InetAddress localhost = InetAddress.getLocalHost();

            boolean ipv6 = false;

            if (localhost instanceof java.net.Inet6Address) {
                ipv6 = true;
            }
        
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            {
                while (interfaces.hasMoreElements()) {
                    NetworkInterface nic = interfaces.nextElement();

                    System.out.println("check addresses: " + nic);
                    
                    // we shouldn't care about loopback addresses
                    if (nic.isLoopback()) {
                        System.out.println(" LOOPBACK");
                        continue;
                    }

                    // if you don't expect the interface to be up you can skip this
                    // though it would question the usability of the rest of the code
                    if (!nic.isUp()) {
                        System.out.println(" NOT UP");
                        continue;
                    }

                    Enumeration<InetAddress> addresses = nic.getInetAddresses();

                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();

                        System.out.print("check addresses:\t " + address);
                        
                        // look only for ipv4 addresses, if ipv6 is false
                        if (ipv6 && address instanceof java.net.Inet6Address) {
                            System.out.println(" REJECT");
                            continue;
                        } else {
                            hostAddr = "::";
                        }
                            
                        if (!address.isLoopbackAddress()) {
                            String addr = address.getHostAddress();
                            hostAddr = addr;
                            System.out.println(" ACCEPT " + hostAddr);
                        } else {
                            System.out.println(" REJECT");
                        }
                    }

                    System.out.println("");
                }
            }

            System.err.println("ORIG hostAddr = " + hostAddr);

            // make sure we dont get a hard to use address
            if (hostAddr != null && ! hostAddr.startsWith("169.254")) {
                /*
                  try {
                        
                  //gcAddress = InetAddress.getLocalHost().getHostAddress(); 

                  //System.err.println("ALT gcAddress = " + InetAddress.getLocalHost().getHostAddress());

                  // InetAddress.getByName(hostName).getHostAddress();

                  //System.err.println("ALT gcAddress = " + InetAddress.getByName("localhost").getHostAddress());


                  // InetAddress.getByName(System.getenv("HOSTNAME")).getHostAddress();
                  // InetAddress.getLocalHost().getHostAddress();  // getHostName();
                  // or InetAddress.getByName(ip).getHostName()
                  } catch (UnknownHostException uhe) {
                  Logger.getLogger("log").logln(USR.ERROR, "IPAddress " + uhe.getMessage());
                  uhe.printStackTrace();
                  }
                */

                gcAddress = hostAddr;

            } else {
            }
                
        } catch (UnknownHostException uhe) {
            Logger.getLogger("log").logln(USR.ERROR, "IPAddress " + uhe.getMessage());
        } catch (SocketException se) {
            Logger.getLogger("log").logln(USR.ERROR, "IPAddress " + se.getMessage());
            se.printStackTrace();
        }

        System.err.println("Actual gcAddress = " + gcAddress);

        return gcAddress;
    }
}
