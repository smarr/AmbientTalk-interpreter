/**
 * AmbientTalk/2 Project
 * Coercer.java created on 3-okt-2006 at 16:12:05
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
package edu.vub.at.objects.coercion;

import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.mirrors.Reflection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * A coercer is a dynamic proxy which is used to 'cast' Ambienttalk base-level NATObjects to a certain ATxxx interface.
 * The dynamic proxy is responsible for transforming java calls to meta_invoke calls.
 * 
 * For example, a method from an AT interface
 * 
 * ATExpression base_getExpression() throws NATException
 * 
 * receives the following implementation:
 * 
 * ATExpression getExpression() throws NATException {
 *	return principal.meta_invoke(principal, Reflection.downSelector("getExpression"), NATTable.EMPTY).asExpression();
 * }
 * 
 * where principal is the original object 'coerced into' the given interface
 * 
 * @author tvc
 */
public final class Coercer implements InvocationHandler {
	
	private final ATObject principal_;
	
	private Coercer(ATObject principal) {
		principal_ = principal;
	}
	
	public static final Object coerce(ATObject object, Class type) {
		return Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, new Coercer(object));
	}

	public Object invoke(Object receiver, Method method, Object[] arguments) throws Throwable {
		// handle toString, hashCode and equals in a dedicated fashion
		if (method.getDeclaringClass() == Object.class) {
			// invoke these methods on the principal rather than on the proxy
			try {
				return method.invoke(principal_, arguments);
			} catch (InvocationTargetException e) {
				throw e.getTargetException();
			}
		} else {
			return Reflection.downInvocation(principal_, method.getName(), arguments);
		}
	}
	
}
