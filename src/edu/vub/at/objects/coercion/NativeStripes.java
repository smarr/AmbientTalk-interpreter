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
	public final static NATStripe _ISOLATE_ = NATStripe.atValue("Isolate");

//defstripe Meta;
	public final static NATStripe _META_ = NATStripe.atValue("Meta");
	
//defstripe Boolean;
	public final static NATStripe _BOOLEAN_ = NATStripe.atValue("Boolean");
	
//defstripe Closure;
	public final static NATStripe _CLOSURE_ = NATStripe.atValue("Closure");
	
//defstripe Context;
	public final static NATStripe _CONTEXT_ = NATStripe.atValue("Context");
	
//defstripe Field;
	public final static NATStripe _FIELD_ = NATStripe.atValue("Field");
	
//defstripe Handler;
	public final static NATStripe _HANDLER_ = NATStripe.atValue("Handler");
	
//defstripe Method;
	public final static NATStripe _METHOD_ = NATStripe.atValue("Method");
	
//defstripe Message;
	public final static NATStripe _MESSAGE_ = NATStripe.atValue("Message");
	
//defstripe MethodInvocation <: Message;
	public final static NATStripe _METHODINV_ = NATStripe.atValue("MethodInvocation", _MESSAGE_);

//defstripe Delegation <: Message;
	public final static NATStripe _DELEGATION_ = NATStripe.atValue("Delegation", _MESSAGE_);
	
//defstripe AsyncMessage <: Message;
	public final static NATStripe _ASYNCMSG_ = NATStripe.atValue("AsyncMessage", _MESSAGE_);
	
//defstripe Mirror;
	public final static NATStripe _MIRROR_ = NATStripe.atValue("Mirror");
	
//defstripe ActorMirror;
	public final static NATStripe _ACTORMIRROR_ = NATStripe.atValue("ActorMirror");
	
//defstripe Stripe;
	public final static NATStripe _STRIPE_ = NATStripe.atValue("Stripe");
	

// abstract grammar
//defstripe AbstractGrammar;
	public final static NATStripe _ABSTRACTGRAMMAR_ = NATStripe.atValue("AbstractGrammar");
	
//defstripe Statement;
	public final static NATStripe _STATEMENT_ = NATStripe.atValue("Statement", _ABSTRACTGRAMMAR_);
	
//	defstripe Expression;
	public final static NATStripe _EXPRESSION_ = NATStripe.atValue("Expression", _STATEMENT_);
	
// literal values
//defstripe Table <: Expression;
	public final static NATStripe _TABLE_ = NATStripe.atValue("Table", _EXPRESSION_);
	
//defstripe Text <: Expression;
	public final static NATStripe _TEXT_ = NATStripe.atValue("Text", _EXPRESSION_);
	
//defstripe Numeric <: Expression;
	public final static NATStripe _NUMERIC_ = NATStripe.atValue("Numeric", _EXPRESSION_);
	
//defstripe Number <: Numeric;
	public final static NATStripe _NUMBER_ = NATStripe.atValue("Number", _EXPRESSION_);
	
//defstripe Fraction <: Numeric;
	public final static NATStripe _FRACTION_ = NATStripe.atValue("Fraction", _EXPRESSION_);

//defstripe Symbol <: Expression;
	public final static NATStripe _SYMBOL_ = NATStripe.atValue("Symbol", _EXPRESSION_);

//defstripe Begin <: AbstractGrammar;
	public final static NATStripe _BEGIN_ = NATStripe.atValue("Begin", _ABSTRACTGRAMMAR_);
	
//defstripe Splice <: Expression;
	public final static NATStripe _SPLICE_ = NATStripe.atValue("Splice", _EXPRESSION_);
	
//defstripe UnquoteSplice <: Expression;
	public final static NATStripe _UQSPLICE_ = NATStripe.atValue("UnquoteSplice", _EXPRESSION_);
	
//defstripe MessageCreation <: Expression;
	public final static NATStripe _MSGCREATION_ = NATStripe.atValue("MessageCreation", _EXPRESSION_);
	
//defstripe Definition <: Statement;
	public final static NATStripe _DEFINITION_ = NATStripe.atValue("Definition", _STATEMENT_);
	
//
//// exception types
//defstripe Exception;
	public final static NATStripe _EXCEPTION_ = NATStripe.atValue("Exception");
	
//defstripe ArityMismatch <: Exception;
	public final static NATStripe _ARITYMISMATCH_ = NATStripe.atValue("ArityMismatch", _EXCEPTION_);
	
