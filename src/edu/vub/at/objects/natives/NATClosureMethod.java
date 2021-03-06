/**
 * AmbientTalk/2 Project
 * NATClosureMethod.java created on 16-nov-2006 at 9:12:46
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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATDefExternalMethod;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.util.TempFieldGenerator;

/**
 * A 'closure method' is literally a function that sits in between of a full closure and a method.
 * It is a method that captures only its lexical scope of definition, but not the value of 'self'
 * and 'super' which are active at that time. When invoked, the method is evaluated in its lexical
 * scope, but with the values of 'self' and 'super' equal to those given to it at method invocation time.
 * 
 * Closure methods are used to implement 'externally added' methods to objects. In such cases,
 * the external methods can only access their own surrounding lexical scope (and not that of the actual
 * object to which they are added), but their values for 'self' and 'super' will act as if the method
 * was actually defined within the object itself.
 * 
 * See the description of the {@link ATDefExternalMethod} interface for more information.
 * 
 * @author tvcutsem
 */
public class NATClosureMethod extends NATByRef implements ATMethod {
	
	private final ATObject lexicalScope_;
	private final ATMethod method_;
	
	/** construct a new closure method */
	public NATClosureMethod(ATObject scope, ATMethod method) throws InterpreterException {
		lexicalScope_ = scope;
		method_ = method;
	}
	
	/**
	 * A closure method application acts exactly like a regular direct method application, except that
	 * the given lexical scope is disregarded and replaced by the lexical scope encapsulated by the
	 * closure method. The bindings for 'self' and 'super' remain intact.
	 * 
	 * Closure methods are a result of external method definitions.
	 * 
	 * @param arguments the evaluated actual arguments
	 * @param ctx the context whose 'self' binding will be used during the execution of the method.
	 * The lexical scope, however, will be replaced by the closure method's own lexical scope. A call frame will
	 * be inserted into this lexical scope before executing the method body.
	 * To ensure that 'super' points to the parent of the actual 'self' and not the parent of the object
	 * performing the external method definition, a 'super' field is added to the call frame shadowing
	 * the defining object's 'super' field.
	 * @return the value of evaluating the function body
	 */
	public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
		// should be: method_.base_apply(arguments, ctx.base_withLexicalEnvironment(lexicalScope_),
		// but this version is more efficient, as we immediately create the right context, rather than
		// creating a temporary context which will in turn be modified by method_.base_apply
		
		// hostObject = the object to which the external method was added (actually the object in which
		// the external method was now found)
		ATObject hostObject = ctx.base_lexicalScope();
		ATObject hostParent = hostObject.base_super();
		
		NATCallframe externalFrame = new NATCallframe(lexicalScope_);
		// super = the parent of the object to which this method was added
		externalFrame.meta_defineField(NATObject._SUPER_NAME_, hostParent);
		
		return method_.base_applyInScope(arguments, ctx.base_withLexicalEnvironment(externalFrame));
	}

	public ATObject base_applyInScope(ATTable arguments, ATContext ctx) throws InterpreterException {
		return method_.base_applyInScope(arguments, ctx);
	}

	public ATBegin base_bodyExpression() throws InterpreterException {
		return method_.base_bodyExpression();
	}

	public ATSymbol base_name() throws InterpreterException {
		return method_.base_name();
	}

	public ATTable base_parameters() throws InterpreterException {
		return method_.base_parameters();
	}
	
	public ATTable base_annotations() throws InterpreterException {
		return method_.base_annotations();
	}
	
	/**
	 * When wrapping a closure method, return a closure that is bound to my own lexical scope.
	 * The lexical scope passed to wrap is ignored.
	 */
	public ATClosure base_wrap(ATObject lexicalScope, ATObject dynamicReceiver) throws InterpreterException {
		return method_.base_wrap(lexicalScope_, dynamicReceiver);
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<closure:"+method_.base_name()+">");
	}

	public ATMethod asMethod() throws XTypeMismatch {
		return this;
	}
	
    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._METHOD_);
    }
	
}
