package usr.router.command;

import usr.protocol.MCRP;
import usr.logging.*;
import usr.router.RouterManagementConsole;
import usr.router.RouterPort;
import usr.router.NetIF;
import usr.router.NetStats;
import usr.net.Address;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The GET_NETIF_STATS command.
 */
public class GetNetIFStatsCommand extends RouterCommand {
    /**
     * Construct a GetNetIFStatsCommand.
     */
    public GetNetIFStatsCommand() {
        super(MCRP.GET_NETIF_STATS.CMD, MCRP.GET_NETIF_STATS.CODE, MCRP.ERROR.CODE);
    }

    /**
     * Evaluate the Command.
     */
    public boolean evaluate(String req) {
        List<RouterPort> ports = controller.listPorts();
        int count = 0;
        NetStats stats= null;
        String statsString;
        // do localnet first
        NetIF localNetIF = controller.getLocalNetIF();
        if (localNetIF != null) {
           stats = localNetIF.getStats();
       
        // put out netif name
            statsString =localNetIF.getRemoteRouterName()+ " "+localNetIF.getName() 
               + " " + stats.toString();
            list(statsString);
            count++;
        }
        

        for (RouterPort rp : ports) {
            if (rp.equals(RouterPort.EMPTY)) {
                continue;
            } else {
                NetIF netIF = rp.getNetIF();
                if (netIF != null) {

                    stats = netIF.getStats();


                // put out netif name
                    statsString = netIF.getRemoteRouterName()+ " " +
                        netIF.getName() + " " + stats.toString();
                //System.err.println(statsString);  

                      list(statsString);
                      count++;
                }
            }               
        }             

        boolean result = success("END " + count);

        if (!result) {
            Logger.getLogger("log").logln(USR.ERROR, leadin() + "LIST_CONNECTIONS response failed");
        }

        return result;

    }

}
