/**
 * AmbientTalk/2 Project
 * XObjectOffline.java created on 12-mrt-2007 at 17:17:18
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
package edu.vub.at.exceptions;

import edu.vub.at.actors.id.ATObjectID;
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.coercion.NativeStripes;

/**
 * An XObjectOffline exception is raised when an object ID cannot be resolved to a local
 * object because it has been taken offline previously.
 * 
 *
 * @author tvcutsem
 */
public final class XObjectOffline extends InterpreterException {

	private static final long serialVersionUID = -4121266169861882140L;

	ATObjectID objectId_;
	
	/**
	 * Constructor signalling that a particular object id could not be resolved by the 
	 * destination actor since the object was previously taken offline.
	 * @param id the object id which failed to resolve to local object
	 */
	public XObjectOffline(ATObjectID id) {
		super("Asked for an object id that was taken offline: " + id);
		objectId_ = id;
	}
	
	public ATStripe getStripeType() {
		return NativeStripes._OBJECTOFFLINE_;
	}
	
	/**
	 * @return the object id which failed to resolve to local object
	 */
	public ATObjectID getObjectId(){
		return objectId_;
	}

}
