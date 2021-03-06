/**
 * AmbientTalk/2 Project
 * NATException.java created on Oct 13, 2006 at 1:46:47 PM
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
package edu.vub.at.objects.natives;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATException;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.symbiosis.Symbiosis;

/**
 * Instances of the class NATException provide a AmbientTalk representation for the 
 * default instances of all InterpreterExceptions. They allow catching exceptions 
 * thrown by the interpreter in AmbientTalk, as well as raising them for within
 * custom code. 
 *
 * @author smostinc
 */
public class NATException extends NATByCopy implements ATException {
	
	private final InterpreterException wrappedException_;
	
	public NATException(InterpreterException wrappedException) {
		wrappedException_ = wrappedException;
	}

	public InterpreterException getWrappedException() {
		return wrappedException_;
	}

	public NATText base_message() throws InterpreterException {
		return NATText.atValue(wrappedException_.getMessage());
	}
	
	public NATText base_stackTrace() throws InterpreterException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		wrappedException_.printAmbientTalkStackTrace(new PrintStream(out));
		return NATText.atValue(out.toString());
	}
	
	public NativeATObject base_stackTrace__opeql_(NATText newTrace) throws InterpreterException {
		return Evaluator.getNil();
	}
	
	public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
        return Reflection.upExceptionCreation(wrappedException_, initargs);
    }

	public ATBoolean meta_isRelatedTo(ATObject object) throws InterpreterException {
		if(object instanceof NATException) {
			return NATBoolean.atValue(
					wrappedException_.getClass().isAssignableFrom(
					((NATException)object).wrappedException_.getClass())); 
		} else {
			return NATBoolean._FALSE_;
		}
	}
	
    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.of(wrappedException_.getType(), NativeTypeTags._ISOLATE_);
    }
    
    public NATText meta_print() throws InterpreterException {
    	return NATText.atValue("<exception:" + wrappedException_.getType() + ": " + wrappedException_.getMessage() + ">");
    }
    
	public ATObject meta_clone() throws InterpreterException {
		return this;
	}
	
	/**
	 * delegate messages not understood to the wrapped exception object
	 */
	public ATClosure meta_doesNotUnderstand(final ATSymbol selector) {
		return new NativeClosure(this) {
			public ATObject base_apply(ATTable args) throws InterpreterException {
				return Symbiosis.symbioticInvocation(scope_,
						                             wrappedException_,
						                             wrappedException_.getClass(),
						                             Reflection.upSelector(selector),
						                             args.asNativeTable().elements_);
			}
		};
	}
}
