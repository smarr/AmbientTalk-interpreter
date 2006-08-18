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
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XIndexOutOfBounds;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.natives.grammar.AGExpression;

import java.util.LinkedList;

/**
 * @author tvc
 *
 * The native implementation of an AmbientTalk table.
 * A table is implemented by a java array.
 * 
 * An important distinction between AT tables and Java arrays is that
 * ATTable objects are indexed from [1..size] rather than [0..size[
 */
public final class NATTable extends AGExpression implements ATTable {

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
	 * TODO: currently does not work for user-defined tables
	 */
	public static final NATTable evaluateArguments(NATTable args, ATContext ctx) throws NATException {
		if (args == EMPTY) return EMPTY;
		
		ATObject[] els = args.elements_;
		
		LinkedList result = new LinkedList();
		int siz = els.length;
		for (int i = 0; i < els.length; i++) {
			if (els[i].isSplice()) {
				ATObject[] tbl = els[i].asSplice().getExpression().meta_eval(ctx).asNativeTable().elements_;
				for (int j = 0; j < tbl.length; j++) {
					result.add(tbl[j]);
				}
				siz += (tbl.length - 1); // -1 because we replace one element by a table of elements
			} else {
				result.add(els[i].meta_eval(ctx));
			}
		}
		return new NATTable((ATObject[]) result.toArray(new ATObject[siz]));
	}
	
	/**
	 * Auxiliary function to bind formal parameters to actual arguments within a certain scope.
	 * TODO: currently does not work for user-defined ATTables
	 * 
	 * @param funnam the name of the function for which to bind these elements, for debugging purposes only
	 * @param scope the frame in which to store the bindings
	 * @param parameters the formal parameter references (of which the last element may be a 'rest' arg to collect left-over arguments)
	 * @param arguments the actual arguments, already evaluated
	 * @param isDefinition if true, define the parameters, if false, assign them instead
	 * @throws XArityMismatch when the formals don't match the actuals
	 */
	public static final void bindArguments(String funnam, ATObject scope, ATTable parameters, ATTable arguments, boolean isDefinition) throws NATException {
		ATObject[] pars = parameters.asNativeTable().elements_;
		ATObject[] args = arguments.asNativeTable().elements_;
		
		// check to see whether the last argument is a spliced parameters, which
		// indicates a variable parameter list
		if (pars[pars.length - 1].isSplice()) {
			int numMandatoryPars = (pars.length - 1);
			// if so, check whether at least all mandatory parameters are matched
			if (args.length < numMandatoryPars)
				throw new XArityMismatch(funnam, numMandatoryPars, args.length);
			
			// bind all parameters except for the last one
			for (int i = 0; i < numMandatoryPars; i++) {
				scope.meta_defineField(pars[i].asSymbol(), args[i]);
			}
			
			// bind the last parameter to the remaining arguments
			int numRemainingArgs = args.length - numMandatoryPars;
			ATObject[] restArgs = new ATObject[numRemainingArgs];
			for (int i = 0; i < numRemainingArgs; i++) {
				restArgs[i] = args[i + numMandatoryPars];
			}
			ATSymbol restArgsName = pars[numMandatoryPars].asSplice().getExpression().asSymbol();
			if (isDefinition)
				scope.meta_defineField(restArgsName, new NATTable(restArgs));
			else
				scope.meta_assignField(restArgsName, new NATTable(restArgs));
			
		} else {
			// regular parameter list: arguments and parameters have to match exactly
			if (pars.length != args.length)
				throw new XArityMismatch(funnam, pars.length, args.length);	
		
			if (isDefinition) {
				for (int i = 0; i < pars.length; i++) {
				     scope.meta_defineField(pars[i].asSymbol(), args[i]);	
			    }
			} else {
				for (int i = 0; i < pars.length; i++) {
					scope.meta_assignField(pars[i].asSymbol(), args[i]);	
				}
			}
		}
	}
	
	
	// instance variables
	
	public final ATObject[] elements_;
	
	public static final NATTable atValue(ATObject[] array) {
		if (array.length == 0)
			return NATTable.EMPTY;
		else
			return new NATTable(array);
	}
	
	// TODO: make these constructors private to ensure singleton EMPTY
	
