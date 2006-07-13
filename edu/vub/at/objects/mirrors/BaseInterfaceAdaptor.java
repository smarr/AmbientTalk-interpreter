/**
 * AmbientTalk/2 Project
 * BaseInterfaceAdaptor.java created on Jul 13, 2006 at 10:25:01 PM
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
package edu.vub.at.objects.mirrors;

import edu.vub.at.exceptions.TypeException;
import edu.vub.at.objects.ATArray;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATSymbol;

import java.lang.reflect.Method;

/**
 * @author smostinc
 *
 * BaseInterfaceAdaptor is a class which provides some static methods to deify method
 * calls from the ambienttalk level to the java level. Its use is twofold: it is used
 * by mirrors (which allow viewing a java-level object as an ambienttalk mirror) and
 * it allows sending messages to ambienttalk objects whose implementation is provided
 * by their java representation objects.
 */
public class BaseInterfaceAdaptor {

	public static Object deifyInvocation (
			Class baseInterface, ATObject receiver,
			ATSymbol methodName, ATArray arguments) 
			throws TypeException {

		Object[] values	= new Object[arguments.size()];
		Class[]  types	= new Class[arguments.size()];
		
		for(int i = 0; i < arguments.size(); i++) {
			ATObject argument = arguments.at(i+1);
			values[i] 	= argument;
			types[i] 	= argument.getClass();
		};
		
		try {
			Method deified = baseInterface.getMethod(methodName.toString(), types);
			return deified.invoke(receiver, values);
		} catch (Exception e) {
			// Exceptions during method invocation imply that the requested method was
			// not found in the interface. Hence a TypeException is thrown to signal 
			// that the object could not respond to the request.
			throw new TypeException(
				"Could not invoke method with selector" + methodName.toString() + " on the given object.",
				e, receiver);
		}
	}
}
