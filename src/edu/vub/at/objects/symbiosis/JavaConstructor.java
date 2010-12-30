/**
 * AmbientTalk/2 Project
 * JavaConstructor.java created on May 20, 2008 at 4:56:32 PM
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
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XNotInstantiatable;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATByRef;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGBegin;
import edu.vub.at.parser.SourceLocation;
import edu.vub.util.TempFieldGenerator;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Vector;

/**
 * JavaConstructor is a wrapper class encapsulating one or more java.lang.reflect.Constructor objects.
 * All methods in the choices array are overloaded constructors for the same class. The choices array 
 * should never be empty!
 * 
 * Since constructors do not need an receiver to become a full closure, this class implements both the
 * ATMethod and the ATJavaClosure interface. The latter ensures that constructors can be cast properly.
 * 
 * @author smostinc
 */
public class JavaConstructor extends NATByRef implements ATMethod, ATJavaClosure {
	

	protected final Class class_;
	protected final Constructor[] choices_;
	
	private ATContext context_ = null; // lazily initialized 
	
	public JavaConstructor(Class clazz) {
		this(clazz, clazz.getConstructors());
	}
	
	public JavaConstructor(Class clazz, Constructor[] choices) {
		// assertion
		class_   = clazz;
		choices_ = choices;
	}
		
	// ATObject Interface implementation

    public ATBoolean base__opeql__opeql_(ATObject comparand) throws InterpreterException {
        return NATBoolean.atValue(this.equals(comparand));
    }
	
