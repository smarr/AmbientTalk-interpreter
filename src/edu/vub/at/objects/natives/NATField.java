/**
 * AmbientTalk/2 Project
 * NATField.java created on Jul 27, 2006 at 2:27:53 AM
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
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeStripes;
import edu.vub.at.objects.grammar.ATSymbol;

/**
 * NATField implements a causally connected field of an object. Rather than storing
 * these fields directly in every object, we choose to only make people pay when they
 * choose to reify them. 
 * 
 * @author smostinc
 */
public class NATField extends NATByRef implements ATField {

	private final ATSymbol name_;
	private final NATCallframe frame_;
	
	public NATField(ATSymbol name, NATCallframe frame) {
		name_ = name;
		frame_ = frame;
	}

	public ATSymbol base_getName() {
		return name_;
	}

	public ATObject base_readField() throws InterpreterException {
		try {
			return frame_.getLocalField(name_);
		} catch (InterpreterException e) {
			// Since the field was selected from the receiver, it should always be found
			throw new XIllegalOperation("Incorrectly initialised field accessed", e);
		}
	}

	public ATNil base_writeField(ATObject newValue) throws InterpreterException {
		return frame_.meta_assignField(frame_, name_, newValue);
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<field:"+name_.meta_print().javaValue+">");
	}
	
    public boolean isNativeField() {
        return true;
    }

	public ATField asField() throws XTypeMismatch {
		return this;
	}
	
	/**
	 * Fields can be re-initialized when installed in an object that is being cloned.
	 * They expect the new owner of the field as the sole instance to their 'new' method
	 */
	public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
		if (initargs.base_getLength() != NATNumber.ONE) {
			return super.meta_newInstance(initargs);
		} else {
			ATObject newhost = initargs.base_at(NATNumber.ONE);
			return new NATField(name_, (NATCallframe) newhost);
		}
	}
	
    public ATTable meta_getStripes() throws InterpreterException {
    	return NATTable.of(NativeStripes._FIELD_);
    }

}
