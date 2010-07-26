/**
 * AmbientTalk/2 Project
 * NativeMethod.java created on Jul 27, 2006 at 1:35:19 AM
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

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATByRef;
import edu.vub.at.objects.natives.NATClosure;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGBegin;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * The first-class representation of native AmbientTalk methods that are
 * invoked using an ad hoc, optimized lookup, rather than via the standard
 * reflective invocation mechanism based on the 'base_' naming convention.
 * (native methods invoked via reflection are reified as
 * {@link NativeMethod} instances instead.
 * 
 * This abstract class is subclassed for each concrete native method in the
 * system that is directly invoked. Each subclass implements
 * {@link DirectNativeMethod#base_apply(ATTable, ATContext)}
 * 
 * Note: {@link DirectNativeMethod} instances are intended to be shared between
 * multiple actors. In order to uphold this property, subclasses of
 * {@link DirectNativeMethod} should ensure that these instances remain immutable.
 * 
 * @author tvcutsem
 */
public abstract class DirectNativeMethod extends NATByRef implements ATMethod {

	private final ATSymbol name_;
		
	/**
	 * @param name the AmbientTalk name of this method.
	 */
	public DirectNativeMethod(String name) {
		name_ = AGSymbol.jAlloc(name);
	}

	public ATClosure base_wrap(ATObject lexicalScope, ATObject dynamicReceiver) throws InterpreterException {
		return new NATClosure(this, lexicalScope, dynamicReceiver);
	}
	
	/**
	 * The name of a wrapped Java method is the name of the Java method, converted to an
	 * AmbientTalk selector name.
	 */
	public ATSymbol base_name() throws InterpreterException {
		return name_;
	}
	
	/**
	 * TODO: more useful implementation? There is no easy way to get
	 * access to the formal parameter list of native methods, since
	 * the parameter list is never made explicit. 
	 */
	public ATTable base_parameters() throws InterpreterException {
		return NATTable.EMPTY;
	}

	public ATBegin base_bodyExpression() {
		return new AGBegin(NATTable.atValue(new ATObject[] {
				NATText.atValue("Native implementation of " + name_)}));
	}
	
	public ATTable base_annotations() throws InterpreterException {
		return NATTable.EMPTY;
	}
		
	public abstract ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException;
	
	public ATObject base_applyInScope(ATTable arguments, ATContext ctx) throws InterpreterException {
		return base_apply(arguments, ctx);
	}

	public ATMethod asMethod() throws XTypeMismatch {
		return this;
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<native method:"+name_+">");
	}
	
    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._METHOD_);
    }
    
	/**
	 * Auxiliary method to more easily extract arguments from an ATTable
	 */
	protected static ATObject get(ATTable args, int n) throws InterpreterException {
		return args.base_at(NATNumber.atValue(n));
	}

	protected static int getNbr(ATTable args, int n) throws InterpreterException {
		return args.base_at(NATNumber.atValue(n)).asNativeNumber().javaValue;
	}
	
	protected static double getFrc(ATTable args, int n) throws InterpreterException {
		return args.base_at(NATNumber.atValue(n)).asNativeFraction().javaValue;
	}
	
	protected static String getTxt(ATTable args, int n) throws InterpreterException {
		return args.base_at(NATNumber.atValue(n)).asNativeText().javaValue;
	}
	
	protected static boolean getBln(ATTable args, int n) throws InterpreterException {
		return args.base_at(NATNumber.atValue(n)).asNativeBoolean().javaValue;
	}
	
	protected static Object[] getTab(ATTable args, int n) throws InterpreterException {
		return args.base_at(NATNumber.atValue(n)).asNativeTable().elements_;
	}
	
	protected static void checkArity(ATTable args, int required) throws InterpreterException {
		int provided = args.base_length().asNativeNumber().javaValue;
		if (provided != required) {
			throw new XArityMismatch(Evaluator._ANON_MTH_NAM_.toString(), required, provided);
		}
	}
	
	protected static void checkNullaryArguments(ATSymbol selector, ATTable args) throws InterpreterException {
		if (args != NATTable.EMPTY)
			throw new XArityMismatch("access to non-closure field " + selector.toString(), 0, args.base_length().asNativeNumber().javaValue);
	}
	
	protected static ATObject checkUnaryArguments(ATSymbol selector, ATTable args) throws InterpreterException {
    	int len = args.base_length().asNativeNumber().javaValue;
		if (len != 1)
			throw new XArityMismatch("mutation of field " + selector.toString(), 1, len);
		return args.base_at(NATNumber.ONE);
	}
	
}
