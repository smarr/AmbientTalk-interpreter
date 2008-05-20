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
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATMethod;
import edu.vub.at.objects.natives.OBJNil;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.NATAbstractGrammar;

/**
 * A primitive method is the equivalent of a NativeClosure but for methods rather
 * than closures. The advantage of PrimtiveMethods is that their base_apply method
 * gives access to both arguments as well as to the runtime context in that contains
 * the lexical environment in which they were defined.
 * 
 * Example primitive methods are '==', 'new' and 'init' implemented in NATObject.
 *
 * Primitive methods should implement this method's base_apply method or invoke the
 * constructor taking a PrimitiveBody parameter and implement that class's meta_eval
 * method, if they want to make use of AT/2's parameter binding semantics.
 * 
 * Primitive methods installed in native objects that can be extended should ensure
 * that they use the dynamic receiver stored in the application context (the AmbientTalk
 * 'self') rather than the Java 'this' variable to perform self-sends. The former will
 * properly invoke/select the overridden methods/fields of a child AT object, the latter
 * will simply refer to the native instance, disregarding any modifications by child objects.
 *
 * @author tvcutsem
 */
public class PrimitiveMethod extends NATMethod {
	
	/**
	 * Instances of this helper class represent primitive method bodies. To the
	 * AT programmer, they look like empty methods (i.e. { nil }). The native Java
	 * implementation is specified by overriding the meta_eval method.
	 */
	public static abstract class PrimitiveBody extends NATAbstractGrammar implements ATBegin {
		
		/**
		 * A primitive can override this method and has access to:
		 * - ctx.lexicalScope = the call frame for this method invocation
		 * - ctx.lexicalScope.lexicalParent = the object in which the method was found
		 * - ctx.dynamicReceiver = the object on which the method was invoked
		 */
		public abstract ATObject meta_eval(ATContext ctx) throws InterpreterException;

		public ATObject meta_quote(ATContext ctx) throws InterpreterException {
			return this;
		}
		
		public ATTable base_statements() { return NATTable.of(OBJNil._INSTANCE_); }
		
		public NATText meta_print() throws InterpreterException {
			return NATText.atValue("<primitive body>");
		}
		
		public ATBegin asBegin() throws XTypeMismatch {
			return this;
		}
		
	}
	
	public PrimitiveMethod(ATSymbol name, ATTable formals, PrimitiveBody body) {
		super(name, formals, body, NATTable.EMPTY);
	}

	/**
	 * Constructor for the creation of primitive methods where the body is left empty.
	 * The idea is that the creator of such primitive methods overrides the base_apply
	 * method because the primitive method has no need for a call frame or parameter binding.
	 */
	public PrimitiveMethod(ATSymbol name, ATTable formals) {
		super(name, formals, new PrimitiveBody() {
            private static final long serialVersionUID = -2177230227968386983L;

			public ATObject meta_eval(ATContext ctx) throws InterpreterException {
				return OBJNil._INSTANCE_;
			}
		}, NATTable.EMPTY);
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<primitive method:"+base_name()+">");
	}
	
}