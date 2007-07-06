/**
 * AmbientTalk/2 Project
 * Import.java created on 8-mrt-2007 at 12:57:46
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
import edu.vub.at.exceptions.XDuplicateSlot;
import edu.vub.at.exceptions.XImportConflict;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATCallframe;
import edu.vub.at.objects.natives.NATClosure;
import edu.vub.at.objects.natives.NATClosureMethod;
import edu.vub.at.objects.natives.NATMethod;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGBegin;
import edu.vub.at.objects.natives.grammar.AGDelegationCreation;
import edu.vub.at.objects.natives.grammar.AGMessageSend;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Auxiliary class that provides the implementation of the native 'import' statement.
 *
 * @author tvcutsem
 */
public final class Import {

	private static HashSet _DEFAULT_EXCLUDED_SLOTS_;
	private static HashSet getDefaultExcludedSlots() {
		if (_DEFAULT_EXCLUDED_SLOTS_ == null) {
			_DEFAULT_EXCLUDED_SLOTS_ = new HashSet();
			  // prepare the default names to exclude
			_DEFAULT_EXCLUDED_SLOTS_.add(NATObject._SUPER_NAME_); // skip 'super', present in all objects
			_DEFAULT_EXCLUDED_SLOTS_.add(Evaluator._CURNS_SYM_); // sip '~', present in all namespaces
			_DEFAULT_EXCLUDED_SLOTS_.add(NATObject._EQL_NAME_); // skip '==', present in all objects
			_DEFAULT_EXCLUDED_SLOTS_.add(NATObject._INI_NAME_); // skip 'init', present in all objects
			_DEFAULT_EXCLUDED_SLOTS_.add(NATObject._NEW_NAME_); // skip 'new', present in all objects
		}
		return _DEFAULT_EXCLUDED_SLOTS_;
	}
	
	/**
	 * Given a table of tables, of the form [ [oldname, newname], ... ], returns a hashtable
	 * mapping the old names to the new names.
	 */
	public static Hashtable preprocessAliases(ATTable aliases) throws InterpreterException {
		  Hashtable aliasMap = new Hashtable();
		  if (aliases != NATTable.EMPTY) {
			  NATNumber two = NATNumber.atValue(2);
			  
			  // preprocess the aliases
			  ATObject[] mappings = aliases.asNativeTable().elements_;
			  for (int i = 0; i < mappings.length; i++) {
				  // expecting tuples [ oldname, newname ]
				  ATTable alias = mappings[i].asTable();
				  aliasMap.put(alias.base_at(NATNumber.ONE).asSymbol(), alias.base_at(two).asSymbol());
			  }  
		  }		  
		  return aliasMap;
	}
	
	/**
	 * Given a table of symbols, returns a hashset containing all the names.
	 */
	public static HashSet preprocessExcludes(ATTable exclusions) throws InterpreterException {
		if (exclusions != NATTable.EMPTY) {
			// make a copy of the default exclusion set such that the default set is not modified
			HashSet exclude = (HashSet) getDefaultExcludedSlots().clone();
			  
			// preprocess the exclusions
			ATObject[] excludedNames = exclusions.asNativeTable().elements_;
			for (int i = 0; i < excludedNames.length; i++) {
			  // expecting symbols
			  exclude.add(excludedNames[i].asSymbol());
			}
			  
			return exclude;	
		} else {
			return getDefaultExcludedSlots();
		}
	}
	
	private static final AGSymbol _IMPORTED_OBJECT_NAME_ = AGSymbol.jAlloc("importedObject");
	
