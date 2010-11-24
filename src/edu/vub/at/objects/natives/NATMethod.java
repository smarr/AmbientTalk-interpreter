/**
 * AmbientTalk/2 Project
 * NATMethod.java created on Jul 24, 2006 at 11:30:35 PM
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
import edu.vub.at.eval.PartialBinder;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.PrimitiveMethod;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.at.parser.SourceLocation;
import edu.vub.at.util.logging.Logging;
import edu.vub.util.TempFieldGenerator;

/**
 * NATMethod implements methods as named functions which are in fact simply containers
 * for a name, a table of arguments and a body.
 * 
 * @author smostinc
 * @author tvcutsem
 */
public class NATMethod extends NATByCopy implements ATMethod {

	private final ATSymbol 	name_;
	private final ATTable 	parameters_;
	private final ATBegin	body_;
	private final ATTable	annotations_;
	
	// partial function denoting a parameter binding algorithm specialized for this method's parameter list
	private final PartialBinder parameterBindingFunction_;
	
	/** construct a new method. This method may raise an exception if the parameter list is illegal. */
	public NATMethod(ATSymbol name, ATTable parameters, ATBegin body, ATTable annotations) throws InterpreterException {
		name_ 		= name;
		parameters_ = parameters;
		body_ 		= body;
		annotations_= annotations;
		
		// calculate the parameter binding strategy to use using partial evaluation
		parameterBindingFunction_ =
			PartialBinder.calculateResidual(name_.base_text().asNativeText().javaValue, parameters);
	}
	
	/**
	 * Constructor to be used by primitive methods only.
	 */
	protected NATMethod(ATSymbol name, ATTable parameters, PrimitiveMethod.PrimitiveBody body, ATTable annotations) {
		name_ 		= name;
		parameters_ = parameters;
		body_ 		= body;
		annotations_= annotations;
		
		PartialBinder parameterBindingFunction;
		try {
			// calculate the parameter binding strategy to use using partial evaluation
			parameterBindingFunction = PartialBinder.calculateResidual(name_.base_text().asNativeText().javaValue, parameters);
		} catch (InterpreterException e) {
			parameterBindingFunction = null;
			// this indicates a bug, primitive methods should not contain erroneous parameter lists
			Logging.VirtualMachine_LOG.fatal("error creating primitive method: ",e);
		}
		parameterBindingFunction_ = parameterBindingFunction;
	}

	public ATClosure base_wrap(ATObject lexicalScope, ATObject dynamicReceiver) throws InterpreterException {
		NATClosure clo = new NATClosure(this, lexicalScope, dynamicReceiver);
		// make the closure inherit its source location from this method
		clo.impl_setLocation(this.impl_getLocation());
		return clo;
	}
	
	public ATSymbol base_name() {
		return name_;
	}

	public ATTable base_parameters() {
		return parameters_;
	}

	public ATBegin base_bodyExpression() {
		return body_;
	}
	
	public ATTable base_annotations() throws InterpreterException {
		return annotations_;
	}
	
	/**
	 * To apply a function, first bind its parameters to the evaluated arguments within a new call frame.
	 * This call frame is lexically nested within the current lexical scope.
	 * 
	 * This method is invoked via the following paths:
	 *  - either by directly 'calling a function', in which case this method is applied via NATClosure.base_apply.
	 *    The closure ensures that the context used is the lexical scope, not the dynamic scope of invocation.
	 *  - or by 'invoking a method' through an object, in which case this method is applied via NATObject.meta_invoke.
	 *    The enclosing object ensures that the context is properly initialized with the implementor, the dynamic receiver
	 *    and the implementor's parent.
	 * 
	 * @param arguments the evaluated actual arguments
	 * @param ctx the context in which to evaluate the method body, where a call frame will be inserted first
	 * @return the value of evaluating the function body
	 */
	public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
		NATCallframe cf = new NATCallframe(ctx.base_lexicalScope());
		ATContext evalCtx = ctx.base_withLexicalEnvironment(cf);
		PartialBinder.defineParamsForArgs(parameterBindingFunction_, evalCtx, arguments);
		return body_.meta_eval(evalCtx);
	}
	
	/**
	 * Applies the method in the context given, without first inserting a call frame to bind parameters.
	 * Arguments are bound directly in the given lexical scope.
	 * 
	 * This method is often invoked via its enclosing closure when used to implement various language
	 * constructs such as object:, mirror:, extend:with: etc.
	 * 
	 * @param arguments the evaluated actual arguments
	 * @param ctx the context in which to evaluate the method body, to be used as-is
	 * @return the value of evaluating the function body
	 */
	public ATObject base_applyInScope(ATTable arguments, ATContext ctx) throws InterpreterException {
		PartialBinder.defineParamsForArgs(parameterBindingFunction_, ctx, arguments);
		return body_.meta_eval(ctx);
	}

	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("<method:"+name_.meta_print().javaValue+">");
	}
	
	public NATText impl_asCode(TempFieldGenerator objectMap) throws InterpreterException {
		if(objectMap.contains(this)) {
			return objectMap.getName(this);
		}
		StringBuffer out = new StringBuffer("");
		out.append("def "+ name_.toString());
		out.append(Evaluator.codeAsList(objectMap, parameters_.asNativeTable()).javaValue);
		if(annotations_.base_length().asNativeNumber().javaValue > 0)
			out.append("@"+annotations_.impl_asCode(objectMap).javaValue);
		out.append("{" + body_.meta_print().javaValue + "}");
		return NATText.atValue(out.toString());
	}
	
	public NATText impl_asCode(TempFieldGenerator objectMap, boolean asClosure) throws InterpreterException {
		if(objectMap.contains(this)) {
			return objectMap.getName(this);
		}
		
		if(asClosure) {
			StringBuffer out = new StringBuffer("");
			out.append("{");
			out.append(Evaluator.codeParameterList(objectMap, parameters_.asNativeTable()).javaValue);
			out.append(body_.meta_print().javaValue + "}");
			return NATText.atValue(out.toString());
		} else {
			return this.impl_asCode(objectMap);
		}
	}
	
	public ATObject meta_clone() throws InterpreterException {
		return this;
	}

	public ATMethod asMethod() throws XTypeMismatch {
		return this;
	}
	
    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.atValue(NATTable.collate(
    			new ATObject[] { NativeTypeTags._METHOD_, NativeTypeTags._ISOLATE_ },
    			annotations_.asNativeTable().elements_));
    }
    
	// Debugging API:
	
    private SourceLocation loc_;
    public SourceLocation impl_getLocation() { return loc_; }
    public void impl_setLocation(SourceLocation loc) {
    	// overriding the source location of an AmbientTalk object
    	// is probably the sign of a bug: locations should be single-assignment
    	// to prevent mutable shared-state. That is, loc_ is effectively 'final'
    	if (loc_ == null) {
        	loc_ = loc;  		
    	} else {
    		throw new RuntimeException("Trying to override source location of "+this.toString()+" from "+loc_+" to "+loc);
    	}
    }

}
