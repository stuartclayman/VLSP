package usr.applications;

import java.util.Map;

/**
 * A marker interface for applications that support run-time monitoring.
 * If they do support run-time monitoring, a router or a probe may call
 * the getMonitoringData() method and extract some useful data from the
 * application.
 */
public interface RuntimeMonitoring {
    /**
     * Return a map of data.
     * Label -> Value
     */
    public Map<String, String> getMonitoringData();
}