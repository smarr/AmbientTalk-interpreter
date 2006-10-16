/**
 * AmbientTalk/2 Project
 * NATException.java created on Oct 13, 2006 at 1:46:47 PM
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

import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATNil;

/**
 * Instances of the class NATException provide a AmbientTalk representation for the 
 * default instances of all InterpreterExceptions. They allow catching exceptions 
 * thrown by the interpreter in AmbientTalk, as well as raising them for within
 * custom code. 
 *
 * @author smostinc
 */
public class NATException extends NATNil {
	
	private final InterpreterException wrappedException_;
	
	public NATException(InterpreterException wrappedException) {
		wrappedException_ = wrappedException;
	}

	public InterpreterException asInterpreterException() throws XTypeMismatch {
		return wrappedException_;
	}
	
    public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
        return Reflection.downObject(Reflection.upExceptionCreation(wrappedException_, initargs));
    }

	public ATBoolean meta_isRelatedTo(ATObject object) throws InterpreterException {
		if(object instanceof NATException) {
			return NATBoolean.atValue(
					wrappedException_.getClass().isAssignableFrom(
					object.asInterpreterException().getClass())); 
		} else {
			return NATBoolean._FALSE_;
		}
	}
    
    

}
