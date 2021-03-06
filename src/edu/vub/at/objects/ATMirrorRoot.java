/**
 * AmbientTalk/2 Project
 * JavaInterfaceAdaptor.java created on Jul 13, 2006 at 10:25:01 PM
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
package edu.vub.at.objects;

import edu.vub.at.actors.ATAsyncMessage;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XDuplicateSlot;
import edu.vub.at.exceptions.XIllegalQuote;
import edu.vub.at.exceptions.XIllegalUnquote;
import edu.vub.at.exceptions.XObjectOffline;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XUndefinedSlot;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NATMirage;
import edu.vub.at.objects.natives.NATText;

/**
 * This is the interface of the root node of the intercessive mirrors delegation hierarchy.
 * <p>
 * Intercessive mirrors are always tied to a particular 'base' object.
 * The default intercessive mirror is named 'defaultMirror' and is an object
 * that understands all meta-level operations applicable on objects, implementing them using default semantics.
 * It can be thought of as being defined as follows:
 * 
 * <pre>
 * def mirrorroot := object: {
 *   def base := object: { nil } mirroredBy: self // base of the mirror root is an empty mirage
 *   def init(b) {
 *     base := b
 *   }
 *   def invoke(@args) { <default native invocation behaviour on base> }
 *   def select(@args) { <default native selection behaviour on base> }
 *   ...
 * } taggedAs: [ Mirror ]
 * </pre>
 * <p>
 * This object can then simply be extended / composed by other objects to deviate from the default semantics.
 * Note that the default semantics is applied to 'base' and *not* 'self.base', in other words:
 * although child mirrors can define their own 'base' field, it is not taken into consideration
 * by the mirror root. This also ensures that the mirror root is not abused to enact upon a mirage
 * for which it was not assigned to be the mirror.
 * <p>
 * Hence, 'mirrors' are simply objects with the same interface as this mirrorroot object: they should be
 * able to respond to all meta-level messages and have a 'base' field.
 * 
 * @author tvcutsem, smostinc
 */
public interface ATMirrorRoot extends ATObject {

	/**
	 * The read-only field containing the mirror's base-level mirage.
	 */
	public NATMirage base_base() throws InterpreterException;
	
	/* ------------------------------
     * -- Message Sending Protocol --
     * ------------------------------ */

   /**
    * This behavioural meta-level operation reifies the act of sending
    * an asynchronous message.
    * 
    * When the base-level AmbientTalk code <code>rcv<-m()</code> is
    * evaluated in the context of an object <tt>o</tt>, an asynchronous message
    * <code><-m()</code> is first created by the current actor mirror.
    * Subsequently, this message needs to be sent to the receiver. This
    * meta-level operation is reified by this method, as if by invoking:
    * <pre>(reflect: o).send(message)</pre>
    * The default behaviour is to access the current actor's mirror and to
    * ask the actor to send the message in this object's stead by invoking
    * <pre>actor.send(message)</pre>
    * @param receiver the object designated to receive the asynchronous message
    * @param message the asynchronous message to be sent by this object
    * 
    * @return the result of message sending, which will be the value of an
    * asynchronous message send expression.
    */
   public ATObject base_send(ATObject receiver, ATAsyncMessage message) throws InterpreterException;
   
   /**
    * This behavioural meta-level operation reifies the act of receiving
    * an asynchronous message.
    * 
    * When an asynchronous message is sent to an AmbientTalk object, its mirror
    * is notified of this event by the invocation of this method. The method
    * is invoked in the same execution turn as the turn in which the message
    * is sent. This allows the receiver (e.g. a custom eventual reference proxy)
    * to intervene in the message sending process and return a value different
    * than the default <tt>nil</tt> value.
    * <p>
    * The default behaviour of a mirror on a local reference in response to
    * the reception of an async
    * message is to schedule this message for execution in a later turn
    * in its owner's message queue. The actor will then later process
    * the message by invoking
    * <pre>msg.process(self)</pre>
    * In turn, the default message processing behaviour is to invoke
    * the method corresponding to the message's selector on this object.
    * Hence, usually a <tt>receive</tt> operation is translated into
    * a <tt>invoke</tt> operation in a later turn. The reason for having a
    * separate <tt>receive</tt>
    * operation is that this enables the AmbientTalk meta-level programmer to
    * distinguish between synchronously and asynchronously received messages.
    * 
    * Far references react to <tt>receive</tt> by transmitting their message
    * to their remote target.
    * 
    * @param message the message that was asynchronously sent to this object
    * @return <tt>nil</tt>, by default
    */
   public ATObject base_receive(ATAsyncMessage message) throws InterpreterException;
  
