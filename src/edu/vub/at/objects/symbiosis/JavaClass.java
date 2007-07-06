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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XDuplicateSlot;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.exceptions.XUnassignableField;
import edu.vub.at.exceptions.XUndefinedSlot;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
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
import edu.vub.at.objects.mirrors.PrimitiveMethod;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGAssignmentSymbol;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.at.util.logging.Logging;
import edu.vub.util.IdentityHashMap;

import java.lang.ref.SoftReference;

/**
 * A JavaClass instance represents a Java Class under symbiosis.
 * 
 * Java classes are treated as AmbientTalk 'singleton' objects:
 * 
 *  - cloning a Java class results in the same Java class instance
 *  - sending 'new' to a Java class invokes the constructor and returns a new instance of the class under symbiosis
 *  - all static fields and methods of the Java class are reflected under symbiosis as fields and methods of the AT object
 *  
 * A Java Class object that represents an interface can furthermore be used
 * as an AmbientTalk type. The type's name corresponds to the interface's full name.
 *  
 * JavaClass instances are pooled (on a per-actor basis): there should exist only one JavaClass instance
 * for each Java class loaded into the JVM. Because the JVM ensures that a Java class
 * can only be loaded once, we can use the Java class wrapped by the JavaClass instance
 * as a unique key to identify its corresponding JavaClass instance.
 *  
 * @author tvcutsem
 */
public final class JavaClass extends NATObject implements ATTypeTag {
	
