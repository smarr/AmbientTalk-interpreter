/**
 * AmbientTalk/2 Project
 * Evaluator.java created on 27-sep-2006 at 15:53:39
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
package edu.vub.at.eval;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.regexp.RE;
import org.apache.regexp.REProgram;

import edu.vub.at.actors.ATFarReference;
import edu.vub.at.actors.natives.ELActor;
import edu.vub.at.actors.natives.NATAsyncMessage;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XAmbienttalk;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMirrorRoot;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.mirrors.NATMirrorRoot;
import edu.vub.at.objects.natives.NATException;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.OBJLexicalRoot;
import edu.vub.at.objects.natives.grammar.AGSplice;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.at.objects.natives.grammar.NATAbstractGrammar;
import edu.vub.at.objects.symbiosis.JavaObject;
import edu.vub.at.objects.symbiosis.JavaPackage;
import edu.vub.at.objects.symbiosis.XJavaException;
import edu.vub.at.util.logging.Logging;
import edu.vub.util.Regexp;
import edu.vub.util.TempFieldGenerator;

/**
 * The Evaluator class serves as a repository for auxiliary evaluation methods.
 * 
 * @author tvcutsem
 */
public final class Evaluator {
	
	// important symbols
	
	public static final AGSymbol _ANON_MTH_NAM_ = AGSymbol.jAlloc("nativelambda");
	public static final NATTable _ANON_MTH_ARGS_ = NATTable.of(new AGSplice(AGSymbol.jAlloc("args")));
	public static final AGSymbol _LAMBDA_    = AGSymbol.alloc(NATText.atValue("lambda"));
	public static final AGSymbol _APPLY_     = AGSymbol.alloc(NATText.atValue("apply"));
	public static final AGSymbol _INIT_      = AGSymbol.alloc(NATText.atValue("init"));
	public static final AGSymbol _CURNS_SYM_ = AGSymbol.jAlloc("~");
	
	/**
	 * A thread-local variable is used to assign a unique global scope to
	 * each separate actor. Each actor that invokes the getGlobalLexicalScope
	 * method receives its own separate copy of the global scope
	 */
	private static final ThreadLocal _GLOBAL_SCOPE_ = new ThreadLocal() {
	    protected synchronized Object initialValue() {
	        return createGlobalLexicalScope();
	    }
	};
	
	/**
	 * A thread-local variable is used to assign a unique lobby namespace to
	 * each separate actor. Each actor that invokes the getLobby()
	 * method receives its own separate copy of the lobby namespace
	 */
	private static final ThreadLocal _LOBBY_NAMESPACE_ = new ThreadLocal() {
	    protected synchronized Object initialValue() {
	        return createLobbyNamespace();
	    }
	};
	
	/**
	 * A thread-local variable is used to assign a unique jlobby root to
	 * each separate actor. The jlobby root is the root JavaPackage from
	 * which other Java packages can be loaded. Each actor that invokes the getJLobbyRoot()
	 * method receives its own separate copy of the jlobby root
	 */
	private static final ThreadLocal _JLOBBY_ROOT_ = new ThreadLocal() {
	    protected synchronized Object initialValue() {
	        return createJLobbyRoot();
	    }
	};
	
	/**
	 * A thread-local variable is used to assign a unique mirror root to
	 * each separate actor. The mirror root encapsulates the default semantics
	 * for AmbientTalk objects and is the parent of most interecessive custom mirrors
	 * defined by AmbientTalk programmers themselves.
	 */
	private static final ThreadLocal _MIRROR_ROOT_ = new ThreadLocal() {
	    protected synchronized Object initialValue() {
	        return createMirrorRoot();
	    }
	};
	
