package usr.globalcontroller;

import usr.localcontroller.LocalControllerInfo;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.net.InetAddress;
import java.util.Properties;


class LocalControllerInitiator {
    /** Return string to launch local controller on remote
     * machine given machine name
     */
    static synchronized String [] localControllerStartCommand(LocalControllerInfo lh, ControlOptions options) {
        String remoteLoginCommand_ = "ssh";     // Command used  to login to start local controller
        String remoteLoginFlags_ = "-n";        //  Flags used for ssh to login to remote machine
        Properties prop = System.getProperties();
        String remoteStartController_ = "java -cp " + prop.getProperty("java.class.path", null) + " usr.localcontroller.LocalController";
        

        boolean isLocal = false;
        InetAddress addr = null;

        try {
            addr = InetAddress.getByName(lh.getName());
        } catch (UnknownHostException uhe) {
            throw new Error(uhe.getMessage());
        }
        
        // Check if the address is a valid special local or loop back
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) {
            isLocal = true;
        
        } else {
            // Check if the address is defined on any interface
            try {
                isLocal = NetworkInterface.getByInetAddress(addr) != null;
            } catch (SocketException e) {
                isLocal = false;
            }
        }

        if (isLocal) {

            // no need to do remote command
            String classpath = System.getProperty("java.class.path");

            String [] cmd = new String[6];
            cmd[0] = "java";
            cmd[1] = "-classpath";
            cmd[2] = classpath;
            cmd[3] = "usr.localcontroller.LocalController";
            cmd[4] = String.valueOf(lh.getName());
            cmd[5] = String.valueOf(lh.getPort());

            return cmd;

        } else {
            // its a remote command
            String [] cmd = new String[6];
            cmd[0] = remoteLoginCommand_;
            cmd[1] = remoteLoginFlags_;

            // For user name in turn try info from remote, or info
            // from global or fall back to no username
            String user = lh.getRemoteLoginUser();

            if (user == null) {
                user = options.getRemoteLoginUser();
            }

            if (user == null) {
                cmd[2] = lh.getName();
            } else {
                cmd[2] = user + "@" + lh.getName();
            }

            String remote = lh.getRemoteStartController();

            if (remote == null) {
                remote = remoteStartController_;
            }
            cmd[3] = remote;
            cmd[4] = String.valueOf(lh.getName());
            cmd[5] = String.valueOf(lh.getPort());
            return cmd;
        }
    }
}
