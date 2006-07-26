/**
 * AmbientTalk/2 Project
 * AGMessageCreation.java created on 26-jul-2006 at 16:18:10
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
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATMessageCreation;
import edu.vub.at.objects.grammar.ATSymbol;

/**
 * @author tvc
 *
 * The native implementation of a first-class message creation AG element.
 */
public final class AGMessageCreation extends NATAbstractGrammar implements ATMessageCreation {

	private final ATSymbol selector_;
	private final ATTable arguments_;
	
	public AGMessageCreation(ATSymbol sel, ATTable args) {
		selector_ = sel;
		arguments_ = args;
	}
	
	public ATSymbol getSelector() { return selector_; }

	public ATTable getArguments() { return arguments_; }

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

}