   /**
    * This meta-level operation reifies synchronous message sending ("method invocation").
    * Hence, the meta-level equivalent
    * of the base-level code <code>o.m()</code> is:
    * <pre>(reflect: o).invoke(o,`m,[])</pre>.
    * 
    * Method invocation comprises selector lookup and the application of the value
    * bound to the selector. Selector lookup first queries an object's local
    * fields, then the method dictionary:
    * <ul>
    *  <li>If the selector ends with <tt>:=</tt> and matches a field, the field
    *  is assigned if a unary argument list is specified (i.e. the field is treated
    *  as a mutator method).
    *  <li>Otherwise, if the selector is bound to a field containing
    * a closure, that closure is applied to the given arguments.
    *  <li>If the field is not bound to a closure, the field value is returned provided no arguments were
    * specified (i.e. the field is treated like an accessor method).
    *  <li>If the selector is bound to a method, the method is applied.
    *  <li>If the selector is not found, the search continues in the objects <i>dynamic parent</i>.
    * </ul>
    * <p>
    * Note also that the first argument to <tt>invoke</tt> denotes the
    * so-called "receiver" of the invocation. It is this object to which
    * the <tt>self</tt> pseudo-variable should be bound during method execution.
    * 
    * @see #base_doesNotUnderstand(ATSymbol) for what happens if the selector
    * is not found.
    *
    * @param delegate the object to which <tt>self</tt> is bound during execution
    * of the method
    * @param invocation an object encapsulating at least the invocation's
    *        <tt>selector</tt> (a {@link ATSymbol}) and <tt>arguments</tt> (a {@link ATTable}).
    * @return by default, the object returned from the invoked method
    */
   public ATObject base_invoke(ATObject delegate, ATMethodInvocation invocation) throws InterpreterException;

   /**
    * This meta-level operation reifies "field selection".
    * In other words, the base-level code
    * <code>o.m</code>
    * is interpreted at the meta-level as:
    * <code>(reflect: o).invokeField(o, `m)</code>
    * 
    * This meta-level operation is nearly identical to {@link #base_invoke(ATObject, ATMethodInvocation)} with one
    * important difference. When the selector is bound to a field storing a closure, this meta-level operation
    * does <b>not</b> auto-apply the closure, but returns the closure instead.
    * 
    * For all other cases, the following equality holds:
    * <code>o.m == o.m()</code>
    * or, at the meta-level:
    * <code>(reflect: o).invokeField(o, `m) == (reflect: o).invoke(o, MethodInvocation.new(`m, []))</code>
    * 
    * This effectively means that for client objects, it should not matter whether
    * a property is implemented as a field or as a pair of accessor/mutator methods.
    * 
    * @param receiver the base-level object from which the 'field' should be selected.
    * @param selector a symbol denoting the name of the method, accessor or mutator to be invoked
    * @return the value of a field, or the return value of a nullary method.
    */
   public ATObject base_invokeField(ATObject receiver, ATSymbol selector) throws InterpreterException;
   
   /**
    * This meta-level method is used to determine whether an object has a
    * field or method corresponding to the given selector, without actually invoking
    * or selecting any value associated with that selector.
    * <p>
    * The lookup process is the same as that for the <tt>invoke</tt> operation (i.e.
    * not only the object's own fields and methods are searched, but also those of
    * its dynamic parents).
    * 
    * @param selector a symbol denoting the name of a field (accessor or mutator) or method
    * @return a boolean denoting whether the object responds to <tt>o.selector</tt>
    */
   public ATBoolean base_respondsTo(ATSymbol selector) throws InterpreterException;

