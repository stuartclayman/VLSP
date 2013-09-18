package usr.console;

import java.io.IOException;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

import cc.clayman.console.BasicRequestHandler;
import cc.clayman.console.ManagementConsole;

/**
 * An abstraxct base class for all RequestHandlers.
 */
public class CommandAsRestHandler extends BasicRequestHandler  {
    public CommandAsRestHandler() {
    }

    /**
     * Handle a request and send a response.
     */
    @Override
	public boolean handle(Request request, Response response) {
        try {
            long time = System.currentTimeMillis();

            response.set("Content-Type", "application/json");
            response.set("Server", "Router/1.0 (Simple 4.0)");
            response.setDate("Date", time);
            response.setDate("Last-Modified", time);

            String path = request.getPath().getPath();

            String value = path.substring(9);

            //System.out.println("value: " + value);

            value = java.net.URLDecoder.decode(value, "UTF-8");

            //System.out.println("decode value: " + value);

            // find the command
            int endOfCommand = value.indexOf(' ');
            String commandName;

            // get the command from the input
            if (endOfCommand == -1) {
                // if there is no space the whole input is the command name
                commandName = value;
            } else {
                // from 0 to first space
                commandName = value.substring(0, endOfCommand);
            }

            //System.out.println(getClass().getName() + " command: " + commandName);

            // now lookup the command
            ManagementConsole mc = getManagementConsole();
            USRRestConsole rc = (USRRestConsole)mc;
            RestCommand command = (RestCommand)rc.find(commandName);
            if (command != null) {
                // we got a command
            } else {
                //fetch the UnknownCommand
                command = (RestCommand)rc.find("__UNKNOWN__");
            }

            // and evaluate the input
            try {
                command.evaluate(request, response);

            } catch (Exception e) {
                // try and send generic error code
                e.printStackTrace();
            }

            // the evaluator will handle the response
            // check if the response is closed
            response.close();

            return true;

        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        }

        return false;

    }

}