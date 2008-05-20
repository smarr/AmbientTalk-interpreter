/**
 * AmbientTalk/2 Project
 * NATContext.java created on Jul 24, 2006 at 11:22:36 PM
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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;

/**
 * NATContext is a purely functional implementation of the ATContext interface.
 * It allows for storing the context parameters (scope self super) in such a way 
 * that no ill-placed assignment may affect other frames on the stack.
 * 
 * @author smostinc
 */
public class NATContext extends NATByCopy implements ATContext {

	private final ATObject scope_;
	private final ATObject self_;
	
	public NATContext(ATObject scope, ATObject self) {
		scope_ = scope;
		self_ = self;
	}

	public ATObject base_lexicalScope() {
		return scope_;
	}

	public ATObject base_receiver() {
		return self_;
	}

	public ATContext base_withLexicalEnvironment(ATObject scope) {
		return new NATContext(scope, self_);
	}
	
	public ATContext base_withDynamicReceiver(ATObject self) {
		return new NATContext(scope_, self);		
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<context("+scope_.meta_print().javaValue+
				                      ","+self_.meta_print().javaValue+")>");
	}
	
    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._CONTEXT_, NativeTypeTags._ISOLATE_);
    }
    
	public ATObject meta_clone() throws InterpreterException {
		return this;
	}

}