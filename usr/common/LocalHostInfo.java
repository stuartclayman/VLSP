package usr.common;

import java.net.InetAddress;

import demo_usr.energymodel.EnergyModel;


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
    // 50 watts per processor 
 	private double cpuLoadC = 50;
 	
 	// hardware related coefficient for energy consumption of memory
 	// assuming 40 watt per gigabyte
 	private double memoryAllocationC=40;
 	
 	// hardware related coefficient for energy consumption of network
 	// assuming 20 watts per network card
 	private double networkLoadC=20;
 	
 	// total memory of particular physical server (in MBs)
 	long totalMemory = 8000;
 	
 	//maximum network load of particular physical server (in Mbytes);
 	double maximumNetworkLoad=1000;
 	
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

    @Override
	public String toString() {
        return hostName_ + ":" + port_;
    }

 // returns cpuload consumption coefficient
  	public double GetCPULoadCoefficient () {
  		return cpuLoadC;
  	}
  	
  	// returns memory allocation consumption coefficient
  	public double GetMemoryAllocationCoefficient () {
  		return memoryAllocationC;
  	}
  	
 	// returns network load coefficient
  	public double GetNetworkLoadCoefficient () {
  		return networkLoadC;
  	}
  	
  	// returns total physical memory in MBs
  	public long GetTotalPhysicalMemory () {
  		return totalMemory;
  	}
  	
  	// returns maximum network load (in MBytes)
  	public double GetMaximumNetworkLoad () {
  		return maximumNetworkLoad;
  	}
  	
  	// returns baseline energy consumption of particular physical host
  	public double GetBaseLineEnergyConsumption() {
  		return baseLineEnergyConsumption;
  	}
  	
  	// sets cpuload consumption coefficient
  	public void SetCPULoadCoefficient (double cpuLoadC_) {
  		cpuLoadC = cpuLoadC_;
  	}
  	
  	// returns memory allocation consumption coefficient
  	public void SetMemoryAllocationCoefficient (double memoryAllocationC_) {
  		memoryAllocationC = memoryAllocationC_;
  	}
  	
 	// returns network load coefficient
  	public void SetNetworkLoadCoefficient (double networkLoadC_) {
  		networkLoadC = networkLoadC_;
  	}
  	
  	// returns total physical memory in MBs
  	public void SetTotalPhysicalMemory (long totalMemory_) {
  		totalMemory = totalMemory_;
  	}
  	
  	// sets maximum network load (in MBytes)
  	public void SetMaximumNetworkLoad (double maximumNetworkLoad_) {
  		maximumNetworkLoad = maximumNetworkLoad_;
  	}
  	
  	// sets baseline energy consumption of particular physical host
  	public void SetBaseLineEnergyConsumption(double baseLineEnergyConsumption_) {
  		baseLineEnergyConsumption = baseLineEnergyConsumption_;
  	}
  	
  	// initialise energy model for particular physical host
  	public void InitEnergyModel () {
  		energyModel = new EnergyModel (cpuLoadC, memoryAllocationC, networkLoadC, baseLineEnergyConsumption, totalMemory, maximumNetworkLoad);
  	}
  	
  	// returns current energy consumption from the energy model
  	public double GetCurrentEnergyConsumption (double averageCPULoad, long freeMemory, double networkLoad) {
  		return energyModel.CurrentEnergyConsumption(averageCPULoad, freeMemory, networkLoad);
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

}
