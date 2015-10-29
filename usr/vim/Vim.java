package usr.vim;

import usr.globalcontroller.GlobalController;

/**
 * The Virtual Infrastructure Manager.
 */
public class Vim {
    // The GlobalController
    GlobalController gc;

    /**
     * Construct a Virtual Infrastructure Manager
     */
    public Vim() {
        gc = new GlobalController();
    }


    /**
     * Init the Vim
     */
    protected boolean init() {
        return gc.init();
    }


    /**
     * Start the Vim
     */
    public void start() {
        gc.start();
    }


    /**
     * Stop the Vim
     */
    public void stop() {
        gc.shutDown();
    }

    /**
     * Set the startup file
     */
    public void setStartupFile(String file) {
        gc.setStartupFile(file);
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Command line must specify XML file to read.");
            System.exit(-1);
        }

        try {
            Vim vControl = new Vim();

            if (args.length > 1) {
                vControl.setStartupFile(args[1]);
                vControl.init();
            } else {
                vControl.setStartupFile(args[0]);
                vControl.init();
            }

            vControl.start();


            System.out.println("Vim complete");

        } catch (Throwable t) {
            System.exit(1);
        }

    }

}
