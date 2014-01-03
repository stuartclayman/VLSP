/**
 * Title:        Client.java<p>
 * Description:  <p>
 * Copyright:    (c) Roses B.V. 2003<p>
 * Company:      Roses B.V.<p>
 *
 * @author      Kinshuk
 * Created:      1-jul-2003
 */
package plugins_usr.tftp.com.globalros.tftp.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;

import plugins_usr.tftp.com.globalros.tftp.common.ACK;
import plugins_usr.tftp.com.globalros.tftp.common.DATA;
import plugins_usr.tftp.com.globalros.tftp.common.ERROR;
import plugins_usr.tftp.com.globalros.tftp.common.FRQ;
import plugins_usr.tftp.com.globalros.tftp.common.OACK;
import plugins_usr.tftp.com.globalros.tftp.common.RRQ;
import plugins_usr.tftp.com.globalros.tftp.common.TFTPPacket;
import plugins_usr.tftp.com.globalros.tftp.common.TFTPSocket;
import plugins_usr.tftp.com.globalros.tftp.common.TFTPUtils;
import plugins_usr.tftp.com.globalros.tftp.common.WRQ;
import usr.applications.Application;
import usr.applications.ApplicationResponse;
import usr.logging.Logger;
import usr.logging.USR;
import usr.net.Address;
import usr.net.AddressFactory;

/**
 * class:   Client
 * package: com.globalros.tftp.client
 * project: tftp4java
 */
public class TFTPClient implements Application {

    /**
     * logger
     */
    private static Logger log = Logger.getLogger("log");
   
    /**
     * port for socket communication between client and server.
     */
    public static final int DEFAULT_PORT = 1069;

    /**
     * default hostname to be used if the argument is not provided.
     */
    public static final String DEFAULT_HOSTNAME = "localhost";

    /**
     * file that client wants downloaded.
     */
    public static final String TEST_CLIENT_DOWNLOAD_FILENAME = "/etc/passwd";

    /**
     * file that client has downloaded - this is the output of the client
     */
    public static final String TEST_CLIENT_OUTPUT_FILENAME = "/tmp/TFTPClient.out";


    /**
     * file that client wants to upload to server
     */
    public static final String TEST_CLIENT_UPLOAD_FILENAME = "/etc/passwd";

    /**
     * file that server wants uploading from client - 
     */
    public static final String TEST_SERVER_UPLOAD_FILENAME = "/tmp/server-upload.out";

    /**
     * max size for the package. 
     */
    private static final int MAX_PACKAGE_SIZE = 512;

    /**
     * the name of the host
     */
    private String hostName = null;

    /**
     * options.
     */
    private int timeout;
    private int tsize;
   
    /**
     * Client constructor    
     */
    public TFTPClient(String hostName)
    {
        this.setHostName(hostName);
    }
      
    public TFTPClient() {
    }

    /**
     * This main method is kept here for historic reasons.
     * It can be used to run standalone.    
     */
    public static void main(String[] args)
        throws IOException, InstantiationException
    {

        String hostName = null;

        if (args.length != 1)
            {
                // use default hostname
                hostName = DEFAULT_HOSTNAME;
            } else
            hostName = args[0];

        TFTPClient client = new TFTPClient(hostName);

        client.init(args);

        client.start();

        client.run();

        client.stop();
    }

    /**
     * Initialize with some args
     */
    public ApplicationResponse init(String[] args) {
        if (args.length != 1)
            {
                // use default hostname
                hostName = DEFAULT_HOSTNAME;
            } else
            hostName = args[0];


        this.setHostName(hostName);

        return new ApplicationResponse(true, "");
    }

    /**
     * Start an application.
     * This is called before run().
     */
    public ApplicationResponse start() {
        return new ApplicationResponse(true, "");
    }


    public void run() {
        //log.logln(USR.STDOUT, "TFTPClient top of run()");

        test();

        //log.logln(USR.STDOUT, "TFTPClient bottom of run()");

    }

    /**
     * Stop an application.
     * This is called to implement graceful shut down
     * and cause run() to end.
     */
    public ApplicationResponse stop() {
      
        log.logln(USR.STDOUT, "Client application stopping....");
        return new ApplicationResponse(true, "");
    }



    public RRQ initialiseDownload(String filename) throws InstantiationException, UnknownHostException
    {
        return this.initialiseDownload(filename, 0, 0);
    }
   
