/**
 * AmbientTalk/2 Project
 * Symbiosis.java created on 5-nov-2006 at 19:22:26
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
package edu.vub.at.objects.symbiosis;

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XIllegalArgument;
import edu.vub.at.exceptions.XNotInstantiatable;
import edu.vub.at.exceptions.XReflectionFailure;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XSymbiosisFailure;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.exceptions.XUnassignableField;
import edu.vub.at.exceptions.XUndefinedSlot;
import edu.vub.at.exceptions.signals.Signal;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.coercion.Coercer;
import edu.vub.at.objects.mirrors.JavaInterfaceAdaptor;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.natives.NATException;
import edu.vub.at.objects.natives.OBJNil;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

/**
 * The Symbiosis class is a container for auxiliary methods pertaining to making symbiotic
 * reflective Java invocations.
 * 
 * @author tvcutsem
 */
public final class Symbiosis {

	/**
	 * Invoke a java method symbiotically, given only its name (not its implementation).
	 * First retrieves all of the methods matching the given selector in the given class, then tries
	 * to invoke the method symbiotically using the default symbiotic invocation algorithm.
	 * 
	 * @see #symbioticInvocation(ATObject, Object, String, JavaMethod, ATObject[]) the symbiotic invocation algorithm.
	 */
	public static ATObject symbioticInvocation(ATObject wrapper, Object symbiont, Class ofClass, String selector, ATObject[] atArgs) throws InterpreterException {
		return symbioticInvocation(wrapper, symbiont, selector, getMethods(ofClass, selector, (symbiont==null)), atArgs);
	}
	
	/**
	 * Invoke a java method symbiotically.
	 * The Java method invocation algorithm is as follows:
	 * <pre>
	 * case of # of methods matching selector:
	 *   0 => XSelectorNotFound
	 *   1 => invoke the method OR XIllegalArgument, XArityMismatch, XReflectionFailure
	 *   * => (case of # of methods with matching arity OR taking varargs:
	 *           0 => XSymbiosisFailure
	 *           1 => invoke the method OR XIllegalArgument, XReflectionFailure
	 *           * => (case of # of methods matching 'default type' of the actual arguments:
	 *                   0 => XSymbiosisFailure
	 *                   1 => invoke OR XReflectionFailure
	 *                   * => XSymbiosisFailure))
	 * </pre>
	 * A Java method takes a variable number of AT arguments <=> it has one formal parameter of type ATObject[]
	 * 
	 * @param wrapper the ATObject wrapper for the symbiont
	 * @param symbiont the Java object being accessed from within AmbientTalk
	 * @param selector the Java selector corresponding to the method invocation
	 * @param jMethod a JavaMethod encapsulating all applicable Java methods that correspond to the selector
	 * @param atArgs the AT args to the symbiotic invocation
	 * @return the wrapped result of the Java method invocation
	 * 
	 * @throws XArityMismatch if the wrong number of arguments were supplied
	 * @throws XSelectorNotFound if no methods correspond to the given selector (i.e. jMethod is null)
	 * @throws XTypeMismatch if one of the arguments cannot be converted into the static type expected by the Java method
	 * @throws XSymbiosisFailure if the method is overloaded and cannot be unambiguously resolved given the actual arguments
	 * @throws XReflectionFailure if the invoked method is not accessible from within AmbientTalk
	 * @throws XJavaException if the invoked Java method throws a Java exception
	 */
	public static ATObject symbioticInvocation(ATObject wrapper, Object symbiont, String selector, JavaMethod jMethod, ATObject[] atArgs)
	                                           throws InterpreterException {
		if (jMethod == null) {
		    // no methods found? selector does not exist...
			throw new XSelectorNotFound(Reflection.downSelector(selector), wrapper);
		} else {
			Method[] methods = jMethod.choices_;
			if (methods.length == 1) {
				// just one method found, no need to resolve overloaded methods
				// if the Java method takes an ATObject array as its sole parameter, it is interpreted as taking
				// a variable number of ambienttalk arguments
				Class[] params = methods[0].getParameterTypes();
				Object[] args;
				if ((params.length == 1) && params[0].equals(ATObject[].class)) {
					args = new Object[] { atArgs };
				} else {
					if (params.length != atArgs.length) {
						throw new XArityMismatch("Java method "+Reflection.downSelector(methods[0].getName()), params.length, atArgs.length);
					}
					// make sure to properly 'coerce' each argument into the proper AT interface type
					args = atArgsToJavaArgs(atArgs, params);
				}
				return invokeUniqueSymbioticMethod(symbiont, methods[0], args);
			} else {	
				// overloading: filter out all methods that do not match arity or whose
				// argument types do not match
				Object[] actuals = null;
				Class[] params;
				LinkedList matchingMethods = new LinkedList();
				for (int i = 0; i < methods.length; i++) {
					params = methods[i].getParameterTypes();
					// is the method a varargs method?
					if ((params.length == 1) && params[0].equals(ATObject[].class)) {
						actuals = new Object[] { atArgs };
						matchingMethods.addFirst(methods[i]);
					// does the arity match?
					} else if (params.length == atArgs.length) {
						// can it be invoked with the given actuals?
						try {
							actuals = atArgsToJavaArgs(atArgs, params);
							matchingMethods.addFirst(methods[i]);
						} catch(XTypeMismatch e) {
							// types don't match
						}
					} else {
				      // arity does not match
					}
				}
				
				switch (matchingMethods.size()) {
				    case 0: {
					    // no methods left: overloading resolution failed
					    throw new XSymbiosisFailure(symbiont, selector, atArgs);
				    }
				    case 1: {
				    	// just one method left, invoke it
						return invokeUniqueSymbioticMethod(symbiont, (Method) matchingMethods.getFirst(), actuals);
				    }
				    default: {
				    	// more than one method left: overloading resolution failed
						throw new XSymbiosisFailure(symbiont, selector, matchingMethods, atArgs);
				    }
				}
			}
		}
	}
	