   /**
    * This behavioural meta-level operation reifies a failed dynamic method or field lookup.
    * 
    * When method invocation or field selection fails to find the selector in
    * the dynamic parent chain of an object, rather than immediately raising an
    * {@link XSelectorNotFound} exception, the mirror of the original receiver
    * of the method invocation or field selection is asked to handle failed lookup.
    * <p>
    * The default behaviour of <tt>doesNotUnderstand</tt> is to raise an
    * {@link XSelectorNotFound} exception.
    * <p>
    * This method is very reminiscent of Smalltalk's well-known
    * <tt>doesNotUnderstand:</tt> and of Ruby's <tt>method_missing</tt> methods.
    * There are, however, two important differences:
    * <ul>
    *  <li> <tt>doesNotUnderstand</tt> is a <b>meta</b>-level operation in AmbientTalk.
    *  It is an operation defined on mirrors, not on regular objects.
    *  <li> <tt>doesNotUnderstand</tt> in AmbientTalk relates to <i>attribute
    *  selection</i>, not to <i>method invocation</i>. Hence, this operation is
    *  more general in AmbientTalk than in Smalltalk: it intercepts both failed
    *  method invocations as well as failed field selections. Hence, it can be used
    *  to model "virtual" fields. This shows in the interface: this operation
    *  does not consume the actual arguments of a failed method invocation. Moreover,
    *  a closure should be returned which can subsequently be applied for failed invocations.
    *  Failed selections can simply return this closure without application. Hence, arguments
    *  should be consumed by means of currying, e.g. by making <tt>doesNotUnderstand</tt>
    *  return a block which can then take the arguments table as its sole parameter.
    * </ul>
    *
    * @param selector a symbol denoting the name of a method or field that could not be found
    * @return by default, this operation does not return a value, but raises an exception instead.
    * @throws edu.vub.at.exceptions.XSelectorNotFound the default reaction to a failed selection
    */
   public ATClosure base_doesNotUnderstand(ATSymbol selector) throws InterpreterException;

   /* -----------------------------
    * -- Object Passing protocol --
    * ----------------------------- */

   /**
    * This behavioural meta-level operation reifies object serialization.
    * 
    * When an AmbientTalk object crosses actor boundaries, e.g. by means of
    * parameter passing, as a return value or because it was explicitly
    * exported, this meta-level operation is invoked on the object's mirror.
    * <p>
    * This operation allows objects to specify themselves how they should
    * be parameter-passed during inter-actor communication. The interpreter
    * will never pass an object to another actor directly, but instead always
    * parameter-passes the <i>return value</i> of invoing <tt>pass()</tt> on
    * the object's mirror.
    * <p>
    * Mirrors on by-copy objects implement <tt>pass</tt> as follows:
    * <pre>def pass() { base }</pre>
    * Mirrors on by-reference objects implement <tt>pass</tt> by returning
    * a far reference to their base-level object.
    * 
    * @return the object to be parameter-passed instead of this object. For objects,
    * the default is a far reference to themselves. For isolates, the default is
    * to return themselves.
    */
	public ATObject base_pass() throws InterpreterException;

   /**
    * This behavioural meta-level operation reifies object deserialization.
    * 
    * When an AmbientTalk object has just crossed an actor boundary (e.g.
    * because of inter-actor message sending) this meta-level operation
    * is invoked on the object's mirror.
    * <p>
    * This meta-level operation gives objects a chance to tell the interpreter
    * which object they actually represent, because the object retained
    * after parameter passing is the return value of the <tt>resolve</tt>
    * operation.
    * <p>
    * Mirrors on by-copy objects, like isolates, implement <tt>resolve</tt> as follows:
    * <pre>def resolve() { base }</pre>
    * In other words, by-copy objects represent themselves. By-reference objects
    * are paremeter passed as far references. Mirrors on far references implement
    * <tt>resolve</tt> by trying to resolve the far reference into a local, regular
    * object reference (which is possible if the object they point to is located
    * in the actor in which they just arrived). If it is not possible to resolve
    * a far reference into a local object, the far reference remains a far reference.
    * <p>
    * Note that for isolates, this operation also ensures that the isolate's
    * lexical scope is rebound to the lexical root of the recipient actor.
    *  
    * @return the object represented by this object
    * @throws XObjectOffline if a far reference to a local object can no longer be resolved
    * because the object has been taken offline 
    */
	public ATObject base_resolve() throws InterpreterException;
   
