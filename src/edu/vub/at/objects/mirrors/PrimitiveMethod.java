/**
 * AmbientTalk/2 Project
 * PrimitiveMethod.java created on 2-feb-2007 at 21:44:00
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
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeStripes;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATByCopy;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGBegin;

/**
 * A primitive method is the equivalent of a NativeClosure but for methods rather
 * than closures. The advantage of PrimtiveMethods is that their base_apply method
 * gives access to both arguments as well as to the runtime context in which they
 * were invoked.
 * 
 * Example primitive methods are '==', 'new' and 'init' implemented in NATObject.
 *
 * Primitive methods should implement this method's base_apply method.
 *
 * @author tvcutsem
 */
public abstract class PrimitiveMethod extends NATByCopy implements ATMethod {
	
	private final ATSymbol name_;
	private final ATTable  formals_;
	
	public PrimitiveMethod(ATSymbol name, ATTable formals) {
		name_ = name;
		formals_ = formals;
	}

	public ATSymbol base_getName() throws InterpreterException {
		return name_;
	}
	
	public ATTable base_getParameters() throws InterpreterException {
		return formals_;
	}

	public ATBegin base_getBodyExpression() {
		return new AGBegin(NATTable.atValue(new ATObject[] {
				NATText.atValue("Primitive implementation of " + name_) }));
	}
	
	public abstract ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException;
	
	public ATObject base_applyInScope(ATTable arguments, ATContext ctx) throws InterpreterException {
		return base_apply(arguments, ctx);
	}
	
	public ATMethod base_asMethod() throws XTypeMismatch {
		return this;
	}

	public boolean base_isMethod() {
		return true;
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<primitive method:"+name_+">");
	}
	
    public ATTable meta_getStripes() throws InterpreterException {
    	return NATTable.of(NativeStripes._METHOD_);
    }
	
}