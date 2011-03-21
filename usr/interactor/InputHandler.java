// InputHandler.java

package usr.interactor;
import usr.logging.*;

import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.EOFException;

/**
 * A class to handle the input from a ManagementConsole.
 * It will listen for a response from the console, or
 * it will listen for asynchronous events.
 * In either case it will callback to the MCRPInteractor object
 * with the relevant call.
 * The object runs in it;s own thread.
 */
public class InputHandler implements Runnable {
    // The MCRPInteractor object that started this handler
    MCRPInteractor mcrp = null;

    // The reader from the socket
    BufferedReader input = null;

    // My thread
    Thread myThread = null;

    // Is the thread running
    boolean running = false;

    /**
     * Construct an InputHandler
     */
    protected InputHandler(MCRPInteractor mcrp, Reader inp) {
        this.mcrp = mcrp;
	this.input = new BufferedReader(inp);

	myThread = new Thread(this, mcrp.getClass().getName() + "-" + "InputHandler-" + hashCode());
	running = true;
	myThread.start();
    }

    /**
     * The main guts of the thread.
     * It gets an answer from the remote end and processes it
     * in order to determine if if is a normal response or
     * an event.
     */
    public void run() {
	// sit in a loop and grab input
	while (running) {
	    try {
		MCRPResponse response = grabAnswer();

        // System.err.println("MCRPInputHandler: got " + response.getCode());

		// check the response
		if (response == null) {
		    // the socket input is at EOF
		    // no need to carry on
		    running = false;

		    mcrp.gotEOF();
		} else {
		    // got a valid response
		    if (response.isEvent()) {
			mcrp.gotEvent(response);
		    } else {
			mcrp.gotResponse(response);
		    }
		}
	    } catch (Exception e) {
		running = false;
		mcrp.gotException(e);
	    }
	}
    } 


    /**
     * Grab an answer
     * We will see one of the following answers:
     * i) An immedate return value with code and a string
     *    218 OK VALUE SET
     * ii) A list response which has a list of items
     *     225-1  part 1
     *     225-2  part 2
     * iii) A stream response
     *     Lines of text
     *     which end with a dot
     *     .
     *     Then a return code
     * @return a MCRPResponse object, or null at EOF
     */
    private MCRPResponse grabAnswer() throws IOException, MCRPException {
	MCRPResponse response = new MCRPResponse();

        // does the response look like a stream.
        boolean isStreamResponse = false;

        // Have we finished reading the stream
        boolean isStreamFinished = false;

        // The actual stream
        StringBuilder builder = null;

	// check input
	if (input == null) {
	    // there is no input
	    throw new EOFException("MCRPResponse: input unexpected closed");
	}

	// final reply
	boolean finalReply = false;

	// read a line
	String line = null;

	// loop around getting all message id tagged replies
	do {
            // read a line
            line = input.readLine();


	    if (line == null) {
		// the input is at EOF
		return null;
	    }

            // check to see if the line starts with [0-9][0-9][0-9]
            if (line.matches("^[0-9][0-9][0-9].*")) {
                if (isStreamResponse) {
                    // this is just a line of data in the stream
                    //System.err.println("MCRPInputHandler: line of data: " + line);
                } else {
                    // this is the final response code
                    // and is what happens most of the time
                    //System.err.println("MCRPInputHandler: normal response: " + line);
                }
            } else {                
                if (isStreamResponse) {
                    // it does not match [0-9][0-9][0-9]
                    // and we're in a stream response
                    // so it is kust a line of data
                    //System.err.println("MCRPInputHandler: line of data: " + line);
                } else {
                    // it does not match [0-9][0-9][0-9]
                    // therefore it must be the first line of a stream response
                    // so we set up the variables
                    isStreamResponse = true;
                    isStreamFinished = false;
                    builder = new StringBuilder();

                    //System.err.println("MCRPInputHandler: setup StreamResponse: " + line);
                }
            }
                

            // if we are doing stream response
            // then read lines until we hit dot (.)
            if (isStreamResponse && !isStreamFinished) {
                if (line.equals(".")) {
                    isStreamFinished = true;
                   continue;
                 } else {
                    builder.append(line);
                    builder.append("\n");
                    continue;
                }
            }

            

	    // find the first space
	    int spacePos = line.indexOf(' ');

	    if (spacePos == 3) {
                if (isStreamResponse) {
                    // send the stream back

                    // The parts
                    String[] parts  = new String[2];
                    parts[0] = line.substring(0, spacePos);
                    parts[1] = builder.toString();

                    response.add(parts);

                } else {
                    // it's a normal response
                    // e.g. 218 OK VOLUME SET

                    // The parts
                    String[] parts  = new String[2];
                    parts[0] = line.substring(0, spacePos);
                    parts[1] = line.substring(spacePos+1);

                    response.add(parts);
                }

                finalReply = true;

             //   Logger.getLogger("log").logln(USR.ERROR, "MCRPInputHandler: response add " + line);

                break;

	    } else {
		// it's a message id tagged response
		// e.g. 225-4
		int dashPos = line.indexOf('-');

                // The parts
                String[] parts  = new String[2];

		parts[0] = line.substring(0, dashPos);
		parts[1] = line.substring(dashPos+1);


		response.add(parts);

	    }

	} while (!finalReply);


	return response;

     }

}
