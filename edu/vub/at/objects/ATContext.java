/**
 * AmbientTalk/2 Project
 * ATContext.java created on Jul 23, 2006 at 11:35:12 AM
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
package edu.vub.at.objects;

/**
 * @author smostinc
 *
 * ATContext describes a triplet of scope pointers used during evaluation, namely
 * one for the lexical scope (where the lookup starts for receiverless messages),
 * one for the late-bound receiver and one for the parent object.
 */
public interface ATContext extends ATObject {
	
	/**
	 * Structural access to the lexical environment of the current context.
	 */
	public ATObject getLexicalEnvironment();
	
	/**
	 * Structural access to the receiver (self pseudovariable) in the current context.
	 */
	public ATObject getLateBoundReceiver();
	
	/**
	 * Structural access to the parent (super pseudovariable) in the current context.
	 */
	public ATObject getParentObject();

}