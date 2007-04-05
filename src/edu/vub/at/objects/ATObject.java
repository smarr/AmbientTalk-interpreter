/**
 * AmbientTalk/2 Project
 * ATObject.java created on 13-jul-2006 at 15:33:42
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
import edu.vub.at.exceptions.XObjectOffline;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XUnassignableField;
import edu.vub.at.exceptions.XUndefinedField;
import edu.vub.at.objects.coercion.ATConversions;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NATMirage;
import edu.vub.at.objects.mirrors.OBJMirrorRoot;
import edu.vub.at.objects.natives.NATCallframe;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATObject;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.symbiosis.JavaClass;
import edu.vub.at.objects.symbiosis.JavaObject;

/**
 * ATObject represents the public interface common to any AmbientTalk/2 object.
 * Any value representing an ambienttalk object should implement this interface.
 * 
 * More specifically, this interface actually defines two interfaces all at once:
 * <ul>
 *  <li>A <b>base-level</b> interface to AmbientTalk objects, describing all
 *      methods and fields that a regular AmbientTalk object understands.
 *  <li>A <b>meta-level</b> interface to AmbientTalk objects, describing all
 *      methods and fields that the <b>mirror</b> on any AmbientTalk object
 *      understands.
 * </ul>
 * 
 * In the AmbientTalk/2 Interpreter implementation, there are only a few classes
 * that (almost) fully implement this interface. The principal implementors are:
 * 
 * <ul>
 *   <li>{@link NATNil}: provides a default implementation for all <i>native</i> data types.
 *   For example, native methods, closures, abstract grammar nodes, booleans, numbers, etc.
 *   are all represented as AmbientTalk objects with 'native' behaviour.
 *   <li>{@link NATCallframe}: overrides most of the default behaviour of {@link NATNil} to
 *   implement the behaviour of call frames, also known as <i>activation records</i>. In
 *   AmbientTalk, call frames are the objects that together define the runtime stack.
 *   They are objects with support for fields but without support for actual methods.
 *   <li>{@link NATObject}: extends the behaviour of call frames to include support for
 *   full-fledged, programmer-defined objects with support for methods and delegation.
 *   <li>{@link JavaClass} and {@link JavaObject}: adapt the behaviour of native AmbientTalk
 *   objects to engage in symbiosis with either a Java class or a Java object.
 *   This implementation makes use of the Java Reflection API to regard Java objects
 *   as though it were AmbientTalk objects.
 *   <li>{@link NATMirage} and {@link OBJMirrorRoot}: these two classes work in tandem to
 *   enable reflection on AmbientTalk objects. That is, because of these two classes, an
 *   AmbientTalk programmer can himself invoke the methods provided in this interface.
 *   {@link NATMirage} implements each operation in this interface by forwarding a
 *   downed invocation to a custom so-called <i>mirror</i> object. This mirror object
 *   can delegate to {@link OBJMirrorRoot}, which is a special object that implements
 *   each meta-level operation of this interface as a base-level operation. Hence, in
 *   a sense, {@link OBJMirrorRoot} also 'implements' this interface, but at the
 *   AmbientTalk level, rather than at the Java level.
 * </ul>
 * 
 * @author tvcutsem
 */
public interface ATObject extends ATConversions {

    /* ------------------------------
      * -- Message Sending Protocol --
      * ------------------------------ */

    /**
     * When the base-level AmbientTalk code <code>rcv<-m()</code> is
     * evaluated in the context of an object <tt>o</tt>, an asynchronous message
     * <code><-m()</code> is first created by the current actor mirror.
     * Subsequently, this message needs to be sent to the receiver. This
     * meta-level operation is reified by this method, as if by invoking:
     * <pre>(reflect: o).send(message)</pre>
     * The default behaviour is to access the current actor's mirror and to
     * ask the actor to send the message in this object's stead by invoking
     * <pre>actor.send(message)</pre>
     * 
     * @param message the asynchronous message to be sent by this object
     * @return the result of message sending, which will be the value of an
     * asynchronous message send expression.
     */
    public ATObject meta_send(ATAsyncMessage message) throws InterpreterException;
    