   /* ------------------------------------------
    * -- Slot accessing and mutating protocol --
    * ------------------------------------------ */
   
   /**
    * This meta-level operation reifies first-class field or method selection. Hence, the
    * base-level evaluation of <code>o.&x</code> is interpreted at the meta-level as:
    * <pre>(reflect: o).select(o, `x)</pre>
    * 
    * The selector lookup follows the same search rules as those for <tt>invoke</tt>.
    * That is: first an object's local fields and method dictionary are searched,
    * and only then the object's <i>dynamic parent</i>.
    * <p>
    * The <tt>select</tt> operation can be used to both select fields or methods from
    * an object. When the selector is bound to a method, the return value of
    * <tt>select</tt> is a closure that wraps the found method in the object in which
    * the method was found. This ensures that the method retains its context information,
    * such as the lexical scope in which it was defined and the value of <tt>self</tt>, which
    * will be bound to the original receiver, i.e. the first argument of <tt>select</tt>.
    * <p>
    * If the selector matches a field, an accessor is returned. If the selector ends with
    * <tt>:=</tt>, a mutator is returned instead. An accessor is a nullary closure which upon
    * application yields the field's value. A mutator is a unary closure which upon
    * application assigns the field to the specified value.
	 * Even for fields already bound to a closure, selecting the field returns an accessor
	 * closure, not the bound closure itself.
    *
    * @see #base_doesNotUnderstand(ATSymbol) for what happens if the selector is not found.
    *
    * @param receiver the dynamic receiver of the selection. If the result of the selection is
    * a method, the closure wrapping the method will bind <tt>self</tt> to this object.
    * @param selector a symbol denoting the name of the field or method to select.
    * @return if selector is bound to a field, an accessor or mutator for the field; otherwise if
    * the selector is bound to a method, a closure wrapping the method.
    */
   public ATClosure base_select(ATObject receiver, ATSymbol selector) throws InterpreterException;

   /**
    * This meta-level operation reifies field definition. Hence, the base-level
    * code <code>def x := v</code> evaluated in a lexical scope <tt>lex</tt>
    * is interpreted at the meta-level as:
    * <pre>(reflect: lex).defineField(`x, v)</pre>
    * 
    * Invoking this meta-level operation on an object's mirror adds a new field
    * to that object. An object cannot contain two or more fields with the
    * same name.
    *
    * @param name a symbol denoting the name of the new field
    * @param value the value of the new field
    * @return nil
    * @throws edu.vub.at.exceptions.XDuplicateSlot if the object already has a
    * local field with the given name
    */
   public ATNil base_defineField(ATSymbol name, ATObject value) throws InterpreterException;

   /* -----------------------------------------
     * -- Cloning and instantiation protocol --
     * ---------------------------------------- */

   /**
    * This meta-level operation reifies the act of cloning the base-level object.
    * Hence, the code <code>clone: o</code> is interpreted at the meta-level as
    * <pre>(reflect: o).clone()</pre>
    * 
    * AmbientTalk's default cloning semantics are based on shallow copying.
    * A cloned object has copies of the original object's fields, but the values
    * of the fields are shared between the clones. A clone has the same methods
    * as the original object. Methods added at a later stage to the original
    * will not affect the clone's methods and vice versa. This means that each
    * objects has its own independent fields and methods.
    * <p>
    * If the cloned AmbientTalk object contains programmer-defined field objects,
    * each of these fields is re-instantiated with the clone as a parameter. The
    * clone is intialized with the re-instantiated fields rather than with the
    * fields of the original object. This property helps to ensure that each
    * object has its own independent fields.
    * <p>
    * If the object has a <i>shares-a</i> relationship with its parent, the object
    * and its clone will <b>share</b> the same parent object. Shares-a relationships
    * are the default in AmbientTalk, and they match with the semantics of
    * shallow copying: the dynamic parent of an object is a regular field, hence
    * its contents is shallow-copied.
    * <p>
    * If the object has an <i>is-a</i> relationship with its parent object, a
    * clone of the object will receive a clone of the parent object as its parent.
    * Hence, is-a relationships "override" the default shallow copying semantics
    * and recursively clone the parent of an object up to a shares-a relationship.
    * <p>
    * If a mirage is cloned, its mirror is automatically re-instantiated with
    * the new mirage, to ensure that each mirage has its independent mirror.
    * @return a clone of the mirror's <tt>base</tt> object
    */
	public ATObject base_clone() throws InterpreterException;

