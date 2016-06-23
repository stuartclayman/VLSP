package demo_usr.energy.viewer;

import cc.clayman.console.AbstractRestConsole;

/**
 * A Rest console for the EnergyViewer
 */
public class EnergyViewerConsole extends AbstractRestConsole {
    public EnergyViewerConsole(EnergyViewer viewer, int port) {
        setAssociated(viewer);
        initialise(port);
    }

    public void registerCommands() {

        // setup  /energy/ handler 
        defineRequestHandler("/energy/.*", new EnergyHandler());

    }

    /**
     * The handler calls this to set the energy price
     */
    protected void setEnergyPrice(Double price) {
        ((EnergyViewer)getAssociated()).setEnergyPrice(price);
    }

    /**
     * Get the energy usage
     */
    protected double getEnergyUsage() {
        return ((EnergyViewer)getAssociated()).getEnergyUsage();
    }

}
