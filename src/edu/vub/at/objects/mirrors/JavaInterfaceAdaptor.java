/**
 * AmbientTalk/2 Project
 * JavaInterfaceAdaptor.java created on Jul 13, 2006 at 10:25:01 PM
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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Vector;

import junit.framework.AssertionFailedError;

/**
 * @author smostinc
 *
 * JavaInterfaceAdaptor is a class providing several static methods which allow 
 * accessing and invoking Java methods as if they were in fact AmbientTalk value.
 * It is used by the Reflection class to up ambienttalk invocations and field 
 * accesses and translate them using java reflection. 
 * 
 * This class also encapsulates static methods for manually implementing dynamic 
 * dispatch over Java types. This is needed since whenever an invocation is made on
 * an ambienttalk object we cannot foresee the expected java types. We can use this 
 * technique to our advantage by using overloading on typically double dispatch 
 * methods such as plus.
 * 
 */
public class JavaInterfaceAdaptor {
		
	public static final String _BASE_PREFIX_ = "base_";
	public static final String _BGET_PREFIX_ = "base_get";
	public static final String _BSET_PREFIX_ = "base_set";
	
	public static final String _META_PREFIX_ = "meta_";
	public static final String _MGET_PREFIX_ = "meta_get";
	public static final String _MSET_PREFIX_ = "meta_set";
	
	
	/**
	 * Tests given a class, whether the class either declares or inherits a method
	 * for a given selector. 
	 * @param jClass - a Java class, representing an AT object.
	 * @param selector - a selector, describing the method to be searched for.
	 * @return
	 */
	public static boolean hasApplicableJavaMethod (
			Class jClass, 
			String jSelector) {

		return (getMethodsForSelector(jClass, jSelector).length != 0);
	}
	
	/**
	 * Invokes a method on a Java object   
	 * @param jClass
	 * @param jReceiver
	 * @param jSelector
	 * @param jArguments
	 * @return
	 * @throws NATException
	 */	
	public static Object invokeJavaMethod (
			Class jClass, Object jReceiver,
			String jSelector, Object[] jArguments) 
			throws NATException {
		
		try {
			Method[] applicable = getMethodsForSelector(jClass, jSelector);
			switch(applicable.length) {
				case 0:
					throw new XSelectorNotFound(AGSymbol.alloc(jSelector), (ATObject)jReceiver);
				case 1: // TODO: will have to convert some NATObjects to proper ATXXX argument interfaces using mirages!
					return applicable[0].invoke(jReceiver, jArguments);
				default:
					throw new XIllegalOperation("Dynamic dispatching on overloaded methods not yet implemented");
			}
		} catch (NATException nate) {
			// NATExceptions raised mean that the requested method is either overloaded or
			// does not exist. These are ambienttalk exceptions and may be safely rethrown.
			throw nate;
		} catch (InvocationTargetException ite) { // XXX for the purpose of supporting junit tests
			// An InvocationTargetException may be thrown when a junit components fails
			// therefore we check the cause to propagate tyhe correct exception.
			if(ite.getCause() instanceof AssertionFailedError) {
				throw (AssertionFailedError)ite.getCause();
			} else {
				throw new XTypeMismatch(
						"Could not invoke method with selector " + jSelector.toString() + " on the given object.",
						ite, (ATObject)jReceiver);				
			}
		} catch (Exception e) {
			// Exceptions during method invocation imply that the requested method was
			// not found in the interface. Hence a XTypeMismatch is thrown to signal 
			// that the object could not respond to the request.
			// e.printStackTrace();
			throw new XTypeMismatch(
				"Could not invoke method with selector " + jSelector.toString() + " on the given object.",
				e, (ATObject)jReceiver);
		}
	}
	
	/**
	 * Try to create a new instance of a Java class given an array of initialization arguments.
	 * Because we do not have exact typing information, all of the public constructors of the
	 * class are traversed until one is found that can create new instances given the current
	 * initargs.
	 */
	public static Object createClassInstance(Class jClass, Object[] jInitArgs) throws NATException {
		Constructor[] ctors = jClass.getConstructors();
		for (int i = 0; i < ctors.length; i++) {
			Constructor ctor = ctors[i];
			if (ctor.getParameterTypes().length == jInitArgs.length) {
				try {
					return ctor.newInstance(jInitArgs);
				} catch (IllegalArgumentException e) {
					continue; // argument types don't match, may find other constructor
				} catch (InstantiationException e) {
					break; // class is an abstract class, won't find a match
				} catch (IllegalAccessException e) {
					continue; // private or protected constructor, may find another one
				} catch (InvocationTargetException e) {
					// an exception was raised by the constructor
					if (e.getCause() instanceof NATException)
						throw ((NATException) e.getCause());
					else // fatal exception
						throw new XIllegalOperation("Instance creation of type " + jClass.getName() + " failed: " + e.getMessage());
				}
			} else {
				// arity does not match, try finding another one
				continue;
			}
		}
		// no matching constructors were found
		throw new XIllegalOperation("Unable to create a new instance of type " + jClass.getName());
	}

	public static JavaClosure wrapMethodFor(
			Class baseInterface, 
			ATObject receiver,
			String methodName) throws NATException {
		Method[] applicable = getMethodsForSelector(baseInterface, methodName);
		switch (applicable.length) {
			case 0:
				throw new XSelectorNotFound(AGSymbol.alloc(methodName), receiver);
			case 1:
				return new JavaClosure(receiver, new JavaMethod(applicable[0]));
			default:
				// TODO return new JavaMethod.Dispatched(receiver, applicable);
				throw new XIllegalOperation("Java Method Wrappers not yet implemented");
		}
	}
	
	/**
	 * Since Java uses strict matching when asked for a method, given an array of 
	 * classes, this often means that the types are overspecified and therefore no
	 * matches can be found. As a consequence we have our own mechanism to select
	 * which set of methods is applicable given a selector. Further dispatch needs
	 * only to be performed when more than a single match exists.
	 * @param jClass - the class from which the methods will be selected.
	 * @param selector - the name of the requested method.
	 * @return
	 */
	private static Method[] getMethodsForSelector(Class jClass, String selector) {
		Method[] allMethods = jClass.getMethods();
		
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
	
	public static String transformField(
			String addPrefix, String removePrefix, 
			String selector, boolean toUpper) {
		char[] charArray = selector.replaceFirst(removePrefix,"").toCharArray();
		if(toUpper) {
			charArray[0] = Character.toUpperCase(charArray[0]);
		} else {
			charArray[0] = Character.toLowerCase(charArray[0]);			
		}
		
		selector = new String(charArray);
		return addPrefix + selector;
	}

}