	/**
	 * A thread-local variable is used to assign a unique nil object to
	 * each separate actor. This object is the root of the delegation
	 * chain of all objects owned by that actor.
	 */
	private static final ThreadLocal _NIL_ = new ThreadLocal() {
	    protected synchronized Object initialValue() {
	        return createNil();
	    }
	};
	
	
	/**
	 * Auxiliary function used to print the elements of a table using various separators.
	 */
	public final static NATText printElements(ATObject[] els,String start, String sep, String stop) throws InterpreterException {
		if (els.length == 0)
			return NATText.atValue(String.valueOf(start+stop));
		
	    StringBuffer buff = new StringBuffer(start);
		for (int i = 0; i < els.length - 1; i++) {
			buff.append(els[i].meta_print().asNativeText().javaValue + sep);
		}
		buff.append(els[els.length-1].meta_print().asNativeText().javaValue + stop);
	    return NATText.atValue(buff.toString());
	}
	
	/**
	 * Auxiliary function used to print the elements of a table using various separators.
	 */
	public final static NATText printElements(NATTable tab,String start, String sep, String stop) throws InterpreterException {
		return printElements(tab.elements_, start, sep, stop);
	}

	public static final NATText printAsStatements(ATTable tab) throws InterpreterException {
		return printElements(tab.asNativeTable(), "", "; ", "");
	}

	public static final NATText printAsList(ATTable tab) throws InterpreterException {
		return printElements(tab.asNativeTable(), "(", ", ", ")");
	}
	
	/**
	 * Auxiliary function used to code the elements of a table using various separators.
	 */
	public final static NATText codeElements(TempFieldGenerator objectMap, ATObject[] els,String start, String sep, String stop) throws InterpreterException {
		if (els.length == 0)
			return NATText.atValue(String.valueOf(start+stop));
		
	    StringBuffer buff = new StringBuffer(start);
		for (int i = 0; i < els.length; i++) {
			ATObject e = els[i];
			if (e instanceof NATAbstractGrammar) {
				//buff.append(e.toString());
				buff.append(e.meta_print().javaValue);
			} else {
				buff.append(e.impl_asCode(objectMap).asNativeText().javaValue);
			}
			//buff.append(e.impl_asCode(objectMap).asNativeText().javaValue);
			if (i < (els.length - 1)) { buff.append(sep); }
		}
		buff.append(stop);
	    return NATText.atValue(buff.toString());
	}
	
	public final static NATText codeParameterList(TempFieldGenerator objectMap, NATTable params) throws InterpreterException {
		NATText plist = codeElements(objectMap, params, "", ", ", "");
		if (plist.javaValue.isEmpty()) {
			return plist;
		} else {
			return NATText.atValue("|" + plist.javaValue + "| ");
		}
	}
	
	/**
	 * Auxiliary function used to code the elements of a table using various separators.
	 */
	public final static NATText codeElements(TempFieldGenerator objectMap, NATTable tab,String start, String sep, String stop) throws InterpreterException {
		return codeElements(objectMap, tab.elements_, start, sep, stop);
	}

	public static final NATText codeAsStatements(TempFieldGenerator objectMap, ATTable tab) throws InterpreterException {
		return codeElements(objectMap, tab.asNativeTable(), "", "; ", "");
	}
	
	public static final NATText codeAsStatements(TempFieldGenerator objectMap, ATObject[] els) throws InterpreterException {
		return codeElements(objectMap, els, "", "; ", "");
	}


	public static final NATText codeAsList(TempFieldGenerator objectMap, ATTable tab) throws InterpreterException {
		return codeElements(objectMap, tab.asNativeTable(), "(", ", ", ")");
	}

	/**
	 * This function is called whenever arguments to a function, message, method need to be evaluated.
	 * TODO(coercers) currently does not work for user-defined tables
	 */
	public static final NATTable evaluateArguments(NATTable args, ATContext ctx) throws InterpreterException {
		if (args == NATTable.EMPTY) return NATTable.EMPTY;
		
		ATObject[] els = args.elements_;
		
		LinkedList result = new LinkedList();
		int siz = els.length;
		for (int i = 0; i < els.length; i++) {
			if (els[i].isSplice()) {
				ATObject[] tbl = els[i].asSplice().base_expression().meta_eval(ctx).asNativeTable().elements_;
				for (int j = 0; j < tbl.length; j++) {
					result.add(tbl[j]);
				}
				siz += (tbl.length - 1); // -1 because we replace one element by a table of elements
			} else {
				result.add(els[i].meta_eval(ctx));
			}
		}
		return NATTable.atValue((ATObject[]) result.toArray(new ATObject[siz]));
	}
	
