package usr.APcontroller;

/** Implements Extra information for the Null AP controller (essentially none) */


public class RandomAPInfo implements APInfo {
    boolean controller_ = false;  // Is this router an AP controller

    public RandomAPInfo() {
    }

    /** Returns true if this router is an AP Controller */
    public boolean isAPController() {
        return controller_;
    }

    /** Sets whether this router is an AP Controller */
    public void setAPController(boolean controller) {
        controller_ = controller;
    }

}