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

import edu.vub.at.actors.ATActorMirror;
import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.actors.ATFarReference;
import edu.vub.at.actors.natives.NATFarReference;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATHandler;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATStripe;
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
import edu.vub.at.objects.mirrors.NATMirage;
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
 * ATConversions is an interface defining all conversion methods between different
 * types of ambienttalk language elements. They are hidden from the language level
 * because they neither belong to base- nor meta-level.
 * 
 * @author smostinc
 */
public interface ATConversions {

	public boolean isClosure() throws InterpreterException;
	public boolean isSymbol() throws InterpreterException;
	public boolean isTable() throws InterpreterException;
	public boolean isBoolean() throws InterpreterException;
	public boolean isCallFrame() throws InterpreterException;
	public boolean isUnquoteSplice() throws InterpreterException;
	public boolean isVariableAssignment() throws InterpreterException;
	public boolean isSplice() throws InterpreterException;
	public boolean isMethod() throws InterpreterException;
	public boolean isMessageCreation() throws InterpreterException;
	public boolean isStripe() throws InterpreterException;
	public boolean isFarReference() throws InterpreterException;
	
	public ATClosure   asClosure() throws InterpreterException;
	public ATSymbol    asSymbol() throws InterpreterException;
	public ATTable     asTable() throws InterpreterException;
	public ATBoolean   asBoolean() throws InterpreterException;
	public ATNumber    asNumber() throws InterpreterException;
	public ATMessage   asMessage() throws InterpreterException;
	public ATField     asField() throws InterpreterException;
	public ATMethod    asMethod() throws InterpreterException;
	public ATHandler   asHandler() throws InterpreterException;
	public ATStripe    asStripe() throws InterpreterException;
	public ATFarReference asFarReference() throws InterpreterException;
	public ATAsyncMessage asAsyncMessage() throws InterpreterException;
	public ATActorMirror asActorMirror() throws InterpreterException;
	
	// Abstract Grammar Elements
	
	public ATStatement  		asStatement() throws InterpreterException;
	public ATDefinition 		asDefinition() throws InterpreterException;
	public ATExpression 		asExpression() throws InterpreterException;
	public ATBegin      		asBegin() throws InterpreterException;
	public ATMessageCreation 	asMessageCreation() throws InterpreterException;
	public ATUnquoteSplice 		asUnquoteSplice() throws InterpreterException;
	public ATAssignVariable 	asVariableAssignment() throws InterpreterException;
	public ATSplice 			asSplice() throws InterpreterException;

	// Native Value Elements
	
	public boolean isNativeBoolean();
	public boolean isNativeText();
	public boolean isAmbientTalkObject();
	public boolean isJavaObjectUnderSymbiosis();
	public boolean isNativeField();
	
	public NATObject   asAmbientTalkObject() throws XTypeMismatch;
	public NATMirage   asMirage() throws XTypeMismatch;
	public NATNumber   asNativeNumber() throws XTypeMismatch;
	public NATFraction asNativeFraction() throws XTypeMismatch;
	public NATText     asNativeText() throws XTypeMismatch;
	public NATTable    asNativeTable() throws XTypeMismatch;
	public NATBoolean  asNativeBoolean() throws XTypeMismatch;
	public NATNumeric  asNativeNumeric() throws XTypeMismatch;
	public NATException asNativeException() throws XTypeMismatch;
	public NATFarReference asNativeFarReference() throws XTypeMismatch;
	
	public JavaObject  asJavaObjectUnderSymbiosis() throws XTypeMismatch;
	public JavaClass   asJavaClassUnderSymbiosis() throws XTypeMismatch;
	

}
