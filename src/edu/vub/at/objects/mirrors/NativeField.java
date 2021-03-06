/**
 * AmbientTalk/2 Project
 * NATPrimitiveField.java created on Aug 2, 2006 at 12:47:02 AM
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

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATByRef;
import edu.vub.at.objects.natives.NATField;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

import java.lang.reflect.Method;

/**
 * Native Fields are represented in our reflective implementation as a pair
 * of accessor and mutator methods in the class of the native AmbientTalk object.
 * For instance, a native AmbientTalk object of class C has a field 'f' if the
 * class C implements a method 'getF()'. If the class also implements 'setF(v)'
 * the field is assignable.
 * 
 * @author tvcutsem
 * @author smostinc
 */
public class NativeField extends NATByRef implements ATField {
	
	/**
	 * The AmbientTalk name of the field
	 */
	private final ATSymbol name_;
	
	/**
	 * The AmbientTalk native object to which this field belongs
	 */
	private final ATObject host_;
	
	/**
	 * The native Java accessor method to be called when accessing the field
	 */
	private final Method accessor_;
	
	/**
	 * The native Java mutator method to be called when assigning to the field.
	 * This field may be null which indicates a read-only field
	 */
	private final Method mutator_;

	public NativeField(ATObject host, ATSymbol name, Method accessor, Method mutator) {
		host_ = host;
		name_ = name;
		accessor_ = accessor;
		mutator_ = mutator;
	}

	public ATSymbol base_name() {
		return name_;
	}

	public ATObject base_readField() throws InterpreterException {
		return JavaInterfaceAdaptor.invokeNativeATMethod(accessor_, host_, NATTable.EMPTY.elements_);
	}

	public ATObject base_writeField(ATObject newValue) throws InterpreterException {
		// certain fields may not have setters
		if (mutator_ != null) {
			JavaInterfaceAdaptor.invokeNativeATMethod(accessor_, host_, new ATObject[] { newValue });
			return Evaluator.getNil();
		} else {
			throw new XIllegalOperation("Field " + name_ + " cannot be set.");
		}
	}
	
	/**
	 * Fields can be re-initialized when installed in an object that is being cloned.
	 * They expect the new owner of the field as the sole instance to their 'new' method
	 */
	public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
		if (initargs.base_length() != NATNumber.ONE) {
			return super.meta_newInstance(initargs);
		} else {
		    ATObject newhost = initargs.base_at(NATNumber.ONE);
		    return new NativeField(newhost, name_, accessor_, mutator_);
		}
	}

	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<native field:"+name_+" of "+ host_.meta_print().javaValue +">");
	}
	
    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._FIELD_);
    }
	
    public boolean isNativeField() {
        return true;
    }
	
	public ATField asField() {
		return this;
	}

    public ATMethod base_accessor() throws InterpreterException {
    	return NATField.accessorForField(this);
    }
    
    public ATMethod base_mutator() throws InterpreterException {
    	return NATField.mutatorForField(this);
    }
	
}