   /**
    * This meta-level operation reifies instance creation. The default
    * implementation of an AmbientTalk object's <tt>new</tt> method is:
    * <pre>def new(@initargs) { (reflect: self).newInstance(initargs) }</pre>
    * 
    * Creating a new instance of an object is a combination of:
    * <ul>
    *  <li>creating a clone of the object
    *  <li>initializing the clone by invoking its <tt>init</tt> method
    * </ul>
    * 
    * The default implementation is:
    * <pre>def newInstance(initargs) {
    *  def instance := self.clone();
    *  instance.init(@initargs);
    *  instance;
    *}
    * </pre>
    * 
    * Instance creation in AmbientTalk is designed to mimick class instantiation
    * in a class-based language. Instantiating a class <tt>c</tt> requires <i>allocating</i>
    * a new instance <tt>i</tt> and then invoking the <i>constructor</i> on that
    * new instance. In AmbientTalk, class allocation is replaced by object
    * <i>cloning</i>. The benefit is that an instantiated object its variables are
    * already initialized to useful values, being those of the object from which
    * it is instantiated. The <tt>init</tt> method plays the role of "constructor"
    * in AmbientTalk.
    *
    * @param initargs a table denoting the actual arguments to be passed to
    * the <tt>init</tt> method
    * @return the new instance
    */
   public ATObject base_newInstance(ATTable initargs) throws InterpreterException;

   /* ---------------------------------
     * -- Structural Access Protocol  --
     * --------------------------------- */

   /**
    * This structural meta-level operation adds a field object to the receiver mirror's
    * base object. An object cannot contain two or more fields with the same name.
    * 
    * Note that the field object passed as an argument serves as a <i>prototype</i>
    * object: the actual field object added is an <i>instance</i> of the passed field object.
    * A field object should always have an <tt>init</tt> method that takes as an argument
    * the new host object to which it is added. This is often useful, as the behaviour
    * of a field may depend on the object in which it resides. Because <tt>addField</tt>
    * creates a new instance of the field, this gives the field object a chance to
    * properly refer to its new host. 
    * <p>
    * As an example, here is how to add a read-only field <tt>foo</tt> initialized
    * to <tt>5</tt> to an object <tt>obj</tt>:
    * <pre>def makeConstantField(nam, val) {
    *   object: {
    *     def new(newHost) { self }; // singleton pattern
    *     def name := nam;
    *     def readField() { val };
    *     def writeField(newVal) { nil };
    *   }
    * };
    * (reflect: obj).addField(makeConstantField(`foo, 5));
    * </pre>
    * 
    * @param field the prototype field object whose instance should be added
    * to the receiver's base object
    * @return nil
    * @throws XDuplicateSlot if the base object already has a field with the
    * same name as the new field
    */
   public ATNil base_addField(ATField field) throws InterpreterException;

   /**
    * This structural meta-level operation adds a method to the receiver
    * mirror's base object. An object cannot contain two or more methods
    * with the same name.
    * 
    * @param method a method object to add to the receiver's base object's
    * method dictionary.
    * @return nil
    * @throws XDuplicateSlot if a method with the new method's selector already
    * exists in the base object.
    */
   public ATNil base_addMethod(ATMethod method) throws InterpreterException;

