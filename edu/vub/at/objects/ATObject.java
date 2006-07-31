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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XTypeMismatch;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATText;

/**
 * @author tvc
 *
 * ATObject represents the public interface of an AmbientTalk/2 object.
 * Any value representing an ambienttalk object should implement this interface.
 * 
 * Some meta methods defined in this interface will give rise to events in the receiver object's
 * beholders (i.e. the observers of the object's mirror).
 */
public interface ATObject extends ATConversions {

	/* ------------------------------
	 * -- Message Sending Protocol --
	 * ------------------------------ */
	
	/**
	 * Sends a newly created message asynchronously to this object.
	 * The message may be scheduled in the current actor's outbox if the current ATObject is a remote reference.
	 * @param msg, the asynchronous message (by default created using the actor's ATMessageFactory)
	 * 
	 * Triggers the following events on this object's beholders (mirror observers):
	 *  - <tt>sentMessage</tt> when the message was sent by the actor.
	 */
	public ATNil meta_send(ATMessage message) throws NATException;
	
	/**
	 * Invoke a method corresponding to the selector (and located along the dynamic 
	 * parent chain) with the given arguments
	 * @param receiver TODO
	 * @param selector the name of the method to be invoked
	 * @param arguments the table of arguments passed to the method
	 * @param receiver the value for self when invoking the method
	 * @return return value of the method
	 * 
	 * Triggers the following events on this object's beholders (mirror observers):
	 *  - <tt>methodFound</tt> when a method has been found but not yet applied
	 *  - <tt>methodInvoked</tt> when the received method has been applied
	 */
	public ATObject meta_invoke(ATObject receiver, ATSymbol selector, ATTable arguments) throws NATException;
	
	/**
	 * Invoke a function corresponding to the selector (and located along the lexical 
	 * parent chain) with the given arguments
	 * @param selector the name of the method to be invoked
	 * @param arguments the table of arguments passed to the method
	 * @return return value of the method
	 * 
	 * Triggers the following events on this object's beholders (mirror observers):
	 *  - <tt>functionFound</tt> when a function has been found but not yet applied
	 *  - <tt>functionApplied</tt> when the received function has been applied
	 */	
	public ATObject meta_call(ATSymbol selector, ATTable arguments) throws NATException;
	
	/**
	 * Query an object for a given field or method which is visible to the outside world.
	 * In other words only methods in the dynamic parent chain are considered.
	 * @param selector the name of the method
	 * @return a 'boolean' denoting whether the object responds to a message o.selector
	 */
	public ATBoolean meta_respondsTo(ATSymbol selector) throws NATException;
	
	/* ------------------------------------------
	 * -- Slot accessing and mutating protocol --
	 * ------------------------------------------ */
	
	/**
	 * Select a slot (field | method) from an object's dynamic parent chain whose name
	 * corresponds to the given selector.
	 * @param receiver the dynamic receiver to which method closures should bind self.
	 * @param selector the name of the field or method sought for. 
	 * @return the contents of the slot
	 * 
	 * Triggers the <tt>slotSelected</tt> event on this object's beholders (mirror observers).
	 */
	public ATObject meta_select(ATObject receiver, ATSymbol selector) throws NATException;
	
	/**
	 * Select a slot (field | method) from an object's lexical parent chain whose name
	 * corresponds to the given selector.
	 * @param selector the name of the field or method sought for. 
	 * @return the contents of the slot
	 * 
	 * Triggers the <tt>slotSelected</tt> event on this object's beholders (mirror observers).
	 */
	public ATObject meta_lookup(ATSymbol selector) throws NATException;
	
	/**
	 * Defines a new field in an object.
	 * Triggers the <tt>fieldAdded</tt> event on this object's beholders (mirror observers) if
	 * the field is added successfully.
	 * 
	 * @param a field name (a symbol) and a value (an object)
	 * @return nil
	 * @throws ATException if the field name already exists
	 */
	public ATNil meta_defineField(ATSymbol name, ATObject value) throws NATException;
	
	/**
	 * Sets the value of the given field name to the value given.
	 * Triggers the <tt>fieldAssigned</tt> event on this object's beholders (mirror observers).
	 * 
	 * @param name a symbol representing the name of the field to assign.
	 * @param value the value to assign to the specified slot.
	 * @return nil
	 * @throws ATException if the field to set cannot be found.
	 */
	public ATNil meta_assignField(ATSymbol name, ATObject value) throws NATException;
	
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
	 * Initializing the clone is the responsibility of the method named <new>.
	 */
	public ATObject meta_clone() throws NATException;
	
