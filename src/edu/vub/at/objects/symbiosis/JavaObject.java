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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XDuplicateSlot;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.exceptions.XUnassignableField;
import edu.vub.at.exceptions.XUndefinedSlot;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATAssignmentSymbol;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGAssignmentSymbol;
import edu.vub.util.IdentityHashMap;

import java.io.Serializable;
import java.lang.ref.SoftReference;

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
	 * A thread-local identity hashmap pooling all of the JavaObject wrappers for
	 * the current actor, referring to them using SOFT references, such
	 * that unused wrappers can be GC-ed when running low on memory.
	 * 
	 * Note that the use of an identity hashmap rather than a normal hashmap
	 * is crucial here! Using a normal hashmap compares objects by means of their
	 * equals method, which means that two distinct Java objects could be assigned
	 * the same wrapper, which is obviously unwanted. Using an identity hashmap
	 * avoids this.
	 */
	private static final ThreadLocal _JAVAOBJECT_POOL_ = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return new IdentityHashMap();
        }
	};
	
	/**
	 * Return a unique appearance for the Java object.
	 */
	public static final JavaObject wrapperFor(Object o) {
		IdentityHashMap map = (IdentityHashMap) _JAVAOBJECT_POOL_.get();
		if (map.containsKey(o)) {
			SoftReference ref = (SoftReference) map.get(o);
			JavaObject obj = (JavaObject) ref.get();
			if (obj != null) {
				return obj;
			} else {
				map.remove(obj);
				obj = new JavaObject(o);
				map.put(o, new SoftReference(obj));
				return obj;
			}
		} else {
			JavaObject jo = new JavaObject(o);
			map.put(o, new SoftReference(jo));
			return jo;
		}
	}
	
	private final Object wrappedObject_;
	
	/**
	 * A JavaObject wrapping an object o has a dynamic SHARES-A parent pointing to the
	 * wrapper of o's class.
	 * 
	 * A symbiotic Java object is tagged with all of the Java interface
	 * type tags that correspond to the interface types implemented by the
	 * wrapped Java object's class.
	 */
	private JavaObject(Object wrappedObject) {
		super(JavaClass.wrapperFor(wrappedObject.getClass()), NATObject._SHARES_A_);
		wrappedObject_ = wrappedObject;
		
		// initialize the Java symbiotic object's type tags
		Class[] extendedInterfaces = wrappedObject_.getClass().getInterfaces();
		if (extendedInterfaces.length > 0) {
			typeTags_ = new ATTypeTag[extendedInterfaces.length];
			for (int i = 0; i < extendedInterfaces.length; i++) {
				typeTags_[i] = JavaClass.wrapperFor(extendedInterfaces[i]);
			}	
		}
	}

	/**
	 * @return the Java object denoted by this JavaObject
	 */
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
	 * Implements symbiotic slot (field or method) access.
	 * 
	 * This method is an implementation-level method that needs to be supported by any AmbientTalk object,
	 * although it is not part of the metaobject protocol. Therefore it has no meta_ but an impl_ prefix.
	 * <p>
     * When a method is invoked upon a symbiotic Java object, an underlying Java method
     * with the same name as the AmbientTalk selector is invoked. Its arguments are converted
     * into their Java equivalents. Conversely, the result of the method invocation is converted
     * into an AmbientTalk object. If no such method exists, a method is searched for in the
     * symbiotic AmbientTalk part.
	 * <p>
	 * Slot access proceeds as follows:
	 *  - if selector is bound to a Java method, the method is invoked.
	 *  - if selector is bound to a Java field, the field is treated as a nullary closure (UAP).
	 *  - otherwise the slot access is performed on the AmbientTalk symbiont.
	 * @see NATObject#impl_accessSlot(ATObject, ATSymbol, ATTable)
	 */
	public ATObject impl_accessSlot(ATObject receiver, ATSymbol atSelector, ATTable arguments) throws InterpreterException {
		String jSelector = Reflection.upSelector(atSelector);
        try {
        	// first try to invoke a method with the given name
			return Symbiosis.symbioticInvocation(
					this, wrappedObject_, wrappedObject_.getClass(), jSelector,
					arguments.asNativeTable().elements_);
		} catch (XSelectorNotFound e) {
			e.catchOnlyIfSelectorEquals(atSelector);
			// next, try field access
			if (Symbiosis.hasField(wrappedObject_.getClass(), jSelector, false)) {
				NativeClosure.checkNullaryArguments(atSelector, arguments);
				return Symbiosis.readField(wrappedObject_, wrappedObject_.getClass(), jSelector);
			} else {
                // if no method or field matches, look in the AmbientTalk symbiont
				return super.impl_accessSlot(receiver, atSelector, arguments);
			}
		}
	}
	
	/**
	 * Implements slot assignment. This method expects its selector to be an {@link AGAssignmentSymbol}
	 * which either represents a method directly, or represents field assignment implicitly.
	 * 
	 * This method is an implementation-level method that needs to be supported by any AmbientTalk object,
	 * although it is not part of the metaobject protocol. Therefore it has no meta_ but an impl_ prefix.
	 * 
	 * Slot mutation proceeds as follows:
	 *  - if selector is bound to a method, the method is invoked.
	 *  - if selector \ { := } is bound to a field, that field is assigned to the given value
	 *  (this implements the uniform access principle).
	 *  - otherwise, the slot mutation is carried out in the parent object.
	 */
	public ATObject impl_mutateSlot(ATObject receiver, ATAssignmentSymbol atSelector, ATTable arguments) throws InterpreterException {
		String jSelector = Reflection.upSelector(atSelector);
        try {
        	// first try to invoke a method with the given name
			return Symbiosis.symbioticInvocation(
					this, wrappedObject_, wrappedObject_.getClass(), jSelector,
					arguments.asNativeTable().elements_);
		} catch (XSelectorNotFound e) {
			e.catchOnlyIfSelectorEquals(atSelector);
			// next, try field assignment
			jSelector = Reflection.upSelector(atSelector.getFieldName());
			if (Symbiosis.hasField(wrappedObject_.getClass(), jSelector, false)) {
				ATObject val = NativeClosure.checkUnaryArguments(atSelector, arguments);
				Symbiosis.writeField(wrappedObject_, wrappedObject_.getClass(), jSelector, val);
				return val;
			} else {
                // if no method or field matches, look in the AmbientTalk symbiont
				return super.impl_mutateSlot(receiver, atSelector, arguments);
			}
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
     * When selecting a field from a symbiotic Java object, if the
     * Java symbiont object's class has a non-static field with a matching selector,
     * it is automatically read; if it has a corresponding method, the method is returned
     * in a closure. If no matching field is found, the fields and methods of the
     * AmbientTalk symbiont are checked.
     */
    public ATClosure meta_select(ATObject receiver, ATSymbol selector) throws InterpreterException {
    	final String jSelector = Reflection.upSelector(selector);
    	try {
   			ATObject val = Symbiosis.readField(wrappedObject_, wrappedObject_.getClass(), jSelector);
   			if (val.meta_isTaggedAs(NativeTypeTags._CLOSURE_).asNativeBoolean().javaValue) {
   				return val.asClosure();
   			} else {
   				return new NativeClosure.Accessor(selector,this) {
   					public ATObject access() throws InterpreterException {
   						return Symbiosis.readField(wrappedObject_, wrappedObject_.getClass(), jSelector);
   					}
   				};
   			}
    	} catch(XUndefinedSlot e) {
       	    JavaMethod choices = Symbiosis.getMethods(wrappedObject_.getClass(), jSelector, false);
       	    if (choices != null) {
       	     	return new JavaClosure(this, choices);
       	    } else {
       	    	return super.meta_select(receiver, selector);
       	    }
    	}
    }
    
    /**
     * When a slot (field or method) is selected from a symbiotic Java object, the
     * selection proceeds as follows:
     *  - if the selector matches a non-static Java method, a closure wrapping that method is returned.
     *  - if the selector matches a non-static Java field, a nullary closure wrapping the field access
     *    is returned (UAP)
     *  - otherwise, the selector is looked up in the AmbientTalk symbiont.
     */
	public ATClosure impl_selectAccessor(ATObject receiver, ATSymbol atSelector) throws InterpreterException {
		final String jSelector = Reflection.upSelector(atSelector);
		try {
			ATObject val = Symbiosis.readField(wrappedObject_, wrappedObject_.getClass(), jSelector);
			if (val.meta_isTaggedAs(NativeTypeTags._CLOSURE_).asNativeBoolean().javaValue) {
				return val.asClosure();
			} else {
				return new NativeClosure.Accessor(atSelector, this) {
					public ATObject access() throws InterpreterException {
						return Symbiosis.readField(wrappedObject_, wrappedObject_.getClass(), jSelector);
					}
				};
			}
		} catch(XUndefinedSlot e) {
			JavaMethod choices = Symbiosis.getMethods(wrappedObject_.getClass(), jSelector, false);
			if (choices != null) {
				return new JavaClosure(this, choices);
			} else {
				return super.impl_selectAccessor(receiver, atSelector);
			}
		}
	}
	
    /**
     * When a slot (field or method) is selected from a symbiotic Java object, the
     * selection proceeds as follows:
     *  - if the selector matches a non-static Java method, a closure wrapping that method is returned.
     *  - if the selector matches a non-static Java field, a unary closure wrapping the field mutation
     *    is returned (UAP)
     *  - otherwise, the selector is looked up in the AmbientTalk symbiont.
     */
	public ATClosure impl_selectMutator(ATObject receiver, ATAssignmentSymbol atSelector) throws InterpreterException {
		final String jFieldSelector = Reflection.upSelector(atSelector.getFieldName());
		if (Symbiosis.hasField(wrappedObject_.getClass(), jFieldSelector, false)) {
			return new NativeClosure.Mutator(atSelector, this) {
				public ATObject mutate(ATObject val) throws InterpreterException {
					Symbiosis.writeField(wrappedObject_, wrappedObject_.getClass(), jFieldSelector, val);
					return val;
				}
			};
		} else {
			String jMethodSelector = Reflection.upSelector(atSelector);
			JavaMethod choices = Symbiosis.getMethods(wrappedObject_.getClass(), jMethodSelector, false);
			if (choices != null) {
				return new JavaClosure(this, choices);
			} else {
				return super.impl_selectMutator(receiver, atSelector);
			}
		}
	}
    
    /**
     * A variable lookup is resolved by first checking whether the Java object has a field with
     * a matching name. If not, the symbiotic AmbientTalk object is checked.
     */
    public ATObject meta_lookup(ATSymbol selector) throws InterpreterException {
        try {
        	String jSelector = Reflection.upSelector(selector);
      	    return Symbiosis.readField(wrappedObject_, wrappedObject_.getClass(), jSelector);
        } catch(XUndefinedSlot e) {
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
        	throw new XDuplicateSlot(name);
        } else {
        	return super.meta_defineField(name, value);
        }
    }
    
    /**
     * Variables can be assigned within a symbiotic Java object if that object's class
     * has a mutable field with a matching name.
     */
    public ATNil meta_assignVariable(ATSymbol name, ATObject value) throws InterpreterException {
        try {
        	String jSelector = Reflection.upSelector(name);
        	Symbiosis.writeField(wrappedObject_, wrappedObject_.getClass(), jSelector, value);
        	return NATNil._INSTANCE_;
		} catch (XUnassignableField e) {
			return super.meta_assignVariable(name, value);
		}
    }
    
    /**
     * Fields can be assigned within a symbiotic Java object if that object's class
     * has a mutable field with a matching name. Field assignment is first resolved
     * in the wrapped Java object and afterwards in the AT symbiont.
     * @deprecated use invoke with assignment symbol (UAP) instead
     */
    public ATNil meta_assignField(ATObject receiver, ATSymbol name, ATObject value) throws InterpreterException {
        try {
     	    String jSelector = Reflection.upSelector(name);
    	    Symbiosis.writeField(wrappedObject_, wrappedObject_.getClass(), jSelector, value);
    	    return NATNil._INSTANCE_;
		} catch (XUnassignableField e) {
			return super.meta_assignField(receiver, name, value);
		}
    }
    
    /**
     * Cloning a symbiotic object is not always possible as Java has no uniform cloning semantics.
     * Even if the symbiotic object implements java.lang.Cloneable, a clone cannot be made of
     * the wrapped object as java.lang.Object's clone method is protected, and must be overridden
     * by a public clone method in the cloneable subclass.
     */
	public ATObject meta_clone() throws InterpreterException {
		throw new XIllegalOperation("Cannot clone Java object under symbiosis: " + wrappedObject_.toString());
	}
	
	/**
	 * Invoking new on a JavaObject will exhibit the same behaviour as if new was invoked on the parent class.
	 */
    public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
    	return base_super().meta_newInstance(initargs);
    }
    
    /**
     * Methods can be added to a symbiotic Java object provided they do not already
     * exist in the Java object's class.
     */
    public ATNil meta_addMethod(ATMethod method) throws InterpreterException {
        ATSymbol name = method.base_name();
        if (Symbiosis.hasMethod(wrappedObject_.getClass(), Reflection.upSelector(name), false)) {
    	    throw new XDuplicateSlot(name);
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
        	return new JavaField(wrappedObject_,
		             Symbiosis.getField(wrappedObject_.getClass(), Reflection.upSelector(fieldName), false));
        } catch(XUndefinedSlot e) {
        	return super.meta_grabField(fieldName);
        }
    }

    /**
     * Methods can be grabbed from a symbiotic Java object. Methods that correspond
     * to methods in the Java object's class are returned as JavaMethod instances.
     */
    public ATMethod meta_grabMethod(ATSymbol methodName) throws InterpreterException {
        JavaMethod choices = Symbiosis.getMethods(wrappedObject_.getClass(), Reflection.upSelector(methodName), false);
        if (choices != null) {
        	return choices;
        } else {
        	return super.meta_grabMethod(methodName);
        }
    }

    /**
     * Querying a symbiotic Java object for its fields results in a table containing
     * both the 'native' Java fields and the fields of its AT symbiont
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
     * both all 'native' Java instance methods and the methods of its AT symbiont 
     */
    public ATTable meta_listMethods() throws InterpreterException {
		// instance methods of the wrapped object's class
		JavaMethod[] jMethods = Symbiosis.getAllMethods(wrappedObject_.getClass(), false);
        // methods of the AT symbiont
		ATObject[] symbiontMethods = super.meta_listMethods().asNativeTable().elements_;
		return NATTable.atValue(NATTable.collate(jMethods, symbiontMethods));
    }
    
	public ATBoolean meta_isCloneOf(ATObject original) throws InterpreterException {
		return NATBoolean.atValue(this == original);
	}

	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<java:"+wrappedObject_.toString()+">");
	}
	
	/**
	 * Passing a Java Object wrapper to another actor has the following effect:
	 *  - if the wrapped Java object is serializable, the symbiotic AmbientTalk object
	 *    is treated as by copy (i.e. as an isolate).
	 *  - if the wrapped Java object is not serializable, the symbiotic AmbientTalk object
	 *    is treated as by reference and a far reference will be passed instead.
	 */
	public ATObject meta_pass() throws InterpreterException {
		if (wrappedObject_ instanceof Serializable) {
			return this;
		} else {
			return super.meta_pass();
		}
	}
	
	/**
	 * If the wrapped object was serializable, we may be asked to resolve ourselves.
	 */
	public ATObject meta_resolve() throws InterpreterException {
		if (wrappedObject_ instanceof Serializable) {
			return this;
		} else {
			return super.meta_resolve();
		}
	}
	
}