   /**
    * This structural meta-level operation allows the metaprogrammer to reify a
    * field of the receiver mirror's base object. Hence, unlike <tt>select</tt>
    * and <tt>lookup</tt>, <tt>grabField</tt> returns a <i>field object</i> rather
    * than the <i>value</i> bound to the field. For example: one could express
    * <code>obj.super := val</code> at the meta-level as:
    * 
    * <pre>
    * def superField := (reflect: obj).grabField(`super);
    * superField.writeField(val);
    * </pre>
    *
    * Another important difference between <tt>select</tt>, <tt>lookup</tt> and
    * <tt>grabField</tt> is that <tt>grabField</tt> only considers the fields
    * <i>local</i> to the receiver's base object. Fields of lexical or dynamic
    * parent objects are <i>not</i> considered.
    *
    * @param selector a symbol representing the name of the field to select.
    * @return a mirror on this object's field slot.
    * @throws XUndefinedSlot if the field cannot be found within the receiver's
    * base object.
    */
   public ATField base_grabField(ATSymbol selector) throws InterpreterException;

   /**
    * This structural meta-level operation allows the metaprogrammer to
    * reify a method defined on the receiver mirror's base object. Note that,
    * unlike the <tt>select</tt> or <tt>lookup</tt> operations, <tt>grabMethod</tt>
    * returns the bare method object, i.e. <i>not</i> a closure wrapping the method.
    * <p>
    * Also, unlike <tt>select</tt> and <tt>lookup</tt>, <tt>grabField</tt> only
    * considers the locally defined methods of an object, methods of lexical or
    * dynamic parent objects are <i>not</i> considered.
    *
    * @param selector a symbol representing the name of the method to grab from
    * the receiver's base object.
    * @return the bare method object bound to the given selector.
    * @throws XSelectorNotFound if the method object cannot be found within the
    * receiver's base object.
    */
   public ATMethod base_grabMethod(ATSymbol selector) throws InterpreterException;

   /**
    * This structural meta-level operation allows access to all of the
    * fields defined on the receiver mirror's base object. Note that
    * this method only returns the base object's <i>locally</i> defined
    * fields. Fields from parent objects are not returned.
    * 
    * @see ATMirrorRoot#base_grabField(ATSymbol) for details about the returned
    * field objects. 
    * @return a table of field objects (of type {@link ATField}).
    */
   public ATTable base_listFields() throws InterpreterException;

   /**
    * This structural meta-level operation allows access to all of the
    * methods defined on the receiver mirror's base object. Note that
    * this method only returns the base object's <i>locally</i> defined
    * methods. Methods from parent objects are not returned.
    * 
    * @see ATMirrorRoot#base_grabMethod(ATSymbol) for details about the returned
    * method objects.
    * @return a table of method objects (of type {@link ATMethod}).
    */
   public ATTable base_listMethods() throws InterpreterException;

   /**
    * This structural meta-level operation adds a slot object to the receiver mirror's
    * base object. An object cannot contain two or more slots with the same name.
    * 
    * A slot is either a method or a closure. A closure serves to encapsulate access to
    * or mutation of a field.
    * 
    * Care must be taken with closures when the object to which they are added is
    * cloned or instantiated: the closure will be shared between clones!
    * <p>
    * As an example, here is how to add a read-only field <tt>foo</tt> initialized
    * to <tt>5</tt> to an object <tt>obj</tt>:
    * <pre>
    * def [accessor,mutator] := /.at.lang.values.createFieldSlot(`foo, 5);
    * (reflect: obj).addSlot(accessor);
    * </pre>
    * 
    * @param slot the method representing the slot to be added
    * to the receiver's base object
    * @return nil
    * @throws XDuplicateSlot if the base object already has a slot with the
    * same name as the new slot
    */
   public ATNil base_addSlot(ATMethod slot) throws InterpreterException;

   /**
    * This structural meta-level operation allows the metaprogrammer to reify a
    * slot of the receiver mirror's base object. Hence, unlike <tt>select</tt>
    * and <tt>lookup</tt>, <tt>grabSlot</tt> returns a <i>slot object</i> rather
    * than the <i>value</i> bound to the slot. For example: one could express
    * <code>obj.super := val</code> at the meta-level as:
    * 
    * <pre>
    * def superMutator := (reflect: obj).grabSlot(`super:=);
    * superMutator(val);
    * </pre>
    *
    * Another important difference between <tt>select</tt>, <tt>lookup</tt> and
    * <tt>grabSlot</tt> is that <tt>grabSlot</tt> only considers the slots
    * <i>local</i> to the receiver's base object. Slots of lexical or dynamic
    * parent objects are <i>not</i> considered.
    *
    * @param selector a symbol representing the name of the slot to select.
    * @return a method representing the selected slot.
    * @throws XUndefinedSlot if the field cannot be found within the receiver's
    * base object.
    */
   public ATMethod base_grabSlot(ATSymbol selector) throws InterpreterException;

