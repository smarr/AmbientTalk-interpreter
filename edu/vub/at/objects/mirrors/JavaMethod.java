/**
 * AmbientTalk/2 Project
 * JavaMethod.java created on Jul 27, 2006 at 1:35:19 AM
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
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

import java.lang.reflect.Method;

/**
 * @author smostinc
 *
 * A JavaMethod is a wrapper around Java methods allowing them to be selected 
 * from base-level objects and passed around as ordinary methods.
 */
public final class JavaMethod extends NATNil implements ATMethod {

	private final Method javaMethod_;
	
	public JavaMethod(Method javaMethod) {
		javaMethod_ = javaMethod;
	}

	/**
	 * The name of a wrapped Java method is the name of the Java method, converted to an
	 * AmbientTalk selector name.
	 */
	public ATSymbol getName() {
		return Reflection.downSelector(javaMethod_.getName());
	}
	
	public ATTable getArguments() {
		return new NATTable(javaMethod_.getParameterTypes());
	}

	public ATAbstractGrammar getBody() {
		return NATText.atValue("Native implementation of " + javaMethod_.toString());
	}
	
	public ATObject meta_apply(ATTable arguments, ATContext ctx) throws NATException {
		try {
			return Reflection.downObject(
					javaMethod_.invoke(ctx.getLexicalScope(), arguments.asNativeTable().elements_));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new NATException("Invocation on a Java Method failed", e);
		}
	}

}
