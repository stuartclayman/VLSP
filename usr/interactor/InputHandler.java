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
     * @return a MCRPResponse object, or null at EOF
     */
    private MCRPResponse grabAnswer() throws IOException, MCRPException {
	MCRPResponse response = new MCRPResponse();

	// check input
	if (input == null) {
	    // there is no input
	    throw new EOFException("MCRPResponse: input unexpected closed");
	}

	// final reply
	boolean finalReply = false;

	// read a line
	String line = input.readLine();

	// loop around getting all message id tagged replies
	do {

	    if (line == null) {
		// the input is at EOF
		return null;
	    }

	    // find the first space
	    int spacePos = line.indexOf(' ');

	    // The parts
	    String[] parts = null;

	    if (spacePos == 3) {
		// it's a normal response
		// e.g. 218 OK VOLUME SET
		parts = new String[2];
		parts[0] = line.substring(0, spacePos);
		parts[1] = line.substring(spacePos+1);

		response.add(parts);

		finalReply = true;

		//Logger.getLogger("log").logln(USR.ERROR, "SocketInputHandler: response add " + line);

		break;

	    } else {
		// it's a message id tagged response
		// e.g. 225-4
		int dashPos = line.indexOf('-');

		parts = new String[2];

		parts[0] = line.substring(0, dashPos);
		parts[1] = line.substring(dashPos+1);


		response.add(parts);

		// read another line
		line = input.readLine();

	    }

	} while (!finalReply);


	return response;

     }

}