	public NATTable(ATObject[] elements) {
		// assert elements.length > 0
		elements_ = elements;
	}
	
	public NATTable(Object[] javaArray) {
		elements_ = new ATObject[javaArray.length];
		
		for(int i = 0; i < javaArray.length; i++) {
			Object element = javaArray[i];
			elements_[i] = Reflection.downObject(element);
		}
	}
	
	public ATNumber base_getLength() { return NATNumber.atValue(elements_.length); }

	public ATObject base_at(ATNumber index) throws NATException {
		return elements_[extractIndex(index)];
	}

	public ATObject base_atPut(ATNumber index, ATObject value) throws NATException {
		elements_[extractIndex(index)] = value;
		return value;
	}
	
	public ATBoolean base_isEmpty() {
		return NATBoolean.atValue(elements_.length == 0);
	}
	
	public ATNil base_each_(ATClosure clo) throws NATException {
		for (int i = 0; i < elements_.length; i++) {
			clo.meta_apply(new NATTable(new ATObject[] { elements_[i] }));
		}
		return NATNil._INSTANCE_;
	}
	
	public ATObject base_collect_(ATClosure clo) throws NATException {
		if (this == EMPTY) return EMPTY;
		
		ATObject[] result = new ATObject[elements_.length];
		for (int i = 0; i < elements_.length; i++) {
			result[i] = clo.meta_apply(new NATTable(new ATObject[] { elements_[i] }));
		}
		return new NATTable(result);
	}
	
	public ATTable asTable() { return this; }
	
	public NATTable asNativeTable() { return this; }
	
	/**
	 * To evaluate a table, evaluate all of its constituent expressions, taking
	 * special care to take into account spliced expressions.
	 * 
	 * NATTAB(exps).eval(ctx) = NATTAB(map eval(ctx) over exps)
	 * 
	 * @return a table of evaluated arguments
	 */
	public ATObject meta_eval(ATContext ctx) throws NATException {
		if (this == EMPTY) return EMPTY;
		
		LinkedList result = new LinkedList();
		int siz = elements_.length;
		for (int i = 0; i < elements_.length; i++) {
			if (elements_[i].isSplice()) {
				ATObject[] tbl = elements_[i].asSplice().getExpression().meta_eval(ctx).asNativeTable().elements_;
				for (int j = 0; j < tbl.length; j++) {
					result.add(tbl[j]);
				}
				siz += (tbl.length - 1); // -1 because we replace one element by a table of elements
			} else {
				result.add(elements_[i].meta_eval(ctx));
			}
		}
		return new NATTable((ATObject[]) result.toArray(new ATObject[siz]));
	}
	
	/**
	 * To quote a table, quote all elements of the table.
	 * Special care needs to be taken in order to properly deal with unquote-spliced elements.
	 * When one of the direct elements of the table is an unquote-splice element, the resulting
	 * unquotation must result in a table whose elements are directly added to this table's elements.
	 */
	public ATObject meta_quote(ATContext ctx) throws NATException {
		if (this == EMPTY) return EMPTY;
		
		LinkedList result = new LinkedList();
		int siz = elements_.length;
		for (int i = 0; i < elements_.length; i++) {
			if (elements_[i].isUnquoteSplice()) {
				ATObject[] tbl = elements_[i].asUnquoteSplice().getExpression().meta_eval(ctx).asNativeTable().elements_;
				for (int j = 0; j < tbl.length; j++) {
					result.add(tbl[j]);
				}
				siz += (tbl.length - 1); // -1 because we replace one element by a table of elements
			} else {
				result.add(elements_[i].meta_quote(ctx));
			}
		}
		return new NATTable((ATObject[]) result.toArray(new ATObject[siz]));
	}
	
	public NATText meta_print() throws XTypeMismatch {
		return NATTable.printElements(this, "[", ", ","]");
	}
	
	protected int extractIndex(ATNumber atIndex) throws NATException {
		int javaIndex = atIndex.asNativeNumber().javaValue - 1;
		if ((javaIndex < 0) || (javaIndex >= elements_.length))
			throw new XIndexOutOfBounds(javaIndex + 1, elements_.length);
		else
			return javaIndex;
	}

}
