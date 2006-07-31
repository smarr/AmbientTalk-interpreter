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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.natives.grammar.NATAbstractGrammar;

/**
 * @author tvc
 *
 * The native implementation of an AmbientTalk table.
 * A table is implemented by a java array.
 * 
 * An important distinction between AT tables and Java arrays is that
 * ATTable objects are indexed from [1..size] rather than [0..size[
 */
public final class NATTable extends NATAbstractGrammar implements ATTable {

	public final static NATTable EMPTY = new NATTable(new ATObject[] {});
	
	// AUXILIARY STATIC FUNCTIONS
	
	/**
	 * Auxiliary function used to print the elements of the table using various separators.
	 */
	public final static NATText printElements(NATTable tab,String start, String sep, String stop) throws XTypeMismatch {
		ATObject[] els = tab.elements_;
		if (els.length == 0)
			return NATText.atValue(String.valueOf(start+stop));
		
	    StringBuffer buff = new StringBuffer(start);
		for (int i = 0; i < els.length - 1; i++) {
			buff.append(els[i].meta_print().asNativeText().javaValue + sep);
		}
		buff.append(els[els.length-1].meta_print().asNativeText().javaValue + stop);
        return NATText.atValue(buff.toString());
	}
	
	public static final NATText printAsStatements(ATTable tab) throws XTypeMismatch {
		return printElements(tab.asNativeTable(), "", "; ", "");
	}
	
	public static final NATText printAsList(ATTable tab) throws XTypeMismatch {
		return printElements(tab.asNativeTable(), "(", ", ", ")");
	}
	
	/**
	 * This function is called whenever arguments to a function, message, method need to be evaluated.
	 * TODO take variable arguments into account (i.e. table splicing)
	 */
	public static final NATTable evaluateArguments(NATTable args, ATContext ctx) throws NATException {
		return mapEvalOverExpressions(args, ctx);
	}
	
	private static final NATTable mapEvalOverExpressions(NATTable tab, ATContext ctx) throws NATException {
		if (tab == EMPTY)
			return EMPTY;
		
		ATObject[] els = tab.elements_;
		ATObject[] result = new ATObject[els.length];
		for (int i = 0; i < els.length; i++) {
			result[i] = els[i].meta_eval(ctx);
		}
		
		return new NATTable(result);
	}
	
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
	public ATObject at(ATNumber index) throws NATException {
		return elements_[index.asNativeNumber().javaValue - 1];
	}

	// TODO: index out of bounds checks
	public ATObject atPut(ATNumber index, ATObject value) throws NATException {
		elements_[index.asNativeNumber().javaValue - 1] = value;
		return value;
	}
	
	public ATBoolean isEmpty() {
		return NATBoolean.atValue(elements_.length == 0);
	}
	
	public ATTable asTable() { return this; }
	
	public NATTable asNativeTable() { return this; }
	
	public NATText meta_print() throws XTypeMismatch {
		return NATTable.printElements(this, "[", ", ","]");
	}

}