	/**
	 * Two JavaConstructor instances are equal if they both represent a set of constructors
	 * from the same declaring class.
	 */
	public boolean equals(Object other) {
		if (other instanceof JavaConstructor) {
			JavaConstructor mth = (JavaConstructor) other;
			return (mth.class_ == this.class_);
		} else {
			return false;
		}
	}

    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._METHOD_, NativeTypeTags._CLOSURE_);
    }


	// ATMethod Interface implementation
	
	public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
		return base_apply(arguments);
	}
	
	public ATObject base_applyInScope(ATTable arguments, ATContext ctx) throws InterpreterException {
		return base_apply(arguments);
	}
	
	public ATBegin base_bodyExpression() throws InterpreterException {
		// list all of the method signatures of the (possibly overloaded) Java method
		StringBuffer buff = new StringBuffer("Java implementation of: ");
		for (int i = 0; i < choices_.length; i++) {
			buff.append("\n");
			buff.append(choices_[i].toString());
		}
		buff.append("\n");
		return new AGBegin(NATTable.atValue(new ATObject[] { NATText.atValue(buff.toString()) }));
	}

	public ATSymbol base_name() throws InterpreterException {
		return NATNil._NEW_NAME_;
	}

	public ATTable base_parameters() throws InterpreterException {
		return Evaluator._ANON_MTH_ARGS_;
	}
	
	public ATTable base_annotations() throws InterpreterException {
		return NATTable.EMPTY;
	}

	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<java constructor:"+class_+">");
	}
	
	
	
	public NATText impl_asCode(TempFieldGenerator objectMap) throws InterpreterException {
		String simpleClassName = class_.getSimpleName();
		char[] simpleClassNameChars = simpleClassName.toCharArray();
		if (Character.isLowerCase(simpleClassNameChars[0])) {
			String packageName = class_.getPackage().getName();
			return NATText.atValue("jlobby." + packageName + ".class(`" + simpleClassName + ").&new");
		} else {
			return NATText.atValue("jlobby." + class_.getCanonicalName() + ".&new");
		}
	}

	public ATMethod asMethod() throws XTypeMismatch {
		return this;
	}
	
	/**
	 * When selected from a JavaClass object, the constructor needs to be wrapped in a dedicated
	 * closure object. For constructors the JavaConstructor instance itself can be used. 
	 */
	public ATClosure base_wrap(ATObject lexicalScope, ATObject dynamicReceiver) {
		return this;
	}
	
	// ATJavaClosure Interface implementation
	
	public ATClosure asClosure() throws InterpreterException {
		return this;
	}
	
	public ATObject base_apply(ATTable arguments) throws InterpreterException {
		return Symbiosis.symbioticInstanceCreation(this, arguments.asNativeTable().elements_);
	}

	public ATObject base_applyInScope(ATTable arguments, ATObject scope) throws InterpreterException {
		return base_apply(arguments);
	}

	public ATContext base_context() throws InterpreterException {
		if(context_ == null) {
			JavaClass atClass = JavaClass.wrapperFor(class_);
			context_ =  new NATContext(atClass, atClass);
		}
		
		return context_;
	}
	
	public ATObject base_escape() throws InterpreterException {
		throw new XIllegalOperation("Cannot call escape() in a Java Constructor");
	}

	public ATMethod base_method() throws InterpreterException {
		return this;
	}

	public ATObject base_whileTrue_(final ATClosure body) throws InterpreterException {
		final ATClosure test = this;
		ATBoolean cond;
		
		while (true) {
			// cond = self.apply()
			cond = this.base_apply(NATTable.EMPTY).asBoolean();
			if(cond.isNativeBoolean()) {
				// cond is a native boolean, perform the conditional ifTrue: test natively
				if (cond.asNativeBoolean().javaValue) {
					// execute body and continue while loop
					body.base_apply(NATTable.EMPTY);
					continue;
				} else {
					// return nil
					return Evaluator.getNil();
				}
			} else {
				// cond is a user-defined boolean, do a recursive send
				return cond.base_ifTrue_(new NativeClosure(this) {
					public ATObject base_apply(ATTable args) throws InterpreterException {
						// if user-defined bool is true, execute body and recurse
						body.base_apply(NATTable.EMPTY);
						return test.base_whileTrue_(body);
					}
				});
			}
		}	}
	
	/**
	 * For each Method in the wrapped JavaMethod's choices_, check whether it is compatible with
	 * the given types. If so, add it to the choices_ array of the new JavaMethod.
	 */
	public ATClosure base_cast(ATObject[] types) throws InterpreterException {
		// unwrap the JavaClass wrappers
		Class[] actualTypes = new Class[types.length];
		for (int i = 0; i < actualTypes.length; i++) {
			// Array types may be represented as one-arg tables of a type: [Type]
			// TODO: properly refactor the instanceof test
			// problem: cannot do base_isTable because JavaObject/JavaClass objects will say yes!
			if (types[i] instanceof NATTable) {
				// Array.newInstance([Type][1],0).getClass()
				actualTypes[i] = Array.newInstance(types[i].asTable().
				    base_at(NATNumber.ONE).asJavaClassUnderSymbiosis().getWrappedClass(), 0).getClass();
			} else {
				actualTypes[i] = types[i].asJavaClassUnderSymbiosis().getWrappedClass();
			}
		}
		Vector matchingMethods = new Vector();
		
		for (int i = 0; i < choices_.length; i++) {
			if(matches(choices_[i].getParameterTypes(), actualTypes)) {
				matchingMethods.add(choices_[i]);
			}
		}
		
		Constructor[] matches = (Constructor[]) matchingMethods.toArray(new Constructor[matchingMethods.size()]);

		if (matches.length > 0) {
			return new JavaConstructor(class_, matches);
		} else {
			throw new XNotInstantiatable(class_);
		}
	}
	
	/**
	 * Compares two Class arrays and returns true iff both arrays have equal size and all members are the same.
	 */
	private static final boolean matches(Class[] formals, Class[] actuals) {
		if (formals.length != actuals.length)
			return false;
		
		for (int i = 0; i < formals.length; i++) {
			if (!(formals[i] == actuals[i])) {
				return false;
			}
		}
		
		return true;
	}
}