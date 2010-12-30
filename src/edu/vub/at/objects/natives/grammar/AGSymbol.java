/**
 * AmbientTalk/2 Project
 * AGSymbol.java created on 26-jul-2006 at 16:21:55
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
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATText;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.parser.SourceLocation;
import edu.vub.util.TempFieldGenerator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @author tvcutsem
 *
 * The native implementation of a symbol AG element.
 * Symbols should only be created via a call to AGSymbol.alloc
 * This ensures that symbols remain unique within one AmbientTalk VM.
 */
public class AGSymbol extends AGExpression implements ATSymbol {

	/** hashmap shared by ALL actors/VMs, hence, thread access should be explicitly synchronized */
	protected static final HashMap _STRINGPOOL_ = new HashMap();

	private final String txt_;
	
	protected AGSymbol(String txt) {
		txt_ = txt;
	}
	
	/**
	 * Allocate and return a unique symbol object denoting the given
	 * text string.
	 */
	public static AGSymbol jAlloc(String name) {
		synchronized (_STRINGPOOL_) {
			AGSymbol existing = (AGSymbol) _STRINGPOOL_.get(name);
			if (existing == null) {
				existing = new AGSymbol(name);
				_STRINGPOOL_.put(name, existing);
			}
			return existing;	
		}
	}
	
	/**
	 * Allocate and return a unique symbol denoting the given native text.
	 */
	public static final AGSymbol alloc(NATText txt) {
		return jAlloc(txt.javaValue);
	}
	
	public ATText base_text() { return NATText.atValue(txt_); }
	
	/**
	 * To evaluate a symbol reference, look up the symbol in the lexical scope.
	 * 
	 * AGSYM(txt).eval(ctx) = ctx.scope.lookup(AGSYM(txt))
	 * 
	 * @return the value bound to this symbol in the lexical environment
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		return ctx.base_lexicalScope().impl_callField(this);
	}

	/**
	 * Quoting a symbol results in the same symbol.
	 * 
	 * sym.quote(ctx) = sym
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return this;
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue(txt_);
	}
	
	public NATText impl_asCode(TempFieldGenerator objectMap) throws InterpreterException {
		return NATText.atValue("`" + txt_);
	}
	
	public NATText impl_asUnquotedCode(TempFieldGenerator objectMap) throws InterpreterException {
		return NATText.atValue(txt_);
	}
	
	public boolean isSymbol() {
		return true;
	}
	
	public ATSymbol asSymbol() {
		return this;
	}
	
	public boolean isAssignmentSymbol() {
		return false;
	}
	
	public AGAssignmentSymbol asAssignmentSymbol() {
		return (AGAssignmentSymbol) AGAssignmentSymbol.jAlloc(txt_+":=");
	}
	
	// comparison and identity operations
	
	public boolean equals(Object other) {
		// pointer equality is valid for symbols as they are pooled
		return this == other;
	}
	
    public ATBoolean base__opeql__opeql_(ATObject comparand) throws InterpreterException {
        return NATBoolean.atValue(this == comparand);
    }
	
	// important as AGSymbols are often used as keys in hash tables
	
	public int hashCode() {
		return this.txt_.hashCode();
	}
	
	public String toString() {
		return txt_;
	}
	
	/**
	 * After deserialization, ensure that the symbol remains unique.
	 */
	public ATObject meta_resolve() throws InterpreterException {
		return jAlloc(txt_);
	}
	
	/**
	 * FV(nam) = { nam }
	 */
	public Set impl_freeVariables() throws InterpreterException {
        HashSet singleton = new HashSet();
        singleton.add(this);
        return singleton;
	}
	
	/**
	 * Within a quoted expression, a variable reference is not considered
	 * a free variable, e.g. `(x)
	 */
	public Set impl_quotedFreeVariables() throws InterpreterException {
		return new HashSet();
	}
	
	// since symbols are interned, they are shared among parse trees
	// (even among actors!) so their source location is meaningless
    public SourceLocation impl_getLocation() { return null; }
    public void impl_setLocation(SourceLocation loc) {}

}
