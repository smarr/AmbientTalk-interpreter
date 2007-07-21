/**
 * AmbientTalk/2 Project
 * SymbioticATObjectMarker.java created on 6-nov-2006 at 10:49:45
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
package edu.vub.at.objects.symbiosis;

import edu.vub.at.objects.natives.NATObject;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * This empty marker interface marks Java objects which actually represent wrappers around AmbientTalk
 * objects that 'pretend' to be a Java object of a certain interface type. It is used to properly
 * convert such objects back into their AmbientTalk equivalent when they re-enter the AmbientTalk meta- or base-level.
 * 
 * @author tvcutsem
 */
public interface SymbioticATObjectMarker extends Serializable {

	/**
	 * Accessor to unwrap the symbiotic object.
	 */
	public NATObject _returnNativeAmbientTalkObject();
	
	/**
	 * Coercers implement writeReplace such that their principal is serialized as them instead.
	 */
	//public Object writeReplace() throws ObjectStreamException;
	
}
