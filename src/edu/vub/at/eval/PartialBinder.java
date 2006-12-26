/**
 * AmbientTalk/2 Project
 * PartialBinder.java created on 26-dec-2006 at 17:25:12
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
package edu.vub.at.eval;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XIllegalParameter;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATAssignVariable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATTable;

/**
 * Instances of the class PartialBinder represent 'partial functions' whose task it is
 * to bind the formal parameters of a function to the actual arguments upon function application.
 * 
 * The binding process of formals to actuals is an ideal candidate for partial evaluation, because
 * the binding algorithm depends mainly on two parameters: the formal parameter list (which is known
 * at function definition time, and does not change) and the actual argument list (provided at
 * call time). At function definition time, one may partially evaluate the binding algorithm
 * according to the form of the formal parameter list. The resulting 'residual function' is an
 * instance of this class and can subsequently be used to bind (define or assign) actual arguments
 * at runtime, saving out a number of checks which would have otherwise been performed at every
 * function invocation.
 * 
 * The general form of a function's formal parameter list is:
 * (mandatory arguments: ATSymbol)* (optional arguments: ATAssignVariable)* (rest argument: ATSplice)?
 * 
 * Given such a formal parameter list, we define 8 different kinds of partial functions, each
 * specialized for the absence of one of the three different kinds of parameters: (the numbers behind
 * the partial function's name denote the number of mandatory, optional and rest args)
 * 
 * - ZeroArity           (0 0 0) example: f()
 * - Mandatory           (n 0 0) example: f(a,b)
 * - MandatoryOptional   (n m 0) example: f(a,b,c:=1)
 * - Optional            (0 m 0) example: f(a:=1,b:=2)
 * - VariableArity       (0 0 1) example: f(@rest)
 * - MandatoryVariable   (n 0 1) example: f(a,b,@rest)
 * - OptionalVariable    (0 m 1) example: f(a:=1,@rest)
 * - Generic             (n m 1) example: f(a,b:=1,@rest)
 * 
 * Note also that the partial evaluation of the binding algorithm at function definition time
 * allows the signalling of illegal parameter lists (e.g. when optional arguments are followed
 * by mandatory arguments) early, rather than latently detecting such illegal parameter lists
 * at method invocation time.
 *
 * @author tvcutsem
 */
public abstract class PartialBinder {
	
	/**
	 * Bind the given actual arguments to the formal parameters encapsulated by this partial bind function.
	 * @param arguments the actual arguments to the function, supplied at function application time
	 * @param inContext the context in which to bind the formal parameters and in which to evaluate default optional parameter expressions
	 * @param binder a closure which determines whether to define or assign the formals in the scope
	 */
	protected abstract void bind(ATObject[] arguments, ATContext inContext, BindClosure binder) throws InterpreterException;
	
	// auxiliary interface to support functor objects
	private interface BindClosure {
		public void bindParamToArg(ATObject inScope, ATSymbol param, ATObject arg) throws InterpreterException;
	}
	
	/**
	 * Bind all of the given parameters as newly defined slots in the given scope to the given arguments.
	 * The scope is defined as the lexical scope of the given context.
	 */
	public static final void defineParamsForArgs(PartialBinder residual, ATContext context, ATTable arguments) throws InterpreterException {
		residual.bind(arguments.asNativeTable().elements_, context, new BindClosure() {
			public void bindParamToArg(ATObject scope, ATSymbol param, ATObject arg) throws InterpreterException {
				scope.meta_defineField(param, arg);
			}
		});
	}
	
	/**
	 * Assign all of the formal parameter names in the scope object to the given arguments
	 * The scope is defined as the lexical scope of the given context.
	 */
	public static final void assignArgsToParams(PartialBinder residual, ATContext context, ATTable arguments) throws InterpreterException {
		residual.bind(arguments.asNativeTable().elements_, context, new BindClosure() {
			public void bindParamToArg(ATObject scope, ATSymbol param, ATObject arg) throws InterpreterException {
				scope.meta_assignVariable(param, arg);
			}
		});
	}
	
