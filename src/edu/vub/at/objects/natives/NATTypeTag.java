/**
 * AmbientTalk/2 Project
 * NATTypeTag.java created on 18-feb-2007 at 15:59:20
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

import edu.vub.at.actors.natives.DiscoveryManager;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * The native implementation of AmbientTalk type tag objects.
 *
 * In principle, care should be taken that all objects implementing the
 * type tag interface are isolates, because type tags are usually attributed
 * to messages which are isolates themselves.
 *
 * @author tvcutsem
 */
public class NATTypeTag extends NATByCopy implements ATTypeTag {

	private final ATSymbol typeName_;
	private final ATTable parentTypes_;
	
	public static ATTypeTag[] toTypeTagArray(ATTable types) throws InterpreterException {
		if (types == NATTable.EMPTY) {
			return NATObject._NO_TYPETAGS_;
		}
		ATObject[] unwrapped = types.asNativeTable().elements_;
		ATTypeTag[] unwrappedTypes = new ATTypeTag[unwrapped.length];
		for (int i = 0; i < unwrappedTypes.length; i++) {
			unwrappedTypes[i] = unwrapped[i].asTypeTag();
		}
		return unwrappedTypes;
	}
	
	public static NATTypeTag atValue(String typeName) {
		return atValue(AGSymbol.jAlloc(typeName));
	}
	
	public static NATTypeTag atValue(ATSymbol typeName) {
		return new NATTypeTag(typeName,
				             NATTable.atValue(new ATObject[] { OBJRootType._INSTANCE_ }));
	}
	
	public static NATTypeTag atValue(String typeName, NATTypeTag singleParent) {
		return new NATTypeTag(AGSymbol.jAlloc(typeName),
				             NATTable.atValue(new ATObject[] { singleParent }));
	}
	
	/**
	 * Types should not be created directly because it should be verified
	 * that their list of parent types is never empty. Types created
	 * with an empty parent list automatically get assigned the root type
	 * as their single parent.
	 */
	public static NATTypeTag atValue(ATSymbol typeName, ATTable parentTypes) {
		if (parentTypes == NATTable.EMPTY) {
			return new NATTypeTag(typeName, NATTable.atValue(new ATObject[] { OBJRootType._INSTANCE_ }));
		} else {
			return new NATTypeTag(typeName, parentTypes);
		}
	}
	
	/**
	 * The constructor is declared protected such that it cannot be used externally,
	 * but can be used by the OBJRootType class to create a type with an empty
	 * parent table, which is normally not allowed. Hence, by construction the only
	 * type with an empty parent table is the root type. 
	 */
	protected NATTypeTag(ATSymbol typeName, ATTable parentTypes) {
		typeName_ = typeName;
		parentTypes_ = parentTypes;
	}

	public ATSymbol base_typeName() throws InterpreterException {
		return typeName_;
	}

	public ATTable base_superTypes() throws InterpreterException {
		return parentTypes_;
	}

	/**
	 * Native implementation of:
	 * 
	 *	def isSubtypeOf(supertype) {
	 *		  (supertype.name() == name).or:
	 *			  { (supertypes.find: { |type|
	 *				  type.isSubtypeOf(supertype) }) != nil }
	 *	};
	 */
	public ATBoolean base_isSubtypeOf(final ATTypeTag supertype) throws InterpreterException {
		if (supertype.base_typeName().equals(typeName_)) {
			return NATBoolean._TRUE_;
		} else {
			ATObject found = parentTypes_.base_find_(new NativeClosure(this) {
				public ATObject base_apply(ATTable args) throws InterpreterException {
					ATTypeTag type = get(args, 1).asTypeTag();
					return type.base_isSubtypeOf(supertype);
				}
			});
			return NATBoolean.atValue(found != Evaluator.getNil());
		}
	}
	
	/**
	 * By default, annotateMessage is the identity function, it does not add any new metadata
	 * to the message.
	 */
	public ATObject base_annotateMessage(ATObject originalMessage) throws InterpreterException {
		return originalMessage;
	}
	
	/**
	 * By default, annotateMethod is the identity function, it does not add any new metadata
	 * to the method.
	 */
	public ATMethod base_annotateMethod(ATMethod originalMethod) throws InterpreterException {
		return originalMethod;
	}

	
	/**
	 * Identity of types is based on their name
	 */
    public ATBoolean base__opeql__opeql_(ATObject comparand) throws InterpreterException {
    	if (comparand.isTypeTag()) {
    		return NATBoolean.atValue(comparand.asTypeTag().base_typeName().equals(typeName_));
    	} else {
    		return NATBoolean._FALSE_;
    	}
    }
	
	public boolean isTypeTag() { return true; }
	
	public ATTypeTag asTypeTag() { return this; }
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<type tag:"+typeName_+">");
	}
	
	/**
	 * Types are singletons
	 */
	public ATObject meta_clone() throws InterpreterException {
		return this;
	}
	
    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._TYPETAG_, NativeTypeTags._ISOLATE_);
    }
	
    /** required as type tags are stored in a hashset in the {@link DiscoveryManager} */
    public int hashCode() { return typeName_.hashCode(); }
    
	/**
	 * The root type of the type hierarchy: every type eventually
	 * has this type as its parent.
	 */
	public static class OBJRootType extends NATTypeTag implements ATTypeTag {
		
		private final static AGSymbol _ROOT_NAME_ = AGSymbol.jAlloc("Type");
		
		public static final OBJRootType _INSTANCE_ = new OBJRootType();
		
		/**
		 * The root type is named `Type and has no parent types
		 */
		private OBJRootType() {
			super(_ROOT_NAME_, NATTable.EMPTY);
		}

		/**
		 * The root type is only a subtype of the root type itself
		 */
		public ATBoolean base_isSubtypeOf(ATTypeTag supertype) throws InterpreterException {
			return NATBoolean.atValue(supertype.base_typeName().equals(_ROOT_NAME_));
		}
		
	}
	
}
