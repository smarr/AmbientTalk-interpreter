/**
 * AmbientTalk/2 Project
 * ATParsetree.java created on Jul 23, 2006 at 11:17:27 AM
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
package edu.vub.at.objects;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.parser.SourceLocation;

import java.util.Set;


/**
 * ATAbstractGrammar contains all methods to be understood by any parsetree element
 * in the ambienttalk/2 programming language. As the parsetree is a first-class
 * entity (it can be manipulated in the language using the MOP) parsetree elements
 * are also ATObjects.
 * 
 * @author smostinc
 */
public interface ATAbstractGrammar extends ATObject {
	
	/**
	 * Return a table of {@link ATSymbol} objects representing
	 * the set of free variables of the Abstract Grammar expression.
	 */
	public ATTable base_freeVariables() throws InterpreterException;
	
	/**
	 * @return the free variables of this abstract grammar element
	 * as a set (whose elements are of type {@link ATSymbol}).
	 */
	public abstract Set impl_freeVariables() throws InterpreterException;
	
	/**
     * Should return the set of free variables contained within this AG element,
     * given that the AG element appears in the context of a quoted expression.
	 * @return a set of elements of type {@link ATSymbol}).
	 */
	public abstract Set impl_quotedFreeVariables() throws InterpreterException;

	/**
	 * @return the source location of this AG element or null if not set.
	 */
	public abstract SourceLocation impl_getLocation();
	public abstract void impl_setLocation(SourceLocation loc);

}