    /**
     * When an AmbientTalk object receives a message that was sent to it
     * asynchronously, the message is delivered to the object's mirror by
     * means of this meta-level operation.
     * <p>
     * The default behaviour of a mirror in response to the reception of a
     * message <tt>msg</tt> is to invoke:
     * <pre>msg.process(self)</pre>
     * In turn, the default message processing behaviour is to invoke
     * the method corresponding to the message's selector on this object.
     * Hence, usually a <tt>receive</tt> operation is simply translated into
     * a <tt>invoke</tt> operation. The reason for having a separate <tt>receive</tt>
     * operation is that this enables the AmbientTalk meta-level programmer to
     * distinguish between synchronously and asynchronously received messages.
     * @param message the message that was asynchronously sent to this object
     * @return by default, the value of invoking the method corresponding to the message
     */
    public ATObject meta_receive(ATAsyncMessage message) throws InterpreterException;
   
    /**
     * This meta-level operation reifies the act of synchronous message sending
     * (better known as "method invocation"). Hence, the meta-level equivalent
     * of the base-level code <code>o.m()</code> is:
     * <pre>(reflect: o).invoke(o,`m,[])</pre>.
     * 
     * Method invocation comprises selector lookup and the application of the value
     * bound to the selector. Selector lookup first queries an object's local
     * fields, then the method dictionary. If the selector is not found, the
     * search continues in the objects <i>dynamic parent</i>.
     * <p>
     * Note also that the first argument to <tt>invoke</tt> denotes the
     * so-called "receiver" of the invocation. It is this object to which
     * the <tt>self</tt> pseudo-variable should be bound during method execution.
     * 
     * @see #meta_doesNotUnderstand(ATSymbol) for what happens if the selector
     * is not found.
     *
     * @param receiver the object to which <tt>self</tt> is bound during execution
     * of the method
     * @param selector a symbol denoting the name of the method to be invoked
     * @param arguments the table of actual arguments to be passed to the method
     * @return by default, the object returned from the invoked method
     */
    public ATObject meta_invoke(ATObject receiver, ATSymbol selector, ATTable arguments) throws InterpreterException;

    /**
     * This meta-level method is used to determine whether an object has a
     * field or method corresponding to the given selector, without actually invoking
     * or selecting any value associated with that selector.
     * <p>
     * The lookup process is the same as that for the <tt>invoke</tt> operation (i.e.
     * not only the object's own fields and methods are searched, but also those of
     * its dynamic parents).
     * 
     * @param selector a symbol denoting the name of a field or method
     * @return a boolean denoting whether the object responds to <tt>o.selector</tt>
     */
    public ATBoolean meta_respondsTo(ATSymbol selector) throws InterpreterException;

    /**
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
     *  does not consume the actual arguments of a failed method invocation. These
     *  should be consumed by means of currying, e.g. by making <tt>doesNotUnderstand</tt>
     *  return a block which can then take the arguments table as its sole parameter.
     * </ul>
     *
     * @param selector a symbol denoting the name of a method or field that could not be found
     * @return by default, this operation does not return a value, but raises an exception instead.
     * @throws edu.vub.at.exceptions.XSelectorNotFound the default reaction to a failed selection
     */
    public ATObject meta_doesNotUnderstand(ATSymbol selector) throws InterpreterException;

    /* -----------------------------
     * -- Object Passing protocol --
     * ----------------------------- */

    /**
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
    public ATObject meta_pass() throws InterpreterException;

    /**
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
    public ATObject meta_resolve() throws InterpreterException;
    
    /* ------------------------------------------
     * -- Slot accessing and mutating protocol --
     * ------------------------------------------ */
    
