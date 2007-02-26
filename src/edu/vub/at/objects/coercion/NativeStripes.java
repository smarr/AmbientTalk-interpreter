/**
 * AmbientTalk/2 Project
 * NativeStripes.java created on 26-feb-2007 at 16:29:25
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
package edu.vub.at.objects.coercion;

import edu.vub.at.objects.natives.NATStripe;

/**
 * This class serves to hold references to the native stripes with which native
 * AmbientTalk objects are tagged.
 *
 * @author tvcutsem
 */
public final class NativeStripes {

	/* Java implementation of lobby.at.stripes.at: */

//defstripe Isolate;
	public final static NATStripe _ISOLATE_ = NATStripe.atValue("at.stripes.Isolate");
	
//defstripe JavaObject;
	public final static NATStripe _JAVAOBJECT_ = NATStripe.atValue("at.stripes.JavaObject");
	
//defstripe Boolean;
	public final static NATStripe _BOOLEAN_ = NATStripe.atValue("at.stripes.Boolean");
	
//defstripe Closure;
	public final static NATStripe _CLOSURE_ = NATStripe.atValue("at.stripes.Closure");
	
//defstripe Context;
	public final static NATStripe _CONTEXT_ = NATStripe.atValue("at.stripes.Context");
	
//defstripe Field;
	public final static NATStripe _FIELD_ = NATStripe.atValue("at.stripes.Field");
	
//defstripe Handler;
	public final static NATStripe _HANDLER_ = NATStripe.atValue("at.stripes.Handler");
	
//defstripe Method;
	public final static NATStripe _METHOD_ = NATStripe.atValue("at.stripes.Method");
	
//defstripe Message;
	public final static NATStripe _MESSAGE_ = NATStripe.atValue("at.stripes.Message");
	
//defstripe MethodInvocation <: Message;
	public final static NATStripe _METHODINV_ = NATStripe.atValue("at.stripes.MethodInvocation", _MESSAGE_);
	
//defstripe AsyncMessage <: Message;
	public final static NATStripe _ASYNCMSG_ = NATStripe.atValue("at.stripes.AsyncMessage", _MESSAGE_);
	
//defstripe Mirror;
	public final static NATStripe _MIRROR_ = NATStripe.atValue("at.stripes.Mirror");
	
//defstripe ActorMirror;
	public final static NATStripe _ACTORMIRROR_ = NATStripe.atValue("at.stripes.ActorMirror");
	
//defstripe Stripe;
	public final static NATStripe _STRIPE_ = NATStripe.atValue("at.stripes.Stripe");
	

// abstract grammar
//defstripe AbstractGrammar;
	public final static NATStripe _ABSTRACTGRAMMAR_ = NATStripe.atValue("at.stripes.AbstractGrammar");
	
//defstripe Statement;
	public final static NATStripe _STATEMENT_ = NATStripe.atValue("at.stripes.Statement", _ABSTRACTGRAMMAR_);
	
//	defstripe Expression;
	public final static NATStripe _EXPRESSION_ = NATStripe.atValue("at.stripes.Expression", _STATEMENT_);
	
// literal values
//defstripe Table <: Expression;
	public final static NATStripe _TABLE_ = NATStripe.atValue("at.stripes.Table");
	
//defstripe Text <: Expression;
	public final static NATStripe _TEXT_ = NATStripe.atValue("at.stripes.Text");
	
//defstripe Numeric <: Expression;
	public final static NATStripe _NUMERIC_ = NATStripe.atValue("at.stripes.Numeric");
	
//defstripe Number <: Numeric;
	public final static NATStripe _NUMBER_ = NATStripe.atValue("at.stripes.Number");
	
//defstripe Fraction <: Numeric;
	public final static NATStripe _FRACTION_ = NATStripe.atValue("at.stripes.Fraction");
	
//
//// exception types
//defstripe Exception;
	public final static NATStripe _EXCEPTION_ = NATStripe.atValue("at.stripes.Exception");
	
//defstripe ArityMismatch <: Exception;
	public final static NATStripe _ARITYMISMATCH_ = NATStripe.atValue("at.stripes.ArityMismatch", _EXCEPTION_);
	
//defstripe ClassNotFound <: Exception;
	public final static NATStripe _CLASSNOTFOUND_ = NATStripe.atValue("at.stripes.ClassNotFound", _EXCEPTION_);
	
//defstripe DuplicateSlot <: Exception;
	public final static NATStripe _DUPLICATESLOT_ = NATStripe.atValue("at.stripes.DuplicateSlot", _EXCEPTION_);
	
//defstripe IllegalApplication <: Exception;
	public final static NATStripe _ILLAPP_ = NATStripe.atValue("at.stripes.IllegalApplication", _EXCEPTION_);
	
//defstripe IllegalArgument <: Exception;
	public final static NATStripe _ILLARG_ = NATStripe.atValue("at.stripes.IllegalArgument", _EXCEPTION_);
	
//defstripe IllegalIndex <: Exception;
	public final static NATStripe _ILLIDX_ = NATStripe.atValue("at.stripes.IllegalIndex", _EXCEPTION_);
	
//defstripe IllegalOperation <: Exception;
	public final static NATStripe _ILLOP_ = NATStripe.atValue("at.stripes.IllegalOperation", _EXCEPTION_);
	
//defstripe IllegalParameter <: Exception;
	public final static NATStripe _ILLPARAM_ = NATStripe.atValue("at.stripes.IllegalParameter", _EXCEPTION_);
	
//defstripe IllegalQuote <: Exception;
	public final static NATStripe _ILLQUOTE_ = NATStripe.atValue("at.stripes.IllegalQuote", _EXCEPTION_);
	
//defstripe IllegalSplice <: Exception;
	public final static NATStripe _ILLSPLICE_ = NATStripe.atValue("at.stripes.IllegalSplice", _EXCEPTION_);
	
//defstripe IllegalUnquote <: Exception;
	public final static NATStripe _ILLUQUOTE_ = NATStripe.atValue("at.stripes.IllegalUnquote", _EXCEPTION_);
	
//defstripe IndexOutOfBounds <: Exception;
	public final static NATStripe _IDXOUTOFBOUNDS_ = NATStripe.atValue("at.stripes.IndexOutOfBounds", _EXCEPTION_);
	
//defstripe IOProblem <: Exception;
	public final static NATStripe _IOPROBLEM_ = NATStripe.atValue("at.stripes.IOProblem", _EXCEPTION_);
	
//defstripe NotInstantiatable <: Exception;
	public final static NATStripe _NOTINSTANTIATABLE_ = NATStripe.atValue("at.stripes.NotInstantiatable", _EXCEPTION_);
	
//defstripe ParseError <: Exception;
	public final static NATStripe _PARSEERROR_ = NATStripe.atValue("at.stripes.ParseError", _EXCEPTION_);
	
//defstripe ReflectionFailure <: Exception;
	public final static NATStripe _REFLECTIONFAILURE_ = NATStripe.atValue("at.stripes.ReflectionFailure", _EXCEPTION_);

//	defstripe SelectorNotFound <: Exception;
	public final static NATStripe _SELECTORNOTFOUND_ = NATStripe.atValue("at.stripes.SelectorNotFound", _EXCEPTION_);
	
//defstripe SymbiosisFailure <: Exception;
	public final static NATStripe _SYMBIOSISFAILURE_ = NATStripe.atValue("at.stripes.SymbiosisFailure", _EXCEPTION_);
	
//defstripe TypeMismatch <: Exception;
	public final static NATStripe _TYPEMISMATCH_ = NATStripe.atValue("at.stripes.TypeMismatch", _EXCEPTION_);
	
//defstripe UnassignableField <: Exception;
	public final static NATStripe _UNASSIGNABLEFIELD_ = NATStripe.atValue("at.stripes.UnassignableField", _EXCEPTION_);
	
//defstripe UndefinedField <: Exception;
	public final static NATStripe _UNDEFINEDFIELD_ = NATStripe.atValue("at.stripes.UndefinedField", _EXCEPTION_);
	
//defstripe CustomException <: Exception;
	public final static NATStripe _CUSTOMEXCEPTION_ = NATStripe.atValue("at.stripes.CustomException", _EXCEPTION_);

//defstripe JavaException <: Exception;
	public final static NATStripe _JAVAEXCEPTION_ = NATStripe.atValue("at.stripes.JavaException", _EXCEPTION_);
}
