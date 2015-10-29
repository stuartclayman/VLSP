package usr.common;

import java.net.InetAddress;

import demo_usr.energy.energymodel.EnergyModel;


/**
 * LocalHostInfo contains basic info about one host in the system
 * It deals with finding out IP addresses, ports and so on
 */
public class LocalHostInfo {
    private String hostName_;                 // Name of host -- should be resolvable
    private int port_;                        // Port host listens on
    private java.net.InetAddress ipAddress_;  // IP address of host
    private int lowPort_ = 0;                 // Lowest number port available (if localcontroller)
    private int highPort_ = 0;                // Highest number port available (if localcontroller)

    // energy efficiency model parameters (physical server related)
    // hardware related coefficient for energy consumption of cpu
    // assuming maximum average consumption per machine
    // 50 watts at working state, 20 watts per idle state
    private double cpuLoadCoefficient = 50;
    private double cpuIdleCoefficient = 20;

    // hardware related coefficient for energy consumption of memory
    // assuming 4 watt per gigabyte
    private double memoryAllocationCoefficient=4;
    private double freeMemoryCoefficient=2;

    // hardware related coefficient for energy consumption of network
    // assuming 0.00001 watts to send and 0.000005 watts to receive (per byte)
    private double networkOutboundBytesCoefficient=0.00001;
    private double networkIncomingBytesCoefficient=0.000005;

    // average energy consumption of all server devices, besides newtwork, cpu and memory
    // assuming 300Watts in total
    private double baseLineEnergyConsumption=300;

    // energy model for particular physical machine
    private EnergyModel energyModel;

    /** initMyInfo -- assumes that the current local host is the
     * host in question and finds out the correct info for host name
     * and ip address
     */
    public LocalHostInfo (int port) throws java.net.UnknownHostException {

        port_ = port;
        ipAddress_ = java.net.InetAddress.getLocalHost();

        hostName_ = ipAddress_.getHostName();
        //      Logger.getLogger("log").logln(USR.STDOUT, hostName_+" "+ipAddress_.getHostAddress());
    }

    public LocalHostInfo (String hostPort) throws java.net.UnknownHostException
    // Construct LocalHostInfo from host and port
    {
        int port = 0;
        String [] args = hostPort.split(":");

        if (args.length != 2) {
            throw new java.net.UnknownHostException("LocalHostInfo constructor expects string host:port");
        }
        try {
            port = Integer.parseInt(args[1]);
        } catch (java.lang.NumberFormatException e) {
            throw new java.net.UnknownHostException("LocalHostInfo constructor expects string host:port "+e.getMessage());

        }

        init(args[0], port);
    }

    public LocalHostInfo (java.net.InetAddress ip, int port) {

        port_ = port;
        ipAddress_ = ip;
        hostName_ = ipAddress_.getHostName();
        //      Logger.getLogger("log").logln(USR.STDOUT, hostName_+" "+ipAddress_.getHostAddress());
    }

    public LocalHostInfo (String hostName, int port) throws java.net.UnknownHostException {

        init(hostName, port);
        //      Logger.getLogger("log").logln(USR.STDOUT, hostName_+" "+ipAddress_.getHostAddress());
    }

    public void init(String hostName, int port) throws java.net.UnknownHostException {
        port_ = port;

        hostName_ = hostName;

        ipAddress_ = InetAddress.getByName(hostName);

    }

    /** Accessor function for port_
     */
    public int getPort() {
        return port_;
    }

    /**Accessor function for IP address
     */
    public java.net.InetAddress getIp() {
        return ipAddress_;
    }

    /**Accessor function for name
     */
    public String getName() {
        return hostName_;
    }

    public int getLowPort() {
        return lowPort_;
    }

    public int getHighPort() {
        return highPort_;
    }

    public void setLowPort(int low) {
        lowPort_ = low;
    }

    public void setHighPort(int high) {
        highPort_ = high;
    }

    // returns cpuload consumption coefficient (working mode)
    public double GetCPULoadCoefficient () {
        return cpuLoadCoefficient;
    }

    // returns cpuload consumption coefficient (idle mode)
    public double GetCPUIdleCoefficient () {
        return cpuIdleCoefficient;
    }	

    // returns memory allocation consumption coefficient
    public double GetMemoryAllocationCoefficient () {
        return memoryAllocationCoefficient;
    }

    // returns free memory consumption coefficient
    public double GetFreeMemoryCoefficient () {
        return freeMemoryCoefficient;
    }

    // returns network outbound load energy consumption coefficient
    public double GetNetworkOutboundBytesCoefficient () {
        return networkOutboundBytesCoefficient;
    }

    // returns network incoming load energy consumption coefficient
    public double GetNetworkIncomingBytesCoefficient () {
        return networkIncomingBytesCoefficient;
    }

    // returns baseline energy consumption of particular physical host
    public double GetBaseLineEnergyConsumption() {
        return baseLineEnergyConsumption;
    }

    // sets cpuload consumption coefficient
    public void SetCPULoadCoefficient (double cpuLoadCoefficient_) {
        cpuLoadCoefficient = cpuLoadCoefficient_;
    }

    // sets cpuload consumption coefficient (idle mode)
    public void SetCPUIdleCoefficient (double cpuIdleCoefficient_) {
        cpuIdleCoefficient = cpuIdleCoefficient_;
    }	

    // sets memory allocation consumption coefficient
    public void SetMemoryAllocationCoefficient (double memoryAllocationCoefficient_) {
        memoryAllocationCoefficient = memoryAllocationCoefficient_;
    }

    // sets free memory consumption coefficient
    public void SetFreeMemoryCoefficient (double freeMemoryCoefficient_) {
        freeMemoryCoefficient = freeMemoryCoefficient_;
    }

    // sets network outbound load energy consumption coefficient
    public void SetNetworkOutboundBytesCoefficient (double networkOutboundBytesCoefficient_) {
        networkOutboundBytesCoefficient = networkOutboundBytesCoefficient_;
    }

    // sets network incoming load energy consumption coefficient
    public void SetNetworkIncomingBytesCoefficient (double networkIncomingBytesCoefficient_) {
        networkIncomingBytesCoefficient = networkIncomingBytesCoefficient_;
    }
	
    // sets baseline energy consumption of particular physical host
    public void SetBaseLineEnergyConsumption(double baseLineEnergyConsumption_) {
        baseLineEnergyConsumption = baseLineEnergyConsumption_;
    }

    // initialise energy model for particular physical host
    public void InitEnergyModel () {
        energyModel = new EnergyModel (cpuLoadCoefficient, cpuIdleCoefficient, memoryAllocationCoefficient, freeMemoryCoefficient, networkOutboundBytesCoefficient, networkIncomingBytesCoefficient, baseLineEnergyConsumption);
    }

    // returns current energy consumption from the energy model
    public double GetCurrentEnergyConsumption (float averageCPULoad, float averageIdleCPU, float memoryUsed, float freeMemory, long networkOutboundBytes, long networkIncomingBytes) {
        return energyModel.CurrentEnergyConsumption (averageCPULoad, averageIdleCPU, memoryUsed, freeMemory, networkOutboundBytes, networkIncomingBytes);
    }

    /**
     * Check if this is equal to another LocalHostInfo
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LocalHostInfo) {
            LocalHostInfo other = (LocalHostInfo)obj;

            if (other.ipAddress_ == this.ipAddress_ &&
                other.port_ == this.port_) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * hashCode for LocalHostInfo
     */
    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    /**
     * To string
     */
    @Override
    public String toString() {
        return getName() + ":" + getPort();
    }


}
