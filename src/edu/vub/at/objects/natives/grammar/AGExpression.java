/**
 * AmbientTalk/2 Project
 * AGExpression.java created on 31-jul-2006 at 15:28:56
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
import edu.vub.at.exceptions.XSerializationError;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATStatement;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.util.TempFieldGenerator;

/**
 * The common interface of all AGExpression abstract grammar elements.
 * 
 * @author tvcutsem
 */
public abstract class AGExpression extends NATAbstractGrammar implements ATExpression {
	
	public ATExpression asExpression() { return this; }

    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._EXPRESSION_, NativeTypeTags._ISOLATE_);
    }

    public NATText impl_asCode(TempFieldGenerator objectMap) throws InterpreterException {
    	NATText code = this.impl_asUnquotedCode(objectMap);
    	return NATText.atValue("`(" + code.javaValue + ")");
    }
    
    public NATText impl_asUnquotedCode(TempFieldGenerator objectMap) throws InterpreterException {
    	return this.impl_asCode(objectMap);
    }
	
}
