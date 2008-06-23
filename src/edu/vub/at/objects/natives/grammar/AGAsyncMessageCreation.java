/**
 * AmbientTalk/2 Project
 * AGAsyncMessageCreation.java created on Jul 24, 2006 at 7:45:37 PM
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
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATAsyncMessageCreation;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.OBJLexicalRoot;

/**
 * AGAsyncMessageCreation implements the ATAsyncMessageCreation interface natively. It is a container for the
 * message's selector, a table of arguments and a number of annotations (which later become the message's type tags)
 * 
 * @author tvcutsem
 * @author smostinc
 */
public class AGAsyncMessageCreation extends AGMessageCreation implements ATAsyncMessageCreation {

	public AGAsyncMessageCreation(ATSymbol selector, ATTable arguments, ATExpression annotations) {
		super(selector, arguments, annotations);
	}
	
	protected ATMessage createMessage(ATContext ctx, ATSymbol selector, ATTable evaluatedArgs, ATTable annotations) throws InterpreterException {
		return OBJLexicalRoot._INSTANCE_.base_reflectOnActor().base_createMessage(selector, evaluatedArgs, annotations);
	}
	
	protected ATObject newQuoted(ATSymbol quotedSel, ATTable quotedArgs, ATExpression quotedAnnotations) {
		return new AGAsyncMessageCreation(quotedSel, quotedArgs, quotedAnnotations);
	}

	protected String getMessageToken() { return "<-"; }

}