	/**
	 * Performs the partial evaluation of the binding algorithm given the formal parameters.
	 * @param forFunction the name of the function for which the parameter list is partially evaluated, for debugging purposes.
	 * @param parameters the formal parameter list
	 * @return a partial function which, when applied using the {@link PartialBinder#bind(ATTable, ATContext, BindClosure)}
	 * method binds the formal parameters given here to the actual arguments supplied at function application time.
	 * @throws XIllegalParameter when the formal parameter list does not adhere to the language format
	 */
	public static PartialBinder calculateResidual(String forFunction, ATTable parameters) throws InterpreterException {
		if (parameters == NATTable.EMPTY) {
			return makeZeroArity(forFunction);
		}
		
		ATObject[] pars = parameters.asNativeTable().elements_;

		int numMandatoryArguments = 0;
		int numOptionalArguments = 0;
		
		int paridx = 0;
		
		// determine the number of mandatory arguments
		for (; paridx < pars.length && pars[paridx].base_isSymbol(); paridx++) {
			numMandatoryArguments++;
		}
		
		// determine the number of optional arguments
		for (; paridx < pars.length && pars[paridx].base_isVariableAssignment(); paridx++) {
			numOptionalArguments++;
		}
		
		boolean hasSplice;
		
		if (paridx != pars.length) {
			// not all formal parameters processed yet
			// this can only happen when the last parameter is a rest parameter

			if (pars[paridx].base_isSplice()) {
				hasSplice = true;
				
				// rest parameter should always be last
				if (paridx != pars.length - 1) {
					throw new XIllegalParameter(forFunction, "rest parameter " + pars[paridx] + " is not the last parameter");
				}
				
			} else {
                // optionals followed by mandatory parameter
				throw new XIllegalParameter(forFunction, "optional parameters followed by mandatory parameter " + pars[paridx]);
			}
		} else {
			// all parameters processed, there is no rest parameter
			hasSplice = false;
		}

		// decision tree for which partial function to return
		
		if (numMandatoryArguments > 0) {
			// mandatory parameters
			if (numOptionalArguments > 0) {
				// mandatory and optional parameters
				if (hasSplice) {
					// mandatory, optional and rest parameters
					return makeGeneric(forFunction, pars, numMandatoryArguments, numOptionalArguments);
				} else {
					// mandatory and optional but no rest parameters
					return makeMandatoryOptional(forFunction, pars, numMandatoryArguments, numOptionalArguments);
				}
			} else {
				// mandatory and no optional parameters
				if (hasSplice) {
					// mandatory and rest parameters, but no optional parameters
					return makeMandatoryVariable(forFunction, pars);
				} else {
					// mandatory parameters, but no optional or rest parameters
					return makeMandatory(forFunction, pars);
				}
			}
		} else {
			// no mandatory parameters
			if (numOptionalArguments > 0) {
				// no mandatory parameters but some optional parameters
				if (hasSplice) {
					// no mandatory, some optional and a rest parameter
					return makeOptionalVariable(forFunction, pars);
				} else {
					// optional parameters only
					return makeOptional(forFunction, pars);
				}
			} else {
				// no mandatory and no optional parameters
				if (hasSplice) {
					// only a rest parameter
					return makeVariableArity(forFunction, pars[paridx].base_asSplice().base_getExpression().base_asSymbol());
				} else {
					// no mandatory, no optional and no rest parameter: this can normally only happen when
					// the formal parameter list is empty, but this case is checked at the beginning
					// if we arrive here, this can only signify an illegal type of parameter
					throw new XIllegalParameter(forFunction, "unexpected formal parameter types in " + parameters);
				}
			}
		}
	}
	
	/* ============================
	 * = The 8 residual functions =
	 * ============================ */

	/**
	 * - ZeroArity           (0 0 0) example: f()
	 */
	private static final PartialBinder makeZeroArity(final String funnam) {
		return new PartialBinder() {
			protected void bind(ATObject[] arguments, ATContext inContext, BindClosure binder) throws InterpreterException {
				if (arguments == NATTable.EMPTY.elements_)
					return; // no need to bind any arguments
				else
					throw new XArityMismatch(funnam, 0, arguments.length);
			}
		};
	}
	
	/**
	 * - Mandatory           (n 0 0) example: f(a,b)
	 */
	private static final PartialBinder makeMandatory(final String funnam, final ATObject[] formals) {
		return new PartialBinder() {
			protected void bind(ATObject[] args, ATContext inContext, BindClosure binder) throws InterpreterException {
				int numMandatoryArguments = formals.length;
				// perform arity check: number of arguments must match number of parameters exactly
				if (numMandatoryArguments != args.length) {
					// error: too many or not enough actuals
					throw new XArityMismatch(funnam, numMandatoryArguments, args.length);
				}
				
				ATObject scope = inContext.base_getLexicalScope();
				
				// bind all mandatory arguments
				for (int paridx = 0; paridx < numMandatoryArguments; paridx++) {
					// bind formal to actual
					binder.bindParamToArg(scope, formals[paridx].base_asSymbol(), args[paridx]);			
				}
			}
		};
	}
	
