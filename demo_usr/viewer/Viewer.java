package demo_usr.viewer;

import usr.interactor.GlobalControllerInteractor;
import usr.interactor.MCRPException;
import java.net.UnknownHostException;
import java.io.*;
import java.lang.management.ManagementFactory;

/**
 * A viewer of the network.
 */
public class Viewer {
    // The GlobalControllerInteractor
    GlobalControllerInteractor interactor;

    // GlobalController host
    String host;

    // GlobalController port
    int port;

    // The directory with all the output
    File destDir;

    // The style of the layout
    String layout = "dot";

    // The last xdot output
    String xdot = "";

    // Current snapshot no
    int count = 0;

    // The timeout for each wait
    int timeout = 2000;

    /**
     * Construct a Viewer.
     * It connects to a GlobalController on port 8888 of localhost
     */
    public Viewer()  {
        this("localhost", 8888);
    }

    /**
     * Construct a Viewer.
     * It connects to a GlobalController on port 8888 of a specified host
     */
    public Viewer(String host) {
        this(host, 8888);
    }

    /**
     * Construct a Viewer.
     * It connects to a GlobalController on the specified port of a host.
     */
    public Viewer(String host, int port) {
        this.host = host;
        this.port = port;

        destDir = createTempDirectory();

        System.err.println("Viewer: tmp dir = " + destDir);

    }

    /**
     * Connect to the GlobalController
     */
    boolean connect() throws UnknownHostException {
        try {
            interactor = new GlobalControllerInteractor(host, port);

            return true;
        } catch (UnknownHostException uke) {
            throw uke;
        } catch (IOException ioe) {
            return false;
        }
    }

    /**
     * Get the nth snapshot of the data
     */
    public String getSnapshot(String n) {
        if (n == null) {
            return getGraphData(destDir, "xdot", ""+count);
        } else {
            return getGraphData(destDir, "xdot", n);
        }
    }

    /**
     * Get the current snapshot of the data
     */
    public String getData() {
        return xdot;
    }

    /**
     * Get the current snapshot number.
     */
    public int getSnapshotNumber() {
        return count;
    }

    /**
     * Collect data from the GlobalController.
     */
    protected void collectData() {
        String graph = null;
        PipeProcess process;

        while (true) {

            /*
             * Send a version of the network is graphviz dot
             * format to 'dot' and get a version back as xdot.
             */
            try {
                // get a view of the network as a graph
                graph = interactor.networkGraph("dot");

                saveGraphData(destDir, "dot", Integer.toString(count), graph);

                String processData = null;

                do {
                    // start dot down a pipe
                    process = startDot();

                    // now get dot to process the view
                    processData = sendDataToProcess(process, graph);

                    if (processData == null) {
                        System.err.println("Viewer: pipe error - restarting pipe");
                        process.stop();
                    }

                } while (processData == null);

                // save data in xdot
                xdot = processData;

                // save the data in a file
                saveGraphData(destDir, "xdot", Integer.toString(count), xdot);

                // incr count
                count++;

            } catch (IOException ioe) {
                System.err.println("Viewer error: " + ioe);
            } catch (MCRPException me) {
                System.err.println("Viewer error: " + me);

                // if the GlobalController has gone away
                // then hang around indefinitely to drive the UI
                if (me instanceof usr.interactor.MCRPNoConnectionException) {
                    synchronized (this) {
                        try {
                            System.err.println("Waiting.....");
                            wait();
                        } catch (InterruptedException ie) {
                        }
                    }
                }
            }

            try {
                Thread.sleep(timeout);
            } catch (InterruptedException ie) {
            }

        }
    }

    /**
     * Save some data in a file
     */
    protected boolean saveGraphData(File destDir, String extention, String count, String data) {
        try {
            File file = new File(destDir, "graph-" + count + "." + extention);

            System.err.println("Viewer: save to " + file);

            FileOutputStream output = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(output);
            PrintStream ps = new PrintStream(bos);

            ps.print(data);
            ps.flush();
            ps.close();

            return true;
        } catch (IOException ioe) {
            return false;
        }
    }


    /**
     * Get some data in a file
     */
    protected String getGraphData(File destDir, String extention, String count) {
        try {
            File file = new File(destDir, "graph-" + count + "." + extention);

            System.err.println("Viewer: read from " + file);


            FileInputStream input = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(input);

            StringBuilder builder = new StringBuilder();
            int ch;
         
            while ((ch = bis.read()) != -1) {
                builder.append((char)ch);
            }

            bis.close();

            String result =  builder.toString();

            System.err.println("result = " + result.length());

            return result;

        } catch (IOException ioe) {
            return "";
        }
    }