    /**
     * initialiseDownload
     * @param fileName
     * @param optionTimeout
     * @param optionTransferSize
     * @return
     * @throws InstantiationException
     * @throws UnknownHostException
     */
    public RRQ initialiseDownload(String fileName, int optionTimeout, int optionTransferSize) throws InstantiationException, UnknownHostException
    {
        log.logln(USR.STDOUT, "TFTPClient initialiseDownload: " + fileName + " " + optionTimeout + " " + optionTransferSize);

        // The following code sets up a read request to the server, providing the server 
        // with the file name that it wants the data for.
        RRQ rrq = new RRQ();
        rrq.setFileName(fileName);
        if(optionTimeout != 0)
            rrq.setTimeout(optionTimeout);
        if(optionTransferSize != 0)
            rrq.setTransferSize(optionTransferSize);
            
        rrq.setMode(FRQ.OCTET_MODE);
        rrq.setAddress(AddressFactory.newAddress(getHostName()));
        rrq.setPort(DEFAULT_PORT);
   
        return rrq;
    }
   
   
    public WRQ initialiseUpload(String filename) throws InstantiationException, UnknownHostException
    {
        // The following code sets up a write request to the server.     
        return this.initialiseUpload(filename, 0, 0);
    }

    public WRQ initialiseUpload(String fileName, int optionTimeout, int optionTransferSize) throws InstantiationException, UnknownHostException
    {
        log.logln(USR.STDOUT, "TFTPClient initialiseUpload: " + fileName + " " + optionTimeout + " " + optionTransferSize);

        // The following code sets up a write request to the server.     
        WRQ wrq = new WRQ();
        wrq.setFileName(fileName);
        if(optionTimeout != 0)
            wrq.setTimeout(optionTimeout);
        if(optionTransferSize != 0)
            wrq.setTransferSize(optionTransferSize);

        wrq.setMode(FRQ.OCTET_MODE);
        wrq.setAddress(AddressFactory.newAddress(getHostName()));
        wrq.setPort(DEFAULT_PORT);
      
        return wrq;
    }
   
    /**
     * Method that is responsible for the mechanism by which files are sent to the server from client.
     * 
     * @param rrq The Read Request object
     * @param os 
     * @return boolean
     * @throws SocketException
     * @throws InstantiationException
     * @throws IOException
     */
    public boolean download(RRQ rrq, OutputStream os) throws SocketException, InstantiationException, IOException
    {      
        // create a TFTP Socket     
        TFTPSocket tftpSock = new TFTPSocket(5);
      
        int sequenceNumber = 1;
      
        byte[] dummyByteArray = new byte[1]; // used in constructor for DATA....doesnt do anything      
        DATA receive = new DATA(sequenceNumber, dummyByteArray);       
        ACK surprisePacket = this.sendRequest(tftpSock, rrq, receive);
    
        if(surprisePacket == null) 
            {
                // nothing returned from server...should throw an exception at this point.
                log.logln(USR.STDOUT, "Nothing returned from the server after the initial read request.");         
                return false;
            }
      
        if (surprisePacket instanceof OACK)
            {
                // we received some extra's
                OACK oack = (OACK) surprisePacket;
                tsize = oack.getTransferSize();
                timeout = oack.getTimeout();
         
                ACK ack = new ACK(0);
                ack.setPort(surprisePacket.getPort());
                ack.setAddress(surprisePacket.getAddress());
         
                receive = (DATA) TFTPUtils.dataTransfer(tftpSock, ack, receive);
                if(receive == null) 
                    {
                        // nothing returned from server...on acking the oack.
                        log.logln(USR.STDOUT, "Nothing returned from the server after ack on oack.");         
                        return false;
                    }         
            }
        else if (surprisePacket instanceof DATA)
            {
                // too bad, now we need to work
                receive = (DATA) surprisePacket;
            }
      
        // write this data to the output stream....                 
        os.write(receive.getData());
      
        // need to find the port and address the server has chosen to communicate on and connect to it. 
        int serverPort = receive.getPort();
        log.logln(USR.STDOUT, "The server has chosen the following port as the communication port: "+serverPort+" "+new Date().getTime());      
        Address serverAddress = rrq.getAddress();      
        tftpSock.connect(serverAddress, serverPort);
      
        ACK ack = null;
        while (receive.getData().length >= MAX_PACKAGE_SIZE){      
            ack = new ACK(sequenceNumber++);
            receive = new DATA(sequenceNumber, dummyByteArray);
            log.logln(USR.STDOUT, "receiving block"+sequenceNumber+" "+new Date().getTime());
         
            receive = (DATA)TFTPUtils.dataTransfer(tftpSock, ack, receive);
         
            if(receive == null) 
                {
                    // nothing returned from server....
                    log.logln(USR.STDOUT, "Nothing returned from the server after the transfer.");
                    return false;
                }
            os.write(receive.getData());
        }
        // now that the last packet in the file has been sent, the client must sent an 
        // acknowledgement to confirm it has received the last package...or else the server
        // tries to resend..and resend....etc      
        log.logln(USR.STDOUT, "send ack to say that we have received last message."+" "+new Date().getTime());      
        ack = new ACK(sequenceNumber);

        // do not wait for an ACK this time
        ACK result = TFTPUtils.dataTransfer(tftpSock, ack, null, false);

        receive = (DATA) result;
        // ensure that the stream is closed.

        os.close();                  
                                         
        return true;
    }
   
