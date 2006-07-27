/**
 * AmbientTalk/2 Project
 * ATMethod.java created on Jul 24, 2006 at 9:42:24 PM
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

import edu.vub.at.objects.grammar.ATSymbol;

/**
 * @author smostinc
 *
 * ATMethods are ambienttalk's representation of methods as named functions. These
 * functions do not close over an environment allowing for them to be shared between 
 * different clones. The environment is to be supplied during lookup (which wraps 
 * ATMethods into ATClosures). As a consequence it is not possible to
 * a) get hold of an ATMethod at the base-level (since lookup implies wrapping)
 * b) directly apply an ATMethod (as application requires context parameters)
 */
public interface ATMethod extends ATObject {

	/**
	 * Structural access to the name of the method. Note that all methods (defined
	 * using def name( ...args... ) { ... } of def foo: arg bar: arg { ... }) retain
	 * the name with which they were first bound. Literal blocks which may be created
	 * outside of a definition are implicitly named lambda.
	 */
	public ATSymbol getName();
	
	/**
	 * Structural access to the argument list of the method.
	 */
	public ATTable getArguments();
	
	/**
	 * Structural access to the body of the method.
	 */
	public ATAbstractGrammar getBody();
}
