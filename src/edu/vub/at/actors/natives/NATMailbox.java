/**
 * AmbientTalk/2 Project
 * NATMailbox.java created on Oct 16, 2006 at 3:32:35 PM
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

import edu.vub.at.actors.ATActor;
import edu.vub.at.actors.ATMailbox;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATText;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * The class NATMailbox implements a dynamically resizable table which can in principle
 * take any ambienttalk object as an element. The default mailboxes accumulate 
 * <ul>
 *   <li>async messages to be processed (inbox),</li>
 *   <li>async messages to be transmitted (outbox). </li>
 *</ul>
 *
 * @author smostinc
 */
public class NATMailbox extends NATObservable implements ATMailbox {

	public static final ATSymbol _ADD_ = AGSymbol.jAlloc("uponAdditionDo:");
	public static final ATSymbol _DELETE_ = AGSymbol.jAlloc("uponDeletionDo:");
	
	private LinkedList elements_ = new LinkedList();
	private final ATActor owner_;
	private final ATSymbol name_;
	
	public NATMailbox(ATActor owner, ATSymbol name) {
		owner_ = owner;
		name_ = name;
	}

	// used to clone a mailbox as well as to create mapped equivalents
	private NATMailbox(ATActor owner, ATSymbol name, LinkedList elements) {
		owner_ = owner;
		name_ = name;
		elements_ = elements;
	}
	
	public ATClosure base_uponAdditionDo(ATClosure additionObserver) {
		return base_upon_do_(_ADD_, additionObserver);
	}

	public ATClosure base_uponDeletionDo(ATClosure deletionObserver) {
		return base_upon_do_(_DELETE_, deletionObserver);
	}

	public ATActor meta_getActor() {
		return owner_;
	}

	public ATSymbol base_getName() {
		return name_;
	}

	public ATNumber base_getLength() {
		synchronized(elements_) {
			return NATNumber.atValue(elements_.size());
		}
	}

	public ATObject base_enqueue(ATObject value) throws InterpreterException {
		synchronized (elements_) {
			elements_.addLast(value);
			elements_.notify();
		}
		
		base_fire_withArgs_(_ADD_, NATTable.atValue(new ATObject[] { value }));

		return value;
	}

	public ATObject base_dequeue() throws InterpreterException {
		ATObject result;
		
		synchronized (elements_) {
			while(elements_.isEmpty()) {
				try {
					elements_.wait();
				} catch (InterruptedException e) { }
			}
			result = (ATObject)elements_.removeFirst();
		}
		
		base_fire_withArgs_(_DELETE_, NATTable.atValue(new ATObject[] { result }));

		return result;
	}
	
	public ATBoolean base_remove(ATObject toBeRemoved) throws InterpreterException {
		boolean result;
		
		synchronized (elements_) {
			result = elements_.remove(toBeRemoved);
		};
		
		if(result)
			base_fire_withArgs_(_DELETE_, NATTable.atValue(new ATObject[] { toBeRemoved }));
		
		return NATBoolean.atValue(result);
	}

	public ATObject base_at(ATNumber index) throws InterpreterException {
		synchronized(elements_) {
			return (ATObject)elements_.get(index.asNativeNumber().javaValue);
		}
	}

	public ATObject base_atPut(ATNumber index, ATObject value)
			throws InterpreterException {
		synchronized(elements_) {
			elements_.set(
					index.asNativeNumber().javaValue,
					value);
			return value;
		}
	}

	public ATBoolean base_isEmpty() {
		synchronized(elements_) {
			return NATBoolean.atValue(elements_.isEmpty());
		}
	}

	public ATNil base_each_(ATClosure clo) throws InterpreterException {
		synchronized(elements_) {
			for (Iterator i = elements_.iterator(); i.hasNext();) {
				ATObject element = (ATObject) i.next();
				
				clo.base_apply(NATTable.atValue(new ATObject[] { element }));
			}
			return NATNil._INSTANCE_;
		}
	}

	public ATObject base_map_(ATClosure clo) throws InterpreterException {
		synchronized(elements_) {
			Vector mappedElements = new Vector(elements_.size());
			
			for (Iterator i = elements_.iterator(); i.hasNext();) {
				ATObject element = (ATObject) i.next();
				
				mappedElements.add(
						clo.base_apply(NATTable.atValue(new ATObject[] { element })));
			}
			return new NATMailbox(owner_, AGSymbol.jAlloc("mapped-" + name_.base_getText().asNativeText().javaValue), new LinkedList(mappedElements));
		}
	}

	public ATObject base_with_collect_(ATObject init, ATClosure clo)
			throws InterpreterException {
		synchronized(elements_) {
			for (Iterator i = elements_.iterator(); i.hasNext();) {
				ATObject element = (ATObject) i.next();
				
				init = clo.base_apply(NATTable.atValue(new ATObject[] { init, element }));
			}
			return init;
		}
	}

	public ATText base_implode() throws InterpreterException {
		throw new XIllegalOperation("Mailboxes cannot be imploded as they are not tables of characters");
	}

	public ATText base_join(ATText txt) throws InterpreterException {
		throw new XIllegalOperation("Mailboxes cannot be joined with a text as they are not tables of characters");
	}

	public ATTable base_select(ATNumber start, ATNumber stop)
			throws InterpreterException {
		synchronized(elements_) {
			elements_.subList(start.asNativeNumber().javaValue, stop.asNativeNumber().javaValue);
			
			return new NATMailbox(
					owner_, 
					AGSymbol.jAlloc("selectedFrom-" + name_.base_getText().asNativeText().javaValue),
					new LinkedList(elements_.subList(start.asNativeNumber().javaValue, stop.asNativeNumber().javaValue)));
		}
	}

	public ATTable base_filter_(ATClosure clo) throws InterpreterException {
		// TODO Auto-generated method stub
		return null;
	}

}