    /**
     * Start dot down the end of a pipe.
     */
    protected PipeProcess startDot() throws IOException {
        // create a subrocess
        String [] processArgs = {"/usr/local/bin/dot", "-Txdot", "-K"+layout };
        ProcessBuilder child = new ProcessBuilder(processArgs);
        Process process = child.start();

        // get a wrapper on the process 
        return new PipeProcess(process);
    }

    /**
     * Send some data to a process
     */
    protected String sendDataToProcess(PipeProcess pipe, String data) {
        Process process = pipe.getProcess();

        // send the data to the process
        OutputStream output = process.getOutputStream();
        PrintStream ps = new PrintStream(output);

        ps.print(data);
        ps.flush();


        // close the stdin to stop the process
        ps.close();


        // wait for the process to actually end
        try {
            process.waitFor();
        } catch (InterruptedException ie) {
            System.err.println("Viewer: process wait for error: " + ie);
        }

        pipe.stop();

        // and collect the output
        String result =  pipe.getData();

        if (result == null) {
            return null;
        } else {
            System.err.println("Viewer: dot converted " + data.length() + " to " + result.length());

            return result;
        }
    }

    /**
     * Create a temporary directory
     */
    protected File createTempDirectory() {
        //String dir = System.getProperty("java.io.tmpdir");
        String dir = "/tmp";

        File dest = new File(dir, "Viewer-" + getPID());

        dest.mkdir();

        return dest;
    }

    /**
     * Get the pid of this process
     */
    protected String getPID() {
        String name = ManagementFactory.getRuntimeMXBean().getName();

        String [] parts = name.split("@");

        return parts[0];
    }

    /**
     * Set the layout
     */
    void setLayout(String l) {
        layout = l;
    }

    /**
     * Set the timeout
     */
    void setTimeout(int t) {
        timeout = t;
    }

    /**
     * The main entry point
     * Viewer args:
     * -t timeout, (default: 2 secs)
     * -l layout, (default: dot)
     * -p web_port, (default: 8080)
     * -h gc_host, (default: localhost)
     */
    public static void main(String[] args) {
        String gcHost = "localhost";
        int webPort = 8080;
        String layout = "dot";
        int timeout = 2000;

        Viewer viewer = null;

        WebServer webServer = null;

	// process args
	int argc = args.length;

	for (int arg=0; arg < argc; arg++) {
	    String thisArg = args[arg];

	    // check if its a flag
	    if (thisArg.charAt(0) == '-') {
		// get option
		char option = thisArg.charAt(1);

		// gwet next arg
		String argValue = args[++arg];

		switch (option) {

		case 'l': {
                    layout = argValue;
                    break;
                }

                case 'h': {
                    gcHost = argValue;
                    break;
                }

                case 'p': {
                    try {
                        webPort = Integer.parseInt(argValue);
                    } catch (Exception e) {
                        System.err.println("Error: " + e);
                        System.exit(1);
                    }
                    break;
                }

                case 't': {
                    try {
                        int secs = Integer.parseInt(argValue);
                        timeout = secs * 1000;
                    } catch (Exception e) {
                        System.err.println("Error: " + e);
                        System.exit(1);
                    }
                    break;
                }

                default:
		    System.err.println("Viewer: unknown option " + option);
		    break;

                }
            }
        }

        try {
            // start a web server
            webServer = new WebServer(webPort);

            // start a Viewer
            viewer = new Viewer(gcHost);
            // set the timeout
            viewer.setTimeout(timeout);
            // and set the layout
            viewer.setLayout(layout);

            // connect to the GlobalController
            boolean connected;

            System.err.print("Connecting to " + viewer.host + "/" + viewer.port + " ");

            while (true) {
                connected = viewer.connect();

                if (connected) {
                    System.err.println();
                    break;
                } else {
                    Thread.sleep(1000);
                    System.err.print("+");
                }
            }

            // tell the web server about the viewer
            webServer.setViewer(viewer);

            // now start collecting data
            viewer.collectData();
        } catch (Exception e) {
            System.err.println("Viewer Exception: " + e);
            System.exit(1);
        }
    }
 
    private static void help() {
        System.err.println("Viewer [-t timeout] [-l layout] [-p web_port] [-h gc_host]");
        System.exit(1);
    }

}