    /**
     * Method that is responsible for the mechanism by which files are sent to the client from the server.
     * 
     * Upload - Sending from the client to the server.....
     * 1. send a WRQ to the server.
     * 2. receive an ACK with 0 for block number from the server
     * --- 3. need to check that it isnt an ERROR code and is definitely an ACK! *** this is done in the sendRequest() method
     * 4. read the first package from the file and transfer to server
     * 5. receive an ACK from the server with the block number that has been sent.
     * 6. check the block number, if this does not correspond with the block sent, then resend.
     * 
     * @param wrq The Write Request object
     * @param is 
     * @return boolean
     * @throws SocketException
     * @throws InstantiationException
     * @throws IOException
     */
    public boolean upload(WRQ wrq, InputStream is) throws SocketException, InstantiationException, IOException
    {               
        log.logln(USR.STDOUT, "[M] : upload: "+wrq.getFileName());  
        // create a TFTP Socket
        TFTPSocket tftpSock = new TFTPSocket(5);
     
        int sequenceNumber = 0;
   
        ACK receive = new ACK(0);
        receive = (ACK)this.sendRequest(tftpSock, wrq, receive);
                   
        // the server should send an ACK (where block number=0, need to check...)
        if(receive == null) 
            {
                // nothing returned from server...should throw an exception at this point.
                log.logln(USR.ERROR, "Nothing returned from the server after the initial send.");         
                return false;
            }
        if(receive.getBlockNr() != 0){
            // nothing returned from server...should throw an exception at this point.
            log.logln(USR.ERROR, "The server has sent an ACK with wrong block number.");         
            return false;
        }
      
        if (receive instanceof OACK)
            {
                // we received some extra's
                OACK oack = (OACK) receive;
                tsize = oack.getTransferSize();
                timeout = oack.getTimeout();
            }      
            
        // need to find the port and address the server has chosen to communicate on and connect to it. 
        int serverPort = receive.getPort();
        log.logln(USR.STDOUT, "The server has chosen the following port as the communication port: "+serverPort);      
        Address serverAddress = wrq.getAddress();      
        tftpSock.connect(serverAddress, serverPort);
   
        // need to read the file and send to server....
        byte[] sendBytes = new byte[MAX_PACKAGE_SIZE];
        DATA send = new DATA();
        int returnValue = 0;
              
        while ((returnValue = is.read(sendBytes)) != -1)
            {
                log.logln(USR.STDOUT, "sending packet number: "+sequenceNumber);
         
                // need to convert the byte array into correct TFTP format for the DATA obejct
                send = new DATA(++sequenceNumber, sendBytes, 0, returnValue);
                receive = new ACK(sequenceNumber);
         
                // now send to server, which in turn sends an acknowledgement  
                receive = (ACK)TFTPUtils.dataTransfer(tftpSock, send, receive);             
                 
            }                        
        //must remember to close the inputstream!
        is.close();
                                                                    
        return true;
    }
       
