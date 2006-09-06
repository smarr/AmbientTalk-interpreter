/**
 * AmbientTalk/2 Project
 * OBJLexicalRoot.java created on 8-aug-2006 at 16:51:10
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
package edu.vub.at.objects.natives;

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XUndefinedField;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NATMirrorFactory;

/**
 * @author tvc
 *
 * An instance of the class OBJLexicalRoot represents the lexical root of an actor.
 * Since a lexical root is sealed (it cannot be modified) and contains no mutable fields,
 * it should be possible to share a singleton instance of this class among all actors.
 * 
 * The lexical root is an object containing globally visible AmbientTalk native methods.
 * Such methods include control structures such as if:then:else: and while:do:
 * but also object creation methods like object: and extend:with:
 * 
 * Furthermore, the lexical root is also responsible for ending recursive meta-level methods
 * such as lookup and assignField.
 * 
 * OBJLexicalRoot extends NATNil such that it inherits that class's ATObject protocol
 * to convert AmbientTalk invocations of a method m into Java base_m invocations.
 */
public final class OBJLexicalRoot extends NATNil {

	public static final OBJLexicalRoot _INSTANCE_ = new OBJLexicalRoot();
	
	/**
	 * Constructor made private for singleton design pattern
	 */
	private OBJLexicalRoot() { }
	
	/**
	 * Unlike a failed meta_select, a failed meta_lookup will not perform a
	 * 'doesNotUnderstand'-like extra invocation to intervene in the failed lookup.
	 * This is because a failed lookup is generally a programming error, meaning a
	 * variable was accessed which should have been lexically visible.
	 */
	public ATObject meta_lookup(ATSymbol selector) throws NATException {
		throw new XUndefinedField("variable access", selector.getText().asNativeText().javaValue);
	}
	
	public ATNil meta_assignVariable(ATSymbol selector, ATObject value) throws NATException {
		throw new XUndefinedField("variable assignment", selector.getText().asNativeText().javaValue);
	}
	
	/* ------------------------
	 * -- Control Structures --
	 * ------------------------ */
	
	/**
	 * The if:then: primitive, which calls back on the boolean using ifTrue:
	 * 
	 * usage:
	 *  if: booleanCondition then: { consequent }
	 * 
	 * @param cond a boolean object
	 * @param consequent a closure containing the code to execute if the boolean is true
	 * @return the result of invoking booleanCondition.ifTrue: { consequent }
	 * @throws NATException if raised inside the consequent closure.
	 */
	public ATObject base_if_then_(ATBoolean cond, ATClosure consequent) throws NATException {
		return cond.base_ifTrue_(consequent);
	}
	
	/**
	 * The if:then:else primitive, which calls back on the boolean using ifTrue:ifFalse:
	 * 
	 * usage:
	 *  if: booleanCondition then: { consequent } else: { alternative }
	 * 
	 * pseudo-implementation:
	 *  booleanCondition.ifTrue: { consequent } ifFalse: { alternative }
	 * 
	 * @param cond a boolean object
	 * @param consequent a closure containing the code to execute if the boolean is true
	 * @param alternative a closure containing the code to execute if the boolean is false
	 * @return the result of invoking booleanCondition.ifTrue: { consequent }
	 * @throws NATException if raised inside the consequent or alternative closure.
	 */
	public ATObject base_if_then_else_(ATBoolean cond, ATClosure consequent, ATClosure alternative) throws NATException {
		return cond.base_ifTrue_ifFalse_(consequent, alternative);
	}
	
	/**
	 * The while:do: primitive, which calls back on the closure using whileTrue:
	 * 
	 * usage:
	 *  while: { condition } do: { body }
	 * 
	 * pseudo-implementation:
	 *  { condition }.whileTrue: { body }
	 * 
	 * @param condition a closure expected to return a boolean object
	 * @param body a closure containing the code to execute as long as the condition closure returns true
	 * @return the result of invoking { body }.whileTrue: { condition }
	 * @throws NATException if raised inside the condition or body closures.
	 */
	public ATObject base_while_do_(ATClosure condition, ATClosure body) throws NATException {
		return condition.base_whileTrue_(body);
	}
	
	/**
	 * The foreach:in: primitive, which calls back on the table using each:
	 * 
	 * usage:
	 *  foreach: { |v| body } in: [ table ]
	 * 
	 * pseudo-implementation:
	 *  [ table ].each: { |v| body }
	 * 
	 * @param body a closure expected to take one argument to be applied to each element of the table
	 * @param tab a table to apply the iterator block to
	 * @return the result of invoking [ table ].each: { |v| body }
	 * @throws NATException if raised inside the iterator block.
	 */
	public ATObject base_foreach_in_(ATClosure body, ATTable tab) throws NATException {
		return tab.base_each_(body);
	}

