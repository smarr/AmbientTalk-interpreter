/**
 * AmbientTalk/2 Project
 * NATByRef.java created on 29-dec-2006 at 15:57:37
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
package edu.vub.at.objects.natives;

import edu.vub.at.actors.natives.ELActor;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.objects.ATObject;

/**
 * This class is the abstract superclass of all AT/2 objects that require by-reference
 * parameter passing semantics when passed between actors.
 *
 * @author tvcutsem
 */
public abstract class NATByRef extends NativeATObject {

	/**
	 * By reference objects serialize to a proxy representation. This proxy
	 * representation takes the form of a far reference to the local object.
	 */
	public ATObject meta_pass() throws InterpreterException {
		return ELActor.currentActor().getActorMirror().base_createReference(this);
	}
	
	/**
	 * By reference objects cannot be deserialized, by definition.
	 * It is an error to try to resolve an unserializable by reference object.
	 * The object on which meta_resolve is normally invoked is the proxy
	 * representation that was returned by this object's meta_pass method.
	 */
	public ATObject meta_resolve() throws InterpreterException {
		throw new XIllegalOperation("Cannot deserialize a by reference object: " + this);
	}
	
}
