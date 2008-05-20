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
import edu.vub.at.actors.ATFarReference;
import edu.vub.at.actors.natives.ELActor;
import edu.vub.at.actors.natives.ELVirtualMachine;
import edu.vub.at.actors.natives.NATFarReference;
import edu.vub.at.actors.natives.Packet;
import edu.vub.at.actors.net.OBJNetwork;
import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XUnassignableField;
import edu.vub.at.exceptions.XUndefinedSlot;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATHandler;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATNumeric;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATText;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATAssignmentSymbol;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NATMirage;
import edu.vub.at.objects.mirrors.NATMirrorRoot;
import edu.vub.at.parser.NATParser;

/**
 * The singleton instance of this class represents the lexical root of an actor.
 * Since this lexical root is constant (it cannot be modified) and contains no mutable fields,
 * it is possible to share a singleton instance of this class among all actors.
 * <p>
 * The lexical root is an object containing globally visible AmbientTalk native methods.
 * Such methods include control structures such as <tt>if:then:else:</tt>
 * but also object creation methods like <tt>object:</tt> and reflective constructs
 * like <tt>reflect:</tt>.
 * 
 * Furthermore, the lexical root is also the root of the lexical parent hierarchy for objects.
 * This means that this object's mirror is responsible for ending recursive meta-level methods
 * such as <tt>lookup</tt> and <tt>assignField</tt>.
 * <p>
 * Like any class whose instances represent native AmbientTalk objects, this class is a subclass
 * of {@link NativeATObject}. This means that this class can use the typical protocol of native objects
 * to implement base-level AmbientTalk methods as Java methods whose name is prefixed with
 * <tt>base_</tt>.
 * <p>
 * Note that OBJLexicalRoot is a <i>sentinel</i> class. The actual object bound to the
 * lexical root of an actor (accessible via the field <tt>root</tt> will be a normal
 * AmbientTalk object whose lexical parent is this object.
 * The real, empty, root object is local to each actor and is mutable. The definitions
 * from the <tt>init.at</tt> file are added to that object.
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
	
	/**
	 * The lexical root has a special lexical parent object which ends the recursion
	 * along the lexical lookup chain. These methods cannot be implemented
	 * directly in this class because this class still implements useful
	 * <tt>base_</tt> Java methods which have to be invoked by means of the
	 * implementations defined in {@link NativeATObject}.
	 */
	private final NativeATObject lexicalSentinel_ = new NATByCopy() {
		// METHODS THAT END THE LEXICAL LOOKUP CHAIN
		
		public ATObject impl_callAccessor(ATSymbol selector, ATTable arguments) throws InterpreterException {
			throw new XUndefinedSlot("variable access", selector.toString());
		}

		public ATObject impl_callMutator(ATAssignmentSymbol selector, ATTable arguments) throws InterpreterException {
			throw new XUnassignableField(selector.toString());
		}	
		
		public ATObject impl_callField(ATSymbol selector) throws InterpreterException {
			throw new XUndefinedSlot("variable access", selector.toString());
		}
		
		public ATClosure impl_lookupAccessor(final ATSymbol selector) throws InterpreterException {
			throw new XUndefinedSlot("accessor", selector.toString());
		}

		public ATClosure impl_lookupMutator(ATAssignmentSymbol selector) throws InterpreterException {
			throw new XUnassignableField(selector.toString());
		}

		public NATText meta_print() throws InterpreterException {
			return NATText.atValue("lexicalrootsentinel");
		}
	};
	
	public ATObject impl_lexicalParent() {
		return lexicalSentinel_;
	}
	
	/* -----------------------
	 * -- Primitive Methods --
	 * ----------------------- */
	
	/* ===============================================================================
	 * NOTE: the code below has been replaced by dedicated syntax and AST elements.
	 * However, the skeleton of this code may still prove useful in the future, if
	 * we ever plan to implement all base_ native methods as true AmbientTalk methods
	 * (i.e. as PrimitiveMethod instances).
	 * ===============================================================================
	 */
	
	
	/*
	private static final AGSymbol _IMPORT_NAME_ = AGSymbol.jAlloc("import:");
	private static final AGSymbol _IMPORT_ALIAS_NAME_ = AGSymbol.jAlloc("import:alias:");
	private static final AGSymbol _IMPORT_EXCLUDE_NAME_ = AGSymbol.jAlloc("import:exclude:");
	private static final AGSymbol _IMPORT_ALIAS_EXCLUDE_NAME_ = AGSymbol.jAlloc("import:alias:exclude:");
	
	private static final AGSymbol _SRC_PARAM_ = AGSymbol.jAlloc("sourceObject");
	private static final AGSymbol _ALIAS_PARAM_ = AGSymbol.jAlloc("aliases");
	private static final AGSymbol _EXCLUDE_PARAM_ = AGSymbol.jAlloc("exclude");
	*/
	
	
	/*protected static final PrimitiveMethod _PRIM_IMPORT_ = new PrimitiveMethod(_IMPORT_NAME_, NATTable.atValue(new ATObject[] { _SRC_PARAM_ })) {
		public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
			  ATObject sourceObject = arguments.base_at(NATNumber.ONE);
			  return performImport(sourceObject, ctx, new Hashtable(), OBJLexicalRoot.getDefaultExcludedSlots());
		}
	};*/
	
	/**
	 * def import: sourceObject alias: [ `oldname -> `newname , ... ]
	 */
	/*protected static final PrimitiveMethod _PRIM_IMPORT_ALIAS_ = new PrimitiveMethod(_IMPORT_ALIAS_NAME_, NATTable.atValue(new ATObject[] { _SRC_PARAM_, _ALIAS_PARAM_ })) {
		public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
			  ATObject sourceObject = arguments.base_at(NATNumber.ONE);
			  ATObject aliases = arguments.base_at(NATNumber.atValue(2));
			  return performImport(sourceObject, ctx, preprocessAliases(aliases.base_asTable()), OBJLexicalRoot.getDefaultExcludedSlots());
		}
	};*/
	
	/**
	 * def import: sourceObject excludes: [ `name1, `name2, ... ]
	 */
	/*protected static final PrimitiveMethod _PRIM_IMPORT_EXCLUDE_ = new PrimitiveMethod(_IMPORT_EXCLUDE_NAME_, NATTable.atValue(new ATObject[] { _SRC_PARAM_, _EXCLUDE_PARAM_ })) {
		public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
			  ATObject sourceObject = arguments.base_at(NATNumber.ONE);
			  ATObject exclusions = arguments.base_at(NATNumber.atValue(2));
			  return performImport(sourceObject, ctx, new Hashtable(), preprocessExcludes(exclusions.base_asTable()));
		}
	};*/
	
	/**
	 * def import: sourceObject alias: [ `oldname -> `newname, ... ] excludes: [ `name1, `name2, ... ]
	 */
	/*protected static final PrimitiveMethod _PRIM_IMPORT_ALIAS_EXCLUDE_ = new PrimitiveMethod(_IMPORT_ALIAS_EXCLUDE_NAME_,
			                                                                                 NATTable.atValue(new ATObject[] { _SRC_PARAM_, _ALIAS_PARAM_, _EXCLUDE_PARAM_ })) {
		public ATObject base_apply(ATTable arguments, ATContext ctx) throws InterpreterException {
			  ATObject sourceObject = arguments.base_at(NATNumber.ONE);
			  ATObject aliases = arguments.base_at(NATNumber.atValue(2));
			  ATObject exclusions = arguments.base_at(NATNumber.atValue(3));
			  return performImport(sourceObject, ctx, preprocessAliases(aliases.base_asTable()), preprocessExcludes(exclusions.base_asTable()));
		}
	};*/
	
	/**
	 * Invoked whenever a new true AmbientTalk object is created that should
	 * represent the root. This gives the lexical root a chance to install its
	 * primitive methods.
	 */
	/*public static void initializeRoot(NATObject root) {
		try {
			// add import: native
			root.meta_addMethod(_PRIM_IMPORT_);
			// add import:alias: native
			root.meta_addMethod(_PRIM_IMPORT_ALIAS_);
			// add import:exclude: native
			root.meta_addMethod(_PRIM_IMPORT_EXCLUDE_);
			// add import:alias:exclude: native
			root.meta_addMethod(_PRIM_IMPORT_ALIAS_EXCLUDE_);
		} catch (InterpreterException e) {
			Logging.Init_LOG.fatal("Failed to initialize the root!", e);
		}
	}*/
	
	/* ----------------------
	 * -- Global variables --
	 * ---------------------- */
	
	/**
	 * <tt>nil</tt> evaluates to the nil object, which is
	 * the empty, dynamic parent of all AmbientTalk objects. 
	 */
	public ATNil base_nil() {
		return OBJNil._INSTANCE_;
	}
	
	/**
	 * <tt>true</tt> evaluates to the unique boolean true object.
	 */
	public ATBoolean base_true() {
		return NATBoolean._TRUE_;
	}
	
	/**
	 * <tt>false</tt> evaluates to the unique boolean false object.
	 */
	public ATBoolean base_false() {
		return NATBoolean._FALSE_;
	}
	
	/**
	 * <tt>/</tt> evaluates to the global namespace. It is
	 * simply an alias for <tt>lobby</tt>.
	 * @see #base_lobby()
	 */
	public ATObject base__opdiv_() {
		return base_lobby();
	}
	
	/**
	 * <tt>lobby</tt> evaluates to the global namespace object.
	 * For each <tt>name=path</tt> entry on AmbientTalk's
	 * <i>object path</i>, the lobby object contains a slot
	 * <tt>name</tt> bound to a namespace object bound to
	 * the directory referred to by <tt>path</tt>.
	 * <p>
	 * Accessing the lobby allows loading in AmbientTalk source code
	 * from external files.
	 */
	public ATObject base_lobby() {
		return Evaluator.getLobbyNamespace();
	}
	
	/**
	 * <tt>root</tt> evaluates to the global lexical scope object.
	 * This is the top-level object in which the definitions of
	 * the file <tt>at/init/init.at</tt> are evaluated. All code
	 * is assumed to be "nested" in the lexical root, so all definitions
	 * of this object are lexically accessible.
	 */
	public ATObject base_root() {
		return Evaluator.getGlobalLexicalScope();
	}
	
	/**
	 * <tt>jlobby</tt> evaluates to the Java namespace root. It is a
	 * special object which is part of the symbiosis infrastructure of
	 * AmbientTalk. <tt>jlobby</tt> acts like an object that has field
	 * names that correspond to Java package names. By selecting fields
	 * from this object, an appropriate Java package can be created
	 * from which a Java class can be accessed. Only the Java classes
	 * accessible in the Java classpath are accessible.
	 * 
	 * Example:
	 * <code>jlobby.java.util.Vector</code> evaluates to a reference to
	 * the Java <tt>Vector</tt> class.
	 */
	public ATObject base_jlobby() {
		return Evaluator.getJLobbyRoot();
	}

	/**
	 * <tt>network</tt> evaluates to the unique network control object.
	 * It is a simple native object with two methods:
	 * <ul>
	 *  <li><tt>network.online()</tt> makes the interpreter go online. This allows
	 *  publications of local actors to be discovered by remote objects and vice versa.
	 *  <li><tt>network.offline()</tt> makes the interpreter go offline. All
	 *  remote references to remote objects will become disconnected.
	 * </ul>
	 */
	public ATObject base_network() {
		return OBJNetwork._INSTANCE_;
	}
	
	/**
	 * <tt>defaultMirror</tt> evaluates to the default mirror on objects. This
	 * is the mirror encapsulating the standard AmbientTalk object semantics.
	 * That is, it is a mirror with similar behaviour as the mirror created by
	 * executing: <code>reflect: (object: { ... })</code>.
	 * 
	 * The default mirror is an object with a read-only <tt>base</tt> field
	 * that signifies the base-level object of this mirror. The main purpose
	 * of this object is to serve as a prototype whose methods can be overridden
	 * by custom mirrors. The syntax:
	 * <pre>
	 * mirror: { ... }
	 * </pre>
	 * is syntactic sugar for:
	 * <pre>
	 * extend: defaultMirror with: { ... }
	 * </pre>
	 * 
	 * Note that the default mirror is typed with the <tt>/.at.types.Mirror</tt> type.
	 */
	public ATObject base_defaultMirror() {
		return Evaluator.getMirrorRoot();
	}
	
	/* ------------------------
	 * -- Control Structures --
	 * ------------------------ */
	
	/**
	 * The <tt>if:then:</tt> control structure. Usage:
	 *  <pre>if: cond then: consequent</pre>
	 * 
	 * pseudo-implementation:
	 * <pre>cond.ifTrue: consequent</pre>
	 * 
	 * Note that the consequent parameter should be a <i>closure</i>, i.e.
	 * the caller is responsible for delaying the evaluation of the consequent!
	 * 
	 * @param cond a boolean object
	 * @param consequent a closure containing the code to execute if the boolean is true
	 * @return if <tt>cond</tt> is true, the value of applying the consequent, <tt>nil</tt> otherwise
	 */
	public ATObject base_if_then_(ATBoolean cond, ATClosure consequent) throws InterpreterException {
		return cond.base_ifTrue_(consequent);
	}
	
	/**
	 * The <tt>if:then:else:</tt> control structure. Usage:
	 *  <pre>if: cond then: consequent else: alternative</pre>
	 * 
	 * pseudo-implementation:
	 * <pre>cond.ifTrue: consequent ifFalse: alternative</pre>
	 * 
	 * Note that the consequent and alternative parameters should be <i>closures</i>, i.e.
	 * the caller is responsible for delaying the evaluation of these arguments!
	 * 
	 * @param cond a boolean object
	 * @param consequent a closure containing the code to execute if the boolean is true
	 * @param alternative a closure containing the code to execute if the boolean is false
	 * @return the value of consequent if the boolean is true, the value of the alternative otherwise.
	 */
	public ATObject base_if_then_else_(ATBoolean cond, ATClosure consequent, ATClosure alternative) throws InterpreterException {
		return cond.base_ifTrue_ifFalse_(consequent, alternative);
	}
	
	/**
	 * The <tt>while:do:</tt> control structure. Usage:
	 * <pre>while: condition do: body</pre>
	 * 
	 * pseudo-implementation:
	 * <pre>condition.whileTrue: body</pre>
	 * 
	 * Note that <i>both</i> the condition and the body should be <i>closures</i>, because
	 * they represent pieces of code that have to be executed repeatedly. Because of traditional
	 * syntax, novice programmers are inclined to make the mistake of writing, e.g.:
	 * <pre>while: (i < 10) do: { i := i + 1 }</pre>
	 * Which is wrong because the first parameter should evaluate to a closure that itself
	 * returns a boolean value, not to a boolean value directly.
	 * 
	 * @param condition a closure expected to return a boolean object
	 * @param body a closure containing the code to execute as long as the condition closure returns true
	 * @return if conditions is true at least once, the last value of body, <tt>nil</tt> otherwise.
	 */
	public ATObject base_while_do_(ATClosure condition, ATClosure body) throws InterpreterException {
		return condition.base_whileTrue_(body);
	}
	
	/**
	 * The <tt>foreach:in:</tt> control structure. Usage:
	 * 
	 * <pre>foreach: body in: table</pre>
	 * 
	 * pseudo-implementation:
	 * <pre>table.each: body</pre>
	 * 
	 * Example: <code>[1,2,3].each: { |i| system.println(i) }</code>
	 * 
	 * @param body a one-arity closure that is to be applied to each element of the table
	 * @param tab a table to apply the body closure to
	 * @return <tt>nil</tt>, by default
	 */
	public ATObject base_foreach_in_(ATClosure body, ATTable tab) throws InterpreterException {
		return tab.base_each_(body);
	}

	/**
	 * The <tt>do:if:</tt> control structure. Usage:
	 * <pre>do: body if: condition</pre>
	 * 
	 * pseudo-implementation:
	 * <pre>condition.ifTrue: body</pre>
	 *
	 * In Ruby, this kind of control structure is called a <i>statement modifier</i>.
	 * 
	 * @param body a zero-argument closure to execute if the condition is true
	 * @param condition a boolean value
	 * @return the result of invoking body if the condition is true or nil if the
	 * condition is false
	 */
	public ATObject base_do_if_(ATClosure body, ATBoolean condition) throws InterpreterException {
		return condition.base_ifTrue_(body);
	}
	
	/**
	 * The <tt>do:unless:</tt> control structure. Usage:
	 * <pre>do: body unless: condition</pre>
	 * 
	 * pseudo-implementation:
	 * <pre>condition.ifFalse: body</pre>
	 *
	 * In Ruby, this kind of control structure is called a <i>statement modifier</i>.
	 * Example: <code>do: { file.close() } unless: (nil == file)</code>
	 * 
	 * @param body a zero-argument closure to execute only if the condition is false
	 * @param condition a boolean value
	 * @return the result of invoking body if the condition is false, nil otherwise
	 */
	public ATObject base_do_unless_(ATClosure body, ATBoolean condition) throws InterpreterException {
		return condition.base_ifFalse_(body);
	}
	
	/**
	 * The <tt>let:</tt> construct. Usage:
	 * <pre>let: { |var := value| body }</pre>
	 * 
	 * pseudo-implementation:
	 * <pre>closure()</pre>
	 * 
	 * <tt>let:</tt> allows for the easy creation of temporary local variables.
	 * This construct should be used in conjunction with a closure that declares optional
	 * parameters. Because the closure will be invoked with zero arguments, all of the
	 * parameters will be given their corresponding default initial value. The parameters
	 * are defined local to the closure's body.
	 * 
	 * AmbientTalk's <tt>let:</tt> behaves like Scheme's <tt>let*</tt> and <tt>letrec</tt>,
	 * i.e. the following is legal:
	 * <pre>let: {
	 *  |var1 := value1,
	 *   var2 := var1,
	 *   var3 := { ... var3() ... }|
	 *  ...
	 *}</pre>
	 * 
	 * @param body a closure which is supposed to declare some optional parameters
	 * @return the result of invoking the body closure
	 */
	public ATObject base_let_(ATClosure body) throws InterpreterException {
		return body.base_apply(NATTable.EMPTY);
	}
	
	/* ------------------------------------------
	 * -- Actor Creation and accessing Methods --
	 * ------------------------------------------ */
	
	/**
	 * The <tt>actor: closure</tt> construct.
	 *  
	 * The semantics of actor creation is as follows:
	 * <ul>
	 *  <li> Mandatory parameters to the block of initialization code are treated as lexically visible
	 *   variables that have to remain available in the new actor behaviour. Hence, these variables
	 *   are evaluated to values immediately at creation-time and parameter-passed to the new actor.
	 *  <li> The closure containing the initialization code is unpacked, its lexical scope is disregarded
	 *   and the unwrapped method is serialized and sent to the new actor, which can use it to
	 *   initialize his behaviour object.
	 *  <li>The creating actor waits for the created actor to spawn a new behaviour and to return a far
	 *   reference to this behaviour. From that point on, the creating actor can run in parallel with
	 *   the created actor, which only then evaluates the initialization code to initialize its behaviour.
	 * </ul>
	 * 
	 * @param closure the closure whose parameters define lexical fields to be copied and whose
	 * method specifies the code of the new actor's behaviour object
	 * @return a far reference to the behaviour of the new actor
	 */
	public ATObject base_actor_(ATClosure closure) throws InterpreterException {
		ATMethod method = closure.base_method();
		NATTable copiedBindings = Evaluator.evalMandatoryPars(
				method.base_parameters(),
				closure.base_context());
		
		Packet serializedBindings = new Packet("actor-bindings", copiedBindings);
		Packet serializedInitCode = new Packet("actor-initcode", method);
		return ELVirtualMachine.currentVM().createActor(serializedBindings, serializedInitCode);
	}
	
	/**
	 * <tt>actor</tt> evaluates to the mirror on the actor executing this code.
	 * The actor mirror is an object whose behaviour is consulted for operations
	 * such as creating and sending asynchronous messages or creating mirrors on
	 * other objects. It can be replaced by a custom mirror by means of the actor
	 * mirror's <tt>install:</tt> primitive.
	 */
	public ATActorMirror base_actor() throws InterpreterException {
		return ELActor.currentActor().getActorMirror();
	}
	
	/**
	 * The <tt>export: object as: topic</tt> construct. Pseudo-implementation:
	 * <pre>actor.provide(topic, object)</pre>
	 * 
	 * This construct enables the given object to become discoverable by objects
	 * in other actors by means of the topic type.
	 * 
	 * @param object the object to export to remote actors' objects
	 * @param topic a type denoting the abstract 'publication topic' for this object's publication
	 * @return a publication object whose <tt>cancel</tt> method can be used to cancel the publication.
	 */
	public ATObject base_export_as_(ATObject object, ATTypeTag topic) throws InterpreterException {
		return ELActor.currentActor().getActorMirror().base_provide(topic, object);
	}
	
	/**
	 * The <tt>when: topic discovered: handler</tt> construct. Pseudo-implementation:
	 * <pre>actor.require(topic, handler, false)</pre>
	 * 
	 * When an object is exported by <i>another</i> actor under topic, this construct triggers
	 * the given code, passing a reference to the exported object as argument to the closure.
	 * 
	 * Once the code block has run once, it will not be triggered again.
	 * 
	 * @param topic the abstract 'subscription topic' used to find an exported object
	 * @param handler a one-argument closure to apply to a discovered exported object
	 * @return a subscription object whose <tt>cancel</tt> method can be used to cancel the subscription,
	 * such that the handler will no longer be invoked. Beware, however, that at the time the
	 * subscription is cancelled, a request to apply the closure may already have been scheduled
	 * for execution by the current actor. This request is not cancelled by invoking the <tt>cancel</tt> method.
	 */
	public ATObject base_when_discovered_(ATTypeTag topic, ATClosure handler) throws InterpreterException {
		return ELActor.currentActor().getActorMirror().base_require(topic, handler, NATBoolean._FALSE_);
	}
	
	/**
	 * The <tt>whenever: topic discovered: handler</tt> construct. Pseudo-implementation:
	 * <pre>actor.require(topic, handler, true)</pre>
	 * 
	 * When an object is exported by <i>another</i> actor under topic, this construct triggers
	 * the given code, passing a reference to the exported object as argument to the closure.
	 * 
	 * The code block can be fired multiple times upon discovering multiple exported objects.
	 * To stop the block from triggering upon new publications, it must be explicitly cancelled
	 * 
	 * @param topic the abstract 'subscription topic' used to find an exported object
	 * @param handler a one-argument closure to apply to any discovered exported object
	 * @return a subscription object whose <tt>cancel</tt> method can be used to cancel the subscription,
	 * such that the handler will no longer be invoked. Beware, however, that at the time the
	 * subscription is cancelled, a request to apply the closure may already have been scheduled
	 * for execution by the current actor. This request is not cancelled by invoking the <tt>cancel</tt> method.
	 */
	public ATObject base_whenever_discovered_(ATTypeTag topic, ATClosure handler) throws InterpreterException {
		return ELActor.currentActor().getActorMirror().base_require(topic, handler, NATBoolean._TRUE_);
	}
	
	/**
	 * The <tt>when: farReference disconnected: listener</tt> construct.
	 * When the far reference is broken due to network disconnections, triggers the zero-arity listener
	 * closure. It is possible to register listeners on local far references. These may trigger if the
	 * local actor takes its object offline. In this case, these listeners will trigger as well.
	 * 
	 * @param farReference a native far reference
	 * @param listener a zero-arity closure to invoke if the far reference becomes disconnected
	 * @return a subscription object whose <tt>cancel</tt> method can be used to cancel future
	 * notifications of the listener.
	 */
	public ATObject base_when_disconnected_(ATFarReference farReference, ATClosure listener) throws InterpreterException {
		farReference.asNativeFarReference().addDisconnectionListener(listener);
		return new NATFarReference.NATDisconnectionSubscription(farReference.asNativeFarReference(), listener);
	}
	
	/**
	 * The <tt>when: farReference reconnected: listener</tt> construct.
	 * When the remote reference is reinstated after a network disconnection, trigger the zero-arity
	 * listener. Although it is allowed to register these listeners on local far references,
	 * these are normally not invoked because the only possibility for a local far ref to become
	 * disconnected is because the object was taken offline, and this is a permanent disconnect.
	 * 
	 * @param farReference a native far reference
	 * @param listener a zero-arity closure to invoke if the far reference becomes reconnected
	 * @return a subscription object whose <tt>cancel</tt> method can be used to cancel future
	 * notifications of the listener.
	 */
	public ATObject base_when_reconnected_(ATFarReference farReference, ATClosure listener) throws InterpreterException {
		farReference.asNativeFarReference().addReconnectionListener(listener);
		return new NATFarReference.NATReconnectionSubscription(farReference.asNativeFarReference(), listener);
	}
	
	/**
	 * The <tt>when: farReference takenOffline:</tt> construct.
	 *  When the (remote/local) far reference is broken because the object referenced was 
	 *  taken offline, trigger the code.
	 *  
	 * @param farReference a native far reference
	 * @param listener a zero-arity closure to invoke if the referenced object has been taken offline.
	 * @return a subscription object whose <tt>cancel</tt> method can be used to cancel future
	 * notifications of the listener.
	 */
	public ATObject base_when_takenOffline_(ATFarReference farReference, ATClosure listener) throws InterpreterException {
		farReference.asNativeFarReference().addTakenOfflineListener(listener);
		return new NATFarReference.NATExpiredSubscription(farReference.asNativeFarReference(), listener);
	}
	

	/**
	 * The <tt>retract: farReference</tt> construct. 
	 * Retracts all currently unsent messages from the far reference's outbox.
	 * This has the side effect that the returned messages will <b>not</b> be sent
	 * automatically anymore, the programmer is responsible to explicitly resend
	 * all messages that were retracted but still need to be sent.
	 *  
	 * Note that the returned messages are copies of the original.
	 * @param farReference the far reference of which to retract outgoing message sends
	 * @return a table containing copies of all messages that were sent to this far reference, but
	 * not yet transmitted by the far reference to its referent.
	 */
	public ATTable base_retract_(ATFarReference farReference) throws InterpreterException {
		return farReference.meta_retractUnsentMessages();
	}
	
	/* -----------------------------
	 * -- Object Creation Methods --
	 * ----------------------------- */
	
	/**
	 * The <tt>object:</tt> object creation primitive.
	 * This construct creates a new AmbientTalk object where:
	 * <ul>
	 *  <li>The object is initialized with the <i>code</i> of the argument closure.
	 *  <li>The object's <b>lexical parent</b> is the lexical scope of the argument closure. 
	 *  <li>The object's <b>dynamic parent</b> is <tt>nil</tt>.
	 *  <li>The object's <b>parent type</b> is <b>SHARES-A</b> (i.e. it is not an extension of its parent).
	 *  <li>The object's <b>types</b> is <tt>[]</tt> (i.e. it has no types).
	 *  <li>The object's <b>mirror</b> is the <tt>defaultMirror</tt> on objects (i.e. it is an object
	 *  with a 'native' metaobject protocol).
	 * </ul>
	 * 
	 * Example: <code>object: { def x := 1; }</code>
	 * <p>
	 * Pseudo-implementation:
	 * <pre>object: code childOf: nil extends: false taggedAs: [] mirroredBy: defaultMirror</pre>
	 * 
	 * The closure used to initialize the object may contain formal parameters. The closure
	 * will always be invoked with <i>its own mandatory formal parameters</i>. E.g., a closure
	 * <code>{ |x| nil }</code> is invoked as <code>{ |x| nil }(x)</code>. The net effect of this
	 * mechanic is that if <tt>x</tt> is a lexically visible variable at the object-creation
	 * site, the value of the variable will be copied into a copy with the same name which
	 * resides in the newly created object. This mechanic is primarily useful for copying surrounding
	 * variables within the object, e.g. for isolates which lose access to their surrounding scope.
	 * <p>
	 * Also, if the closure has optional parameters, they will always be triggered.
	 * The expressions to initialize the formal parameters are <i>evaluated</i>
	 * in the context of the closure's lexical scope but are <i>added</i> to the newly created object.
	 * 
	 * @param code a closure containing both the code with which to initialize the object and the new object's lexical parent
	 * @return a new AmbientTalk object with the properties defined above.
	 * @see #base_object_childOf_extends_taggedAs_mirroredBy_(ATClosure, ATObject, ATBoolean, ATTable, ATObject)
	 */
	public ATObject base_object_(ATClosure code) throws InterpreterException {
		return base_object_childOf_extends_taggedAs_mirroredBy_(
				code,
				OBJNil._INSTANCE_,
				NATBoolean._FALSE_ /* SHARES-A link */,
				NATTable.EMPTY,
				base_defaultMirror());
	}
	
	/**
	 * The <tt>extend:with:</tt> object creation primitive.
	 * This construct creates a new AmbientTalk object where:
	 * <ul>
	 *  <li>The object is initialized with the <i>code</i> of the argument closure.
	 *  <li>The object's <b>lexical parent</b> is the lexical scope of the argument closure. 
	 *  <li>The object's <b>dynamic parent</b> is the argument object.
	 *  <li>The object's <b>parent type</b> is <b>IS-A</b> (i.e. it is an extension of its parent).
	 *  <li>The object's <b>types</b> is <tt>[]</tt> (i.e. it has no types).
	 *  <li>The object's <b>mirror</b> is the <tt>defaultMirror</tt> on objects (i.e. it is an object
	 *  with a 'native' metaobject protocol).
	 * </ul>
	 * 
	 * Example: <code>extend: parent with: { def x := 1; }</code>
	 * <p>
	 * Pseudo-implementation:
	 * <pre>object: code childOf: parent extends: true taggedAs: [] mirroredBy: defaultMirror</pre>
	 * 
	 * @param parent the dynamic parent object of the newly created object.
	 * @param code a closure containing both the code with which to initialize the object and the new object's lexical parent
	 * @return a new AmbientTalk object with the properties defined above.
	 * @see #base_object_(ATClosure)
	 * @see #base_object_childOf_extends_taggedAs_mirroredBy_(ATClosure, ATObject, ATBoolean, ATTable, ATObject)
	 */
	public ATObject base_extend_with_(ATObject parent, ATClosure code) throws InterpreterException {
		return base_object_childOf_extends_taggedAs_mirroredBy_(
				code,
				parent,
				NATBoolean._TRUE_ /* IS-A link */,
				NATTable.EMPTY,
				base_defaultMirror());
	}
	
    /**
     * The <tt>extend:with:taggedAs:</tt> object creation primitive.
	 * This construct creates a new AmbientTalk object where:
	 * <ul>
	 *  <li>The object is initialized with the <i>code</i> of the argument closure.
	 *  <li>The object's <b>lexical parent</b> is the lexical scope of the argument closure. 
	 *  <li>The object's <b>dynamic parent</b> is the argument object.
	 *  <li>The object's <b>parent type</b> is <b>IS-A</b> (i.e. it is an extension of its parent).
	 *  <li>The object's <b>types</b> are initialized to the argument types table.
	 *  <li>The object's <b>mirror</b> is the <tt>defaultMirror</tt> on objects (i.e. it is an object
	 *  with a 'native' metaobject protocol).
	 * </ul>
	 * 
	 * Example: <code>extend: parent with: { def x := 1; } taggedAs: [foo,bar]</code>
	 * <p>
	 * Pseudo-implementation:
	 * <pre>object: code childOf: parent extends: true taggedAs: types mirroredBy: defaultMirror</pre>
	 * 
	 * @param parent the dynamic parent object of the newly created object.
	 * @param code a closure containing both the code with which to initialize the object and the new object's lexical parent.
	 * @param types a table of types with which to type the newly created object.
	 * @return a new AmbientTalk object with the properties defined above.
	 * @see #base_object_(ATClosure)
	 * @see #base_object_childOf_extends_taggedAs_mirroredBy_(ATClosure, ATObject, ATBoolean, ATTable, ATObject)
	 */
	public ATObject base_extend_with_taggedAs_(ATObject parent, ATClosure code, ATTable types) throws InterpreterException {
		return base_object_childOf_extends_taggedAs_mirroredBy_(
				code,
				parent,
				NATBoolean._TRUE_ /* IS-A link */,
				types,
				base_defaultMirror());
	}
	
    /**
     * The <tt>extend:with:mirroredBy:</tt> object creation primitive.
	 * This construct creates a new AmbientTalk object where:
	 * <ul>
	 *  <li>The object is initialized with the <i>code</i> of the argument closure.
	 *  <li>The object's <b>lexical parent</b> is the lexical scope of the argument closure. 
	 *  <li>The object's <b>dynamic parent</b> is the argument object.
	 *  <li>The object's <b>parent type</b> is <b>IS-A</b> (i.e. it is an extension of its parent).
	 *  <li>The object's <b>types</b> are set to <tt>[]</tt> (i.e. the object has no types).
	 *  <li>The object's <b>mirror</b> is the given mirror. This means that this object is a <i>mirage</i>
	 *  whose metaobject protocol is entirely dictated by the given mirror.
	 * </ul>
	 * 
	 * Example: <code>extend: parent with: { def x := 1; } mirroredBy: (mirror: {...})</code>
	 * <p>
	 * Pseudo-implementation:
	 * <pre>object: code childOf: parent extends: true taggedAs: [] mirroredBy: mirror</pre>
	 * 
	 * @param parent the dynamic parent object of the newly created object.
	 * @param code a closure containing both the code with which to initialize the object and the new object's lexical parent.
	 * @param mirror the mirror of the newly created mirage object.
	 * @return a new AmbientTalk object with the properties defined above.
	 * @see #base_object_(ATClosure)
	 * @see #base_object_childOf_extends_taggedAs_mirroredBy_(ATClosure, ATObject, ATBoolean, ATTable, ATObject)
	 */
	public ATObject base_extend_with_mirroredBy_(ATObject parent, ATClosure code, ATObject mirror) throws InterpreterException {
		return base_object_childOf_extends_taggedAs_mirroredBy_(
				code,
				parent,
				NATBoolean._TRUE_ /* IS-A link */,
				NATTable.EMPTY,
				mirror);
	}
	
    /**
     * The <tt>extend:with:taggedAs:mirroredBy:</tt> object creation primitive.
	 * This construct creates a new AmbientTalk object where:
	 * <ul>
	 *  <li>The object is initialized with the <i>code</i> of the argument closure.
	 *  <li>The object's <b>lexical parent</b> is the lexical scope of the argument closure. 
	 *  <li>The object's <b>dynamic parent</b> is the argument object.
	 *  <li>The object's <b>parent type</b> is <b>IS-A</b> (i.e. it is an extension of its parent).
	 *  <li>The object's <b>types</b> are initialized to the argument types table.
	 *  <li>The object's <b>mirror</b> is the given argument mirror. This means that the newly
	 *  created object is a <i>mirage</i> whose metaobject protocol is dictated by the given mirror.
	 * </ul>
	 * 
	 * Example: <code>extend: parent with: { def x := 1; } taggedAs: [foo,bar] mirroredBy: mirror</code>
	 * <p>
	 * Pseudo-implementation:
	 * <pre>object: code childOf: parent extends: true taggedAs: types mirroredBy: mirror</pre>
	 * 
	 * @param parent the dynamic parent object of the newly created object.
	 * @param code a closure containing both the code with which to initialize the object and the new object's lexical parent.
	 * @param types a table of types with which to type the newly created object.
	 * @param the mirror object of the newly created mirage object.
	 * @return a new AmbientTalk object with the properties defined above.
	 * @see #base_object_(ATClosure)
	 * @see #base_object_childOf_extends_taggedAs_mirroredBy_(ATClosure, ATObject, ATBoolean, ATTable, ATObject)
	 */
	public ATObject base_extend_with_taggedAs_mirroredBy_(ATObject parent, ATClosure code, ATTable types, ATObject mirror) throws InterpreterException {
		return base_object_childOf_extends_taggedAs_mirroredBy_(
				code,
				parent,
				NATBoolean._TRUE_ /* IS-A link */,
				types,
				mirror);
	}
	
	/**
	 * The <tt>share:with:</tt> object creation primitive.
	 * This construct creates a new AmbientTalk object where:
	 * <ul>
	 *  <li>The object is initialized with the <i>code</i> of the argument closure.
	 *  <li>The object's <b>lexical parent</b> is the lexical scope of the argument closure. 
	 *  <li>The object's <b>dynamic parent</b> is the argument object.
	 *  <li>The object's <b>parent type</b> is <b>SHARES-A</b> (i.e. it is not an extension of its parent).
	 *  <li>The object's <b>types</b> is <tt>[]</tt> (i.e. it has no types).
	 *  <li>The object's <b>mirror</b> is the <tt>defaultMirror</tt> on objects (i.e. it is an object
	 *  with a 'native' metaobject protocol).
	 * </ul>
	 * 
	 * Example: <code>share: parent with: { def x := 1; }</code>
	 * <p>
	 * Pseudo-implementation:
	 * <pre>object: code childOf: parent extends: false taggedAs: [] mirroredBy: defaultMirror</pre>
	 * 
	 * @param parent the dynamic parent object of the newly created object.
	 * @param code a closure containing both the code with which to initialize the object and the new object's lexical parent
	 * @return a new AmbientTalk object with the properties defined above.
	 * @see #base_object_(ATClosure)
	 * @see #base_object_childOf_extends_taggedAs_mirroredBy_(ATClosure, ATObject, ATBoolean, ATTable, ATObject)
	 */
	public ATObject base_share_with_(ATObject parent, ATClosure code) throws InterpreterException {
		return base_object_childOf_extends_taggedAs_mirroredBy_(
				code,
				parent,
				NATBoolean._FALSE_ /* SHARES-A link */,
				NATTable.EMPTY,
				base_defaultMirror());
	}

    /**
     * The <tt>share:with:taggedAs:</tt> object creation primitive.
	 * This construct creates a new AmbientTalk object where:
	 * <ul>
	 *  <li>The object is initialized with the <i>code</i> of the argument closure.
	 *  <li>The object's <b>lexical parent</b> is the lexical scope of the argument closure. 
	 *  <li>The object's <b>dynamic parent</b> is the argument object.
	 *  <li>The object's <b>parent type</b> is <b>SHARES-A</b> (i.e. it is not an extension of its parent).
	 *  <li>The object's <b>types</b> are initialized to the argument types table.
	 *  <li>The object's <b>mirror</b> is the <tt>defaultMirror</tt> on objects (i.e. it is an object
	 *  with a 'native' metaobject protocol).
	 * </ul>
	 * 
	 * Example: <code>share: parent with: { def x := 1; } taggedAs: [foo,bar]</code>
	 * <p>
	 * Pseudo-implementation:
	 * <pre>object: code childOf: parent extends: false taggedAs: types mirroredBy: defaultMirror</pre>
	 * 
	 * @param parent the dynamic parent object of the newly created object.
	 * @param code a closure containing both the code with which to initialize the object and the new object's lexical parent.
	 * @param types a table of types with which to type the newly created object.
	 * @return a new AmbientTalk object with the properties defined above.
	 * @see #base_object_(ATClosure)
	 * @see #base_object_childOf_extends_taggedAs_mirroredBy_(ATClosure, ATObject, ATBoolean, ATTable, ATObject)
	 */
	public ATObject base_share_with_taggedAs_(ATObject parent, ATClosure code, ATTable types) throws InterpreterException {
		return base_object_childOf_extends_taggedAs_mirroredBy_(
				code,
				parent,
				NATBoolean._FALSE_ /* SHARES-A link */,
				types,
				base_defaultMirror());
	}
	
    /**
     * The <tt>share:with:mirroredBy:</tt> object creation primitive.
	 * This construct creates a new AmbientTalk object where:
	 * <ul>
	 *  <li>The object is initialized with the <i>code</i> of the argument closure.
	 *  <li>The object's <b>lexical parent</b> is the lexical scope of the argument closure. 
	 *  <li>The object's <b>dynamic parent</b> is the argument object.
	 *  <li>The object's <b>parent type</b> is <b>SHARES-A</b> (i.e. it is not an extension of its parent).
	 *  <li>The object's <b>types</b> are set to <tt>[]</tt> (i.e. the object has no types).
	 *  <li>The object's <b>mirror</b> is the given mirror. This means that this object is a <i>mirage</i>
	 *  whose metaobject protocol is entirely dictated by the given mirror.
	 * </ul>
	 * 
	 * Example: <code>share: parent with: { def x := 1; } mirroredBy: (mirror: {...})</code>
	 * <p>
	 * Pseudo-implementation:
	 * <pre>object: code childOf: parent extends: false taggedAs: [] mirroredBy: mirror</pre>
	 * 
	 * @param parent the dynamic parent object of the newly created object.
	 * @param code a closure containing both the code with which to initialize the object and the new object's lexical parent.
	 * @param mirror the mirror of the newly created mirage object.
	 * @return a new AmbientTalk object with the properties defined above.
	 * @see #base_object_(ATClosure)
	 * @see #base_object_childOf_extends_taggedAs_mirroredBy_(ATClosure, ATObject, ATBoolean, ATTable, ATObject)
	 */
	public ATObject base_share_with_mirroredBy_(ATObject parent, ATClosure code, ATObject mirror) throws InterpreterException {
		return base_object_childOf_extends_taggedAs_mirroredBy_(
				code,
				parent,
				NATBoolean._FALSE_ /* SHARES-A link */,
				NATTable.EMPTY,
				mirror);
	}
	
    /**
     * The <tt>share:with:taggedAs:mirroredBy:</tt> object creation primitive.
	 * This construct creates a new AmbientTalk object where:
	 * <ul>
	 *  <li>The object is initialized with the <i>code</i> of the argument closure.
	 *  <li>The object's <b>lexical parent</b> is the lexical scope of the argument closure. 
	 *  <li>The object's <b>dynamic parent</b> is the argument object.
	 *  <li>The object's <b>parent type</b> is <b>SHARES-A</b> (i.e. it is not an extension of its parent).
	 *  <li>The object's <b>types</b> are initialized to the argument types table.
	 *  <li>The object's <b>mirror</b> is the given argument mirror. This means that the newly
	 *  created object is a <i>mirage</i> whose metaobject protocol is dictated by the given mirror.
	 * </ul>
	 * 
	 * Example: <code>share: parent with: { def x := 1; } taggedAs: [foo,bar] mirroredBy: mirror</code>
	 * <p>
	 * Pseudo-implementation:
	 * <pre>object: code childOf: parent extends: false taggedAs: types mirroredBy: mirror</pre>
	 * 
	 * @param parent the dynamic parent object of the newly created object.
	 * @param code a closure containing both the code with which to initialize the object and the new object's lexical parent.
	 * @param types a table of types with which to type the newly created object.
	 * @param mirror the mirror object of the newly created mirage object.
	 * @return a new AmbientTalk object with the properties defined above.
	 * @see #base_object_(ATClosure)
	 * @see #base_object_childOf_extends_taggedAs_mirroredBy_(ATClosure, ATObject, ATBoolean, ATTable, ATObject)
	 */
	public ATObject base_share_with_taggedAs_mirroredBy_(ATObject parent, ATClosure code, ATTable types, ATObject mirror) throws InterpreterException {
		return base_object_childOf_extends_taggedAs_mirroredBy_(
				code,
				parent,
				NATBoolean._FALSE_ /* SHARES-A link */,
				types,
				mirror);
	}
	
    /**
     * The <tt>object:taggedAs:</tt> object creation primitive.
	 * This construct creates a new AmbientTalk object where:
	 * <ul>
	 *  <li>The object is initialized with the <i>code</i> of the argument closure.
	 *  <li>The object's <b>lexical parent</b> is the lexical scope of the argument closure. 
	 *  <li>The object's <b>dynamic parent</b> is <tt>nil</tt>.
	 *  <li>The object's <b>parent type</b> is <b>SHARES-A</b> (i.e. it is not an extension of its parent).
	 *  <li>The object's <b>types</b> are initialized to the argument types table.
	 *  <li>The object's <b>mirror</b> is <tt>defaultMirror</tt> (i.e. the object has the
	 *  default metaobject protocol).
	 * </ul>
	 * 
	 * Example: <code>object: { def x := 1; } taggedAs: [foo,bar]</code>
	 * <p>
	 * Pseudo-implementation:
	 * <pre>object: code childOf: nil extends: false taggedAs: types mirroredBy: defaultMirror</pre>
	 * 
	 * @param code a closure containing both the code with which to initialize the object and the new object's lexical parent.
	 * @param types a table of type tags with which to type the newly created object.
	 * @return a new AmbientTalk object with the properties defined above.
	 * @see #base_object_(ATClosure)
	 * @see #base_object_childOf_extends_taggedAs_mirroredBy_(ATClosure, ATObject, ATBoolean, ATTable, ATObject)
	 */
	public ATObject base_object_taggedAs_(ATClosure code, ATTable types) throws InterpreterException {
		return base_object_childOf_extends_taggedAs_mirroredBy_(
				code,
				OBJNil._INSTANCE_,
				NATBoolean._FALSE_ /* SHARES-A link */,
				types,
				base_defaultMirror());
	}
	
    /**
     * The <tt>isolate:</tt> object creation primitive.
	 * This construct creates a new AmbientTalk object where:
	 * <ul>
	 *  <li>The object is initialized with the <i>code</i> of the argument closure.
	 *  <li>The object's <b>lexical parent</b> is initialized to the lexical scope of the argument closure,
	 *  but because it is typed as an isolate, the parent link is replaced by a link to the lexical <tt>root</tt>.
	 *  <li>The object's <b>dynamic parent</b> is <tt>nil</tt>.
	 *  <li>The object's <b>parent type</b> is <b>SHARES-A</b> (i.e. it is not an extension of its parent).
	 *  <li>The object's <b>types</b> are initialized to <tt>[/.at.types.Isolate]</tt>, i.e.
	 *  the object is typed as an isolate.
	 *  <li>The object's <b>mirror</b> is <tt>defaultMirror</tt> (i.e. the object has the
	 *  default metaobject protocol).
	 * </ul>
	 * 
	 * Example: <code>isolate: { def x := 1; }</code>
	 * <p>
	 * Pseudo-implementation:
	 * <pre>object: code childOf: nil extends: false taggedAs: [/.at.types.Isolate] mirroredBy: defaultMirror</pre>
	 * 
	 * An isolate is an object without a proper lexical parent. It is as if the isolate is always
	 * defined at top-level. However, lexically visible variables can be retained by copying them into the isolate
	 * by means of formal parameters to the argument closure. Isolate objects are passed by-copy during
	 * inter-actor parameter and result passing.
	 * 
	 * @param code a closure containing both the code with which to initialize the object and the new object's lexical parent.
	 * @return a new AmbientTalk object with the properties defined above.
	 * @see #base_object_(ATClosure)
	 * @see #base_object_childOf_extends_taggedAs_mirroredBy_(ATClosure, ATObject, ATBoolean, ATTable, ATObject)
	 */
	public ATObject base_isolate_(ATClosure code) throws InterpreterException {
		return base_object_taggedAs_(code, NATTable.of(NativeTypeTags._ISOLATE_));
	}
	
    /**
     * The <tt>mirror:</tt> object creation primitive.
	 * This construct creates a new AmbientTalk object where:
	 * <ul>
	 *  <li>The object is initialized with the <i>code</i> of the argument closure.
	 *  <li>The object's <b>lexical parent</b> is the lexical scope of the argument closure. 
	 *  <li>The object's <b>dynamic parent</b> is <tt>defaultMirror</tt>.
	 *  <li>The object's <b>parent type</b> is <b>IS-A</b> (i.e. it is an extension of its parent).
	 *  <li>The object's <b>types</b> are initialized to <tt>[]</tt>.
	 *  <li>The object's <b>mirror</b> is <tt>defaultMirror</tt> (i.e. the object has the
	 *  default metaobject protocol).
	 * </ul>
	 * 
	 * Example: <code>mirror: { def x := 1; }</code>
	 * <p>
	 * Pseudo-implementation:
	 * <pre>object: code childOf: defaultMirror extends: true taggedAs: [] mirroredBy: defaultMirror</pre>
	 * 
	 * This construct is mere syntactic sugar for creating an extension of the default mirror root.
	 * It follows that AmbientTalk mirrors are plain AmbientTalk objects. They simply need to implement
	 * the entire metaobject protocol, and the easiest means to achieve this is by extending the default mirror.
	 * Also keep in mind that the mirror is an extension object. This is important because the default
	 * mirror has <i>state</i>, being the <tt>base</tt> field that points to the base-level object
	 * being mirrorred. Hence, always make sure that, if overriding <tt>init</tt>, the parent's
	 * <tt>init</tt> method is invoked with the appropriate <tt>base</tt> value.
	 * 
	 * @param code a closure containing both the code with which to initialize the object and the new object's lexical parent.
	 * @return a new AmbientTalk object with the properties defined above.
	 * @see #base_object_(ATClosure)
	 * @see #base_object_childOf_extends_taggedAs_mirroredBy_(ATClosure, ATObject, ATBoolean, ATTable, ATObject)
	 */
	public ATObject base_mirror_(ATClosure code) throws InterpreterException {
		return base_object_childOf_extends_taggedAs_mirroredBy_(
				code,
				base_defaultMirror(),
				NATBoolean._TRUE_ /* IS-A link */, 
			    NATTable.EMPTY,
			    base_defaultMirror());
	}
	
    /**
     * The <tt>object:mirroredBy:</tt> object creation primitive.
	 * This construct creates a new AmbientTalk object where:
	 * <ul>
	 *  <li>The object is initialized with the <i>code</i> of the argument closure.
	 *  <li>The object's <b>lexical parent</b> is the lexical scope of the argument closure. 
	 *  <li>The object's <b>dynamic parent</b> is <tt>nil</tt>.
	 *  <li>The object's <b>parent type</b> is <b>SHARES-A</b> (i.e. it is not an extension of its parent).
	 *  <li>The object's <b>types</b> are set to <tt>[]</tt> (i.e. the object has no types).
	 *  <li>The object's <b>mirror</b> is the given mirror. This means that this object is a <i>mirage</i>
	 *  whose metaobject protocol is entirely dictated by the given mirror.
	 * </ul>
	 * 
	 * Example: <code>object: { def x := 1; } mirroredBy: (mirror: {...})</code>
	 * <p>
	 * Pseudo-implementation:
	 * <pre>object: code childOf: nil extends: false taggedAs: [] mirroredBy: mirror</pre>
	 * 
	 * This primitive allows the construction of so-called <i>mirage</i> objects which are
	 * AmbientTalk objects whose metaobject protocol behaviour is dictated by a custom mirror
	 * object.
	 * 
	 * @param code a closure containing both the code with which to initialize the object and the new object's lexical parent.
	 * @param mirror the mirror of the newly created mirage object.
	 * @return a new AmbientTalk object with the properties defined above.
	 * @see #base_object_(ATClosure)
	 * @see #base_object_childOf_extends_taggedAs_mirroredBy_(ATClosure, ATObject, ATBoolean, ATTable, ATObject)
	 */
	public ATObject base_object_mirroredBy_(ATClosure code, ATObject mirror) throws InterpreterException {
		return base_object_childOf_extends_taggedAs_mirroredBy_(
				code,
				OBJNil._INSTANCE_,
				NATBoolean._FALSE_ /* SHARES-A link */,
				NATTable.EMPTY,
				mirror);
	}
	
    /**
     * The <tt>object:taggedAs:mirroredBy:</tt> object creation primitive.
	 * This construct creates a new AmbientTalk object where:
	 * <ul>
	 *  <li>The object is initialized with the <i>code</i> of the argument closure.
	 *  <li>The object's <b>lexical parent</b> is the lexical scope of the argument closure. 
	 *  <li>The object's <b>dynamic parent</b> is <tt>nil</tt>.
	 *  <li>The object's <b>parent type</b> is <b>SHARES-A</b> (i.e. it is not an extension of its parent).
	 *  <li>The object's <b>types</b> are set to the argument types.
	 *  <li>The object's <b>mirror</b> is the given mirror. This means that this object is a <i>mirage</i>
	 *  whose metaobject protocol is entirely dictated by the given mirror.
	 * </ul>
	 * 
	 * Example: <code>object: { def x := 1; } taggedAs: [foo,bar] mirroredBy: (mirror: {...})</code>
	 * <p>
	 * Pseudo-implementation:
	 * <pre>object: code childOf: nil extends: false taggedAs: types mirroredBy: mirror</pre>
	 * 
	 * @param code a closure containing both the code with which to initialize the object and the new object's lexical parent.
	 * @param types a table of types with which to type the newly created object.
	 * @param mirror the mirror of the newly created mirage object.
	 * @return a new AmbientTalk object with the properties defined above.
	 * @see #base_object_(ATClosure)
	 * @see #base_object_childOf_extends_taggedAs_mirroredBy_(ATClosure, ATObject, ATBoolean, ATTable, ATObject)
	 */
	public ATObject base_object_taggedAs_mirroredBy_(ATClosure code, ATTable types, ATObject mirror) throws InterpreterException {
		return base_object_childOf_extends_taggedAs_mirroredBy_(
				code,
				OBJNil._INSTANCE_,
				NATBoolean._FALSE_ /* SHARES-A link */,
				types,
				mirror);
	}
	
    /**
     * The <tt>object:childOf:extends:taggedAs:mirroredBy:</tt> object creation primitive.
	 * This construct creates a new AmbientTalk object where:
	 * <ul>
	 *  <li>The object is initialized with the <i>code</i> of the argument closure.
	 *  <li>The object's <b>lexical parent</b> is the lexical scope of the argument closure. 
	 *  <li>The object's <b>dynamic parent</b> is the argument parent object.
	 *  <li>The object's <b>parent type</b> is the argument parent type, true being <tt>IS-A</tt>, false being <tt>SHARES-A</tt>.
	 *  <li>The object's <b>types</b> are set to the argument types table.
	 *  <li>The object's <b>mirror</b> is the given mirror. This means that this object is a <i>mirage</i>
	 *  whose metaobject protocol is entirely dictated by the given mirror, if the mirror is not <tt>defaultMirror</tt>.
	 * </ul>
	 * 
	 * Example: <code>object: { def x := 1; } childOf: parent extends: true taggedAs: [foo,bar] mirroredBy: mirror</code>
	 * <p>
	 * Pseudo-implementation:
	 * <pre>let o = OBJECT(parent,code.lexicalParent,parentType,types);
	 *  code.applyInScope(o, code.mandatoryPars);
	 *  o
	 * </pre>
	 * 
	 * @param code a closure containing both the code with which to initialize the object and the new object's lexical parent.
	 * @param parent the dynamic parent object of the newly created object.
	 * @param parentType a boolean denoting whether or not the object is an extension of its dynamic parent.
	 * @param types a table of types with which the newly created object should be typed.
	 * @param mirror the mirror of the newly created object.
	 * @return a new AmbientTalk object with the properties defined above.
	 * @see #base_object_(ATClosure) for more information about the properties of the passed closure.
	 */
	public ATObject base_object_childOf_extends_taggedAs_mirroredBy_(ATClosure code, ATObject parent, ATBoolean parentType, ATTable types, ATObject mirror) throws InterpreterException {
		ATTypeTag[] typeArray = NATTypeTag.toTypeTagArray(types);
		boolean parentRelation = parentType.asNativeBoolean().javaValue;
		NATObject newObject;
		// if the mirror is a 'default' mirror...
		if (mirror instanceof NATMirrorRoot) {
			// then create a native object
			newObject = new NATObject(parent, // dynamic parent
					                  code.base_context().base_lexicalScope(), // lexical parent
					                  parentRelation, // IS-A or SHARES-A
					                  typeArray); // initial types
		} else {
			// else create a mirage mirrored by the custom mirror
			newObject = NATMirage.createMirage(code, parent, parentRelation, typeArray, mirror);
		}
		
		newObject.initializeWithCode(code);
		return newObject;
	}
	
	/**
	 * The <tt>reflect:</tt> construct. This construct returns a mirror on an object.
	 * 
	 * pseudo-implementation:
	 * <pre>actor.createMirror(reflectee)</pre>
	 * 
	 * An actor can change its default mirror creation policy by installing a new
	 * actor protocol that overrides <tt>createMirror</tt>.
	 * 
	 * @param reflectee the object to reflect upon
	 * @return a mirror reflecting on the given object
	 * @see ATActorMirror#base_createMirror(ATObject) for the details about mirror creation on objects.
	 */
	public ATObject base_reflect_(ATObject reflectee) throws InterpreterException {
		return base_actor().base_createMirror(reflectee);
	}
	
	/**
	 * The <tt>clone:</tt> language construct. Returns a clone of an object.
	 * 
	 * Care must be taken when cloning a mirror. If a mirror would simply be cloned
	 * using the regular cloning semantics, its base field would be <b>shared</b>
	 * between the clone and the original mirror (because of shallow copy semantics).
	 * However, the base object will still be tied to the original mirror, not the clone.
	 * Therefore, the clone: operator is implemented as follows:
	 * 
     * <pre>def clone: obj {
     *   if: (is: obj taggedAs: Mirror) then: {
     *     reflect: (clone: obj.base)
     *   } else: {
     *     (reflect: obj).clone()
     *   }
     * }</pre>
	 * 
	 * The default cloning semantics ensures that all fields of the object are shallow-copied.
	 * Because methods are immutable, a clone and its original object share their method dictionary,
	 * but whenever a change is made to the dictionary, the changer creates a local copy of the
	 * dictionary as to not modify any clones. Hence, each object is truly stand-alone and independent
	 * of its clone.
	 * 
	 * @param original the object to copy
	 * @return a clone of the given object (default semantics results in a shallow copy)
	 */
	public ATObject base_clone_(ATObject original) throws InterpreterException {
		if (original.meta_isTaggedAs(NativeTypeTags._MIRROR_).asNativeBoolean().javaValue) {
		    return base_reflect_(base_clone_(original.impl_invokeAccessor(original, NATMirrorRoot._BASE_NAME_, NATTable.EMPTY)));
		} else {
			return original.meta_clone();
		}
	}
	
	/**
	 * The <tt>takeOffline:</tt> construct. 
	 * Removes an object from the export table of an actor. This ensures that the object
	 * is no longer remotely accessible. This method is the cornerstone of distributed
	 * garbage collection in AmbientTalk. When an object is taken offline, remote clients
	 * that would still access it perceive this as if the object had become permanently
	 * disconnected.
	 * 
	 * @return <tt>nil</tt>.
	 */
	 public ATNil base_takeOffline_ (ATObject object) throws InterpreterException{
		ELActor.currentActor().takeOffline(object);
		return OBJNil._INSTANCE_;
	 }
	
	/* -------------------
	 * -- Type Support -
	 * ------------------- */
	
	/**
	 * The <tt>is: object taggedAs: type</tt> construct.
	 * @return true if the given object is typed with the given type, false otherwise
	 */
	public ATBoolean base_is_taggedAs_(ATObject object, ATTypeTag type) throws InterpreterException {
		return object.meta_isTaggedAs(type);
	}
	
	/**
	 * The <tt>tagsOf: object</tt>
	 * @return a table of all of the <i>local</i> types of an object.
	 */
	public ATTable base_tagsOf_(ATObject object) throws InterpreterException {
		return object.meta_typeTags();
	}
	
	/* -------------------------------
	 * -- Exception Handling Support -
	 * ------------------------------- */
	
	/**
	 * The <tt>try: { tryBlock } finally: { finallyBlock }</tt> construct.
	 * 
	 * Applies the tryBlock closure (to <tt>[]</tt>).
	 * Whether the tryBlock raises an exception or not, the finallyBlock closure is guaranteed to be applied either 
	 * after normal termination of the tryBlock or when an exception is propagated from the tryBlock.
	 */
	public ATObject base_try_finally_(ATClosure tryBlock, ATClosure finallyBlock) throws InterpreterException {
		try {
			return tryBlock.base_apply(NATTable.EMPTY);
		} finally {
		    finallyBlock.base_apply(NATTable.EMPTY);
		}
	}
	
	/**
	 * The <tt>try: { tryBlock } usingHandlers: [ handler1, handler2, ... ] finally: { finallyBlock }</tt> construct.
	 * 
	 * Applies the tryBlock closure (to <tt>[]</tt>) and handles exceptions using the given exception handlers.
	 * Whether the tryBlock raises an exception or not, the finallyBlock closure is guaranteed to be applied either 
	 * after the termination of the tryBlock or the execution of a fitting handler either before the exception is 
	 * propagated if no matching handler is provided. This construct is the most general means of doing exception
	 * handling in AmbientTalk.
	 * 
	 * The handlers given in the handler table represent first-class handler objects,
	 * which should respond to the <tt>canHandle</tt> message.
	 * @see ATHandler for the interface to which a handler object has to adhere
	 */
	public ATObject base_try_usingHandlers_finally_(ATClosure tryBlock, ATTable exceptionHandlers, ATClosure finallyBlock) throws InterpreterException {
		try {
			return tryBlock.base_apply(NATTable.EMPTY);
		} catch(InterpreterException e) {
			ATObject[] handlers = exceptionHandlers.asNativeTable().elements_;
			
			// find the appropriate handler
			for (int i = 0; i < handlers.length; i++) {
				ATHandler handler = handlers[i].asHandler();
				ATObject exc = e.getAmbientTalkRepresentation();
				if (handler.base_canHandle(exc).asNativeBoolean().javaValue) {
					return handler.base_handle(exc);
				};	
			}
			
			// no handler found, re-throw the exception
			throw e;
		} finally {
			finallyBlock.base_apply(NATTable.EMPTY);
		}
	}
	
	/**
	 * The <tt>try: { tryBlock } usingHandlers: [ handler1, handler2, ... ]</tt> construct.
	 * 
	 * Ad hoc code for tryBlocks which have an empty finally block
	 * @see OBJLexicalRoot#base_try_usingHandlers_finally_(ATClosure, ATTable, ATClosure)
	 */
	public ATObject base_try_usingHandlers_(ATClosure tryBlock, ATTable exceptionHandlers) throws InterpreterException {
		// An ad hoc version is provided since not using a finally block is a lot cheaper 
		// when we know for sure we won't be needing one.
		try {
			return tryBlock.base_apply(NATTable.EMPTY);
		} catch(InterpreterException e) {
			ATObject[] handlers = exceptionHandlers.asNativeTable().elements_;
			
			// find the appropriate handler
			for (int i = 0; i < handlers.length; i++) {
				ATHandler handler = handlers[i].asHandler();
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
	 * The <tt>try: { tryBlock} using: handler</tt> construct.
	 * 
	 * Ad-hoc code for one exception handler.
	 * 
	 * @see #base_try_usingHandlers_(ATClosure, ATTable)
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
	 * The <tt>try: { tryBlock} using: handler finally: { finallyBlock }</tt> construct.
	 * 
	 * Ad-hoc code for one exception handler.
	 * 
	 * @see #base_try_usingHandlers_finally_(ATClosure, ATTable, ATClosure)
	 */
	public ATObject base_try_using_finally_(ATClosure tryBlock, ATHandler handler, ATClosure finallyBlock) throws InterpreterException {
		try {
			return tryBlock.base_apply(NATTable.EMPTY);
		} catch(InterpreterException e) {
			ATObject exc = e.getAmbientTalkRepresentation();
			if (handler.base_canHandle(exc).asNativeBoolean().javaValue) {
				return handler.base_handle(exc);
			} else {
				throw e;
			}
		} finally {
			finallyBlock.base_apply(NATTable.EMPTY);
		}
	}
	
	/**
	 * The <tt>try: { tryBlock} using: handler1 using: handler2</tt> construct.
	 * 
	 * Ad-hoc code for two exception handlers.
	 * @see #base_try_usingHandlers_(ATClosure, ATTable)
	 */
	public ATObject base_try_using_using_(ATClosure tryBlock, ATHandler hdl1, ATHandler hdl2) throws InterpreterException {
		return base_try_usingHandlers_(tryBlock, NATTable.atValue(new ATObject[] { hdl1, hdl2 }));
	}
	
	/**
	 * The <tt>try: { tryBlock} using: handler1 using: handler2 finally: { finallyBlock }</tt> construct.
	 * 
	 * Ad-hoc code for two exception handlers.
	 * @see #base_try_usingHandlers_(ATClosure, ATTable)
	 */
	public ATObject base_try_using_using_finally_(ATClosure tryBlock, ATHandler hdl1, ATHandler hdl2, ATClosure finallyBlock) throws InterpreterException {
		return base_try_usingHandlers_finally_(tryBlock, NATTable.atValue(new ATObject[] { hdl1, hdl2 }), finallyBlock);
	}

	
	/**
	 * The <tt>try: { tryBlock} using: hdl1 using: hdl2 using: hdl3</tt> construct.
	 * 
	 * Ad-hoc code for three exception handlers
	 * @see #base_try_usingHandlers_(ATClosure, ATTable)
	 */
	public ATObject base_try_using_using_using_(ATClosure tryBlock, ATHandler hdl1, ATHandler hdl2, ATHandler hdl3) throws InterpreterException {
		return base_try_usingHandlers_(tryBlock, NATTable.atValue(new ATObject[] { hdl1, hdl2, hdl3 }));
	}
	
	/**
	 * The <tt>try: { tryBlock} using: hdl1 using: hdl2 using: hdl3 finally: { finallyBlock }</tt> construct.
	 * 
	 * Ad-hoc code for three exception handlers.
	 * @see #base_try_usingHandlers_(ATClosure, ATTable)
	 */
	public ATObject base_try_using_using_using_finally_(ATClosure tryBlock, ATHandler hdl1, ATHandler hdl2, ATHandler hdl3, ATClosure finallyBlock) throws InterpreterException {
		return base_try_usingHandlers_finally_(tryBlock, NATTable.atValue(new ATObject[] { hdl1, hdl2, hdl3 }), finallyBlock);
	}
	
	/**
	 * The <tt>try: { tryBlock} catch: type using: { |e| replacementCode }</tt>
	 * 
	 * 'Syntactic sugar' for one "in-line", native handler.
	 * @see #base_try_usingHandlers_(ATClosure, ATTable)
	 */
	public ATObject base_try_catch_using_(ATClosure tryBlock, ATTypeTag filter, ATClosure replacementCode) throws InterpreterException {
		return base_try_using_(tryBlock, new NATHandler(filter, replacementCode));
	}
	
	/**
	 * The <tt>try: { tryBlock} catch: type using: { |e| replacementCode } finally: { finallyBlock }</tt>
	 * 
	 * 'Syntactic sugar' for one "in-line", native handler.
	 * @see #base_try_usingHandlers_(ATClosure, ATTable)
	 */
	public ATObject base_try_catch_using_finally_(ATClosure tryBlock, ATTypeTag filter, ATClosure replacementCode, ATClosure finallyBlock) throws InterpreterException {
		return base_try_using_finally_(tryBlock, new NATHandler(filter, replacementCode), finallyBlock);
	}
	
	/**
	 * The <tt>try:catch:using:catch:using:</tt> construct.
	 * 
	 * <pre>try: {
	 *   tryBlock
	 * } catch: type using: { |e|
	 *   replacementCode
	 * } catch: type2 using: { |e|
	 *   replacementCode2
	 * }</pre>
	 * 
	 * 'Syntactic sugar' for two in-line handlers
	 * @see #base_try_usingHandlers_(ATClosure, ATTable)
	 */
	public ATObject base_try_catch_using_catch_using_(	ATClosure tryBlock,
														ATTypeTag filter1, ATClosure hdl1,
			                						   	ATTypeTag filter2, ATClosure hdl2) throws InterpreterException {
		return base_try_using_using_(tryBlock, new NATHandler(filter1, hdl1), new NATHandler(filter2, hdl2));
	}
	
	
	/**
	 * The <tt>try:catch:using:catch:using:finally:</tt> construct.
	 * 
	 * <pre>try: {
	 *   tryBlock
	 * } catch: type using: { |e|
	 *   replacementCode
	 * } catch: type2 using: { |e|
	 *   replacementCode2
	 * } finally: {
	 *   finalizationCode
	 * }</pre>
	 * 
	 * 'Syntactic sugar' for two in-line handlers
	 * @see #base_try_usingHandlers_(ATClosure, ATTable)
	 */
	public ATObject base_try_catch_using_catch_using_finally_(	ATClosure tryBlock,
														ATTypeTag filter1, ATClosure hdl1,
			                						   	ATTypeTag filter2, ATClosure hdl2, 
			                						   	ATClosure finallyBlock) throws InterpreterException {
		return base_try_using_using_finally_(tryBlock, new NATHandler(filter1, hdl1), new NATHandler(filter2, hdl2), finallyBlock);
	}
	
	/**
	 * The <tt>try:catch:using:catch:using:catch:using:</tt> construct.
	 * 
	 * <pre>try: {
	 *   tryBlock
	 * } catch: type using: { |e|
	 *   replacementCode
	 * } catch: type2 using: { |e|
	 *   replacementCode2
	 * } catch: type3 using: { |e|
	 *   replacementCode3
	 * }</pre>
	 * 
	 * 'Syntactic sugar' for three in-line handlers
	 * @see #base_try_usingHandlers_(ATClosure, ATTable)
	 */
	public ATObject base_try_catch_using_catch_using_catch_using_(ATClosure tryBlock,
															   ATTypeTag filter1, ATClosure hdl1,
															   ATTypeTag filter2, ATClosure hdl2,
															   ATTypeTag filter3, ATClosure hdl3) throws InterpreterException {
		return base_try_using_using_using_(tryBlock, new NATHandler(filter1, hdl1), new NATHandler(filter2, hdl2), new NATHandler(filter3, hdl3));
	}
	
	/**
	 * The <tt>try:catch:using:catch:using:catch:using:finally:</tt> construct.
	 * 
	 * <pre>try: {
	 *   tryBlock
	 * } catch: type using: { |e|
	 *   replacementCode
	 * } catch: type2 using: { |e|
	 *   replacementCode2
	 * } catch: type3 using: { |e|
	 *   replacementCode3
	 * }</pre>
	 * 
	 * 'Syntactic sugar' for three in-line handlers
	 * @see #base_try_usingHandlers_(ATClosure, ATTable)
	 */
	public ATObject base_try_catch_using_catch_using_catch_using_finally_(ATClosure tryBlock,
															   ATTypeTag filter1, ATClosure hdl1,
															   ATTypeTag filter2, ATClosure hdl2,
															   ATTypeTag filter3, ATClosure hdl3,
															   ATClosure finallyBlock) throws InterpreterException {
		return base_try_using_using_using_finally_(tryBlock, new NATHandler(filter1, hdl1), new NATHandler(filter2, hdl2), new NATHandler(filter3, hdl3), finallyBlock);
	}

	/**
	 * The <tt>handle: type with: { |e| replacementCode }</tt> construct.
	 * 
	 * @return a first-class handler from a filter prototype and some handler code.
	 * @see ATHandler for the interface to which a handler object responds.
	 */
	public ATObject base_handle_with_(ATTypeTag filter, ATClosure replacementCode) {
		return new NATHandler(filter, replacementCode);
	}
	
	/**
	 * The <tt>raise: exception</tt> construct.
	 * 
	 * Raises an exception which can be caught by dynamically installed try-catch-using blocks.
	 * @see #base_try_usingHandlers_(ATClosure, ATTable)
	 */
	public ATNil base_raise_(ATObject anExceptionObject) throws InterpreterException {
		throw Evaluator.asNativeException(anExceptionObject);
	}
	
	/* --------------------
	 * -- Unary Operators -
	 * -------------------- */
	
	/**
	 * The unary <tt>!</tt> primitive.
	 * <pre>!b == b.not()</pre>
	 * @param b the boolean to negate.
	 * @return the negation of the boolean.
	 */
	public ATBoolean base__opnot_(ATBoolean b) throws InterpreterException {
		return b.base_not();
	}
	
	/**
	 * The unary <tt>-</tt> primitive.
	 * <pre>-NUM(n) == 0 - n</pre>
	 * 
	 * @param n a number or a fraction to negate.
	 */
	public ATNumeric base__opmns_(ATNumeric n) throws InterpreterException {
		return NATNumber.ZERO.base__opmns_(n);
	}
	
	/**
	 * The unary <tt>+</tt> primitive.
	 * <pre>+NBR(n) == NBR(n)</pre>
	 */
	public ATNumber base__oppls_(ATNumber n) throws InterpreterException {
		return n;
	}
	
	/* -------------------
	 * -- Miscellaneous --
	 * ------------------- */
	
	/**
	 * The <tt>read:</tt> metaprogramming construct. Parses the given text string into an
	 * abstract syntax tree.
	 * 
	 * Example: <code>read: "x" => `x</code>
	 */
	public ATAbstractGrammar base_read_(ATText source) throws InterpreterException {
		return NATParser._INSTANCE_.base_parse(source);
	}
	
	/**
	 * The <tt>eval:in:</tt> metaprogramming construct.
	 * Evaluates the given AST in the context of the given object, returning its value.
	 * 
	 * Example: <code>eval: `x in: object: { def x := 1 } => 1</code>
	 * 
	 * This is a "dangerous" operation in the sense that lexical encapsulation of the given
	 * object can be violated.
	 */
	public ATObject base_eval_in_(ATAbstractGrammar ast, ATObject obj) throws InterpreterException {
		return ast.meta_eval(new NATContext(obj, obj));
	}

	/**
	 * The <tt>print:</tt> metaprogramming construct.
	 * This construct invokes the object mirror's <tt>print</tt> method.
	 * 
	 * @return a text string being a human-readable representation of the given object.
	 */
	public ATText base_print_(ATObject obj) throws InterpreterException {
		return obj.meta_print();
	}
	
	/**
	 * Compare the receiver object to the <tt>root</tt> object.
	 * the reason for this custom implementation: during the execution
	 * of this method, 'this' should refer to the global lexical scope object (the root),
	 * not to the OBJLexicalRoot instance.
	 * 
	 * When invoking one of these methods lexically, the receiver is always 'root'
	 * For example, <code>==(obj)</code> is equivalent to <code>root == obj</code> (or "root.==(obj)")
	 */
    public ATBoolean base__opeql__opeql_(ATObject comparand) throws InterpreterException {
        return Evaluator.getGlobalLexicalScope().base__opeql__opeql_(comparand);
    }
	
    /**
     * Instantiate the <tt>root</tt> object. I.e. <code>new()</code> is equivalent
     * to <tt>root.new()</tt>.
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

	public NATText meta_print() throws InterpreterException {
		return NATText.atValue("lexroot");
	}
    
}
