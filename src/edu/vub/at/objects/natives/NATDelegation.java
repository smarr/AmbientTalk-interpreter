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
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.util.TempFieldGenerator;

import java.util.LinkedList;
import java.util.Set;
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
	
	public NATDelegation(ATObject delegator, ATSymbol sel, ATTable arg, ATTable annotations) throws InterpreterException {
		super(sel, arg, annotations, NativeTypeTags._DELEGATION_);
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
            ATTypeTag[] types,
            Set freeVars) throws InterpreterException {
    	super(map, state, originalCustomFields, methodDict, dynamicParent, lexicalParent, flags, types, freeVars);
    }

    public ATMethodInvocation asMethodInvocation() { return this; }
    
	/**
	 * To evaluate a delegating message send, invoke the method corresponding to the encapsulated
	 * selector with the encapsulated arguments. During execution of the method, 'self' should be
	 * bound to the object that initiated the delegating method invocation.
	 * 
	 * @return the return value of the invoked method.
	 */
	public ATObject prim_sendTo(ATMessage self, ATObject receiver, ATObject sender) throws InterpreterException {
		return receiver.meta_invoke(super.meta_invokeField(self, _DELEGATOR_), self.asMethodInvocation());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<delegation:"+base_selector()+Evaluator.printAsList(base_arguments()).javaValue+">");
	}
	
	public NATText impl_asCode(TempFieldGenerator objectMap) throws InterpreterException {
		if (objectMap.contains(this)) {
			return objectMap.getName(this);
		}
		//StringBuffer codeString = new StringBuffer("^" + base_selector().meta_print().javaValue + Evaluator.printAsList(base_arguments()).javaValue);
		StringBuffer codeString = new StringBuffer("^" + base_selector().meta_print().javaValue + Evaluator.codeAsList(objectMap, base_arguments()).javaValue);
		NATTable annotations = NATTable.atValue(this.typeTags_);
		if(annotations.base_length().asNativeNumber().javaValue > 0) {
			codeString.append("@"+annotations.impl_asCode(objectMap).javaValue);
		}
		NATText code = NATText.atValue(codeString.toString());
		NATText name = objectMap.put(this, code);
		return name;
	}
	
	protected NATObject createClone(FieldMap map,
			Vector state,
			LinkedList originalCustomFields,
			MethodDictionary methodDict,
			ATObject dynamicParent,
			ATObject lexicalParent,
			byte flags,
			ATTypeTag[] types,
			Set freeVars) throws InterpreterException {
		return new NATDelegation(map,
				state,
				originalCustomFields,
				methodDict,
				dynamicParent,
				lexicalParent,
				flags,
				types,
				freeVars);
	}
	
}
