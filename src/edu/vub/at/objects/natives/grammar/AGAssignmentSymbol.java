/**
 * AmbientTalk/2 Project
 * AGAssignmentSymbol.java created on 2-jul-2007 at 13:45:32
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
package edu.vub.at.objects.natives.grammar;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATText;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATText;

/**
 * An assignment symbol is created by suffixing an ordinary symbol with a :=. It can be created 
 * explicitly through qutoting and is created when assigning a field at base-level to map such
 * assignments onto a method invocation. Also it can be created when explicitly defining accessor 
 * methods.
 * 
 * @author smostinc
 */
public class AGAssignmentSymbol extends AGSymbol{

	/**
	 * @param name a string with a ':=' suffix
	 */
	public static AGSymbol jAlloc(String name) {
		synchronized (_STRINGPOOL_) {
			AGSymbol existing = (AGSymbol) _STRINGPOOL_.get(name);
			if (existing == null) {
				existing = new AGAssignmentSymbol(name);
				_STRINGPOOL_.put(name, existing);
			}
			return existing;	
		}
	}
	
	protected AGAssignmentSymbol(String txt) {
		// cut the := part of the symbol name, e.g. foo:= -> foo
		super(txt.substring(0,txt.length()-2));
	}

	public AGAssignmentSymbol asAssignmentSymbol() {
		return this;
	}
	
	public ATText base_getText() {
		return NATText.atValue(toString());
	}

	/**
	 * It is illegal to evaluate an assignment symbol in base-level code
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		throw new XIllegalOperation("Cannot evaluate an assignment symbol as an ordinary symbol");
	}

	public NATText meta_print() {
		return NATText.atValue(toString());
	}

	public ATObject meta_resolve() throws InterpreterException {
		return AGAssignmentSymbol.jAlloc(super.toString());
	}

	public String toString() {
		return super.toString() + ":=";
	}
	
	/**
	 * @return the field name being assigned by this assignment symbol
	 */
	public AGSymbol getFieldName() {
		return AGSymbol.jAlloc(super.toString());
	}
	
}