	/**
	 * A thread-local hashmap pooling all of the JavaClass wrappers for
	 * the current actor, referring to them using SOFT references, such
	 * that unused wrappers can be GC-ed when running low on memory.
	 */
	private static final ThreadLocal _JAVACLASS_POOL_ = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return new IdentityHashMap();
        }
	};
	
	/**
	 * Allocate a unique symbiont object for the given Java class.
	 */
	public static final JavaClass wrapperFor(Class c) {
		IdentityHashMap map = (IdentityHashMap) _JAVACLASS_POOL_.get();
		if (map.containsKey(c)) {
			SoftReference ref = (SoftReference) map.get(c);
			JavaClass cls = (JavaClass) ref.get();
			if (cls != null) {
				return cls;
			} else {
				map.remove(c);
				cls = new JavaClass(c);
				map.put(c, new SoftReference(cls));
				return cls;
			}
		} else {
			JavaClass jc = new JavaClass(c);
			map.put(c, new SoftReference(jc));
			return jc;
		}
	}
	
	// primitive fields and method of a JavaClass wrapper
	
	private static final AGSymbol _PTS_NAME_ = AGSymbol.jAlloc("parentTypes");
	private static final AGSymbol _TNM_NAME_ = AGSymbol.jAlloc("typeName");
	
	/** def isSubtypeOf(type) { nil } */
	private static final PrimitiveMethod _PRIM_STP_ = new PrimitiveMethod(
			AGSymbol.jAlloc("isSubtypeOf"), NATTable.atValue(new ATObject[] { AGSymbol.jAlloc("type")})) {
		public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
			if (!arguments.base_length().equals(NATNumber.ONE)) {
				throw new XArityMismatch("isSubtypeOf", 1, arguments.base_length().asNativeNumber().javaValue);
			}
			return ctx.base_lexicalScope().asJavaClassUnderSymbiosis().base_isSubtypeOf(arguments.base_at(NATNumber.ONE).asTypeTag());
		}
	};
	
	private final Class wrappedClass_;
	
	/**
	 * A JavaClass wrapping a class c is an object that has the lexical scope as its lexical parent
	 * and has NIL as its dynamic parent.
	 * 
	 * If the JavaClass wraps a Java interface type, JavaClass instances are
	 * also types.
	 */
	private JavaClass(Class wrappedClass) {
		super(wrappedClass.isInterface() ?
			  new ATTypeTag[] { NativeTypeTags._TYPETAG_ } :
			  NATObject._NO_TYPETAGS_);
		wrappedClass_ = wrappedClass;
		
		// add the two fields and one method needed for an ATTypeTag
		if (wrappedClass.isInterface()) {
			Class[] extendedInterfaces = wrappedClass_.getInterfaces();
			ATObject[] types = new ATObject[extendedInterfaces.length];
			for (int i = 0; i < extendedInterfaces.length; i++) {
				types[i] = JavaClass.wrapperFor(extendedInterfaces[i]);
			}
			
			try {
				super.meta_defineField(_PTS_NAME_, NATTable.atValue(types));
				super.meta_defineField(_TNM_NAME_, AGSymbol.jAlloc(wrappedClass_.getName()));
				super.meta_addMethod(_PRIM_STP_);
			} catch (InterpreterException e) {
				Logging.Actor_LOG.fatal("Error while initializing Java Class as type tag: " + wrappedClass.getName(), e);
			}
		}
	}
	
	/** return the class object denoted by this AmbientTalk symbiont */
	public Class getWrappedClass() { return wrappedClass_; }
	
	public JavaClass asJavaClassUnderSymbiosis() throws XTypeMismatch { return this; }
	
    public ATBoolean base__opeql__opeql_(ATObject comparand) throws InterpreterException {
        return NATBoolean.atValue(this.equals(comparand));
    }
	
	public boolean equals(Object other) {
		return ((other instanceof JavaClass) &&
				(wrappedClass_.equals(((JavaClass) other).wrappedClass_)));
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
     * When a method is invoked upon a symbiotic Java class object, an underlying static Java method
     * with the same name as the AmbientTalk selector is invoked. Its arguments are converted
     * into their Java equivalents. Conversely, the result of the method invocation is converted
     * into an AmbientTalk object. If no such method exists, a method is searched for in the
     * symbiotic AmbientTalk part.
	 * <p>
	 * Slot access proceeds as follows:
	 *  - if selector is bound to a static Java method, the method is invoked.
	 *  - if selector is bound to a static Java field, the field is treated as a nullary closure (UAP).
	 *  - otherwise the slot access is performed on the AmbientTalk symbiont.
	 * @see NATObject#impl_accessSlot(ATObject, ATSymbol, ATTable)
	 */
	public ATObject impl_accessSlot(ATObject receiver, ATSymbol atSelector, ATTable arguments) throws InterpreterException {
		String jSelector = Reflection.upSelector(atSelector);
        try {
        	// first try to invoke a method with the given name
			return Symbiosis.symbioticInvocation(
					this, null, wrappedClass_, jSelector, arguments.asNativeTable().elements_);
		} catch (XSelectorNotFound e) {
			e.catchOnlyIfSelectorEquals(atSelector);
			// next, try field access
			if (Symbiosis.hasField(wrappedClass_, jSelector, true)) {
				NativeClosure.checkNullaryArguments(atSelector, arguments);
				return Symbiosis.readField(null, wrappedClass_, jSelector);
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
	 *  - if selector is bound to a static Java method, the method is invoked.
	 *  - if selector \ { := } is bound to a static Java field, that field is assigned to the given value
	 *  (this implements the uniform access principle).
	 *  - otherwise, the slot mutation is carried out in the parent object.
	 */
	public ATObject impl_mutateSlot(ATObject receiver, ATAssignmentSymbol atSelector, ATTable arguments) throws InterpreterException {
		String jSelector = Reflection.upSelector(atSelector);
        try {
        	// first try to invoke a method with the given name
			return Symbiosis.symbioticInvocation(
					this, null, wrappedClass_, jSelector,
					arguments.asNativeTable().elements_);
		} catch (XSelectorNotFound e) {
			e.catchOnlyIfSelectorEquals(atSelector);
			// next, try field assignment
			jSelector = Reflection.upSelector(atSelector.getFieldName());
			if (Symbiosis.hasField(wrappedClass_, jSelector, true)) {
				ATObject val = NativeClosure.checkUnaryArguments(atSelector, arguments);
				Symbiosis.writeField(null, wrappedClass_, jSelector, val);
				return val;
			} else {
                // if no method or field matches, look in the AmbientTalk symbiont
				return super.impl_mutateSlot(receiver, atSelector, arguments);
			}
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
     * When a slot (field or method) is selected from a symbiotic Java class object, the
     * selection proceeds as follows:
     *  - if the selector matches a static Java method, a closure wrapping that method is returned.
     *  - if the selector matches a static Java field, a nullary closure wrapping the field access
     *    is returned (UAP)
     *  - otherwise, the selector is looked up in the AmbientTalk symbiont.
     */
	public ATClosure impl_selectAccessor(ATObject receiver, ATSymbol atSelector) throws InterpreterException {
		final String jSelector = Reflection.upSelector(atSelector);
		try {
			ATObject val = Symbiosis.readField(null, wrappedClass_, jSelector);
			if (val.meta_isTaggedAs(NativeTypeTags._CLOSURE_).asNativeBoolean().javaValue) {
				return val.asClosure();
			} else {
				return new NativeClosure.Accessor(atSelector, this) {
					public ATObject access() throws InterpreterException {
						return Symbiosis.readField(null, wrappedClass_, jSelector);
					}
				};
			}
		} catch(XUndefinedSlot e) {
			JavaMethod choices = Symbiosis.getMethods(wrappedClass_, jSelector, true);
			if (choices != null) {
				return new JavaClosure(this, choices);
			} else {
				return super.impl_selectAccessor(receiver, atSelector);
			}
		}
	}
	
    /**
     * When a slot (field or method) is selected from a symbiotic Java class object, the
     * selection proceeds as follows:
     *  - if the selector matches a static Java method, a closure wrapping that method is returned.
     *  - if the selector matches a static Java field, a unary closure wrapping the field mutation
     *    is returned (UAP)
     *  - otherwise, the selector is looked up in the AmbientTalk symbiont.
     */
	public ATClosure impl_selectMutator(ATObject receiver, ATAssignmentSymbol atSelector) throws InterpreterException {
		final String jFieldSelector = Reflection.upSelector(atSelector.getFieldName());
		if (Symbiosis.hasField(wrappedClass_, jFieldSelector, true)) {
			return new NativeClosure.Mutator(atSelector, this) {
				public ATObject mutate(ATObject val) throws InterpreterException {
					Symbiosis.writeField(null, wrappedClass_, jFieldSelector, val);
					return val;
				}
			};
		} else {
			String jMethodSelector = Reflection.upSelector(atSelector);
			JavaMethod choices = Symbiosis.getMethods(wrappedClass_, jMethodSelector, true);
			if (choices != null) {
				return new JavaClosure(this, choices);
			} else {
				return super.impl_selectMutator(receiver, atSelector);
			}
		}
	}
    
    /**
     * A variable lookup is resolved by first checking whether the Java object has an appropriate static
     * field with a matching name. If so, that field's contents are returned. If not, the AT symbiont's
     * fields are checked.
     */
    public ATObject meta_lookup(ATSymbol selector) throws InterpreterException {
        try {
        	String jSelector = Reflection.upSelector(selector);
      	    return Symbiosis.readField(null, wrappedClass_, jSelector);
        } catch(XUndefinedSlot e) {
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
    	    throw new XDuplicateSlot(name);
        } else {
    	    return super.meta_defineField(name, value);
        }
    }
    
    /**
     * Variables can be assigned within a symbiotic Java class object if that class object
     * has a mutable static field with a matching name. Variable assignment is first
     * resolved in the Java object and afterwards in the AT symbiont.
     */
    public ATNil meta_assignVariable(ATSymbol name, ATObject value) throws InterpreterException {
        try {
        	String jSelector = Reflection.upSelector(name);
        	Symbiosis.writeField(null, wrappedClass_, jSelector, value);
        	return NATNil._INSTANCE_;
		} catch (XUnassignableField e) {
			return super.meta_assignVariable(name, value);
		}
    }
    
    /**
     * Fields can be assigned within a symbiotic Java class object if that class
     * has a mutable field with a matching name. Field assignment is first resolved
     * in the Java object and afterwards in the AT symbiont.
     * @deprecated now use invoke
     */
    public ATNil meta_assignField(ATObject receiver, ATSymbol name, ATObject value) throws InterpreterException {
        try {
     	    String jSelector = Reflection.upSelector(name);
    	    Symbiosis.writeField(null, wrappedClass_, jSelector, value);
    	    return NATNil._INSTANCE_;	
		} catch (XUnassignableField e) {
			return super.meta_assignField(receiver, name, value);
		}
    }
    
	/**
	 * Symbiotic Java class objects are singletons.
	 */
	public ATObject meta_clone() throws InterpreterException { return this; }
	
	/**
	 * aJavaClass.new(@args) == invoke a Java constructor
	 * AmbientTalk objects can add a custom new method to the class in order to intercept
	 * instance creation. The original instance can then be performed by invoking the old new(@args).
	 * 
	 * For example, imagine we want to extend the class java.lang.Point with a 3D coordinate, e.g. a 'z' field:
	 * <tt>
	 * def Point := jlobby.java.awt.Point;
	 * def oldnew := Point.new;
	 * def Point.new(x,y,z) { // 'override' the new method
	 *   def point := oldnew(x,y); // invokes the Java constructor
	 *   def point.z := z; // adds a field dynamically to the new JavaObject wrapper
	 *   point; // important! new should return the newly created instance
	 * }
	 * def mypoint := Point.new(1,2,3);
	 * </tt>
	 */
    public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
    	return Symbiosis.symbioticInstanceCreation(wrappedClass_, initargs.asNativeTable().elements_);
    }
    
    /**
     * Methods can be added to a symbiotic Java class object provided they do not already
     * exist in the Java class.
     */
    public ATNil meta_addMethod(ATMethod method) throws InterpreterException {
        ATSymbol name = method.base_name();
        if (Symbiosis.hasMethod(wrappedClass_, Reflection.upSelector(name), true)) {
    	    throw new XDuplicateSlot(name);
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
        	return new JavaField(null,
        			             Symbiosis.getField(wrappedClass_, Reflection.upSelector(fieldName), true));
        } catch(XUndefinedSlot e) {
        	return super.meta_grabField(fieldName);
        }
    }

    /**
     * Methods can be grabbed from a symbiotic Java class object. Methods that correspond
     * to static methods in the Java class are returned as JavaMethod instances.
     */
    public ATMethod meta_grabMethod(ATSymbol methodName) throws InterpreterException {
        JavaMethod choices = Symbiosis.getMethods(wrappedClass_, Reflection.upSelector(methodName), true);
        if (choices != null) {
        	return choices;
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
    	return NATTable.atValue(NATTable.collate(jFields, symbiontFields));
    }

    /**
     * Querying a symbiotic Java class object for its methods results in a table containing
     * both 'native' static Java methods and the methods of its AT symbiont
     */
    public ATTable meta_listMethods() throws InterpreterException {
		// instance methods of the wrapped object's class
		JavaMethod[] jMethods = Symbiosis.getAllMethods(wrappedClass_, true);
        // methods of the AT symbiont
		ATObject[] symbiontMethods = super.meta_listMethods().asNativeTable().elements_;
		return NATTable.atValue(NATTable.collate(jMethods, symbiontMethods));
    }

	public ATBoolean meta_isCloneOf(ATObject original) throws InterpreterException {
		return NATBoolean.atValue(this == original);
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<java:"+wrappedClass_.toString()+">");
	}

	/**
     * A Java Class object remains unique within an actor.
     */
    public ATObject meta_resolve() throws InterpreterException {
    	return wrapperFor(wrappedClass_);
    }
    
    /* ========================
     * == ATTypeTag Interface ==
     * ======================== */
    
    /**
     * If this class represents an interface type, parentTypes
     * are wrappers for all interfaces extended by this Java interface type
     */
	public ATTable base_superTypes() throws InterpreterException {
		return super.impl_accessSlot(this, _PTS_NAME_, NATTable.EMPTY).asTable();
	}

	public ATSymbol base_typeName() throws InterpreterException {
		return super.impl_accessSlot(this, _TNM_NAME_, NATTable.EMPTY).asSymbol();
	}

	/**
	 * A Java interface type used as a type can only be a subtype of another
	 * Java interface type used as a type, and only if this type is assignable
	 * to the other type.
	 */
	public ATBoolean base_isSubtypeOf(ATTypeTag other) throws InterpreterException {
		if (other instanceof JavaClass) {
			JavaClass otherClass = (JavaClass) other;
			// wrappedClass <: otherClass <=> otherClass >= wrappedClass
			return NATBoolean.atValue(otherClass.wrappedClass_.isAssignableFrom(wrappedClass_));
		} else {
			return NATBoolean._FALSE_;
		}
	}
	
}
