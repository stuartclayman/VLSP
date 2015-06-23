package usr.localcontroller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Scanner;
import usr.common.PipeProcess;
import java.util.regex.*;

import eu.reservoir.monitoring.appl.datarate.EveryNSeconds;
import eu.reservoir.monitoring.core.DefaultProbeAttribute;
import eu.reservoir.monitoring.core.DefaultProbeValue;
import eu.reservoir.monitoring.core.Probe;
import eu.reservoir.monitoring.core.ProbeAttributeType;
import eu.reservoir.monitoring.core.ProbeMeasurement;
import eu.reservoir.monitoring.core.ProbeValue;
import eu.reservoir.monitoring.core.ProducerMeasurement;
import eu.reservoir.monitoring.core.TypeException;
import eu.reservoir.monitoring.core.list.DefaultMList;
import eu.reservoir.monitoring.core.list.MList;
import eu.reservoir.monitoring.core.table.DefaultTable;
import eu.reservoir.monitoring.core.table.DefaultTableHeader;
import eu.reservoir.monitoring.core.table.DefaultTableRow;
import eu.reservoir.monitoring.core.table.DefaultTableValue;
import eu.reservoir.monitoring.core.table.Table;
import eu.reservoir.monitoring.core.table.TableHeader;
import eu.reservoir.monitoring.core.table.TableProbeAttribute;
import eu.reservoir.monitoring.core.table.TableRow;
import eu.reservoir.monitoring.core.table.TableValue;

import eu.reservoir.monitoring.appl.host.linux.CPUDev;
import eu.reservoir.monitoring.appl.host.linux.MemoryDev;
import eu.reservoir.monitoring.appl.host.linux.NetDev;


/**
 * A probe that gets info on a host
 */
public class HostInfoProbe extends LocalControllerProbe implements Probe {
    PipeProcess process;

    CPUDev cpuDev;

    MemoryDev memDev;

    NetDev netDev;

    /**
     * Construct a HostInfoProbe
     */
    public HostInfoProbe(LocalController cont) {
        setController(cont);


        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.startsWith("mac")) {
            // Mac OS

        } else if (osName.startsWith("linux")) {
            // Linux

            cpuDev = new CPUDev();

            // base reading
            cpuDev.read(false);

            memDev = new MemoryDev();

            netDev = new NetDev("eth0");

            // read data, but calculate nothing
            netDev.read(false);




        } else {
            throw new Error("HostInfoProbe: not implemented for " + osName + " yet!");
        }


        // set probe name
        setName(cont.getName()+".hostInfo");
        // set data rate
        setDataRate(new EveryNSeconds(10));


        // setup the probe attributes
        // The LocalController name
        addProbeAttribute(new DefaultProbeAttribute(0, "Name", ProbeAttributeType.STRING, "name"));
        addProbeAttribute(new DefaultProbeAttribute(1, "cpu-user", ProbeAttributeType.FLOAT, "percent"));
        addProbeAttribute(new DefaultProbeAttribute(2, "cpu-sys", ProbeAttributeType.FLOAT, "percent"));
        addProbeAttribute(new DefaultProbeAttribute(3, "cpu-idle", ProbeAttributeType.FLOAT, "percent"));

        addProbeAttribute(new DefaultProbeAttribute(4, "mem-used", ProbeAttributeType.INTEGER, "Mb"));
        addProbeAttribute(new DefaultProbeAttribute(5, "mem-free", ProbeAttributeType.INTEGER, "Mb"));
        addProbeAttribute(new DefaultProbeAttribute(6, "mem-total", ProbeAttributeType.INTEGER, "Mb"));

        addProbeAttribute(new DefaultProbeAttribute(7, "in-packets", ProbeAttributeType.LONG, "n"));
        addProbeAttribute(new DefaultProbeAttribute(8, "in-bytes", ProbeAttributeType.LONG, "n"));

