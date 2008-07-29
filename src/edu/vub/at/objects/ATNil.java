/**
 * AmbientTalk/2 Project
 * ATNil.java created on 26-jul-2006 at 12:05:01
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
import edu.vub.at.objects.grammar.ATExpression;

/**
 * The methods understood by the native <tt>nil</tt> object.
 * All other objects in the system delegate to <tt>nil</tt> eventually. Hence,
 * <tt>nil</tt>'s methods may be invoked on all objects in the system.
 * 
 * @author tvcutsem
 */
public interface ATNil extends ATObject {

	/**
	 * The <tt>!=</tt> operator which returns <tt>false</tt> only if
	 * the passed parameter equals <tt>nil</tt>.
	 */
	public ATBoolean base__opnot__opeql_(ATObject other) throws InterpreterException;
	
    /**
     * The identity operator. In AmbientTalk, equality of objects
     * is by default pointer-equality (i.e. objects are equal only
     * if they are identical).
     * 
     * @return by default, true if the parameter object and this object are identical,
     * false otherwise.
     */
    public ATBoolean base__opeql__opeql_(ATObject other) throws InterpreterException;
    
    /**
     * This method is invoked when a new instance of the object is created
     * using {@link this#base_new(ATObject[])}. The default implementation
     * does nothing, but can be overridden to re-initialize the cloned
     * instance.
     */
    public ATObject base_init(ATObject[] initargs) throws InterpreterException;
    
    /**
     * Creates a new instance (a clone) of the receiver and initialises it
     * by invoking its {@link this#base_init(ATObject[])} method.
     */
    public ATObject base_new(ATObject[] initargs) throws InterpreterException;
}
