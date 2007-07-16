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
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.mirrors.Reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * An instance of this class is raised whenever a symbiotic method invocation fails due to overloading
 * which could not be resolved given the actual arguments. Dedicated constructors are provided to
 * deal with:
 * 
 * <ul>
 * 	<li>the case when no specific match could be identified,</li>
 * 	<li>the case where multiple matches are possible based on the actual arguments,</li>
 *  <li>the case where the overloaded method is a constructor call.</li>
 * </ul>
 * 
 * Note that the symbiosis support only evaluates whether the actual arguments can match with
 * the types specified in the method signature. If multiple possibilities remain, the closest
 * possible match is not sought for, instead this exception is raised.
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
	public XSymbiosisFailure(Object symbiont, Method method, ATObject[] atArgs) throws InterpreterException {
		StringBuffer buff = new StringBuffer("Overloaded Java invocation has no matches:\n");
		if (symbiont == null) {
			symbiont = method.getDeclaringClass().getName();
		}
		buff.append(symbiont + "." + Reflection.downSelector(method.getName()) + Evaluator.printElements(atArgs, "(",",",")").javaValue);
		message_ = buff.toString();
	}
	
	/**
	 * Reports that an overloaded method could not be resolved to a unique implementation
	 * because there are no matches on arity.
	 * @param symbiont the Java object upon which the overloaded symbiotic invocation failed.
	 * @param selector the name of the invoked overloaded method
	 * @param numArgs the number of actual arguments to the overloaded invocation
	 */
	public XSymbiosisFailure(Method method, int numArgs) throws InterpreterException {
		message_ = "Wrong number of arguments supplied for "
			+ method.getName() + ", given: " + numArgs;
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
	
	public ATTypeTag getType() {
		return NativeTypeTags._SYMBIOSISFAILURE_;
	}

}