	/**
	 * Creates a new instance of a Java class.
	 * 
	 * @param ofClass the Java class of which to create an instance
	 * @param atArgs the AmbientTalk arguments to the constructor, to be converted to Java arguments
	 * @return an unitialized JavaObject wrapper around a newly created instance of the class
	 * 
	 * @throws XArityMismatch if the wrong number of arguments were supplied
	 * @throws XNotInstantiatable if no public constructors are available or if the class is abstract
	 * @throws XTypeMismatch if one of the arguments cannot be converted into the static type expected by the constructor
	 * @throws XSymbiosisFailure if the constructor is overloaded and cannot be unambiguously resolved given the actual arguments
	 * @throws XReflectionFailure if the invoked constructor is not accessible from within AmbientTalk
	 * @throws XJavaException if the invoked Java constructor throws a Java exception
	 */
	public static ATObject symbioticInstanceCreation(Class ofClass, ATObject[] atArgs) throws InterpreterException {
		Constructor[] ctors = ofClass.getConstructors();
		switch (ctors.length) {
		     // no constructors found? class is not instantiatable...
		case 0:
			throw new XNotInstantiatable(ofClass);
			// just one constructor found, no need to resolve overloaded methods
		case 1: {
			// if the constructor takes an ATObject array as its sole parameter, it is interpreted as taking
			// a variable number of ambienttalk arguments
			Class[] params = ctors[0].getParameterTypes();
			Object[] args;
			if ((params.length == 1) && params[0].equals(ATObject[].class)) {
				args = new Object[] { atArgs };
			} else {
				if (params.length != atArgs.length) {
					throw new XArityMismatch("Java constructor "+Reflection.downSelector(ctors[0].getName()), params.length, atArgs.length);
				}
				// make sure to properly convert actual arguments into Java objects
				args = atArgsToJavaArgs(atArgs, params);
			}
			return invokeUniqueSymbioticConstructor(ctors[0], args);
		  }	
		}
		
		// overloading: filter out all constructors that do not match arity or whose argument types do not match
		int matchingCtors = 0;
		Constructor matchingCtor = null;
		Object[] actuals = null;
		Class[] params;
		for (int i = 0; i < ctors.length; i++) {
			params = ctors[i].getParameterTypes();
			// is the constructor a varargs constructor?
			if ((params.length == 1) && params[0].equals(ATObject[].class)) {
				actuals = new Object[] { atArgs };
				matchingCtor = ctors[i];
				matchingCtors++;
				// does the arity match?
			} else if (params.length == atArgs.length) {
				// can it be invoked with the given actuals?
				try {
					actuals = atArgsToJavaArgs(atArgs, params);
					matchingCtor = ctors[i];
					matchingCtors++;
				} catch(XTypeMismatch e) {
					// types don't match
					ctors[i] = null; // TODO: don't assign to null, array may be cached or used later on (or by wrapper method)
				}
			} else {
				// arity does not match
				ctors[i] = null;
			}
		}
		
		if (matchingCtors != 1) {
			// no constructors left or more than one constructor left? overloading resolution failed
			throw new XSymbiosisFailure(ofClass, ctors, atArgs, matchingCtors);
		} else {
			// just one constructor left, invoke it
			return invokeUniqueSymbioticConstructor(matchingCtor, actuals);
		}
	}
	
