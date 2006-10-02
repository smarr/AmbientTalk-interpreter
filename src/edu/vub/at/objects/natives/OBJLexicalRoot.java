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

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.NATException;
import edu.vub.at.objects.ATAbstractGrammar;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATMirror;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATText;
import edu.vub.at.objects.mirrors.JavaClosure;
import edu.vub.at.objects.mirrors.NATMirageFactory;
import edu.vub.at.objects.mirrors.NATMirror;
import edu.vub.at.objects.mirrors.NATMirrorFactory;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.at.parser.NATParser;

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
 * 
 * Note that OBJLexicalRoot is a 'sentinel' class. The actual object bound to the
 * lexical root of an actor will be a normal NATObject which is assumed to be 'nested' in this instance.
 * This empty object is local to each actor and is mutable.
 */
public final class OBJLexicalRoot extends NATNil {
	
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
		code.getMethod().getBodyExpression().meta_eval(new NATContext(newObject, newObject, NATNil._INSTANCE_));
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
	public ATObject base_clone_(ATObject original) throws NATException {
		return original.meta_clone();
	}
	
	
	public ATObject base_mirror_(ATClosure code) throws NATException {
		
		/*
		 * mirageMaker is an object which implements the default behaviour for certain 
		 * methods of mirrors. These methods are all part of the extension and cloning
		 * protocol of objects, where special care needs to be taken to deal with the 
		 * doulbe object identity of mirages (mirage <-> custom mirror + object <-> 
		 * default behaviour).
		 */
		final ATObject mirageMaker = new NATObject(code.getContext().getLexicalScope(), NATMirror._PROTOTYPE_, NATObject._SHARES_A_);
		
		/*
		 * customMirror is a user-defined mirror object, which implements (part of) the 
		 * meta-object protocol using ordinary methods. Per construction new base-level 
		 * objects created from the mirror will forward meta_operations to their mirror
		 */
		final ATObject customMirror = mirageMaker.meta_extend(code);
		
		// Therefore, the mirageMaker which defines the default cloning and extension 
		// protocol for mirages does this by defining base-level methods newInstance,
		// extend, share and clone. If these methods are not shadowed by definitions 
		// in the customMirror object, lookup will resume in the mirageMaker.
		//
		// NOTE : if these methods need to be replaced with base_ methods, the mirageMaker
		// would need to be a subclass of NATNil to ensure that these methods are found 
		// before delegating to the default NATMirror. However, if the method would not
		// exist (consider e.g. the defineField method), then special care needs to be 
		// taken that the resulting exception would result in delegation. Given this rather
		// complex mechanism, we consider this solution to have simpler semantics.
		

		/*
		 * To manage the double identity of the mirage, we need a pointer to it from
		 * the mirageMaker, to be able to extend or share it.
		 */
		mirageMaker.meta_defineField(
				AGSymbol.alloc("mirage_"),
				NATNil._INSTANCE_);
		
		/*
		 * To manage allow for proper cloning of a mirage, the custom mirror object 
		 * needs to be cloned, hence we store it in the mirageMaker.
		 */
		mirageMaker.meta_defineField(
				AGSymbol.alloc("mirror_"),
				customMirror);
		
		/*
		 * Inside a JavaClosure this points to the closure, not to the surrounding 
		 * mirageMaker object. In the face of cloning, a self-pointer is direly 
		 * needed.
		 */
		mirageMaker.meta_defineField(
				AGSymbol.alloc("mirror_"),
				customMirror);
		
		/*
		 * Instead of delegating to the default behaviour which makes a new object,
		 * create a mirage such that the new meta-behaviour is used instead of the 
		 * default meta-behaviour which is hardwired into the evaluator.
		 */
		mirageMaker.meta_defineField(
				AGSymbol.alloc("newInstance"), 
				new JavaClosure(this) {
					public ATObject base_apply(ATTable initargs) throws NATException {
						ATObject newReflectee = NATMirageFactory.createMirage(customMirror);
						newReflectee.meta_invoke(newReflectee, Evaluator._INIT_, initargs);
						meta_assignField(
								AGSymbol.alloc("mirage_"),
								newReflectee);
						return newReflectee;
					}
				});
		
		/*
		 * Ensures the mirage is extended, not the default object.
		 */
		mirageMaker.meta_defineField(
				AGSymbol.alloc("extend"), 
				new JavaClosure(this) {
					public ATObject meta_apply(ATClosure code) throws NATException {
						ATObject mirage = meta_lookup(
								AGSymbol.alloc("mirage_"));
						
						ATObject extension = new NATObject(code.getContext().getLexicalScope(), mirage, NATObject._IS_A_);
						
						ATAbstractGrammar body = code.getMethod().getBodyExpression();
						body.meta_eval(new NATContext(extension, extension, this));
						
						return extension;
					}
				});
		
		/*
		 * Ensures the mirage is extended, not the default object.
		 */
		mirageMaker.meta_defineField(
				AGSymbol.alloc("share"), 
				new JavaClosure(this) {
					public ATObject meta_apply(ATClosure code) throws NATException {
						ATObject mirage = meta_lookup(
								AGSymbol.alloc("mirage_"));
						
						ATObject extension = new NATObject(code.getContext().getLexicalScope(), mirage, NATObject._SHARES_A_);
						
						ATAbstractGrammar body = code.getMethod().getBodyExpression();
						body.meta_eval(new NATContext(extension, extension, this));
						
						return extension;
					}
				});
		
		mirageMaker.meta_defineField(
				AGSymbol.alloc("clone"), 
				new JavaClosure(this) {
					public ATObject meta_apply() throws NATException {
						/*
						 * 1) Above the mirageMaker is a NATMirror instance which is linked to a unique 
						 * base object. Since this base object may be modified as a result of adding
						 * of fields and methods, the clone should also have these fields requiring 
						 * a clone of both the NATMirror and its base to be made. 
						 */
						ATMirror defaultMirror 	= this.getDynamicParent().asMirror();
						ATObject defaultBase		= defaultMirror.base_getBase();
						
						ATObject defaultMirrorClone = 
							defaultMirror.base_init(new ATObject[] { defaultBase.meta_clone() });
						
						// XXX This trick fools the cloning algorithm and makes a clone of the customMirror
						// transitively attached to the defaultMirrorClone, the pointer will be reset to our 
						// defaultMirror afterwards.
						
						
						/*
						 * 2) Clone the mirageMaker object itself. This implies that a new object will be
						 * returned yet it will not be bound to a new mirage yet. This clone will be 
						 * created by cloning the custom mirror object (the 'self') which is connected
						 * to the mirageMaker through an IS-A link. 
						 */
						// ATObject clone = this.meta_clone();
						
						/* 
						 * 3) Clone the custom mirror object. This results in transitively cloning the 
						 * mirageMaker and the defaultMirror. 
						 */
						ATObject customMirrorClone = 
							this.meta_lookup(
									AGSymbol.alloc("mirror_")).meta_clone();
						
						/*
						 * Our own clone is the dynamic parent of the cloned customMirror.
						 */
						ATObject mirageMakerClone = 
							customMirrorClone.getDynamicParent();
						
						//mirageMakerClone
						
						/*
						 * Update the clone's down-pointer to its mirror for consistent cloning
						 */
						mirageMakerClone.meta_assignVariable(
								AGSymbol.alloc("mirror_"),
								customMirrorClone);
						
						/*
						 * Finally, make a new mirage and register it with the cloned mirageMaker.
						 * 
						 * NOTE : Unlike when making a new instance, there is no init message sent. 
						 */
						ATObject newReflectee = NATMirageFactory.createMirage(customMirror);
						meta_assignField(
								AGSymbol.alloc("mirage_"),
								newReflectee);
						
						return newReflectee;
					}
				});
		return customMirror;
		
	}
	/* --------------------
	 * -- Unary Operators -
	 * -------------------- */
	
