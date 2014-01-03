package ikms.console;

import ikms.IKMS;
import ikms.core.DataHandler;
import ikms.core.ProcessorHandler;
import ikms.core.RegisterHandler;
import ikms.core.StreamHandler;
import cc.clayman.console.AbstractRestConsole;

/**
 * A ManagementConsole for the IKMS.
 * It listens for commands.
 */
public class IKMSManagementConsole extends AbstractRestConsole {

    private IKMS ikms;

    public IKMSManagementConsole(IKMS ikms, int port) {

        this.ikms = ikms;
        initialise(port);
    }

    public IKMS getIKMS() {
        return ikms;
    }


    public void registerCommands() {

        // setup  /data/ handler to get and set data values
        defineRequestHandler("/data/.*", new DataHandler());

        // setup  /register/ handler to handle registrations
        defineRequestHandler("/register/.*", new RegisterHandler());

        // setup default /stream/ handler
        // handles requests for streams only
        defineRequestHandler("/stream/", new StreamHandler());

        // setup default /stream/id/info/ handler
        //defineRequestHandler("/stream/[0-9]+/info/", new StreamInfoHandler());

        // setup default /processor/ handler
        defineRequestHandler("/processor/", new ProcessorHandler());
    }

}