	/**
	 * - MandatoryOptional   (n m 0) example: f(a,b,c:=1)
	 */
	private static final PartialBinder makeMandatoryOptional(final String funnam, final ATObject[] formals,
															 final int numMandatory, final int numOptional) {
		return new PartialBinder() {
			protected void bind(ATObject[] args, ATContext inContext, BindClosure binder) throws InterpreterException {
				// perform arity check: number of arguments must at least equal number of mandatory arguments
				// and must not be greater than the total number of mandatory and optional arguments
				if (args.length < numMandatory || args.length > numMandatory + numOptional) {
					// error: not enough actuals or too many actuals
					throw new XArityMismatch(funnam, numMandatory, args.length);
				}
				
				int paridx = 0;
				ATObject scope = inContext.base_getLexicalScope();
				
				// bind all mandatory arguments
				for (; paridx < numMandatory; paridx++) {
					// bind formal to actual
					binder.bindParamToArg(scope, formals[paridx].base_asSymbol(), args[paridx]);			
				}
				
				// bind all optional arguments
				for (; paridx < numMandatory + numOptional; paridx++) {
					if (paridx < args.length) {
						// bind formal to actual and ignore default initialization expression
						binder.bindParamToArg(scope, formals[paridx].base_asVariableAssignment().base_getName(), args[paridx]);	
					} else {
						// no more actuals: bind optional parameter to default initialization expression
						ATAssignVariable param = formals[paridx].base_asVariableAssignment();
						binder.bindParamToArg(scope, param.base_getName(), param.base_getValueExpression().meta_eval(inContext));
					}
				}
			}
		};
	}
	
	/**
	 * - Optional            (0 m 0) example: f(a:=1,b:=2)
	 */
	private static final PartialBinder makeOptional(final String funnam, final ATObject[] formals) {
		return new PartialBinder() {
			protected void bind(ATObject[] args, ATContext inContext, BindClosure binder) throws InterpreterException {
				int numOptional = formals.length;
				
				// perform arity check: number of arguments must not exceed number of optional arguments
				if (args.length > numOptional) {
					// error: too many actuals
					throw new XArityMismatch(funnam, numOptional, args.length);
				}
				
				ATObject scope = inContext.base_getLexicalScope();
				
				// bind all optional arguments
				for (int paridx = 0; paridx < numOptional; paridx++) {
					if (paridx < args.length) {
						// bind formal to actual and ignore default initialization expression
						binder.bindParamToArg(scope, formals[paridx].base_asVariableAssignment().base_getName(), args[paridx]);	
					} else {
						// no more actuals: bind optional parameter to default initialization expression
						ATAssignVariable param = formals[paridx].base_asVariableAssignment();
						binder.bindParamToArg(scope, param.base_getName(), param.base_getValueExpression().meta_eval(inContext));
					}
				}
			}
		};
	}
	
	/**
	 * - VariableArity       (0 0 1) example: f(@rest)
	 */
	private static final PartialBinder makeVariableArity(final String funnam, final ATSymbol formal) {
		return new PartialBinder() {
			protected void bind(ATObject[] args, ATContext inContext, BindClosure binder) throws InterpreterException {
				// no arity check needed
				
				// bind the formal parameter to all given arguments
				binder.bindParamToArg(inContext.base_getLexicalScope(), formal, NATTable.atValue(args));
			}
		};
	}
	
	/**
	 * - MandatoryVariable   (n 0 1) example: f(a,b,@rest)
	 */
	private static final PartialBinder makeMandatoryVariable(final String funnam, final ATObject[] formals) {
		return new PartialBinder() {
			protected void bind(ATObject[] args, ATContext inContext, BindClosure binder) throws InterpreterException {
				int numMandatoryArguments = formals.length - 1;
				// perform arity check: number of arguments must be at least the number of mandatory arguments
				if (args.length < numMandatoryArguments) {
					// error: not enough actuals
					throw new XArityMismatch(funnam, numMandatoryArguments, args.length);
				}
				
				ATObject scope = inContext.base_getLexicalScope();
				
				// bind all mandatory arguments
				for (int paridx = 0; paridx < numMandatoryArguments; paridx++) {
					// bind formal to actual
					binder.bindParamToArg(scope, formals[paridx].base_asSymbol(), args[paridx]);			
				}
				
				// bind remaining arguments to the rest parameter
				int numRemainingArgs = args.length - numMandatoryArguments;
				ATObject[] restArgs = new ATObject[numRemainingArgs];
				for (int i = 0; i < numRemainingArgs; i++) {
					restArgs[i] = args[numMandatoryArguments + i];
				}
				ATSymbol restArgsName = formals[numMandatoryArguments].base_asSplice().base_getExpression().base_asSymbol();
				binder.bindParamToArg(scope, restArgsName, NATTable.atValue(restArgs));
			}
		};
	}
	
