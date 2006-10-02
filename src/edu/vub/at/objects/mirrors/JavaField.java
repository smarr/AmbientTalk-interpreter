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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

/**
 * @author smostinc
 *
 * NATPrimitiveField allows an ambienttalk primitive to have a field if it has a
 * corresponding getter and optionally a setter method.
 */
public class JavaField extends NATNil implements ATField {

	private ATClosure getter_;
	private ATClosure setter_;
	private ATSymbol name_;
	
	public static JavaField createPrimitiveField(
			ATObject receiver, ATSymbol fieldName) throws NATException {

		String getterSel = Reflection.upBaseFieldAccessSelector(fieldName);
		String setterSel = Reflection.upBaseFieldMutationSelector(fieldName);

		JavaClosure getter = JavaInterfaceAdaptor.wrapMethodFor(receiver.getClass(), receiver, getterSel);
		JavaClosure setter = null;

		try {
			setter = JavaInterfaceAdaptor.wrapMethodFor(receiver.getClass(), receiver, setterSel);
		} catch (NATException e) {
			// The lack of a setter method for a field on a primitive can be tolerated
		}	
		
		return new JavaField(fieldName, getter, setter);
	}
	
	
	private JavaField(ATSymbol name, ATClosure getter, ATClosure setter) {
		name_ = name;
		getter_ = getter;
		setter_ = setter;
	}


	public ATSymbol getName() {
		return name_;
	}

	public ATObject getFieldValue() {
		try {
			return getter_.base_apply(NATTable.EMPTY.elements_);
		} catch (NATException e) {
			// the application of the getter should normally not give a problem
			throw new RuntimeException(e);
		}
	}

	public ATObject setValue(ATObject newValue) throws NATException {
		// certain fields may not have setters
		if(setter_ != null)
			return getter_.base_apply(new ATObject[] { newValue });
		throw new XIllegalOperation("Field " + name_.getText().asNativeText().javaValue + " cannot be set.");
	}

	public NATText meta_print() throws XTypeMismatch {
		return NATText.atValue("<native field:"+name_.getText().asNativeText().javaValue+">");
	}
}
