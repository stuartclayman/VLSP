package usr.APcontroller;


import usr.logging.Logger;
import usr.logging.USR;
import usr.router.RouterOptions;
import java.lang.reflect.Constructor;
import usr.logging.Logger;
import usr.logging.USR;

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

        // if the name has a value, try ans resolve the APController
        // by assuming it is a classname
        if (name != null) {
            try {
                //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "APController class name: " + name);
                Class<?> c = Class.forName(name);
                Class<? extends APController> cc = c.asSubclass(APController.class);

                //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "APController class: " + cc);
                // find Constructor for when arg is RouterController
                Constructor<? extends APController> cons = cc.getDeclaredConstructor(RouterOptions.class);
                //Logger.getLogger("log").logln(USR.STDOUT, leadin() + "APController Constructor: " + cons);

                a = cons.newInstance(o);

                Logger.getLogger("log").logln(USR.STDOUT, leadin() + "Setup APController: " + a);


                return a;

            } catch (ClassNotFoundException cnfe) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Class not found " + name);
            } catch (Exception e) {
                Logger.getLogger("log").logln(USR.ERROR, leadin() + "Cannot instantiate class " + name);
                e.printStackTrace();
            }
        }
        
        Logger.getLogger("log").logln(USR.ERROR, "Unknown Access Point controller name "+name+
                                      " using no controller");
        a = new NoAPController();
        return a;
    }

    /**
     * Create the String to print out before a message
     */
    private static String leadin() {
        return "ConstructAPController: ";
    }


}
