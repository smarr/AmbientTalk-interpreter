/**
 * AmbientTalk/2 Project
 * AGDefExternalField.java created on 16-nov-2006 at 8:31:45
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
package edu.vub.at.objects.natives.grammar;

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATDefExternalField;
import edu.vub.at.objects.grammar.ATDefinition;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATText;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents the abstract grammar for defining external fields.
 *   
 *  @author tvcutsem
 */
public class AGDefExternalField extends AGDefinition implements ATDefExternalField {

	private final ATSymbol rcvNam_;
	private final ATSymbol name_;
	private final ATExpression valueExp_;
	
	public AGDefExternalField(ATSymbol rcv, ATSymbol name, ATExpression value) {
		rcvNam_ = rcv;
		name_ = name;
		valueExp_ = value;
	}
	
	public ATSymbol base_receiver() { return rcvNam_; }
	public ATSymbol base_name() { return name_; }
	public ATExpression base_valueExpression() { return valueExp_; }
	
	/**
	 * Defines a new field in the object denoted by the receiver symbol.
	 * The return value is the value of the field.
	 * 
	 * AGDEFEXTFLD(rcv,nam,val).eval(ctx) =
	 *   rcv.eval(ctx).addField(nam, val.eval(ctx))
	 *   
	 * @throws XIllegalOperation if the receiver is an instance of a native type (whose field maps are sealed).
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		ATObject receiver = rcvNam_.meta_eval(ctx);
		ATObject val = valueExp_.meta_eval(ctx);
		receiver.meta_defineField(name_, val);
		return val;
	}

	/**
	 * Quoting an external field definition results in a new quoted external field definition.
	 * 
	 * AGDEFEXTFLD(rcv,nam,val).quote(ctx) = AGDEFEXTFLD(rcv.quote(ctx), nam.quote(ctx), val.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGDefExternalField(rcvNam_.meta_quote(ctx).asSymbol(),
									name_.meta_quote(ctx).asSymbol(),
									valueExp_.meta_quote(ctx).asExpression());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("def " + rcvNam_.meta_print().javaValue
				+ "." + name_.meta_print().javaValue
				+ " := " + valueExp_.meta_print().javaValue);
	}
	
	/**
	 * IV(def o.x := val) = { }
	 */
	public Set impl_introducedVariables() throws InterpreterException {
		return new HashSet();
	}

	/**
	 * FV(def o.x := val) = FV(val) U { o }
	 */
	public Set impl_freeVariables() throws InterpreterException {
		Set fvValueExp = valueExp_.impl_freeVariables();
		fvValueExp.add(rcvNam_);
		return fvValueExp;
	}
	
	
	public Set impl_quotedFreeVariables() throws InterpreterException {
		Set qfv = valueExp_.impl_quotedFreeVariables();
		qfv.addAll(rcvNam_.impl_quotedFreeVariables());
		qfv.addAll(name_.impl_quotedFreeVariables());
		return qfv;
	}
	
}