	/**
	 * The do:if: primitive, which in Ruby terminology is a 'statement modifier'
	 * 
	 * usage:
	 *  do: { body } if: condition
	 * 
	 * pseudo-implementation:
	 *  condition.ifTrue: { body }
	 * 
	 * @param body a zero-argument closure to execute if the condition is true
	 * @param condition a boolean expression
	 * @return the result of invoking body if the condition is true or nil if the condition is false
	 * @throws NATException if raised inside the body block.
	 */
	public ATObject base_do_if_(ATClosure body, ATBoolean bool) throws NATException {
		return bool.base_ifTrue_(body);
	}
	
	/**
	 * The do:unless: primitive, which in Ruby terminology is a 'statement modifier'
	 * 
	 * usage:
	 *  do: { body } unless: condition
	 * 
	 * pseudo-implementation:
	 *  condition.ifFalse: { body }
	 * 
	 * @param body a zero-argument closure to execute if the condition is false
	 * @param condition a boolean expression
	 * @return the result of invoking body if the condition is false or nil if the condition is true
	 * @throws NATException if raised inside the body block.
	 */
	public ATObject base_do_unless_(ATClosure body, ATBoolean bool) throws NATException {
		return bool.base_ifFalse_(body);
	}
	
	/* -----------------------------
	 * -- Object Creation Methods --
	 * ----------------------------- */
	
	/**
	 * The extend:with: primitive, which delegates to the extend meta operation on the parent object. 
	 * 
	 * usage:
	 *  extend: anObject with: { someCode }
	 * 
	 * pseudo-implementation:
	 *  mirrorOf(anObject).extend(someCode)
	 *  
	 * @param parent the object to extend
	 * @param code a closure containing the code to extend the parent object with
	 * @return an object whose dynamic parent is an is-a link to the parent parameter
	 * @throws NATException if raised inside the code closure.
	 */
	public ATObject base_extend_with_(ATObject parent, ATClosure code) throws NATException {
		return parent.meta_extend(code);
	}
	
	/**
	 * The object: primitive, implemented as base-level code.
	 * object: expects to be passed a closure such that it can extract the correct
	 * scope to be used as the object's lexical parent.
	 * 
	 * usage:
	 *  object: { someCode }
	 *  
	 * pseudo-implementation:
	 *  { def obj := objectP.new(mirrorOf(someCode).context.lexicalScope);
	 *    mirrorOf(someCode).method.body.eval(contextP.new(obj, obj, nil));
	 *    obj }
	 *  
	 * @param code a closure containing both the code with which to initialize the object and the new object's lexical parent
	 * @return a new object whose dynamic parent is NIL, whose lexical parent is the closure's lexical scope, initialized by the closure's code
	 * @throws NATException if raised inside the code closure.
	 */
	public ATObject base_object_(ATClosure code) throws NATException {
		NATObject newObject = new NATObject(code.getContext().getLexicalScope());
		code.getMethod().getBody().meta_eval(new NATContext(newObject, newObject, NATNil._INSTANCE_));
		return newObject;
	}
	
	/**
	 * The share:with: primitive, which delegates to the share meta operation on the parent object. 
	 * 
	 * usage:
	 *  share: anObject with: { someCode }
	 * 
	 * pseudo-implementation:
	 *  mirrorOf(anObject).share(someCode)
	 * 
	 * @param parent the object to extend
	 * @param code a closure containing the code to extend the parent object with
	 * @return an object whose dynamic parent is a shares-a link to the parent parameter
	 * @throws NATException if raised inside the code closure.
	 */
	public ATObject base_share_with_(ATObject parent, ATClosure code) throws NATException {
		return parent.meta_share(code);
	}
	
	/**
	 * The reflect: primitive, which returns a mirror on an object.
	 * 
	 * usage:
	 *  reflect: anObject
	 * 
	 * pseudo-implementation:
	 *  at.mirrors.mirrorfactory.createMirror(anObject)
	 * 
	 * @param reflectee the object to reflect upon
	 * @return a mirror reflecting the given object
	 */
	public ATObject base_reflect_(ATObject reflectee) throws NATException {
		return NATMirrorFactory._INSTANCE_.base_createMirror(reflectee);
	}
	
	/* -------------------
	 * - Unary Operators -
	 * ------------------- */
	
	/**
	 * The unary ! primitive:
	 * !b == b.not()
	 */
	public ATBoolean base_opnot_(ATBoolean b) throws NATException {
		return b.base_not();
	}
	
	/**
	 * The unary - primitive:
	 * -NBR(n) == NBR(-n)
	 */
	public ATNumber base_opmns_(ATNumber n) throws NATException {
		return NATNumber.atValue(- n.asNativeNumber().javaValue);
	}

}