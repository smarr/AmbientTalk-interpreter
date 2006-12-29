/**
 * AmbientTalk/2 Project
 * MethodDictionary.java created on Oct 10, 2006 at 1:10:24 PM
 * (c) Programming Technology Lab, 2006 - 2007
 * Authors: Tom Van Cutsem & Stijn Mostinckx
 * 
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package edu.vub.at.objects.natives;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Instances of this class implement a dictionary mapping selectors to AmbientTalk methods.
 * These method dictionaries are a traditional candidate for being shared among different
 * clones of an object, improving the space efficiency of the language. 
 * 
 * The advantage of maps lies in the fact that they can be used implicitly to keep track
 * of which objects were cloned from one another. As said above clones share the dictionaries.
 * To ensure that this relation is upheld when one clone makes an idiosyncratic change, 
 * we store from which method dictionary the altered copy was created.
 *
 * @author smostinc
 */
public class MethodDictionary implements Map, Cloneable, Serializable {

	private static final int _DEFAULT_SIZE_ = 5;
	
	private MethodDictionary parent_ = null;
	
	private HashMap methods_;
	
	public MethodDictionary() {
		methods_ = new HashMap(_DEFAULT_SIZE_);
	}
	
	// used internally to clone the method dictionary
	private MethodDictionary(HashMap methods, MethodDictionary parent) {
		this.methods_ = methods;
		this.parent_ = parent;
	}

	public void clear() {
		methods_.clear();
	}

	public boolean containsKey(Object key) {
		return methods_.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return methods_.containsKey(value);
	}

	public Set entrySet() {
		return methods_.entrySet();
	}

	public Object get(Object key) {
		return methods_.get(key);
	}

	public boolean isEmpty() {
		return methods_.isEmpty();
	}

	public Set keySet() {
		return methods_.keySet();
	}

	public Object put(Object key, Object value) {
		return methods_.put(key, value);
	}

	public void putAll(Map keyValuePairs) {
		methods_.putAll(keyValuePairs);
	}

	public Object remove(Object key) {
		return methods_.remove(key);
	}

	public int size() {
		return methods_.size();

	}

	public Collection values() {
		return methods_.values();
	}
	
	/* 
	 * Creates a shallow copy of the method dictionary to allow for addition of methods to an object 
	 * which has already been cloned. Keeps track of the original MethodDictionary to be able to derive
	 * whether method dictionaries have a common origin.
	 */
	protected Object clone() {
		return new MethodDictionary((HashMap)methods_.clone(), this);
	}
	
	/**
	 * Checks whether both MethodDictionaries are equal or whether the passed object is a
	 * MethodDictionary from which this one (indirectly) originates. 
	 */
	public boolean isDerivedFrom(MethodDictionary aMethodDictionary) {
		return (this == aMethodDictionary) ||
			((parent_ != null) && parent_.isDerivedFrom(aMethodDictionary));
	}


}
