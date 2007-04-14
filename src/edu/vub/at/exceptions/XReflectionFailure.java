/**
 * AmbientTalk/2 Project
 * XReflectionFailure.java created on Jul 13, 2006 at 8:32:03 PM
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
package edu.vub.at.exceptions;

import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.coercion.NativeStripes;

/**
 * An instance of the class XReflectionFailure is thrown when something goes wrong when reflectively
 * invoking natively implemented methods from AmbientTalk. This mechanism is used to call base-level
 * methods on native types such as tables and numbers. The default mirror architecture relies on 
 * this feature as well to call the meta_operations on these values. Finally, the invocation of Java
 * methods also uses this mechanism in part. The circumstances in which an XReflectionFailure 
 * exception can be raised are the following:  
 * 
 * <ul>
 *   <li>when attempting to access a Java method (both natively implemented AmbientTalk methods 
 *   and symbiotic Java methods) which is not accessible due to visibility constraints</li>
 *   <li>when calling a natively implemented AmbientTalk method which throws an exception which
 *   is not either an InterpreterException or a Signal.</li>
 *   <li>when a natively implemented AmbientTalk method returns an object which is not a subtype
 *   of ATObject, and it is not a Java native type (detected in primitiveJavaToATObject)</li>
 *   <li>when reading a field from a Java object which is not present or visible</li>
 * </ul>
 * 
 * @author smostinc
 */
public class XReflectionFailure extends InterpreterException {

	private static final long serialVersionUID = 4082945147643295718L;

	/**
	 * Constructor used to report failed reflective invocations which are due to underlying 
	 * Java exceptions reporting e.g. the lack of sufficient access rights to invoke a method.
	 * @param message details the error and the selector being invoked or selected
	 * @param cause underlying exception (IllegalAccessException or InvocationTargetException)
	 */
	public XReflectionFailure(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Constructor used to report failed reflective invocations which are not directly cause by
	 * Java exceptions.
	 * @param message details the precise error. 
	 */
	public XReflectionFailure(String message) {
		super(message);
	}
	
	public ATStripe getStripeType() {
		return NativeStripes._REFLECTIONFAILURE_;
	}

}
