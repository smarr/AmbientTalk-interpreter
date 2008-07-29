/**
 * AmbientTalk/2 Project
 * AGDefinition.java created on 29 jul 2008 at 14:42:21
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
import edu.vub.at.objects.grammar.ATDefinition;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.NativeATObject;

import java.util.Set;

/**
 * Common superclass for all definitional statements in the abstract grammar tree.
 * 
 * @author tvcutsem
 */
public abstract class AGDefinition extends NATAbstractGrammar implements ATDefinition {

	public boolean isDefinition() { return true; }
	public ATDefinition asDefinition() { return this; }

	/**
	 * My subclasses have to provide their own implementation for this, they cannot
	 * reuse the default implementation of {@link NativeATObject}
	 */
	public abstract Set impl_introducedVariables() throws InterpreterException;
	
}
