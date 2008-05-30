package edu.vub.at.actors;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATObject;

/**
 * ATLetter is the public interface to an AmbientTalk letter. The inbox of
 * and actor consists of a set of objects that implement this interface.
 * @author stimberm
 */
public interface ATLetter extends ATObject {
	
	/**
	 * Returns the receiver of the letter
	 * @return the receiver of the letter
	 */
	public ATObject base_receiver() throws InterpreterException;
	
	/**
	 * Returns the message that is contained in the letter and
	 * that should be delivered to the receiver.
	 * @return an {@link ATAsyncMessage} denoting the message
	 */
	public ATAsyncMessage base_message() throws InterpreterException;
	
	/**
	 * Cancels the delivery of this letter. It is immediately removed
	 * from the actor's inbox.
	 */
	public ATObject base_cancel() throws InterpreterException;

}
