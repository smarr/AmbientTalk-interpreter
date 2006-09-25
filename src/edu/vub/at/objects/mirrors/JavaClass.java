/**
 * AmbientTalk/2 Project
 * JavaClass.java created on Jul 27, 2006 at 1:59:58 AM
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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATText;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

/**
 * @author smostinc
 *
 * JavaClass wraps a Java class under the guise of a table of JavaMethods.
 * @deprecated is this class ever used?
 */
public class JavaClass extends NATNil implements ATTable {

	private final Class c_;
	
	public JavaClass(Class c) {
		this.c_ = c;
	}

	public NATText meta_print() throws XTypeMismatch {
		return NATText.atValue("<javaclass>");
	}
	
	// base-level interface
	
	public ATNumber base_getLength() {
		return NATNumber.atValue(c_.getMethods().length);
	}

	public ATObject base_at(ATNumber index) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.vub.at.objects.ATTable#atPut(edu.vub.at.objects.ATNumber, edu.vub.at.objects.ATObject)
	 */
	public ATObject base_atPut(ATNumber index, ATObject value) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/* (non-Javadoc)
	 * @see edu.vub.at.objects.ATTable#base_collect_(edu.vub.at.objects.ATClosure)
	 */
	public ATObject base_collect_(ATClosure clo) throws NATException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.vub.at.objects.ATTable#base_each_(edu.vub.at.objects.ATClosure)
	 */
	public ATNil base_each_(ATClosure clo) throws NATException {
		// TODO Auto-generated method stub
		return null;
	}

	public ATBoolean base_isEmpty() {
		return NATBoolean.atValue(c_.getMethods().length == 0);
	}

	public NATTable asNativeTable() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.vub.at.objects.ATTable#base_implode()
	 */
	public ATText base_implode() throws NATException {
		// TODO Auto-generated method stub
		return null;
	}

	public ATText base_join(ATText txt) throws NATException {
		// TODO Auto-generated method stub
		return null;
	}
	
	/* (non-Javadoc)
	 * @see edu.vub.at.objects.ATTable#base_map_(edu.vub.at.objects.ATClosure)
	 */
	public ATObject base_map_(ATClosure clo) throws NATException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.vub.at.objects.ATTable#base_select(edu.vub.at.objects.ATNumber, edu.vub.at.objects.ATNumber)
	 */
	public ATTable base_select(ATNumber start, ATNumber stop) throws NATException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.vub.at.objects.ATTable#base_with_collect_(edu.vub.at.objects.ATObject, edu.vub.at.objects.ATClosure)
	 */
	public ATObject base_with_collect_(ATObject init, ATClosure clo) throws NATException {
		// TODO Auto-generated method stub
		return null;
	}

}
