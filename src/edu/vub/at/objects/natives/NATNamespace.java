/**
 * AmbientTalk/2 Project
 * NATNamespace.java created on 6-sep-2006 at 14:33:41
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
package edu.vub.at.objects.natives;

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XDuplicateSlot;
import edu.vub.at.exceptions.XIOProblem;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.parser.NATParser;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Vector;

/**
 * Instances of the class NATNamespace represent namespace objects.
 * 
 * Namespace objects act as regular AmbientTalk objects with the following differences and conventions:
 *  - Behaviourally, a namespace object is mirrored by a mirror whose doesNotUnderstand
 *    method reacts differently from the standard semantics of raising a 'selector not found' exception.
 *  - Structurally, a namespace has the lexical root as its lexical parent and the dynamic root as its dynamic parent.
 *    Furthermore, a namespace object encapsulates an absolute file system path and a relative 'path name'.
 *    The name should correspond to a portion of the tail of the absolute path.
 *    These variables are not visible to AmbientTalk code.
 *         
 * When a slot is looked up in a namespace NS for a path P (via meta_select) and not found, the namespace object
 * queries the local file system to see whether the selector corresponds to a directory or file in the
 * directory P. Either the selector:
 *  - corresponds to a directory, in which case the missing slot is bound to a new namespace object corresponding to the path P/selector
 *  - corresponds to a file named selector.at, in which case:
 *    1) the slot is temporarily bound to nil
 *       (this is to prevent loops when the code to be evaluated would refer to itself;
 *        it also means there can be no circular dependencies, because referring to a slot still under construction yields nil)
 *    2) a new object FS (the 'file scope') is created.
 *       This object acts as the local scope for the file and has access to its 'enclosing' namespace via the '~' slot.
 *       Hence, it can refer to other files in the 'current directory' using ~.filename
 *    3) the code in the file is loaded and evaluated in the context (current=FS, self=FS, super=FS.parent=dynroot)
 *    4) the result of the evaluated code is bound to the missing slot.
 *       The next time the slot is queried for in the namespace, the value is immediately returned. This prevents
 *       files from being loaded twice.
 *  - does not correspond to any file or directory, resulting in a selector not found exception as usual.
 * 
 * @author tvcutsem
 * @author smostinc
 */
public final class NATNamespace extends NATObject {

	private static final String _AT_EXT_ = ".at";
	private final File path_;
	private final String name_;
	
	/**
	 * A namespace object encapsulates a given absolute path and represents the given relative path.
	 * 
	 * @param name the name of this namespace (corresponding to a certain depth to the tail of the absolute path)
	 * @param path an absolute path referring to a local file system directory.
	 */
	public NATNamespace(String name, File path) {
		super();
		name_ = name;
		path_ = path;
	}
	
	/**
	 * Private constructor used only for cloning
	 */
	private NATNamespace(FieldMap map,
			  Vector state,
			  LinkedList customFields,
			  MethodDictionary methodDict,
			  ATObject dynamicParent,
			  ATObject lexicalParent,
			  byte flags,
			  ATTypeTag[] types,
			  File path,
			  String name) throws InterpreterException {
	  super(map, state, customFields, methodDict, dynamicParent, lexicalParent, flags, types);
	  path_ = path;
	  name_ = name;
	}
	
	/**
	 * For a namespace object, doesNotUnderstand triggers the querying of the local file system
	 * to load files corresponding to the missing selector.
	 */
	public ATClosure meta_doesNotUnderstand(ATSymbol selector) throws InterpreterException {
		// first, convert the AmbientTalk name to a Java selector. Java selectors are always valid filenames because
		// they do not contain special operator characters
		String javaSelector = Reflection.upSelector(selector);
		
		// first, try to see if the file exists and corresponds to a directory
		File dir = new File(path_, javaSelector);
		if (dir.exists() && dir.isDirectory()) {
             // create a new namespace object for this directory
			final NATNamespace childNS = new NATNamespace(name_ + "/" + javaSelector, dir);

			// bind the new child namespace to the selector
			this.meta_defineField(selector, childNS);
			return new NativeClosure(this) {
				public ATObject base_apply(ATTable args) {
					return childNS;
				}
			};
		} else {
			// try to see if a file with extension .at exists corresponding to the selector
			File src = new File(path_, javaSelector + _AT_EXT_);
			if (src.exists() && src.isFile()) {
			
                 // bind the missing slot to nil to prevent calling this dNU recursively when evaluating the code in the file
				this.meta_defineField(selector, NATNil._INSTANCE_);
				
	             // create a new file scope object for this file
				NATObject fileScope = createFileScopeFor(this);
				
				try {
                     // load the code from the file
					String code = Evaluator.loadContentOfFile(src);
				
				    // construct the proper evaluation context for the code
				    NATContext ctx = new NATContext(fileScope, fileScope);
				    
				    // parse and evaluate the code in the proper context and bind its result to the missing slot
					ATAbstractGrammar source = NATParser.parse(src.getName(), code);
					final ATObject result = source.meta_eval(ctx);
					this.impl_mutateSlot(this, selector.asAssignmentSymbol(), NATTable.of(result));
					//this.meta_assignField(this, selector, result);
					
					return new NativeClosure(this) {
						public ATObject base_apply(ATTable args) {
							return result;
						}
					};
				} catch (IOException e) {
					throw new XIOProblem(e);
				}
				
			} else { // neither a matching directory nor a matching file.at were found
                 // perform the default dNU behaviour, which means raising a 'selector not found' exception
				return super.meta_doesNotUnderstand(selector);
			}
		}
	}

	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<ns:"+name_+">");
	}
	
	public static NATObject createFileScopeFor(NATNamespace ns) {
		NATObject fileScope = new NATObject();
		// a fileScope object is empty, save for a reference to its creating namespace
		try {
			fileScope.meta_defineField(Evaluator._CURNS_SYM_, ns);
		} catch (XDuplicateSlot e) {
			// impossible: the object is empty
			e.printStackTrace();
		} catch (XTypeMismatch e) {
			// impossible: the given selector is native
			e.printStackTrace();
		} catch (InterpreterException e) {
			// impossible : call cannot be intercepted : namespaces are not mirages
			e.printStackTrace();			
		}
		return fileScope;
	}
	
	protected NATObject createClone(FieldMap map,
			  Vector state,
			  LinkedList customFields,
			  MethodDictionary methodDict,
			  ATObject dynamicParent,
			  ATObject lexicalParent,
			  byte flags, ATTypeTag[] types) throws InterpreterException {
      return new NATNamespace(map,
    		  				    state,
    		  				    customFields,
    		  				    methodDict,
    		  				    dynamicParent,
    		  				    lexicalParent,
    		  				    flags,
    		  				    types,
    		  				    path_,
    		  				    name_);
	}
	
}
