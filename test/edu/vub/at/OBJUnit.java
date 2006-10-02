/**
 * AmbientTalk/2 Project
 * OBJUnit.java created on Aug 22, 2006 at 11:32:30 AM
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
package edu.vub.at;

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XParseError;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATText;
import edu.vub.at.objects.mirrors.JavaClosure;
import edu.vub.at.objects.natives.NATClosure;
import edu.vub.at.objects.natives.NATContext;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.parser.NATParser;

import junit.framework.Assert;

/**
 * OBJUnit is a preliminary version of a unit test framework to be used in AmbientTalk.
 * It contains a set of methods comparable to (and translated to) the methods offered
 * by the JUnit framework. 
 *
 * @author smostinc
 */
public class OBJUnit extends NATNil {
		
	/**
	 * Default instance : used in general to store in the at dictionary. New 
	 * instances can be made using the unittest: constructor.
	 */
	public static final OBJUnit _INSTANCE_ = new OBJUnit();
	
	private ATContext ctx_ = new NATContext(
			OBJUnit._INSTANCE_,
			OBJUnit._INSTANCE_, 
			NATNil._INSTANCE_);
	
	private OBJUnit() { }
	
	public NATNil base_echo_(ATObject message) throws XTypeMismatch {
		System.out.println(message.meta_print().javaValue);
		return NATNil._INSTANCE_;
	};
	
	
	public NATNil base_fail()  {
		Assert.fail();
		return NATNil._INSTANCE_;
	};
	
	public NATNil base_fail_(NATText description)  {
		Assert.fail(description.javaValue);
		return NATNil._INSTANCE_;
	};
		
	
	public NATNil base_success() {
		return NATNil._INSTANCE_;
	};
	
	public NATNil base_assert_equals_(ATObject expected, ATObject actual) {
		Assert.assertEquals(expected, actual);
		return NATNil._INSTANCE_;
	}
	
	public ATObject meta_evaluate(ATText source) {
        try {
			ATAbstractGrammar ast = NATParser._INSTANCE_.base_parse(source);
			return ast.meta_eval(ctx_);
		} catch (XParseError e) {
			Assert.fail("Parse error: "+e.getMessage());
		} catch (NATException e) {
			Assert.fail("Eval error: "+e.getMessage());
		}
		return null;
	}
	
	public NATNil base_assert_evaluatesTo(ATText source, ATObject expected) {
		ATObject actual = meta_evaluate(source);
		if(actual != null) {
			this.base_assert_equals_(expected, actual);
		} 
		return NATNil._INSTANCE_;
	}
	
	public NATNil base_assert_printsTo(ATText source, ATObject expected) {
		ATObject actual = meta_evaluate(source);
		try {
			if(actual != null) {
				this.base_assert_equals_(expected, actual.meta_print());
			}
		} catch (XTypeMismatch e) {
			Assert.fail("Value cannot be represented in a textual format : " + e);
		} 
		return NATNil._INSTANCE_;
	}
	/**
	 * The unittest: primitive, implemented as base-level code.
	 * unit: expects to be passed a closure such that it can extract the correct
	 * scope to be used as the object's lexical parent.
	 * 
	 * usage:
	 *  unittest: { someCode }
	 *  
	 * pseudo-implementation:
	 *  { def obj := objectP.new(at.unit, mirrorOf(someCode).context.lexicalScope);
	 *    mirrorOf(someCode).method.body.eval(contextP.new(obj, obj, at.unit));
	 *    obj }
	 *  
	 * @param code a closure containing both the code with which to initialize the object and the new object's lexical parent
	 * @return a new object whose dynamic parent is NIL, whose lexical parent is the closure's lexical scope, initialized by the closure's code
	 * @throws NATException if raised inside the code closure.
	 */
	public ATObject base_unittest_(ATClosure code) throws NATException {
		OBJUnit clone = new OBJUnit();
		NATObject extension = new NATObject(
				/* dynamic parent */
				clone,
				/* lexical parent */
				code.getContext().getLexicalScope(),
				/* parent pointer type */
				NATObject._SHARES_A_);
		
		clone.ctx_ = new NATContext(extension, extension, clone);
		
		ATAbstractGrammar body = code.getMethod().getBodyExpression();
		body.meta_eval(clone.ctx_);
		
		return extension;
	}
}


