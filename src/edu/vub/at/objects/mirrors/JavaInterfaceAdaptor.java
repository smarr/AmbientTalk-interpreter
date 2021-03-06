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
import edu.vub.at.exceptions.signals.Signal;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.coercion.Coercer;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATFraction;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.symbiosis.JavaObject;

import java.lang.reflect.Constructor;
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
		Method[] allMethods = jClass.getMethods();
		for (int i = 0; i < allMethods.length; i++) {
			if (allMethods[i].getName().equals(jSelector)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Invokes a method on a Java object identified by a selector.
	 * 
	 * @param jClass the class of the receiver object
	 * @param natReceiver the receiver (a native AmbientTalk object)
	 * @param jSelector the java-level selector identifying the method to invoke
	 * @param atSelector the original AmbientTalk selector
	 * @param jArguments parameters, normally AT objects
	 * @return the return value of the reflectively invoked method
	 */	
	public static ATObject invokeNativeATMethod(Class jClass, ATObject natReceiver,
										        String jSelector, ATSymbol atSelector, ATObject[] jArguments) throws InterpreterException {
		return invokeNativeATMethod(getNativeATMethod(jClass, natReceiver, jSelector, atSelector), natReceiver, jArguments);
	}
	
	/**
	 * Invokes a method on a native AmbientTalk object identified by a java.lang.reflect.Method object.
	 * Note that if the method to invoke reflectively has a formal parameter list consisting
	 * of one argument of type ATObject[], then the arguments are wrapped in an array such that
	 * the function actually takes a variable number of arguments.
	 * 
	 * A native AmbientTalk method is an ordinary Java method with the following constraints:
	 *  - its name usually starts with base_ or meta_, identifying whether the method is accessible at
	 *    the AmbientTalk base or meta level
	 *  - its formal parameters MUST all be subtypes of ATObject (or be a single array of ATObject[] for varargs)
	 *  - its return type must be a subtype of ATObject or a native Java type
	 *    (native types are subject to default conversion to the appropriate AmbientTalk natives)
	 *  - it may only throw InterpreterException exceptions
	 * 
	 * @param javaMethod the Java method to invoke
	 * @param jReceiver the Java object representing the receiver (normally an AT object)
	 * @param jArguments the AT arguments to pass
	 * @return the return value of the reflectively invoked method
	 * 
	 * TODO: code duplication w.r.t. invokeSymbioticMethod => replace this method by calls to invokeSymbioticMethod?
	 */
	public static ATObject invokeNativeATMethod(Method javaMethod, ATObject jReceiver, ATObject[] jArguments) throws InterpreterException {
		try {
			// if the native method takes an array as its sole parameter, it is interpreted as taking
			// a variable number of ambienttalk arguments
			Class[] params = javaMethod.getParameterTypes();
			Object[] args;
			if ((params.length == 1) && params[0].equals(ATObject[].class)) {
				args= new Object[] { jArguments };
			} else {
				if (params.length != jArguments.length) {
					throw new XArityMismatch("native method "+Reflection.downSelector(javaMethod.getName()), params.length, jArguments.length);
				}
				// make sure to properly 'coerce' each argument into the proper AT interface type
				args = coerceArguments(jArguments, params);
			}
			Object rval = javaMethod.invoke(jReceiver, args);
			if (rval instanceof ATObject) {
				return (ATObject) rval;
			} else {
				return primitiveJavaToATObject(rval);
			}
		} catch (IllegalAccessException e) {
			// the invoked method is not publicly accessible
			throw new XReflectionFailure("Native method "+Reflection.downSelector(javaMethod.getName()) + " not accessible.", e);
		} catch (IllegalArgumentException e) {
			// illegal argument types were supplied
			throw new XIllegalArgument("Illegal argument for native method "+Reflection.downSelector(javaMethod.getName()) + ": " + e.getMessage(), e);
		} catch (InvocationTargetException e) {
			// the invoked method threw an exception
			if (e.getTargetException() instanceof InterpreterException)
				throw (InterpreterException) e.getTargetException();
			else if (e.getTargetException() instanceof Signal) {
			    throw (Signal) e.getTargetException();	
			} else {
				e.printStackTrace();
				throw new XReflectionFailure("Native method "+Reflection.downSelector(javaMethod.getName())+" threw internal exception", e.getTargetException());
		    }
		}
	}
	
	/**
	 * Try to create a new instance of a Java class given an array of initialization arguments.
	 * Because we do not have exact typing information, all of the public constructors of the
	 * class are traversed until one is found that can create new instances given the current
	 * initargs.
	 */
	public static ATObject createNativeATObject(Class jClass, ATObject[] jInitArgs) throws InterpreterException {
		Constructor[] ctors = jClass.getConstructors();
		for (int i = 0; i < ctors.length; i++) {
			Constructor ctor = ctors[i];
			if (ctor.getParameterTypes().length == jInitArgs.length) {
				try {
					// make sure to properly 'coerce' each argument into the proper AT interface type
					Object[] coercedInitArgs = coerceArguments(jInitArgs, ctor.getParameterTypes());
					return (ATObject) ctor.newInstance(coercedInitArgs);
				} catch (IllegalArgumentException e) {
					continue; // argument types don't match, may find other constructor
				} catch (InstantiationException e) {
					break; // class is an abstract class, won't find a match
				} catch (IllegalAccessException e) {
					continue; // private or protected constructor, may find another one
				} catch (InvocationTargetException e) {
					// an exception was raised by the constructor
					if (e.getTargetException() instanceof InterpreterException)
						throw ((InterpreterException) e.getTargetException());
					else if (e.getTargetException() instanceof Signal) {
					    throw (Signal) e.getTargetException();	
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
	
	public static Method getNativeATMethod(
			Class baseInterface, 
			ATObject receiver,
			String methodName, ATSymbol atSelector) throws InterpreterException {
		Method[] applicable = getMethodsForSelector(baseInterface, methodName);
		switch (applicable.length) {
			case 0:
				throw new XSelectorNotFound(atSelector, receiver);
			case 1:
				return applicable[0];
			default:
				throw new XIllegalOperation("Native method uses overloading: " + atSelector + " in " + baseInterface);
		}
	}
	
	/**
	 * Returns all public methods from the given class parameter whose name starts with the
	 * given prefix. Moreover, the boolean parameter isStatic determines whether
	 * to consider only static or only non-static methods.
	 */
	public static Method[] allMethodsPrefixed(Class fromClass, String prefix, boolean isStatic) {
		// all public methods defined in the class
		Method[] allPublicMethods = (isStatic) ? fromClass.getDeclaredMethods() : fromClass.getMethods();
		
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
	
	private static Object[] coerceArguments(ATObject[] args, Class[] types) throws XTypeMismatch {
		Object[] coercedArgs = new Object[args.length];
		for (int i = 0; i < args.length; i++) {
			coercedArgs[i] = Coercer.coerce(args[i], types[i]);
		}
		return coercedArgs;
	}
	
	public static final boolean isPrimitiveType(Class c) {
		return (c.isPrimitive() ||
				c == Integer.class ||
				c == Double.class ||
				c == Float.class ||
				c == Character.class ||
				c == Boolean.class ||
				c == Byte.class ||
				c == Long.class ||
				c == Short.class ||
				c == Void.class);
	}
	
	/**
	 * Convert an primitive Java value to an AmbientTalk object
	 * Supported mappings are:
	 *  int -> NATNumber
	 *  double -> NATFraction
	 *  char -> NATText
	 *  boolean -> NATBoolean
	 *  void -> NIL
	 *  
	 *  float, byte, long and short are left as JavaObjects.
	 *  They are not 'converted' into AmbientTalk primitives because of the opposite
	 *  reasons why e.g. numbers cannot be converted automatically to long, short, etc.
	 *  See the description of the 'atObjectToPrimitiveJava' method.
	 *  
	 *  Conversion from e.g. a java.lang.Long to a NATNumber is still possible by
	 *  using code such as:
	 *    
	 *    <code>aWrappedLongValue.intValue();</code>
	 */
	public static final ATObject primitiveJavaToATObject(Object jObj) throws XReflectionFailure {
		// integer
		if (jObj instanceof Integer) {
			return NATNumber.atValue(((Integer) jObj).intValue());
		// double
		} else if (jObj instanceof Double) {
			return NATFraction.atValue(((Double) jObj).doubleValue());
		// char
		} else if (jObj instanceof Character) {
			return NATText.atValue(((Character) jObj).toString());
		// boolean
		} else if (jObj instanceof Boolean) {
			return NATBoolean.atValue(((Boolean) jObj).booleanValue());
		// float
		} else if (jObj instanceof Float) {
			return JavaObject.wrapperFor(jObj);
			//return NATFraction.atValue(((Float) jObj).floatValue());
		// byte
		} else if (jObj instanceof Byte) {
			return JavaObject.wrapperFor(jObj);
			//return NATNumber.atValue(((Byte) jObj).byteValue());
		// long
		} else if (jObj instanceof Long) {
			return JavaObject.wrapperFor(jObj);
			//return NATFraction.atValue(((Long) jObj).longValue());
		// short
		} else if (jObj instanceof Short) {
			return JavaObject.wrapperFor(jObj);
			//return NATNumber.atValue(((Short) jObj).shortValue());
		} else {
		    throw new XReflectionFailure("Expected a primitive Java value, given: " + jObj);
		}
	}
	
	/**
	 * Convert an ambienttalk object to a primitive type.
	 * Supported mappings are:
	 *  NATNumber -> int
	 *  NATFraction -> double
	 *  NATText -> char
	 *  NATBoolean -> boolean
	 *  ATObject -> void
	 *  
	 *  Conversion to float, byte, long and short is not supported.
	 *  The reason for this is that otherwise, symbiotic invocations
	 *  will match with a lot of method signatures, and we would have to keep
	 *  track of the 'best fitting match'. E.g. given methods m(int) m(long) and m(short)
	 *  then invoking 'o.m(10)' in AmbientTalk would match all three methods.
	 *  By disabling conversions from NATNumber to long and short, only one match remains.
	 *  If conversion to any one of these primitive types is needed, use code such as:
	 *    
	 *    <code>jlobby.java.lang.Integer.new(10).longValue();</code>
	 */
	public static final Object atObjectToPrimitiveJava(ATObject atObj, Class type) throws XTypeMismatch, XIllegalArgument {
		// integer
		if (type == int.class || type == Integer.class) {
			return new Integer(atObj.asNativeNumber().javaValue);
		// double
		} else if (type == double.class || type == Double.class) {
			return new Double(atObj.asNativeFraction().javaValue);
		// char
		} else if (type == char.class || type == Character.class) {
			return new Character(atObj.asNativeText().asChar());
		// boolean
		} else if (type == boolean.class || type == Boolean.class) {
			return new Boolean(atObj.asNativeBoolean().javaValue);
		// float
		} else if (type == float.class || type == Float.class) {
			// can only convert wrapped java.lang.Float
			if (atObj.isJavaObjectUnderSymbiosis()) {
				Object wrapped = atObj.asJavaObjectUnderSymbiosis().getWrappedObject();
				if (wrapped instanceof Float) {
					return wrapped;
				}
			}
			throw new XTypeMismatch(Float.class, atObj);
			//return Float.valueOf((float) atObj.asNativeFraction().javaValue);
		// byte
		} else if (type == byte.class || type == Byte.class) {
			// can only convert wrapped java.lang.Byte
			if (atObj.isJavaObjectUnderSymbiosis()) {
				Object wrapped = atObj.asJavaObjectUnderSymbiosis().getWrappedObject();
				if (wrapped instanceof Byte) {
					return wrapped;
				}
			}
			throw new XTypeMismatch(Byte.class, atObj);
			//return Byte.valueOf((byte) atObj.asNativeNumber().javaValue);
		// long
		} else if (type == long.class || type == Long.class) {
			// can only convert wrapped java.lang.Long
			if (atObj.isJavaObjectUnderSymbiosis()) {
				Object wrapped = atObj.asJavaObjectUnderSymbiosis().getWrappedObject();
				if (wrapped instanceof Long) {
					return wrapped;
				}
			}
			throw new XTypeMismatch(Long.class, atObj);
			//return Long.valueOf((long) atObj.asNativeFraction().javaValue);
		// short
		} else if (type == short.class || type == Short.class) {
			// can only convert wrapped java.lang.Short
			if (atObj.isJavaObjectUnderSymbiosis()) {
				Object wrapped = atObj.asJavaObjectUnderSymbiosis().getWrappedObject();
				if (wrapped instanceof Short) {
					return wrapped;
				}
			}
			throw new XTypeMismatch(Short.class, atObj);
			//return Short.valueOf((short) atObj.asNativeNumber().javaValue);
		} else if (type == void.class || type == Void.class) {
			return null;
		} else {
		    throw new XIllegalArgument("Expected a primitive Java type, given: " + type);
		}
	}
}
