/**
 * AmbientTalk/2 Project
 * NATMethod.java created on Jul 24, 2006 at 11:30:35 PM
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
package edu.vub.at.objects.natives;

import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATSymbol;
import edu.vub.at.objects.ATTable;

/**
 * @author smostinc
 *
 * NATMethod implements methods as named functions which are in fact simply containers
 * for a name, a table of arguments and a body.
 */
public class NATMethod extends NATNil implements ATMethod {

	private final ATSymbol 			name_;
	private final ATTable 			arguments_;
	private final ATAbstractGrammar	body_;
	
	
	public NATMethod(ATSymbol name, ATTable arguments, ATAbstractGrammar body) {
		name_ 		= name;
		arguments_ 	= arguments;
		body_ 		= body;
	}

	public ATSymbol getName() {
		return name_;
	}

	public ATTable getArguments() {
		return arguments_;
	}

	public ATAbstractGrammar getBody() {
		return body_;
	}

}
