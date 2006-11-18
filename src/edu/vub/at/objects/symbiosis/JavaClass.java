/**
 * AmbientTalk/2 Project
 * JavaClass.java created on 3-nov-2006 at 10:54:38
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

import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XDuplicateSlot;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.exceptions.XUndefinedField;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * A JavaClass instance represents a Java Class under symbiosis.
 * 
 * Java classes are treated as AmbientTalk 'singleton' objects:
 * 
 *  - cloning a Java class results in the same Java class instance
 *  - sending 'new' to a Java class invokes the constructor and returns a new instance of the class under symbiosis
 *  - all static fields and methods of the Java class are reflected under symbiosis as fields and methods of the AT object
 *  
 * JavaClass instances are pooled (on a per-actor basis): there should exist only one JavaClass instance
 * for each Java class loaded into the JVM. Because the JVM ensures that a Java class
 * can only be loaded once, we can use the Java class wrapped by the JavaClass instance
 * as a unique key to identify its corresponding JavaClass instance.
 *  
 * @author tvcutsem
 */
public final class JavaClass extends NATObject {
	
	/**
	 * A thread-local hashmap pooling all of the JavaClass wrappers for
	 * the current actor, referring to them using WEAK references, such
	 * that unused wrappers can be GC-ed.
	 */
	private static final ThreadLocal _JAVACLASS_POOL_ = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return new HashMap();
        }
	};
	
	public static final JavaClass wrapperFor(Class c) {
		HashMap map = (HashMap) _JAVACLASS_POOL_.get();
		if (map.containsKey(c)) {
			WeakReference ref = (WeakReference) map.get(c);
			JavaClass cls = (JavaClass) ref.get();
			if (cls != null) {
				return cls;
			} else {
				map.remove(c);
				cls = new JavaClass(c);
				map.put(c, new WeakReference(cls));
				return cls;
			}
		} else {
			JavaClass jc = new JavaClass(c);
			map.put(c, new WeakReference(jc));
			return jc;
		}
	}
	
	private final Class wrappedClass_;
	
	/**
	 * A JavaClass wrapping a class c is an object that has the lexical scope as its lexical parent
	 * and has NIL as its dynamic parent.
	 */
	private JavaClass(Class wrappedClass) {
		wrappedClass_ = wrappedClass;
	}
	
	public Class getWrappedClass() { return wrappedClass_; }
	
	public boolean equals(Object other) {
		return ((other instanceof JavaClass) &&
				(wrappedClass_.equals(((JavaClass) other).wrappedClass_)));
	}
	
    /* ------------------------------------------------------
     * - Symbiotic implementation of the ATObject interface -
     * ------------------------------------------------------ */
    
    /**
     * Asynchronous messages sent to a Java object ( o<-m( args )) are scheduled
     * for later execution by the current actor
     */
    public ATNil meta_send(ATAsyncMessage message) throws InterpreterException {
         // TODO: nil <- m() => also do invoke-like deification?
        throw new RuntimeException("Not yet implemented: async message sends to JavaClass");
    }
    
    /**
     * When a method is invoked upon a symbiotic Java class object, the underlying static Java method
     * with the same name as the AmbientTalk selector is invoked. Its arguments are converted
     * into their Java equivalents. Conversely, the result of the method invocation is converted
     * into an AmbientTalk object.
     */
    public ATObject meta_invoke(ATObject receiver, ATSymbol atSelector, ATTable arguments) throws InterpreterException {
        try {
			String jSelector = Reflection.upSelector(atSelector);
			return Symbiosis.symbioticInvocation(this, null, wrappedClass_, jSelector, arguments.asNativeTable().elements_);
		} catch (XSelectorNotFound e) {
			return super.meta_invoke(receiver, atSelector, arguments);
		}
    }
    
    /**
     * A symbiotic Java class object responds to all of the public static selectors of its Java class
     * plus all of the per-instance selectors added to its AmbientTalk symbiont.
     */
    public ATBoolean meta_respondsTo(ATSymbol atSelector) throws InterpreterException {
     	String jSelector = Reflection.upSelector(atSelector);
    	    if (Symbiosis.hasMethod(wrappedClass_, jSelector, true) ||
    	    		Symbiosis.hasField(wrappedClass_, jSelector, true)) {
    		    return NATBoolean._TRUE_;
    	    } else {
    		    return super.meta_respondsTo(atSelector);
    	    }
    }
    
    /**
     * When selecting a field from a symbiotic Java class object, if the object's class
     * has a static field with a matching selector, it is automatically read;
     * if it has methods corresponding to the selector, they are returned in a JavaMethod wrapper,
     * otherwise, the fields of its AT symbiont are checked.
     */
    public ATObject meta_select(ATObject receiver, ATSymbol selector) throws InterpreterException {
        String jSelector = Reflection.upSelector(selector);

        try {
            return Symbiosis.readField(null, wrappedClass_, jSelector);
        } catch (XUndefinedField e) {
    	   		Method[] choices = Symbiosis.getMethods(wrappedClass_, jSelector, true);
    	   		if (choices.length > 0) {
    	   			return new JavaMethod(this, null, choices);
    	   		} else {
    	   			return super.meta_select(receiver, selector);
    	   		}
        }
    }
    
    /**
     * A variable lookup is resolved by first checking whether the wrapped Java class object
     * has an appropriate static field with a matching name. If so, that field's contents are
     * returned. If not, the lookup continues within that Java object's AT symbiont.
     */
    public ATObject meta_lookup(ATSymbol selector) throws InterpreterException {
        try {
        	  String jSelector = Reflection.upSelector(selector);
        	  return Symbiosis.readField(null, wrappedClass_, jSelector);
        } catch(XUndefinedField e) {
        	  return super.meta_lookup(selector);
        }
    }
    
    /**
     * Fields can be defined within a symbiotic Java class object. They are added
     * to its AmbientTalk symbiont, but only if they do not clash with already
     * existing field names.
     */
    public ATNil meta_defineField(ATSymbol name, ATObject value) throws InterpreterException {
        if (Symbiosis.hasField(wrappedClass_, Reflection.upSelector(name), true)) {
        	    throw new XDuplicateSlot("field ", name.toString());
        } else {
        	    return super.meta_defineField(name, value);
        }
    }
    
    /**
     * Variables can be assigned within a symbiotic Java class object if that class object
     * has a mutable static field with a matching name. If not, the field assignment is delegated
     * to its AT symbiont.
     */
    public ATNil meta_assignVariable(ATSymbol name, ATObject value) throws InterpreterException {
        try {
        	   String jSelector = Reflection.upSelector(name);
        	   Symbiosis.writeField(null, wrappedClass_, jSelector, value);
        	   return NATNil._INSTANCE_;
		} catch (XUndefinedField e) {
			return super.meta_assignVariable(name, value);
		}
    }
    
    /**
     * Fields can be assigned within a symbiotic Java class object if that class
     * has a mutable field with a matching name. If not, the field assignment is delegated
     * to its AT symbiont.
     */
    public ATNil meta_assignField(ATObject receiver, ATSymbol name, ATObject value) throws InterpreterException {
        try {
        	   String jSelector = Reflection.upSelector(name);
        	   Symbiosis.writeField(null, wrappedClass_, jSelector, value);
        	   return NATNil._INSTANCE_;
		} catch (XUndefinedField e) {
			return super.meta_assignField(receiver, name, value);
		}
    }
    
	/**
	 * Symbiotic Java class objects are singletons.
	 */
	public ATObject meta_clone() throws InterpreterException { return this; }
	
	/**
	 * aJavaClass.new(@args) == aJavaClass.init(@args)
	 * AmbientTalk objects can add a custom init method to the class in order to intercept
	 * instance creation. The original instance can then be created by invoking super.init(@args).
	 * 
	 * For example, imagine we want to extend the class java.lang.Point with a 3D coordinate, e.g. a 'z' field:
	 * <tt>
	 * def Point := jlobby.java.awt.Point;
	 * def Point.init(x,y,z) {
	 *   def point := super.init(x,y); // invokes the Java constructor
	 *   def point.z := z; // adds a field dynamically to the new JavaObject wrapper
	 *   point; // important! the return value of init determines the return value of 'new'
	 * }
	 * def mypoint := Point.new(1,2,3);
	 * </tt>
	 */
    public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
    	    // immediately perform the NATObject behaviour: don't look for an 'init' method in the Java class,
    	    // only look for an 'init' method at the AmbientTalk level.
    	    return super.meta_invoke(this, Evaluator._INIT_, initargs);
    }
    
    /**
     * Methods can be added to a symbiotic Java class object provided they do not already
     * exist in the Java class.
     */
    public ATNil meta_addMethod(ATMethod method) throws InterpreterException {
        ATSymbol name = method.base_getName();
        if (Symbiosis.hasMethod(wrappedClass_, Reflection.upSelector(name), true)) {
    	        throw new XDuplicateSlot("method ", name.toString());
        } else {
    	        return super.meta_addMethod(method);
        }
    }

    /**
     * Fields can be grabbed from a symbiotic Java class object. Fields that correspond
     * to static fields in the Java class are returned as JavaField instances.
     */
    public ATField meta_grabField(ATSymbol fieldName) throws InterpreterException {
        try {
        	  return new JavaField(null, Symbiosis.getField(wrappedClass_, Reflection.upSelector(fieldName), true));
        } catch(XUndefinedField e) {
        	  return super.meta_grabField(fieldName);
        }
    }

    /**
     * Methods can be grabbed from a symbiotic Java class object. Methods that correspond
     * to static methods in the Java class are returned as JavaMethod instances.
     */
    public ATMethod meta_grabMethod(ATSymbol methodName) throws InterpreterException {
        Method[] choices = Symbiosis.getMethods(wrappedClass_, Reflection.upSelector(methodName), true);
        if (choices.length > 0) {
        		return new JavaMethod(this, null, choices);
        } else {
        	    return super.meta_grabMethod(methodName);
        }
    }

    /**
     * Querying a symbiotic Java class object for its fields results in a table containing
     * both 'native' static Java fields and the fields of its AT symbiont
     */
    public ATTable meta_listFields() throws InterpreterException {
		// instance fields of the wrapped object's class
		JavaField[] jFields = Symbiosis.getAllFields(null, wrappedClass_);
        // fields of the AT symbiont
    		ATObject[] symbiontFields = super.meta_listFields().asNativeTable().elements_;
    		return new NATTable(NATTable.collate(jFields, symbiontFields));
    }

    /**
     * Querying a symbiotic Java class object for its methods results in a table containing
     * both 'native' static Java methods and the methods of its AT symbiont
     */
    public ATTable meta_listMethods() throws InterpreterException {
		// instance methods of the wrapped object's class
		JavaMethod[] jMethods = Symbiosis.getAllMethods(this, null, wrappedClass_);
        // methods of the AT symbiont
		ATObject[] symbiontMethods = super.meta_listMethods().asNativeTable().elements_;
		return new NATTable(NATTable.collate(jMethods, symbiontMethods));
    }

	public ATBoolean meta_isCloneOf(ATObject original) throws InterpreterException {
		return NATBoolean.atValue(this == original);
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<java:"+wrappedClass_.toString()+">");
	}
	
    /**
     * See JavaClass#meta_newInstance(ATTable) for more details about the behaviour
     * of Java class instantiation in AmbientTalk.
     */
    public ATObject base_init(ATObject[] initargs) throws InterpreterException {
    		return Symbiosis.symbioticInstanceCreation(wrappedClass_, initargs);
    }
	
    public JavaClass asJavaClassUnderSymbiosis() throws XTypeMismatch { return this; }

}
