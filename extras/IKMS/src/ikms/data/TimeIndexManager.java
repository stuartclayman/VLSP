package ikms.data;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.timeindexing.index.IndexType;
import com.timeindexing.index.IndexView;
import com.timeindexing.index.TimeIndexException;
import com.timeindexing.index.TimeIndexFactory;


public class TimeIndexManager {
    // A map of keys to TimeIndexes
    Map<String, IndexView> timeindexMap;

    //  a TimeIndexFactory
    TimeIndexFactory factory;

    // The place to store the collected data
    String collectorPath = "/tmp/";

    /**
     * Construct a TimeIndexManager which has a bunch of TimeIndexes
     * and behaves like a Map.
     * <p>
     * The TimeIndexMap keeps every value put into it.
     * <p>
     * The caller must close() the TimeIndexMap if all the data is to be saved.
     */
    public TimeIndexManager() {
        timeindexMap = new HashMap<String, IndexView>() ;

        // create a TimeIndexFactory
        factory = new TimeIndexFactory();
    }


    /**
     * Get an index by name
     */
    public IndexView getIndex(String name) {
        String internal = internalName(name);
        if (timeindexMap.containsKey(internal)) {
            IndexView index = timeindexMap.get(internal);
            return index;

        } else {
            IndexView index = createIndex(name);
            return index;
        }
    }
            


    /**
     * Create a time index
     */
    protected IndexView createIndex(String name) {
	try {
	    Properties indexProperties = new Properties();

	    String realName = internalName(name);
	    File dataIndexPath = new File(collectorPath, realName);
	    indexProperties.setProperty("indexpath",  dataIndexPath.getPath());
	    indexProperties.setProperty("name", realName);

	    IndexView dataIndex = factory.create(IndexType.EXTERNAL, indexProperties);

            System.err.println("TimeIndexManager: createIndex " + realName);

            timeindexMap.put(realName, dataIndex);

            return dataIndex;

	} catch (TimeIndexException tie) {
	    tie.printStackTrace();
	    throw new RuntimeException("Cannot create TimeIndex " + name);
	}
    }



    /**
     * Entry point to list Indexes
     */
    public synchronized Collection<IndexView> listIndexes() {
        return timeindexMap.values();
    }



    /**
     * Convert a simple name to an internal name
     */
    private String internalName(String name) {
        return name + "-stream";
    } 

}