    /**
     * This method is responsible for the initial communication between the client and the server    .
     * 
     * @param tftpSock
     * @param frq
     * @param recv
     * @return
     */
    public ACK sendRequest(TFTPSocket tftpSock, FRQ frq, ACK recv)
    {      
      
        int retransmits = 0;
        int spamcount = 0;

        /** 
         * Boolean flag that is used internally to indicate we might have stuffed
         * the connection to the client with resent packages. This flag is cleared
         * when we duplicate the timeout with the next  data package sent which 
         * should help to clean up the channel that did get stuffed
         */
        boolean stuffedLink = false;

        // send the packet....
        try
            {
                tftpSock.write(frq);
                // if stuffed use duplicate timeout
                if (stuffedLink)
                    tftpSock.setSocketTimeOut(tftpSock.getSocketTimeOut() * 2);
            } catch (Exception e)
            {
                log.logln(USR.STDOUT, TFTPUtils.getClient(tftpSock) + " UDP send packet failure1.");
                log.logln(USR.STDOUT, e.toString());
            }

        TFTPPacket tftpP = null;
        boolean receiving = true;
      
        // wait for successful acknowledgement!
        while (receiving)
            {
                try
                    {
                        tftpP = tftpSock.read();
            
                        // set timeout back because we gave enough time to clean up stuffed channel
                        if (stuffedLink)
                            {
                                tftpSock.setSocketTimeOut(tftpSock.getSocketTimeOut());
                                stuffedLink = false;
                            }
                    } catch (IOException ios)
                    {
                        log.logln(USR.ERROR, "IOException: " + ios.getMessage());
                        return null;
                    } 

                // case we did not receive any packet
                if (tftpP == null)
                    {
                        if (retransmits++ > 5)
                            {
                                // Too many retries, give up.
                                TFTPUtils.sendErrPacket(
                                                        tftpSock,
                                                        ERROR.ERR_NOT_DEFINED,
                                                        "Retransmit limit exceeded");
                                log.logln(USR.STDOUT, 
                                          TFTPUtils.getClient(tftpSock) + " Maximum retransmit count exceeded");
                                return null;
                            } else
                            {
                                // resend the packet and wait again!
                                log.logln(USR.STDOUT, 
                                          TFTPUtils.getClient(tftpSock)
                                          + " expected packet before time out, sending ACK/DATA again");
                                try
                                    {
                                        tftpSock.write(frq);
                                        // set the flag to indicate that we might be stuffing the pipe for the
                                        // client, next packet needs to have longer timeout to give client some
                                        // time to clean up the pipe
                                        stuffedLink = true;
                                    } catch (Exception e)
                                    {
                                        log.logln(USR.STDOUT, 
                                                  TFTPUtils.getClient(tftpSock) + " UDP send packet failure2.");
                                        log.logln(USR.STDOUT, e.toString());
                                    }
                                continue;
                            }
                    }

                // case we received error
                if (tftpP instanceof ERROR)
                    {
                        log.logln(USR.STDOUT, 
                                  TFTPUtils.getClient(tftpSock) + " " + ((ERROR) tftpP).getErrorMessage());
                        return null;
                    }

                //case we did recieve option acknowledgement while we expect data(1)
                if ((tftpP instanceof OACK) && 
                    (recv instanceof DATA) && 
                    (recv.getBlockNr() == 1)) 
                    return (ACK) tftpP;

                //case we did recieve option acknowledgement while we expect ack(0)
                if ((tftpP instanceof OACK) && 
                    (recv instanceof ACK) && 
                    (recv.getBlockNr() == 0)) 
                    return (ACK) tftpP;
         
                // case we did receive expected
                if ((tftpP instanceof ACK) && TFTPUtils.correctAnswer(recv, (ACK) tftpP))
                    {
                        return (ACK) tftpP;
                    }

                // all other is spam and when too many of this crap is give up, and do not signal
                if (spamcount++ > 5)
                    {
                        return null;
                    }
            }
        return null;
    }

    public int getOptionTimeout()
    {
        return timeout;
    }

    public int getOptionTransferSize()
    {
        return tsize;
    }

    private void setHostName(String hostName)
    {
        this.hostName = hostName;
    }

    private String getHostName()
    {
        return this.hostName;
    }

    private Address getAddress() throws UnknownHostException
    {
        return AddressFactory.getAddress();
    }



    private void test() {
        try {
            // invoke the download mechanism. This involves the device making a Read 
            // Request to the server.
            RRQ rrq = initialiseDownload(TEST_CLIENT_DOWNLOAD_FILENAME);
      
            // create an output stream to write the data to.
            FileOutputStream readFos = new FileOutputStream(new File(TEST_CLIENT_OUTPUT_FILENAME));  
            System.out.println ("Starting downloading:"+new Date().getTime());
            download(rrq,readFos);
            System.out.println ("Finished downloading:"+new Date().getTime());

            // invoke the upload mechanism. This requires the device to make a Write 
            // Request to the server.
            WRQ wrq = initialiseUpload(TEST_SERVER_UPLOAD_FILENAME);
      
            // create an output stream to write the data to.
            // need to send the data to the server...      
            FileInputStream writeFis = new FileInputStream(new File(TEST_CLIENT_UPLOAD_FILENAME));
            System.out.println ("Starting uploading:"+new Date().getTime());

            upload(wrq, writeFis);
            System.out.println ("Finished uploading:"+new Date().getTime());

        } catch (Exception e) {
            System.err.println("TFTPClient: Exception " + e);
            e.printStackTrace();
        }
    }
}