	/**
	 * Given a formal parameter list, this auxiliary method returns a new table
	 * consisting of the values of the bindings of the mandatory parameters of
	 * formals within the context ctx.
	 */
	public static NATTable evalMandatoryPars(ATTable formals, ATContext ctx) throws InterpreterException {
		if (formals == NATTable.EMPTY) {
			return NATTable.EMPTY;
		} else {
			ATObject[] pars = formals.asNativeTable().elements_;
			int numMandatory;
			for (numMandatory = 0; numMandatory < pars.length; numMandatory++) {
				if (!pars[numMandatory].isSymbol()) {
					break;
				}
			}
			if (numMandatory > 0) {
				ATObject[] bindings = new ATObject[numMandatory];
				for (int i = 0; i < bindings.length; i++) {
					bindings[i] = pars[i].asSymbol().meta_eval(ctx);
				}
				return NATTable.atValue(bindings);
			} else {
				return NATTable.EMPTY;
			}
		}
	}
	
	/**
	 * Given a set and a formal parameter list:
	 * <ul>
	 *   <li>first removes from the set all variable names declared in the parameterlist.
	 *   <li>then adds to the set all free variables found in optional variable expressions.
	 *   
	 *   For example, applied to the parameterlist <tt>f(x,y := z + 1, @ rest)</tt>, the variables
	 *   <tt>x, y</tt> and <tt>rest</tt> would be removed from the set and the variable
	 *   <tt>z</tt> would be added to the set.
	 * </ul>
	 */
	public static void processFreeVariables(Set freeVars, ATTable paramlist) throws InterpreterException {
		HashSet toAdd = new HashSet();
		ATObject[] params = paramlist.asNativeTable().elements_;
		for (int i = 0; i < params.length; i++) {
			if (params[i].isSymbol()) {
				// Mandatory arguments, e.g. x
				freeVars.remove(params[i].asSymbol());
			} else if (params[i].isVariableAssignment()) {
				// Optional arguments, e.g. x := 5
				freeVars.remove(params[i].asVariableAssignment().base_name());
				toAdd.addAll(params[i].asVariableAssignment().base_valueExpression().impl_freeVariables());
			} else if (params[i].isSplice()) {
				// Rest arguments, e.g. @x
				freeVars.remove(params[i].asSplice().base_expression().asSymbol());
			}
		}
		freeVars.addAll(toAdd);
	}
	
	/**
	 * Returns the raw contents of a file in a String (using this JVM's default character encoding)
	 */
	public static String loadContentOfFile(File file) throws IOException {
		InputStream is = new FileInputStream(file);
		
	    // Get the size of the file
	    long length = file.length();
	
	    // You cannot create an array using a long type.
	    // It needs to be an int type.
	    // Before converting to an int type, check
	    // to ensure that file is not larger than Integer.MAX_VALUE.
	    if (length > Integer.MAX_VALUE) {
	        throw new IOException("File is too large: "+file.getName());
	    }
	
	    // Create the byte array to hold the data
	    byte[] bytes = new byte[(int)length];
	
	    // Read in the bytes
	    int offset = 0;
	    int numRead = 0;
	    while (offset < bytes.length
	           && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
	        offset += numRead;
	    }
	
	    // Ensure all the bytes have been read in
	    if (offset < bytes.length) {
	        throw new IOException("Could not completely read file "+file.getName());
	    }
	
	    // Close the input stream and return bytes
	    is.close();
	    return new String(bytes);
	}

	/**
	 * @return the 'global' lexical scope of an actor, which is a normal native object
	 * whose lexical parent is OBJLexicalRoot.
	 */
	public static NATObject getGlobalLexicalScope() {
		return (NATObject) _GLOBAL_SCOPE_.get();
	}

	/**
	 * @return the lobby namespace of an actor, which is a normal empty object
	 */
	public static NATObject getLobbyNamespace() {
		return (NATObject) _LOBBY_NAMESPACE_.get();
	}
	
