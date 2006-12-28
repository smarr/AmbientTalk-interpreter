/**
 * AmbientTalk/2 Project
 * NATObservable.java created on Dec 5, 2006 at 8:57:18 PM
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
package edu.vub.at.actors.natives;

import edu.vub.at.actors.ATFarReference;
import edu.vub.at.actors.ATObservable;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.util.MultiMap;

import java.util.Iterator;
import java.util.Set;

/**
 * NATObservable is the superclass of all AmbientTalk objects which need to be equipped 
 * with observer functionality. The installed observers are closures which are coupled
 * to a symbol describing the event they are interested in.
 *
 * @author smostinc
 */
public class NATObservable extends NATNil implements ATObservable {

	protected final MultiMap observers_ = new MultiMap();
	
	/**
	 * Installs an observer on this object
	 * @param event a symbol denoting a potentially interesting event
	 * @param observer a closure to be triggered when the event is fired
	 * @return a closure which can be applied to cancel the observer's subscription
	 */
	public ATClosure base_upon_do_(final ATSymbol event, final ATClosure observer) {
		synchronized(observers_) {
			observers_.put(event, observer);
		}
		
		return new NativeClosure(this) {
			public ATObject base_apply(ATTable args) {
				synchronized(observers_) {
					return NATBoolean.atValue(observers_.removeValue(event, observer));			
				}
			}
		};
	}
	
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
	public ATNil base_fire_withArgs_(ATSymbol event, NATTable arguments) throws InterpreterException {
		synchronized (observers_) {
			Set eventObservers = (Set)observers_.get(event);
			if(eventObservers != null) {
				for (Iterator iter = eventObservers.iterator(); iter.hasNext();) {
					ATClosure observer = (ATClosure) iter.next();
					this.meta_send(new NATAsyncMessage(this, observer, Evaluator._APPLY_, arguments));
				}
			}
		}
		return NATNil._INSTANCE_;
	}

    /* -----------------------------
     * -- Object Passing protocol --
     * ----------------------------- */

    /**
     * TODO Proper semantics
     */
    public ATObject meta_pass(ATFarReference client) throws InterpreterException {
    		throw new RuntimeException("Attempting to pass an observable - not yet implemented");
    }
}