	/**
	 * Create an is-a extension of the receiver object.
	 * The base-level code <obj.extend { code }> is represented at the meta-level by <mirror(obj).meta_extend(code)>
	 * 
	 * Triggers the <tt>objectExtended</tt> event on this object's beholders (mirror observers).
	 * 
	 * @return a fresh object whose dynamic parent points to <this> with 'is-a' semantics.
	 */
	public ATObject meta_extend(ATClosure code) throws NATException;
	
	/**
	 * Create a shares-a extension of the receiver object.
	 * The base-level code <code>obj.share { code }</code> is represented at the meta-level by
	 * <code>mirror(obj).meta_share(code)</code>
	 * 
	 * Triggers the <tt>objectShared</tt> event on this object's beholders (mirror observers).
	 * 
	 * @return a fresh object whose dynamic parent points to <this> with 'shares-a' semantics.
	 */
	public ATObject meta_share(ATClosure code) throws NATException;
	
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
	 * @throws ATException if the field name already exists
	 * 
	 * TODO: return value = nil?
	 */
	public ATNil meta_addField(ATField field) throws NATException;
	
	/**
	 * Adds a method slot to an object at runtime.
	 * Triggers the <tt>methodAdded</tt> event on this object's beholders (mirror observers) if
	 * the method is added successfully.
	 * 
	 * @param method a mirror on the method to add. A method consists of a selector, arguments and a body.
	 * @return nil
	 * @throws ATException if the method's selector already exists
	 * 
	 * TODO: return value = nil? argument = a method mirror or a closure mirror?
	 */
	public ATNil meta_addMethod(ATMethod method) throws NATException;
	
	/**
	 * Queries an object for one of its field slots.
	 * Triggers the <tt>fieldAccessed</tt> event on this object's beholders (mirror observers).
	 * 
	 * @param selector a symbol representing the name of the slot.
	 * @return a mirror on this object's field slot.
	 * @throws ATException if the field cannot be found.
	 */
	public ATField meta_getField(ATSymbol selector) throws NATException;
	
	/**
	 * Queries an object for one of its method slots.
	 * Triggers the <tt>methodAccessed</tt> event on this object's beholders (mirror observers).
	 * 
	 * @param selector a symbol representing the name of the slot.
	 * @return a mirror on this object's method slot.
	 * @throws ATException if the method cannot be found.
	 */
	public ATMethod meta_getMethod(ATSymbol selector) throws NATException;
	
	/**
	 * Queries an object for a list of all of its field slots.
	 * TODO: should this method trigger beholders?
	 *   if so, using a single 'fieldsQueried' event or by
	 *   invoking 'fieldAccessed' for each field in the list returned?
	 * 
	 * @return a table of ATField mirrors.
	 */
	public ATTable meta_listFields() throws NATException;
	
	/**
	 * Queries an object for a list of all of its method slots.
	 * TODO: should this method trigger beholders?
	 *   if so, using a single 'methodsQueried' event or by
	 *   invoking 'methodAccessed' for each field in the list returned?
	 * 
	 * @return a table of ATMethod mirrors.
	 */
	public ATTable meta_listMethods() throws NATException;
	
	/* ---------------------
	 * -- Mirror Fields   --
	 * --------------------- */
	
	/**
	 * Objects have a classical dynamic parent chain created using extension 
	 * primitives. This getter method allows accessing the parent alongside
	 * this dynamic parent chain to be accessed as a field of the object's 
	 * mirror.
	 */
	public ATObject getDynamicParent();
	
	/**
	 * Objects also have a lexical parent which is the scope in which their 
	 * definitions are nested. This scope is visible using receiverless messages.
	 * This getter method allows accessing the parent alongside the lexical nesting
	 * chain to be accessed as a field of the object's mirror.
	 */
	public ATObject getLexicalParent();
	
	/* ------------------------------------------
	 * -- Abstract Grammar evaluation protocol --
	 * ------------------------------------------ */
	
	/**
	 * Evaluates a particular parsetree with respect to a particular context.
	 * @param ctx - context (object) to lookup bindings in.
	 * @throws NATException 
	 */
	public ATObject meta_eval(ATContext ctx) throws NATException;
	
	/**
	 * Quotes a parsetree, in other words allows the parsetree to return itself
	 * instead of evaluating. This mode is triggered when a quotation parsetree
	 * element was encountered and is switched off again when an unquotation 
	 * parsetree element is found. The context is passed on behalf of these possible
	 * future evaluations.
	 * @param ctx - context passed on to be used in subsequent evaluations.
	 * @throws NATException upon conversion errors or upon illegal unquoted expressions
	 */
	public ATObject meta_quote(ATContext ctx) throws NATException;
	
	/**
	 * Prints out the object in a human-readable way.
	 * @return a native textual representation of the object.
	 * @throws XTypeMismatch if an element does not represent itself using a native text value
	 */
	public NATText meta_print() throws XTypeMismatch;
	
}
