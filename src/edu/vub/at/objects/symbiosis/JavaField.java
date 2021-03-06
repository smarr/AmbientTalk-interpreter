/**
 * AmbientTalk/2 Project
 * JavaField.java created on 5-nov-2006 at 20:08:18
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
package edu.vub.at.objects.symbiosis;

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalArgument;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.natives.NATByRef;
import edu.vub.at.objects.natives.NATField;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

import java.lang.reflect.Field;

/**
 * A JavaField is a simple wrapper around a native java.lang.reflect.Field
 * 
 * @author tvcutsem
 */
public final class JavaField extends NATByRef implements ATField {

	private final Object host_;
	private final Field field_;

	public JavaField(Object host, Field f) {
		host_ = host;
		field_ = f;
	}

	public ATSymbol base_name() {
		return Reflection.downSelector(field_.getName());
	}

	public ATObject base_readField() throws InterpreterException {
		return Symbiosis.readField(host_, field_);
	}

	public ATObject base_writeField(ATObject newValue) throws InterpreterException {
		Symbiosis.writeField(host_, field_, newValue);
		return Evaluator.getNil();
	}

	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<java field:"+field_+">");
	}
	
	public ATField asField() {
		return this;
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
			if (newhost.isJavaObjectUnderSymbiosis()) {
				return new JavaField(newhost.asJavaObjectUnderSymbiosis().getWrappedObject(), field_);
			} else {
				throw new XIllegalArgument("Java Field re-initialization requires a symbiotic Java object, given " + newhost);
			}
		}
	}
	
    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._FIELD_);
    }

    public ATMethod base_accessor() throws InterpreterException {
    	return NATField.accessorForField(this);
    }
    
    public ATMethod base_mutator() throws InterpreterException {
    	return NATField.mutatorForField(this);
    }
    	
}
