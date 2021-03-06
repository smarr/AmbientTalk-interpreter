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
import edu.vub.at.actors.ATLetter;
import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.actors.natives.NATActorMirror.NATLetter;
import edu.vub.at.actors.natives.NATFarReference.NATOutboxLetter;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.util.logging.Logging;

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

	public NATLocalFarRef(ELActor farObjectHost, ATObjectID objectId, ATTypeTag[] types, ELActor myHost, boolean isConnected) {
		super(objectId, types, myHost, isConnected);
		farObjectHost_ = farObjectHost;
	}
	
	protected void transmit(ATLetter letter) throws InterpreterException {
		synchronized (this) {
			if (!connected_) {
				outbox_.addLast(letter);
			} else{
				// the far reference itself is the receiver of the asynchronous message
				farObjectHost_.event_localAccept(this, letter.asNativeOutboxLetter().impl_getSerializedMessage());
			}
		}	
	}
	
	//called to notify connected/disconnected
	protected synchronized void notifyStateToSendLoop(boolean state){
		//if notifying reconnection, flush the outbox
		if (state) {
		  if (outbox_.size() > 0) {
			  for (Iterator Iter = outbox_.iterator(); Iter.hasNext();) {
				try {
					ATLetter next = (ATLetter)  Iter.next();
					NATOutboxLetter letter = next.asNativeOutboxLetter();
					 farObjectHost_.event_localAccept(this, letter.impl_getSerializedMessage());
				} catch (XTypeMismatch e) {
					Logging.RemoteRef_LOG.info(this + ": unexpected type mismatch: " + e.getMessage());
					e.printStackTrace();
				}
			  }
			  // empty the outbox
			  outbox_.clear();
		  }
		}
	}
	
	/**
	 * Note that the 'outbox' of a local far reference is normally empty, except if
	 * the local far reference was disconnected by means of a 'soft disconnect'.
	 * 
	 * In pseudocode this method does the following:
	 * def messages := [];
	 * outbox.each: { |letter|  letter.cancel();  messages := messages + [letter.message] };
	 * messages;
	 * 
	 */
	public ATTable meta_retractUnsentMessages() throws InterpreterException {
		synchronized (this) {
			if (outbox_.size() > 0 ) {
				ATObject[] messages = new ATObject[outbox_.size()];
				int i = 0;
				for (Iterator iterator = outbox_.iterator(); iterator.hasNext();) {
					ATLetter letter = (ATLetter) iterator.next();
					// no need to call cancel(), just clear the outbox at the end
					// since we'll be removing all letters. Also, calling cancel()
					// leads to a ConcurrentModificationException since it will try
					// to remove the letter while we are iterating over the list
					// letter.base_cancel();
					messages[i] = letter.base_message().asAsyncMessage();
					i = i + 1;
				}
				outbox_.clear(); // empty the outbox
				return NATTable.atValue(messages);	
			}
		}
		// if you arrive here outbox_.size == 0 thus it returns [];
		return NATTable.EMPTY;		
	}
	
	public ELActor getFarHost() {
		return farObjectHost_;
	}
	
}
