/**
 * AmbientTalk/2 Project
 * OBJDynamicRoot.java created on 11-aug-2006 at 12:54:38
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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XUndefinedField;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATSymbol;

/**
 * @author tvc
 *
 * An instance of the class OBJDynamicRoot represents the root of all dynamic delegation
 * hierarchies within an actor. Since a dynamic root is sealed (it cannot be modified)
 * and contains no mutable fields, it should be possible to share a singleton instance of
 * this class among all actors.
 * 
 * The dynamic root is an empty sentinel object that is primarily used to end the
 * recursive nature of a number of meta_ methods of NATObject. Primary examples include
 * meta_respondsTo and meta_select, which traverse the dynamic delegation chain.
 * 
 * When reaching the top of the dynamic delegation chain, without success of finding a selector,
 * the meta_ method of OBJDynamicRoot should end the delegation with appropriate semantics.
 */
public final class OBJDynamicRoot extends NATNil {

	public static final OBJDynamicRoot _INSTANCE_ = new OBJDynamicRoot();
	
	/**
	 * Constructor made private for singleton design pattern
	 */
	private OBJDynamicRoot() { }
	
	/**
	 * The respondsTo delegation chain is ended by returning false:
	 * the selector has not been found.
	 */
	public ATBoolean meta_respondsTo(ATSymbol selector) throws NATException {
		return NATBoolean._FALSE_;
	}
	
	/**
	 * The select delegation chain is not ended by simply raising an error.
	 * Instead, the initial receiver of the message is notified of the failure.
	 * This is the good old Smalltalk 'doesNotUnderstand' behaviour, except that
	 * 'doesNotUnderStand' is a meta-level operation applied to mirrors in AmbientTalk.
	 */
	public ATObject meta_select(ATObject receiver, ATSymbol selector) throws NATException {
		return receiver.meta_doesNotUnderstand(selector);
	}
	
	public ATNil meta_assignField(ATSymbol selector, ATObject value) throws NATException {
		throw new XUndefinedField("field assignment", selector.getText().asNativeText().javaValue);
	}
	
	/**
	 * The dynamic root is a singleton
	 */
	public ATObject meta_clone() {
		return this;
	}
	
}
