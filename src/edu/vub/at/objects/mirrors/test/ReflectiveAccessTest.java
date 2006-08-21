/**
 * AmbientTalk/2 Project
 * InvocationTest.java created on Aug 11, 2006 at 11:12:57 PM
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

package edu.vub.at.objects.mirrors.test;

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XParseError;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.mirrors.JavaClosure;
import edu.vub.at.objects.mirrors.NATMirrorFactory;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATClosure;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.at.parser.NATParser;

import junit.framework.TestCase;

/**
 * @author smostinc
 * 
 * ReflectiveAccessTest is a superclass to a framework of test cases geared towards
 * exploiting the reflective machinery provided in the mirrors package to explore
 * whether the mechanism works correctly and therefore allows one to access and 
 * invoke java fields and methods from within AmbientTalk (both at the base and 
 * meta-level) and to access and invoke Ambienttalk fields and methods through 
 * the mediation of Mirages. 
 * 
 * This file establishes a common vocabulary for these idiosyncratic tests and allows
 * code reuse for commonly used features.
 */
public class ReflectiveAccessTest extends TestCase {

	/* ---------------------------
	 * -- Auxiliary definitions --
	 * --------------------------- */	
	
	protected final NATClosure fail = new JavaClosure(null)  {
		public ATObject meta_apply(ATTable arguments) throws NATException {
			fail();
			return NATNil._INSTANCE_;
		}
	};
	
	protected final NATClosure success = new JavaClosure(null) {
		public ATObject meta_apply(ATTable arguments) throws NATException {
			return NATNil._INSTANCE_;
		}		
	};
	
	protected final NATClosure symbol = new JavaClosure(null) {
		public ATObject meta_apply(ATTable arguments) throws NATException {
			return AGSymbol.alloc(arguments.base_at(NATNumber.ONE).asNativeText());
		}				
	};
	
	protected final ATBoolean True		= NATBoolean._TRUE_;
	protected final ATBoolean False		= NATBoolean._FALSE_;
	
	protected ATObject lexicalRoot		= null;
	
	protected ATTable closures 			= new NATTable(
			new ATObject[] { fail, fail, success });
	
	protected void evaluateInput(String input, ATContext ctx) throws NATException {
		try {
			ATAbstractGrammar ag = NATParser._INSTANCE_.base_parse(NATText.atValue(input));
			
			// Evaluate the corresponding tree of ATAbstractGrammar objects
			ag.meta_eval(ctx);
		} catch(XParseError e) {
			e.printStackTrace();
			fail("exception: "+e);
		}
	}
	
	/**
	 * Initializes the lexical root for the purpose of this test.
	 */
	protected void setUp() throws Exception {
		lexicalRoot = new NATObject(NATNil._INSTANCE_);
		lexicalRoot.meta_defineField(AGSymbol.alloc("success"), success);
		lexicalRoot.meta_defineField(AGSymbol.alloc("fail"), fail);
		lexicalRoot.meta_defineField(AGSymbol.alloc("symbol"), symbol);
		lexicalRoot.meta_defineField(AGSymbol.alloc("true"), True);
		lexicalRoot.meta_defineField(AGSymbol.alloc("false"), False);
		lexicalRoot.meta_defineField(AGSymbol.alloc("closures"), closures);
		
		ATObject mirrors = new NATObject(lexicalRoot);
		mirrors.meta_defineField(AGSymbol.alloc("Factory"), NATMirrorFactory._INSTANCE_);
		
		ATObject natives = new NATObject(lexicalRoot);
		natives.meta_defineField(AGSymbol.alloc("Context"), NATMirrorFactory._INSTANCE_);
		
		ATObject at = new NATObject(lexicalRoot);
		at.meta_defineField(AGSymbol.alloc("mirrors"), mirrors);
		at.meta_defineField(AGSymbol.alloc("natives"), natives);
		
		lexicalRoot.meta_defineField(AGSymbol.alloc("at"), at);
		lexicalRoot.meta_defineField(AGSymbol.alloc("root"), lexicalRoot);
	}

}
