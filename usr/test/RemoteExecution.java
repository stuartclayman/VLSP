package usr.test;

import usr.events.*;
import usr.engine.*;
import usr.globalcontroller.RemoteEventDelegate;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class RemoteExecution extends RemoteEventDelegate implements EventDelegate {
    /**
     * Construct a RemoteExecution.
     */
    public RemoteExecution() throws UnknownHostException, IOException {
        super();
    }

    /**
     * Construct a RemoteExecution.
     */
    public RemoteExecution(String addr, int port)  throws UnknownHostException, IOException {
        super(addr, port);
    }

    /**
     * Construct a RemoteExecution.
     */
    public RemoteExecution(InetAddress addr, int port)  throws UnknownHostException, IOException {
        super(addr, port);
    }


    /**
     * Start an EventEngine
     */
    public EventEngine startEventEngine(EventDelegate ed) {
        try {
            // time to run:  86400 seconds = 1 day
            // startup script

            //return new usr.engine.ScriptEngine(86400, "scripts/AppScript2Ca");

            return new usr.engine.IKMSEventEngine(1800, "scripts/ikms.xml");
        } catch (EventEngineException eee) {
            throw new Error("Cant start event engine");
        }
    }


    /**
     * Get the name of the delegate
     */
    public String getName() {
        return "RemoteExecution";
    }

    public static void main(String[] args) {
        try {
            RemoteExecution rexec = new RemoteExecution();

            rexec.init();

            rexec.start();

            rexec.run();

            rexec.stop();
        } catch (Exception e) {
        }
    }

}

