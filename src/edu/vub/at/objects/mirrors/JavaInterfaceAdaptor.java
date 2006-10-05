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
import edu.vub.at.exceptions.XIllegalArgument;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XReflectionFailure;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.objects.ATObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Vector;

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
	
	public static final String _MAGIC_PREFIX_ = "magic_";
	public static final String _MAGET_PREFIX_ = "magic_get";
	public static final String _MASET_PREFIX_ = "magic_set";
	
	
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
	 * Invokes a method on a Java object identified by a selector.
	 * 
	 * @param jClass the class of the receiver object
	 * @param jReceiver the receiver (normally an object from the AT hierarchy)
	 * @param jSelector the java-level selector identifying the method to invoke
	 * @param jArguments parameters, normally AT objects
	 * @return the return value of the reflectively invoked method
	 */	
	public static Object invokeJavaMethod (Class jClass, Object jReceiver,
										 String jSelector, Object[] jArguments) throws NATException {
		Method[] applicable = getMethodsForSelector(jClass, jSelector);
		switch(applicable.length) {
		  case 0:
		  	throw new XSelectorNotFound(Reflection.downBaseLevelSelector(jSelector), (ATObject)jReceiver);
		  case 1:
			return invokeJavaMethod(applicable[0], jReceiver, jArguments);
		  default:
			throw new XIllegalOperation("Dynamic dispatching on overloaded methods not yet implemented");
		}
	}
	
	/**
	 * Invokes a method on a Java object identified by a java.lang.reflect.Method object.
	 * Note that if the method to invoke reflectively has a formal parameter list consisting
	 * of one argument of type ATObject[], then the arguments are wrapped in an array such that
	 * the function actually takes a variable number of arguments.
	 * 
	 * @param javaMethod the Java method to invoke
	 * @param jReceiver the Java object representing the receiver (normally an AT object)
	 * @param jArguments the AT arguments to pass
	 * @return the return value of the reflectively invoked method
	 */
	public static Object invokeJavaMethod(Method javaMethod, Object jReceiver, Object[] jArguments) throws NATException {
         // TODO: will have to convert some NATObjects to proper ATXXX argument interfaces using mirages!
		try {
			// if the native method takes an array as its sole parameter, it is interpreted as taking
			// a variable number of ambienttalk arguments
			Class[] params = javaMethod.getParameterTypes();
			
			if ((params.length == 1) && params[0].equals(ATObject[].class)) {
				return javaMethod.invoke(jReceiver, new Object[] { (ATObject[]) jArguments });
			} else {
				return javaMethod.invoke(jReceiver, jArguments);
			}
		} catch (IllegalAccessException e) {
			// the invoked method is not publicly accessible
			throw new XReflectionFailure("Native method "+javaMethod.getName() + " not accessible.", e);
		} catch (IllegalArgumentException e) {
			// illegal argument types were supplied
			throw new XIllegalArgument("Illegal argument for native method "+Reflection.downSelector(javaMethod.getName()) + ": " + e.getMessage(), e);
		} catch (InvocationTargetException e) {
			// the invoked method threw an exception
			if (e.getCause() instanceof NATException)
				throw (NATException) e.getCause();
			else
				throw new XReflectionFailure("Native method "+Reflection.downSelector(javaMethod.getName())+" threw internal exception", e);
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
		JavaMethod method = getMethod(baseInterface, receiver, methodName);
	    return new JavaClosure(receiver, method);
	}
	
	public static JavaMethod getMethod(
			Class baseInterface, 
			ATObject receiver,
			String methodName) throws NATException {
		Method[] applicable = getMethodsForSelector(baseInterface, methodName);
		switch (applicable.length) {
			case 0:
				throw new XSelectorNotFound(Reflection.downBaseLevelSelector(methodName), receiver);
			case 1:
				return new JavaMethod(applicable[0]);
			default:
				// TODO return new JavaMethod.Dispatched(receiver, applicable);
				throw new XIllegalOperation("A native method uses overloading!");
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
}