	/**
	 * @return the jlobby root package of an actor, which is a JavaPackage with an empty path prefix.
	 */
	public static JavaPackage getJLobbyRoot() {
		return (JavaPackage) _JLOBBY_ROOT_.get();
	}
	
	/**
	 * @return the mirror root of an actor, from which intercessive mirrors usually inherit.
	 */
	public static NATMirrorRoot getMirrorRoot() {
		return (NATMirrorRoot) _MIRROR_ROOT_.get();
	}
	
	/**
	 * @return the nil object of an actor, to which all objects owned by that actor eventually delegate.
	 */
	public static NATNil getNil() {
		return (NATNil) _NIL_.get();
	}
	
	/**
	 * Restores the global lexical scope to a fresh empty object.
	 * Resets the lobby global namespace.
	 */
	public static void resetEnvironment() {
		_GLOBAL_SCOPE_.set(createGlobalLexicalScope());
		_LOBBY_NAMESPACE_.set(createLobbyNamespace());
		_JLOBBY_ROOT_.set(createJLobbyRoot());
		_MIRROR_ROOT_.set(createMirrorRoot());
	}
	
	/**
	 * A global scope has the sentinel instance as its lexical parent.
	 */
	private static NATObject createGlobalLexicalScope() {
		return new NATObject(OBJLexicalRoot._INSTANCE_);
		
		/* This will create an unnnecessary remote far references for the
		 * global lexical scope for isolate objects taking along
		 * the lexicalParent_. However, lexicaParent_ is rebound 
		 * upon arrival. As solution, we could pass the root as follows
		 * by it fails in Android (see issue#44 in the google code site).

		NATObject root = new NATObject(OBJLexicalRoot._INSTANCE_) {
			// override meta_pass to avoid the creation of a far reference 
			// when the root object gets parameter passed.
			public ATObject meta_pass() throws InterpreterException {
				return createSerializedLexicalRoot();
			}	
		};
		return root; */
	}
	
	/**
	 * returns an object equivalent to:
	 * <code>
	 * isolate: { nil } mirroredBy: (mirror: {
	 *   def resolve() { root } // re-bind to the root of the resolving actor
	 * })
	 * </code>
	 * 
	 * Note: in principle the code of this method could be called in-line
	 * in the meta_pass method defined in the createGlboalLexicalScope method above.
	 * However, this causes the Java serializer to throw a ClassCastException. 
	 * 
	 * IMPORTANT: Not used by now because it raises a java.exception.IndexOutOfBound in Android. 
	 * See issue#44 in the google code site.
	 */
	/*private static ATObject createSerializedLexicalRoot() {
		return new NATObject(new ATTypeTag[] {NativeTypeTags._ISOLATE_}) {
			public ATObject meta_resolve() throws InterpreterException {
				return Evaluator.getGlobalLexicalScope();
			}
			public NATText meta_print() throws InterpreterException {
				return NATText.atValue("<serialized lexical root>");
			}
		};
	} */
	
    /**
     * A lobby namespace is a simple empty object
     */
	private static NATObject createLobbyNamespace() {
		return new NATObject();
	}

    /**
     * A jlobby root package is a JavaPackage with an empty path prefix
     */
	private static NATObject createJLobbyRoot() {
		return new JavaPackage("");
	}

    /**
     * The default mirror root, with an empty base-level object
     */
	private static ATMirrorRoot createMirrorRoot() {
		return new NATMirrorRoot();
	}
	
    /**
     * The default nil object.
     */
	private static NATNil createNil() {
		return new NATNil();
	}
	
	public static final String valueNameOf(Class c) {
		String name = getSimpleName(c);
		if (name.startsWith("AT")) {
			return "a" + classnameToValuename(name, "AT");
		} else if (name.startsWith("NAT")) {
			return "a native" + classnameToValuename(name, "NAT");
		} else if (name.startsWith("AG")) {
			return "a native AST" + classnameToValuename(name, "AG");
		} else if (name.startsWith("X")) {
			return "a native exception" + classnameToValuename(name, "X");
		} else if (name.startsWith("OBJ")) {
			return "the native object" + classnameToValuename(name, "OBJ");
		} else {
			return name;
		}
	}
	
