/**
 * AmbientTalk/2 Project
 * ATApplication.java created on 26-jul-2006 at 15:00:53
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
package edu.vub.at.objects.grammar;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;

/**
 * The public interface to an application AG element.
 * 
 * <p>
 * Example: <code>f(a, b, c)</code> where <code>f</code> is an expression that
 * has to evaluate to a closure.
 * </p><p>
 * It is allowed to use splicing in the argument list, for example <code>f(a, @[b, c])</code>
 * or <code>f(a, @t)</code>.
 * </p><p>
 * Note that <code>o.f(a, b, c)</code> is not represented with this interface,
 * but rather with the combination of an {@link ATMessageSend} and an {@link ATMethodInvocationCreation}.
 * </p>
 * 
 * @author tvc
 */
public interface ATApplication extends ATExpression {

	/**
	 * The function may be any AmbientTalk expression that evaluates to a closure.
	 * Example: <code>`(f(1)(2)).function == `f(1)</code>
	 * 
	 * @return the function expression of the application
	 */
	public ATExpression base_getFunction();
	
	/**
	 * Zero, one or more arguments may be passed. It is allowed to use splicing
	 * in the argument list, for example <code>f(a, @[b, c])</code> or <code>f(a, @t)</code>.
	 * Example: <code>`(f(1, 2, @[3, 4])).arguments == `[1, 2, @[3, 4]]</code>
	 * @return the argument list of the application
	 */
	public ATTable base_getArguments();
	
}
