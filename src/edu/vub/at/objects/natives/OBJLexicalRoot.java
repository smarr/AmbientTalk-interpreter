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

import edu.vub.at.actors.ATActorMirror;
import edu.vub.at.actors.natives.ELActor;
import edu.vub.at.actors.natives.ELVirtualMachine;
import edu.vub.at.actors.natives.NATActorMirror;
import edu.vub.at.actors.natives.Packet;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATHandler;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATStripe;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATText;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NATIntercessiveMirror;
import edu.vub.at.objects.mirrors.NATIntrospectiveMirror;
import edu.vub.at.objects.mirrors.NATMirage;
import edu.vub.at.objects.mirrors.OBJMirrorRoot;
import edu.vub.at.parser.NATParser;

/**
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
 * 
 * Note that OBJLexicalRoot is a 'sentinel' class. The actual object bound to the
 * lexical root of an actor will be a normal NATObject which is assumed to be 'nested' in this instance.
 * This empty object is local to each actor and is mutable.
 * 
 * @author smostinc
 * @author tvcutsem
 */
public final class OBJLexicalRoot extends NATByCopy {
	
	/**
	 * The singleton instance of the sentinel lexical root
	 */
	static public final OBJLexicalRoot _INSTANCE_ = new OBJLexicalRoot();
	
	/**
	 * Constructor made private for singleton design pattern
	 */
	private OBJLexicalRoot() { }
	
	/* ----------------------
	 * -- Global variables --
	 * ---------------------- */
	
	/**
	 * nil
	 */
	public ATNil base_getNil() {
		return NATNil._INSTANCE_;
	}
	
	/**
	 * true
	 */
	public ATBoolean base_getTrue() {
		return NATBoolean._TRUE_;
	}
	
	/**
	 * false
	 */
	public ATBoolean base_getFalse() {
		return NATBoolean._FALSE_;
	}
	
	/**
	 * '/' (the global namespace)
	 * '/' is an alias for 'lobby'
	 */
	public ATObject base_get_opdiv_() {
		return base_getLobby();
	}
	
	/**
	 * lobby (the global namespace initialized using the objectpath)
	 */
	public ATObject base_getLobby() {
		return Evaluator.getLobbyNamespace();
	}
	
	/**
	 * root (the global scope)
	 */
	public ATObject base_getRoot() {
		return Evaluator.getGlobalLexicalScope();
	}
	
