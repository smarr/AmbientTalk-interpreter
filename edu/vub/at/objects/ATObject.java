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

/**
 * @author tvc
 *
 * ATObject represents the public interface of an AmbientTalk/2 object.
 * Any value representing an ambienttalk object should implement this interface.
 */
public interface ATObject {

	/* ------------------------------
	 * -- Message Sending Protocol --
	 * ------------------------------ */
	
	/**
	 * Invoke a method corresponding to the given message.
	 * @param msg the message denoting the method to invoke
	 * @return return value of the method
	 * 
	 * Triggers the following events on this object's beholders (mirror observers):
	 *  - <tt>beforeMessage</tt> when a message has been received but not yet processed
	 *  - <tt>afterMessage</tt> when the received message has been processed
	 */
	public ATObject meta_invoke(ATMessage msg) throws ATException;
	
	/**
	 * Query an object for a given field or method.
	 * @param msg
	 * @return a 'boolean' denoting whether the object responds to the message
	 */
	public ATBoolean meta_respondsTo(ATMessage msg) throws ATException;
	
	/* ------------------------------------------
	 * -- Slot accessing and mutating protocol --
	 * ------------------------------------------ */
	
	/**
	 * Select a slot (field | method) from an object whose name corresponds to msg.selector
	 * @param msg a message encapsulating the selector to lookup
	 * @return the contents of the slot
	 * 
	 * Triggers the <tt>slotSelected</tt> event on this object's beholders (mirror observers).
	 */
	public ATObject meta_select(ATMessage msg) throws ATException;
	
	/**
	 * Sets the value of the given field name to the value given.
	 * Triggers the <tt>fieldAssigned</tt> event on this object's beholders (mirror observers).
	 * 
	 * @param name a symbol representing the name of the field to assign.
	 * @param value the value to assign to the specified slot.
	 * @return nil
	 * @throws ATException if the field to set cannot be found.
	 */
	public ATNil meta_assignField(ATSymbol name, ATObject value) throws ATException;
	
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
	public ATObject meta_clone() throws ATException;
	
	/**
	 * Create an is-a extension of the receiver object.
	 * The base-level code <obj.extend { code }> is represented at the meta-level by <mirror(obj).meta_extend(code)>
	 * 
	 * Triggers the <tt>objectExtended</tt> event on this object's beholders (mirror observers).
	 * 
	 * @return a fresh object whose dynamic parent points to <this> with 'is-a' semantics.
	 */
	public ATObject meta_extend(ATClosure code) throws ATException;
	
	/**
	 * Create a shares-a extension of the receiver object.
	 * The base-level code <code>obj.share { code }</code> is represented at the meta-level by
	 * <code>mirror(obj).meta_share(code)</code>
	 * 
	 * Triggers the <tt>objectShared</tt> event on this object's beholders (mirror observers).
	 * 
	 * @return a fresh object whose dynamic parent points to <this> with 'shares-a' semantics.
	 */
	public ATObject meta_share(ATClosure code) throws ATException;
	
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
	 * TODO: return value = nil? argument = a field mirror or a pair (symbol, value)?
	 */
	public ATNil meta_addField(ATField field) throws ATException;
	
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
	public ATNil meta_addMethod(ATMethod method) throws ATException;
	
	/**
	 * Queries an object for one of its field slots.
	 * Triggers the <tt>fieldAccessed</tt> event on this object's beholders (mirror observers).
	 * 
	 * @param selector a symbol representing the name of the slot.
	 * @return a mirror on this object's field slot.
	 * @throws ATException if the field cannot be found.
	 */
	public ATField meta_getField(ATSymbol selector) throws ATException;
	
	/**
	 * Queries an object for one of its method slots.
	 * Triggers the <tt>methodAccessed</tt> event on this object's beholders (mirror observers).
	 * 
	 * @param selector a symbol representing the name of the slot.
	 * @return a mirror on this object's method slot.
	 * @throws ATException if the method cannot be found.
	 */
	public ATMethod meta_getMethod(ATSymbol selector) throws ATException;
	
	/**
	 * Queries an object for a list of all of its field slots.
	 * TODO: should this method trigger beholders?
	 *   if so, using a single 'fieldsQueried' event or by
	 *   invoking 'fieldAccessed' for each field in the list returned?
	 * 
	 * @return a table of ATField mirrors.
	 */
	public ATTable meta_listFields() throws ATException;
	
	/**
	 * Queries an object for a list of all of its method slots.
	 * TODO: should this method trigger beholders?
	 *   if so, using a single 'methodsQueried' event or by
	 *   invoking 'methodAccessed' for each field in the list returned?
	 * 
	 * @return a table of ATMethod mirrors.
	 */
	public ATTable meta_listMethods() throws ATException;
	
}
