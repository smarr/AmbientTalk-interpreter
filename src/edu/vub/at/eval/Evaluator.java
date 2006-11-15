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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.OBJLexicalRoot;
import edu.vub.at.objects.natives.grammar.AGSplice;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Evaluator class serves as a repository for auxiliary evaluation methods.
 * 
 * @author tvc
 */
public final class Evaluator {
	
	// important symbols
	
	public static final AGSymbol _ANON_MTH_NAM_ = AGSymbol.alloc("nativelambda");
	public static final NATTable _ANON_MTH_ARGS_ = new NATTable(new ATObject[] { new AGSplice(AGSymbol.alloc("args")) });
	public static final AGSymbol _LAMBDA_    = AGSymbol.alloc(NATText.atValue("lambda"));
	public static final AGSymbol _INIT_      = AGSymbol.alloc(NATText.atValue("init"));
	public static final AGSymbol _CURNS_SYM_ = AGSymbol.alloc("~");
	
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
			if (els[i].base_isSplice()) {
				ATObject[] tbl = els[i].base_asSplice().base_getExpression().meta_eval(ctx).asNativeTable().elements_;
				for (int j = 0; j < tbl.length; j++) {
					result.add(tbl[j]);
				}
				siz += (tbl.length - 1); // -1 because we replace one element by a table of elements
			} else {
				result.add(els[i].meta_eval(ctx));
			}
		}
		return new NATTable((ATObject[]) result.toArray(new ATObject[siz]));
	}

	// auxiliary interface to support functor objects
	private interface BindClosure {
		public void bindParamToArg(ATObject inScope, ATSymbol param, ATObject arg) throws InterpreterException;
	}
	
	/**
	 * Auxiliary function to bind formal parameters to actual arguments within a certain scope.
	 * TODO(coercers) currently does not work for user-defined ATTables
	 * 
	 * @param funnam the name of the function for which to bind these elements, for debugging purposes only
	 * @param scope the frame in which to store the bindings
	 * @param parameters the formal parameter references (of which the last element may be a 'rest' arg to collect left-over arguments)
	 * @param arguments the actual arguments, already evaluated
	 * @param binder a functor object describing the strategy to bind an argument to a parameter (assign or define the parameter)
	 * @throws XArityMismatch when the formals don't match the actuals
	 */
	private static final void bindArguments(String funnam, ATObject scope, ATTable parameters, ATTable arguments, BindClosure binder) throws InterpreterException {
		if (parameters == NATTable.EMPTY) {
			if (arguments == NATTable.EMPTY)
				return; // no need to bind any arguments
			else
				throw new XArityMismatch(funnam, 0, arguments.base_getLength().asNativeNumber().javaValue); 
		}
		
		ATObject[] pars = parameters.asNativeTable().elements_;
		ATObject[] args = arguments.asNativeTable().elements_;
		
		// check to see whether the last argument is a spliced parameters, which
		// indicates a variable parameter list
		if (pars[pars.length - 1].base_isSplice()) {
			int numMandatoryPars = (pars.length - 1);
			// if so, check whether at least all mandatory parameters are matched
			if (args.length < numMandatoryPars)
				throw new XArityMismatch(funnam, numMandatoryPars, args.length);
			
			// bind all parameters except for the last one
			for (int i = 0; i < numMandatoryPars; i++) {
				binder.bindParamToArg(scope, pars[i].base_asSymbol(), args[i]);
			}
			
			// bind the last parameter to the remaining arguments
			int numRemainingArgs = args.length - numMandatoryPars;
			ATObject[] restArgs = new ATObject[numRemainingArgs];
			for (int i = 0; i < numRemainingArgs; i++) {
				restArgs[i] = args[i + numMandatoryPars];
			}
			ATSymbol restArgsName = pars[numMandatoryPars].base_asSplice().base_getExpression().base_asSymbol();
			binder.bindParamToArg(scope, restArgsName, new NATTable(restArgs));
			
		} else {
			// regular parameter list: arguments and parameters have to match exactly
			if (pars.length != args.length)
				throw new XArityMismatch(funnam, pars.length, args.length);	
		
			for (int i = 0; i < pars.length; i++) {
			     binder.bindParamToArg(scope, pars[i].base_asSymbol(), args[i]);	
		    }
		}
	}
	
	/**
	 * Bind all of the given parameters as newly defined slots in the given scope to the given arguments
	 */
	public static final void defineParamsForArgs(String funnam, ATObject scope, ATTable parameters, ATTable arguments) throws InterpreterException {
		bindArguments(funnam, scope, parameters, arguments, new BindClosure() {
			public void bindParamToArg(ATObject scope, ATSymbol param, ATObject arg) throws InterpreterException {
				scope.meta_defineField(param, arg);
			}
		});
	}
	
	/**
	 * Assign all of the formal parameter names in the scope object to the given arguments
	 */
	public static final void assignArgsToParams(String funnam, ATObject scope, ATTable parameters, ATTable arguments) throws InterpreterException {
		bindArguments(funnam, scope, parameters, arguments, new BindClosure() {
			public void bindParamToArg(ATObject scope, ATSymbol param, ATObject arg) throws InterpreterException {
				scope.meta_assignVariable(param, arg);
			}
		});
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
	 * Restores the global lexical scope to a fresh empty object.
	 * Resets the lobby global namespace.
	 */
	public static void resetEnvironment() {
		_GLOBAL_SCOPE_.set(createGlobalLexicalScope());
		_LOBBY_NAMESPACE_.set(createLobbyNamespace());
	}
	
	/**
	 * A global scope has the sentinel instance as its lexical parent
	 */
	private static NATObject createGlobalLexicalScope() {
		return new NATObject(OBJLexicalRoot._INSTANCE_);
	}
	
    /**
     * A lobby namespace is a simple empty object
     */
	private static NATObject createLobbyNamespace() {
		return new NATObject();
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
	
	private static final String classnameToValuename(String classname, String prefix) {
		 // replace all uppercased letters "L" by " l"
		 Pattern p = Pattern.compile("[A-Z]");
		 Matcher m = p.matcher(classname.replaceFirst(prefix, ""));
		 StringBuffer sb = new StringBuffer();
		 while (m.find()) {
		     m.appendReplacement(sb, " " + Character.toString(Character.toLowerCase(m.group().charAt(0))));
		 }
		 m.appendTail(sb);
		 return sb.toString();
	}
	
	// UTILITY METHODS
	
	/**
	 * Returns the unqualified name of a class.
	 */
	public static final String getSimpleName(Class c) {
		String nam = c.getName();
		return nam.substring(nam.lastIndexOf(".") + 1);
	}
	
}
