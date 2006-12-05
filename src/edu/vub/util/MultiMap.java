package edu.vub.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author smostinc
 *
 * A simple Map allowing one key to be associated with a Set of values.
 */
public class MultiMap implements Map, Serializable, Cloneable {

	private static final long serialVersionUID = -7040809979612963953L;

	Map internal_ = new HashMap();

	public void clear() {
		internal_.clear();
	}

	public boolean containsKey(Object key) {
		return internal_.containsKey(key);
	}

	public boolean containsValue(Object value) {
		Collection valuesSets = internal_.values();
		for (Iterator iter = valuesSets.iterator(); iter.hasNext();) {
			Set element = (Set) iter.next();
			if(element.contains(value))
				return true;
		}
		return false;
	}

	public Set entrySet() {
		Set result = new HashSet();
		Set keys = internal_.keySet();
		for (Iterator iter = keys.iterator(); iter.hasNext();) {
			Object key = iter.next();
			Set values = (Set)get(key);
			for (Iterator iterator = values.iterator(); iterator.hasNext();) {
				Object value = iterator.next();
				result.add(new Entry(key, value));
			}
		}
		return null;
	}

	public boolean equals(Object toBeCompared) {
		if(toBeCompared instanceof MultiMap) {
			if(toBeCompared != null) {
				return internal_.equals(((MultiMap)toBeCompared).internal_);
			}
		}
		return false;
	}

	public Object get(Object key) {
		return internal_.get(key);
	}

	public int hashCode() {
		return internal_.hashCode();
	}


	public boolean isEmpty() {
		return internal_.isEmpty();
	}

	public Set keySet() {
		return internal_.keySet();
	}

	public Object put(Object key, Object value) {
		Object existingValues = internal_.get(key);
		if(existingValues == null) { 
			Set values = new HashSet();
			values.add(value); 
			internal_.put(key, values);
		} else {
			((Set)existingValues).add(value);
		}
		return null;
	}

	/**
	 * Puts multivalued entries for one key
	 */
	public Object putValues(Object key, Collection values) {
		Object existingValues = internal_.get(key);
		if(existingValues == null) {
			Set setOfValues = new HashSet();
			values.addAll(values); 
			internal_.put(key, setOfValues);
		} else {
			((Set)existingValues).addAll(values);
		}
		return null;
	}

	public void putAll(Map toMerge) {
		Set merged = toMerge.entrySet();
		
		// INFO this can probably be done more efficient
		for (Iterator iter = merged.iterator(); iter.hasNext();) {
			Map.Entry element = (Map.Entry) iter.next();
			put(element.getKey(), element.getValue());
		}
	}

	/**
	 * Special case of putAll for merging MultiMaps
	 */
	public void putAll(MultiMap toMerge) {
		Set keys = toMerge.keySet();		

		// INFO this can probably be done more efficient
		for (Iterator iter = keys.iterator(); iter.hasNext();) {
			Object key = iter.next();
			Collection values = (Collection)toMerge.get(key);
			putValues(key, values);
		}
	}

	/**
	 * Returns a Set of the values that were associated 
	 * with this key.
	 * @see java.util.Map#remove(java.lang.Object)
	 */
	public Object remove(Object key) {
		return internal_.remove(key);
	}

	/**
	 * Removes a single value associated with a key 
	 */
	public boolean removeValue(Object key, Object value) {
		Set values = (Set)internal_.get(key);
		if(values != null) {
			return values.remove(value);
		} else {
			return false;
		}
	}

	public int size() {
		return internal_.size();
	}

	/**
	 * Returns a Set of Sets
	 * @see java.util.Map#values()
	 */
	public Collection values() {
		return internal_.values();
	}

	private void replaceValue(Object key, Object oldValue, Object newValue) {
		Set values = (Set)internal_.get(key);
		values.remove(oldValue);
		values.add(newValue);
	}

	class Entry implements Map.Entry {

		private final Object key_;
		private Object value_;		

		public Entry(Object key, Object value) {
			key_ = key;
			value_ = value;
		}

		public Object getKey() {
			return key_;
		}

		public Object getValue() {
			return value_;
		}

		public Object setValue(Object newValue) {
			Object result = value_;
			value_ = newValue;
			replaceValue(key_, result, value_);
			return result;
		}
	}
}


