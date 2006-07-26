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

import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATText;
import edu.vub.at.objects.grammar.ATSymbol;

import java.util.HashMap;

/**
 * @author tvc
 *
 * The native implementation of a symbol AG element.
 * Symbols should only be created via a call to AGSymbol.alloc
 * This ensures that symbols remain unique within one AmbientTalk VM.
 */
public final class AGSymbol extends NATAbstractGrammar implements ATSymbol {

	private static final HashMap stringPool = new HashMap();
	
	private final ATText txt_;
	
	private AGSymbol(ATText txt) {
		txt_ = txt;
	}
	
	public static final AGSymbol alloc(ATText txt) {
		AGSymbol existing = (AGSymbol) stringPool.get(txt);
		if (existing == null) {
			existing = new AGSymbol(txt);
			stringPool.put(txt, existing);
		}
		return existing;
	}
	
	public ATText getText() { return txt_; }

	/* (non-Javadoc)
	 * @see edu.vub.at.objects.ATAbstractGrammar#meta_eval(edu.vub.at.objects.ATContext)
	 */
	public ATObject meta_eval(ATContext ctx) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.vub.at.objects.ATAbstractGrammar#meta_quote(edu.vub.at.objects.ATContext)
	 */
	public ATAbstractGrammar meta_quote(ATContext ctx) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean equals(Object other) {
		// pointer equality is valid for symbols as they are pooled
		return this == other;
	}

}
