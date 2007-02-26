/**
 * AmbientTalk/2 Project
 * SharedActorField.java created on Feb 25, 2007 at 9:02:22 AM
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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATSymbol;

/**
 * SharedActorField is a class which is used to allow introducing additional fields into the
 * lexical scope of every actor. They are passed at startup to the virtual machine and every
 * actor will initialize them during the processing of its event_init. Note that actors should 
 * never share objects, which is why this class does not store a value but rather has an abstract
 * initialize method in which a new object can be created.  
 *
 * @author smostinc
 */
public abstract class SharedActorField {
	
	private final ATSymbol name_;
	
	public SharedActorField(ATSymbol name) {
		name_ = name;
	}
	
	public ATSymbol getName() {
		return name_;
	}
	
	/**
	 * Hook to be overridden in order to initialize the field which should be installed in every
	 * actor's lexical root object. The method is executed by the thread of the newly created 
	 * actor, which can thus be accessed using ELActor.currentActor().
	 * 
	 * NOTE: if the return value is null, the field will not be added to the lexical root.
	 * 
	 * @return the object to be assigned to the field with the specified name in the current actor's lexical root
	 */
	public abstract ATObject initialize() throws InterpreterException; 

}
