/**
 * (c) Melexis Telecom and or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */
package plugins_usr.tftp.com.globalros.tftp;

import usr.net.*;

import plugins_usr.tftp.com.globalros.tftp.server.EventListener;
import plugins_usr.tftp.com.globalros.tftp.server.TFTPServer;
import plugins_usr.tftp.com.globalros.tftp.common.VirtualFileSystem;

import usr.logging.*;

/**
 * @author marco
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class Server implements EventListener {
   /**
    * logger
    */
   private Logger log = Logger.getLogger(Server.class.getName());


   private TFTPServer tftpServer;

	/**
	 * Constructor for Server.
	 */
	public Server()
	{
      VirtualFileSystem vfs = new FileSystem("/tmp");
      tftpServer = new TFTPServer(vfs, this);
      tftpServer.setPoolSize(2);
      tftpServer.setPort(1069);
	}

   public void connect() throws Exception
   {
      if (tftpServer == null) return;
      tftpServer.startUp();
   }

   public void disconnect()
   {
      if (tftpServer == null) return;
      tftpServer.shutDown();
   }
   
   public void onAfterDownload(Address a, int p, String fileName, boolean ok)
   {
            if (ok) log.logln(USR.STDOUT, "Send " + fileName + " sucessfully to client: " + a.toString() + " port: " +p);
            else log.logln(USR.STDOUT, "Send " + fileName + " file not sucessfully to client: " + a.toString() + " port: " +p);     
   }
   
   public void onAfterUpload(Address a, int p, String fileName, boolean ok)
   {
            if (ok) log.logln(USR.STDOUT, "received " + fileName + " sucessfully from client: " + a.toString() + " port: " +p);
            else log.logln(USR.STDOUT, "received " + fileName + " file not sucessfully from client: " + a.toString() + " port: " +p);     
   }
   

   public static void main(String [] args)
   {
      Server server = new Server();

      try
      {
         server.connect();
         System.out.println("Press a button to shutdown the server!");
         System.in.read();
         server.disconnect();
      }
      catch (Exception e)
      {
         System.out.println("Exception occured: " + e);
         e.printStackTrace();
      }
   }  

}