   /**
    * This structural meta-level operation allows access to all of the
    * slots defined on the receiver mirror's base object. Note that
    * this method only returns the base object's <i>locally</i> defined
    * slots. Slots from parent objects are not returned.
    * 
    * @see ATMirrorRoot#base_grabSlot(ATSymbol) for details about the returned
    * slot objects. 
    * @return a table of slot objects (of type {@link ATMethod}).
    */
   public ATTable base_listSlots() throws InterpreterException;
   
   /**
    * This structural meta-level operation removes a slot from the
    * object. Note that this method only removes slots that are
    * <i>locally</i> defined in the object.
    * 
    * @param selector the name of the slot to remove
    * @return the value to which the slot was previously bound
    * (e.g. a value or a method object)
    * @throws XSelectorNotFound if no slot with the given name is found in the object
    */
   public ATObject base_removeSlot(ATSymbol selector) throws InterpreterException;
   
   /**
    * This structural meta-level operation returns whether or not
    * the receiver mirror's base object is an <i>extension</i> of its
    * parent object.
    * <p>
    * In AmbientTalk, all objects are part of a dynamic parent delegation chain:
    * each object has a <tt>super</tt> field that denotes the object to which to
    * delegate messages the object cannot understand itself. There are, however,
    * two kinds of delegation links:
    * <ul>
    *  <li><b>IS-A</b> links: this kind of link denotes that the child object is
    *  a true extension of its parent, and cannot meaningfully exist without the
    *  parent's state. When the child is cloned, its parent will be cloned as well.
    *  <li><b>SHARES-A</b> links: this kind of link denotes that the child object
    *  simply delegates to its parent for purposes of sharing or code reuse. The
    *  child can meaningfully exist without the parent's state. When the child is
    *  cloned, the clone will delegate to the same parent.
    * </ul>
    *
    * Examples:
    * <pre>(reflect: (extend: parent with: code)).isExtensionOfParent() => true
    *(reflect: (share: parent with: code)).isExtensionOfParent() => false
    * </pre>
    * 
    * Note that accessing the dynamic parent itself is not a meta-level operation,
    * the dynamic parent can simply be accessed from the base level by performing
    * <code>obj.super</code>.
    * 
    * @return whether the base object extends its parent object via an
    * <b>IS-A</b> link or not.
    */
   public ATBoolean base_isExtensionOfParent() throws InterpreterException;
   
   /* ------------------------------------------
     * -- Abstract Grammar evaluation protocol --
     * ------------------------------------------ */

   /**
    * This behavioural meta-level operation reifies the evaluation of
    * abstract grammar objects into values. For objects, this operation
    * returns the base object itself, signifying that the evaluation
    * function defined on objects is the identity function. In other words,
    * objects are <i>self-evaluating</i>. Parse tree objects (first-class
    * abstract grammar elements), however, have dedicated evaluation
    * functions. For example, evaluating <code>x</code> is equivalent to
    * evaluating <code>(reflect: `x).eval(ctx)</code> where <tt>ctx</tt>
    * is a reification of the current evaluation context.
    * 
    * @param ctx a context object that stores the current lexical scope and
    * the current value of <tt>self</tt>
    * @return the value of the abstract grammar element denoted by this mirror's
    * base object.
    * @throws XIllegalUnquote if an unquote abstract grammar element is evaluated. Such
    * abstract grammar elements should only be encountered in a quoted parse tree.
    */
   public ATObject base_eval(ATContext ctx) throws InterpreterException;