	public static final String toString(ATObject obj) {
		try {
			return obj.meta_print().javaValue;
		} catch(InterpreterException e) {
			return "<unprintable: " + e.getMessage() + ">";
		}
	}
	
    private static final REProgram _UPPERCASE_ = Regexp.compile("[A-Z]");
	
	private static final String classnameToValuename(String classname, String prefix) {
		// first, get rid of the given prefix by replacing it with ""
		String classnameWithoutPrefix = new RE(prefix).subst(classname, "",RE.REPLACE_FIRSTONLY);
		 // next, replace all uppercased letters "L" by " l"		 
        try {
        	return Regexp.replaceAll(new RE(_UPPERCASE_), classnameWithoutPrefix, new Regexp.StringCallable() {
            	public String call(String uppercaseLetter) {
            		return " " + Character.toLowerCase(uppercaseLetter.charAt(0));
            	}
            });
        } catch (InterpreterException e) { // all this just to make the compiler happy
        	Logging.VirtualMachine_LOG.fatal("Unexpected exception: " + e.getMessage(), e);
        	throw new RuntimeException("Unexpected exception: " + e.getMessage());
        }
	}
	
	// UTILITY METHODS
	
	/**
	 * Truncate a given string up to the given maxLength.
	 * An ellipsis is appended to truncated strings.
	 * Note that the ellipsis may cause the string to have length up to maxLength + 3
	 */
	public static final String trunc(String toTruncate, int maxLength) {
		if (toTruncate.length() > maxLength) {
			return toTruncate.substring(0, maxLength) + "...";
		} else {
			return toTruncate;
		}
	}
	
	/**
	 * Returns the unqualified name of a class.
	 */
	public static final String getSimpleName(Class c) {
		String nam = c.getName();
		return nam.substring(nam.lastIndexOf(".") + 1);
	}
	
	public static final Exception asJavaException(ATObject atObj) throws InterpreterException {
		if (atObj instanceof NATException) {
			return ((NATException)atObj).getWrappedException();
		}
		
		if (atObj.isJavaObjectUnderSymbiosis()) {
			JavaObject jObject = atObj.asJavaObjectUnderSymbiosis();
			
			Object object = jObject.getWrappedObject();
			if (object instanceof Exception) {
				return (Exception) object;				
			}
		}
		
		return new XAmbienttalk(atObj);
	}
	
	public static final InterpreterException asNativeException(ATObject atObj) throws InterpreterException {
		Exception exc = asJavaException(atObj);
		if (exc instanceof InterpreterException) {
			return (InterpreterException) exc;
		} else {
			return new XJavaException(exc);
		}
	}
	
	/**
	 * Performs <code>closure&lt;-apply(arguments)@[]</code>
	 * This method must be invoked by the Event Loop that owns the far reference.
	 * 
	 * @param closure an {@link ATFarReference} to an {@link ATClosure}.
	 */
	public static ATObject trigger(ATObject closure, ATTable arguments) throws InterpreterException {
		return closure.meta_receive(
				NATAsyncMessage.createExternalAsyncMessage(Evaluator._APPLY_, 
															NATTable.of(arguments),
															NATTable.EMPTY));
	}
	
	/**
	 * Performs <code>closure&lt;-apply(arguments)@[]</code>
	 * Used when the {@link ELActor} owner of the closure is known.
	 * @param closure a local reference to a closure.
	 * @param type a description of the type of event handler (used for debugging only)
	 */
	public static void trigger(ELActor owner, ATObject closure, ATTable arguments, String type) {
		// PREVIOUS BUG: owner.acceptSelfSend calls owner.mirror_.base_schedule(...), which may run
		// arbitrary AmbientTalk code. However, this method may be invoked by non-actor
		// threads (such as the ELVirtualMachine when notifying when:disconnected: listeners)
		// therefore, we now ask the actor to perform the call to acceptSelfSend itself,
		// by means of an event_trigger event
		//owner.acceptSelfSend(closure,
		//		new NATAsyncMessage(Evaluator._APPLY_,
		//					        NATTable.of(arguments),
		//					        NATTable.EMPTY));
		owner.event_trigger(closure, arguments, type);
	}
	
}
