package usr.common;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Channels;

import usr.common.Lifecycle;

public class StdinHandler implements Runnable {
    Lifecycle controller;
    Thread thread;
    ByteBuffer buffer = ByteBuffer.allocate (80);
    ReadableByteChannel channel;
    boolean running;
    
    public StdinHandler(Lifecycle lf) {
        controller = lf;

        FileInputStream fis = new FileInputStream(FileDescriptor.in);
        channel = fis.getChannel();

        //channel2 = Channels.newChannel(System.in);

        // Execute the listener
        thread = new Thread(this);
        thread.start();

    }

    @Override
    public void run() {
        running = true;
        //System.out.println("StdinHandler listening");

        try {

            String line;
            
            int count;

            while ((count = channel.read (buffer)) >= 0) {
                if (count ==0) {
                    continue;
                } else {
                    // process input
                    buffer.flip();

                    System.out.print ("stdin>> " + count + ": ");

                    while (buffer.hasRemaining()) {
                        System.out.print ((char) buffer.get());
                    }

                    buffer.clear();

                }
            }

            // EOF on stdin
            System.out.println("stdin>> " + "EOF");


            running = false;

            // so get Controller to stop
            controller.stop();

            channel.close();
        
        } catch (IOException ioe) {
            //ioe.printStackTrace();
        }

    }

    public void stop() {

        if (running) {
            //System.out.println("StdinHandler stop");
            
            try {
                thread.interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
