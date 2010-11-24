/**
 * AmbientTalk/2 Project
 * NATMethodInvocation.java created on 31-jul-2006 at 12:40:42
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
import edu.vub.util.TempFieldGenerator;

import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;

/**
 * Instances of this class represent first-class method invocations.
 * They encapsulate a selector and arguments and may be turned into an actual invocation by invoking meta_sendTo.
 * This method provides the invocation with a receiver to apply itself to.
 * 
 * @author tvcutsem
 */
public final class NATMethodInvocation extends NATMessage implements ATMethodInvocation {

	public NATMethodInvocation(ATSymbol sel, ATTable arg, ATTable annotations) throws InterpreterException {
		super(sel, arg, annotations, NativeTypeTags._METHODINV_);
	}
	
    /**
     * Copy constructor.
     */
    private NATMethodInvocation(FieldMap map,
            Vector state,
            LinkedList originalCustomFields,
            MethodDictionary methodDict,
            ATObject dynamicParent,
            ATObject lexicalParent,
            byte flags,
            ATTypeTag[] types) throws InterpreterException {
    	super(map, state, originalCustomFields, methodDict, dynamicParent, lexicalParent, flags, types);
    }

    public ATMethodInvocation asMethodInvocation() {
    	return this;
    }
    
	/**
	 * To evaluate a method invocation, invoke the method corresponding to the encapsulated
	 * selector to the given receiver with the encapsulated arguments.
	 * 
	 * @return the return value of the invoked method.
	 */
	public ATObject prim_sendTo(ATMessage self, ATObject receiver, ATObject sender) throws InterpreterException {
		return receiver.meta_invoke(receiver, self.asMethodInvocation());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<method invocation:"+base_selector()+Evaluator.printAsList(base_arguments()).javaValue+">");
	}
	
	public NATText impl_asCode(TempFieldGenerator objectMap) throws InterpreterException {
		if (objectMap.contains(this)) {
			return objectMap.getName(this);
		}
		StringBuffer codeString = new StringBuffer("." + base_selector().toString() + Evaluator.codeAsList(objectMap, base_arguments()).javaValue);
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
		return new NATMethodInvocation(map,
				state,
				originalCustomFields,
				methodDict,
				dynamicParent,
				lexicalParent,
				flags,
				types);
	}
	
}
