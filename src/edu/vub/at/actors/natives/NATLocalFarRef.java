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

import java.util.Iterator;
import java.util.Vector;

import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;
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
	private transient Vector outboxOfOriginalMessages_;
	private transient Vector outboxOfSerializedMessages_;
	
	public NATLocalFarRef(ELActor farObjectHost, ATObjectID objectId, ATTypeTag[] types, ELActor myHost, boolean isConnected) {
		super(objectId, types, myHost, isConnected);
		farObjectHost_ = farObjectHost;
	}

	protected void transmit(ATAsyncMessage passedMessage) throws InterpreterException {
		Packet serializedMessage = new Packet(passedMessage.toString(),
				NATTable.of(this, passedMessage));
		
		synchronized (this) {
			if (!connected_) {
				//a local far reference buffers messages when it gets (software) disconnected.
				//note that a local far reference will never be hardware disconnected.
				if (outboxOfOriginalMessages_ == null) {
					outboxOfOriginalMessages_ = new Vector(2);
					outboxOfSerializedMessages_ = new Vector(2);
				}
				outboxOfOriginalMessages_.add(passedMessage);
				outboxOfSerializedMessages_.add(serializedMessage);
			} else{
				farObjectHost_.event_localAccept(this, serializedMessage);
			}; 
		}
	}
	
	//called to notify connected/disconnected
	protected synchronized void notifyStateToSendLoop(boolean state){
		//if notifying reconnection, flush the outbox
		if (state) {
		  if (outboxOfSerializedMessages_ != null && !outboxOfSerializedMessages_.isEmpty()) {
			  for (Iterator Iter = outboxOfSerializedMessages_.iterator(); Iter.hasNext();) {
				 farObjectHost_.event_localAccept(this, (Packet) Iter.next());
			  }
			  // empty the outbox
			  outboxOfOriginalMessages_.clear();
			  outboxOfSerializedMessages_.clear();
		  }
		}
	}
	
	/**
	 * The 'outbox' of a far reference to a local actor is normally empty, except if
	 * the local far reference was disconnected by means of a 'soft disconnect'.
	 */
	public ATTable meta_retractUnsentMessages() throws InterpreterException {
		if (outboxOfOriginalMessages_ != null && !outboxOfOriginalMessages_.isEmpty()) {
	        ATObject[] messages = new ATObject[outboxOfOriginalMessages_.size()];
	        outboxOfOriginalMessages_.toArray(messages);
	        // empty both outboxes
			outboxOfOriginalMessages_.clear();
			outboxOfSerializedMessages_.clear();
			return NATTable.atValue(messages);	
		} else {
			return NATTable.EMPTY;		
		}
	}
	
	public ELActor getFarHost() {
		return farObjectHost_;
	}

}
