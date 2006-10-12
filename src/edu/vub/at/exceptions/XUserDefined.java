/**
 * AmbientTalk/2 Project
 * XUserDefined.java created on Oct 10, 2006 at 9:31:40 PM
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

import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.mirrors.JavaClosure;
import edu.vub.at.objects.natives.NATTable;

/**
 * Instances of the XUserDefined act as wrappers for ATObjects which are thrown at the
 * ambienttalk level using the raise: primitive. Since we reuse the Java exception 
 * propagation, we need to wrap these in a custom class which extends the proper class.
 *
 * @author smostinc
 */
public class XUserDefined extends NATException {

	private static final long serialVersionUID = -2859841280138142649L;

	public ATObject customException_;
	
	public XUserDefined(ATObject customException) {
		super("A custom exception object was thrown");
		customException_ = customException;
	}

	public ATObject getAmbientTalkRepresentation() {
		return customException_;
	}

	public ATObject mayBeSignalledFrom(final ATClosure tryBlock, final ATClosure replacementCode) throws NATException {
		try {
			return tryBlock.base_apply(NATTable.EMPTY);
		} catch (final XUserDefined e) {
			return e.getAmbientTalkRepresentation().meta_isRelatedTo(customException_).
				base_ifTrue_ifFalse_(
						new JavaClosure(customException_) {
							public ATObject base_apply(ATTable args) throws NATException {
								return replacementCode.base_apply(new NATTable(new ATObject[] { e.getAmbientTalkRepresentation() }));
							}
						},
						new JavaClosure(customException_) {
							public ATObject base_apply(ATTable args) throws NATException {
								throw e;
							}
						}); 
		}
	};
	
	
}
