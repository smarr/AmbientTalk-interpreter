/**
 * AmbientTalk/2 Project
 * NATLocalFarRef.java created on 22-dec-2006 at 11:34:15
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

import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATTable;

/**
 * Instances of NATLocalFarRef denote far references to objects 'local' to this address space.
 * That is, the far object is hosted by a local actor and hence message transmission is not subject
 * to network routing or partial failure and messages may be immediately scheduled in the inbox of
 * the recipient object's host actor.
 *
 * @author tvcutsem
 */
public class NATLocalFarRef extends NATFarReference {

	/** when serializing a far reference, the event loop stays home */
	private transient final ELActor farObjectHost_;
	
	public NATLocalFarRef(ELActor farObjectHost, ATObjectID objectId) {
		super(objectId);
		farObjectHost_ = farObjectHost;
	}

	protected ATObject transmit(ATAsyncMessage passedMessage) throws InterpreterException {
		farObjectHost_.event_accept(passedMessage);
		return NATNil._INSTANCE_;
	}
	
	/**
	 * The 'outbox' of a far reference into a local actor is always empty.
	 */
	public ATNil meta_transmit(ATClosure processor) throws InterpreterException {
		this.meta_send(new NATAsyncMessage(this, processor, Evaluator._APPLY_, NATTable.EMPTY));
		return NATNil._INSTANCE_;
	}
	
	public ELActor getFarHost() {
		return farObjectHost_;
	}

}
