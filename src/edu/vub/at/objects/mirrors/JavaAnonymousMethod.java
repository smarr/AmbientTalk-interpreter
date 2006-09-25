/**
 * AmbientTalk/2 Project
 * JavaAnonymousMethod.java created on 10-aug-2006 at 10:03:55
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
package edu.vub.at.objects.mirrors;

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XIllegalApplication;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSplice;
import edu.vub.at.objects.natives.grammar.AGSymbol;

/**
 * @author tvc
 *
 * A JavaAnonymousMethod represents the meta_apply method of an anonymous JavaClosure subclass.
 * 
 * An anonymous native method has the following properties:
 *   - name = "nativelambda" (to distinguish it from 'ordinary' lambdas)
 *   - arguments = [ \@args ] (it takes an arbitrary number of arguments)
 *   - body = "Native implementation in {class}" (a string telling an inspector that
 *     this closure is natively implemented in the given Java class)
 *   - applying a nativelambda directly (without going through this JavaClosure)
 *     results in an error
 */
public final class JavaAnonymousMethod extends NATNil implements ATMethod {

	/**
	 * The name of an anonymous native lambda is always 'nativelambda'
	 */
	private static final AGSymbol _ANON_MTH_NAM_ = AGSymbol.alloc("nativelambda");
	
	/**
	 * The argument list of an anonymous native lambda is [ \@args ]
	 */
	private static final NATTable _ANON_MTH_ARGS_ =
		new NATTable(new ATObject[] { new AGSplice(AGSymbol.alloc("args")) });
		
	private final Class creatorClass_;
	
	/**
	 * @param creatorClass class of the object that has created the JavaClosure that will wrap <tt>this</tt>
	 */
	public JavaAnonymousMethod(Class creatorClass) {
		creatorClass_ = creatorClass;
	}

	/**
	 * It is an error to directly apply an anonymous method. The closure must be applied instead.
	 * @throws XIllegalApplication because an anonymous method must be applied through its wrapping closure.
	 */
	public ATObject meta_apply(ATTable arguments, ATContext ctx) throws NATException {
		throw new XIllegalApplication("Cannot apply an anonymous native method. Apply the closure instead.", creatorClass_);
	}

	public ATSymbol getName() { return _ANON_MTH_NAM_; }

	public ATTable getArguments() { return _ANON_MTH_ARGS_; }

	public ATAbstractGrammar getBody() {
		return AGSymbol.alloc("Native anonymous implementation in " + creatorClass_.getName());
	}
	
	public NATText meta_print() throws XTypeMismatch {
		return NATText.atValue("<anonymous java method in "+creatorClass_.getName()+">");
	}

}