//defstripe ClassNotFound <: Exception;
	public final static NATStripe _CLASSNOTFOUND_ = NATStripe.atValue("ClassNotFound", _EXCEPTION_);
	
//defstripe DuplicateSlot <: Exception;
	public final static NATStripe _DUPLICATESLOT_ = NATStripe.atValue("DuplicateSlot", _EXCEPTION_);
	
//defstripe IllegalApplication <: Exception;
	public final static NATStripe _ILLAPP_ = NATStripe.atValue("IllegalApplication", _EXCEPTION_);
	
//defstripe IllegalArgument <: Exception;
	public final static NATStripe _ILLARG_ = NATStripe.atValue("IllegalArgument", _EXCEPTION_);
	
//defstripe IllegalIndex <: Exception;
	public final static NATStripe _ILLIDX_ = NATStripe.atValue("IllegalIndex", _EXCEPTION_);
	
//defstripe IllegalOperation <: Exception;
	public final static NATStripe _ILLOP_ = NATStripe.atValue("IllegalOperation", _EXCEPTION_);
	
//defstripe IllegalParameter <: Exception;
	public final static NATStripe _ILLPARAM_ = NATStripe.atValue("IllegalParameter", _EXCEPTION_);
	
//defstripe IllegalQuote <: Exception;
	public final static NATStripe _ILLQUOTE_ = NATStripe.atValue("IllegalQuote", _EXCEPTION_);
	
//defstripe IllegalSplice <: Exception;
	public final static NATStripe _ILLSPLICE_ = NATStripe.atValue("IllegalSplice", _EXCEPTION_);
	
//defstripe IllegalUnquote <: Exception;
	public final static NATStripe _ILLUQUOTE_ = NATStripe.atValue("IllegalUnquote", _EXCEPTION_);
	
//defstripe IndexOutOfBounds <: Exception;
	public final static NATStripe _IDXOUTOFBOUNDS_ = NATStripe.atValue("IndexOutOfBounds", _EXCEPTION_);
	
//defstripe IOProblem <: Exception;
	public final static NATStripe _IOPROBLEM_ = NATStripe.atValue("IOProblem", _EXCEPTION_);
	
//defstripe NotInstantiatable <: Exception;
	public final static NATStripe _NOTINSTANTIATABLE_ = NATStripe.atValue("NotInstantiatable", _EXCEPTION_);
	
//defstripe ParseError <: Exception;
	public final static NATStripe _PARSEERROR_ = NATStripe.atValue("ParseError", _EXCEPTION_);
	
//defstripe ReflectionFailure <: Exception;
	public final static NATStripe _REFLECTIONFAILURE_ = NATStripe.atValue("ReflectionFailure", _EXCEPTION_);

//	defstripe SelectorNotFound <: Exception;
	public final static NATStripe _SELECTORNOTFOUND_ = NATStripe.atValue("SelectorNotFound", _EXCEPTION_);
	
//defstripe SymbiosisFailure <: Exception;
	public final static NATStripe _SYMBIOSISFAILURE_ = NATStripe.atValue("SymbiosisFailure", _EXCEPTION_);
	
//defstripe TypeMismatch <: Exception;
	public final static NATStripe _TYPEMISMATCH_ = NATStripe.atValue("TypeMismatch", _EXCEPTION_);
	
//defstripe UnassignableField <: Exception;
	public final static NATStripe _UNASSIGNABLEFIELD_ = NATStripe.atValue("UnassignableField", _EXCEPTION_);
	
//defstripe UndefinedField <: Exception;
	public final static NATStripe _UNDEFINEDSLOT_ = NATStripe.atValue("UndefinedSlot", _EXCEPTION_);
	
//defstripe CustomException <: Exception;
	public final static NATStripe _CUSTOMEXCEPTION_ = NATStripe.atValue("CustomException", _EXCEPTION_);

//defstripe JavaException <: Exception;
	public final static NATStripe _JAVAEXCEPTION_ = NATStripe.atValue("JavaException", _EXCEPTION_);

//defstripe ImportConflict <: Exception;
	public final static NATStripe _IMPORTCONFLICT_ = NATStripe.atValue("ImportConflict", _EXCEPTION_);

//defstripe ObjectOffline <: Exception;
    public final static NATStripe _OBJECTOFFLINE_ = NATStripe.atValue("ObjectOffline", _EXCEPTION_);
}