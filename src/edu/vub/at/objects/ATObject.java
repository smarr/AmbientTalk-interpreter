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
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XUnassignableField;
import edu.vub.at.exceptions.XUndefinedField;
import edu.vub.at.objects.coercion.ATConversions;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATText;

/**
 * ATObject represents the public interface of an AmbientTalk/2 object.
 * Any value representing an ambienttalk object should implement this interface.
 * 
 * Some meta methods defined in this interface will give rise to events in the receiver object's
 * beholders (i.e. the observers of the object's mirror).
 * 
 * The principal implementors of this interface are:
 *  - NATNil which provides a default implementation for all native, non-object values
 *    The default implementation tries to make a Java native object look like an AmbientTalk object.
 *  - NATCallframe provides the implementation for a special kind of objects, namely call frames or
 *    'activation records'. These are objects without true methods.
 *  - NATObject provides the most important implementation, namely that of base-level AmbientTalk objects.
 *  - NATSuperObject acts as a proxy to a NATObject. Therefore, it implements the ATObject interface
 *    by properly forwarding all methods to a wrapped object.
 * 
 * @author tvc
 */
public interface ATObject extends ATConversions {

    /* ------------------------------
      * -- Message Sending Protocol --
      * ------------------------------ */

    /**
     * Sends a newly created message asynchronously by this object.
     * The message may be scheduled in the current actor's outbox if the current ATObject is a remote reference.
     * @param message the asynchronous message (by default created using the actor's base_createMessage method)
     *
     * Triggers the following events on this object's beholders (mirror observers):
     *  - <tt>sentMessage</tt> when the message was sent by the actor.
     */
    public ATObject meta_send(ATAsyncMessage message) throws InterpreterException;
    
    /**
     * Handles a first-class message of which it is the receiver. 
     * @param message the asynchronous message send by some object possibly from another actor
     * @return the return value of invoking the method corresponding to the message
     */
    public ATObject meta_receive(ATAsyncMessage message) throws InterpreterException;
   
    /**
     * Invoke a method corresponding to the selector with the given arguments.
     * The selector is looked up along the dynamic delegation chain.
     *
     * The first argument, 'receiver', denotes the receiver of the method invocation.
     *
     * @param receiver the object to which 'self' is bound during execution of the method
     * @param selector the name of the method to be invoked
     * @param arguments the table of arguments passed to the method
     * @return return value of the method
     *
     * Triggers the following events on this object's beholders (mirror observers):
     *  - <tt>methodFound</tt> when a method has been found but not yet applied
     *  - <tt>methodInvoked</tt> when the received method has been applied
     */
    public ATObject meta_invoke(ATObject receiver, ATSymbol selector, ATTable arguments) throws InterpreterException;

    /**
     * Query an object for a given field or method which is visible to the outside world.
     * Only methods in the dynamic parent chain are considered.
     * @param selector the name of a field or method
     * @return a boolean denoting whether the object responds to <tt>o.selector</tt>
     */
    public ATBoolean meta_respondsTo(ATSymbol selector) throws InterpreterException;

    /**
     * Called when a selection fails because the selector was not
     * found along the dynamic delegation hierarchy.
     *
     * Note the differences with Smalltalk's well-known 'doesNotUnderstand':
     *  - dNU is a meta-level operation in AmbientTalk; it is applied to mirrors.
     *  - dNU relates to attribute selection, not to method invocation. Hence, dNU
     *    in AmbientTalk is more general: it can be used to model 'virtual' fields
     *    by returning a value and it can be used to model 'virtual' methods by
     *    returning a block closure.
     *
     * @param selector the selector that could not be found
     * @throws edu.vub.at.exceptions.XSelectorNotFound the default reaction to a failed selection
     */
    public ATObject meta_doesNotUnderstand(ATSymbol selector) throws InterpreterException;

    /* -----------------------------
     * -- Object Passing protocol --
     * ----------------------------- */

    /**
     * Allows objects to specify how they should be passed when passed as an argument
     * in a message sent to another actor.
     * 
     * @return Objects may choose to return themselves, a clone or a proxy representation
     * @throws InterpreterException - when overridden by the programmer
     */
    public ATObject meta_pass() throws InterpreterException;

    /**
     * When an object is deserialized after it has been passed to another actor, it is
     * given a chance to tell the interpreter which object it represents. Normally,
     * by copy objects just return 'this' because they represent themselves. Far references,
     * however, will try to 'resolve' themselves into a near reference when possible.
     *  
     * @return the object represented by this deserialized object
     * @throws XIllegalOperation when by-reference objects are resolved or when overridden by programmer
     */
    public ATObject meta_resolve() throws InterpreterException;
    
    /* ------------------------------------------
     * -- Slot accessing and mutating protocol --
     * ------------------------------------------ */
    
    /**
     * Select a slot (field | method) from an object whose name corresponds to the given
     * selector. The slot lookup follows the dynamic delegation chain.
     *
     * Like with method invocation, slot selection is parameterized by the 'original receiver'.
     * This original receiver is equal to 'this' the first time it is called. Via delegation,
     * 'this' may instead be a dynamic parent of 'receiver'.
     *
     * When a method is selected from an object, it is wrapped in a closure such that
     * the 'self' is properly preserved.
     *
     * @param receiver the dynamic receiver to which method closures should bind self.
     * @param selector the name of the field or method sought for.
     * @return the contents of the slot
     *
     * Triggers the <tt>slotSelected</tt> event on this object's beholders (mirror observers).
     */
    public ATObject meta_select(ATObject receiver, ATSymbol selector) throws InterpreterException;

