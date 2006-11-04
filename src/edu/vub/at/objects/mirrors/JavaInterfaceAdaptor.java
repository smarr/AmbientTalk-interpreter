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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XIllegalArgument;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XReflectionFailure;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.exceptions.XUndefinedField;
import edu.vub.at.exceptions.signals.Signal;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.coercion.Coercer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Vector;

/**
 * JavaInterfaceAdaptor is a class providing several static methods which allow 
 * accessing and invoking Java methods which represent native AmbientTalk methods.
 * It is used by the Reflection class to up ambienttalk invocations and field 
 * accesses and translate them using java reflection. 
 * 
 * @author tvcutsem
 * @author smostinc
 */
public class JavaInterfaceAdaptor {
		
	/**
	 * Tests given a class, whether the class either declares or inherits a method
	 * for a given selector. 
	 * @param jClass - a Java class, representing an AT object.
	 * @param jSelector - a selector, describing the method to be searched for.
	 * @return whether a methods with a matching selector can be found
	 */
	public static boolean hasApplicableJavaMethod(Class jClass, String jSelector) {
		return (getMethodsForSelector(jClass, jSelector).length != 0);
	}
	
	/**
	 * Invokes a method on a Java object identified by a selector.
	 * 
	 * @param jClass the class of the receiver object
	 * @param natReceiver the receiver (a native AmbientTalk object)
	 * @param jSelector the java-level selector identifying the method to invoke
	 * @param jArguments parameters, normally AT objects
	 * @return the return value of the reflectively invoked method
	 */	
	public static Object invokeJavaMethod(Class jClass, ATObject natReceiver,
										String jSelector, Object[] jArguments) throws InterpreterException {
		return invokeJavaMethod(getMethod(jClass, natReceiver, jSelector), natReceiver, jArguments);
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
	public static Object invokeJavaMethod(Method javaMethod, Object jReceiver, Object[] jArguments) throws InterpreterException {
		try {
			// if the native method takes an array as its sole parameter, it is interpreted as taking
			// a variable number of ambienttalk arguments
			Class[] params = javaMethod.getParameterTypes();
			
			if ((params.length == 1) && params[0].equals(ATObject[].class)) {
				return javaMethod.invoke(jReceiver, new Object[] { (ATObject[]) jArguments });
			} else {
				if (params.length != jArguments.length) {
					throw new XArityMismatch("native method "+Reflection.downSelector(javaMethod.getName()), params.length, jArguments.length);
				}
				// make sure to properly 'coerce' each argument into the proper AT interface type
				Object[] coercedArgs = coerceArguments(jArguments, params);
				return javaMethod.invoke(jReceiver, coercedArgs);
			}
		} catch (IllegalAccessException e) {
			// the invoked method is not publicly accessible
			throw new XReflectionFailure("Native method "+Reflection.downSelector(javaMethod.getName()) + " not accessible.", e);
		} catch (IllegalArgumentException e) {
			// illegal argument types were supplied
			throw new XIllegalArgument("Illegal argument for native method "+Reflection.downSelector(javaMethod.getName()) + ": " + e.getMessage(), e);
		} catch (InvocationTargetException e) {
			// the invoked method threw an exception
			if (e.getCause() instanceof InterpreterException)
				throw (InterpreterException) e.getCause();
			else if (e.getCause() instanceof Signal) {
			    throw (Signal) e.getCause();	
			} else {
				e.printStackTrace();
				throw new XReflectionFailure("Native method "+Reflection.downSelector(javaMethod.getName())+" threw internal exception", e.getCause());
		    }
		}
	}
	
	/**
	 * Try to create a new instance of a Java class given an array of initialization arguments.
	 * Because we do not have exact typing information, all of the public constructors of the
	 * class are traversed until one is found that can create new instances given the current
	 * initargs.
	 */
	public static Object createClassInstance(Class jClass, Object[] jInitArgs) throws InterpreterException {
		Constructor[] ctors = jClass.getConstructors();
		for (int i = 0; i < ctors.length; i++) {
			Constructor ctor = ctors[i];
			if (ctor.getParameterTypes().length == jInitArgs.length) {
				try {
					// make sure to properly 'coerce' each argument into the proper AT interface type
					Object[] coercedInitArgs = coerceArguments(jInitArgs, ctor.getParameterTypes());
					return ctor.newInstance(coercedInitArgs);
				} catch (IllegalArgumentException e) {
					continue; // argument types don't match, may find other constructor
				} catch (InstantiationException e) {
					break; // class is an abstract class, won't find a match
				} catch (IllegalAccessException e) {
					continue; // private or protected constructor, may find another one
				} catch (InvocationTargetException e) {
					// an exception was raised by the constructor
					if (e.getCause() instanceof InterpreterException)
						throw ((InterpreterException) e.getCause());
					else if (e.getCause() instanceof Signal) {
					    throw (Signal) e.getCause();	
					} else // fatal exception
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
	
	public static Method getMethod(
			Class baseInterface, 
			ATObject receiver,
			String methodName) throws InterpreterException {
		Method[] applicable = getMethodsForSelector(baseInterface, methodName);
		switch (applicable.length) {
			case 0:
				throw new XSelectorNotFound(Reflection.downBaseLevelSelector(methodName), receiver);
			case 1:
				return applicable[0];
			default:
				throw new XIllegalOperation("Native method uses overloading: " + methodName + " in " + baseInterface);
		}
	}
	
	/**
	 * Read a field from the given Java object reflectively.
	 */
	public static Object readField(Object fromObject, String fieldName, boolean isStatic)
	                     throws XUndefinedField, XReflectionFailure {
		try {
			Field f = getField(fromObject, fieldName, isStatic);
			return f.get(fromObject);
		} catch (IllegalArgumentException e) {
			// the given object is of the wrong class, should not happen!
			throw new XReflectionFailure("Illegal class for field access of "+fieldName + ": " + e.getMessage());
		} catch (IllegalAccessException e) {
             // the read field is not publicly accessible
			throw new XReflectionFailure("field access of " + fieldName + " not accessible.");
		}
	}
	
	/**
	 * Write a field in the given Java object reflectively.
	 */
	public static void writeField(Object toObject, String fieldName, Object value, boolean isStatic)
	                              throws XUndefinedField, XReflectionFailure {
		try {
			Field f = getField(toObject, fieldName, isStatic);
			f.set(toObject, value);
		} catch (IllegalArgumentException e) {
			// the given value is of the wrong type
			throw new XReflectionFailure("Illegal value for field "+fieldName + ": " + e.getMessage());
		} catch (IllegalAccessException e) {
             // the read field is not publicly accessible
			throw new XReflectionFailure("field assignment of " + fieldName + " not accessible.");
		}
	}
	
	/**
	 * Query a field from a Java object.
	 * @throws XUndefinedField if the field does not exist or its static property does not match
	 */
	public static Field getField(Object fromObject, String fieldName, boolean isStatic) throws XUndefinedField {
		try {
			Field f = fromObject.getClass().getField(fieldName);
			if ((Modifier.isStatic(f.getModifiers())) == isStatic) {
				return f;
			} else {
				throw new XUndefinedField("field access ", fieldName + " not accessible.");
			}
		} catch (NoSuchFieldException e) {
			// the field does not exist
			throw new XUndefinedField("field access ", fieldName + " not accessible.");
		}
	}
	
	/**
	 * Returns all public methods from the given class parameter whose name starts with the
	 * given prefix. Moreover, the boolean parameter isStatic determines whether
	 * to consider only static or only non-static methods.
	 */
	public static Method[] allMethodsMatching(Class fromClass, String prefix, boolean isStatic) {
		// all public methods defined in the class
		Method[] allPublicMethods = fromClass.getMethods();
		
		Vector matchingMethods = new Vector(allPublicMethods.length);
		for (int i = 0; i < allPublicMethods.length; i++) {
			Method m = allPublicMethods[i];
			if (Modifier.isStatic(m.getModifiers()) == isStatic) {
				if (m.getName().startsWith(prefix)) {
					matchingMethods.add(m);
				}
			}
		}
		return (Method[]) matchingMethods.toArray(new Method[matchingMethods.size()]);
	}
	
	/**
	 * Since Java uses strict matching when asked for a method, given an array of 
	 * classes, this often means that the types are overspecified and therefore no
	 * matches can be found. As a consequence we have our own mechanism to select
	 * which set of methods is applicable given a selector. Further dispatch needs
	 * only to be performed when more than a single match exists.
	 * @param jClass - the class from which the methods will be selected.
	 * @param selector - the name of the requested method.
	 * @return an array of applicable methods
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
	
	private static Object[] coerceArguments(Object[] args, Class[] types) throws XTypeMismatch {
		Object[] coercedArgs = new Object[args.length];
		for (int i = 0; i < args.length; i++) {
			coercedArgs[i] = Coercer.coerce((ATObject) args[i], types[i]);
		}
		return coercedArgs;
	}
}
