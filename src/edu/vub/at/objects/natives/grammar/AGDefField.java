/**
 * AmbientTalk/2 Project
 * AGDefField.java created on 26-jul-2006 at 12:14:43
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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATDefField;
import edu.vub.at.objects.grammar.ATDefinition;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATText;
import edu.vub.util.TempFieldGenerator;

import java.util.HashSet;
import java.util.Set;

/**
 * @author tvc
 *
 * AGDefField represents the abstract grammar for defining fields.
 * Examples:
 *   <tt>x := 5</tt>
 *   <tt>+ := "foo"</tt>
 */
public final class AGDefField extends AGDefinition implements ATDefField {

	private final ATSymbol name_;
	private final ATExpression valueExp_;
	
	public AGDefField(ATSymbol name, ATExpression value) {
		name_ = name;
		valueExp_ = value;
	}
	
	public ATSymbol base_name() { return name_; }
	public ATExpression base_valueExpression() { return valueExp_; }
	
	/**
	 * Defines a new field in the current scope. The return value is
	 * the evaluated value.
	 * 
	 * AGDEFFIELD(nam,val).eval(ctx) =
	 *   ctx.scope.addField(nam, val.eval(ctx))
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		ATObject val = valueExp_.meta_eval(ctx);
		ctx.base_lexicalScope().meta_defineField(name_, val);
		return val;
	}

	/**
	 * Quoting a field definition results in a new quoted field definition.
	 * 
	 * AGDEFFIELD(nam,val).quote(ctx) = AGDEFFIELD(nam.quote(ctx), val.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGDefField(name_.meta_quote(ctx).asSymbol(), valueExp_.meta_quote(ctx).asExpression());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("def " + name_.meta_print().javaValue + " := " + valueExp_.meta_print().javaValue);
	}
	
	public NATText impl_asUnquotedCode(TempFieldGenerator objectMap) throws InterpreterException {
		return NATText.atValue("def " + name_.impl_asUnquotedCode(objectMap).javaValue + " := " + valueExp_.impl_asUnquotedCode(objectMap).javaValue);
	}
	
	/**
	 * IV(def x := val) = { x }
	 */
	public Set impl_introducedVariables() throws InterpreterException {
		HashSet singleton = new HashSet();
		singleton.add(name_);
		return singleton;
	}
	
	/**
	 * FV(def var := exp) = FV(exp) \ { var }
	 */
	public Set impl_freeVariables() throws InterpreterException {
		Set fvValueExp = valueExp_.impl_freeVariables();
		fvValueExp.remove(name_);
		return fvValueExp;
	}
	
	
	public Set impl_quotedFreeVariables() throws InterpreterException {
		Set qfv = valueExp_.impl_quotedFreeVariables();
		qfv.addAll(name_.impl_quotedFreeVariables());
		return qfv;
	}

}
