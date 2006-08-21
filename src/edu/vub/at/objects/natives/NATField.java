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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATSymbol;

/**
 * @author smostinc
 *
 * NATField implements a causally connected field of an object. Rather than storing
 * these fields directly in every object, we choose to only make people pay when they
 * choose to reify them.
 */
public class NATField extends NATNil implements ATField {

	private final ATSymbol name_;
	private final NATCallframe frame_;
	
	public NATField(ATSymbol name, NATCallframe frame) {
		name_ = name;
		frame_ = frame;
	}

	public ATSymbol getName() {
		return name_;
	}

	public ATObject getValue() {
		try {
			return frame_.getLocalField(name_);
		} catch (NATException e) {
			// TODO XIllegalOperation should be an unchecked exception (maybe two variants)
			// Since the field was selected from the receiver, it should always be found
			throw new RuntimeException("Incorrectly initialised field accessed", e);
		}
	}

	public ATObject setValue(ATObject newValue) throws NATException {
		ATObject result = getValue();
		frame_.meta_assignField(name_, newValue);
		return result;
	}
	
	public NATText meta_print() throws XTypeMismatch {
		return NATText.atValue("<field:"+name_.meta_print().javaValue+">");
	}

}