    /**
     * Select a slot (field | method) from an object whose name corresponds to the given
     * selector. The slot lookup follows the lexical nesting chain.
     *
     * When a method is found in an object, it is wrapped in a closure such that the 'self'
     * is properly preserved.
     *
     * @param selector the name of the field or method to look up.
     * @return the contents of the slot
     * @throws XUndefinedField if the field cannot be found
     *
     * Triggers the <tt>slotSelected</tt> event on this object's beholders (mirror observers).
     */
    public ATObject meta_lookup(ATSymbol selector) throws InterpreterException;

    /**
     * Defines a new field in an object.
     *
     * @param name the name of the new field
     * @param value the value of the new field
     * @return nil
     * @throws edu.vub.at.exceptions.XDuplicateSlot if the field name already exists
     *
     * Triggers the <tt>fieldAdded</tt> event on this object's beholders (mirror observers) if
     * the field is added successfully.
     */
    public ATNil meta_defineField(ATSymbol name, ATObject value) throws InterpreterException;

    /**
     * Sets the value of the variable to the given value.
     * Triggers the <tt>fieldAssigned</tt> event on this object's beholders (mirror observers).
     *
     * Normally, a variable assignment can only be triggered from within the lexical scope of an object.
     *
     * @param name a symbol representing the name of the variable to assign.
     * @param value the value to assign to the specified slot.
     * @return nil
     * @throws XUnassignableField if the field to set cannot be found.
     */
    public ATNil meta_assignVariable(ATSymbol name, ATObject value) throws InterpreterException;

    /**
     * Sets the value of a field to the given value.
     * Triggers the <tt>fieldAssigned</tt> event on this object's beholders (mirror observers).
     *
     * Field assignment may result in the assignment of a parent's field.
     * @param receiver the object which received (and possibly delegated) the request
     * @param name a symbol representing the field to assign.
     * @param value the value to assign to the specified slot.
     *
     * @return nil
     * @throws XUnassignableField if the field to set cannot be found.
     */
    public ATNil meta_assignField(ATObject receiver, ATSymbol name, ATObject value) throws InterpreterException;

    /* ------------------------------------
      * -- Extension and cloning protocol --
      * ------------------------------------ */

    /**
     * Clone the receiver object. This cloning closely corresponds to the allocation phase
     * in class-based OO languages. In class-based languages, instance creation is based on the
     * equation <new Class> = <initialize> (<allocate Class>) (i.e. first allocate a new instance, then
     * initialize it). In our object-based model, allocation is replaced by cloning.
     *
     * When an object is asked to clone itself, it will also clone its dynamic parent if it extends
     * this parent in an 'is-a' relationship. This is similar to the observation that, in class-based
     * languages, allocating a new object of a subclass entails allocating space for the state of the superclass.
     *
     * Triggers the <tt>objectCloned</tt> event on this object's beholders (mirror observers).
     *
     * Initializing the clone is the responsibility of the method named <init>.
     */
    public ATObject meta_clone() throws InterpreterException;

    /**
     * Create a new instance of the receiver object. AmbientTalk mimics the initialization
     * protocol of Class-based languages like Smalltalk. In a typical CBL, object initialization
     * equals class allocation + new instance initialization. In AmbientTalk, class allocation
     * is replaced by cloning via the meta_clone operation. Object initialization itself differs
     * from cloning in that it additionally initializes the clone. For standard AmbientTalk objects,
     * this happens by invoking a method named 'init' on the newly created instance.
     *
     * @param initargs arguments to the 'init' constructor method
     * @return the new instance
     */
    public ATObject meta_newInstance(ATTable initargs) throws InterpreterException;

    /**
     * Create an is-a extension of the receiver object.
     * The base-level code <obj.extend { code }> is represented at the meta-level by <mirror(obj).meta_extend(code)>
     *
     * Triggers the <tt>objectExtended</tt> event on this object's beholders (mirror observers).
     *
     * @return a fresh object whose dynamic parent points to <this> with 'is-a' semantics.
     */
    public ATObject meta_extend(ATClosure code) throws InterpreterException;

    /**
     * Create a shares-a extension of the receiver object.
     * The base-level code <code>obj.share { code }</code> is represented at the meta-level by
     * <code>mirror(obj).meta_share(code)</code>
     *
     * Triggers the <tt>objectShared</tt> event on this object's beholders (mirror observers).
     *
     * @return a fresh object whose dynamic parent points to <this> with 'shares-a' semantics.
     */
    public ATObject meta_share(ATClosure code) throws InterpreterException;

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
     * Objects have a classical dynamic parent chain created using extension
     * primitives. This getter method allows accessing the parent alongside
     * this dynamic parent chain to be accessed as a field of the object's
     * mirror.
     * @throws InterpreterException 
     */
    public ATObject meta_getDynamicParent() throws InterpreterException;

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
    
    /* -------------------------------
      * - Base Level Object interface -
      * -------------------------------
      */

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