	/**
	 * Read a field from the given Java object reflectively.
	 * @return the contents of the Java field, converted into its AmbientTalk equivalent
	 */
	public static ATObject readField(Object fromObject, Class ofClass, String fieldName)
	                     throws InterpreterException {
		Field f = getField(ofClass, fieldName, (fromObject == null));
		return readField(fromObject, f);
	}
	
	/**
	 * Read a field from the given Java object reflectively.
	 * @return the contents of the Java field, converted into its AmbientTalk equivalent
	 */
	public static ATObject readField(Object fromObject, Field f) throws InterpreterException {
		try {
			return Symbiosis.javaToAmbientTalk(f.get(fromObject));
		} catch (IllegalArgumentException e) {
			// the given object is of the wrong class, should not happen!
			throw new XReflectionFailure("Illegal class for field access of "+f.getName() + ": " + e.getMessage());
		} catch (IllegalAccessException e) {
             // the read field is not publicly accessible
			throw new XReflectionFailure("field access of " + f.getName() + " not accessible.");
		}
	}
	
	/**
	 * Write a field in the given Java object reflectively.
	 * @param toObject if null, the field is assumed to be static
	 * @param value the AmbientTalk value which will be converted into its Java equivalent to be written int he field
	 */
	public static void writeField(Object toObject, Class ofClass, String fieldName, ATObject value)
	                              throws InterpreterException {
		Field f = getField(ofClass, fieldName, (toObject == null));
		writeField(toObject, f, value);
	}
	
	/**
	 * Write a field in the given Java object reflectively.
	 * @param value the AmbientTalk value which will be converted into its Java equivalent to be written int he field
	 */
	public static void writeField(Object toObject, Field f, ATObject value) throws InterpreterException {
		try {
			f.set(toObject, Symbiosis.ambientTalkToJava(value, f.getType()));
		} catch (IllegalArgumentException e) {
			// the given value is of the wrong type
			throw new XIllegalArgument("Illegal value for field "+f.getName() + ": " + e.getMessage());
		} catch (IllegalAccessException e) {
             // the read field is not publicly accessible or final
			throw new XUnassignableField(Reflection.downSelector(f.getName()).toString());
		}
	}
	
