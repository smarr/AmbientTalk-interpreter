/**
 * AmbientTalk/2 Project
 * ATConversions.java created on Jul 23, 2006 at 2:20:16 PM
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

import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.actors.ATFarObject;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATHandler;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATMirror;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATAssignVariable;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATDefinition;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATMessageCreation;
import edu.vub.at.objects.grammar.ATSplice;
import edu.vub.at.objects.grammar.ATStatement;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.grammar.ATUnquoteSplice;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATException;
import edu.vub.at.objects.natives.NATFraction;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATNumeric;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.symbiosis.JavaClass;
import edu.vub.at.objects.symbiosis.JavaObject;

/**
 * ATConversions is an interface defining all conversion functions between different
 * types of ambienttalk language elements. 
 * 
 * TODO: rename all base_is/as methods to meta_is/as and add global methods of the form
 * def isXXX: val { (reflect: val).isXXX() }
 * 
 * @author smostinc
 */
public interface ATConversions {

	public boolean base_isClosure() throws InterpreterException;
	public boolean base_isSymbol() throws InterpreterException;
	public boolean base_isTable() throws InterpreterException;
	public boolean base_isBoolean() throws InterpreterException;
	public boolean base_isCallFrame() throws InterpreterException;
	public boolean base_isUnquoteSplice() throws InterpreterException;
	public boolean base_isVariableAssignment() throws InterpreterException;
	public boolean base_isSplice() throws InterpreterException;
	public boolean base_isMethod() throws InterpreterException;
	public boolean base_isMessageCreation() throws InterpreterException;
	public boolean base_isMirror() throws InterpreterException;
	
	public ATClosure   base_asClosure() throws XTypeMismatch;
	public ATSymbol    base_asSymbol() throws XTypeMismatch;
	public ATTable     base_asTable() throws XTypeMismatch;
	public ATBoolean   base_asBoolean() throws XTypeMismatch;
	public ATNumber    base_asNumber() throws XTypeMismatch;
	public ATMessage   base_asMessage() throws XTypeMismatch;
	public ATField     base_asField() throws XTypeMismatch;
	public ATMethod    base_asMethod() throws XTypeMismatch;
	public ATMirror    base_asMirror() throws XTypeMismatch;
	public ATHandler   base_asHandler() throws XTypeMismatch;
	
	// Abstract Grammar Elements
	
	public ATStatement  		base_asStatement() throws XTypeMismatch;
	public ATDefinition 		base_asDefinition() throws XTypeMismatch;
	public ATExpression 		base_asExpression() throws XTypeMismatch;
	public ATBegin      		base_asBegin() throws XTypeMismatch;
	public ATMessageCreation 	base_asMessageCreation() throws XTypeMismatch;
	public ATUnquoteSplice 		base_asUnquoteSplice() throws XTypeMismatch;
	public ATAssignVariable 	base_asVariableAssignment() throws InterpreterException;
	public ATSplice 			base_asSplice() throws XTypeMismatch;
	
	// Concurrency and Distribution related values
	public ATBoolean 		base_isFarReference();
	
	public ATFarObject		base_asFarReference() throws XTypeMismatch;
	public ATAsyncMessage	base_asAsyncMessage() throws XTypeMismatch;

	// Native Value Elements
	
	public boolean isNativeBoolean();
	public boolean isNativeText();
	public boolean isAmbientTalkObject();
	public boolean isJavaObjectUnderSymbiosis();
	public boolean isNativeField();
	
	public NATObject   asAmbientTalkObject() throws XTypeMismatch;
	public NATNumber   asNativeNumber() throws XTypeMismatch;
	public NATFraction asNativeFraction() throws XTypeMismatch;
	public NATText     asNativeText() throws InterpreterException;
	public NATTable    asNativeTable() throws XTypeMismatch;
	public NATBoolean  asNativeBoolean() throws XTypeMismatch;
	public NATNumeric  asNativeNumeric() throws XTypeMismatch;
	public NATException asNativeException() throws XTypeMismatch;
	
	public JavaObject  asJavaObjectUnderSymbiosis() throws XTypeMismatch;
	public JavaClass   asJavaClassUnderSymbiosis() throws XTypeMismatch;
	

}
