/**
 * AmbientTalk/2 Project
 * ATJavaMethod.java created on 14-nov-2006 at 13:17:33
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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;

/**
 * The public interface to a symbiotic Java method.
 * 
 * @author tvcutsem
 */
public interface ATJavaMethod extends ATMethod {

	/**
	 * Using this method, AmbientTalk symbiotic code is able to perform manual overloaded method disambiguation.
	 * When evaluating <tt>javaObject.methodName</tt>, the result is a method wrapper for all of the overloaded methods
	 * whose name corresponds to <tt>methodName</tt>. Sometimes, the symbiosis layer is not able to disambiguate methods
	 * simply by means of the types of the actual arguments. In that case, this 'cast' method can be used to manually
	 * disambiguate methods as follows: <tt>jObject.methodName.cast(JClass1, JClass2)(arg1, arg2)</tt>.
	 * 
	 * <tt>jObject.methodName.cast(JClass1, JClass2)</tt> evaluates to a first-class method that only
	 * contains those overloaded methods whose parameter types exactly match the ones given.
	 * 
	 * @param types an array of JavaClass objects
	 * @return a new JavaMethod where the wrapped overloaded methods correspond to the given types
	 * @throws XSymbiosisFailure when no wrapped methods correspond to the given types
	 */
	public JavaMethod base_cast(ATObject[] types) throws InterpreterException;
	
}
