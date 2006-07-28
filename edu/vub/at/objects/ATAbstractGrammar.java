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

import edu.vub.at.exceptions.NATException;

/**
 * @author smostinc
 *
 * ATAbstractGrammar contains all methods to be understood by any parsetree element
 * in the ambienttalk/2 programming language. As the parsetree is a first-class
 * entity (it can be manipulated in the language using the MOP) parsetree elements
 * are also ATObjects.
 */
public interface ATAbstractGrammar extends ATObject {

	/**
	 * Evaluates a particular parsetree with respect to a particular context.
	 * @param ctx - context (object) to lookup bindings in.
	 * @throws NATException 
	 */
	public ATObject meta_eval(ATContext ctx) throws NATException;
	
	/**
	 * Quotes a parsetree, in other words allows the parsetree to return itself
	 * instead of evaluating. This mode is triggered when a quotation parsetree
	 * element was encountered and is switched off again when an unquotation 
	 * parsetree element is found. The context is passed on behalf of these possible
	 * future evaluations.
	 * @param ctx - context passed on to be used in subsequent evaluations.
	 */
	public ATAbstractGrammar meta_quote(ATContext ctx);
	
}
