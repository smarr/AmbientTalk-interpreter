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


/**
 * The public interface to a native AmbientTalk closure (a piece of code + enclosing environment).
 * 
 * Since ATMethods are always wrapped either at creation time (blocks) or during 
 * lookup (methods), ATClosures are by definition the only way methods and blocks 
 * can be encountered at the ambienttalk base level. Closures should respond to the
 * meta_apply method, which should trigger the invocation of their encapsulating method in the
 * enclosed closure context.
 * 
 * @author smostinc
 */
public interface ATClosure extends ATObject {
	
	/**
	 * Structural access to the encapsulated method. 
	 */
	public ATMethod base_getMethod() throws InterpreterException;

	/**
	 * Structural access to the scope of the closure.
	 */
	public ATContext base_getContext() throws InterpreterException;
	
	/**
	 * Applies the closure to the given arguments, already wrapped in a table
	 * @param args the already evaluated arguments, wrapped in a table
	 * @return the value of evaluating the method body in the context of the closure
	 */
	public ATObject base_apply(ATTable args) throws InterpreterException;
	
	/**
	 * Allows AmbientTalk programmers to write
	 * { boolean }.whileTrue: { body }
	 * which will execute body as long as the boolean condition evaluates to true.
	 */
	public ATObject base_whileTrue_(ATClosure body) throws InterpreterException;
	
	/**
	 * { |quit| ... quit(val) ... }.escape()
	 * 
	 * The escape control construct passes to its receiver block a function which
	 * when invoked, immediately transfers control back to the caller of escape,
	 * returning the value passed to quit.
	 * 
	 * If no value is passed to quit, nil is returned instead.
	 * 
	 * If quit is not invoked during the execution of the receiver block,
	 * the block terminates normally, with its normal return value.
	 *   
	 * If quit is invoked at the point where the call to escape has already returned normally,
	 * invoking quit will raise an XIllegalOperation exception.
	 */
	public ATObject base_escape() throws InterpreterException;
	
}
