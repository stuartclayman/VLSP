package ikms.functions;

//The Information Storage and Indexing (ISI) function is a logical construct representing a distributed repository for 
//registering Entities, indexing (and optionally storing) information/knowledge. 

//List of Information Storage and Indexing operations: 
//- Information storage 
//- Information indexing 
//- Entity registration

import ikms.IKMS;
import ikms.data.DataStoreManager;
import ikms.operations.EntityRegistrationOperation;
import ikms.operations.InformationIndexingOperation;
import ikms.operations.InformationStorageOperation;

import java.util.Collection;

import com.timeindexing.index.IndexView;

public class InformationStorageAndIndexingFunction {
	// We assume a knowledge database with three storages.
	// Uris with Storage:// prefix point to information stored in the main storage
	// Uris with Index:// prefix point to information stored in the indexing storage 
	// Uris with EntityInfo:// prefix point to information stored in the Entity Registration Storage

	// The IKMS itself
	IKMS ikms;

	// The DataStoreManager
	DataStoreManager dataStoreManager;

	// The knowledge database
	//Jedis knowdb = null;
	
	// Defining ISI operations
	public InformationStorageOperation informationStorage=null;
	public InformationIndexingOperation informationIndexing=null;
	public EntityRegistrationOperation entityRegistration=null;

	public InformationStorageAndIndexingFunction (IKMS ikms, String dbHost, String dbPassword) throws Exception {
		// keep a handle on the KnowledgeBlock
		this.ikms = ikms;

		// The DataStoreManager
		dataStoreManager = new DataStoreManager(ikms, dbHost, dbPassword);
		//dataStoreManager = DataStoreManager;
		
		if (DataStoreManager.FlushKeyValueStore ()) {
			// Initialize operations
			informationStorage = new InformationStorageOperation();
			informationIndexing = new InformationIndexingOperation();
			entityRegistration = new EntityRegistrationOperation();
		} else {
			throw new Exception("Key Value Store is not running!");
		}
		
	}

	public int GetStorageMemoryUsed () {
		String memoryInfo = DataStoreManager.GetIKMSDBInfo ();
		String result="";
		String[] lines;
		//System.out.println ("TMPLINE:"+memoryInfo);

		if (memoryInfo==null)
			return 0;
		
		try {
			lines = memoryInfo.split(System.getProperty("line.separator"));
			for(String tmpLine : lines){
				if (tmpLine.contains("used_memory:")) {
					//System.out.println ("TMPLINE:"+tmpLine);
					result = tmpLine.replace("used_memory:", "").trim();
					return Integer.valueOf(result);
				}
			}
		} catch (Exception e){
			System.out.println (e.getMessage());
		} 
		return 0;
	}

	/*public double GetSystemCPUUsed () {
		Jedis knowdb = DataStoreManager.getKeyValueStore();
		String memoryInfo = knowdb.info();
		String result="";
		String[] lines = memoryInfo.split(System.getProperty("line.separator"));
		for(String tmpLine : lines){
			if (tmpLine.contains("used_cpu_sys:")) {
				result = tmpLine.replace("used_cpu_sys:", "").trim();
				DataStoreManager.releaseKeyValueStore(knowdb);
				return Double.valueOf(result);
			}
		}
		DataStoreManager.releaseKeyValueStore(knowdb);
		return 0;
	}*/

	/**
	 * Entry point to list Indexes
	 */
	public Collection<IndexView> listIndexes() {
		return dataStoreManager.listIndexes();
	}


}
