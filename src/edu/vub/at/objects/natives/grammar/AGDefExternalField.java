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

import edu.vub.at.actors.ATFarObject;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATDefExternalField;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATText;

/**
 * Represents the abstract grammar for defining external fields.
 * Examples:
 *   <tt>o.x := 5</tt>
 *   <tt>num.+ := "foo"</tt>
 *   
 *  @author tvcutsem
 */
public class AGDefExternalField extends NATAbstractGrammar implements ATDefExternalField {

	private final ATSymbol rcvNam_;
	private final ATSymbol name_;
	private final ATExpression valueExp_;
	
	public AGDefExternalField(ATSymbol rcv, ATSymbol name, ATExpression value) {
		rcvNam_ = rcv;
		name_ = name;
		valueExp_ = value;
	}
	
	public ATSymbol base_getReceiver() { return rcvNam_; }
	public ATSymbol base_getName() { return name_; }
	public ATExpression base_getValueExpression() { return valueExp_; }
	
	/**
	 * Defines a new field in the object denoted by the receiver symbol. The return value is NIL.
	 * 
	 * AGDEFEXTFLD(rcv,nam,val).eval(ctx) =
	 *   rcv.eval(ctx).addField(nam, val.eval(ctx))
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		rcvNam_.meta_eval(ctx).meta_defineField(name_, valueExp_.meta_eval(ctx));
		return NATNil._INSTANCE_;
	}

	/**
	 * Quoting an external field definition results in a new quoted external field definition.
	 * 
	 * AGDEFEXTFLD(rcv,nam,val).quote(ctx) = AGDEFEXTFLD(rcv.quote(ctx), nam.quote(ctx), val.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGDefExternalField(rcvNam_.meta_quote(ctx).base_asSymbol(),
									name_.meta_quote(ctx).base_asSymbol(),
									valueExp_.meta_quote(ctx).base_asExpression());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("def " + rcvNam_.meta_print().javaValue
				+ "." + name_.meta_print().javaValue
				+ " := " + valueExp_.meta_print().javaValue);
	}
    /* -----------------------------
     * -- Object Passing protocol --
     * ----------------------------- */

    /**
     * Passing a mutable and compound object implies making a new instance of the 
     * object while invoking pass on all its constituents.
     */
    public ATObject meta_pass(ATFarObject client) throws InterpreterException {
    		return new AGDefExternalField(rcvNam_.meta_pass(client).base_asSymbol(), name_.meta_pass(client).base_asSymbol(), valueExp_.meta_pass(client).base_asExpression());
    }
    
    public ATObject meta_resolve() throws InterpreterException {
    		return new AGDefExternalField(rcvNam_.meta_resolve().base_asSymbol(), name_.meta_resolve().base_asSymbol(), valueExp_.meta_resolve().base_asExpression());
    }
}
