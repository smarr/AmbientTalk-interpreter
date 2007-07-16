/**
 * AmbientTalk/2 Project
 * OBJNetwork.java created on 24-feb-2007 at 17:38:31
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
package edu.vub.at.actors.net;

import edu.vub.at.actors.natives.ELVirtualMachine;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.natives.NATByCopy;
import edu.vub.at.objects.natives.OBJNil;
import edu.vub.at.objects.natives.NATText;

/**
 * This class represents the singleton network object which
 * provides methods to interface with AT/2's distribution layer.
 *
 * @author tvcutsem
 */
public final class OBJNetwork extends NATByCopy {
	
	/**
	 * The singleton instance of the network object
	 */
	static public final OBJNetwork _INSTANCE_ = new OBJNetwork();
	
	/**
	 * Constructor made private for singleton design pattern
	 */
	private OBJNetwork() { }
	
	/**
	 * def online() { make the interpreter go online; return nil }
	 * After invoking this method, publications and subscriptions can interact
	 * with those of remote VMs.
	 */
	public ATNil base_online() {
		ELVirtualMachine.currentVM().event_goOnline();
		return OBJNil._INSTANCE_;
	}

	/**
	 * def offline() { make the interpreter go offline; return nil }
	 * Invoking this method causes remote references to become disconnected.
	 * 
	 */
	public ATNil base_offline() {
		ELVirtualMachine.currentVM().event_goOffline();
		return OBJNil._INSTANCE_;
	}
	
	/**
	 * After deserialization, ensure that the network object remains unique.
	 */
	public ATObject meta_resolve() throws InterpreterException {
		return OBJNetwork._INSTANCE_;
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<native object: network>");
	}
	
}
