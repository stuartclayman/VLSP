package ikms.operations;

// Information communicated through the ICD function is optionally indexed. After this stage, the information could be 
// retrieved and passed back to the ICD function for dissemination. 
// Indexed information could be collected as part of an information aggregation or knowledge production operation

import ikms.data.DataStoreManager;

import java.util.ArrayList;
import java.util.Iterator;

public class InformationIndexingOperation {
	//DataStoreManager dataStoreManager;	

	public InformationIndexingOperation () {
		//dataStoreManager = dataStoreManager_;
	}

	public String GetInformationIndex (String uri) {
		String value = DataStoreManager.IKMSDBGet("Index://"+uri);
		System.out.println ("Fetching uri:"+uri+" from index storage, value:"+value);
		if (value==null) {
			value = SearchBreakingDownUri ("Index://"+uri);
		}
		return value;
	}

	public String IndexInformation (String uri, String locationURL) {
		String output = DataStoreManager.IKMSDBSet("Index://"+uri, locationURL);
		System.out.println ("Storing indexing information for uri:"+uri+" in index storage, location:"+locationURL+" output:"+output);
		return output;
	}

	public String IndexArrayList (int entityid, ArrayList<String> uris, String locationURL) {
		String output = null;
		String uri = null;

		//get an Iterator object for ArrayList using iterator() method.
		Iterator<String> itr = uris.iterator();

		//use hasNext() and next() methods of Iterator to iterate through the elements
		ArrayList<String> urisToIndex = new ArrayList<String>();

		while(itr.hasNext()) {
			uri = "Index://"+itr.next();
			urisToIndex.add(uri);
			System.out.println ("Indexing information for entity:"+entityid+" uri:"+uri+" location:"+locationURL);
		}
		output += DataStoreManager.IKMSDBSet(urisToIndex, locationURL);

		return output;
	}

	public long RemoveIndexArrayList (int entityid, ArrayList<String> uris) {
		long output=0;
		String uri = null;

		//get an Iterator object for ArrayList using iterator() method.
		Iterator<String> itr = uris.iterator();

		//use hasNext() and next() methods of Iterator to iterate through the elements
		ArrayList<String> urisToRemove = new ArrayList<String>();
		while(itr.hasNext()) {
			uri = "Index://"+itr.next();
			urisToRemove.add(uri);
			System.out.println ("Removing Information Index for entity:"+entityid+" uri:"+uri);
		}
		output = DataStoreManager.IKMSDBDel(urisToRemove);

		return output;
	}

	public ArrayList<String> GetInformationSetIndexingFromStorage (ArrayList<String> uris) {
		return null;
	}

	String SearchBreakingDownUri (String uri) {
		String[] uris = uri.split("/");
		String result="";
		String value=null;

		for( int i = 0; i <= uris.length - 1; i++)
		{
			if (result=="Index:")
				result+="/";

			if (!result.equals(""))
				result+="/";

			result+=uris[i];
			//System.out.println("Searching URI:"+result+"/All");
			value = DataStoreManager.IKMSDBGet(result+"/All");
			if (value!=null) {
				return value;
			}
		}
		return null;
	}
}
