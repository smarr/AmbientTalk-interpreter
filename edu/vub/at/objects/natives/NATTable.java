/**
 * AmbientTalk/2 Project
 * NATTable.java created on 26-jul-2006 at 16:48:34
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

import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.natives.grammar.NATAbstractGrammar;

/**
 * @author tvc
 *
 * The native implementation of an AmbientTalk table.
 * A table is implemented by a java array.
 */
public final class NATTable extends NATAbstractGrammar implements ATTable {

	public final static NATTable EMPTY = new NATTable(new ATObject[] {});
	
	public final ATObject[] elements_;
	
	public NATTable(ATObject[] elements) {
		elements_ = elements;
	}
	
	public NATTable(Object[] javaArray) {
		elements_ = new ATObject[javaArray.length];
		
		for(int i = 0; i < javaArray.length; i++) {
			Object element = javaArray[i];
			elements_[i] = NATObject.cast(element);
		}
	}
	
	public ATNumber getLength() { return NATNumber.atValue(elements_.length); }

	// TODO: index out of bounds checks
	public ATObject at(ATNumber index) {
		return elements_[index.asNativeNumber().javaValue];
	}

	// TODO: index out of bounds checks
	public ATObject atPut(ATNumber index, ATObject value) {
		elements_[index.asNativeNumber().javaValue] = value;
		return value;
	}
	
	public NATTable asNativeTable() { return this; }

}
