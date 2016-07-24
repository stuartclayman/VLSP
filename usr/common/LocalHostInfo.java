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

    // average energy consumption of all server devices, besides network, cpu and memory
    // assuming 300Watts in total
    private double baseLineEnergyConsumption=300;

    // max network transmission of particular physical host, i.e. for normalizing network related measurements
	private long maxNetworkTransmissionBytes = 100000;

    // for non-linear model (cpu, memory, network, hd, baseline)
    private double a1 = 50; // CPU
    private double b1 = 20; // CPU
    private double c1 = 0; // CPU - all to baseline
    private double r1 = 1.4; // non-linearity factor, 0 for linear
    private double a2 = 4; // MEMORY
    private double b2 = 2; // MEMORY
    private double c2 = 0; // CPU - all to baseline
    private double r2 = 1.4; // non-linearity factor, 0 for linear
    private double a3 = 0.00001; // NETWORK
    private double b3 = 0.000005; // NETWORK
    private double c3 = 0; // NETWORK - all to baseline
    private double r3 = 1.4; // non-linearity factor
    private double a4 = 1; // TBD (HD)
    private double b4 = 1; // TBD (HD)
    private double c4 = 0; // TBD (HD) - all to baseline
    private double r4 = 1.4; // non-linearity factor, 0 for linear
    private double c = 300; // Baseline energy consumption
    
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
    
    // Non-linear model Energy coefficients for CPU
    public double GetA1 () {
        return a1;
    }
    public double GetB1 () {
        return b1;
    }
    public double GetC1 () {
        return c1;
    }
    public double GetR1 () {
        return r1;
    }
    public void SetA1 (double a1_) {
		a1 = a1_;
    }
    public void SetB1 (double b1_) {
		b1 = b1_;
    }
    public void SetC1 (double c1_) {
		c1 = c1_;
    }
    public void SetR1 (double r1_) {
		r1 = r1_;
    }
    
    // Non-linear model Energy coefficients for MEMORY
    public double GetA2 () {
        return a2;
    }
    public double GetB2 () {
        return b2;
    }
    public double GetC2 () {
        return c2;
    }
    public double GetR2 () {
        return r2;
    }
    public void SetA2 (double a2_) {
		a2 = a2_;
    }
    public void SetB2 (double b2_) {
		b2 = b2_;
    }
    public void SetC2 (double c2_) {
		c2 = c2_;
    }
    public void SetR2 (double r2_) {
		r2 = r2_;
    }
    
    // Non-linear model Energy coefficients for NETWORK
    public double GetA3 () {
        return a3;
    }
    public double GetB3 () {
        return b3;
    }
    public double GetC3 () {
        return c3;
    }
    public double GetR3 () {
        return r3;
    }
    public void SetA3 (double a3_) {
		a3 = a3_;
    }
    public void SetB3 (double b3_) {
		b3 = b3_;
    }
    public void SetC3 (double c3_) {
		c3 = c3_;
    }
    public void SetR3 (double r3_) {
		r3 = r3_;
    }
    
    // Non-linear model Energy coefficients for HD - TBD
    public double GetA4 () {
        return a4;
    }
    public double GetB4 () {
        return b4;
    }
    public double GetC4 () {
        return c4;
    }
    public double GetR4 () {
        return r4;
    }
    public void SetA4 (double a4_) {
		a4 = a4_;
    }
    public void SetB4 (double b4_) {
		b4 = b4_;
    }
    public void SetC4 (double c4_) {
		c4 = c4_;
    }
    public void SetR4 (double r4_) {
		r4 = r4_;
    }
    
    // Non-linear model Energy coefficients for HD - TBD
    public double GetC () {
        return c;
    }
    public void SetC (double c_) {
    		c = c_;
    }
    
    // returns maximum network transmission of particular physical host, in bytes
    public double GetMaxNetworkTransmissionBytes() {
        return maxNetworkTransmissionBytes;
    }
    
    // sets maximum network transmission of particular physical host, in bytes
    public void SetMaxNetworkTransmissionBytes(long maxNetworkTransmissionBytes_) {
        maxNetworkTransmissionBytes = maxNetworkTransmissionBytes_;
    }

    // initialise energy model for particular physical host
    public void InitEnergyModel () {
        // old linear
    		//energyModelLinear = new EnergyModelLinear (cpuLoadCoefficient, cpuIdleCoefficient, memoryAllocationCoefficient, freeMemoryCoefficient, networkOutboundBytesCoefficient, networkIncomingBytesCoefficient, baseLineEnergyConsumption);
    		// non-linear
		energyModel = new EnergyModel (a1, b1, c1, r1, a2, b2, c2, r2, a3, b3, c3, r3, a4, b4, c4, r4, c, maxNetworkTransmissionBytes);
    }

    // returns current energy consumption from the energy model
    public double GetCurrentEnergyConsumption (float averageCPULoad, float averageIdleCPU, float memoryUsed, float freeMemory, long networkOutboundBytes, long networkIncomingBytes, float loadAverage) {
        return energyModel.CurrentEnergyConsumption (averageCPULoad, averageIdleCPU, memoryUsed, freeMemory, networkOutboundBytes, networkIncomingBytes, loadAverage);
    }
    
    // returns future energy consumption from the energy model
    public double GetCurrentEnergyConsumption (float averageCPULoad, float averageIdleCPU, float memoryUsed, float freeMemory, long networkOutboundBytes, long networkIncomingBytes, float loadAverage, double extraCPU, double extraMemory) {
        return energyModel.CurrentEnergyConsumption (averageCPULoad, averageIdleCPU, memoryUsed, freeMemory, networkOutboundBytes, networkIncomingBytes, loadAverage, extraCPU, extraMemory);
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
