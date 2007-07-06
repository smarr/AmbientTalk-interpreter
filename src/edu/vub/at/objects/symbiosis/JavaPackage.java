/**
 * AmbientTalk/2 Project
 * JavaPackage.java created on 19-nov-2006 at 12:31:39
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
package edu.vub.at.objects.symbiosis;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XClassNotFound;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.mirrors.PrimitiveMethod;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.objects.natives.FieldMap;
import edu.vub.at.objects.natives.MethodDictionary;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.util.LinkedList;
import java.util.Vector;

/**
 * A JavaPackage represents (part of) a Java package name and serves the same purpose
 * as AmbientTalk Namespace objects, but for loading Java classes rather than AT objects.
 * 
 * The behaviour of a JavaPackage object relies on Java naming conventions for automatic
 * loading of classes. If some Java code does not follow the naming conventions, then explicit
 * loading of packages or classes must be done via a JavaPackage's provided base-level methods.
 * 
 * Selecting a field f from a JavaPackage encapsulating the path p has the following semantics:
 *  - if f starts with an uppercase symbol, the field access is interpreted as a class reference:
 *    The JavaPackage tries to load the class p.f.
 *    If the class does not exist, an XSelectorNotFound exception is thrown.
 *  - if f starts with a lowercase symbol, the field access is interpreted as a subpackage reference:
 *    The JavaPackage creates a new field referring to a JavaPackage whose path equals 'p.f.'
 *    
 * JavaPackage instances are isolates, hence, they are pass-by-copy.
 * 
 * @author tvcutsem
 */
public final class JavaPackage extends NATObject {

	private static final String _PKG_SEP_ = ".";

	/** def class(name) { nil } */
	private static final PrimitiveMethod _PRIM_CLS_ = new PrimitiveMethod(
			AGSymbol.jAlloc("class"), NATTable.atValue(new ATObject[] { AGSymbol.jAlloc("name")})) {
		public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
			return ((JavaPackage)ctx.base_getLexicalScope()).base_class(arguments.base_at(NATNumber.ONE).asSymbol());
		}
	};
	/** def package(name) { nil } */
	private static final PrimitiveMethod _PRIM_PKG_ = new PrimitiveMethod(
			AGSymbol.jAlloc("package"), NATTable.atValue(new ATObject[] { AGSymbol.jAlloc("name")})) {
		public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
			return ((JavaPackage)ctx.base_getLexicalScope()).base_package(arguments.base_at(NATNumber.ONE).asSymbol());
		}
	};
	
	private final String path_;
	
	/**
	 * A JavaPackage object encapsulates a package path.
	 * A package path is a '.'-separated string, always ending with a '.'
	 * The jlobby root package has an empty package path
	 * 
	 * A JavaPackage is initialized as an AT/2 isolate object.
	 * 
	 * @param path the pathname of this JavaPackage, e.g. 'java.' or 'java.lang.'
	 */
	public JavaPackage(String path) {
		super(new ATTypeTag[] { NativeTypeTags._ISOLATE_ });
		path_ = path;
		try {
			super.meta_addMethod(_PRIM_CLS_);
			super.meta_addMethod(_PRIM_PKG_);
		} catch (InterpreterException e) {
			throw new RuntimeException("Failed to initialize a JavaPackage: " + e.getMessage());
		}
	}
	
	/**
	 * Private constructor used only for cloning
	 */
	private JavaPackage(FieldMap map,
			  		   Vector state,
			  		   LinkedList customFields,
			  		   MethodDictionary methodDict,
			  		   ATObject dynamicParent,
			  		   ATObject lexicalParent,
			  		   byte flags,
			  		   ATTypeTag[] types,
			  		   String path) throws InterpreterException {
		super(map, state, customFields, methodDict, dynamicParent, lexicalParent, flags, types);
		path_ = path;
	}
	
	/**
	 * For a JavaPackage object, doesNotUnderstand triggers the querying of the Java classpath
	 * to load classes corresponding to the missing selector. Depending on the case of the
	 * selector's first letter, the access is interpreted as a class or a package reference.
	 */
	public ATClosure meta_doesNotUnderstand(final ATSymbol selector) throws InterpreterException {
		// first, convert the AmbientTalk name to a Java selector.
		String s = selector.base_getText().asNativeText().javaValue;
		if (Character.isUpperCase(s.charAt(0))) {
			// the field access is interpreted as a class reference
			return new NativeClosure.Accessor(selector, this) {
				public ATObject access() throws InterpreterException {
					return base_class(selector);
				}
			};
		} else {
			// the field access is interpreted as a package reference
			return new NativeClosure.Accessor(selector, this) {
				public ATObject access() throws InterpreterException {
					return base_package(selector);
				}
			};
		}
	}

	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<jpackage:"+path_+">");
	}
	
	protected NATObject createClone(FieldMap map,
			  					Vector state,
			  					LinkedList customFields,
			  					MethodDictionary methodDict,
			  					ATObject dynamicParent,
			  					ATObject lexicalParent,
			  					byte flags, ATTypeTag[] types) throws InterpreterException {
		return new JavaPackage(map,
    		  				      state,
    		  				      customFields,
    		  				      methodDict,
    		  				      dynamicParent,
    		  				      lexicalParent,
    		  				      flags,
    		  				      types,
    		  				      path_);
	}
	
	/**
	 * Allows the AT programmer to explicitly load a class. This might be necessary if the
	 * class starts with a lowercase letter.
	 */
	public ATObject base_class(ATSymbol selector) throws InterpreterException {
		// try to see if a class corresponding to the selector prefixed with
		// this package's pathname exists
		String qualifiedClassname = path_ + Reflection.upSelector(selector);
		try {
			Class c = Class.forName(qualifiedClassname);
		    JavaClass jc = JavaClass.wrapperFor(c);
			// bind the new class to the selector within this JavaPackage
			this.meta_defineField(selector, jc);
			return jc;
		} catch (ClassNotFoundException e) {
			throw new XClassNotFound(qualifiedClassname, e);
		}
	}
	
	/**
	 * Allows the AT programmer to explicitly load a package. This might be necessary if the
	 * package starts with an uppercase letter.
	 */
	public ATObject base_package(ATSymbol selector) throws InterpreterException {
         // define a new Java package with a trailing '.'
		JavaPackage jpkg = new JavaPackage(path_ + Reflection.upSelector(selector) + _PKG_SEP_);
		this.meta_defineField(selector, jpkg);
		return jpkg;
	}
	
}
