/**
 * AmbientTalk/2 Project
 * ATDefExternalMethod.java created on 15-nov-2006 at 19:23:51
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

import edu.vub.at.objects.ATTable;

/**
 * @author tvc
 *
 * The public interface to an external method definition abstract grammar element.
 * Created when evaluating <tt>def o.m() { body }</tt>
 * 
 * There are three ways to evaluate such expressions with respect to what context should be
 * active when the method is invoked upon the object:
 *  1) The newly defined method is wrapped in a closure thereby fixing the lexical scope that was
 *     active at method definition/addition time, as well as the values of 'self' and 'super'.
 *     This has the advantage that within the externally added method body:
 *      a) the adding object can still access its lexical scope.
 *      b) the adding object can *not* access the scope of the object it is adding a method to.
 *     This has the disadvantage that within the externally added method body,
 *     'self' and 'super' keep on referring to the lexically active self and super. This has the
 *     disadvantage that the newly added method cannot perform 'super sends', nor is the value of
 *     'self' correct when the method is invoked through a child of o.
 *  2) The newly defined method is added as-is to the object. This has roughly the reverse
 *     effects of option 1: the adding object loses its lexical scope (error-prone and dangerous!)
 *     but the values for self and super are correct. It is as if the method was really defined
 *     in the scope of the original object.
 *  3) The newly defined method is wrapped in a special 'objectless' closure that captures the
 *     lexical scope of the adding object but *not* the values of 'self' and 'super'. When the
 *     added method is subsequently invoked, its lexical scope will still be that of the adding
 *     object (hence, having the benefits of option 1) while the values of 'self' and 'super'
 *     will be correct with respect to the method invocation on o (or a child of o) (hence,
 *     having the benefits of option 2).
 *     
 * AmbientTalk implements the third option, by means of a NATClosureMethod.
 */
public interface ATDefExternalMethod extends ATDefinition {

	public ATSymbol base_getReceiver();
	public ATSymbol base_getSelector();
	public ATTable base_getArguments();
	public ATBegin base_getBodyExpression();
	
}
