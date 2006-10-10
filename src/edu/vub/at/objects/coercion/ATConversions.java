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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMessage;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATMirror;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATBegin;
import edu.vub.at.objects.grammar.ATDefinition;
import edu.vub.at.objects.grammar.ATExpression;
import edu.vub.at.objects.grammar.ATMessageCreation;
import edu.vub.at.objects.grammar.ATSplice;
import edu.vub.at.objects.grammar.ATStatement;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.grammar.ATUnquoteSplice;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATFraction;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATNumeric;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;

/**
 * @author smostinc
 *
 * ATConversions is an interface defining all conversion functions between different
 * types of ambienttalk language elements. 
 */
public interface ATConversions {

	public boolean isClosure();
	public boolean isSymbol();
	public boolean isTable();
	public boolean isBoolean();
	public boolean isCallFrame();
	public boolean isUnquoteSplice();
	public boolean isSplice();
	public boolean isMethod();
	public boolean isMessageCreation();
	public ATBoolean base_isMirror();
	
	public ATClosure   asClosure() throws XTypeMismatch;
	public ATSymbol    asSymbol() throws XTypeMismatch;
	public ATTable     asTable() throws XTypeMismatch;
	public ATBoolean   asBoolean() throws XTypeMismatch;
	public ATNumber    asNumber() throws XTypeMismatch;
	public ATMessage   asMessage() throws XTypeMismatch;
	public ATField     asField() throws XTypeMismatch;
	public ATMethod    asMethod() throws XTypeMismatch;
	public ATMirror    asMirror() throws XTypeMismatch;
	
	// Abstract Grammar Elements
	
	public ATStatement  asStatement() throws XTypeMismatch;
	public ATDefinition asDefinition() throws XTypeMismatch;
	public ATExpression asExpression() throws XTypeMismatch;
	public ATBegin      asBegin() throws XTypeMismatch;
	public ATMessageCreation asMessageCreation() throws XTypeMismatch;
	public ATUnquoteSplice asUnquoteSplice() throws XTypeMismatch;
	public ATSplice asSplice() throws XTypeMismatch;
	
	// Native Value Elements
	
	public boolean isNativeBoolean() throws XTypeMismatch;
	public boolean isNativeText();
	public boolean isAmbientTalkObject();
	
	public NATObject   asAmbientTalkObject() throws XTypeMismatch;
	public NATNumber   asNativeNumber() throws XTypeMismatch;
	public NATFraction asNativeFraction() throws XTypeMismatch;
	public NATText     asNativeText() throws NATException;
	public NATTable    asNativeTable() throws XTypeMismatch;
	public NATBoolean  asNativeBoolean() throws XTypeMismatch;
	public NATNumeric  asNativeNumeric() throws XTypeMismatch;
	public NATException asNativeException() throws XTypeMismatch;
}
