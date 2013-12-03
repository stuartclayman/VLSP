/**
 * (c) Melexis Telecom and/or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */
package plugins_usr.tftp.com.globalros.tftp.server;

import java.io.IOException;
import java.io.InterruptedIOException;
import usr.net.*;
import java.net.SocketException;

import plugins_usr.tftp.com.globalros.tftp.common.ACK;
import plugins_usr.tftp.com.globalros.tftp.common.DATA;
import plugins_usr.tftp.com.globalros.tftp.common.ERROR;
import plugins_usr.tftp.com.globalros.tftp.common.OACK;
import plugins_usr.tftp.com.globalros.tftp.common.RRQ;
import plugins_usr.tftp.com.globalros.tftp.common.TFTPPacket;
import plugins_usr.tftp.com.globalros.tftp.common.WRQ;

import usr.logging.*;

/**
 * @author  marco
 */

/*
 * This class handles off one TFTP write or read request
 * in future time this could be improved to handle more than one
 * request on same socket.
 * Thus the maximum of 65534 ports could be overcome
 * if capacity on NIC and processor would allow. */
public class TFTPSocket
{
   // timeout in msecs
   private int timeout;
   
   byte[] buffer;

   Address destAddr;
   int destPort;

   static int handlePort;
   DatagramSocket usrSocket;

   static Logger tftpLog = Logger.getLogger("log");
   public static final int BLOCK_SIZE = 512;

   /** Creates a new instance of TFTPSocket */
   public TFTPSocket(int timeout) throws SocketException
   {
      usrSocket = getFreeSocket();
      this.setSocketTimeOut(timeout);

      buffer = new byte[BLOCK_SIZE + 16];
   }

   /**
    * This method is used to set the tftp socket timeout in seconds
    * @param secs Number of seconds a read should block. Use zero to make read blocking
    */
   public void setSocketTimeOut(int secs) throws SocketException
   {
      this.timeout = secs * 1000;
      usrSocket.setSoTimeout(secs * 1000);
   }

   public void setSockTimeoutMSec(int msecs) throws SocketException
   {
      this.timeout = msecs;
      usrSocket.setSoTimeout(msecs);
   }
   
   public int getSocketTimeOut()
   {
      return timeout / 1000;
   }


   /** static method to provide port number to */
   private static DatagramSocket getFreeSocket() throws SocketException
   {
      int loopPort = handlePort - 1;
      while (loopPort != handlePort)
      {
         if ((handlePort < 29001) || (++handlePort > 65000))
         {
            handlePort = 29001;
         }
         try
         {
            DatagramSocket freeSocket = new DatagramSocket(handlePort);
            return freeSocket;
         } catch (SocketException e)
         {
            /* continue to find free port */
            continue;
         }
      }
      /* should already be returned with free Socket! */
      tftpLog.logln(USR.ERROR, "Could not find a free port!");
      throw new SocketException();
   }

   public TFTPPacket read() throws IOException
   {
       //DatagramPacket usrPacket = new DatagramPacket(buffer, BLOCK_SIZE + 16);
       Datagram packet;
      try
      {
         packet = usrSocket.receive();
         
      } catch (SocketTimeoutException e)
      {
         // timeout occured, no packet received!
         return null;
      }

      byte[] usrData = packet.getData();
      int usrLength = packet.getLength();
      tftpLog.logln(USR.STDOUT, "usrPacket.length in receive: " + usrLength);

      // copy tftpdata
      byte[] tftpPB = new byte[packet.getLength()];
      System.arraycopy(usrData, 0, tftpPB, 0, usrLength);

      TFTPPacket tftpP = null;
      try
      {
         int opcode = TFTPPacket.fetchOpCode(tftpPB);
         switch (opcode)
         {
            case ACK.OPCODE :
               tftpP = new ACK(tftpPB);
               break;
            case DATA.OPCODE :
               tftpP = new DATA(tftpPB, usrLength);
               break;
            case RRQ.OPCODE :
               tftpP = new RRQ(tftpPB);
               break;
            case WRQ.OPCODE :
               tftpP = new WRQ(tftpPB);
               break;
            case OACK.OPCODE :
               tftpP = new OACK(tftpPB);
               break;               
            case ERROR.OPCODE :
               tftpP = new ERROR(tftpPB);
               break;            
            default:
                tftpLog.logln(USR.ERROR, "Unknown opcode: "+opcode);
               break;   
               
         }
      } catch (InstantiationException e)
      {
         throw new IOException("Could not discover tftp packet in recieved data!"+e.getMessage());
      }                  
      tftpP.setPort(packet.getDstPort());
      tftpP.setAddress(packet.getDstAddress());
      
      return tftpP;
   }