	/**
	 * The unary ! primitive:
	 * !b == b.not()
	 */
	public ATBoolean base__opnot_(ATBoolean b) throws NATException {
		return b.base_not();
	}
	
	/**
	 * The unary - primitive:
	 * -NBR(n) == NBR(-n)
	 */
	public ATNumber base__opmns_(ATNumber n) throws NATException {
		return NATNumber.atValue(- n.asNativeNumber().javaValue);
	}
	
	/**
	 * The unary + primitive:
	 * +NBR(n) == NBR(n)
	 */
	public ATNumber base__oppls_(ATNumber n) throws NATException {
		return n;
	}
	
	/* -------------------
	 * -- Miscellaneous --
	 * ------------------- */
	
	/**
	 * read: "text" => parses the given string into an AST
	 */
	public ATAbstractGrammar base_read_(ATText source) throws NATException {
		return NATParser._INSTANCE_.base_parse(source);
	}
	
	/**
	 * eval: ast in: obj => evaluates the given AST in the context of the given object, returning its value
	 */
	public ATObject base_eval_in_(ATAbstractGrammar ast, ATObject obj) throws NATException {
		return ast.meta_eval(new NATContext(obj, obj, obj.getDynamicParent()));
	}

	/**
	 * print: expression => string representing the expression
	 */
	public ATText base_print_(ATObject obj) throws NATException {
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
    public ATObject base_new(ATObject[] initargs) throws NATException {
	    return Evaluator.getGlobalLexicalScope().base_new(initargs);
    }
}
