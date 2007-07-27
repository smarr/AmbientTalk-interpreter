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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATSymbol;

/**
 * ATMethods are the AmbientTalk's representation of methods as named functions. These
 * functions do not close over an environment allowing for them to be shared between 
 * different clones. The environment is to be supplied during lookup (which wraps a
 * method into a {@link ATClosure}). As a consequence, it is not possible to
 * get hold of a method at the base-level (since lookup implies wrapping).
 * 
 * @author smostinc
 * @author tvcutsem
 */
public interface ATMethod extends ATObject {

	/**
	 * Wrap the receiver method into a closure which packs together the code (method) and the scope (context)
	 * in which the code should be evaluated.
	 * 
	 * @param lexicalScope the lexical scope in which the method was created. During method invocation,
	 * lexical lookup should proceed along this scope.
	 * @param dynamicReceiver the dynamic receiver (value of <tt>self</tt>) at the time the method is
	 * selected from an object.
	 * @return a closure wrapping this method.
	 */
	public ATClosure base_wrap(ATObject lexicalScope, ATObject dynamicReceiver) throws InterpreterException;
	
	/**
	 * Applies the method to the given arguments in the given context.
	 * The context is usually supplied by a closure and is necessary in order to
	 * pair a method with its current receiver (its 'self').
	 * <p>
	 * The method itself is responsible for creating the appropriate 'call frame'
	 * or activation record in which to define temporary variables and parameter bindings.
	 * 
	 * @param arguments the actual arguments, already eagerly evaluated.
	 * @param ctx the context in which to evaluate the method body, call frame not yet inserted.
	 * @return the value of evaluating the method body in a context derived from the given context.
	 */
	public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException;
	
	/**
	 * Applies the method to the given arguments in the given context.
	 * The context is usually supplied by a closure and is necessary in order to
	 * pair a method with its current receiver (its 'self').
	 * <p>
	 * The method will use the given context 'as-is', and will *not* insert an additional call frame.
	 * 
	 * @param arguments the actual arguments, already eagerly evaluated.
	 * @param ctx the context in which to evaluate the method body, to be used without inserting a call frame.
	 * @return the value of evaluating the method body in the given context.
	 */
	public ATObject base_applyInScope(ATTable arguments, ATContext ctx) throws InterpreterException;
	
	/**
	 * Returns the name of the method. 
	 * <p>
	 * Note that all methods (defined using <code>def name( ...args... ) { ... }</code> or <code>def foo: arg bar: arg { ... }</code>) 
	 * retain the name with which they were first bound. Literal blocks which may be created
	 * outside of a definition are implicitly named 'lambda'.
	 * 
	 * @return an {@link ATSymbol} representing the name by which the method can be identified.
	 */
	public ATSymbol base_name() throws InterpreterException;
	
	/**
	 * Returns the parameter list of the method which is normally a table of symbols.
	 * 
	 * @return an {@link ATTable} representing the parameter list of the method.
	 */
	public ATTable base_parameters() throws InterpreterException;
	
	/**
	 * Returns the body of the method.
	 * 
	 * @return an {@link ATBegin} representing the body of the method.
	 */
	public ATBegin base_bodyExpression() throws InterpreterException;
}
