/**
 * AmbientTalk/2 Project
 * NATStripe.java created on 18-feb-2007 at 15:59:20
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

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeStripes;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * The native implementation of AmbientTalk stripe objects.
 *
 * @author tvcutsem
 */
public class NATStripe extends NATByCopy implements ATStripe {

	private final ATSymbol stripeName_;
	private final ATTable parentStripes_;
	
	public static ATStripe[] toStripeArray(ATTable stripes) throws InterpreterException {
		if (stripes == NATTable.EMPTY) {
			return NATObject._NO_STRIPES_;
		}
		ATObject[] unwrapped = stripes.asNativeTable().elements_;
		ATStripe[] unwrappedStripes = new ATStripe[unwrapped.length];
		for (int i = 0; i < unwrappedStripes.length; i++) {
			unwrappedStripes[i] = unwrapped[i].base_asStripe();
		}
		return unwrappedStripes;
	}
	
	public static NATStripe atValue(String stripeName) {
		return atValue(AGSymbol.jAlloc(stripeName));
	}
	
	public static NATStripe atValue(ATSymbol stripeName) {
		return new NATStripe(stripeName,
				             NATTable.atValue(new ATObject[] { OBJRootStripe._INSTANCE_ }));
	}
	
	public static NATStripe atValue(String stripeName, NATStripe singleParent) {
		return new NATStripe(AGSymbol.jAlloc(stripeName),
				             NATTable.atValue(new ATObject[] { singleParent }));
	}
	
	/**
	 * Stripes should not be created directly because it should be verified
	 * that their list of parent stripes is never empty. Stripes created
	 * with an empty parent list automatically get assigned the root stripe
	 * as their single parent.
	 */
	public static NATStripe atValue(ATSymbol stripeName, ATTable parentStripes) {
		if (parentStripes == NATTable.EMPTY) {
			return new NATStripe(stripeName, NATTable.atValue(new ATObject[] { OBJRootStripe._INSTANCE_ }));
		} else {
			return new NATStripe(stripeName, parentStripes);
		}
	}
	
	/**
	 * The constructor is declared protected such that it cannot be used externally,
	 * but can be used by the OBJRootStripe class to create a stripe with an empty
	 * parent table, which is normally not allowed. Hence, by construction the only
	 * stripe with an empty parent table is the root stripe. 
	 */
	protected NATStripe(ATSymbol stripeName, ATTable parentStripes) {
		stripeName_ = stripeName;
		parentStripes_ = parentStripes;
	}

	public ATSymbol base_getStripeName() throws InterpreterException {
		return stripeName_;
	}

	public ATTable base_getParentStripes() throws InterpreterException {
		return parentStripes_;
	}

	/**
	 * Native implementation of:
	 * 
	 *	def isSubstripeOf(superstripe) {
	 *		  (superstripe.name() == name).or:
	 *			  { (superstripes.find: { |sstripe|
	 *				  sstripe.isSubstripeOf(superstripe) }) != nil }
	 *	};
	 */
	public ATBoolean base_isSubstripeOf(final ATStripe superstripe) throws InterpreterException {
		if (superstripe.base_getStripeName().equals(stripeName_)) {
			return NATBoolean._TRUE_;
		} else {
			ATObject found = parentStripes_.base_find_(new NativeClosure(this) {
				public ATObject base_apply(ATTable args) throws InterpreterException {
					ATStripe sstripe = get(args, 1).base_asStripe();
					return sstripe.base_isSubstripeOf(superstripe);
				}
			});
			return NATBoolean.atValue(found != NATNil._INSTANCE_);
		}
	}
	
	/**
	 * Identity of stripes is based on their name
	 */
	public boolean equals(Object other) {
		try {
			return (other instanceof NATStripe) &&
			       ((ATStripe) other).base_getStripeName().equals(stripeName_);
		} catch (InterpreterException e) {
			return false;
		}
	}
	
	public boolean base_isStripe() { return true; }
	
	public ATStripe base_asStripe() { return this; }
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<stripe:"+stripeName_+">");
	}
	
    public ATTable meta_getStripes() throws InterpreterException {
    	return NATTable.of(NativeStripes._STRIPE_);
    }
	
	/**
	 * The root stripe of the stripe hierarchy: every stripe eventually
	 * has this stripe as its parent.
	 */
	public static class OBJRootStripe extends NATStripe implements ATStripe {
		
		private final static AGSymbol _ROOT_NAME_ = AGSymbol.jAlloc("Stripe");
		
		public static final OBJRootStripe _INSTANCE_ = new OBJRootStripe();
		
		/**
		 * The root stripe is named `Stripe and has no parent stripes
		 */
		private OBJRootStripe() {
			super(_ROOT_NAME_, NATTable.EMPTY);
		}

		/**
		 * The root stripe is only a substripe of the root stripe itself
		 */
		public ATBoolean base_isSubstripeOf(ATStripe superstripe) throws InterpreterException {
			return NATBoolean.atValue(superstripe.base_getStripeName().equals(_ROOT_NAME_));
		}
		
	}
	
}
