/**
 * AmbientTalk/2 Project
 * JavaObject.java created on 3-nov-2006 at 11:32:48
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

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.HashMap;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XDuplicateSlot;
import edu.vub.at.exceptions.XIllegalOperation;
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
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

/**
 * JavaObject instances represent java objects under symbiosis.
 * A Java Object is represented in AmbientTalk as an AmbientTalk object where:
 *  - the Java Object's fields and methods correspond to the AmbientTalk object's fields and methods
 *  - overloaded methods are coerced into a single AmbientTalk method, which must be disambiguated at call site
 *  - the Java Object has a shares-a relation with its class (i.e. the class of the object is its dynamic parent)
 * 
 * In addition, a JavaObject carries with it a static type. The static type is used during method dispatch
 * to select the appropriate Java method to be invoked. A JavaObject's static type can be altered by casting it.
 * Casting can be done using JavaClass instances.
 * 
 * @author tvcutsem
 */
public final class JavaObject extends NATObject implements ATObject {

	
	/**
	 * A thread-local hashmap pooling all of the JavaObject wrappers for
	 * the current actor, referring to them using WEAK references, such
	 * that unused wrappers can be GC-ed.
	 */
	private static final ThreadLocal _JAVAOBJECT_POOL_ = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return new HashMap();
        }
	};
	
	public static final JavaObject wrapperFor(Object o) {
		HashMap map = (HashMap) _JAVAOBJECT_POOL_.get();
		if (map.containsKey(o)) {
			WeakReference ref = (WeakReference) map.get(o);
			JavaObject obj = (JavaObject) ref.get();
			if (obj != null) {
				return obj;
			} else {
				map.remove(obj);
				obj = new JavaObject(o);
				map.put(o, new WeakReference(obj));
				return obj;
			}
		} else {
			JavaObject jo = new JavaObject(o);
			map.put(o, new WeakReference(jo));
			return jo;
		}
	}
	
	private final Object wrappedObject_;
	
	/**
	 * A JavaObject wrapping an object o has a dynamic SHARES-A parent pointing to the
	 * wrapper of o's class.
	 */
	private JavaObject(Object wrappedObject) {
		super(JavaClass.wrapperFor(wrappedObject.getClass()), NATObject._SHARES_A_);
		wrappedObject_ = wrappedObject;
	}

	public Object getWrappedObject() {
		return wrappedObject_;
	}
	
	public boolean isJavaObjectUnderSymbiosis() {
		return true;
	}
	
    public JavaObject asJavaObjectUnderSymbiosis() throws XTypeMismatch {
	    return this;
    }
    
    /* ------------------------------------------------------
     * - Symbiotic implementation of the ATObject interface -
     * ------------------------------------------------------ */
    
    /**
     * When a method is invoked upon a symbiotic Java object, the underlying Java method
     * with the same name as the AmbientTalk selector is invoked. Its arguments are converted
     * into their Java equivalents. Conversely, the result of the method invocation is converted
     * into an AmbientTalk object.
     */
    public ATObject meta_invoke(ATObject receiver, ATSymbol atSelector, ATTable arguments) throws InterpreterException {
        try {
			String jSelector = Reflection.upSelector(atSelector);
			return Symbiosis.symbioticInvocation(this, wrappedObject_, wrappedObject_.getClass(), jSelector, arguments.asNativeTable().elements_);
		} catch (XSelectorNotFound e) {
			return super.meta_invoke(receiver, atSelector, arguments);
		}
    }
    
    /**
     * A symbiotic Java object responds to all of the public non-static selectors of its Java class
     * plus all of the per-instance selectors added to its AmbientTalk symbiont.
     */
    public ATBoolean meta_respondsTo(ATSymbol atSelector) throws InterpreterException {
     	String jSelector = Reflection.upSelector(atSelector);
    	    if (Symbiosis.hasMethod(wrappedObject_.getClass(), jSelector, false) ||
    	    		Symbiosis.hasField(wrappedObject_.getClass(), jSelector, false)) {
    		    return NATBoolean._TRUE_;
    	    } else {
    		    return super.meta_respondsTo(atSelector);
    	    }
    }
    
    /**
     * When selecting a field from a symbiotic Java object, if the object's class
     * has a non-static field with a matching selector, it is automatically read;
     * if it has a corresponding method, the method is returned in a closure,
     * otherwise, the fields of its AT symbiont are checked.
     */
    public ATObject meta_select(ATObject receiver, ATSymbol selector) throws InterpreterException {
        String jSelector = Reflection.upSelector(selector);

        try {
            return Symbiosis.readField(wrappedObject_, wrappedObject_.getClass(), jSelector);
        } catch (XUndefinedField e) {
        	    Method[] choices = Symbiosis.getMethods(wrappedObject_.getClass(), jSelector, false);
        	    if (choices.length > 0) {
        	     	return new NativeClosure(receiver, new JavaMethod(this, wrappedObject_, choices));
        	    } else {
        	      	return super.meta_select(receiver, selector);
        	    }
        }
    }
    
    /**
     * A variable lookup is resolved by first checking whether the wrapped Java object
     * has an appropriate field with a matching name. If so, that field's contents are
     * returned. If not, the lookup continues within that Java object's AT symbiont.
     */
    public ATObject meta_lookup(ATSymbol selector) throws InterpreterException {
        try {
        	  String jSelector = Reflection.upSelector(selector);
        	  return Symbiosis.readField(wrappedObject_, wrappedObject_.getClass(), jSelector);
        } catch(XUndefinedField e) {
        	  return super.meta_lookup(selector);
        }
    }
    
    /**
     * Fields can be defined within a symbiotic Java object. They are added
     * to its AmbientTalk symbiont, but only if they do not clash with already
     * existing field names.
     */
    public ATNil meta_defineField(ATSymbol name, ATObject value) throws InterpreterException {
        if (Symbiosis.hasField(wrappedObject_.getClass(), Reflection.upSelector(name), false)) {
        	    throw new XDuplicateSlot("field ", name.toString());
        } else {
        	    return super.meta_defineField(name, value);
        }
    }
    
    /**
     * Variables can be assigned within a symbiotic Java object if that object's class
     * has a mutable field with a matching name. If not, the field assignment is delegated
     * to its AT symbiont.
     */
    public ATNil meta_assignVariable(ATSymbol name, ATObject value) throws InterpreterException {
        try {
        	   String jSelector = Reflection.upSelector(name);
        	   Symbiosis.writeField(wrappedObject_, wrappedObject_.getClass(), jSelector, value);
        	   return NATNil._INSTANCE_;
		} catch (XUndefinedField e) {
			return super.meta_assignVariable(name, value);
		}
    }
    
    /**
     * Fields can be assigned within a symbiotic Java object if that object's class
     * has a mutable field with a matching name. If not, the field assignment is delegated
     * to its AT symbiont.
     */
    public ATNil meta_assignField(ATObject receiver, ATSymbol name, ATObject value) throws InterpreterException {
        try {
        	   String jSelector = Reflection.upSelector(name);
        	   Symbiosis.writeField(wrappedObject_, wrappedObject_.getClass(), jSelector, value);
        	   return NATNil._INSTANCE_;
		} catch (XUndefinedField e) {
			return super.meta_assignField(receiver, name, value);
		}
    }
    
	public ATObject meta_clone() throws InterpreterException {
		throw new XIllegalOperation("Cannot clone Java object under symbiosis: " + wrappedObject_.toString());
	}
	
	/**
	 * Invoking new on a JavaObject will exhibit the same behaviour as if new was invoked on the parent class.
	 */
    public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
    	    return dynamicParent_.meta_newInstance(initargs);
    }
    
    /**
     * Methods can be added to a symbiotic Java object provided they do not already
     * exist in the Java object's class.
     */
    public ATNil meta_addMethod(ATMethod method) throws InterpreterException {
        ATSymbol name = method.base_getName();
        if (Symbiosis.hasMethod(wrappedObject_.getClass(), Reflection.upSelector(name), false)) {
    	        throw new XDuplicateSlot("method ", name.toString());
        } else {
    	        return super.meta_addMethod(method);
        }
    }

    /**
     * Fields can be grabbed from a symbiotic Java object. Fields that correspond
     * to fields in the Java object's class are returned as JavaField instances.
     */
    public ATField meta_grabField(ATSymbol fieldName) throws InterpreterException {
        try {
        	  return new JavaField(wrappedObject_, Symbiosis.getField(wrappedObject_.getClass(), Reflection.upSelector(fieldName), false));
        } catch(XUndefinedField e) {
        	  return super.meta_grabField(fieldName);
        }
    }

    /**
     * Methods can be grabbed from a symbiotic Java object. Methods that correspond
     * to methods in the Java object's class are returned as JavaMethod instances.
     */
    public ATMethod meta_grabMethod(ATSymbol methodName) throws InterpreterException {
        Method[] choices = Symbiosis.getMethods(wrappedObject_.getClass(), Reflection.upSelector(methodName), false);
        if (choices.length > 0) {
        		return new JavaMethod(this, wrappedObject_, choices);
        } else {
        	    return super.meta_grabMethod(methodName);
        }
    }

    /**
     * Querying a symbiotic Java object for its fields results in a table containing
     * both 'native' Java fields and the fields of its AT symbiont
     */
    public ATTable meta_listFields() throws InterpreterException {
		// instance fields of the wrapped object's class
		JavaField[] jFields = Symbiosis.getAllFields(wrappedObject_, wrappedObject_.getClass());
        // fields of the AT symbiont
    		ATObject[] symbiontFields = super.meta_listFields().asNativeTable().elements_;
    		return NATTable.atValue(NATTable.collate(jFields, symbiontFields));
    }

    /**
     * Querying a symbiotic Java object for its methods results in a table containing
     * both 'native' Java methods and the methods of its AT symbiont
     */
    public ATTable meta_listMethods() throws InterpreterException {
		// instance methods of the wrapped object's class
		JavaMethod[] jMethods = Symbiosis.getAllMethods(this, wrappedObject_, wrappedObject_.getClass());
        // methods of the AT symbiont
		ATObject[] symbiontMethods = super.meta_listMethods().asNativeTable().elements_;
		return NATTable.atValue(NATTable.collate(jMethods, symbiontMethods));
    }

	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<java:"+wrappedObject_.toString()+">");
	}
	
}
