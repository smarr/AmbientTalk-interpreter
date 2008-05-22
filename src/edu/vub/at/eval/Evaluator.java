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
import java.util.LinkedList;

import org.apache.regexp.RE;
import org.apache.regexp.REProgram;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XAmbienttalk;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.mirrors.NATMirrorRoot;
import edu.vub.at.objects.natives.NATException;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.OBJLexicalRoot;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.grammar.AGSplice;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.at.objects.symbiosis.JavaObject;
import edu.vub.at.objects.symbiosis.JavaPackage;
import edu.vub.at.objects.symbiosis.XJavaException;
import edu.vub.at.util.logging.Logging;
import edu.vub.util.Regexp;

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
		NATObject root = new NATObject(OBJLexicalRoot._INSTANCE_);
		return root;
	}
	
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
	private static NATMirrorRoot createMirrorRoot() {
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
}