	/**
	 * Imports fields and methods from a given source object. This operation is very
	 * akin to a class using a trait. For each field in the trait, a new field
	 * is created in the importing 'host' object. For each method in the trait, a method
	 * is added to the host object whose body consists of delegating the message
	 * to the trait object.
	 * 
	 * The purpose of import is to:
	 *  - be able to reuse the interface of an existing object (examples are
	 *    traits or 'mixins' such as Enumerable, Comparable, Observable, ...)
	 *  - be able to access the interface of an existing object without having
	 *    to qualify access. This is especially useful when applied to namespace
	 *    objects. E.g. 'import: at.collections' allows the importer to subsequently
	 *    write Vector.new() rather than at.collections.Vector.new()
	 * 
	 * Import is implemented as abstract grammar and not as a native 'import:' function
	 * because it requires access to (and modifies) the lexical scope of the invoker.
	 * Native functions (or normal AT/2 functions, for that matter) have no access to that scope.
	 * However, if a pseudovariable 'thisContext' were available in AT/2, import could probably
	 * be defined as a method on contexts, as follows:
	 * 
	 * def context.import: sourceObject {
	 *   def newHost := context.lexicalScope;
	 *   def allFields := (reflect: sourceObject).listFields().base;
	 *   def allMethods := (reflect: sourceObject).listMethods().base;
	 *   allFields.each: { |field|
	 *     (reflect: newHost).addField(field)
	 *   }
	 *   allMethods.each: { |method|
	 *     (reflect: newHost).addMethod(aliasFor(method.name), `[@args],
	 *       `#sourceObject^#(method.name)(@args))
	 *   }
	 *   nil
	 * }
	 * 
	 * All duplicate slot exceptions, which signify that an imported method or field already
	 * exists, are caught during import. These exceptions are bundled into an XImportConflict
	 * exception, which can be inspected by the caller to detect the conflicting, unimported,
	 * fields or methods.
	 * 
	 * @param sourceObject the object from which to import fields and methods
	 * @param ctx the runtime context during which the import is performed, the lexical scope is the object that performed the import
	 * @param aliases a mapping from old names (ATSymbol) to new names (ATSymbol)
	 * @param exclude a set containing slot names (ATSymbol) to disregard
	 */
	public static ATObject performImport(ATObject sourceObject, ATContext ctx,
			                             Hashtable aliases, HashSet exclude) throws InterpreterException {
		ATObject hostObject = ctx.base_lexicalScope();

		// stores all conflicting symbols, initialized lazily
		Vector conflicts = null;

		// the alias to be used for defining the new fields or methods
		ATSymbol alias;
		
		// define the aliased fields
		ATField[] fields = NATObject.listTransitiveFields(sourceObject);
		for (int i = 0; i < fields.length; i++) {
			ATField field = fields[i];
			// skip excluded fields, such as the 'super' field
			if (!exclude.contains(field.base_name())) {
				
				// check whether the field needs to be aliased
				alias = (ATSymbol) aliases.get(field.base_name());
				if (alias == null) {
					// no alias, use the original name
					alias = field.base_name();
				}
				
				try {
					hostObject.meta_defineField(alias, field.base_readField());
				} catch(XDuplicateSlot e) {
					if (conflicts == null) {
						conflicts = new Vector(1);
					}
					conflicts.add(e.getSlotName());
				}
			}
		}

		// define the delegate methods
		ATMethod[] methods = NATObject.listTransitiveMethods(sourceObject);
		
		if (methods.length > 0) {
			
            // create the lexical scope for the delegate method invocation by hand
			NATCallframe delegateScope = new NATCallframe(hostObject);
			// add the parameter, it is used in the generated method
			delegateScope.meta_defineField(_IMPORTED_OBJECT_NAME_, sourceObject);
			
			for (int i = 0; i < methods.length; i++) {
				ATSymbol origMethodName = methods[i].base_name();

				// filter out exluded methods, such as primitive methods like '==', 'new' and 'init'
				if (exclude.contains(origMethodName)) {
					// if these primitives would not be filtered out, they would override
					// the primitives of the host object, which is usually unwanted and could
					// lead to subtle bugs w.r.t. comparison and instance creation.
					continue;
				}	
				// check whether the method needs to be aliased
				alias = (ATSymbol) aliases.get(origMethodName);
				if (alias == null) {
					// no alias, use the original name
					alias = origMethodName;
				}

				// def alias(@args) { importedObject^origMethodName(@args) }
				ATMethod delegate = new NATMethod(alias, Evaluator._ANON_MTH_ARGS_,
						new AGBegin(NATTable.of(
								//importedObject^origMethodName(@args)@[]
								new AGMessageSend(_IMPORTED_OBJECT_NAME_,
										new AGDelegationCreation(origMethodName,
												Evaluator._ANON_MTH_ARGS_, NATTable.EMPTY)))));

				/*
				 * Notice that the body of the delegate method is
				 *   sourceObject^selector@args)
				 * 
				 * In order for this code to evaluate when the method is actually invoked
				 * on the new host object, the symbol `sourceObject should evaluate to the
				 * object contained in the variable sourceObject.
				 * 
				 * To ensure this binding is correct at runtime, delegate methods are
				 * added to objects as external methods whose lexical scope is the call
				 * frame of this method invocation The delegate methods are not added as closures,
				 * as a closure would fix the value of 'self' too early.
				 * 
				 * When importing into a call frame, care must be taken that imported delegate
				 * methods are added as closures, because call frames cannot contain methods.
				 * In this case, the delegate is wrapped in a closure whose lexical scope is again
				 * the call frame of this primitive method invocation. The value of self is fixed
				 * to the current value, but this is OK given that the method is added to a call frame
				 * which is 'selfless'.
				 */

				try {
					if (hostObject.isCallFrame()) {
						NATClosure clo = new NATClosure(delegate, ctx.base_withLexicalEnvironment(delegateScope));
						hostObject.meta_defineField(origMethodName, clo);
					} else {
						hostObject.meta_addMethod(new NATClosureMethod(delegateScope, delegate));
					}
				} catch(XDuplicateSlot e) {
					if (conflicts == null) {
						conflicts = new Vector(1);
					}
					conflicts.add(e.getSlotName());
				}
			}
			
		}

		if (conflicts == null) {
			// no conflicts found
			return NATNil._INSTANCE_;
		} else {
			throw new XImportConflict((ATSymbol[]) conflicts.toArray(new ATSymbol[conflicts.size()]));
		}
	}
	
}