	/**
	 * jlobby (the Java class package root, initialized using the Java classpath)
	 */
	public ATObject base_getJlobby() {
		return Evaluator.getJLobbyRoot();
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
	 * @throws InterpreterException if raised inside the consequent closure.
	 */
	public ATObject base_if_then_(ATBoolean cond, ATClosure consequent) throws InterpreterException {
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
	 * @throws InterpreterException if raised inside the consequent or alternative closure.
	 */
	public ATObject base_if_then_else_(ATBoolean cond, ATClosure consequent, ATClosure alternative) throws InterpreterException {
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
	 * @throws InterpreterException if raised inside the condition or body closures.
	 */
	public ATObject base_while_do_(ATClosure condition, ATClosure body) throws InterpreterException {
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
	 * @throws InterpreterException if raised inside the iterator block.
	 */
	public ATObject base_foreach_in_(ATClosure body, ATTable tab) throws InterpreterException {
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
	 * @throws InterpreterException if raised inside the body block.
	 */
	public ATObject base_do_if_(ATClosure body, ATBoolean condition) throws InterpreterException {
		return condition.base_ifTrue_(body);
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
	 * @throws InterpreterException if raised inside the body block.
	 */
	public ATObject base_do_unless_(ATClosure body, ATBoolean condition) throws InterpreterException {
		return condition.base_ifFalse_(body);
	}
	
	/**
	 * The let: primitive, which allows for the easy creation of temporary local variables.
	 * This primitive should be used in conjunction with a closure that declares optional
	 * parameters. Because the closure will be invoked with zero arguments, all of the
	 * parameters will be given their corresponding default initial value. The parameters
	 * are defined local to the closure's body.
	 * 
	 * Note: this let behaves like Scheme's let* and letrec, i.e. the following is legal:
	 * let: { |var1 := value1, var2 := var1, var3 := { ... var3() ... } | ... }
	 * 
	 * usage:
	 *  let: { |var := value| body }
	 * 
	 * pseudo-implementation:
	 *  def let: closure { closure() }
	 * 
	 * @param body a closure which is supposed to declare some optional parameters
	 * @return the result of invoking the body closure
	 * @throws InterpreterException if raised inside the body block.
	 */
	public ATObject base_let_(ATClosure body) throws InterpreterException {
		return body.base_apply(NATTable.EMPTY);
	}
	
	/* ------------------------------------------
	 * -- Actor Creation and accessing Methods --
	 * ------------------------------------------ */
	
	/**
	 * actor: { code }
	 *  == actor: { code } mirroredBy: <default actor mirror>
	 */
	public ATObject base_actor_(ATClosure code) throws InterpreterException {
		ATObject isolate = base_isolate_(code);
		Packet serializedIsolate = new Packet("behaviour", isolate);
		ELVirtualMachine host = ELVirtualMachine.currentVM();
		return NATActorMirror.atValue(host, serializedIsolate, new NATActorMirror(host));
	}
	
	/**
	 * actor: { code } mirroredBy: actorMirror
	 *  => far reference to the behaviour of the new actor
	 * REPLACED BY install: primitive!
	 */
	/*public ATObject base_actor_mirroredBy_(ATClosure code, ATActorMirror mirror) throws InterpreterException {
		ATObject isolate = base_isolate_(code);
		Packet serializedIsolate = new Packet("behaviour", isolate);
		Packet serializedMirror = new Packet("mirror", mirror);
		ELVirtualMachine host = ELVirtualMachine.currentVM();
		return NATActorMirror.atValue(host, serializedIsolate, serializedMirror);
	}*/
	
	/**
	 * actor => a reference to a mirror on the current actor
	 */
	public ATActorMirror base_getActor() throws InterpreterException {
		return ELActor.currentActor().getActorMirror();
	}
	
	/**
	 * isolate: { code }
	 *  => create an isolate object
	 */
	public ATObject base_isolate_(ATClosure code) throws InterpreterException {
		NATIsolate isolate = new NATIsolate();
		NATTable copiedBindings = Evaluator.evalMandatoryPars(
				code.base_getMethod().base_getParameters(),
				code.base_getContext());
		code.base_applyInScope(copiedBindings, isolate);
		return isolate;
	}
	
	/**
	 * export: object as: topic
	 *  => object becomes discoverable by objects in other actors via topic
	 * returns a publication object that can be used to cancel the export
	 */
	public ATObject base_export_as_(ATObject object, ATSymbol topic) throws InterpreterException {
		return ELActor.currentActor().getActorMirror().base_provide(topic, object);
	}
	
	/**
	 * when: topic discovered: { code }
	 *  => when an object is exported by another actor under topic, trigger the code
	 * returns a subscription object that can be used to cancel the handler
	 */
	public ATObject base_when_discovered_(ATSymbol topic, ATClosure handler) throws InterpreterException {
		return ELActor.currentActor().getActorMirror().base_require(topic, handler);
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
	 * @throws InterpreterException if raised inside the code closure.
	 */
	public ATObject base_extend_with_(ATObject parent, ATClosure code) throws InterpreterException {
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
	 * @throws InterpreterException if raised inside the code closure.
	 */
	public ATObject base_object_(ATClosure code) throws InterpreterException {
		NATObject newObject = new NATObject(code.base_getContext().base_getLexicalScope());
		code.base_applyInScope(NATTable.EMPTY, newObject);
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
	 * @throws InterpreterException if raised inside the code closure.
	 */
	public ATObject base_share_with_(ATObject parent, ATClosure code) throws InterpreterException {
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
	public ATObject base_reflect_(ATObject reflectee) throws InterpreterException {
		return NATIntrospectiveMirror.atValue(reflectee);
	}
	
	/**
	 * The clone: primitive, which returns a clone of an object.
	 * 
	 * usage:
	 *  clone: anObject
	 * 
	 * pseudo-implementation:
	 *  (reflect: anObject).clone()
	 * 
	 * @param original the object to copy
	 * @return a clone of the given object
	 */
	public ATObject base_clone_(ATObject original) throws InterpreterException {
		return original.meta_clone();
	}
	
	
	/**
	 * The mirror: primitive, which allows creating custom mirrors which can be used
	 * to allow intercessive reflection on objects created from this mirror.
	 * 
	 * usage:
	 *  mirror: { someCode } 
	 * 
	 * pseudo-implementation:
	 *  defaultMirror.extend(somecode)
	 * 
	 * @param code a closure containing both the code with which to initialize the mirror and the new mirror's lexical parent
	 * @return a new mirror containing the specified definitions
	 */
	public ATObject base_mirror_(ATClosure code) throws InterpreterException {
		// As this is a base_method, its result will be downed. Since we explicitly want to
		// return the newly created NATIntercessiveMirror, it needs to be upped explicitly.
		// TODO: discuss with Stijn: base_ methods don't down their ret.val
		return OBJMirrorRoot._INSTANCE_.meta_extend(code);
	}
	
	/**
	 * object: { code } mirroredBy: mirror
	 *  => return an object mirage initialized with code
	 */
	public ATObject base_object_mirroredBy_(ATClosure code, NATIntercessiveMirror mirror) throws InterpreterException {
		
		// Initialise a new pair of mirror-mirage : note that we don't use clone here
		NATIntercessiveMirror mirrorClone = mirror.magic_clone();
		NATMirage newMirage = new NATMirage(code.base_getContext().base_getLexicalScope(), mirrorClone);
		mirrorClone.setBase(newMirage);
		
		code.base_applyInScope(NATTable.EMPTY, newMirage);
		
		return newMirage;
	}
	
	public ATObject base_extend_with_mirroredBy_(ATObject parent, ATClosure code, NATIntercessiveMirror mirror) throws InterpreterException {
		
		// Initialise a new pair of mirror-mirage : note that we don't use clone here
		NATIntercessiveMirror mirrorClone = mirror.magic_clone();
		NATMirage newMirage = new NATMirage(parent, code.base_getContext().base_getLexicalScope(), mirrorClone, NATObject._IS_A_);
		mirrorClone.setBase(newMirage);
		
		code.base_applyInScope(NATTable.EMPTY, newMirage);
		
		return newMirage;
	}
	
	public ATObject base_share_with_mirroredBy_(ATObject parent, ATClosure code, NATIntercessiveMirror mirror) throws InterpreterException {
		
		// Initialise a new pair of mirror-mirage : note that we don't use clone here
		NATIntercessiveMirror mirrorClone = mirror.magic_clone();
		NATMirage newMirage = new NATMirage(parent, code.base_getContext().base_getLexicalScope(), mirrorClone, NATObject._SHARES_A_);
		mirrorClone.setBase(newMirage);
		
		code.base_applyInScope(NATTable.EMPTY, newMirage);
		
		return newMirage;
	}
	
	/* -------------------
	 * -- Stripe Support -
	 * ------------------- */
	
	/**
	 * is: object stripedWith: stripe
	 * => returns true if the given object is striped with the given stripe
	 */
	public ATBoolean base_is_stripedWith_(ATObject object, ATStripe stripe) throws InterpreterException {
		return object.meta_isStripedWith(stripe);
	}
	
	/**
	 * stripesOf: object
	 * => returns all of the stripes of an object
	 */
	public ATTable base_stripesOf_(ATObject object) throws InterpreterException {
		return object.meta_getStripes();
	}
	
	/**
	 * object: { code } stripedWith: [ s1, s2, ... ]
	 * => creates a new object tagged with the given stripes
	 */
	public ATObject base_object_stripedWith_(ATClosure code, ATTable stripes) throws InterpreterException {
		ATObject[] unwrapped = stripes.asNativeTable().elements_;
		ATStripe[] unwrappedStripes = new ATStripe[unwrapped.length];
		for (int i = 0; i < unwrappedStripes.length; i++) {
			unwrappedStripes[i] = unwrapped[i].base_asStripe();
		}
		NATObject newObject = new NATObject(code.base_getContext().base_getLexicalScope(), unwrappedStripes);
		code.base_applyInScope(NATTable.EMPTY, newObject);
		return newObject;
	}
	
	/* -------------------------------
	 * -- Exception Handling Support -
	 * ------------------------------- */
	
	/**
	 * try: { tryBlock } usingHandlers: [ handler1, handler2, ... ]
	 * 
	 * Applies the given closure (to []) and handles exceptions using the given exception handlers.
	 * This is the most general means of doing exception handling in AmbientTalk/2.
	 * 
	 * The handlers given in the handler table represent first-class handler objects, which should respond to the 'canHandle' message.
	 */
	public ATObject base_try_usingHandlers_(ATClosure tryBlock, ATTable exceptionHandlers) throws InterpreterException {
		try {
			return tryBlock.base_apply(NATTable.EMPTY);
		} catch(InterpreterException e) {
			ATObject[] handlers = exceptionHandlers.asNativeTable().elements_;
			
			// find the appropriate handler
			for (int i = 0; i < handlers.length; i++) {
				ATHandler handler = handlers[i].base_asHandler();
				ATObject exc = e.getAmbientTalkRepresentation();
				if (handler.base_canHandle(exc).asNativeBoolean().javaValue) {
					return handler.base_handle(exc);
				};	
			}
			
			// no handler found, re-throw the exception
			throw e;
		}
	}
	
	/**
	 * try: { tryBlock} using: handler
	 * 
	 * Ad-hoc code for one exception handler
	 */
	public ATObject base_try_using_(ATClosure tryBlock, ATHandler handler) throws InterpreterException {
		try {
			return tryBlock.base_apply(NATTable.EMPTY);
		} catch(InterpreterException e) {
			ATObject exc = e.getAmbientTalkRepresentation();
			if (handler.base_canHandle(exc).asNativeBoolean().javaValue) {
				return handler.base_handle(exc);
			} else {
				throw e;
			}
		}
	}
	
	/**
	 * try: { tryBlock} using: handler1 using: handler2
	 * 
	 * Ad-hoc code for two exception handlers
	 */
	public ATObject base_try_using_using_(ATClosure tryBlock, ATHandler hdl1, ATHandler hdl2) throws InterpreterException {
		return base_try_usingHandlers_(tryBlock, NATTable.atValue(new ATObject[] { hdl1, hdl2 }));
	}
	
	/**
	 * try: { tryBlock} using: hdl1 using: hdl2 using: hdl3
	 * 
	 * Ad-hoc code for three exception handlers
	 */
	public ATObject base_try_using_using_using_(ATClosure tryBlock, ATHandler hdl1, ATHandler hdl2, ATHandler hdl3) throws InterpreterException {
		return base_try_usingHandlers_(tryBlock, NATTable.atValue(new ATObject[] { hdl1, hdl2, hdl3 }));
	}
	
	/**
	 * try: { tryBlock} catch: prototype using: { |e| replacementCode }
	 * 
	 * 'Syntactic sugar' for one in-line handler
	 */
	public ATObject base_try_catch_using_(ATClosure tryBlock, ATObject filter, ATClosure replacementCode) throws InterpreterException {
		return base_try_using_(tryBlock, new NATHandler(filter, replacementCode));
	}
	
	/**
	 * try: {
	 *   tryBlock
	 * } catch: prototype using: { |e|
	 *   replacementCode
	 * } catch: prototype2 using: { |e|
	 *   replacementCode2
	 * }
	 * 
	 * 'Syntactic sugar' for two in-line handlers
	 */
	public ATObject base_try_catch_using_catch_using_(ATClosure tryBlock,
			                						   ATObject filter1, ATClosure hdl1,
			                						   ATObject filter2, ATClosure hdl2) throws InterpreterException {
		return base_try_using_using_(tryBlock, new NATHandler(filter1, hdl1), new NATHandler(filter2, hdl2));
	}
	
	/**
	 * try: {
	 *   tryBlock
	 * } catch: prototype using: { |e|
	 *   replacementCode
	 * } catch: prototype2 using: { |e|
	 *   replacementCode2
	 * } catch: prototype3 using: { |e|
	 *   replacementCode3
	 * }
	 * 
	 * 'Syntactic sugar' for three in-line handlers
	 */
	public ATObject base_try_catch_using_catch_using_catch_using_(ATClosure tryBlock,
															   ATObject filter1, ATClosure hdl1,
															   ATObject filter2, ATClosure hdl2,
															   ATObject filter3, ATClosure hdl3) throws InterpreterException {
		return base_try_using_using_using_(tryBlock, new NATHandler(filter1, hdl1), new NATHandler(filter2, hdl2), new NATHandler(filter3, hdl3));
	}
	
	/**
	 * handle: prototype with: { |e| replacementCode }
	 * 
	 * Creates a first-class handler from a filter prototype and some handler code.
	 */
	public ATObject base_handle_with_(ATObject filter, ATClosure replacementCode) {
		return new NATHandler(filter, replacementCode);
	}
	
	/**
	 * raise: exception
	 * 
	 * Raises an exception which can be caught by dynamically installed try-catch-using blocks.
	 */
	public ATNil base_raise_(ATObject anExceptionObject) throws InterpreterException {
		throw anExceptionObject.asNativeException().getWrappedException();
	}
	
	/* --------------------
	 * -- Unary Operators -
	 * -------------------- */
	
	/**
	 * The unary ! primitive:
	 * !b == b.not()
	 */
	public ATBoolean base__opnot_(ATBoolean b) throws InterpreterException {
		return b.base_not();
	}
	
	/**
	 * The unary - primitive:
	 * -NBR(n) == NBR(-n)
	 */
	public ATNumber base__opmns_(ATNumber n) throws InterpreterException {
		return NATNumber.atValue(- n.asNativeNumber().javaValue);
	}
	
	/**
	 * The unary + primitive:
	 * +NBR(n) == NBR(n)
	 */
	public ATNumber base__oppls_(ATNumber n) throws InterpreterException {
		return n;
	}
	
	/* -------------------
	 * -- Miscellaneous --
	 * ------------------- */
	
	/**
	 * read: "text" => parses the given string into an AST
	 */
	public ATAbstractGrammar base_read_(ATText source) throws InterpreterException {
		return NATParser._INSTANCE_.base_parse(source);
	}
	
	/**
	 * eval: ast in: obj => evaluates the given AST in the context of the given object, returning its value
	 */
	public ATObject base_eval_in_(ATAbstractGrammar ast, ATObject obj) throws InterpreterException {
		return ast.meta_eval(new NATContext(obj, obj));
	}

	/**
	 * print: expression => string representing the expression
	 */
	public ATText base_print_(ATObject obj) throws InterpreterException {
		return obj.meta_print();
	}
	
	// custom implementation of the default object methods == and new
	// the reason for this custom implementation: during the execution
	// of these methods, 'this' should refer to the global lexical scope object (the root),
	// not to the OBJLexicalRoot instance.
	// hence, when invoking one of these methods lexically, the receiver is always 'root'
	// For example, "==(obj)" is equivalent to "root == obj" (or "root.==(obj)")

    public ATBoolean base__opeql__opeql_(ATObject comparand) {
        return Evaluator.getGlobalLexicalScope().base__opeql__opeql_(comparand);
    }
	
    /**
     * Invoking root.new(args) results in an exception for reasons of safety.
     * We could also have opted to simply return 'root' (i.e. make it a singleton)
     * 
     * The reason for being conservative and throwing an exception is
     * that when writing 'new(args)' in an object that does not implement
     * new itself, this will implicitly lead to invoking root.new(args), not
     * self.new(args), as the user will probably have intended.
     * To catch such bugs quickly, root.new throws an exception rather than
     * silently returning the root itself.
     */
    public ATObject base_new(ATObject[] initargs) throws InterpreterException {
    	    // root.new(@initargs)
	    return Evaluator.getGlobalLexicalScope().base_new(initargs);
    }
    
	/**
	 * After deserialization, ensure that the lexical root remains unique.
	 */
	public ATObject meta_resolve() throws InterpreterException {
		return OBJLexicalRoot._INSTANCE_;
	}
    
}
