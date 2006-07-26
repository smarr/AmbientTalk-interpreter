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
 * can be encountered at the ambienttalk base level. Since closures encapsulate a
 * context, they can be readily be applied. 
 */
public interface ATClosure extends ATNil {

	/**
	 * Applies a closure to the given arguments
	 * @param arguments- the arguments have been evaluated prior to the application.
	 * @return the value returned by the closure application.
	 */
	public ATObject meta_apply(ATTable arguments) throws NATException;
	
	/**
	 * Structural access to the encapsulated method. 
	 */
	public ATMethod getMethod();

	/**
	 * Structural access to the scope of the closure.
	 */
	public ATContext getContext();
}
