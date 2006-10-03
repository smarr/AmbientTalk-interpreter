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

import edu.vub.at.exceptions.NATException;


/**
 * @author smostinc
 *
 * The public interface to a native AmbientTalk closure (a piece of code + enclosing environment).
 * 
 * Since ATMethods are always wrapped either at creation time (blocks) or during 
 * lookup (methods), ATClosures are by definition the only way methods and blocks 
 * can be encountered at the ambienttalk base level. Closures should respond to the
 * meta_apply method, which should trigger the invocation of their encapsulating method in the
 * enclosed closure context.
 */
public interface ATClosure extends ATObject {
	
	/**
	 * Structural access to the encapsulated method. 
	 */
	public ATMethod base_getMethod();

	/**
	 * Structural access to the scope of the closure.
	 */
	public ATContext base_getContext();
	
	/**
	 * Applies the closure to the given arguments, already wrapped in a table
	 * @param args the already evaluated arguments, wrapped in a table
	 * @return the value of evaluating the method body in the context of the closure
	 */
	public ATObject base_apply(ATTable args) throws NATException;
	
	/**
	 * Allows AmbientTalk programmers to write
	 * { body }.whileTrue: { boolean }
	 * which will execute body as long as the boolean condition evaluates to true.
	 */
	public ATObject base_whileTrue_(ATClosure condition) throws NATException;
	
}