    /**
     * This meta-level operation reifies field or method selection. Hence, the
     * base-level evaluation of <code>o.x</code> is interpreted at the meta-level as:
     * <pre>(reflect: o).select(o, `x)</pre>
     * 
     * The selector lookup follows the same search rules as those for <tt>invoke</tt>.
     * That is: first an object's local fields are searched, then the local method dictionary,
     * and then the object's <i>dynamic parent</i>.
     * <p>
     * The <tt>select</tt> operation can be used to both select fields or methods from
     * an object. When the selector is bound to a method, the return value of
     * <tt>select</tt> is a closure that wraps the found method in the object in which
     * the method was found. This ensures that the method retains its context information,
     * such as the lexical scope in which it was defined and the value of <tt>self</tt>, which
     * will be bound to the original receiver, i.e. the first argument of <tt>select</tt>.
     *
     * @see #meta_doesNotUnderstand(ATSymbol) for what happens if the selector is not found.
     *
     * @param receiver the dynamic receiver of the selection. If the result of the selection is
     * a method, the closure wrapping the method will bind <tt>self</tt> to this object.
     * @param selector a symbol denoting the name of the field or method to select.
     * @return if selector is bound to a field, the value of the field; otherwise if
     * the selector is bound to a method, a closure wrapping the method.
     */
    public ATObject meta_select(ATObject receiver, ATSymbol selector) throws InterpreterException;

    /**
     * This meta-level operation reifies variable lookup. Hence, the base-level code
     * <code>x</code> evaluated in the lexical scope <tt>lex</tt> is interpreted at
     * the meta-evel as:
     * <pre>(reflect: lex).lookup(`x)</pre>
     * 
     * Variable lookup first queries the local fields of this object, then the local
     * method dictionary. If the selector is not found, the search continues in
     * this object's <i>lexical parent</i>. Hence, variable lookup follows
     * <b>lexical scoping rules</b>.
     * <p>
     * Similar to the behaviour of <tt>select</tt>, if the selector is bound to a
     * method rather than a field, <tt>lookup</tt> returns a closure wrapping the method
     * to preserve proper lexical scoping and the value of <tt>self</tt> for the found
     * method.
     * <p>
     * Note that, unlike <tt>invoke</tt> and <tt>select</tt>, <tt>lookup</tt> does
     * not give rise to the invocation of <tt>doesNotUnderstand</tt> if the selector
     * was not found. The reason for this is that lexical lookup is a static process
     * for which it makes less sense to provide dynamic interception facilities.
     *
     * @param selector a symbol denoting the name of the field or method to look up lexically.
     * @return if selector is bound to a field, the value of the field; otherwise if selector
     * is bound to a method, a closure wrapping the method.
     * @throws XUndefinedField if the selector could not be found in the lexical scope of this object
     */
    public ATObject meta_lookup(ATSymbol selector) throws InterpreterException;

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
    public ATNil meta_defineField(ATSymbol name, ATObject value) throws InterpreterException;

    /**
     * This meta-level operation reifies variable assignment. Hence, the base-level
     * code <code>x := v</code> evaluated in a lexical scope <tt>lex</tt>
     * is interpreted at the meta-level as:
     * <pre>(reflect: lex).assignVariable(`x, v)</pre>
     * 
     * When <tt>assignVariable</tt> is invoked on an object's mirror, the variable
     * to assign is looked up according to rules similar to those defined by <tt>lookup</tt>.
     * First, the object's local fields are checked. If the selector is not found there,
     * the fields of the <i>lexical parent</i> of the object are searched recursively.
     * Hence, variable assignment follows <b>lexical scoping rules</b>.
     * Note that local methods are always disregarded: methods are not assignable.
     * <p>
     * When the lookup is successful, the value bound to the found field is assigned
     * to the given value.
     *
     * @param name a symbol representing the name of the variable to assign.
     * @param value the value to assign to the variable.
     * @return nil
     * @throws XUnassignableField if the variable to assign to cannot be found.
     */
    public ATNil meta_assignVariable(ATSymbol name, ATObject value) throws InterpreterException;