	/**
	 * - OptionalVariable    (0 m 1) example: f(a:=1,@rest)
	 */
	private static final PartialBinder makeOptionalVariable(final String funnam, final ATObject[] formals) {
		return new PartialBinder() {
			protected void bind(ATObject[] args, ATContext inContext, BindClosure binder) throws InterpreterException {
				int numOptional = formals.length - 1;
				
				// no arity check needed

				ATObject scope = inContext.base_getLexicalScope();
				
				// bind all optional arguments
				for (int paridx = 0; paridx < numOptional; paridx++) {
					if (paridx < args.length) {
						// bind formal to actual and ignore default initialization expression
						binder.bindParamToArg(scope, formals[paridx].base_asVariableAssignment().base_getName(), args[paridx]);	
					} else {
						// no more actuals: bind optional parameter to default initialization expression
						ATAssignVariable param = formals[paridx].base_asVariableAssignment();
						binder.bindParamToArg(scope, param.base_getName(), param.base_getValueExpression().meta_eval(inContext));
					}
				}
				
				// bind remaining arguments to the rest parameter
				ATSymbol restArgsName = formals[numOptional+1].base_asSplice().base_getExpression().base_asSymbol();
				
				if (args.length <= numOptional) {
					// no more actual arguments to bind to the rest parameter
					binder.bindParamToArg(scope, restArgsName, NATTable.EMPTY);
				} else {
					int numRemainingArgs = args.length - numOptional;
					ATObject[] restArgs = new ATObject[numRemainingArgs];
					for (int i = 0; i < numRemainingArgs; i++) {
						restArgs[i] = args[numOptional + i];
					}
					binder.bindParamToArg(scope, restArgsName, NATTable.atValue(restArgs));	
				}
			}
		};
	}
	
	/**
	 * - Generic             (n m 1) example: f(a,b:=1,@rest)
	 */
	private static final PartialBinder makeGeneric(final String funnam, final ATObject[] formals,
												   final int numMandatory, final int numOptional) {
		return new PartialBinder() {
			protected void bind(ATObject[] args, ATContext inContext, BindClosure binder) throws InterpreterException {
				// perform arity check: number of arguments must at least equal number of mandatory arguments
				if (args.length < numMandatory) {
					// error: not enough actuals
					throw new XArityMismatch(funnam, numMandatory, args.length);
				}
				
				int paridx = 0;
				ATObject scope = inContext.base_getLexicalScope();
				
				// bind all mandatory arguments
				for (; paridx < numMandatory; paridx++) {
					// bind formal to actual
					binder.bindParamToArg(scope, formals[paridx].base_asSymbol(), args[paridx]);			
				}
				
				// bind all optional arguments
				// count the number of actuals used for filling in optional parameters
				int numFilledInOptionals = 0;
				for (; paridx < numMandatory + numOptional; paridx++) {
					if (paridx < args.length) {
						// bind formal to actual and ignore default initialization expression
						binder.bindParamToArg(scope, formals[paridx].base_asVariableAssignment().base_getName(), args[paridx]);	
						numFilledInOptionals++;
					} else {
						// no more actuals: bind optional parameter to default initialization expression
						ATAssignVariable param = formals[paridx].base_asVariableAssignment();
						binder.bindParamToArg(scope, param.base_getName(), param.base_getValueExpression().meta_eval(inContext));
					}
				}
				
				// bind remaining arguments to the rest parameter
				ATSymbol restArgsName = formals[formals.length-1].base_asSplice().base_getExpression().base_asSymbol();
				
				if (args.length <= numMandatory + numOptional) {
					// no more actual arguments to bind to the rest parameter
					binder.bindParamToArg(scope, restArgsName, NATTable.EMPTY);
				} else {
					int numRemainingArgs = args.length - (numMandatory + numOptional);
					ATObject[] restArgs = new ATObject[numRemainingArgs];
					for (int i = 0; i < numRemainingArgs; i++) {
						restArgs[i] = args[(numMandatory + numOptional) + i];
					}
					binder.bindParamToArg(scope, restArgsName, NATTable.atValue(restArgs));	
				}
			}
		};
	}
	
}
