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

import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;

/**
 * @author smostinc
 *
 * NATContext is a purely functional implementation of the ATContext interface.
 * It allows for storing the context parameters (scope self super) in such a way 
 * that no ill-placed assignment may affect other frames on the stack.
 */
public class NATContext extends NATNil implements ATContext {

	private final ATObject scope_;
	private final ATObject self_;
	private final ATObject super_;
	
	NATContext(ATObject scope, ATObject self, ATObject zuper) {
		scope_ = scope;
		self_ = self;
		super_ = zuper;
	}

	public ATObject getLexicalEnvironment() {
		return scope_;
	}

	public ATObject getLateBoundReceiver() {
		return self_;
	}

	public ATObject getParentObject() {
		return super_;
	}

	public ATContext withLexicalEnvironment(ATObject scope) {
		return new NATContext(scope, self_, super_);
	}
	
	public ATContext withParentObject(ATObject zuper) {
		return new NATContext(scope_, self_, zuper);
	}
	
	public ATContext withDynamicReceiver(ATObject self, ATObject zuper) {
		return new NATContext(scope_, self, zuper);		
	}
	
	
	
}