	/**
	 * Query whether the given Java Class contains a (non-)static method with the given selector
	 */
	public static boolean hasMethod(Class c, String selector, boolean isStatic) {
		Method[] methods = c.getMethods();
		for (int i = 0; i < methods.length; i++) {
			if (Modifier.isStatic(methods[i].getModifiers()) == isStatic) {
				if (methods[i].getName().equals(selector)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Query whether the given Java Class contains a (non-)static field with the given selector
	 */
	public static boolean hasField(Class c, String selector, boolean isStatic) {
		try {
			Field f = c.getField(selector);
			return (Modifier.isStatic(f.getModifiers()) == isStatic);
		} catch (NoSuchFieldException e) {
			return false;
		}
	}
	
	/**
	 * Retrieve a field from a Java object.
	 * @throws XUndefinedSlot if the field does not exist or its static property does not match
	 */
	public static Field getField(Class fromClass, String fieldName, boolean isStatic) throws XUndefinedSlot {
		try {
			Field f = fromClass.getField(fieldName);
			if ((Modifier.isStatic(f.getModifiers())) == isStatic) {
				return f;
			} else {
				throw new XUndefinedSlot("field access ", fieldName + " not accessible.");
			}
		} catch (NoSuchFieldException e) {
			// the field does not exist
			throw new XUndefinedSlot("field access ", fieldName + " not accessible.");
		}
	}
	
	/**
	 * Retrieve all methods of a given name from a Java object. These are bundled together
	 * in a first-class JavaMethod object, which is cached for later reference.
	 * 
	 * A null return value indicates no matches.
	 */
	public static JavaMethod getMethods(Class fromClass, String selector, boolean isStatic) {
		// first, check the method cache
		JavaMethod cachedEntry = JMethodCache._INSTANCE_.get(fromClass, selector, isStatic);
		if (cachedEntry != null) {
			// cache hit
			return cachedEntry;
		} else {
			// cache miss: assemble a new JavaMethod entry
			Method[] methods = fromClass.getMethods();
			Method m;
			Vector properMethods = new Vector(methods.length);
			for (int i = 0; i < methods.length; i++) {
				m = methods[i];
				if ((Modifier.isStatic(m.getModifiers())) == isStatic && m.getName().equals(selector)) {
					properMethods.add(methods[i]);
				}
			}
			Method[] choices = (Method[]) properMethods.toArray(new Method[properMethods.size()]);
			if (choices.length == 0) {
				// no matches
				return null;
			} else {
				// add entry to cache and return it
				JavaMethod jMethod = new JavaMethod(choices);
				JMethodCache._INSTANCE_.put(fromClass, selector, isStatic, jMethod);
				return jMethod;
			}
		}
	}
	
	/**
	 * Retrieve all public static or non-static methods from a given Java class
	 * (this includes methods defined in superclasses). All methods are properly wrapped in a
	 * JavaMethod wrapper, taking care to wrap a set of overloaded methods using the same wrapper.
	 * 
	 * @param isStatic if true, all static methods of fromClass are returned, otherwise the instance methods are returned
	 */
	public static JavaMethod[] getAllMethods(Class fromClass, boolean isStatic) {
		// assemble a set of all unique selectors of all (non-)static methods of the class
		HashSet uniqueNames = new HashSet();
		Method[] methods = fromClass.getMethods();
		for (int i = 0; i < methods.length; i++) {
			Method m = methods[i];
			if ((Modifier.isStatic(m.getModifiers())) == isStatic) {
				uniqueNames.add(m.getName());
			}
		}
		
		// create a JavaMethod[] array large enough to contain all 'unique methods'
		JavaMethod[] jmethods = new JavaMethod[uniqueNames.size()];
		// loop over all entries and group the methods into a single wrapper
		int i = 0;
		for (Iterator iter = uniqueNames.iterator(); iter.hasNext();) {
			String methodName = (String) iter.next();
			jmethods[i++] = getMethods(fromClass, methodName, isStatic);
		}
		return jmethods;
	}
	
	/**
	 * Retrieve all public static or non-static fields from a given Java class
	 * (this includes fields defined in superclasses, but excludes shadowed superclass fields). All fields are properly wrapped in a
	 * JavaField wrapper.
	 * 
	 * @param ofObject if null, all static fields of fromClass are returned, otherwise the instance fields are returned
	 */
	public static JavaField[] getAllFields(Object ofObject, Class fromClass) {
		boolean isStatic = (ofObject == null);
		Field[] fields = fromClass.getFields();
		// we do not consider shadowed superclass fields, therefore we store all encountered fields
		// in a table and only keep the field with the most specific class
		Hashtable recordedFields = new Hashtable();
		for (int i = 0; i < fields.length; i++) {
			Field f = fields[i];
			if ((Modifier.isStatic(f.getModifiers())) == isStatic) {
				// did we already encounter this field?
				if (recordedFields.contains(f.getName())) {
					// yes, then compare encountered field with previous field and only store most specific one
					Field prev = (Field) recordedFields.get(f.getName());
					// is f's Class a subclass of prev's Class?
					if (prev.getDeclaringClass().isAssignableFrom(f.getDeclaringClass())) {
						// yes, so f is more specific, store it instead of prev
						recordedFields.remove(prev.getName());
						recordedFields.put(f.getName(), f);
					} // if not, keep previous field
				} else {
					// field not encountered  yet, store it
					recordedFields.put(f.getName(), f);
				}
			}
		}
		// create a JavaField[] array large enough to contain all entries in the table
		JavaField[] jfields = new JavaField[recordedFields.size()];
		// loop over all entries in the table and wrap each field
		int i = 0;
		for (Iterator iter = recordedFields.values().iterator(); iter.hasNext(); i++) {
			jfields[i] = new JavaField(ofObject, (Field) iter.next());
		}
		return jfields;
	}

	/**
	 * Convert a Java object into an AmbientTalk object, according to
	 * the following rules:
	 * <pre>
	 * null = nil
	 * ATObject obj = obj
	 * int n = Number(n)
	 * double d = Fraction(d)
	 * boolean b = Boolean(b)
	 * String s = Text(s)
	 * T[] array = Table(array.length)
	 * InterpreterException e = NATException(e)
	 * Exception e = NATException(XJavaException(e))
	 * SymbioticATObj o = o.wrappedATObject
	 * Class c = JavaClass(c)
	 * Object o = JavaObject(o)
	 * </pre>
	 * 
	 * @param jObj the Java object representing a mirror or a native type
	 * @return the same object if it implements the ATObject interface
	 */
	public static final ATObject javaToAmbientTalk(Object jObj) throws InterpreterException {
		// -- NULL => NIL --
	    if (jObj == null) {
		  return OBJNil._INSTANCE_;
		// -- AmbientTalk implementation-level objects --
	    } else if (jObj instanceof ATObject) {
			return (ATObject) jObj;
	    // -- PRIMITIVE TYPE => NUMERIC, TXT --
		} else if (JavaInterfaceAdaptor.isPrimitiveType(jObj.getClass())) {
		    return JavaInterfaceAdaptor.primitiveJavaToATObject(jObj);
		// -- STRING => TEXT --
		} else if (jObj instanceof String) {
			return NATText.atValue((String) jObj);
		// -- ARRAY => TABLE --
		} else if (jObj.getClass().isArray()) {
			int length = Array.getLength(jObj);
			ATObject[] atTable = new ATObject[length];
			for (int i = 0; i < length; i++) {
				atTable[i] = javaToAmbientTalk(Array.get(jObj, i));
			}
			return NATTable.atValue(atTable);
	    // -- EXCEPTION => NATEXCEPTION --
		} else if(jObj instanceof InterpreterException) {
			return ((InterpreterException)jObj).getAmbientTalkRepresentation();
		} else if (jObj instanceof Exception) {
			return new NATException(new XJavaException((Exception) jObj));
		// -- Symbiotic AmbientTalk object => AmbientTalk object --
		} else if (jObj instanceof SymbioticATObjectMarker) {
			return ((SymbioticATObjectMarker) jObj)._returnNativeAmbientTalkObject();
		// -- java.lang.Class => Symbiotic Class --
		} else if (jObj instanceof Class) {
			return JavaClass.wrapperFor((Class) jObj);
		// -- Object => Symbiotic AT Object --
		} else {
			return JavaObject.wrapperFor(jObj);
		}
	}

	/**
	 * Convert an AmbientTalk object into an equivalent Java object, according
	 * to the following rules:
	 * <pre>
	 * Number n -> int = n.javaValue
	 * Fraction f -> double = f.javaValue
	 * Boolean b -> boolean = b.javaValue
	 * Text t -> String = t.javaValue
	 * JavaObject jobj -> T = (T) jobj.wrappedObject
	 * ATObject obj -> ATObject = obj
	 * Table obj -> T[] = new T[obj.length]
	 * NATException exc -> Exception = exc.wrappedException
	 * JavaClass jcls -> Class = jcls.wrappedClass
	 * nil -> Object = null
	 * ATObject obj -> Interface = Coercer<obj,Interface>
	 * </pre>
	 * @param atObj the AmbientTalk object to convert to a Java value
	 * @param targetType the known static type of the Java object that should be attained
	 * @return a Java object o where (o instanceof targetType) should yield true
	 * 
	 * @throws XTypeMismatch if the object cannot be converted into the correct Java targetType
	 */
	public static final Object ambientTalkToJava(ATObject atObj, Class targetType) throws InterpreterException {
		// -- PRIMITIVE TYPES --
        if (JavaInterfaceAdaptor.isPrimitiveType(targetType)) {
		    return JavaInterfaceAdaptor.atObjectToPrimitiveJava(atObj, targetType);
		// -- WRAPPED JAVA OBJECTS --
        } else if (atObj.isJavaObjectUnderSymbiosis()) {
	    	Object jObj = atObj.asJavaObjectUnderSymbiosis().getWrappedObject();
		    Class jCls = jObj.getClass();
		    // dynamic subtype test: is jCls a subclass of targetType?
		    if (targetType.isAssignableFrom(jCls)) {
		    	return jObj;
		    }
	    }
        
        // -- IMPLEMENTATION-LEVEL OBJECTS --
        if (targetType.isInstance(atObj)) {
			// target type is a subtype of ATObject, return the implementation-level object itself
			return atObj;
		// -- STRINGS --
		} else if (targetType == String.class) {
			return atObj.asNativeText().javaValue;
		// -- ARRAYS --
		} else if (targetType.isArray()) {
			ATObject[] atArray = atObj.asNativeTable().elements_;
			Object jArray = Array.newInstance(targetType.getComponentType(), atArray.length);
			for (int i = 0; i < Array.getLength(jArray); i++) {
				Array.set(jArray, i, ambientTalkToJava(atArray[i], targetType.getComponentType()));
			}
			return jArray;
		// -- EXCEPTIONS --
		} else if (Exception.class.isAssignableFrom(targetType)) {
			return Evaluator.asNativeException(atObj);
		// -- CLASS OBJECTS --
		} else if (targetType == Class.class) {
			return atObj.asJavaClassUnderSymbiosis().getWrappedClass();
	    // -- nil => NULL --
		} else if (atObj == OBJNil._INSTANCE_) {
			return null;
		// -- INTERFACE TYPES AND NAT CLASSES --
		} else {
			return Coercer.coerce(atObj, targetType);	
		}
	}
	
	/**
	 * Returns whether the symbiosis layer should process the given method purely
	 * asynchronously or not.
	 * 
	 * @return whether the specified Java method denotes an event notification
	 */
	public static boolean isEvent(Method method) {
		return EventListener.class.isAssignableFrom(method.getDeclaringClass()) // is an EventListener
		    && (method.getReturnType() == Void.TYPE) // does not return a value
		    && (method.getExceptionTypes().length == 0); // throws no exceptions
	}
	
	private static ATObject invokeUniqueSymbioticMethod(Object symbiont, Method javaMethod, Object[] jArgs) throws InterpreterException {
		try {
			return Symbiosis.javaToAmbientTalk(javaMethod.invoke(symbiont, jArgs));
		} catch (IllegalAccessException e) {
			// the invoked method is not publicly accessible
			// sometimes this may happen when accessing inner classes, try again with an interface method:
			Method interfaceMethod = toInterfaceMethod(javaMethod);
			if (interfaceMethod == null) { // no success
				// try to perform the call without access protection
				if (!javaMethod.isAccessible()) {
					javaMethod.setAccessible(true);
					return invokeUniqueSymbioticMethod(symbiont, javaMethod, jArgs);
				} else {
					// if access protection was already disabled, bail out
		            throw new XReflectionFailure("Java method "+Reflection.downSelector(javaMethod.getName()) + " is not accessible.", e);
				}
			} else {
				return invokeUniqueSymbioticMethod(symbiont, interfaceMethod, jArgs);
			}
		} catch (IllegalArgumentException e) {
			// illegal argument types were supplied, should not happen because the conversion should have already failed earlier (in atArgsToJavaArgs)
            // Backport from JDK 1.4 to 1.3
            // throw new RuntimeException("[broken at2java conversion?] Illegal argument for Java method "+javaMethod.getName(), e);
			throw new RuntimeException("[broken at2java conversion?] Illegal argument for Java method "+javaMethod.getName());
		} catch (InvocationTargetException e) {
			// the invoked method threw an exception
			if (e.getTargetException() instanceof InterpreterException)
				throw (InterpreterException) e.getTargetException();
			else if (e.getTargetException() instanceof Signal) {
			    throw (Signal) e.getTargetException();	
			} else {
				throw new XJavaException(symbiont, javaMethod, e.getTargetException());
		    }
		}
	}
	
	private static ATObject invokeUniqueSymbioticConstructor(Constructor ctor, Object[] jArgs) throws InterpreterException {
		try {
			return Symbiosis.javaToAmbientTalk(ctor.newInstance(jArgs));
		} catch (IllegalAccessException e) {
			// the invoked method is not publicly accessible
			throw new XReflectionFailure("Java constructor "+Reflection.downSelector(ctor.getName()) + " is not accessible.", e);
		} catch (IllegalArgumentException e) {
			// illegal argument types were supplied, should not happen because the conversion should have already failed earlier (in atArgsToJavaArgs)
		    // Backport from JDK 1.4 to 1.3
            // throw new RuntimeException("[broken at2java conversion?] Illegal argument for Java constructor "+ctor.getName(), e);
			throw new RuntimeException("[broken at2java conversion?] Illegal argument for Java constructor "+ctor.getName());
		} catch (InstantiationException e) {
			// the given class is abstract
			throw new XNotInstantiatable(ctor.getDeclaringClass(), e);
		} catch (InvocationTargetException e) {
			// the invoked method threw an exception
			if (e.getTargetException() instanceof InterpreterException)
				throw (InterpreterException) e.getTargetException();
			else if (e.getTargetException() instanceof Signal) {
			    throw (Signal) e.getTargetException();	
			} else {
				throw new XJavaException(null, ctor, e.getTargetException());
		    }
		}
	}
	
	private static Object[] atArgsToJavaArgs(ATObject[] args, Class[] types) throws InterpreterException {
		Object[] jArgs = new Object[args.length];
		for (int i = 0; i < args.length; i++) {
			jArgs[i] = Symbiosis.ambientTalkToJava(args[i], types[i]);
		}
		return jArgs;
	}
	
	/**
	 * Extremely vague and dirty feature of Java reflection: it can sometimes happen that
	 * a method is invoked on a private inner class via a publicly accessible interface method.
	 * In those cases, invoking that method results in an IllegalAccessException.
	 * One example is invoking aVector.iterator().hasNext()
	 * 
	 * The problem is that aVector.iterator() returns an instance of java.util.AbstractList$Itr
	 * which is probably private. Selecting that class's hasNext method and invoking it results in
	 * an IllegalAccessException. This can be circumvented by invoking the hasNext method through
	 * the java.util.Iterator interface class.
	 */
	private static Method toInterfaceMethod(Method m) {
		Class[] interfaces = m.getDeclaringClass().getInterfaces();
		if (interfaces == null) {
			return null;
		} else {
			// find the method in one of the interface declarations
			for (int i = 0; i < interfaces.length; i++) {
				try {
					return interfaces[i].getMethod(m.getName(), m.getParameterTypes());
				} catch(NoSuchMethodException e) {
					// continue searching
				}
			}
			// no declared method found
			return null;
		}
	}
	
}
