/**
 * AmbientTalk/2 Project
 * ATObservable.java
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
package edu.vub.at.actors;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATTable;

/**
 * ATObservable is the common interface used to create callbacks.
 *
 * @author smostinc
 * @deprecated
 */
public interface ATObservable extends ATObject {

	/**
	 * Installs an observer on this object
	 * @param event a symbol denoting a potentially interesting event
	 * @param observer a closure to be triggered when the event is fired
	 * @return a closure which can be applied to cancel the observer's subscription
	 */
	public abstract ATClosure base_upon_do_(ATSymbol event, ATClosure observer);

	/**
	 * Fires an event with the given arguments. All the provided closures are applied
	 * ASYNCHRONOUSLY. This implies that exceptions thrown by such a closure will not
	 * prevent other observers from getting triggered. Moreover, observers are then
	 * triggered breadth-first as opposed to depth-first as is the case with synchronous
	 * invocations.
	 * 
	 * @param event - the event being fired
	 * @param arguments - a possibly empty table of arguments to the event
	 * @return nil
	 */
	public abstract ATNil base_fire_withArgs_(ATSymbol event, NATTable arguments) throws InterpreterException;

}