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

import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.JavaMethod.Simple;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.lang.reflect.Method;
import java.security.spec.InvalidParameterSpecException;
import java.util.Vector;

/**
 * @author smostinc
 *
 * BaseInterfaceAdaptor is a class which provides some static methods to deify method
 * calls from the ambienttalk level to the java level. Its use is twofold: it is used
 * by mirrors (which allow viewing a java-level object as an ambienttalk mirror) and
 * it allows sending messages to ambienttalk objects whose implementation is provided
 * by their java representation objects.
 * 
 * This class also encapsulates static methods for manually implementing dynamic 
 * dispatch over Java types. This is needed since whenever an invocation is made on
 * an ambienttalk object we cannot foresee the expected java types. We can use this 
 * technique to our advantage by using overloading on typically double dispatch 
 * methods such as plus.
 */
public class BaseInterfaceAdaptor {

	public static String transformSelector(String addPrefix, String removePrefix, String selector) {
		return addPrefix + selector.replaceFirst(removePrefix,"").replace(':', '_');
	}
	
	private static class ObjectClassArray {
		
		private final Object[] values_;
		private final Class[] types_;
		
		private ObjectClassArray(ATTable arguments) throws NATException {

			int numberOfArguments = arguments.getLength().asNativeNumber().javaValue;
			
			values_	= new Object[numberOfArguments];
			types_	= new Class[numberOfArguments];
			
			for(int i = 0; i < numberOfArguments; i++) {
				ATObject argument = arguments.at(NATNumber.atValue(i+1));
				values_[i] 	= argument;
				types_[i] 	= argument.getClass();
			};

		}
	}
	
	private static Method[] getMethodsForSelector(Class baseInterface, String selector) {
		Method[] allMethods = baseInterface.getMethods();
		
		Vector matchingMethods = new Vector();
		int numMatchingMethods = 0;
		
		for (int i = 0; i < allMethods.length; i++) {
			if (allMethods[i].getName().equals(selector)) {
				matchingMethods.addElement(allMethods[i]);
				numMatchingMethods++;
			}
		}
		
		return (Method[])matchingMethods.toArray(new Method[numMatchingMethods]);
	}
		
	public static Object deifyInvocation (
			Class baseInterface, ATObject receiver,
			String methodName, ATTable arguments) 
			throws NATException {
		
		try {
			Method[] applicable = getMethodsForSelector(baseInterface, methodName);
			switch(applicable.length) {
				case 0:
					throw new XSelectorNotFound(AGSymbol.alloc(methodName), receiver);
				case 1:
					return applicable[0].invoke(receiver, arguments.asNativeTable().elements_);
				default:
					throw new XIllegalOperation("Dynamic dispatching on overloaded methods not yet implemented");
			}
		} catch (Exception e) {
			// Exceptions during method invocation imply that the requested method was
			// not found in the interface. Hence a XTypeMismatch is thrown to signal 
			// that the object could not respond to the request.
			e.printStackTrace();
			throw new XTypeMismatch(
				"Could not invoke method with selector " + methodName.toString() + " on the given object.",
				e, receiver);
		}
	}

	public static boolean hasApplicableMethod (
			Class baseInterface, 
			ATObject receiver,
			ATSymbol methodName) {

		return (getMethodsForSelector(
				baseInterface, methodName.toString()).length != 0);
	}
	
	public static JavaMethod wrapMethodFor(
			Class baseInterface, 
			ATObject receiver,
			ATSymbol methodName) throws NATException {
		Method[] applicable = getMethodsForSelector(baseInterface, methodName.toString());
		switch (applicable.length) {
			case 0:
				throw new XSelectorNotFound(methodName, receiver);
			case 1:
				return new JavaMethod.Simple(receiver, applicable[0]);
			default:
				// TODO return new JavaMethod.Dispatched(receiver, applicable);
				throw new XIllegalOperation("Java Method Wrappers not yet implemented");
		}
	}
}
