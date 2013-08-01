package usr.globalcontroller;

import java.util.List;

/**
 * An interface for reporters that can give info on traffic.
 * <p>
 * Traffic info will be of the form: <br/>
 * name | InBytes | InPackets | InErrors | InDropped | InDataBytes | InDataPackets | OutBytes | OutPackets | OutErrors | OutDropped
 *| OutDataBytes | OutDataPackets | InQueue | BiggestInQueue | OutQueue | BiggestOutQueue |
 * Router-1 localnet | 2548 | 13 | 0 | 0 | 2548 | 13 | 10584 | 54 | 0 | 0 | 10584 | 54 | 0 | 1 | 0 | 0 |
 */
public interface TrafficInfo {
    /**
     * Get the traffic for a link Router-i to Router-j
     * @param routerSrc the name of source router
     * @param routerDst the name of dest router
     */
    public List<Object> getTraffic(String routerSrc, String routerDst);
}