        addProbeAttribute(new DefaultProbeAttribute(9, "out-packets", ProbeAttributeType.LONG, "n"));
        addProbeAttribute(new DefaultProbeAttribute(10, "out-bytes", ProbeAttributeType.LONG, "n"));

    }

    /**
     * Collect a measurement.
     */
    @Override
    public ProbeMeasurement collect() {
        try {
            String osName = System.getProperty("os.name").toLowerCase();

            if (osName.startsWith("mac")) {
                // Mac OS
                return macos();
            } else if (osName.startsWith("linux")) {
                // Linux
                return linux();
            } else {
                throw new Error("HostInfoProbe: not implemented for " + osName + " yet!");
            }
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Get data for MacOS
     */
    protected ProducerMeasurement macos() {
        try {
            // create a list for Probe Values
            ArrayList<ProbeValue> list = new ArrayList<ProbeValue>();


            // add the name of the controller
            list.add(new DefaultProbeValue(0, getController().getName() ));


            // get some data from "top"
            PipeProcess top = startMacOSProcess("/usr/bin/env top -l 1 -n 0 -F");

            String topData = getDataFromPipeProcess(top);

            processTopData(topData, list);


            // get some data from "netstat"
            PipeProcess netstat = startMacOSProcess("/usr/bin/env netstat -b -i -n");
                
            String netData = getDataFromPipeProcess(netstat);


            processNetstatData(netData, list);


            // Create the Measurement
            ProducerMeasurement m = new ProducerMeasurement(this, list, "HostInfo");


            //System.err.println("m = " + m);

            return m;
        } catch (IOException e) {
            // a failure somewhere
            return null;
        } catch (TypeException te) {
            return null;

        }
    }

    /**
     * Get data for linux
     */
    protected ProducerMeasurement linux() {
        try {
            // create a list for Probe Values
            ArrayList<ProbeValue> list = new ArrayList<ProbeValue>();


            // add the name of the controller
            list.add(new DefaultProbeValue(0, getController().getName() ));

            /* CPU */

            // delta reading
            cpuDev.read(true);

            ArrayList<String> keyList = new ArrayList<String>(cpuDev.dataKeys());

            //System.err.println("cpuDev info => " + keyList);

            int cpuNo = keyList.size() / 4;

            float user = 0;
            float sys  = 0;
            float idle = 0;

            for (int cpu = 0; cpu < cpuNo; cpu++) {
                user += cpuDev.getDeltaValue("cpu" + cpu + "-user") +  cpuDev.getDeltaValue("cpu" + cpu + "-nice");
                sys += cpuDev.getDeltaValue("cpu" + cpu + "-system");
                idle += cpuDev.getDeltaValue("cpu" + cpu + "-idle");

                //java.text.DecimalFormat df = new java.text.DecimalFormat("#.##");

                /* System.err.println("CPU: " + df.format(cpu) + 
                                   " user = " + df.format(user) +
                                   " system = " + df.format(sys) +
                                   " idle = " + df.format(idle)); */
            }


            list.add(new DefaultProbeValue(1,user/cpuNo));
            list.add(new DefaultProbeValue(2, sys/cpuNo));
            list.add(new DefaultProbeValue(3, idle/cpuNo));

            /*  MEMORY */

            memDev.read();

            int memTotal = memDev.getCurrentValue("MemTotal");
            int memFree = memDev.getCurrentValue("MemFree");
            int cached = memDev.getCurrentValue("Cached");
            int buffers = memDev.getCurrentValue("Buffers");

            int used = memTotal - memFree;
            int reallyUsed = used - (cached + buffers);

            /* System.err.println("memoryInfo => " +
              " total = " + memTotal +
              " free = " + memFree +
              " used = " + used +
              " reallyUsed = " + reallyUsed); */

            // convert to Mbs
            list.add(new DefaultProbeValue(4, reallyUsed/1024));
            list.add(new DefaultProbeValue(5, memFree/1024));
            list.add(new DefaultProbeValue(6, memTotal/1024));


            /* NET */

            // read the data
            netDev.read(true);

            // now collect up the results	
            long in_bytes = netDev.getCurrentValue("in_bytes");
            long in_packets = netDev.getCurrentValue("in_packets");
            //int in_errors = netDev.getDeltaValue("in_errors");
            //int in_dropped = netDev.getDeltaValue("in_dropped");
            long out_bytes = netDev.getCurrentValue("out_bytes");
            long out_packets = netDev.getCurrentValue("out_packets");
            //int out_errors = netDev.getDeltaValue("out_errors");
            //int out_dropped = netDev.getDeltaValue("out_dropped");


            /* System.err.println("netInfo => " +
              " inBytes = " + in_bytes +
              " inPackets = " + in_packets +
              " outBytes = " + out_bytes +
              " outPackets = " + out_packets); */

            // add data to ProbeValue list
            list.add(new DefaultProbeValue(7,in_packets));
            list.add(new DefaultProbeValue(8, in_bytes));
            list.add(new DefaultProbeValue(9, out_packets));
            list.add(new DefaultProbeValue(10, out_bytes));



            // Create the Measurement
            ProducerMeasurement m = new ProducerMeasurement(this, list, "HostInfo");

            //System.err.println("m = " + m);

            return m;
        } catch (TypeException te) {
            return null;

        } catch (Exception e) {
            e.printStackTrace();
            // a failure somewhere
            return null;
        }


    }

    /**
     * Start 'top' down the end of a pipe.
     */
    protected PipeProcess startMacOSProcess(String proc) throws IOException {
        // create a subrocess
        String [] processArgs = proc.split(" ");
        ProcessBuilder child = new ProcessBuilder(processArgs);
        Process process = child.start();

        // get a wrapper on the process
        return new PipeProcess(process);
    }


    /**
     * Get Data from PipeProcess
     */
    protected String getDataFromPipeProcess(PipeProcess pipe) throws IOException {
        Process process = pipe.getProcess();

        // wait for the process to actually end
        try {
            process.waitFor();
        } catch (InterruptedException ie) {
            System.err.println("PipeProcess: process wait for error: " + ie);
        }

        pipe.stop();

        // and collect the output
        String result =  pipe.getData();

        if (result == null) {
            return null;
        } else if (result.length() == 0) {
            throw new IOException("PipeProcess: failed to process data");
        } else {
            //System.err.println("PipeProcess: collected " + result.length());

            return result;
        }
    }


    /**
     * Process the returned data.
     * <p>
     * Expecting: <br/>
     * Processes: 246 total, 4 running, 3 stuck, 239 sleeping, 1832 threads 
     * 2014/03/18 16:48:57
     * Load Avg: 0.85, 0.74, 0.74 
     * CPU usage: 3.86% user, 11.15% sys, 84.97% idle 
     * MemRegions: 187131 total, 7462M resident, 0B private, 2246M shared.
     * PhysMem: 3386M wired, 6399M active, 4318M inactive, 14G used, 2277M free.
     * VM: 480G vsize, 0B framework vsize, 41219330(0) pageins, 6670142(0) pageouts.
     * Networks: packets: 86118150/51G in, 76529464/19G out.
     * Disks: 10163200/243G read, 25746920/589G written.
     */
    protected void processTopData(String raw, ArrayList<ProbeValue> list) {
        try { 

            // split lines
            String[] parts = raw.split("\n");

            String cpu = parts[3];
            String mem = parts[5];
            String net = parts[7];

            // split cpu
            // CPU usage: 3.82% user, 11.6% sys, 85.10% idle 
            String[] cpuParts = cpu.split("\\s+");

            //System.err.println("cpuParts = " + Arrays.asList(cpuParts));


            float cpuUser = toFloat(cpuParts[2]);
            float cpuSys = toFloat(cpuParts[4]);
            float cpuIdle = toFloat(cpuParts[6]);

            list.add(new DefaultProbeValue(1,cpuUser));
            list.add(new DefaultProbeValue(2, cpuSys));
            list.add(new DefaultProbeValue(3, cpuIdle));

            // split mem
            // PhysMem: 3411M wired, 6513M active, 4309M inactive, 14G used, 2143M free.
            // PhysMem: 2480M used (666M wired), 2143M unused.
            String[] memParts = mem.split("\\s+");

            //System.err.println("memParts = " + Arrays.asList(memParts));

            int memTotal = 0;
            int memUsed = 0;
            int memFree = 0;

            if (memParts.length == 11) { // style 1
                int used = toInt(memParts[1]) + toInt(memParts[3]) + toInt(memParts[5]);
                int free = toInt(memParts[9]);
                int total = used + free;

                memUsed = used;
                memFree = free;
                memTotal = total;

            } else if (memParts.length == 7) { // style 2
                int used = toInt(memParts[1]) + toInt(memParts[3]);
                int free = toInt(memParts[5]);
                int total = used + free;

                memUsed = used;
                memFree = free;
                memTotal = total;

            } else {
            }

            list.add(new DefaultProbeValue(4,memUsed));
            list.add(new DefaultProbeValue(5, memFree));
            list.add(new DefaultProbeValue(6, memTotal));



            // split net
            // Networks: packets: 86128340/51G in, 76539791/19G out.
            String[] netParts = net.split("\\s+");

            //System.err.println("netParts = " + Arrays.asList(netParts));


            String inParts[] = netParts[2].split("/");
            String outParts[] = netParts[4].split("/");

            int inPackets = toInt(inParts[0]);
            int inVolume = toInt(inParts[1]);

            int outPackets = toInt(outParts[0]);
            int outVolume = toInt(outParts[1]);

        } catch (TypeException te) {
            return;
        }
    }


    // Name  Mtu   Network       Address            Ipkts Ierrs     Ibytes    Opkts Oerrs     Obytes  Coll
    // lo0   16384 <Link#1>                      38160393     0 13512249693 38160393     0 13512249693     0
    // lo0   16384 fe80::1%lo0 fe80:1::1         38160393     - 13512249693 38160393     - 13512249693     -
    // lo0   16384 127           127.0.0.1       38160393     - 13512249693 38160393     - 13512249693     -
    // lo0   16384 ::1/128     ::1               38160393     - 13512249693 38160393     - 13512249693     -
    // gif0* 1280  <Link#2>                             0     0          0        0     0          0     0
    // stf0* 1280  <Link#3>                             0     0          0        0     0          0     0
    // en0   1500  <Link#4>    14:10:9f:ce:34:f9 49615246     0 41909285921 39950842     0 7378437798     0
    // en0   1500  fe80::1610: fe80:4::1610:9fff 49615246     - 41909285921 39950842     - 7378437798     -
    // en0   1500  10.111/17     10.111.112.215  49615246     - 41909285921 39950842     - 7378437798     -
    // p2p0  2304  <Link#5>    06:10:9f:ce:34:f9        0     0          0        0     0          0     0
    protected void processNetstatData(String raw, ArrayList<ProbeValue> list) {
        try { 
            long inBytes = 0;
            long inPackets = 0;
            long outBytes = 0;
            long outPackets = 0;

            // split lines
            String[] parts = raw.split("\n");

            // skip through all lines
            for (String part : parts) {
                String[] words = part.split("\\s+");

                if (words[0].startsWith("en") && words[10].equals("0")) {
                    // we found an en0 value
                    inPackets += toLong(words[4]);
                    inBytes += toLong(words[6]);
                    outPackets += toLong(words[7]);
                    outBytes += toLong(words[9]);

                } else {
                    continue;
                }
            }

            // add data to ProbeValue list
            list.add(new DefaultProbeValue(7,inPackets));
            list.add(new DefaultProbeValue(8, inBytes));
            list.add(new DefaultProbeValue(9, outPackets));
            list.add(new DefaultProbeValue(10, outBytes));



        } catch (TypeException te) {
            return;
        }
    }


    private int toInt(String s) {
        // drop M at end
        String numStr = "";
        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(s); 
        if (m.find()) {
            numStr = m.group();
        }

        // cvt to Integer
        Scanner sc = new Scanner(numStr);
        int i = sc.nextInt();

        return i;
    }

    private long toLong(String s) {
        // drop M at end
        String numStr = "";
        Pattern p = Pattern.compile("\\d+");
        Matcher m = p.matcher(s); 
        if (m.find()) {
            numStr = m.group();
        }

        // cvt to Integer
        Scanner sc = new Scanner(numStr);
        long l = sc.nextLong();

        return l;
    }

    private float toFloat(String s) {
        // drop M at end
        String numStr = "";
        Pattern p = Pattern.compile("[\\d\\.]+");
        Matcher m = p.matcher(s); 
        if (m.find()) {
            numStr = m.group();
        }

        // cvt to Integer
        Scanner sc = new Scanner(numStr);
        float f = sc.nextFloat();

        return f;
    }
}
