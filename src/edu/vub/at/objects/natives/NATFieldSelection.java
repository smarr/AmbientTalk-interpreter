/**
 * AmbientTalk/2 Project
 * NATFieldSelection.java created on 31-jul-2006 at 12:40:42
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

import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATFieldSelection;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.util.TempFieldGenerator;

/**
 * Instances of this class represent first-class field selections.
 * They encapsulate a selector and may be turned into an actual field access by invoking meta_sendTo.
 * This method provides the invocation with a receiver to apply itself to.
 * 
 * @author tvcutsem
 */
public final class NATFieldSelection extends NATMessage implements ATFieldSelection {

	public NATFieldSelection(ATSymbol sel, ATTable annotations) throws InterpreterException {
		super(sel, NATTable.EMPTY, annotations, NativeTypeTags._FIELDSEL_);
	}
	
    /**
     * Copy constructor.
     */
    private NATFieldSelection(FieldMap map,
            Vector state,
            LinkedList originalCustomFields,
            MethodDictionary methodDict,
            ATObject dynamicParent,
            ATObject lexicalParent,
            byte flags,
            ATTypeTag[] types) throws InterpreterException {
    	super(map, state, originalCustomFields, methodDict, dynamicParent, lexicalParent, flags, types);
    }

	/**
	 * To evaluate a field selection, invoke a field accessor method on the receiver.
	 * 
	 * @return the return value of the invoked method.
	 */
	public ATObject prim_sendTo(ATMessage self, ATObject receiver, ATObject sender) throws InterpreterException {
		return receiver.meta_invokeField(receiver, self.base_selector());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<field selection:"+base_selector().toString()+">");
	}
	
	public NATText impl_asCode(TempFieldGenerator objectMap) throws InterpreterException {
		if (objectMap.contains(this)) {
			return objectMap.getName(this);
		}
		NATText name = objectMap.put(this, NATText.atValue("." + base_selector().toString()));
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
		return new NATFieldSelection(map,
				state,
				originalCustomFields,
				methodDict,
				dynamicParent,
				lexicalParent,
				flags,
				types);
	}
	
}
