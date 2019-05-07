package usr.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import usr.localcontroller.LocalControllerInfo;

/**
 * Class provides simple information that the global and local
 * controllers need about a jvm application.
 */
public class BasicJvmInfo extends AbstractElementInfo {
    private String className_ = null;
    private String [] args_ = null;


    /**
     * BasicJvmInfo with jvm id, start time, the LocalController,
     */
    public BasicJvmInfo(int jvmid, long time, LocalControllerInfo lc, String cname, String [] args) {
        id_ = jvmid;
        time_ = time;
        controller_ = lc;
        className_ = cname;
        args_ = args;
    }

    
    /**
     * Get the jvm className_
     */
    public String getName() {
        return className_ + "-" + id_;
    }

    /**
     * Get the jvm className_
     */
    public String getClassName() {
        return className_;
    }

    /**
     * Get the jvm args_
     */
    public  String [] getArgs() {
        return args_;
    }

    /**
     * Get the hostname of the LocalController managing the router
     */
    public String getHost() {
        return controller_.getName();
    }

    /**
     * Check if this is equal to another BasicJvmInfo
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BasicJvmInfo) {
            BasicJvmInfo other = (BasicJvmInfo)obj;

            if (other.id_ == this.id_ &&
                other.className_ == this.className_ &&
                other.args_ == this.args_) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * hashCode for BasicJvmInfo
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
        return getHost() + ":" + getId() +
            " -> " + getName() + " " + getArgs() ;
    }

}
