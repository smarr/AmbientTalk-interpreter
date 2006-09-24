/**
 * AmbientTalk/2 Project
 * FieldMap.java created on 7-aug-2006 at 19:42:44
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

import edu.vub.at.objects.grammar.ATSymbol;

/**
 * @author tvc
 *
 * Instances of this class implement a so-called 'map' for an AmbientTalk object's fields.
 * The terminology stems from the language Self. A 'map' maps variable names to an integer index.
 * This index can be used to index an object's state vector to retrieve the value of a slot.
 * 
 * The advantage of maps lies in their space effiency: clones can share maps, thereby enabling
 * the sharing of variable names (which are immutable).
 * 
 * We implement a custom map from symbols to Java ints ourselves, because the java.util.HashMap
 * requires explicit boxing and unboxing of integers as java.lang.Integers, which is wasteful.
 */
public final class FieldMap {

	private static final int _DEFAULT_SIZE_ = 5;
	
	/*
	 * Field maps are represented as a simple array of the field's names.
	 * The indices of the fields are literally the indices of the field's names.
	 */
	
	private ATSymbol[] varNames_;
	
	// next free position in the varNames_ array
	private int free_;
	
	public FieldMap() {
		varNames_ = new ATSymbol[_DEFAULT_SIZE_];
		free_ = 0;
	}
	
	// used for internal cloning purposes only
	private FieldMap(ATSymbol[] copiedNames, int copiedFree) {
		varNames_ = copiedNames;
		free_ = copiedFree;
	}
	
	/**
	 * Add a new field to the field map. The field will be assigned
	 * the index of the map's current size.
	 * 
	 * If the field already exists, it will not be added twice.
	 * 
	 * @return true if the field was added successfully, false if the field already existed
	 */
	public boolean put(ATSymbol nam) {
		if (findName(nam) != -1) // if the name is found...
			return false;
		
		if(free_ == varNames_.length) {
			reAlloc();
		}
		varNames_[free_++] = nam; // increment free pointer after access
		return true;
	}
	
	/**
	 * Retrieve the index of a field given its name.
	 * @param nam the name of the field
	 * @return the index of the field or -1 if the field cannot be found.
	 */
	public int get(ATSymbol nam) {		
		int namIdx = findName(nam);
		return (namIdx >= 0) ? namIdx : -1;
	}
	
	/**
	 * Returns all field names.
	 * @return an array of the field names stored in the map.
	 */
	public ATSymbol[] listFields() {
		ATSymbol[] varnames = new ATSymbol[free_];
		for (int i = 0; i < varnames.length; i++) {
			varnames[i] = varNames_[i];
		}
		return varnames;
	}
	
	/**
	 * Creates a shallow copy of the field map.
	 */
	public FieldMap copy() {
		// allocate a copy of the names array
		ATSymbol[] newVarNames = new ATSymbol[varNames_.length];
		// copy content into new array
		System.arraycopy(varNames_, 0, newVarNames, 0, varNames_.length);
		return new FieldMap(newVarNames, free_);
	}
	
	/**
	 * Doubles the size of the map to make room for extra fields.
	 */
	private void reAlloc() {
		// allocate the new array
		ATSymbol[] newVarNames = new ATSymbol[free_ * 2];
		// copy old content into new array
		System.arraycopy(varNames_, 0, newVarNames, 0, free_);
		// throw away the old array and replace it by the new one
		varNames_ = newVarNames;
	}
	
	/**
	 * Searches for the name of a field in the varNames_ array.
	 * Performs a simple sequential search, which is usually not a bottleneck
	 * as maps are supposed to be rather small (< 50 elements).
	 * @return the index of the field or -1 if not found.
	 */
	private int findName(ATSymbol nam) {
		for (int i = 0; i < free_; i++) {
			if (varNames_[i] == nam)
				return i;
		}
		return -1;
	}

}
