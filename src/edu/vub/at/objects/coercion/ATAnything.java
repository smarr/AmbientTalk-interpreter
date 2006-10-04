/**
 * AmbientTalk/2 Project
 * ATAnything.java created on Sep 29, 2006 at 8:07:40 AM
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

import edu.vub.at.actors.ATActor;
import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.actors.ATDevice;
import edu.vub.at.actors.ATMailbox;
import edu.vub.at.actors.ATResolution;
import edu.vub.at.actors.ATVirtualMachine;
import edu.vub.at.actors.beholders.ATBeholder;
import edu.vub.at.actors.events.ATEvent;
import edu.vub.at.actors.grammar.ATAsyncMessageCreation;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATFraction;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATMethodInvocation;
import edu.vub.at.objects.ATMirror;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATText;
import edu.vub.at.objects.grammar.ATApplication;
import edu.vub.at.objects.grammar.ATAssignField;
import edu.vub.at.objects.grammar.ATAssignTable;
import edu.vub.at.objects.grammar.ATAssignVariable;
import edu.vub.at.objects.grammar.ATAssignment;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATClosureLiteral;
import edu.vub.at.objects.grammar.ATDefField;
import edu.vub.at.objects.grammar.ATDefMethod;
import edu.vub.at.objects.grammar.ATDefTable;
import edu.vub.at.objects.grammar.ATDefinition;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATMessageCreation;
import edu.vub.at.objects.grammar.ATMessageSend;
import edu.vub.at.objects.grammar.ATMethodInvocationCreation;
import edu.vub.at.objects.grammar.ATMultiAssignment;
import edu.vub.at.objects.grammar.ATMultiDefinition;
import edu.vub.at.objects.grammar.ATQuote;
import edu.vub.at.objects.grammar.ATSelection;
import edu.vub.at.objects.grammar.ATSplice;
import edu.vub.at.objects.grammar.ATStatement;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.grammar.ATTabulation;
import edu.vub.at.objects.grammar.ATUnquote;
import edu.vub.at.objects.grammar.ATUnquoteSplice;

/**
 * @deprecated scheduled for deletion, use coercers instead.
 * 
 * ATAnything is an interface extending all AT language value interfaces.
 * This makes it the top of the AT type lattice of which ATObject is the 
 * bottom element. 
 * 
 * ATAnything is used to allow objects to act in the place of any primitive
 * language value they want.
 *
 * @author smostinc
 */
public interface ATAnything 
		extends ATObject,  ATAbstractGrammar,  ATActor,  ATApplication,
		ATAssignField, ATAssignment,  ATAssignTable,  ATAssignVariable, 
		ATAsyncMessage,  ATAsyncMessageCreation,  ATBegin,  ATBeholder, 
		ATBoolean, ATClosure, ATClosureLiteral, ATContext,  ATDefField, 
		ATDefinition,   ATDefMethod,   ATDefTable,  ATDevice,  ATEvent,
		ATExpression,   ATField,   ATFraction,   ATMailbox,  ATMessage,
		ATMessageCreation, ATMessageSend, ATMethod, ATMethodInvocation, 
		ATMethodInvocationCreation,     ATMirror,    ATMultiAssignment, 
		ATMultiDefinition,  ATNil,  ATNumber,  ATQuote,   ATResolution, 
		ATSelection,   ATSplice,   ATStatement, 
		ATSymbol,    ATTable,    ATTabulation,    ATText,    ATUnquote, 
		ATUnquoteSplice, ATVirtualMachine {

}
