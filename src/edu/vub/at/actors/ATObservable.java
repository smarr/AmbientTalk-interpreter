package edu.vub.at.actors;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATTable;

public interface ATObservable extends ATObject {

	/**
	 * Installs an observer on this object
	 * @param event a symbol denoting a potentially interesting event
	 * @param observer a closure to be triggered when the event is fired
	 * @return a closure which can be applied to cancel the observer's subscription
	 */
	public abstract ATClosure base_upon_do_(ATSymbol event, ATClosure observer);

	/**
	 * Fires an event with the given arguments. All the provided closures are applied
	 * ASYNCHRONOUSLY. This implies that exceptions thrown by such a closure will not
	 * prevent other observers from getting triggered. Moreover, observers are then
	 * triggered breadth-first as opposed to depth-first as is the case with synchronous
	 * invocations.
	 * 
	 * @param event - the event being fired
	 * @param arguments - a possibly empty table of arguments to the event
	 * @return nil
	 */
	public abstract ATNil base_fire_withArgs_(ATSymbol event, NATTable arguments) throws InterpreterException;

}