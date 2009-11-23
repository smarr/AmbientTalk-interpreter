/**
 * AmbientTalk/2 Project
 * AGDefFunction.java created on 26-jul-2006 at 15:44:08
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
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATDefMethod;
import edu.vub.at.objects.grammar.ATDefinition;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATClosure;
import edu.vub.at.objects.natives.NATMethod;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

import java.util.HashSet;
import java.util.Set;

/**
 * The native implementation of a function definition abstract grammar element.
 * This AG element covers both method and closure definition.
 * 
 * @author tvc
 */
public final class AGDefFunction extends AGDefinition implements ATDefMethod {

	private final ATSymbol selectorExp_;
	private final ATTable argumentExps_;
	private final ATBegin bodyStmts_;
	private final ATExpression	annotationExps_;

	private ATMethod preprocessedMethod_;
	
	public AGDefFunction(ATSymbol sel, ATTable args, ATBegin bdy, ATExpression ann) 
			throws InterpreterException {
		selectorExp_ = sel;
		argumentExps_ = args;
		bodyStmts_ = bdy;
		annotationExps_ = ann;
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
	 * Defines a new function (method or closure) in the current scope.
	 * If a function definition is executed in the context of an object,
	 * a new method is created. If, however, the function definition
	 * is executed in the context of a call frame (i.e. its definition
	 * is nested within a method/function) then a closure is created instead.
	 * 
	 * The return value of a function definition is always a closure encapsulating the function.
	 * 
	 * AGDEFFUN(nam,par,bdy).eval(ctx) =
	 *   if ctx.scope.isCallFrame
	 *      ctx.scope.defineField(nam, AGCLO(AGMTH(nam,par,bdy), ctx))
	 *   else
	 *      ctx.scope.addMethod(nam, AGMTH(nam,par,bdy))
	 * 
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		// the method is not yet created in the constructor because this gives problems
		// with quoted parameters: a quoted parameter would result in an illegal parameter
		// exception while actually the function was defined in the context of a quotation,
		// so at runtime the function definition would have never been evaluated (but quoted instead)
		if (preprocessedMethod_ == null) {
			ATObject oneOrMoreAnnotation = annotationExps_.meta_eval(ctx);
			ATTable  annotationTable;
			
			if(oneOrMoreAnnotation.isTable()) {
				annotationTable = oneOrMoreAnnotation.asTable();
			} else {
				annotationTable = NATTable.of(oneOrMoreAnnotation);
			}
			
			preprocessedMethod_ = new NATMethod(selectorExp_, argumentExps_, bodyStmts_, annotationTable);
			
			ATObject[] annotations = annotationTable.asNativeTable().elements_;
			
			for (int i = 0; i < annotations.length; i++) {
				ATTypeTag theAnnotation = annotations[i].asTypeTag();
				
				preprocessedMethod_ = theAnnotation.base_annotateMethod(preprocessedMethod_);
			}
		}
		ATObject current = ctx.base_lexicalScope();
		if (current.isCallFrame()) {
			NATClosure clo = new NATClosure(preprocessedMethod_, ctx);
			current.meta_defineField(selectorExp_, clo);
			return clo;
		} else {
			current.meta_addMethod(preprocessedMethod_);
			return current.meta_select(current, selectorExp_);
		}
	}

	/**
	 * Quoting a function definition results in a new quoted function definition.
	 * 
	 * AGDEFFUN(nam,par,bdy).quote(ctx) = AGDEFFUN(nam.quote(ctx), par.quote(ctx), bdy.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGDefFunction(selectorExp_.meta_quote(ctx).asSymbol(),
				                 argumentExps_.meta_quote(ctx).asTable(),
				                 bodyStmts_.meta_quote(ctx).asBegin(),
				                 annotationExps_.meta_quote(ctx).asExpression());
	}
	
	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("def " +
				selectorExp_.meta_print().javaValue +
				Evaluator.printAsList(argumentExps_).javaValue +
				" { " + bodyStmts_.meta_print().javaValue + " }");
	}
	
    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._METHOD_DEFINITION_, NativeTypeTags._ISOLATE_);
    }
    
	/**
	 * IV(def f(args) @ annotations { body }) = { f }
	 */
	public Set impl_introducedVariables() throws InterpreterException {
		Set singleton = new HashSet();
		singleton.add(selectorExp_);
		return singleton;
	}

	/**
	 * FV(def f(args) @ annotations { body })
	 *   = FV(annotations) U FV(optionalArgExps) U (FV(body) \ { args } \ { f })
	 */
	public Set impl_freeVariables() throws InterpreterException {
		Set fvBody = bodyStmts_.impl_freeVariables();
		fvBody.remove(selectorExp_);
		Evaluator.processFreeVariables(fvBody, argumentExps_);
		fvBody.addAll(annotationExps_.impl_freeVariables());
		return fvBody;
	}
	
	
	public Set impl_quotedFreeVariables() throws InterpreterException {
		Set qfv = selectorExp_.impl_quotedFreeVariables();
		qfv.addAll(argumentExps_.impl_quotedFreeVariables());
		qfv.addAll(bodyStmts_.impl_quotedFreeVariables());
		qfv.addAll(annotationExps_.impl_quotedFreeVariables());
		return qfv;
	}

}
