/**
 * AmbientTalk/2 Project
 * NATNil.java created on 15 jul 2007 at 18:33:28
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
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATAssignmentSymbol;
import edu.vub.at.objects.grammar.ATSymbol;

/**
 * This class encapsulates the behaviour of the native
 * <tt>nil</tt> AmbientTalk object.
 * 
 * @author tvcutsem
 */
public class OBJNil extends NATByCopy implements ATNil {

	/**
	 * This field holds the sole instance of this class. In AmbientTalk,
	 * this is the object that represents <tt>nil</tt>.
	 */
	public final static OBJNil _INSTANCE_ = new OBJNil();
	
	/**
	 * Constructor made private for singleton design pattern
	 */
	private OBJNil() { }
	
	/**
	 * Nil has a special parent object which ends the recursion
	 * along the dynamic delegation chain. These methods cannot be implemented
	 * directly in this class because this class still implements useful
	 * <tt>base_</tt> Java methods which have to be invoked by means of the
	 * implementations defined in {@link NativeATObject}.
	 */
	private final NativeATObject dynamicSentinel_ = new NATByCopy() {

      private static final long serialVersionUID = -1307795172754062220L;
		// METHODS THAT END THE DYNAMIC DELEGATION CHAIN
		
		public ATBoolean meta_respondsTo(ATSymbol selector) throws InterpreterException {
			// no more delegation
			return NATBoolean._FALSE_;
		}
		
		/**
		 * When performing <tt>o.m()</tt> and <tt>m</tt> is not found, invoke
		 * <tt>doesNotUnderStand</tt> and apply the resulting closure to the given arguments.
		 */
		public ATObject impl_invokeAccessor(ATObject receiver, ATSymbol selector, ATTable arguments) throws InterpreterException {
			return receiver.meta_doesNotUnderstand(selector).base_apply(arguments);
		}
		
		public ATObject impl_invokeMutator(ATObject receiver, ATAssignmentSymbol selector, ATTable arguments) throws InterpreterException {
			return receiver.meta_doesNotUnderstand(selector).base_apply(arguments);
		}

		/**
		 * When performing <tt>o.x</tt> and <tt>x</tt> is not found, invoke
		 * <tt>doesNotUnderStand</tt> and apply the corresponding closure with zero arguments.
		 */
		public ATObject meta_invokeField(ATObject receiver, ATSymbol selector) throws InterpreterException {
			return receiver.meta_doesNotUnderstand(selector).base_apply(NATTable.EMPTY);
		}
	    
	    public ATClosure impl_selectAccessor(ATObject receiver, final ATSymbol selector) throws InterpreterException {
	    	return receiver.meta_doesNotUnderstand(selector);
	    }

		public ATClosure impl_selectMutator(ATObject receiver, final ATAssignmentSymbol selector) throws InterpreterException {
			return receiver.meta_doesNotUnderstand(selector);
		}

		public NATText meta_print() throws InterpreterException {
			return NATText.atValue("dynamicsentinel");
		}
	};
	
	public ATBoolean base__opnot__opeql_(ATObject other) throws InterpreterException {
		return this.base__opeql__opeql_(other).base_not();
	}

	public ATObject base_super() {
		return dynamicSentinel_;
	}
	
    /**
     * After deserialization, ensure that nil remains unique.
     */
    public ATObject meta_resolve() throws InterpreterException {
    	return OBJNil._INSTANCE_;
    }

	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("nil");
	}
	
}
