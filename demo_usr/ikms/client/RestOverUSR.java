/**
 * (c) Melexis Telecom and/or Remote Operating Services B.V.
 * 
 * Distributable under LGPL license
 * See terms of license at gnu.org
 */

/**
 * TFTPServer.java
 * @version 0.1 - March 2002
 * @author Marco Dubbeld
 *
 * This is the tftp server in remote operating services. An instance
 * of this class should be constructed. Then configure the server, and then
 * start/stop.
 */
package demo_usr.ikms.client;

import plugins_usr.tftp.com.globalros.tftp.common.VirtualFileSystem;
import plugins_usr.tftp.com.globalros.tftp.server.EventListener;
import plugins_usr.tftp.com.globalros.tftp.server.TFTPServerSocket;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import demo_usr.ikms.TFTP.RestVirtualFileSystem;

public class RestOverUSR implements EventListener {
    static int DEFAULT_PORT = 69;
    // fields for the attributes
    private int poolSize = 100;
    private int port = DEFAULT_PORT;
    private VirtualFileSystem vfs;

    private Thread server;
    private TFTPServerSocket ss;

    static Logger tftpLog = Logger.getLogger("log");

    private EventListener listener = null;
    /**
     * This constructor needs a VirtualFileSystem to get in or 
     * output streams for a file.
     * @param vfs An implementing virtual file system
     */
    public RestOverUSR(VirtualFileSystem vfs, EventListener listener)
    {
        this.vfs = vfs;
        this.listener = listener;
    }

    public RestOverUSR() {
        tftpLog.logln(USR.ERROR, "TFTPServer: TFTPServer()");
        this.vfs = new RestVirtualFileSystem();
        this.listener = this;
        setPoolSize(100);
        //setPort(1069);        
    }

    /**
     * Initialize with port as an arg
     */
    public void init(int port) {
        setPort (port);
    }

    /**
     * Start the RestOverUSR object.
     * This is called before run().
     */
    public void start() {
        startUp();
    }

    public void run() {
        //tftpLog.logln(USR.ERROR, "TFTPServer: top of run()");

        try {
            tftpLog.logln(USR.ERROR, "TFTPServer thread starting: waiting.....");
            server.join();
        } catch (Exception e) {
        }

        //tftpLog.logln(USR.ERROR, "TFTPServer: bottom of run()");
    }

    /**
     * Stops the RestOverUSR object.
     * This is called to implement graceful shut down
     * and cause run() to end.
     */
    public void stop() {
        shutDown();
    }

    /**
     * Returns the port.
     * @return int
     */
    public int getPort()
    {
        return port;
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
     * Sets the size of the workers pool
     * @param size The size to set
     */
    public void setPoolSize(int poolSize)
    {
        this.poolSize = poolSize;
        if (ss == null)
            return;
        ss.setPoolSize(poolSize);
    }

    /**
     * Returns the size of the workers pool
     * @return int
     */
    public int getPoolSize()
    {
        return poolSize;
    }

    /**
     * This method starts an actual server socket and listens
     */
    public void startUp()
    {
        //tftpLog.logln(USR.ERROR, "TFTPServer: top of startUp");

        //if (tftpLog.isDebugEnabled())
        tftpLog.logln(USR.ERROR, "TFTPServer: Starting new TFTP server socket on port: " + port);
         
        ss = new TFTPServerSocket(port, poolSize, vfs, listener);
        server = new Thread(ss);
        server.start();

        //tftpLog.logln(USR.ERROR, "TFTPServer: bottom of startUp");
    }

    public void shutDown()
    {
        //tftpLog.logln(USR.ERROR, "TFTPServer: top of shutDown");

        if (ss == null)
            {
                tftpLog.logln(USR.ERROR, 
                              "ServerSocket is already null so, is tftpServer closed???");
                return;
            }

        //      if (tftpLog.isDebugEnabled())
        tftpLog.logln(USR.ERROR, "Shutting down TFTP server socket.");
        ss.stop();

        if (server == null)
            {
                tftpLog.logln(USR.ERROR, "FIXME: ServerSocket was not null but tftpServer is!");
                return;
            }

        try
            {
                server.join(6000);
            }
        catch (InterruptedException e)
            {
                tftpLog.logln(USR.ERROR, "Could not close all TFTPServer thread!");
            }
        server = null;
        ss = null;
    }

   public void onAfterDownload(Address a, int p, String fileName, boolean ok)
   {
            if (ok) tftpLog.logln(USR.STDOUT, "Send " + fileName + " sucessfully to client: " + a.toString() + " port: " +p);
            else tftpLog.logln(USR.STDOUT, "Send " + fileName + " file not sucessfully to client: " + a.toString() + " port: " +p);     
   }
   
   public void onAfterUpload(Address a, int p, String fileName, boolean ok)
   {
            if (ok) tftpLog.logln(USR.STDOUT, "received " + fileName + " sucessfully from client: " + a.toString() + " port: " +p);
            else tftpLog.logln(USR.STDOUT, "received " + fileName + " file not sucessfully from client: " + a.toString() + " port: " +p);     
   }
   
}
