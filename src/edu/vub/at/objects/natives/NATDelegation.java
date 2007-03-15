/**
 * AmbientTalk/2 Project
 * NATDelegation.java created on 29-jan-2007 at 16:24:59
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

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATMethodInvocation;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeStripes;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.util.LinkedList;
import java.util.Vector;

/**
 * Instances of the class NATMethodInvocation represent first-class method invocations.
 * They encapsulate a selector and arguments and may be turned into an actual invocation by invoking meta_sendTo.
 * This method provides the invocation with a receiver to apply itself to.
 * 
 * @author tvcutsem
 */
public final class NATDelegation extends NATMessage implements ATMethodInvocation {

	private final static AGSymbol _DELEGATOR_ = AGSymbol.jAlloc("delegator");
	
	public NATDelegation(ATObject delegator, ATSymbol sel, ATTable arg) throws InterpreterException {
		super(sel, arg, new ATStripe[] { NativeStripes._ISOLATE_, NativeStripes._METHODINV_ });
		super.meta_defineField(_DELEGATOR_, delegator);
	}
	
    /**
     * Copy constructor.
     */
    private NATDelegation(FieldMap map,
            Vector state,
            LinkedList originalCustomFields,
            MethodDictionary methodDict,
            ATObject dynamicParent,
            ATObject lexicalParent,
            byte flags,
            ATStripe[] stripes) throws InterpreterException {
    	super(map, state, originalCustomFields, methodDict, dynamicParent, lexicalParent, flags, stripes);
    }

	/**
	 * To evaluate a delegating message send, invoke the method corresponding to the encapsulated
	 * selector with the encapsulated arguments. During execution of the method, 'self' should be
	 * bound to the object that initiated the delegating method invocation.
	 * 
	 * @return the return value of the invoked method.
	 */
	public ATObject prim_sendTo(ATMessage self, ATObject receiver, ATObject sender) throws InterpreterException {
		return receiver.meta_invoke(super.meta_select(self, _DELEGATOR_), self.base_getSelector(), self.base_getArguments());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<delegation:"+base_getSelector()+Evaluator.printAsList(base_getArguments()).javaValue+">");
	}
	
	protected NATObject createClone(FieldMap map,
			Vector state,
			LinkedList originalCustomFields,
			MethodDictionary methodDict,
			ATObject dynamicParent,
			ATObject lexicalParent,
			byte flags,
			ATStripe[] stripes) throws InterpreterException {
		return new NATDelegation(map,
				state,
				originalCustomFields,
				methodDict,
				dynamicParent,
				lexicalParent,
				flags,
				stripes);
	}
	
}
