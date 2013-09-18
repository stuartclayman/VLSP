package usr.common;
import java.io.IOException;
/**
   Given a range of ports returns one or
 */
public class PortPool {
    boolean[] occupied_;
    int lowPort_;
    int highPort_;
    int currPort_;



    public PortPool (int lowPort, int highPort) {
        lowPort_ = lowPort;
        highPort_ = highPort;
        int numPorts = highPort-lowPort+1;
        occupied_ = new boolean[numPorts];

        for (int i = 0; i < numPorts; i++) {
            occupied_[i] = false;
        }
        currPort_ = lowPort;
    }

    public int findPort() throws IOException {
        return findPort(1);

    }

    /* Return the number of a port where there are nPorts
       consecutive unused ports and mark them as occupied*/
    public int findPort(int nPorts) throws IOException {
        int startPort = currPort_;

        if (startPort >= highPort_) {
            startPort = lowPort_;
            currPort_ = lowPort_;
        }

        for (int i = startPort; i < highPort_; i++) {
            int freeCount = 0;

            while (freeCount < nPorts) {
                if (occupied_[i-lowPort_] == true) {
                    break;
                }
                i++;
                freeCount++;
            }


            if (freeCount == nPorts) {
                for (int j = 1; j <= nPorts; j++) {
                    occupied_[i-j-lowPort_] = true;
                }
                currPort_ = i;
                return currPort_-nPorts;
            }
        }
        IOException e = new IOException ("No free ports available in port pool");
        throw e;
    }

    /** Free port p*/
    public void freePort(int p) {
        occupied_[p-lowPort_] = false;
    }

    /** Free ports from l to h */
    public void freePorts(int l, int h) {
        for (int i = l; i <=h; i++) {
            occupied_[i-lowPort_] = false;
        }
    }

}