    /**
     * This meta-level operation reifies field assignment. Hence, the base-level
     * code <code>o.x := v</code> is interpreted at the meta-level as:
     * <pre>(reflect: o).assignField(`x, v)</pre>
     * 
     * When <tt>assignField</tt> is invoked on an object's mirror, the field
     * to assign is looked up according to rules similar to those defined by
     * <tt>invoke</tt> and <tt>select</tt>. First, the object's local fields
     * are checked. If the selector is not found there, the fields of its
     * <i>dynamic parent</i> are searched recursively. Note that local methods
     * are always disregarded: methods are not assignable.
     * <p>
     * When the lookup is successful, the value bound to the found field is assigned
     * to the given value.
     *
     * @param name a symbol representing the name of the field to assign.
     * @param value the value to assign to the field.
     * @return nil
     * @throws XUnassignableField if the field to assign to cannot be found.
     */
    public ATNil meta_assignField(ATObject receiver, ATSymbol name, ATObject value) throws InterpreterException;

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
    public ATObject meta_clone() throws InterpreterException;

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
     * a new instance <tt>i</tt> and then invoking the <i>constructor</i> on that new instance.
     * In AmbientTalk, class allocation is replaced by object <i>cloning</i>. The
     * benefit is that an instantiated object its variables are already initialized
     * to useful values, being those of the object from which it is instantiated.
     * The <tt>init</tt> method plays the role of "constructor" in AmbientTalk.
     *
     * @param initargs a table denoting the actual arguments to be passed to the <tt>init</tt> method
     * @return the new instance
     */
    public ATObject meta_newInstance(ATTable initargs) throws InterpreterException;

    /* ---------------------------------
      * -- Structural Access Protocol  --
      * --------------------------------- */

    /**
     * Adds a field slot to an object at runtime.
     * Triggers the <tt>fieldAdded</tt> event on this object's beholders (mirror observers) if
     * the field is added successfully.
     *
     * @param field a mirror on the field to add, consisting of a selector (a symbol) and a value (an object)
     * @return nil
     * @throws XDuplicateSlot if the field name already exists
     *
     * TODO: return value = nil?
     */
    public ATNil meta_addField(ATField field) throws InterpreterException;

    /**
     * Adds a method slot to an object at runtime.
     * Triggers the <tt>methodAdded</tt> event on this object's beholders (mirror observers) if
     * the method is added successfully.
     *
     * @param method a mirror on the method to add. A method consists of a selector, arguments and a body.
     * @return nil
     * @throws XDuplicateSlot if the method's selector already exists
     *
     * TODO: return value = nil? argument = a method mirror or a closure mirror?
     */
    public ATNil meta_addMethod(ATMethod method) throws InterpreterException;

    /**
     * Queries an object for one of its field slots.
     * Triggers the <tt>fieldAccessed</tt> event on this object's beholders (mirror observers).
     *
     * @param selector a symbol representing the name of the slot.
     * @return a mirror on this object's field slot.
     * @throws XUndefinedField if the field cannot be found.
     */
    public ATField meta_grabField(ATSymbol selector) throws InterpreterException;

    /**
     * Queries an object for one of its method slots.
     * Triggers the <tt>methodAccessed</tt> event on this object's beholders (mirror observers).
     *
     * @param selector a symbol representing the name of the slot.
     * @return a mirror on this object's method slot.
     * @throws XSelectorNotFound if the method cannot be found.
     */
    public ATMethod meta_grabMethod(ATSymbol selector) throws InterpreterException;

    /**
     * Queries an object for a list of all of its field slots.
     * TODO(beholders) should this method trigger beholders?
     *   if so, using a single 'fieldsQueried' event or by
     *   invoking 'fieldAccessed' for each field in the list returned?
     *
     * @return a table of ATField mirrors.
     */
    public ATTable meta_listFields() throws InterpreterException;

    /**
     * Queries an object for a list of all of its method slots.
     * TODO(beholders) should this method trigger beholders?
     *   if so, using a single 'methodsQueried' event or by
     *   invoking 'methodAccessed' for each field in the list returned?
     *
     * @return a table of ATMethod mirrors.
     */
    public ATTable meta_listMethods() throws InterpreterException;

    /* ---------------------
      * -- Mirror Fields   --
      * --------------------- */

