/**
 * AmbientTalk/2 Project
 * XSymbiosisFailure.java created on 13-nov-2006 at 14:35:22
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
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.coercion.NativeStripes;
import edu.vub.at.objects.mirrors.Reflection;

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * An instance of this class is raised whenever a symbiotic method invocation fails due to overloading
 * which could not be resolved given the actual arguments.
 * 
 * @author tvcutsem
 */
public class XSymbiosisFailure extends InterpreterException {

	private static final long serialVersionUID = -4161446826939837849L;

	private final String message_;
	
	/**
	 * Reports that an overloaded method could not be resolved to a unique implementation
	 * because there are multiple matches.
	 * @param symbiont the Java object upon which the overloaded symbiotic invocation failed.
	 * @param selector the name of the invoked overloaded method
	 * @param choices a linked list of all applicable java.lang.Method objects
	 * @param atArgs the actual arguments to the overloaded invocation
	 */
	public XSymbiosisFailure(Object symbiont, String selector, LinkedList choices, ATObject[] atArgs) throws InterpreterException {
		StringBuffer buff = new StringBuffer("Overloaded Java invocation has " + choices.size() + " matches:\n");
		buff.append(symbiont.toString() + "." + Reflection.downSelector(selector) + Evaluator.printElements(atArgs, "(",",",")").javaValue);
		for (Iterator iter = choices.iterator(); iter.hasNext();) {
			buff.append("\n" + iter.next().toString());
		}
		message_ = buff.toString();
	}
	
	/**
	 * Reports that an overloaded method could not be resolved to a unique implementation
	 * because there are no matches for any static types.
	 * @param symbiont the Java object upon which the overloaded symbiotic invocation failed.
	 * @param selector the name of the invoked overloaded method
	 * @param atArgs the actual arguments to the overloaded invocation
	 */
	public XSymbiosisFailure(Object symbiont, String selector, ATObject[] atArgs) throws InterpreterException {
		StringBuffer buff = new StringBuffer("Overloaded Java invocation has no matches:\n");
		buff.append(symbiont + "." + Reflection.downSelector(selector) + Evaluator.printElements(atArgs, "(",",",")").javaValue);
		message_ = buff.toString();
	}
	
	/**
	 * Reports that an overloaded constructor could not be resolved to a unique implementation.
	 * @param failedClass the Java class whose constructor could not be resolved.
	 * @param choices all applicable constructors (may contain null values, corresponding to non-applicable choices)
	 * @param numMatchingCtors the number of matching constructors (the number of non-null values in choices)
	 */
	public XSymbiosisFailure(Class failedClass, Constructor[] choices, ATObject[] atArgs, int numMatchingCtors) throws InterpreterException {
		StringBuffer buff = new StringBuffer("Overloaded Java constructor has " + numMatchingCtors + " matches:\n");
		buff.append(Evaluator.getSimpleName(failedClass) + ".new"+ Evaluator.printElements(atArgs, "(",",",")").javaValue);
		for (int i = 0; i < choices.length; i++) {
			if (choices[i] != null) {
				buff.append("\n" + choices[i].toString());
			}
		}
		message_ = buff.toString();
	}
	
	public String getMessage() {
		return message_;
	}
	
	public ATStripe getStripeType() {
		return NativeStripes._SYMBIOSISFAILURE_;
	}

}
