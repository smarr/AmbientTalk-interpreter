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
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATSymbol;

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
 * See the description of the ATDefExternalMethod interface for more information.
 * 
 * @author tvcutsem
 */
public final class NATClosureMethod extends NATMethod {
	
	private final ATObject lexicalScope_;
	
	public NATClosureMethod(ATObject scope, ATSymbol name, ATTable parameters, ATBegin body) {
		super(name, parameters, body);
		lexicalScope_ = scope;
	}
	
	/**
	 * A closure method application acts exactly like a regular direct method application, except that
	 * the given lexical scope is disregarded and replaced by the lexical scope encapsulated by the
	 * closure method. The bindings for 'self' and 'super' remain intact.
	 * 
	 * Closure methods are a result of external method definitions.
	 * 
	 * @param arguments the evaluated actual arguments
	 * @param ctx the context whose 'self' and 'super' bindings will be used during the execution of the method.
	 * The lexical scope, however, will be replaced by the closure method's own lexical scope. A call frame will
	 * be inserted into this lexical scope before executing the method body.
	 * @return the value of evaluating the function body
	 */
	public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
		// should be: super.base_apply(arguments, ctx.base_withLexicalEnvironment(lexicalScope_),
		// but this version is more efficient, as we immediately create the right context, rather than
		// creating a temporary context which will in turn be modified by super.base_apply
		return base_applyInScope(arguments, ctx.base_withLexicalEnvironment(new NATCallframe(lexicalScope_)));
	}
}
