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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XIOProblem;
import edu.vub.at.exceptions.XParseError;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.Reflection;
import edu.vub.at.parser.NATParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author tvc
 *
 * Instances of the class NATNamespace represent namespace objects.
 * A namespace object is simply an object that is mirrored by a mirror whose doesNotUnderstand
 * method reacts differently from the standard semantics of raising a 'selector not found' exception.
 * 
 * A namespace object encapsulates an absolute file system directory path and a relative 'name'.
 * The name should correspond to a portion of the tail of the absolute path.
 * 
 * A namespace delegates to its parent (..) directory (shares-a link) and has the lexical root as its lexical parent.
 * 
 * When a slot is looked up in a namespace NS for a path P (via meta_select) and not found, the namespace object
 * queries the local file system to see whether the selector corresponds to a directory or file in the
 * directory P. Either the selector:
 *  - corresponds to a directory, in which case the missing slot is bound to a new namespace object corresponding to the path P/selector
 *  - corresponds to a file named selector.at, in which case:
 *    1) the slot is temporarily bound to nil
 *       (this is to prevent loops when the code to be evaluated would refer to itself;
 *        it also means there can be no circular dependencies, because referring to a slot still under construction yields nil)
 *    2) a new namespace object NNS initialized with path P/selector is created.
 *       This namespace object acts as the local scope for the file and delegates to its parent namespace.
 *    3) the code in the file is loaded and evaluated in the context (current=NNS, self=NNS, super=NS)
 *    4) the result of the evaluated code is bound to the missing slot.
 *       The next time the slot is queried for in the namespace, the value is immediately returned. This prevents
 *       files from being loaded twice.
 *  - does not correspond to any file or directory, resulting in a selector not found exception as usual.
 */
public final class NATNamespace extends NATObject {

	private static final String _AT_EXT_ = ".at";
	
	private final String path_;
	private final String name_;
	
	/**
	 * A namespace object encapsulates the given path, has the given shares-a dynamic parent
	 * and has the lexical root as its lexical parent.
	 * 
	 * @param name the name of this namespace (corresponding to a certain depth to the tail of the absolute path)
	 * @param path an absolute path referring to a local file system directory.
	 * @param parentNamespace this namespace's parent (the '..' directory)
	 */
	public NATNamespace(String name, String path, ATObject parentNamespace) {
		super(parentNamespace, OBJLexicalRoot._INSTANCE_, NATObject._SHARES_A_);
		name_ = name;
		path_ = path;
	}
	
	/**
	 * For a namespace object, doesNotUnderstand triggers the querying of the local file system
	 * to load files corresponding to the missing selector.
	 */
	public ATObject meta_doesNotUnderstand(ATSymbol selector) throws NATException {
		// first, convert the AmbientTalk name to a Java selector. Java selectors are always valid filenames because
		// they do not contain special operator characters
		String javaSelector = Reflection.upSelector(selector);
		
		// first, try to see if the file exists and corresponds to a directory
		File dir = new File(path_, javaSelector);
		if (dir.exists() && dir.isDirectory()) {
             // create a new namespace object for this directory
			NATNamespace childNS = new NATNamespace(name_ + "/" + javaSelector, path_ + File.separator + javaSelector, this);

			// bind the new child namespace to the selector
			this.meta_defineField(selector, childNS);
			return childNS;
				
		} else {
			// try to see if a file with extension .at exists corresponding to the selector
			File src = new File(path_, javaSelector + _AT_EXT_);
			if (src.exists() && src.isFile()) {
			
                 // bind the missing slot to nil to prevent calling this dNU recursively when evaluating the code in the file
				this.meta_defineField(selector, NATNil._INSTANCE_);
				
	             // create a new namespace object for this file
				NATNamespace childNS = new NATNamespace(name_ + "/" + javaSelector + _AT_EXT_,
						                                path_ + File.separator + javaSelector,
						                                this);
				
				try {
                     // load the code from the file
					String code = loadContentOfFile(src);
				
				    // construct the proper evaluation context for the code
				    NATContext ctx = new NATContext(childNS, childNS, this);
				    
				    // parse and evaluate the code in the proper context and bind its result to the missing slot
					ATAbstractGrammar source = NATParser._INSTANCE_.base_parse(NATText.atValue(code));
					ATObject result = source.meta_eval(ctx);
					this.meta_assignField(selector, result);
					
					return result;
				} catch (IOException e) {
					throw new XIOProblem(e);
				} catch (XParseError e) {
					e.setOriginatingFile(src.getAbsolutePath());
					throw e;
				}
				
			} else { // neither a matching directory nor a matching file.at were found
                 // perform the default dNU behaviour, which means raising a 'selector not found' exception
				return super.meta_doesNotUnderstand(selector);
			}
		}
	}

	public NATText meta_print() {
		return NATText.atValue("<ns:"+name_+">");
	}

	// auxiliary methods
	
	/**
	 * Returns the raw contents of a file in a String (using this JVM's default character encoding)
	 */
    private static String loadContentOfFile(File file) throws IOException {
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
	
}