   public void write(TFTPPacket tftpP) throws IOException
   {           
      byte[] data = tftpP.getBytes();
      Address address = tftpP.getAddress();
      
      int port = tftpP.getPort();      
      
      if(usrSocket.isConnected())
      {
         address = destAddr;
         port = destPort; 
      }
           
      DatagramPacket usrPacket = new DatagramPacket(data, address, port);        
      usrSocket.send(usrPacket);
   }

   public void connect(Address addr, int port)
   {
      /* if (usrSocket.isConnected()) */
      usrSocket.disconnect();
      usrSocket.connect(addr, port);
      destAddr = addr;
      destPort = port;
   }

   public void clear() 
   {
      try
      {
         usrSocket.setSoTimeout(10);
      } catch (SocketException e1)
      {
          tftpLog.logln(USR.ERROR, e1.getMessage());
          e1.printStackTrace();
      }
      byte[] data = new byte[516];
      Datagram usrPacket;
      while (true)
      {
         try
         {
            usrPacket = usrSocket.receive();
         }
         catch (SocketTimeoutException ste)
         {
            //tftpLog.debug("[clear]: "+ste.getMessage());
            try
            {
               usrSocket.setSoTimeout(timeout);
            } catch (SocketException e1)
            {
                tftpLog.logln(USR.ERROR, e1.getMessage());
                e1.printStackTrace();
            }
            return;
         }
         catch (Exception e)
         {
             tftpLog.logln(USR.ERROR, "[clear] :" +e.getMessage());
            try
            {
               usrSocket.setSoTimeout(timeout);
            } catch (SocketException e1)
            {
                tftpLog.logln(USR.ERROR, e1.getMessage());
                e1.printStackTrace();
            }
            return;
         }
      }
      
//      int tempPort = usrSocket.getPort();
//      Address tempAddress = usrSocket.getAddress();
//      boolean connectedAlready = usrSocket.isConnected();
//      
//      try {  
//         usrSocket.disconnect();
//         usrSocket.close();         
//         usrSocket = new DatagramSocket(tempPort);
//         this.setSocketTimeOut(timeout);
//                         
//         if(connectedAlready) 
//         //if(usrSocket.isBound())
//         {
//            // then drop the connection and reconnect...
//            tftpLog.debug("drop the connection and reconnect...");
//            
//            this.connect(tempAddress, tempPort);                                            
//         }         
//      } catch(SocketException se)
//      {
//         tftpLog.debug("Socket is freaking useless...:"+se.getMessage()); 
//      }

   }
   
   public void disconnect()
   {
      usrSocket.disconnect();
   }   

   public int getPort()
   {
      return destPort;
   }
   
   public Address getAddress()
   {
      return destAddr;
   }
   
   /*
       public static String getIP(Address in)
       {
         String ret;
         byte[] buf = in.getAddress();
         StringBuffer rv = new StringBuffer();
         for (int i = 0; i < buf.length; i++) {
     rv.append (buf[i] + ".");
         }
         ret = rv.toString();
         return ret.substring (0, ret.length() - 1);
       }
   */
}
