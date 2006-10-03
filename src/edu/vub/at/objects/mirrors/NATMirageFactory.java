/**
 * AmbientTalk/2 Project
 * NATMirageFactory.java created on Jul 13, 2006 at 6:11:22 PM
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

import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.ATAnything;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author smostinc
 *
 * A MirageInvocationHandler will translate all calls it receives by reifing them and
 * subsequently invoking the reification on its principal. This principal object is a
 * java-level representation for a mirror object implemented at the ambienttalk level.
 * 
 * Put briefly, a mirage is a placeholder-representation for an ambienttalk object whose
 * semantics are defined by a dedicated mirror object written in ambienttalk.
 */
class MirageInvocationHandler implements InvocationHandler {

	/**
	 * An ATMirage will forward a reified version of all invocations to another
	 * object, which represents a custom mirror at the ambienttalk level.
	 */
	private ATObject mirrorRepresentation_;
	
	/**
	 * Constructs a new invocation handler forwarding to an ambienttalk-level mirror.
	 * @param mirrorRepresentation - 
	 */
	public MirageInvocationHandler(ATObject mirrorRepresentation) {
		mirrorRepresentation_ = mirrorRepresentation;
	}
	
	/**
	 * Reify and forward the received method to the denoted principal.
	 * 
	 * mirage.meta_xxx(args) should be transformed into mirrorrepr_.meta_invoke(xxx, [args])
	 * mirage.meta_getxxx() should be transformed into mirrorrepr_.meta_select(xxx)
	 * mirage.meta_setxxx(arg) should be transformed into mirrorrepr_.meta_assignField(xxx, arg)
	 * mirage.base_xxx(args) should be transformed into mirrorrepr_.meta_invoke(invoke, [xxx, [args]])
	 * mirage.base_getxxx() should be transformed into mirrorrepr_.meta_invoke(select, [xxx])
	 * mirage.base_setxxx(arg) should be transformed into mirrorrepr_.meta_invoke(assignField, [xxx, arg])
	 * 
	 * @param proxy - the dynamic proxy created from this invocation handler
	 * @param method - the method as invoked by the interpreter
	 * @param args - a table of arguments
	 */
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		
		String name = method.getName();
		
		if(name.startsWith("is")) {
			return NATBoolean._TRUE_;
		} else if(name.startsWith("as")) {
			return proxy;
		}
		
		if (name.startsWith(JavaInterfaceAdaptor._META_PREFIX_)) {
			if(name.startsWith(JavaInterfaceAdaptor._MGET_PREFIX_)) {
				return mirrorRepresentation_.meta_select(
						mirrorRepresentation_,
						AGSymbol.alloc(name.replaceFirst(JavaInterfaceAdaptor._MGET_PREFIX_, "")));
			} 
			
			else if(name.startsWith(JavaInterfaceAdaptor._MSET_PREFIX_)) {
				return mirrorRepresentation_.meta_assignField(
						AGSymbol.alloc(name.replaceFirst(JavaInterfaceAdaptor._MSET_PREFIX_, "")),
						(ATObject)args[0]);
			} 
			
			else /* if(name.startsWith("meta_")) */ {
				return mirrorRepresentation_.meta_invoke(
						mirrorRepresentation_,
						AGSymbol.alloc(name.replaceFirst(JavaInterfaceAdaptor._META_PREFIX_, "")),
						new NATTable(/*(ATObject[])*/args));
			}
		} else if (name.startsWith(JavaInterfaceAdaptor._BASE_PREFIX_)) {
			if(name.startsWith(JavaInterfaceAdaptor._BGET_PREFIX_)) {
				return mirrorRepresentation_.meta_invoke(
						mirrorRepresentation_,
						AGSymbol.alloc("select"),
						new NATTable(new ATObject[] {
								AGSymbol.alloc(name.replaceFirst(JavaInterfaceAdaptor._BGET_PREFIX_, "")),
								new NATTable((ATObject[])args)}));
			} 
			
			else if(name.startsWith(JavaInterfaceAdaptor._BSET_PREFIX_)) {
				return mirrorRepresentation_.meta_invoke(
						mirrorRepresentation_,
						AGSymbol.alloc("assignField"),
						new NATTable(new ATObject[] {
								AGSymbol.alloc(name.replaceFirst(JavaInterfaceAdaptor._BSET_PREFIX_, "")),
								new NATTable((ATObject[])args)}));
			} 
			
			else /* if(name.startsWith("base_")) */ {
				return mirrorRepresentation_.meta_invoke(
						mirrorRepresentation_,
						AGSymbol.alloc("invoke"),
						new NATTable(new ATObject[] {
								AGSymbol.alloc(name.replaceFirst(JavaInterfaceAdaptor._BASE_PREFIX_, "")),
								new NATTable((ATObject[])args)}));
			}			
		}
		
		ATSymbol selector = Reflection.downMetaLevelSelector(method.getName());
		ATTable arguments = new NATTable(args);
		return mirrorRepresentation_.meta_invoke(mirrorRepresentation_, selector, arguments);
	}

	
}


/**
 * @author smostinc
 *
 * The factory class NATMirageFactory is able to synthetise a mirage object for
 * every interface in the ambienttalk interface hierarchy. Mirage objects are to be 
 * used when objects (or any other type of language value for that matter) are created
 * with a custom mirror object. In this case the implementation does not host a direct 
 * representation of the object, but rather allows the mirror on the ambienttalk-level
 * to handle how the object should respond. To this end, the object is embodied through
 * a mirage, an object which is but a shadow of the mirror at the ambienttalk-level.
 * 
 * Mirages, as defined by the MirageInvocationHandler will simply reify every call they
 * receive and pass it on to their principal (the java representation of the mirror). 
 */
public class NATMirageFactory {

	// TODO: mirrorRepresentation.getClass() returns the NATxxx class instance, require mapping from NATxxx to ATxxx
	public static ATObject createMirage(ATObject mirrorRepresentation) {
		return NATMirageFactory.createMirageForInterface(mirrorRepresentation, ATAnything.class);
	}

	public static ATObject createMirageForInterface(
			ATObject mirrorRepresentation, Class anATInterface) {
		return (ATObject)Proxy.newProxyInstance(
				ClassLoader.getSystemClassLoader(), 
				new Class[] { anATInterface }, 
				new MirageInvocationHandler(
						mirrorRepresentation));
	}
}

