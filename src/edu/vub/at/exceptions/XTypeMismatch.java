/**
 * AmbientTalk/2 Project
 * XTypeMismatch.java created on Jul 13, 2006 at 9:43:54 PM
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

import edu.vub.at.eval.Evaluator;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.coercion.NativeTypeTags;

/**
 * XTypeMismatch instances are thrown when a value conversion failed. Value conversions occur 
 * when the interpreter expects arguments of a particular interface type (e.g. an ATClosure to
 * apply) and the passed argument has another type. The following cases can be distinguished: 
 * <ul>
 *   <li> The argument is of a different native ambienttalk type and therefore cannot be used. 
 *   In this case, an XTypeMismatch exception will be thrown.</li>
 *   <li> The argument is an ambienttalk object. At this point, the coercer will attempt to 
 *   coerce the object into the correct type, which is possible only if the object has the 
 *   correct native type. Otherwise, an XTypeMismatch exception will be thrown.</li>
 *   <li>The target type is a Java interface. The conversion automatically succeeds if the passed
 *   argument is a JavaObject wrapper for an object which implements the interface. If the passed
 *   argument is an ambienttalk object, the coercer will be able to dynamically synthetise a 
 *   proxy implementing the interface. If the argument is of a native ambienttalk type however, 
 *   an XTypeMismatch exception will be thrown.</li>
 *   <li>The target type is a Java class. If the passed argument is not a JavaObject wrapper for
 *   an instance of this class (or one of its subclasses), an XTypeMismatch exception will be 
 *   thrown.</li>
 * </ul>
 * 
 * @author smostinc
 * @author tvcutsem
 */
public class XTypeMismatch extends InterpreterException {

	private static final long serialVersionUID = -3135452124227872807L;
	
	private final ATObject failedObject_;
    private final Class expectedType_;

    /**
     * Creates a new type mismatch exception given the expected type and the object that could not be coerced to this type.
     * @param expectedType a Java Class denoting the type the object was being coerced into.
     * @param failedObject the ambienttalk value that could not be coerced into the correct type
     */
	public XTypeMismatch(Class expectedType, ATObject failedObject) {
		expectedType_ = expectedType;
		failedObject_ = failedObject;
	}

	/**
	 * @return the ambienttalk value that could not be coerced into the correct type.
	 */
	public ATObject getFailedObject() {
		return failedObject_;
	}
	
	/**
	 * @return a Java Class denoting the type the object was being coerced into.
	 */
	public Class getExpectedType() {
		return expectedType_;
	}
	
	/**
	 * @return a textual description of the type mismatch exception.
	 */
	public String getMessage() {
		String obj = Evaluator.toString(failedObject_);
		return "Type mismatch: expected " + Evaluator.valueNameOf(expectedType_)
		           + ", given " + obj + " (type: " + Evaluator.valueNameOf(failedObject_.getClass())+ ")";
	}
	
	public ATTypeTag getType() {
		return NativeTypeTags._TYPEMISMATCH_;
	}

}
