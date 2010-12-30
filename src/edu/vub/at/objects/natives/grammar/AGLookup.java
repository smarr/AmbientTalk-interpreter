/**
 * AmbientTalk/2 Project
 * AGApplication.java created on 10-jul-2007 at 10:13:31
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
import edu.vub.at.objects.grammar.ATLookup;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATText;
import edu.vub.util.TempFieldGenerator;

import java.util.HashSet;
import java.util.Set;

/**
 * The native implementation of a lexical lookup AG element.
 * Example: <tt>&foo</tt>
 * 
 * @author smostinc
 */
public final class AGLookup extends AGExpression implements ATLookup {

	private final ATSymbol selector_;
	
	/** construct a new lookup AG element */
	public AGLookup(ATSymbol fun) {
		selector_ = fun;
	}
	
	public ATSymbol base_selector() { return selector_; }

	/**
	 * To evaluate a lexical lookup, the function name is sought for in the lexical scope.
	 * 
	 * AGLKU(nam).eval(ctx) = ctx.lex.lookup(nam)
	 * 
	 * @return a closure containing the requested function.
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		return ctx.base_lexicalScope().impl_lookup(selector_);
	}

	/**
	 * Quoting a lookup results in a new quoted lookup.
	 * 
	 * AGAPL(sel,arg).quote(ctx) = AGAPL(sel.quote(ctx), arg.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGLookup(selector_.meta_quote(ctx).asSymbol());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("&" + selector_.meta_print().javaValue);
	}
	
	public NATText impl_asUnquotedCode(TempFieldGenerator objectMap) throws InterpreterException {
		return NATText.atValue("&" + selector_.impl_asUnquotedCode(objectMap).javaValue);
	}
	
	/**
	 * FV(&nam) = { nam }
	 */
	public Set impl_freeVariables() throws InterpreterException {
        HashSet singleton = new HashSet();
        singleton.add(selector_);
        return singleton;
	}
	
	public Set impl_quotedFreeVariables() throws InterpreterException {
		return selector_.impl_quotedFreeVariables();
	}

}
