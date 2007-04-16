/**
 * AmbientTalk/2 Project
 * ATAssignField.java created on 17-aug-2006 at 13:43:25
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

/**
 * The public interface to a field assignment AG element.
 * 
 * <p>
 * Example: <code>o.x := 5</code> where <code>x</code> is a literal symbol
 * </p>
 * 
 * @author tvc
 */
public interface ATAssignField extends ATAssignment {
	
	/**
	 * The receiver expression may be any valid AmbientTalk expression.
	 * Example: <code>`{ o.x := 5}.statements[1].receiverExpression == `o</code>
	 * @return The expression for the object whose field is about to be assigned
	 */
	public ATExpression base_getReceiverExpression();
	
	/**
	 * The field name must be a literal symbol
	 * Example: <code>`{ o.x := 5}.statements[1].fieldName == `x</code>
	 * @return The name of the field
	 */
	public ATSymbol base_getFieldName();
	
	/**
	 * The value expression may be any valid AmbientTalk expression.
	 * Example: <code>`{ o.x := 5}.statements[1].valueExpression == `5</code>
	 * @return The expression for the value that will be assigned to the field
	 */
	public ATExpression base_getValueExpression();
	
}