    /**
     * Objects have a dynamic parent delegation chain, but there are two kinds
     * of delegation links: IS-A and SHARES-A links. This method returns whether
     * this object extends its parent object via an IS-A link.
     * 
     * Note that accessing the dynamic parent itself is not a meta-level operation,
     * the dynamic parent can simply be accessed from the base level by performing
     * 'obj.super'.
     */
    public ATBoolean meta_isExtensionOfParent() throws InterpreterException;

    /**
     * Objects also have a lexical parent which is the scope in which their
     * definitions are nested. This scope is visible using receiverless messages.
     * This getter method allows accessing the parent alongside the lexical nesting
     * chain to be accessed as a field of the object's mirror.
     * @throws InterpreterException 
     */
    public ATObject meta_getLexicalParent() throws InterpreterException;

    /* ------------------------------------------
      * -- Abstract Grammar evaluation protocol --
      * ------------------------------------------ */

    /**
     * Evaluates a particular parsetree with respect to a particular context.
     * @param ctx - context (object) to lookup bindings in.
     * @throws InterpreterException
     */
    public ATObject meta_eval(ATContext ctx) throws InterpreterException;

    /**
     * Quotes a parsetree, in other words allows the parsetree to return itself
     * instead of evaluating. This mode is triggered when a quotation parsetree
     * element was encountered and is switched off again when an unquotation
     * parsetree element is found. The context is passed on behalf of these possible
     * future evaluations.
     * @param ctx - context passed on to be used in subsequent evaluations.
     * @throws InterpreterException upon conversion errors or upon illegal unquoted expressions
     */
    public ATObject meta_quote(ATContext ctx) throws InterpreterException;

    /**
     * Prints out the object in a human-readable way.
     * @return a native textual representation of the object.
     */
    public NATText meta_print() throws InterpreterException;

    /* ----------------------------------
     * -- Object Relational Comparison --
     * ---------------------------------- */
    
    /**
     * Detects whether both objects have a common origin, in other words whether 
     * they are related through a combination of the cloning and extension operators.
     * @throws InterpreterException 
     */
    public ATBoolean meta_isRelatedTo(ATObject object) throws InterpreterException;
    
    /**
     * Detects whether this object an the passed parameter are the result of cloning 
     * from a common ancestor (possibly either one of the objects itself).  
     * @param original - the object of which this object is supposedly a sibling
     * @return NATBoolean._TRUE_ if both objects are related.
     */
    public ATBoolean meta_isCloneOf(ATObject original) throws InterpreterException;

    /* ---------------------------------
     * -- Stripe Testing and Querying --
     * --------------------------------- */
    
    /**
     * Tests whether the receiver object is striped with a particular stripe.
     * If the test fails, i.e. the object is not directly striped with (a substripe of)
     * the given stripe, the test is applied recursively to the dynamic parent of the
     * object, until nil is reached.
     */
    public ATBoolean meta_isStripedWith(ATStripe stripe) throws InterpreterException;
    
    /**
     * Returns the stripes of this object. Note that only the stripes that were
     * attached directly to this object are returned, not all of the parent's stripes as well.
     */
    public ATTable meta_getStripes() throws InterpreterException;
    
    /* -------------------------------
      * - Base Level Object interface -
      * -------------------------------
      */

    /**
     * Access the dynamic parent of this object, that is, the object to which
     * locally failed operations such as 'invoke' and 'select' are delegated to.
     */
    public ATObject base_getSuper() throws InterpreterException;
    
    /**
     * The pointer equality == operator.
     * OBJ(o1) == OBJ(o2) => BLN(o1.equals(o2))
     */
    public ATBoolean base__opeql__opeql_(ATObject other) throws InterpreterException;

    /**
     * The object instantiation method.
     * obj.new(@args) => (reflect: obj).newInstance(@args)
     */
    public ATObject base_new(ATObject[] initargs) throws InterpreterException;

    /**
     * The object initialisation method.
     * By default, it does nothing.
     * obj.init(@args) => nil
     */
    public ATObject base_init(ATObject[] initargs) throws InterpreterException;
}
