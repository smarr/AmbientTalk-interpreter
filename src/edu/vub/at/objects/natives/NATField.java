/**
 * AmbientTalk/2 Project
 * NATField.java created on Jul 27, 2006 at 2:27:53 AM
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

import java.util.HashMap;

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.PrimitiveMethod;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.util.TempFieldGenerator;

/**
 * NATField implements a causally connected field of an object. Rather than storing
 * these fields directly in every object, we choose to only make people pay when they
 * choose to reify them. 
 * 
 * @author smostinc, tvcutsem
 */
public class NATField extends NATByRef implements ATField {

	private final ATSymbol name_;
	private final ATObject host_;
	
	/**
	 * Constructs a new native field object linked to the field of a base-level
	 * AmbientTalk object
	 */
	public NATField(ATSymbol name, ATObject host) {
		name_ = name;
		host_ = host;
	}

	public ATSymbol base_name() {
		return name_;
	}

	public ATObject base_readField() throws InterpreterException {
		return host_.meta_invokeField(host_, name_);
	}

	public ATObject base_writeField(ATObject newValue) throws InterpreterException {
		return host_.impl_invoke(host_, name_.asAssignmentSymbol(), NATTable.of(newValue));
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<field:"+name_.meta_print().javaValue+">");
	}
	
	public NATText impl_asCode(TempFieldGenerator objectMap) throws InterpreterException {
		return this.impl_asCode(objectMap, false);
	}
	
	public NATText impl_asCode(TempFieldGenerator objectMap, boolean isIsolate) throws InterpreterException {
		if(objectMap.contains(this)) {
			return objectMap.getName(this);
		}
		String def = "";
		String nameAsCode = name_.toString();
		if(nameAsCode != "super")
			def += "def ";
		//if(isIsolate) // why was this necessary?
		//	def += "self.";
		return NATText.atValue(def + nameAsCode + " := " + base_readField().impl_asCode(objectMap).javaValue);
	}
	
    public boolean isNativeField() {
        return true;
    }

	public ATField asField() throws XTypeMismatch {
		return this;
	}
	
	/**
	 * Fields can be re-initialized when installed in an object that is being cloned.
	 * They expect the new owner of the field as the sole instance to their 'new' method
	 */
	public ATObject meta_newInstance(ATTable initargs) throws InterpreterException {
		if (initargs.base_length() != NATNumber.ONE) {
			return super.meta_newInstance(initargs);
		} else {
			ATObject newhost = initargs.base_at(NATNumber.ONE);
			return new NATField(name_, (NATCallframe) newhost);
		}
	}
	
    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._FIELD_);
    }
    
    public ATMethod base_accessor() throws InterpreterException {
    	return accessorForField(this);
    }
    
    public ATMethod base_mutator() throws InterpreterException {
    	return mutatorForField(this);
    }
    
    public static ATMethod accessorForField(final ATField f) throws InterpreterException {
    	return new PrimitiveMethod(f.base_name(), NATTable.EMPTY, new PrimitiveMethod.PrimitiveBody() {
    		public ATObject meta_eval(ATContext ctx) throws InterpreterException {
    			return f.base_readField();
    		}
    		public NATText meta_print() throws InterpreterException {
    			return NATText.atValue(f.base_readField().toString());
    		}
    		public NATText impl_asCode(TempFieldGenerator objectMap) throws InterpreterException {
    			return f.base_readField().impl_asCode(objectMap);
    		}
    	}) {
    		public NATText meta_print() throws InterpreterException {
    			return NATText.atValue("<accessor method for:"+f.base_name()+">");
    		}
    		public NATText impl_asCode(TempFieldGenerator objectMap) throws InterpreterException {
    			return NATText.atValue("def "
    					+ f.base_name().impl_asCode(objectMap).javaValue 
    					+ "(){"+f.base_name().impl_asCode(objectMap).javaValue
    					+ "}");
    		}
    	};
    }
    
    public static ATMethod mutatorForField(final ATField f) throws InterpreterException {
    	return new PrimitiveMethod(f.base_name().asAssignmentSymbol(), NATTable.of(AGSymbol.jAlloc("v")), new PrimitiveMethod.PrimitiveBody() {
    		public ATObject meta_eval(ATContext ctx) throws InterpreterException {
    			return f.base_writeField(ctx.base_lexicalScope().impl_callField(AGSymbol.jAlloc("v")));
    		}
    		public NATText meta_print() throws InterpreterException {
    			return NATText.atValue(""+f.base_name()+" := v");
    		}
    		public NATText impl_asCode(TempFieldGenerator objectMap) throws InterpreterException {
    			return NATText.atValue(""+f.base_name()+" := v");
    		}
    	}) {
    		public NATText meta_print() throws InterpreterException {
    			return NATText.atValue("<mutator method for:"+f.base_name()+">");
    		}
    		public NATText impl_asCode(TempFieldGenerator objectMap) throws InterpreterException {
    			return NATText.atValue("def "
    					+ f.base_name().impl_asCode(objectMap).javaValue
    					+ ".:=(v){"
    					+ f.base_name().impl_asCode(objectMap).javaValue
    					+ ":=v}");
    		}
    	};
    }

}
