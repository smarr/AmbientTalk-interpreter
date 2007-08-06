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

import edu.vub.at.actors.eventloops.EventLoop;
import edu.vub.at.actors.eventloops.EventLoop.EventProcessor;
import edu.vub.at.actors.natives.ELActor;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.symbiosis.Symbiosis;

import java.io.IOException;
import java.io.Serializable;
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
 * ATExpression base_expression() throws NATException
 * 
 * receives the following implementation:
 * 
 * ATExpression expression() throws NATException {
 *	return principal.meta_invoke(principal, Reflection.downSelector("getExpression"), NATTable.EMPTY).asExpression();
 * }
 * 
 * where principal is the original object 'coerced into' the given interface
 * 
 * @author tvcutsem
 */
public final class Coercer implements InvocationHandler, Serializable {
	
	private final ATObject principal_;
	
	// we have to remember which thread owned the principal
	private transient Thread wrappingThread_;
	
	private Coercer(ATObject principal, Thread owningThread) {
		principal_ = principal;
		wrappingThread_ = owningThread;
	}
	
	public String toString() {
		return "<coercer on: "+principal_+">";
	}
	
	/**
	 * Try to coerce the given AmbientTalk object into the given Java type. This variant implicitly assumes that
	 * the coercion is performed by the thread owning the object, which is the case when coercing arguments to a
	 * Java method call or when passing an AmbientTalk object as a result.
	 * 
	 * Note that the returned coercer object is also an instance of {@link ATObject} and
	 * in the AmbientTalk world the coercer will behave as its coerced AT object.
	 * 
	 * @param object the AmbientTalk object to coerce
	 * @param type the class object representing the target type
	 * @return a Java object <tt>o</tt> for which it holds that <tt>type.isInstance(o)</tt>
	 * @throws XTypeMismatch if the coercion fails
	 */
	public static final Object coerce(ATObject object, Class type) throws XTypeMismatch {
		return coerce(object, type, Thread.currentThread());
	}

	/**
	 * Try to coerce the given AmbientTalk object into the given Java type, while explicitly providing the thread
	 * which is the owning actor for the object. 
	 * <p>
	 * This variant of coerce is provided explicitly to allow coercion to occur from a Java thread which is not the 
	 * owning actor of the object. This occurs when the coercion is performed explicitly on the return value of an 
	 * evaluation, after the latter has been finalized. 
	 *  
	 * @param object the AmbientTalk object to coerce
	 * @param type the class object representing the target type
	 * @param owningThread the owning Actor
	 * @return a Java object <tt>o</tt> for which it holds that <tt>type.isInstance(o)</tt>
	 * @throws XTypeMismatch if the coercion fails
	 */
	public static final Object coerce(ATObject object, Class type, Thread owningThread) throws XTypeMismatch {
		if (type.isInstance(object)) { // object instanceof type
			return object; // no need to coerce
		} else if (type.isInterface()) {
			// note that the proxy implements both the required type
			// and the Symbiotic object marker interface to identify it as a wrapper
			return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
					                      new Class[] { type, ATObject.class },
					                      new Coercer(object, owningThread));
		} else {
			throw new XTypeMismatch(type, object);
		}
	}
	
	public Object invoke(Object receiver, final Method method, Object[] arguments) throws Throwable {
		Class methodImplementor = method.getDeclaringClass();
		// handle toString, hashCode and equals in a dedicated fashion
		// similarly, handle any native AT methods by simply forwarding them to the native AT object
		if (methodImplementor == Object.class || methodImplementor == ATObject.class) {
			// invoke these methods on the principal rather than on the proxy
			try {
				return method.invoke(principal_, arguments);
			} catch (InvocationTargetException e) {
				throw e.getTargetException();
			}
		} else {
			
			final ATObject[] symbioticArgs;
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
			
			// if the current thread is not an actor thread, treat the Java invocation
			// as a message send instead and enqueue it in my actor's thread
			
			if (Thread.currentThread() != wrappingThread_) {
				if (Thread.currentThread() instanceof EventProcessor) {
					// another event loop has direct access to this object, this means
					// an AT object has been shared between actors via Java, signal an error
					throw new XIllegalOperation("Detected illegal invocation: sharing via Java level of object " + principal_);
				}
				
				ELActor owningActor = (ELActor) EventLoop.toEventLoop(wrappingThread_);
				
				// if the invoked method is part of an EventListener interface, treat the
				// invocation as a pure asynchronous message send, if the returntype is void
				if (Symbiosis.isEvent(method)) {
					owningActor.event_symbioticInvocation(principal_, method, symbioticArgs);
					return null; // void return type
				} else {
					// because a message send is asynchronous and Java threads work synchronously,
					// we'll have to make the Java thread wait for the result
					return owningActor.sync_event_symbioticInvocation(principal_, method, symbioticArgs);
				}
			} else {
				// perform a synchronous invocation
				ATObject result = Reflection.downInvocation(principal_, method, symbioticArgs);
				// properly 'cast' the returned object into the appropriate interface
				return Symbiosis.ambientTalkToJava(result, method.getReturnType());		
			}
		}
	}
	
	/**
	 * Upon deserialization, re-assign the thread to the actor deserializing this coercer
	 */
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		wrappingThread_ = Thread.currentThread();
	}
		
}
