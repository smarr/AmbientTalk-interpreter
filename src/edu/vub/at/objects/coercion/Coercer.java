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

import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.symbiosis.Symbiosis;
import edu.vub.at.objects.symbiosis.SymbioticATObjectMarker;

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
	
	private final NATObject principal_;
	
	private Coercer(NATObject principal) {
		principal_ = principal;
	}
	
	public static final Object coerce(ATObject object, Class type) throws XTypeMismatch {
		if (type.isInstance(object)) { // object instanceof type
			return object; // no need to coerce
		} else if (object.isAmbientTalkObject() && type.isInterface()) {
			// note that the proxy implements both the required type
			// and the Symbiotic object marker interface to identify it as a wrapper
			return Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(),
					                      new Class[] { type, SymbioticATObjectMarker.class },
					                      new Coercer(object.asAmbientTalkObject()));
		} else {
			throw new XTypeMismatch(type, object);
		}
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
		// intercept access to the wrapped object for Java->AT value conversion
		} else if (method.getDeclaringClass() == SymbioticATObjectMarker.class) {
			return principal_;
		} else {
			ATObject[] symbioticArgs;
             // support for variable-arity invocations from within AmbientTalk
			if ((arguments != null) && (arguments.length == 1) && (arguments[0] instanceof ATObject[])) {
				// no need to convert arguments
				symbioticArgs = (ATObject[]) arguments[0];
			} else {
				symbioticArgs = new ATObject[(arguments == null) ? 0 : arguments.length];
				for (int i = 0; i < symbioticArgs.length; i++) {
					symbioticArgs[i] = Symbiosis.javaToAmbientTalk(arguments[i]);
				}
			}

			ATObject result = Reflection.downInvocation(principal_, method, symbioticArgs);
			// properly 'cast' the returned object into the appropriate interface
			return Symbiosis.ambientTalkToJava(result, method.getReturnType());
		}
	}
	
}
