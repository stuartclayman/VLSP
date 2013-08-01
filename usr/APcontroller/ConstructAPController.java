package usr.APcontroller;


import usr.logging.*;
import usr.router.RouterOptions;

/** Wrapper to construct appropriate AP controller object */
public class ConstructAPController {
    public static APController constructAPController(RouterOptions o) {
        APController a = null;
        String name = o.getAPControllerName();

        if (name == null) {
            name = "";
        }

        if (name.equals("Random")) {
            a = new RandomAPController(o);
            return a;
        }

        if (name.equals("HotSpot")) {
            a = new HotSpotAPController(o);
            return a;
        }

        if (name.equals("Pressure")) {
            a = new PressureAPController(o);
            return a;
        }

        if (name.equals("Null")) {
            a = new NullAPController(o);
            return a;
        }

        if (name.equals("") || name.equals("None")) {
            a = new NoAPController();
            return a;
        }

        Logger.getLogger("log").logln(USR.ERROR, "Unknown Access Point controller name "+name+
                                      " using no controller");
        a = new NoAPController();
        return a;
    }

}