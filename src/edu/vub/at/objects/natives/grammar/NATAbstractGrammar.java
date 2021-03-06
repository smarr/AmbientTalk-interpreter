/**
 * AmbientTalk/2 Project
 * NATAbstractGrammar.java created on 26-jul-2006 at 11:57:00
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
package edu.vub.at.objects.natives.grammar;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XSerializationError;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.natives.NATByCopy;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.parser.SourceLocation;
import edu.vub.util.TempFieldGenerator;

/**
 * @author tvcutsem
 *
 * NATAbstractGrammar is the common superclass of all native ambienttalk objects
 * that represent abstract grammar parse tree elements. That is, any object that
 * can be returned as part of the AST produced by the native parser.
 */
public abstract class NATAbstractGrammar extends NATByCopy implements ATAbstractGrammar {
	
	// subclasses of NATAbstractGrammar will override meta_eval and meta_quote as appropriate,
	// except for the literal grammar elements which can inherit the self-evaluating behaviour of NATNil.
	
    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._ABSTRACTGRAMMAR_, NativeTypeTags._ISOLATE_);
    }
    
    // AST nodes, like AmbientTalk objects, should have an assignable location,
    // as long as the AST nodes are unique
    
    private SourceLocation loc_;
    public SourceLocation impl_getLocation() { return loc_; }
    public void impl_setLocation(SourceLocation loc) {
    	// overriding the source location of an AG element
    	// is probably the sign of a bug: locations should be single-assignment
    	// to prevent mutable shared-state. That is, loc_ is effectively 'final'
    	if (loc_ == null) {
        	loc_ = loc;
    	} else {
    		throw new RuntimeException("Trying to override source location of "+this.toString()+" from "+loc_+" to "+loc);
    	}
    }
    
    // "normal" serialization results in the quoted value
    // for unquoted values use impl_asUnquotedCode
    public NATText impl_asCode(TempFieldGenerator objectMap) throws InterpreterException {
    	NATText code = this.impl_asUnquotedCode(objectMap);
    	return NATText.atValue("`" + code.javaValue);
    }
    
    // should be overridden in the subclasses
    public NATText impl_asUnquotedCode(TempFieldGenerator objectMap) throws InterpreterException {
    	throw new XSerializationError("Unable to serialize object: " + this.meta_print().javaValue + " (type: " + this.getClass() + ")");
    }
    
    public boolean isNativeAbstractGrammar() { return true; }
    
}
