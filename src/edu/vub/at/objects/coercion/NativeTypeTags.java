/**
 * AmbientTalk/2 Project
 * NativeTypeTags.java created on 26-feb-2007 at 16:29:25
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

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Vector;

import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.natives.NATTypeTag;

/**
 * This class serves to hold references to the native type tags with which native
 * AmbientTalk objects are tagged.
 *
 * @author tvcutsem
 */
public final class NativeTypeTags {
	
	private static HashSet<NATTypeTag> nativeTypeTags_ = null;

	/* Java implementation of lobby.at.types.at: */

//deftype Isolate;
	public final static NATTypeTag _ISOLATE_ = NATTypeTag.atValue("Isolate");

//deftype Meta;
	public final static NATTypeTag _META_ = NATTypeTag.atValue("Meta");
	
//deftype Boolean;
	public final static NATTypeTag _BOOLEAN_ = NATTypeTag.atValue("Boolean");
	
//deftype Closure;
	public final static NATTypeTag _CLOSURE_ = NATTypeTag.atValue("Closure");
	
//deftype Context;
	public final static NATTypeTag _CONTEXT_ = NATTypeTag.atValue("Context");
	
//deftype Field;
	public final static NATTypeTag _FIELD_ = NATTypeTag.atValue("Field");
	
//deftype Handler;
	public final static NATTypeTag _HANDLER_ = NATTypeTag.atValue("Handler");
	
//deftype Method;
	public final static NATTypeTag _METHOD_ = NATTypeTag.atValue("Method");
	
//deftype Message;
	public final static NATTypeTag _MESSAGE_ = NATTypeTag.atValue("Message");
	
//deftype MethodInvocation <: Message;
	public final static NATTypeTag _METHODINV_ = NATTypeTag.atValue("MethodInvocation", _MESSAGE_);

//deftype FieldSelection <: Message;
    public final static NATTypeTag _FIELDSEL_ = NATTypeTag.atValue("FieldSelection", _MESSAGE_);

	
//deftype Delegation <: Message;
	public final static NATTypeTag _DELEGATION_ = NATTypeTag.atValue("Delegation", _MESSAGE_);
	
//deftype AsyncMessage <: Message;
	public final static NATTypeTag _ASYNCMSG_ = NATTypeTag.atValue("AsyncMessage", _MESSAGE_);
	
//deftype Letter;
	public final static NATTypeTag _LETTER_ = NATTypeTag.atValue("Letter");
	
//deftype Mirror;
	public final static NATTypeTag _MIRROR_ = NATTypeTag.atValue("Mirror");
	
//deftype ActorMirror;
	public final static NATTypeTag _ACTORMIRROR_ = NATTypeTag.atValue("ActorMirror");
	
//deftype TypeTag;
	public final static NATTypeTag _TYPETAG_ = NATTypeTag.atValue("TypeTag");
	
//deftype FarReference;
	public final static NATTypeTag _FARREF_ = NATTypeTag.atValue("FarReference");	

// abstract grammar
//deftype AbstractGrammar;
	public final static NATTypeTag _ABSTRACTGRAMMAR_ = NATTypeTag.atValue("AbstractGrammar");
	
//deftype Statement;
	public final static NATTypeTag _STATEMENT_ = NATTypeTag.atValue("Statement", _ABSTRACTGRAMMAR_);
	
//	deftype Expression;
	public final static NATTypeTag _EXPRESSION_ = NATTypeTag.atValue("Expression", _STATEMENT_);
	
// literal values
//deftype Table <: Expression;
	public final static NATTypeTag _TABLE_ = NATTypeTag.atValue("Table", _EXPRESSION_);
	
//deftype Text <: Expression;
	public final static NATTypeTag _TEXT_ = NATTypeTag.atValue("Text", _EXPRESSION_);
	
//deftype Numeric <: Expression;
	public final static NATTypeTag _NUMERIC_ = NATTypeTag.atValue("Numeric", _EXPRESSION_);
	
//deftype Number <: Numeric;
	public final static NATTypeTag _NUMBER_ = NATTypeTag.atValue("Number", _EXPRESSION_);
	
//deftype Fraction <: Numeric;
	public final static NATTypeTag _FRACTION_ = NATTypeTag.atValue("Fraction", _EXPRESSION_);

//deftype Symbol <: Expression;
	public final static NATTypeTag _SYMBOL_ = NATTypeTag.atValue("Symbol", _EXPRESSION_);

//deftype Begin <: AbstractGrammar;
	public final static NATTypeTag _BEGIN_ = NATTypeTag.atValue("Begin", _ABSTRACTGRAMMAR_);
	
//deftype Splice <: Expression;
	public final static NATTypeTag _SPLICE_ = NATTypeTag.atValue("Splice", _EXPRESSION_);
	
//deftype UnquoteSplice <: Expression;
	public final static NATTypeTag _UQSPLICE_ = NATTypeTag.atValue("UnquoteSplice", _EXPRESSION_);
	
//deftype MessageCreation <: Expression;
	public final static NATTypeTag _MSGCREATION_ = NATTypeTag.atValue("MessageCreation", _EXPRESSION_);
	
//deftype Definition <: Statement;
	public final static NATTypeTag _DEFINITION_ = NATTypeTag.atValue("Definition", _STATEMENT_);
	
//	deftype MethodDefinition <: Definition;
	public final static NATTypeTag _METHOD_DEFINITION_ = NATTypeTag.atValue("MethodDefinition", _DEFINITION_);

	
//
//// exception types
//deftype Exception;
	public final static NATTypeTag _EXCEPTION_ = NATTypeTag.atValue("Exception");
	
//deftype ArityMismatch <: Exception;
	public final static NATTypeTag _ARITYMISMATCH_ = NATTypeTag.atValue("ArityMismatch", _EXCEPTION_);
	
//deftype ClassNotFound <: Exception;
	public final static NATTypeTag _CLASSNOTFOUND_ = NATTypeTag.atValue("ClassNotFound", _EXCEPTION_);
	
//deftype DuplicateSlot <: Exception;
	public final static NATTypeTag _DUPLICATESLOT_ = NATTypeTag.atValue("DuplicateSlot", _EXCEPTION_);
	
//deftype IllegalApplication <: Exception;
	public final static NATTypeTag _ILLAPP_ = NATTypeTag.atValue("IllegalApplication", _EXCEPTION_);
	
//deftype IllegalArgument <: Exception;
	public final static NATTypeTag _ILLARG_ = NATTypeTag.atValue("IllegalArgument", _EXCEPTION_);
	
//deftype IllegalIndex <: Exception;
	public final static NATTypeTag _ILLIDX_ = NATTypeTag.atValue("IllegalIndex", _EXCEPTION_);
	
//deftype IllegalOperation <: Exception;
	public final static NATTypeTag _ILLOP_ = NATTypeTag.atValue("IllegalOperation", _EXCEPTION_);
	
//deftype IllegalParameter <: Exception;
	public final static NATTypeTag _ILLPARAM_ = NATTypeTag.atValue("IllegalParameter", _EXCEPTION_);
	
//deftype IllegalQuote <: Exception;
	public final static NATTypeTag _ILLQUOTE_ = NATTypeTag.atValue("IllegalQuote", _EXCEPTION_);
	
//deftype IllegalSplice <: Exception;
	public final static NATTypeTag _ILLSPLICE_ = NATTypeTag.atValue("IllegalSplice", _EXCEPTION_);
	
//deftype IllegalUnquote <: Exception;
	public final static NATTypeTag _ILLUQUOTE_ = NATTypeTag.atValue("IllegalUnquote", _EXCEPTION_);
	
//deftype IndexOutOfBounds <: Exception;
	public final static NATTypeTag _IDXOUTOFBOUNDS_ = NATTypeTag.atValue("IndexOutOfBounds", _EXCEPTION_);
	
//deftype IOProblem <: Exception;
	public final static NATTypeTag _IOPROBLEM_ = NATTypeTag.atValue("IOProblem", _EXCEPTION_);
	
//deftype NotInstantiatable <: Exception;
	public final static NATTypeTag _NOTINSTANTIATABLE_ = NATTypeTag.atValue("NotInstantiatable", _EXCEPTION_);
	
//deftype ParseError <: Exception;
	public final static NATTypeTag _PARSEERROR_ = NATTypeTag.atValue("ParseError", _EXCEPTION_);
	
//deftype ReflectionFailure <: Exception;
	public final static NATTypeTag _REFLECTIONFAILURE_ = NATTypeTag.atValue("ReflectionFailure", _EXCEPTION_);

//	deftype SelectorNotFound <: Exception;
	public final static NATTypeTag _SELECTORNOTFOUND_ = NATTypeTag.atValue("SelectorNotFound", _EXCEPTION_);
	
//deftype SymbiosisFailure <: Exception;
	public final static NATTypeTag _SYMBIOSISFAILURE_ = NATTypeTag.atValue("SymbiosisFailure", _EXCEPTION_);
	
//deftype TypeMismatch <: Exception;
	public final static NATTypeTag _TYPEMISMATCH_ = NATTypeTag.atValue("TypeMismatch", _EXCEPTION_);
	
//deftype UnassignableField <: Exception;
	public final static NATTypeTag _UNASSIGNABLEFIELD_ = NATTypeTag.atValue("UnassignableField", _EXCEPTION_);
	
//deftype UndefinedField <: Exception;
	public final static NATTypeTag _UNDEFINEDSLOT_ = NATTypeTag.atValue("UndefinedSlot", _EXCEPTION_);
	
//deftype CustomException <: Exception;
	public final static NATTypeTag _CUSTOMEXCEPTION_ = NATTypeTag.atValue("CustomException", _EXCEPTION_);

//deftype JavaException <: Exception;
	public final static NATTypeTag _JAVAEXCEPTION_ = NATTypeTag.atValue("JavaException", _EXCEPTION_);

//deftype ImportConflict <: Exception;
	public final static NATTypeTag _IMPORTCONFLICT_ = NATTypeTag.atValue("ImportConflict", _EXCEPTION_);

//deftype ObjectOffline <: Exception;
    public final static NATTypeTag _OBJECTOFFLINE_ = NATTypeTag.atValue("ObjectOffline", _EXCEPTION_);
    
//deftype SerializationError <: Exception;
	public final static NATTypeTag _SERIALIZATIONERROR_ = NATTypeTag.atValue("SerializationError", _EXCEPTION_);

    // misc
    

  //deftype Required;
    public final static NATTypeTag _REQUIRED_ = NATTypeTag.atValue("Required");
    
    //deftype ExternalEvent;
    public final static NATTypeTag _EXTERNAL_MSG_ = NATTypeTag.atValue("ExternalMessage");
    
    public static HashSet<NATTypeTag> getNativeTypeTags() {
    	if (nativeTypeTags_ == null) {

    		nativeTypeTags_ = new HashSet<NATTypeTag>();
    		Field[] fields = NativeTypeTags.class.getFields();
    		for (Field f : fields) {
    			Object fi;
    			try {
    				fi = f.get(NativeTypeTags.class);
    				if (fi instanceof NATTypeTag) {
    					NATTypeTag tt = (NATTypeTag) fi;
    					nativeTypeTags_.add(tt);
    				}
    			} catch (SecurityException e) {
    				// ignore
    			} catch (IllegalAccessException e) {
    				// ignore
    			}
    		}
    	}
    	return nativeTypeTags_;
    }

    
}