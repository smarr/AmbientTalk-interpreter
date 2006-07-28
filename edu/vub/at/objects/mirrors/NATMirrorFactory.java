/**
 * AmbientTalk/2 Project
 * NATMirrorFactory.java created on Jul 13, 2006 at 6:57:39 PM
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
import edu.vub.at.exceptions.XReflectionFailure;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


/**
 * @author smostinc
 *
 * A MirrorInvocationHandler will deify all 'invoke'-calls it receives by deifing them 
 * and subsequently invoking the deification on its principal. This principal object 
 * is a java-level representation for an object which was reified at the ambienttalk 
 * level.
 * 
 * Put briefly, a mirror is a placeholder-representation for an java-level 
 * representation object which was reified as a mirror object in ambienttalk.
 */
class MirrorInvocationHandler implements InvocationHandler {
	
	/**
	 * A MirrorInvocationHandler forwards a deified version of 'invoke'-calls to its 
	 * principal which is a java-level representation of an ambienttalk object. 
	 */
	private ATObject objectRepresentation_;
	
	/**
	 * Constructs a new invocation handler forwarding to a (reified) java representation.
	 * @param objectRepresentation - java-level representation of an ambienttalk object.
	 */
	public MirrorInvocationHandler(ATObject objectRepresentation) {
		objectRepresentation_ = objectRepresentation;	
	}
	
	/**
	 * Deify and forward 'invoke'-calls to the denoted principal.
	 * @param proxy - the dynamic proxy created from this invocation handler
	 * @param method - the method as invoked by the interpreter
	 * @param args - a table of arguments
	 * 
	 * TODO We need to consider what the effects of other methods on this object are:
	 * slot addition etc. can be refused, but what about cloning and parameter passing?
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws NATException {
		String selector = method.getName();
		if(selector == "meta_invoke") {
			ATSymbol methodName = ((ATObject)args[0]).asSymbol();
			ATTable arguments = ((ATObject)args[1]).asTable();
			
			return BaseInterfaceAdaptor.deifyInvocation(
					objectRepresentation_.getClass(), 
					objectRepresentation_,
					methodName,
					arguments);
			} else {
				// TODO We can send the method to a NATMirror (or this?) to defer implementation.
				// method.invoke(mirrorImplementation_, args);
				throw new XReflectionFailure("Attempted to invoke a method directly on a mirror");
		}
	}


}

/**
 * @author smostinc
 *
 * The NATMirrorFactory allows reifying an java-level implementation object which 
 * represents an ambienttalk object to be reified to the ambienttalk level. This 
 * implies that a new representation needs to be created which will stand in for the
 * shell surrounding the java object at the ambienttalk level.
 */
public class NATMirrorFactory {
	
	public static ATObject createMirror(ATObject objectRepresentation) {
		return NATMirrorFactory.createMirrorForInterface(objectRepresentation, ATObject.class);
	}

	public static ATObject createMirrorForInterface(
			ATObject objectRepresentation, Class anATInterface) {
		return (ATObject)Proxy.newProxyInstance(
				ClassLoader.getSystemClassLoader(), 
				new Class[] { anATInterface }, 
				new MirrorInvocationHandler(
						objectRepresentation));
	}
	
}
