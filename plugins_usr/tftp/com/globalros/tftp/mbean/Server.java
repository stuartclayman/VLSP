/**
 * (c) Melexis Telecom and/or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */
package plugins_usr.tftp.com.globalros.tftp.mbean;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import plugins_usr.tftp.com.globalros.tftp.server.TFTPServer;
import plugins_usr.tftp.com.globalros.tftp.FileSystem;
import plugins_usr.tftp.com.globalros.tftp.common.VirtualFileSystem;

import usr.logging.*;

/**
 * @author marco
 *
 */
public class Server implements MBeanRegistration
{
   // fields for the attributes
   private int size = 10;
   private int port = 1069;
   
//   private Thread tftpServer;
   private TFTPServer tftpServer;

   public Server()
   {
      VirtualFileSystem vfs = new FileSystem("/home/tftp");
      tftpServer = new TFTPServer(vfs, null);
   }

   public String getName()
   {
      return "TFTPServer";
   }

   private void loadProperties() throws FileNotFoundException
   {
      String properties = "META-INF/tftp.properties";
      InputStream is =
         Thread.currentThread().getContextClassLoader().getResourceAsStream(
            properties);
      if (is == null)
      {
         throw new java.io.FileNotFoundException(
            "Cannot find file '" + properties + "'");
      }
      try
      {
         p.load(is);
      }
      catch (Throwable t)
      {
      }
   }

   public ObjectName preRegister(MBeanServer server, ObjectName name)
      throws Exception
   {
      reloadConfig();
      connect();
      return name;
   }

   public void postRegister(Boolean registrationDone)
   {
      if (!registrationDone.booleanValue())
      {
         disconnect();
         return;
      }
   }

   public void reloadConfig()
   {
      try
      {
         loadProperties();
      }
      catch (FileNotFoundException e)
      {
          tftpLog.logln(USR.ERROR, e.toString());
      }
   }

   public void connect() throws Exception
   {
      tftpServer.start();
   }

   public void preDeregister() throws Exception
   {
      disconnect();
   }

   public void postDeregister()
   {
   }

   public void disconnect()
   {
      tftpServer.stop();
   }

   private Properties p = null;
   static Logger tftpLog = Logger.getLogger("log");

   /**
    * Returns the port.
    * @return int
    */
   public int getPort()
   {
      return port;
   }

   /**
    * Returns the size.
    * @return int
    */
   public int getSize()
   {
      return size;
   }

   /**
    * Sets the port.
    * @param port The port to set
    */
   public void setPort(int port)
   {
      this.port = port;
   }

   /**
    * Sets the size.
    * @param size The size to set
    */
   public void setSize(int size)
   {
      this.size = size;
      tftpServer.setPoolSize(size);
   }

}
