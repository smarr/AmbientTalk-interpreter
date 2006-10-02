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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATDefField;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATText;

/**
 * @author tvc
 *
 * AGDefField represents the abstract grammar for defining fields.
 * Examples:
 *   <tt>x := 5</tt>
 *   <tt>+ := "foo"</tt>
 */
public final class AGDefField extends NATAbstractGrammar implements ATDefField {

	private final ATSymbol name_;
	private final ATExpression valueExp_;
	
	public AGDefField(ATSymbol name, ATExpression value) {
		name_ = name;
		valueExp_ = value;
	}
	
	public ATSymbol base_getName() { return name_; }
	public ATExpression getValue() { return valueExp_; }
	
	/**
	 * Defines a new field in the current scope. The return value is NIL.
	 * 
	 * AGDEFFIELD(nam,val).eval(ctx) =
	 *   ctx.scope.addField(nam, val.eval(ctx))
	 */
	public ATObject meta_eval(ATContext ctx) throws NATException {
		ctx.base_getLexicalScope().meta_defineField(name_, valueExp_.meta_eval(ctx));
		return NATNil._INSTANCE_;
	}

	/**
	 * Quoting a field definition results in a new quoted field definition.
	 * 
	 * AGDEFFIELD(nam,val).quote(ctx) = AGDEFFIELD(nam.quote(ctx), val.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws NATException {
		return new AGDefField(name_.meta_quote(ctx).asSymbol(), valueExp_.meta_quote(ctx).asExpression());
	}
	
	public NATText meta_print() throws XTypeMismatch {
		return NATText.atValue("def " + name_.meta_print().javaValue + " := " + valueExp_.meta_print().javaValue);
	}
	
}
