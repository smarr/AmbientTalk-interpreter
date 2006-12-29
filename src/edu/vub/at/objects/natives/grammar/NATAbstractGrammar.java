/**
 * AmbientTalk/2 Project
 * NATAbstractGrammar.java created on 26-jul-2006 at 11:57:00
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
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.natives.NATByCopy;
import edu.vub.at.objects.natives.NATText;

/**
 * @author tvc
 *
 * NATAbstractGrammar is the common superclass of all native ambienttalk objects
 * that represent abstract grammar parse tree elements. That is, any object that
 * can be returned as part of the AST produced by the native parser.
 */
public abstract class NATAbstractGrammar extends NATByCopy implements ATAbstractGrammar {

	// This is an empty superclass used only for proper documentation and
	// to identify which native objects can be output by the parser.
	
	// subclasses of NATAbstractGrammar will override meta_eval and meta_quote as appropriate,
	// except for the literal grammar elements which can inherit the self-evaluating behaviour of NATNil.
	
	public NATText meta_print() throws InterpreterException {
        throw new RuntimeException("all subclasses of NATAbstractGrammar should override the default behaviour of NATNil");
	}
	
}
