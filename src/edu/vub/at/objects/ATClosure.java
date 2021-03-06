/**
 * AmbientTalk/2 Project
 * ATClosure.java created on Jul 23, 2006 at 11:52:10 AM
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
import edu.vub.at.exceptions.XIllegalOperation;


/**
 * ATClosure is the public interface to a native AmbientTalk closure (a method + enclosing environment).
 * <p>
 * Since {@link ATMethod}s are always wrapped either at creation time (blocks) or during 
 * lookup (methods), ATClosures are by definition the only way methods and blocks 
 * can be encountered at the AmbientTalk base level. 
 * <p>
 * Closures should respond to the base_apply method, which should trigger the invocation of their encapsulating method in the
 * enclosed closure context.
 * <p>
 * Closures are sometimes also 'abused' to simply represent blocks of source code whose
 * body has to be evaluated not in the enclosed lexical context, but within the context
 * of another object. To facilitate such use, a closure provides the method 'base_applyInScope'
 * which will execute the enclosed method in the scope of the given object, rather than
 * in the enclosed lexical context.
 * 
 * @author smostinc
 * @author tvcutsem
 */
public interface ATClosure extends ATObject {
	
	/**
	 * Returns the encapsulated method. 
	 * 
	 * @return an {@link ATMethod} that returns the encapsulated method.
	 */
	public ATMethod base_method() throws InterpreterException;

	/**
	 * Returns the scope of the closure.
	 * 
	 * @return an {@link ATMethod} that returns the scope of the closure.
	 */
	public ATContext base_context() throws InterpreterException;
	
	/**
	 * Applies the closure to the given arguments, already wrapped in a table.
	 * The enclosed method is executed in the context provided by the closure.
	 * 
	 * @param args the already evaluated arguments, wrapped in a table.
	 * @return the value of evaluating the method body in the context of the closure.
	 */
	public ATObject base_apply(ATTable args) throws InterpreterException;
	
	/**
	 * Applies the closure to the given arguments, already wrapped in a table.
	 * The enclosed method is executed in the context of the given object. The
	 * enclosed closure context is disregarded.
	 * <p>
	 * The context provided by an object is always equal to:
	 * <tt>ctx(cur=object,self=object,super=object.dynamicParent)</tt>
	 * 
	 * @param args the already evaluated arguments, wrapped in a table.
	 * @param scope the object that will act as self and as lexically enclosing scope.
	 * @return the value of evaluating the method body in the context of the given object scope.
	 */
	public ATObject base_applyInScope(ATTable args, ATObject scope) throws InterpreterException;
	
	/**
	 * Allows AmbientTalk programmers to write
	 * <code>{ booleanCondition }.whileTrue: { body }</code>
	 * which will execute body as long as the boolean condition evaluates to true.
	 * <p>
	 * More specifically, what the native implementation (expressed in AmbientTalk syntax) does is:
	 * <p>
	 * <code>
	 * def whileTrue: body {
	 *   self.apply().ifTrue: {
	 *     body();
	 *     self.whileTrue: body
	 *   }
	 * }
	 * </code>
	 * 	 
	 * @param body the block of code that will be executed as long as the boolean condition evaluates to true.
	 * @return the value of the last closure applied. 
	 * @throws InterpreterException if raised inside the code closure.
	 */
	public ATObject base_whileTrue_(ATClosure body) throws InterpreterException;
	
	/**
	 * Escape control construct <code>{ |quit| ... quit(val) ... }.escape()</code>
	 * <p>
	 * The escape control construct passes to its receiver block a function which
	 * when invoked, immediately transfers control back to the caller of escape,
	 * returning the value passed to quit.
	 * <p>
	 * If no value is passed to quit, nil is returned instead.
	 * <p>
	 * If quit is not invoked during the execution of the receiver block,
	 * the block terminates normally, with its normal return value.
	 * <p>  
	 * If quit is invoked at the point where the call to escape has already returned,
	 * either normally or via an exception or another escape call,
	 * invoking quit will raise a {@link XIllegalOperation} exception.
	 * 
	 * @return the value passed to quit.
	 */
	public ATObject base_escape() throws InterpreterException;
	
}
