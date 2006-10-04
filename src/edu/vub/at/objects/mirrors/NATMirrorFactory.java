/**
 * AmbientTalk/2 Project
 * NATMirrorFactory.java created on Jul 13, 2006 at 6:57:39 PM
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
package edu.vub.at.objects.mirrors;

import edu.vub.at.objects.ATMirror;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.natives.NATNil;

/**
 * The NATMirrorFactory allows reifying an java-level implementation object which 
 * represents an ambienttalk object to be reified to the ambienttalk level. This 
 * implies that a new representation needs to be created which will stand in for the
 * shell surrounding the java object at the ambienttalk level.
 * 
 * @author smostinc
 */
public class NATMirrorFactory extends NATNil {
	
	public static final NATMirrorFactory _INSTANCE_ = new NATMirrorFactory();
	
	public ATMirror base_createMirror(ATObject objectRepresentation) {
		if(objectRepresentation instanceof NATMirage) {
			return ((NATMirage)objectRepresentation).getMirror();
		};
		return new NATIntrospectiveMirror(objectRepresentation);
	}
	
}
