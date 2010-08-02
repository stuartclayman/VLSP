package usr.controllers;

import java.net.Socket;
import java.io.*;

class PayLoad {
  public static final int ALIVE_MESSAGE = 1;
  public static final int SHUTDOWN_MESSAGE = 2;
  private Object data_= null;
  private int messageType_= 0;
  private byte[] raw_= null;
  private int messageLen_= 0;
  
  /** Send data gram to given local host 
   */
  public void sendPayLoad(Socket sock) {
      try {
          OutputStream os= sock.getOutputStream();
          os.write(messageType_);
          os.write(messageLen_);
          if (messageLen_ > 0) {
              os.write(raw_);
          }
      }
      catch (java.io.IOException e) {
          String name= sock.getInetAddress().getHostName();
          int port= sock.getPort();
          System.err.println ("Cannot send data gram to "+
            name+":"+String.valueOf(port));
          System.err.println ("Error: "+e.getMessage());
          System.exit(-1);
      }
  }
  /** Read data gram from socket 
   *
   */
  public PayLoad(BufferedInputStream reader) throws java.io.IOException {
      try {

          messageType_= reader.read();
          messageLen_= reader.read();
          if (messageType_ < 0 || messageLen_ < 0) {
              throw new java.io.IOException("Read corrupt datagram");
          }
          int lenRead;
          if (messageLen_ > 0) {
            raw_= new byte[messageLen_];
            lenRead= reader.read(raw_,0,messageLen_);
          } else {
            raw_= null;
            lenRead= 0;
          }
//          System.out.println("Read "+String.valueOf(lenRead)+" bytes");
          if (lenRead != messageLen_) {
              throw new java.io.IOException("Message has incorrect length."
                + " Read :"+String.valueOf(lenRead)+" expected "+
                String.valueOf(messageLen_));
          }
          decodeMessage();
      } 
      catch (java.io.IOException e) {
          throw (e);
      }
  }
  
  /** Create objects from byte array and message type in data gram 
   */
   
  public void decodeMessage() throws java.io.IOException {
      if (messageType_ == SHUTDOWN_MESSAGE) {
        if (messageLen_ != 0) {
           throw new java.io.IOException("Expected zero length message "+
             "with message type SHUTDOWN_MESSAGE");
        }
        data_= null;
        return;
      }
      ByteArrayInputStream bis= new ByteArrayInputStream(raw_);
      ObjectInputStream in = new ObjectInputStream(bis);
      try {
          data_= in.readObject();
          in.close();
      }
      catch (java.lang.ClassNotFoundException e) {
          System.err.println("Decoded corrupted object from datagram");
          System.exit(-1);
      }
      String className= data_.getClass().getSimpleName();
      if (messageType_ == ALIVE_MESSAGE) {
          if (!(data_ instanceof LocalHostInfo)) {
             System.err.println("\""+className+"\"");
             System.err.println("Decoded corrupted object from ALIVE datagram");
             System.exit(-1);
          }
          return;
      }
      
      throw new java.io.IOException("Unknown message type "+
        String.valueOf(messageType_));
  }
  
  /** Create data gram from message type and object -- create byte
   *array and length 
   */
  public PayLoad(int messType, Object o) {
      messageType_= messType;
      data_= o;
      if (o == null) {
           messageLen_= 0;
           raw_= null;
           return;
      }
      try { // Serialize to a file ObjectOutput 
          ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
          ObjectOutputStream out = new ObjectOutputStream(bos); 
          out.writeObject(o); 
          out.close();  
          raw_= bos.toByteArray();
          messageLen_ = raw_.length;
          //System.out.println("Datagram type "+String.valueOf(messageType_)+
         //    " length "+String.valueOf(messageLen_));
      } catch (java.io.IOException e) {
          System.err.println("Unable to serialize Datagram "+
            e.getMessage());
      } 
 
  }
  
  /** Accessor function for message type 
   */
  public int getMessageType() {
    return messageType_;
  }
  
  /** Accessor function for object 
   */
  public Object getObject() {
    return data_;
  }
   
}

