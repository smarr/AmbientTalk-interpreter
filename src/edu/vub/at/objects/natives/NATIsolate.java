/**
 * AmbientTalk/2 Project
 * NATIsolate.java created on 29-dec-2006 at 16:19:29
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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATObject;

import java.util.LinkedList;
import java.util.Vector;

/**
 * An isolate is an AmbientTalk/2 object that may be passed by copy when passed
 * between actors.
 *
 * @author tvcutsem
 */
public class NATIsolate extends NATObject {

	public NATIsolate() {
		super();
	}
	
	public NATIsolate(ATObject dynamicParent, ATObject lexicalParent, boolean parentType) {
		super(dynamicParent, lexicalParent, parentType);
	}

	public NATIsolate(ATObject dynamicParent, boolean parentType) {
		super(dynamicParent, parentType);
	}

	public NATIsolate(ATObject lexicalParent) {
		super(lexicalParent);
	}

	// for cloning purposes only
	protected NATIsolate(FieldMap map,
	         Vector state,
	         LinkedList originalCustomFields,
	         MethodDictionary methodDict,
	         ATObject dynamicParent,
	         ATObject lexicalParent,
	         byte flags) throws InterpreterException {
		super(map, state, originalCustomFields, methodDict, dynamicParent, lexicalParent, flags);
	}
	
	// NATIsolate has to implement the NATByCopy implementation by hand
	// because NATObject inherits from NATByRef, and because Java has no
	// multiple inheritance to override that implementation.
	
    /**
     * An isolate object does not return a proxy representation of itself
     * during serialization, hence it is serialized itself.
     */
    public ATObject meta_pass() throws InterpreterException {
    	return this;
    }
	
    /**
     * An isolate object represents itself upon deserialization.
     */
    public ATObject meta_resolve() throws InterpreterException {
    	return this;
    }
    
	protected NATObject createClone(FieldMap map,
			  Vector state,
			  LinkedList customFields,
			  MethodDictionary methodDict,
			  ATObject dynamicParent,
			  ATObject lexicalParent,
			  byte flags) throws InterpreterException {
        return new NATIsolate(map,
  		  				    state,
  		  				    customFields,
  		  				    methodDict,
  		  				    dynamicParent,
  		  				    lexicalParent,
  		  				    flags);
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<isolate:"+this.hashCode()+">");
	}
	
	/**
	 * An extension of an isolate is itself an isolate
	 */
	protected ATObject createChild(ATClosure code, boolean parentPointerType) throws InterpreterException {
		NATIsolate extension = new NATIsolate(
				/* dynamic parent */
				this,
				/* lexical parent */
				code.base_getContext().base_getLexicalScope(),
				/* parent porinter type */
				parentPointerType);
			
		code.base_applyInScope(NATTable.EMPTY, extension);
		return extension;
	}
	
}
