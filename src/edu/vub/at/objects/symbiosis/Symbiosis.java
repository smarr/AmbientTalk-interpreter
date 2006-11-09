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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XReflectionFailure;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XUndefinedField;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.mirrors.Reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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

	public static ATObject symbioticInvocation(Object symbiont, Class ofClass, String selector, ATObject[] atArgs) throws XSelectorNotFound {
		return symbioticInvocation(symbiont, getMethods(ofClass, selector, (symbiont==null)), atArgs);
	}
	
	public static ATObject symbioticInvocation(Object symbiont, Method[] choices, ATObject[] atArgs) {
		return null; // TODO: implement invoke for overloaded methods
	}
	
	public static ATObject symbioticInstanceCreation(Class ofClass, ATObject[] atArgs) {
		return null; // TODO: implement jClass.new(args) => JavaObject.wrapperFor( jClass.class.newInstance(args) )
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
			return Reflection.javaToAmbientTalk(f.get(fromObject));
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
			f.set(toObject, Reflection.ambientTalkToJava(value, f.getType()));
		} catch (IllegalArgumentException e) {
			// the given value is of the wrong type
			throw new XReflectionFailure("Illegal value for field "+f.getName() + ": " + e.getMessage());
		} catch (IllegalAccessException e) {
             // the read field is not publicly accessible
			throw new XReflectionFailure("field assignment of " + f.getName() + " not accessible.");
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
			c.getField(selector);
			return true;
		} catch (NoSuchFieldException e) {
			return false;
		}
	}
	
	/**
	 * Retrieve a field from a Java object.
	 * @throws XUndefinedField if the field does not exist or its static property does not match
	 */
	public static Field getField(Class fromClass, String fieldName, boolean isStatic) throws XUndefinedField {
		try {
			Field f = fromClass.getField(fieldName);
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
	 * Retrieve all methods of a given name from a Java object.
	 * An empty returned array indicates no match.
	 */
	public static Method[] getMethods(Class fromClass, String fieldName, boolean isStatic) throws XSelectorNotFound {
		Method[] methods = fromClass.getMethods();
		Vector properMethods = new Vector(methods.length);
		for (int i = 0; i < methods.length; i++) {
			if ((Modifier.isStatic(methods[i].getModifiers())) == isStatic) {
				properMethods.add(methods[i]);
			}
		}
		return (Method[]) properMethods.toArray(new Method[properMethods.size()]);
	}
	
	/**
	 * Retrieve all public static or non-static methods from a given Java class
	 * (this includes methods defined in superclasses). All methods are properly wrapped in a
	 * JavaMethod wrapper, taking care to wrap a set of overloaded methods using the same wrapper.
	 * 
	 * @param ofObject if null, all static methods of fromClass are returned, otherwise the instance methods are returned
	 */
	public static JavaMethod[] getAllMethods(Object ofObject, Class fromClass) {
		boolean isStatic = (ofObject == null);
		Method[] methods = fromClass.getMethods();
		// the following table sorts methods into lists according to their method name
		Hashtable sorted = new Hashtable();
		for (int i = 0; i < methods.length; i++) {
			Method m = methods[i];
			if ((Modifier.isStatic(m.getModifiers())) == isStatic) {
				LinkedList l = (LinkedList) sorted.get(m.getName());
				// does an entry for this method already exist?
				if (l == null) {
					// no? then add a list entry to the table of found methods
					l = new LinkedList();
					sorted.put(m.getName(), l);
				}
				// add method to appropriate entry
				l.addLast(m);
			}
		}
		// create a JavaMethod[] array large enough to contain all entries in 'sorted'
		JavaMethod[] jmethods = new JavaMethod[sorted.size()];
		// loop over all entries in the table and group the methods into a single wrapper
		int i = 0;
		for (Iterator iter = sorted.values().iterator(); iter.hasNext(); i++) {
			LinkedList entry = (LinkedList) iter.next();
			jmethods[i] = new JavaMethod(ofObject, (Method[]) entry.toArray(new Method[entry.size()]));
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
	
}