   /**
    * This behavioural meta-level operation reifies the quotation of
    * abstract grammar elements. Regular objects simply return themselves
    * upon quotation. When an abstract grammar element is quoted, rather
    * than tree-recursively invoking <tt>eval</tt> on the parse trees,
    * <tt>quote</tt> is tree-recursively invoked. When encountering
    * an unquote, <tt>eval</tt> is again invoked on the unquoted subtree,
    * with the context passed as an argument to <tt>quote</tt>.
    * 
    * @param ctx a context object passed on to be used in subsequent evaluations.
    * @throws XIllegalQuote exception whenever an unquote-splice unquotation is discovered
    * in an Abstract Grammar node where the resulting table cannot be spliced.
    */
   public ATObject base_quote(ATContext ctx) throws InterpreterException;

   /**
    * This behavioural meta-level operation reifies the act of printing
    * the base object in the read-eval-print loop. This operation may be
    * overridden by mirrors to customise the printed representation of
    * their base object.
    * 
    * @return a text value denoting a human-readable representation of the object.
    */
   public NATText base_print() throws InterpreterException;
   
   /**
    * This behavioural meta-level operation reifies the act of representing
    * the base object as self-containing source code. This operation may be
    * overridden by mirrors to customise the printed representation of
    * their base object.
    * 
    * @return a text value representating the source code of the object.
    */
   public NATText base_asCode()  throws InterpreterException;

   /* ----------------------------------
    * -- Object Relational Comparison --
    * ---------------------------------- */
   
   /**
    * This meta-level operation determines whether this mirror's base object
    * is related to the parameter object by a combination of cloning and
    * extension operators. The default implementation is:
    * 
    * <pre>def isRelatedTo(object) {
    *  self.isCloneOf(object).or: { (reflect: base.super).isRelatedTo(object) }
    *}</pre>
    * 
    * @param object the object to compare this mirror's base object to
    * @return true if the given object is a clone of the base object or a clone
    * of the base object's parents.
    */
   public ATBoolean base_isRelatedTo(ATObject object) throws InterpreterException;
   
   /**
    * This meta-level operation determines whether this mirror's base object
    * is a clone of the parameter object. The <i>is-clone-of</i> relation is transitive,
    * so if <tt>martin</tt> is a clone of <tt>sally</tt> and <tt>sally</tt> is a clone of
    * <tt>dolly</tt>, then <tt>martin</tt> is a clone of <tt>dolly</tt> as well.
    * The relation is reflexive: <tt>dolly</tt> is a clone of itself.
    * The relation is symmetric: <tt>dolly</tt> is also a clone of <tt>sally</tt>.
    * 
    * @param other the object to check the is-clone-of relationship with.
    * @return true if the base object and the parameter object are clones (i.e. one
    * was created by cloning the other), false otherwise.
    */
   public ATBoolean base_isCloneOf(ATObject other) throws InterpreterException;

   /* ---------------------------------
    * -- Type Testing and Querying --
    * --------------------------------- */
   
   /**
    * Tests whether the receiver mirror's base object is tagged as a particular type.
    * 
    * The default implementation first compares the object's local type tags to the given type
    * by means of the {@link ATTypeTag#base_isSubtypeOf(ATTypeTag)} method. If no local type
    * is found, the test is applied recursively on this object's dynamic parent. In code:
    * <pre>def isTaggedAs(type) {
    *  (nil != (self.tagsOf: object).find: { |localType|
	 *    localType.isSubtypeOf(type)
	 *  }).or: { (reflect: base.super).isTaggedAs(type) }
	 * };
    * </pre>
    * 
    * The primitive method <tt>is: obj taggedAs: type</tt> is defined in terms of this
    * method:
    * <pre>
    * def is: obj taggedAs: type {
    *  (reflect: obj).isTaggedAs(type)
    *};
    * </pre>
    * 
    * @param type the type tag object to check for
    * @return true if this mirror's base object or one of its parent objects is tagged
    * with a subtype of the given type, false otherwise.
    */
   public ATBoolean base_isTaggedAs(ATTypeTag type) throws InterpreterException;
   
   /**
    * Returns all of the local type tags of this object. The primitive method
    * <tt>tagsOf: obj</tt> is defined in terms of this method:
    * 
    * <pre>
    * def tagsOf: obj {
    *  (reflect: obj).typeTags
    *};
    * </pre>
    * 
    * @return a table of the type tags that were attached directly to this mirror's base
    * object. The type tags of its parent objects are not returned.
    */
	public ATTable base_typeTags() throws InterpreterException;

}