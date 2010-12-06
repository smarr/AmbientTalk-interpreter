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
import edu.vub.at.actors.ATLetter;
import edu.vub.at.actors.natives.NATFarReference;
import edu.vub.at.actors.natives.NATRemoteFarRef;
import edu.vub.at.actors.natives.NATFarReference.NATOutboxLetter;
import edu.vub.at.eval.Import.DelegateMethod;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATHandler;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATMethodInvocation;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.grammar.ATAssignVariable;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATDefinition;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATMessageCreation;
import edu.vub.at.objects.grammar.ATQuote;
import edu.vub.at.objects.grammar.ATSplice;
import edu.vub.at.objects.grammar.ATStatement;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.grammar.ATUnquoteSplice;
import edu.vub.at.objects.mirrors.NATIntrospectiveMirror;
import edu.vub.at.objects.mirrors.NATMirage;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATFraction;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATNumeric;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.symbiosis.JavaClass;
import edu.vub.at.objects.symbiosis.JavaMethod;
import edu.vub.at.objects.symbiosis.JavaObject;

/**
 * ATConversions is an interface defining all conversion methods between different
 * types of ambienttalk language elements. They are hidden from the language level
 * because they neither belong to base- nor meta-level.
 * 
 * @author smostinc
 */
public interface ATConversions {

	/**
	 * Used to distinguish between symbol, assignment, splice in formal param list.
	 */
	public boolean isSymbol() throws InterpreterException;
	/**
	 * Used to check in <tt>o.m()@exp</tt> if exp is a table or not.
	 */
	public boolean isTable() throws InterpreterException;
	/**
	 * Used to recognize unquote-splice elements when quoting a table.
	 */
	public boolean isUnquoteSplice() throws InterpreterException;
	/**
	 * Used to distinguish between symbol, assignment, splice in formal param list
	 */
	public boolean isVariableAssignment() throws InterpreterException;
	/**
	 * Used to distinguish between symbol, assignment, splice in formal param list and
	 * to recognize spliced elements during table evaluation.
	 */
	public boolean isSplice() throws InterpreterException;
	/**
	 * Used to distinguish definitions in statement lists
	 */
	public boolean isDefinition() throws InterpreterException;
	/**
	 * Used in message send expressions to print, to check between <tt><+</tt> and
	 * <tt>./^/<-</tt> operators.
	 */
	public boolean isMessageCreation() throws InterpreterException;
	/**
	 * Used to identify type tags for comparison using <tt>==</tt>.
	 */
	public boolean isTypeTag() throws InterpreterException;
	/**
	 * Used when sending an asynchronous message, to determine whether to
	 * invoke <tt>receive</tt> on the receiver object directly, or to pass
	 * via the actor's queue first.
	 */
	public boolean isFarReference() throws InterpreterException;
	
	public ATClosure   asClosure() throws InterpreterException;
	public ATSymbol    asSymbol() throws InterpreterException;
	public ATTable     asTable() throws InterpreterException;
	public ATBoolean   asBoolean() throws InterpreterException;
	public ATNumber    asNumber() throws InterpreterException;
	public ATMessage   asMessage() throws InterpreterException;
	public ATField     asField() throws InterpreterException;
	public ATMethod    asMethod() throws InterpreterException;
	public ATMethodInvocation asMethodInvocation() throws InterpreterException;
	public ATHandler   asHandler() throws InterpreterException;
	public ATTypeTag    asTypeTag() throws InterpreterException;
	public ATFarReference asFarReference() throws InterpreterException;
	public ATAsyncMessage asAsyncMessage() throws InterpreterException;
	public ATActorMirror asActorMirror() throws InterpreterException;
	public ATLetter    asLetter() throws InterpreterException;
	
	// Abstract Grammar Elements
	
	public ATAbstractGrammar	asAbstractGrammar() throws InterpreterException;
	public ATDefinition 		asDefinition() throws InterpreterException;
	public ATExpression 		asExpression() throws InterpreterException;
	public ATBegin      		asBegin() throws InterpreterException;
	public ATQuote				asQuote() throws InterpreterException;
	public ATMessageCreation 	asMessageCreation() throws InterpreterException;
	public ATUnquoteSplice 		asUnquoteSplice() throws InterpreterException;
	public ATAssignVariable 	asVariableAssignment() throws InterpreterException;
	public ATSplice 			asSplice() throws InterpreterException;
	
	// Native Value Elements
	
	// The isNative / asNative type-casting protocol is crucial for correct
	// semantics in the case of e.g. wrapped natives in a Coercer (for symbiosis)
	// Never use Java type-casts explicitly!
	
	public boolean isNativeBoolean();
	public boolean isNativeText();
	public boolean isAmbientTalkObject(); // distinguish native from non-native objects
	public boolean isCallFrame() throws InterpreterException; // distinguish call frames from objects
	public boolean isJavaObjectUnderSymbiosis();
	public boolean isJavaClassUnderSymbiosis();
	public boolean isJavaMethodUnderSymbiosis();
	public boolean isNativeField();
	public boolean isNativeFarReference();
	public boolean isNativeFraction();
	public boolean isNativeNumber();
	public boolean isNativeIntrospectiveMirror();
	public boolean isNativeDelegateMethod();
	public boolean isMirage();
	
	public NATObject   asAmbientTalkObject() throws XTypeMismatch;
	public NATMirage   asMirage() throws XTypeMismatch;
	public NATNumber   asNativeNumber() throws XTypeMismatch;
	public NATFraction asNativeFraction() throws XTypeMismatch;
	public NATText     asNativeText() throws XTypeMismatch;
	public NATTable    asNativeTable() throws XTypeMismatch;
	public NATBoolean  asNativeBoolean() throws XTypeMismatch;
	public NATNumeric  asNativeNumeric() throws XTypeMismatch;
	public NATFarReference asNativeFarReference() throws XTypeMismatch;
	public NATIntrospectiveMirror asNativeIntrospectiveMirror() throws XTypeMismatch;
	public DelegateMethod asNativeDelegateMethod() throws XTypeMismatch;
	public JavaObject  asJavaObjectUnderSymbiosis() throws XTypeMismatch;
	public JavaClass   asJavaClassUnderSymbiosis() throws XTypeMismatch;
	public JavaMethod  asJavaMethodUnderSymbiosis() throws XTypeMismatch;
	//egb added for pool
	public NATRemoteFarRef asNativeRemoteFarReference() throws XTypeMismatch;
	public NATOutboxLetter asNativeOutboxLetter() throws XTypeMismatch;

}
