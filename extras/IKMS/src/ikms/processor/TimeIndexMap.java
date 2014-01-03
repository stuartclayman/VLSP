package ikms.processor;

import ikms.data.DataStoreManager;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.timeindexing.data.SerializableItem;
import com.timeindexing.index.IndexView;
import com.timeindexing.index.TimeIndexException;
import com.timeindexing.time.MillisecondTimestamp;

public class TimeIndexMap {
    // A map of keys to TimeIndexes
    Map<String, IndexView> timeindexMap;

    /**
     * Construct a TimeIndexManager which has a bunch of TimeIndexes
     * and behaves like a Map.
     * <p>
     * The TimeIndexMap keeps every value put into it.
     * <p>
     * The caller must close() the TimeIndexMap if all the data is to be saved.
     */
    public TimeIndexMap() {
        timeindexMap = new HashMap<String, IndexView>() ;
    }


    /**
     * Returns the number of key-value mappings in this map. 
     *
     * @return the number of key-value mappings in this map.
     */
    int size() {
        return timeindexMap.size();
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings.
     */
    boolean isEmpty()  {
        return timeindexMap.isEmpty();
    }


    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key. 
     *
     * @param key key whose presence in this map is to be tested.
     * @return <tt>true</tt> if this map contains a mapping for the specified
     *         key.
     * 
     */
    boolean containsKey(Object key)  {
        return timeindexMap.containsKey(key);
    }


    /**
     * Returns the value to which the specified key is mapped in this identity
     * hash map, or <tt>null</tt> if the map contains no mapping for this key.
     * A return value of <tt>null</tt> does not <i>necessarily</i> indicate
     * that the map contains no mapping for the key; it is also possible that
     * the map explicitly maps the key to <tt>null</tt>. The
     * <tt>containsKey</tt> method may be used to distinguish these two cases.
     *
     * @param   key the key whose associated value is to be returned.
     * @return  the value to which this map maps the specified key, or
     *          <tt>null</tt> if the map contains no mapping for this key.
     * @see #put(Object, Object)
     */
    IndexView get(String key)  {
        IndexView index = null;

        // Get a handle on the IndexView
        if (timeindexMap.containsKey(key)) {
            // we've seen this one before
            index = timeindexMap.get(key);

            return index;

        } else {
            return null;
        }
    }


    // Modification Operations

    /**
     * Associates the specified value with the specified key in this map
     * (optional operation).  If the map previously contained a mapping for
     * this key, the old value is replaced by the specified value.  (A map
     * <tt>m</tt> is said to contain a mapping for a key <tt>k</tt> if and only
     * if {@link #containsKey(Object) m.containsKey(k)} would return
     * <tt>true</tt>.)) 
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or <tt>null</tt>
     *	       if there was no mapping for key.  A <tt>null</tt> return can
     *	       also indicate that the map previously associated <tt>null</tt>
     *	       with the specified key, if the implementation supports
     *	       <tt>null</tt> values.
     * 
     * @throws UnsupportedOperationException if the <tt>put</tt> operation is
     *	          not supported by this map.
     * @throws ClassCastException if the class of the specified key or value
     * 	          prevents it from being stored in this map.
     * @throws IllegalArgumentException if some aspect of this key or value
     *	          prevents it from being stored in this map.
     * @throws NullPointerException if this map does not permit <tt>null</tt>
     *            keys or values, and the specified key or value is
     *            <tt>null</tt>.
     */
    void put(String key, Object value) throws TimeIndexException {
        IndexView index = null;

        // Get a handle on the IndexView
        if (timeindexMap.containsKey(key)) {
            // we've seen this one before
            index = timeindexMap.get(key);
        } else {
            // we need to open this index
            IndexView timeI = DataStoreManager.getTimeIndex(key);
            timeindexMap.put(key, timeI);

            index = timeI;
        }

        if (value instanceof Serializable) {
            Serializable object = (Serializable)value;
            index.addItem(new SerializableItem(object), new MillisecondTimestamp());

            return;

        } else {
            throw new IllegalArgumentException("Class " + value.getClass().getName() +
                                               " is not Serializable");
        }
        
    }

    /**
     * Removes the mapping for this key from this map if it is present
     * (optional operation). 
     *
     * @param key key whose mapping is to be removed from the map.
     */
    void remove(Object key) throws TimeIndexException {
        IndexView index = null;

        // Get a handle on the IndexView
        if (timeindexMap.containsKey(key)) {
            // we've seen this one before
            index = timeindexMap.get(key);
            index.close();
        } else {
        }
    }


    // Bulk Operations

    /**
     * Copies all of the mappings from the specified map to this map
     * (optional operation).  The effect of this call is equivalent to that
     * of calling {@link #put(Object,Object) put(k, v)} on this map once
     * for each mapping from key <tt>k</tt> to value <tt>v</tt> in the 
     * specified map.  The behavior of this operation is unspecified if the
     * specified map is modified while the operation is in progress.
     *
     * @param t Mappings to be stored in this map.
     * 
     * @throws UnsupportedOperationException if the <tt>putAll</tt> method is
     * 		  not supported by this map.
     * 
     * @throws ClassCastException if the class of a key or value in the
     * 	          specified map prevents it from being stored in this map.
     * 
     * @throws IllegalArgumentException some aspect of a key or value in the
     *	          specified map prevents it from being stored in this map.
     * @throws NullPointerException if the specified map is <tt>null</tt>, or if
     *         this map does not permit <tt>null</tt> keys or values, and the
     *         specified map contains <tt>null</tt> keys or values.
     */
    void putAll(Map<String, Object> t) throws TimeIndexException {
        for (Map.Entry<String, Object> entry : t.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();

            // store the value
            put(k, v);
        }

    }

    /**
     * Removes all mappings from this map (optional operation).
     *
     * @throws UnsupportedOperationException clear is not supported by this
     * 		  map.
     */
    void clear() throws TimeIndexException {
        for (IndexView index : timeindexMap.values()) {
            index.close();
        }
        timeindexMap.clear();
    }


    // Views

    /**
     * Returns a set view of the keys contained in this map.  The set is
     * backed by the map, so changes to the map are reflected in the set, and
     * vice-versa.  If the map is modified while an iteration over the set is
     * in progress (except through the iterator's own <tt>remove</tt>
     * operation), the results of the iteration are undefined.  The set
     * supports element removal, which removes the corresponding mapping from
     * the map, via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt> <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the add or <tt>addAll</tt> operations.
     *
     * @return a set view of the keys contained in this map.
     */
    Set<String> keySet() {
        return timeindexMap.keySet();
    }



    // Comparison and hashing

    /**
     * Compares the specified object with this map for equality.  Returns
     * <tt>true</tt> if the given object is also a map and the two Maps
     * represent the same mappings.  More formally, two maps <tt>t1</tt> and
     * <tt>t2</tt> represent the same mappings if
     * <tt>t1.entrySet().equals(t2.entrySet())</tt>.  This ensures that the
     * <tt>equals</tt> method works properly across different implementations
     * of the <tt>Map</tt> interface.
     *
     * @param o object to be compared for equality with this map.
     * @return <tt>true</tt> if the specified object is equal to this map.
     */
    public boolean equals(Object o) {
        if (o instanceof TimeIndexMap) {
            TimeIndexMap tim = (TimeIndexMap)o;
            return this.timeindexMap.hashCode() == tim.timeindexMap.hashCode();
        } else {
            return false;
        }
    }

    /**
     * Returns the hash code value for this map.  The hash code of a map
     * is defined to be the sum of the hashCodes of each entry in the map's
     * entrySet view.  This ensures that <tt>t1.equals(t2)</tt> implies
     * that <tt>t1.hashCode()==t2.hashCode()</tt> for any two maps
     * <tt>t1</tt> and <tt>t2</tt>, as required by the general
     * contract of Object.hashCode.
     *
     * @return the hash code value for this map.
     * @see Map.Entry#hashCode()
     * @see Object#hashCode()
     * @see Object#equals(Object)
     * @see #equals(Object)
     */
    public int hashCode() {
        return timeindexMap.hashCode();
    }


    /**
     * Close down this map.
     */
    public void close() throws TimeIndexException {
        clear();
    }

}


