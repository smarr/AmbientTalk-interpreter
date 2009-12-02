/**
 * AmbientTalk/2 Project
 * AGDefExternalMethod.java created on 15-nov-2006 at 19:33:18
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

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIllegalOperation;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATDefExternalMethod;
import edu.vub.at.objects.grammar.ATDefinition;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATClosureMethod;
import edu.vub.at.objects.natives.NATMethod;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

import java.util.HashSet;
import java.util.Set;

/**
 * The native implementation of an external method definition abstract grammar element.
 * 
 * @author tvcutsem
 */
public final class AGDefExternalMethod extends AGDefinition implements ATDefExternalMethod {

	private final ATSymbol		rcvNam_;
	private final ATSymbol		selectorExp_;
	private final ATTable		argumentExps_;
	private final ATBegin		bodyStmts_;
	private final ATExpression	annotationExps_;

	private ATMethod preprocessedMethod_;
	
	public AGDefExternalMethod(ATSymbol rcv, ATSymbol sel, ATTable args, ATBegin bdy, ATExpression ann)
	       throws InterpreterException {
		rcvNam_ = rcv;
		selectorExp_ = sel;
		argumentExps_ = args;
		bodyStmts_ = bdy;
		annotationExps_ = ann;
	}

	public ATSymbol base_receiver() {
		return rcvNam_;
	}
	
	public ATSymbol base_selector() {
		return selectorExp_;
	}

	public ATTable base_arguments() {
		return argumentExps_;
	}

	public ATBegin base_bodyExpression() {
		return bodyStmts_;
	}
	
	public ATExpression base_annotationExpression() {
		return annotationExps_;
	}
	
	/**
	 * Evaluates the receiver symbol to an object to which a new 'closure method' will be added.
	 * Such a closure captures the current lexical scope, but not the values for 'self' and 'super'.
	 * 
	 * The return value of an external method definition is always the external method itself.
	 * 
	 * AGDEFEXTMTH(rcv,nam,par,bdy).eval(ctx) =
	 *   rcv.eval(ctx).defineField(nam, NATCLOMTH(ctx.cur,nam,par,bdy))
	 *   
	 * @throws XIllegalOperation if the receiver is an instance of a native 
	 * type (whose method dictionaries are sealed) or if the receiver is an isolate. 
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		// the method is not yet created in the constructor because this gives problems
		// with quoted parameters: a quoted parameter would result in an illegal parameter
		// exception while actually the external method was defined in the context of a quotation,
		// so at runtime the external definition would have never been evaluated (but quoted instead)
		if (preprocessedMethod_ == null) {
			ATObject oneOrMoreAnnotation = annotationExps_.meta_eval(ctx);
			ATTable  annotationTable;
			
			if(oneOrMoreAnnotation.isTable()) {
				annotationTable = oneOrMoreAnnotation.asTable();
			} else {
				annotationTable = NATTable.of(oneOrMoreAnnotation);
			}
			
			NATMethod preprocessedMethod = new NATMethod(selectorExp_, argumentExps_, bodyStmts_, annotationTable);
			preprocessedMethod.impl_setLocation(this.impl_getLocation());
			preprocessedMethod_ = preprocessedMethod;

			ATObject[] annotations = annotationTable.asNativeTable().elements_;
			
			for (int i = 0; i < annotations.length; i++) {
				ATTypeTag theAnnotation = annotations[i].asTypeTag();
				
				preprocessedMethod_ = theAnnotation.base_annotateMethod(preprocessedMethod_);
			}
		}
		
		ATObject receiver = rcvNam_.meta_eval(ctx);
		if (receiver.meta_isTaggedAs(NativeTypeTags._ISOLATE_).asNativeBoolean().javaValue) {
			throw new XIllegalOperation("Cannot define external methods on isolates");
			
		} else {
			NATClosureMethod extMethod = new NATClosureMethod(ctx.base_lexicalScope(),
                    preprocessedMethod_);

			receiver.meta_addMethod(extMethod);
			return extMethod;
		}
	}

	/**
	 * Quoting an external method definition results in a new quoted external method definition.
	 * 
	 * AGDEFEXTMTH(rcv,nam,par,bdy).quote(ctx) = AGDEFEXTMTH(rcv.quote(ctx),nam.quote(ctx), par.quote(ctx), bdy.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGDefExternalMethod(rcvNam_.meta_quote(ctx).asSymbol(),
				               selectorExp_.meta_quote(ctx).asSymbol(),
				               argumentExps_.meta_quote(ctx).asTable(),
				               bodyStmts_.meta_quote(ctx).asBegin(),
				               annotationExps_.meta_quote(ctx).asExpression());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("def " +
				rcvNam_.meta_print().javaValue + "." +
				selectorExp_.meta_print().javaValue +
				Evaluator.printAsList(argumentExps_).javaValue +
				" { " + bodyStmts_.meta_print().javaValue + " }");
	}

	/**
	 * IV(def o.m(args) @ anns { body }) = { }
	 */
	public Set impl_introducedVariables() throws InterpreterException {
		return new HashSet();
	}

	/**
	 * FV(def o.m(args) @ anns { body }) =
	 *   { o } U FV(anns) U FV(optionalArgExps) U (FV(body) \ { args }) 
	 */
	public Set impl_freeVariables() throws InterpreterException {
		Set fvBody = bodyStmts_.impl_freeVariables();
		Evaluator.processFreeVariables(fvBody, argumentExps_);
		fvBody.add(rcvNam_);
		fvBody.addAll(annotationExps_.impl_freeVariables());
		return fvBody;
	}
	
	public Set impl_quotedFreeVariables() throws InterpreterException {
		Set qfv = argumentExps_.impl_quotedFreeVariables();
		qfv.addAll(bodyStmts_.impl_quotedFreeVariables());
		qfv.addAll(annotationExps_.impl_quotedFreeVariables());
		qfv.addAll(rcvNam_.impl_quotedFreeVariables());
		qfv.addAll(selectorExp_.impl_quotedFreeVariables());
		return qfv;
	}
	
}
