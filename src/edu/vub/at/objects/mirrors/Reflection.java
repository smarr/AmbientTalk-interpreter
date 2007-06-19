/**
 * AmbientTalk/2 Project
 * Reflection.java created on 10-aug-2006 at 16:19:17
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
package edu.vub.at.objects.mirrors;

import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XIllegalArgument;
import edu.vub.at.exceptions.XSelectorNotFound;
import edu.vub.at.exceptions.XUndefinedSlot;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATException;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.grammar.AGSymbol;
import edu.vub.at.objects.symbiosis.Symbiosis;
import edu.vub.util.Matcher;
import edu.vub.util.Pattern;

import java.lang.reflect.Method;
import java.util.Vector;

/**
 * Reflection is an auxiliary class meant to serve as a repository for methods
 * related to 'up' and 'down' Java values properly into and out of the AmbientTalk base level.
 * 
 * Keep the following mental picture in mind:
 * 
 *                ^        Java = implementation-level          |
 *  to deify      |        Java AT objects = meta-level         | to reify
 *  (= to up)     | ------------------------------------------  | (= to down)
 *  (= to absorb) |         AmbientTalk = base-level            v (= to reflect)
 * 
 * Although deification and reification are more accurate terms, we will use 'up' and 'down'
 * because they are the clearest terminology, and clarity matters.
 * 
 * In this class, the following conventions hold:
 *  - methods start with either 'up' or 'down', denoting whether they deify or reify something
 *  - arguments start with either 'j' or 'at', denoting whether they represent Java or AmbientTalk values
 *  With 'java values' is meant 'java objects representing mirrors'
 * 
 * @author tvc
 */
public final class Reflection {
	
	public static final String _BASE_PREFIX_ = "base_";
	public static final String _BGET_PREFIX_ = "base_get";
	public static final String _BSET_PREFIX_ = "base_set";
	
	public static final String _META_PREFIX_ = "meta_";
	public static final String _MGET_PREFIX_ = "meta_get";
	public static final String _MSET_PREFIX_ = "meta_set";
	
	/**
	 * A selector passed from the Java to the AmbientTalk level undergoes the following transformations:
	 * 
	 * - any pattern of the form _op{code}_ is transformed to a symbol corresponding to the operator code
	 *  Operator codes are:
	 *   pls -> +
	 *   mns -> -
	 *   tms -> *
	 *   div -> /
	 *   bsl -> \
	 *   and -> &
	 *   not -> !
	 *   gtx -> >
	 *   ltx -> <
	 *   eql -> =
	 *   til -> ~
	 *   que -> ?
	 *   rem -> %
	 * - any underscores (_) are replaced by colons (:)
	 */
	public static final ATSymbol downSelector(String jSelector) {
		return AGSymbol.jAlloc(javaToAmbientTalkSelector(jSelector));
	}
	
	/**
	 * Transforms a Java selector prefixed with base_ into an AmbientTalk selector without the prefix.
	 */
	public static final ATSymbol downBaseLevelSelector(String jSelector) throws InterpreterException {
		if (jSelector.startsWith(Reflection._BASE_PREFIX_)) {
			return downSelector(stripPrefix(jSelector, Reflection._BASE_PREFIX_));
		} else if (jSelector.startsWith(Reflection._MGET_PREFIX_)) {
			return downFieldName(stripPrefix(jSelector, Reflection._MGET_PREFIX_));
		} else if (jSelector.startsWith(Reflection._MSET_PREFIX_)) {
			return downFieldName(stripPrefix(jSelector, Reflection._MSET_PREFIX_));
		} else if (jSelector.startsWith(Reflection._META_PREFIX_)) {
			return downSelector(stripPrefix(jSelector, Reflection._META_PREFIX_));
		} else {
			throw new XIllegalArgument("Illegal base level selector to down: " + jSelector);
		}
	}
	
	/**
	 * Transforms a Java selector prefixed with meta_ into an AmbientTalk selector without the prefix.
	 */
	public static final ATSymbol downMetaLevelSelector(String jSelector) throws InterpreterException {
		if (jSelector.startsWith(Reflection._META_PREFIX_)) {
			return downSelector(stripPrefix(jSelector, Reflection._META_PREFIX_));
		} else {
			throw new XIllegalArgument("Illegal meta level selector to down: " + jSelector);
		}
	}
	
	/**
	 * Transforms a Java selector prefixed with base_get into an equivalent AmbientTalk selector.
	 * 
	 * Example:
	 *  downBaseFieldAccessSelector("base_getReceiver") => ATSymbol("receiver")
	 */
	public static final ATSymbol downBaseFieldAccessSelector(String jSelector) throws InterpreterException {
		if (jSelector.startsWith(Reflection._BGET_PREFIX_)) {
			return downFieldName(stripPrefix(jSelector, Reflection._BGET_PREFIX_));
		} else {
			throw new XIllegalArgument("Illegal base level accessor to down: " + jSelector);
		}
	}

	/**
	 * Transforms a Java selector prefixed with base_set into an equivalent AmbientTalk selector.
	 * 
	 * Example:
	 *  downBaseFieldMutationSelector("base_setReceiver") => ATSymbol("receiver")
	 */
	public static final ATSymbol downBaseFieldMutationSelector(String jSelector) throws InterpreterException {
		if (jSelector.startsWith(Reflection._BSET_PREFIX_)) {
			return downFieldName(stripPrefix(jSelector, Reflection._BSET_PREFIX_));
		} else {
			throw new XIllegalArgument("Illegal base level mutator to down: " + jSelector);
		}
	}

	/**
	 * Transforms a Java selector prefixed with meta_get into an equivalent AmbientTalk selector
	 * 
	 * Example:
	 *  downMetaFieldAccessSelector("meta_getReceiver") => ATSymbol("receiver")
	 */
	public static final ATSymbol downMetaFieldAccessSelector(String jSelector) throws InterpreterException {
		if (jSelector.startsWith(Reflection._MGET_PREFIX_)) {
			return downFieldName(stripPrefix(jSelector, Reflection._MGET_PREFIX_));
		} else {
			throw new XIllegalArgument("Illegal meta level accessor to down: " + jSelector);
		}
	}
	
	/**
	 * Transforms a Java selector prefixed with meta_set into an equivalent AmbientTalk selector.
	 * 
	 * Example:
	 *  downMetaFieldMutationSelector("meta_setReceiver") => ATSymbol("receiver")
	 */
	public static final ATSymbol downMetaFieldMutationSelector(String jSelector) throws InterpreterException {
		if (jSelector.startsWith(Reflection._MSET_PREFIX_)) {
			return downFieldName(stripPrefix(jSelector, Reflection._MSET_PREFIX_));
		} else {
			throw new XIllegalArgument("Illegal meta level mutator to down: " + jSelector);
		}
	}
	
	/**
	 * A field name "Field" passed from the Java to the AmbientTalk level undergoes the following transformations:
	 * 
	 *  - the same transformations applicable to downSelector
	 *    @see Reflection#downSelector(String)
	 *  - the first letter is transformed into lower case (as it was uppercased for Java conventions)
	 */
	public static final ATSymbol downFieldName(String jName) throws InterpreterException {
		char[] charArray = jName.toCharArray();
		charArray[0] = Character.toLowerCase(charArray[0]);
		return downSelector(new String(charArray));
	}
	
	/**
	 * A selector passed from the AmbientTalk to the Java level undergoes the following transformations:
	 * 
	 * - any colons (:) are replaced by underscores (_)
	 * - any operator symbol is replaced by _op{code}_ where code is generated as follows:
	 *  Operator codes are:
	 *   + -> pls
	 *   - -> mns
	 *   * -> tms
	 *   / -> div
	 *   \ -> bsl
	 *   & -> and
	 *   ! -> not
	 *   > -> gtx
	 *   < -> ltx
	 *   = -> eql
	 *   ~ -> til
	 *   ? -> que
	 *   % -> rem
	 */
	public static final String upSelector(ATSymbol atSelector) throws InterpreterException {
		// : -> _
		String nam = atSelector.base_getText().asNativeText().javaValue;
        // Backport from JDK 1.4 to 1.3
		// nam = nam.replaceAll(":", "_");
        nam = Pattern.compile(":").matcher(new StringBuffer(nam)).replaceAll("_");
        
		// operator symbol -> _op{code}_
		Matcher m = symbol.matcher(new StringBuffer(nam));
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			// find every occurence of a non-word character and convert it into a symbol
			String oprCode = symbol2oprCode(m.group(0));
			// only add the _op prefix and _ postfix if the code has been found...
			m.appendReplacement(sb, (oprCode.length() == 3) ? "_op" + oprCode  + "_" : oprCode);
		}
		m.appendTail(sb);
		return sb.toString();
	}
	
	/**
	 * Transforms an AmbientTalk selector into a Java-level selector prefixed with base_.
	 */
	public static final String upBaseLevelSelector(ATSymbol atSelector) throws InterpreterException {
		return Reflection._BASE_PREFIX_ + upSelector(atSelector);
	}

	/**
	 * Transforms an AmbientTalk selector into a Java-level selector prefixed with meta_.
	 */
	public static final String upMetaLevelSelector(ATSymbol atSelector) throws InterpreterException {
		return Reflection._META_PREFIX_ + upSelector(atSelector);
	}
	
	/**
	 * A field name "field" passed from the AmbientTalk to the Java level undergoes the following transformations:
	 * 
	 *  - the same transformations applicable to upSelector
	 *    @see Reflection#upSelector(ATSymbol)
	 *  - the first letter is transformed into upper case such that it can be accessed using respectively
	 *    "getField" | "setField" methods at the Java level.  
	 */
	public static final String upFieldName(ATSymbol atName) throws InterpreterException {
		char[] charArray = upSelector(atName).toCharArray();
		charArray[0] = Character.toUpperCase(charArray[0]);
		return new String(charArray);
	}
	
	/**
	 * Transforms an AmbientTalk selector into an equivalent Java selector uppercased and prefixed with "base_get".
	 * 
	 * Example:
	 *  upBaseFieldAccessSelector(ATSymbol('receiver')) => "base_getReceiver"
	 */
	public static final String upBaseFieldAccessSelector(ATSymbol atName) throws InterpreterException {
		return Reflection._BGET_PREFIX_ + upFieldName(atName);
	}

	/**
	 * Transforms an AmbientTalk selector into an equivalent Java selector uppercased and prefixed with "base_set".
	 * 
	 * Example:
	 *  upBaseFieldMutationSelector(ATSymbol('receiver')) => "base_setReceiver"
	 */
	public static final String upBaseFieldMutationSelector(ATSymbol atName) throws InterpreterException {
		return Reflection._BSET_PREFIX_ + upFieldName(atName);
	}

	/**
	 * Transforms an AmbientTalk selector into an equivalent Java selector uppercased and prefixed with "meta_get".
	 * 
	 * Example:
	 *  upMetaFieldAccessSelector(ATSymbol('receiver')) => "meta_getReceiver"
	 */
	public static final String upMetaFieldAccessSelector(ATSymbol atName) throws InterpreterException {
		return Reflection._MGET_PREFIX_ + upFieldName(atName);
	}
	
	/**
	 * Transforms an AmbientTalk selector into an equivalent Java selector uppercased and prefixed with "meta_set".
	 * 
	 * Example:
	 *  upMetaFieldMutationSelector(ATSymbol('receiver')) => "meta_setReceiver"
	 */
	public static final String upMetaFieldMutationSelector(ATSymbol atName) throws InterpreterException {
		return Reflection._MSET_PREFIX_ + upFieldName(atName);
	}
	
	/**
	 * Constructs an AmbientTalk ATField from a pair of accessor/mutator methods of
	 * a Java object. Given an object obj and a String sel, it is checked whether
	 *  a) obj has a method named getPrefix + Sel, if so, a field can be created
	 *  b) obj has a method named setPrefix + Sel, if so, the field is mutable, otherwise it is read-only
	 *
	 * The accessor method cannot take any arguments, the mutator method must have a unary arity.
	 *
	 * @param natObject the native AT object in whose class the accessor/mutator methods should be found
	 * @param atSelector the AmbientTalk name of the field
	 * @return a reified field, which may either be read-only or mutable depending on available methods
	 * 
	 * Example:
	 *  eval "(reflect: msg).getField('selector')" where msg is a NATMessage
	 *  => downField(aNATMessage, "selector", "base_get", "base_set")
	 *  => NATMessage must have a zero-arg method base_getSelector and optionally base_setSelector
	 */
	public static final ATField downField(ATObject natObject, ATSymbol atSelector,
			                              String getPrefix, String setPrefix) throws InterpreterException {
		String fieldName = upFieldName(atSelector);
		try {
			Method accessorMethod = JavaInterfaceAdaptor.getNativeATMethod(natObject.getClass(), natObject, getPrefix + fieldName, atSelector);
			Method mutatorMethod = null;
			try {
				mutatorMethod = JavaInterfaceAdaptor.getNativeATMethod(natObject.getClass(), natObject, setPrefix + fieldName, atSelector);
			} catch (XSelectorNotFound e) {
				// no setter, return a read-only field
			}
			return new NativeField(natObject, atSelector, accessorMethod, mutatorMethod);
		} catch (XSelectorNotFound e) {
			// selector not found exceptions have to be translated to field not found exceptions
			throw new XUndefinedSlot("field access", atSelector.toString());
		}
	}
	
	public static final ATField downBaseLevelField(ATObject natObject, ATSymbol atSelector) throws InterpreterException {
		return downField(natObject, atSelector, Reflection._BGET_PREFIX_, Reflection._BSET_PREFIX_);
	}
	
	public static final ATField downMetaLevelField(ATObject natObject, ATSymbol atSelector) throws InterpreterException {
		return downField(natObject, atSelector, Reflection._MGET_PREFIX_, Reflection._MSET_PREFIX_);
	}
	
	/**
	 * Constructs an AmbientTalk ATMethod from a Java method.
	 * Given an object obj and a String sel, it is checked whether obj has a method
	 * named sel. If so, the corresponding Java method is wrapped in a NativeMethod.
	 * If not, the downing fails.
	 *
	 * @param natObject the native AmbientTalk object in whose class the method should be found
	 * @param jSelector a selector which should yield a method in natObject
	 * @param origName the original AmbientTalk name of the method
	 * @return a reified method wrapping the Java method
	 * 
	 * Example:
	 *  eval "(reflect: tbl).getMethod('at')" where tbl is a NATTable
	 *  => downMethod(aNATTable, "base_at")
	 *  => NATTable must have a method named base_at
	 *  
	 * Callers should use the more specialised 'downBaseLevelMethod' and 'downMetaLevelMethod'
	 * methods to specify the prefix of the method to be found
	 */
	public static final ATMethod downMethod(ATObject natObject, String jSelector, ATSymbol origName) throws InterpreterException {
		return new NativeMethod(JavaInterfaceAdaptor.getNativeATMethod(natObject.getClass(), natObject, jSelector, origName), origName);
	}
	
	public static final ATMethod downBaseLevelMethod(ATObject natObject, ATSymbol atSelector) throws InterpreterException {
		return downMethod(natObject, upBaseLevelSelector(atSelector), atSelector);
	}
	
	public static final ATMethod downMetaLevelMethod(ATObject natObject, ATSymbol atSelector) throws InterpreterException {
		return downMethod(natObject, upMetaLevelSelector(atSelector), atSelector);
	}

	/**
	 * downInvocation takes an implicit Java invocation and turns it into an explicit
	 * AmbientTalk invocation process. This happens when Java code sends normal
	 * Java messages to AmbientTalk objects (wrapped by a mirage).
	 * 
	 * @param atRcvr the AmbientTalk object having received the Java method invocation
	 * @param jSelector the Java selector, to be converted to an AmbientTalk selector
	 * @param jArgs the arguments to the Java method invocation (normally all args are ATObjects)
	 * jArgs may be null, indicating that there are no arguments
	 * @return the return value of the AmbientTalk method invoked via the java invocation.
	 * 
	 * Example:
	 *  in Java: "tbl.base_at(1)" where tbl is an ATTable coercer wrapping aNATObject
	 *  => downInvocation(aNATObject, "base_at", ATObject[] { ATNumber(1) })
	 *  => aNATObject must implement a method named "at"
	 *  
	 * Depending on the prefix of the invoked Java method selector, the following translation should occur:
	 *  - obj.base_selector(args) => obj.meta_invoke(obj, selector, args)
	 *  - obj.base_getSelector() => obj.meta_select(obj, selector)
	 *  - obj.base_setSelector(x) => obj.meta_assignField(selector, x)
	 *  - obj.meta_selector(args) => obj.meta_selector(args)
	 *  - obj.meta_set|getSelector(args) => obj.meta_set|getSelector(args)
	 *  - obj.selector(args) => either obj.selector(args) if selector is understood natively
	 *                          or     obj.meta_invoke(obj, selector, args) otherwise
	 */
	public static final ATObject downInvocation(ATObject atRcvr, Method jMethod, ATObject[] jArgs) throws InterpreterException {
		String jSelector = jMethod.getName();
		if (jArgs == null) { jArgs = NATTable.EMPTY.elements_; }
		
		if (jSelector.startsWith(Reflection._BGET_PREFIX_)) {
			// obj.base_getSelector() => obj.meta_select(obj, selector)
			if (jArgs.length != 0) {
				throw new XArityMismatch(downBaseFieldAccessSelector(jSelector).toString(), 0, jArgs.length);
			}
			return atRcvr.meta_select(atRcvr, downBaseFieldAccessSelector(jSelector));
		} else if (jSelector.startsWith(Reflection._BSET_PREFIX_)) {
			// obj.base_setSelector(x) => obj.meta_assignField(selector, x)
			if (jArgs.length != 1) {
				throw new XArityMismatch(downBaseFieldMutationSelector(jSelector).toString(), 1, jArgs.length);
			}
			return atRcvr.meta_assignField(atRcvr, downBaseFieldMutationSelector(jSelector), jArgs[0]);
		} else if (jSelector.startsWith(Reflection._BASE_PREFIX_)) {
			// obj.base_selector(args) => obj.meta_invoke(obj, selector, args)
			return atRcvr.meta_invoke(atRcvr, downBaseLevelSelector(jSelector), NATTable.atValue(jArgs));
		} else if (jSelector.startsWith(Reflection._META_PREFIX_)) {
			// obj.meta_selector(args) => obj.meta_selector(args)
			return JavaInterfaceAdaptor.invokeNativeATMethod(jMethod, atRcvr, jArgs);
		} else {
			// atRcvr can respond to the given method natively
			if (jMethod.getDeclaringClass().isInstance(atRcvr)) {
				return JavaInterfaceAdaptor.invokeNativeATMethod(jMethod, atRcvr, jArgs);
			} else {
			    // obj.selector(args) => obj.meta_invoke(obj, selector, args)
			    return atRcvr.meta_invoke(atRcvr, downSelector(jSelector), NATTable.atValue(jArgs));
			}
		}
	}

	/**
	 * upInvocation takes an explicit AmbientTalk method invocation and turns it into an
	 * implicitly performed Java invocation.
	 * 
	 * Depending on whether the AmbientTalk invocation happens at the base-level or the meta-level
	 * (i.e. the receiver denotes a base-level object or a mirror), the jSelector parameter will have
	 * a different prefix.
	 * 
	 * @param atOrigRcvr the original AmbientTalk object that received the invocation
	 * @param jSelector the selector of the message to be invoked, converted to a Java selector
	 * @param atArgs the arguments to the AmbientTalk method invocation
	 * @return the return value of the Java method invoked via the java invocation.
	 * 
	 * Example:
	 *  eval "tbl.at(1)" where tbl is a NATTable
	 *  => upInvocation(aNATTable, "base_at", ATObject[] { ATNumber(1) })
	 *  => NATTable must have a method named base_at
	 * 
	 * Example:
	 *  eval "(reflect: tbl).invoke(tbl, "at", [1])" where tbl is a NATTable
	 *  => upInvocation(aNATTable, "meta_invoke", ATObject[] { aNATTable, ATSymbol('at'), ATTable([ATNumber(1)]) })
	 *  => NATTable must have a method named meta_invoke
	 */
	public static final ATObject upInvocation(ATObject atOrigRcvr, String jSelector, ATSymbol atSelector, ATTable atArgs) throws InterpreterException {
		return JavaInterfaceAdaptor.invokeNativeATMethod(
				    atOrigRcvr.getClass(),
				    atOrigRcvr,
					jSelector,
					atSelector, atArgs.asNativeTable().elements_);
	}
	
	/**
	 * upRespondsTo transforms an explicit AmbientTalk respondsTo meta-level request
	 * into an implicit check whether the given jRcvr java object has a method
	 * corresponding to the given selector, prefixed with base_
	 * 
	 * @param jRcvr the Java object being queried for a certain selector
	 * @param jSelector the selector of the message to be invoked, converted to a Java selector
	 * @return a boolean indicating whether the jRcvr implements a method corresponding to base_ + atSelector
	 * 
	 * Example:
	 *  eval "(reflect: [1,2,3]).respondsTo("at")" where the receiver of repondsTo is a NATTable
	 *  => upRespondsTo(aNATTable, "at")
	 *  => NATTable must have a method named base_at
	 */
	public static final boolean upRespondsTo(ATObject jRcvr,String jSelector) throws InterpreterException {
		return JavaInterfaceAdaptor.hasApplicableJavaMethod(
				jRcvr.getClass(),
				jSelector);
	}

//	/**
//	 * downSelection takes an implicit Java selection (in the guise of invoking a getter method)
//	 * and turns it into an explicit AmbientTalk selection process. This happens when Java code sends normal
//	 * Java messages to AmbientTalk objects (wrapped by a mirage).
//	 * 
//	 * @param atRcvr the AmbientTalk object having received the Java selection
//	 * @param jSelector the Java selector, without the 'get' prefix, to be converted to an AmbientTalk selector
//	 * @return the value of the AmbientTalk field selected by the java selection.
//	 * 
//	 * Example:
//	 *  in Java: "msg.getSelector()" where msg is am ATMessage mirage wrapping a NATObject
//	 *  => downSelection(aNATObject, "selector")
//	 *  => aNATObject must implement a field named "selector"
//	 * The get prefix is normally stripped off by a mirage
//	 */
//	public static final ATObject downSelection(ATObject atRcvr, String jSelector) {
//		return null;
//	}
	
	/**
	 * upFieldSelection takes an explicit AmbientTalk field selection and turns it into 
	 * an implicitly performed Java selection by invoking a getter method, if such a getter method
	 * exists. 
	 * 
	 * @param atOrigRcvr the original AmbientTalk object that received the selection
	 * @param jSelector the selector of the message to be invoked, already converted to a Java selector
	 * @return the return value of the Java getter method invoked via the AmbientTalk selection.
	 * 
	 * Example:
	 *  eval "msg.selector" where msg is a NATMessage
	 *  => upSelection(aNATMessage, "selector")
	 *  => NATMessage must have a zero-argument method named getSelector
	 *  
	 */
	public static final ATObject upFieldSelection(ATObject atOrigRcvr, String jSelector, ATSymbol atSelector) throws InterpreterException {
		return JavaInterfaceAdaptor.invokeNativeATMethod(
				atOrigRcvr.getClass(),
				atOrigRcvr,
				jSelector,
				atSelector, NATTable.EMPTY.elements_);		
	}
	
	/**
	 * upFieldAssignment takes an explicit AmbientTalk field assignment and turns it into 
	 * an implicitly performed Java field assignment by invoking a mutator method, if such a method
	 * exists. 
	 * 
	 * @param atOrigRcvr the original AmbientTalk object that received the assignField request
	 * @param jSelector the selector of the message to be invoked, already converted to a Java selector
	 * @return the return value of the Java mutator method invoked via the AmbientTalk assignField request.
	 * 
	 * Example:
	 *  eval "msg.selector := v" where msg is a NATMessage
	 *  => upFieldAssignment(aNATMessage, "selector", v)
	 *  => NATMessage must have a one-argument method named base_setSelector
	 *  
	 */
	public static final ATObject upFieldAssignment(ATObject atOrigRcvr, String jSelector, ATSymbol atSelector, ATObject value) throws InterpreterException {
		return JavaInterfaceAdaptor.invokeNativeATMethod(
				atOrigRcvr.getClass(),
				atOrigRcvr,
				jSelector,
				atSelector, new ATObject[] { value });
	}
	
	/**
	 * upMethodSelection takes an explicit AmbientTalk field selection and checks whether 
	 * a Java method exists that matches the selector. If so, this method is wrapped in a 
	 * NativeClosure and returned.
	 * 
	 * @param atOrigRcvr the original AmbientTalk object that received the selection
	 * @param jSelector the selector of the message to be invoked, converted to a Java selector
	 * @return a closure wrapping the method selected via the AmbientTalk selection.
	 * 
	 * Example:
	 *  eval "[1,2,3].at"
	 *  => upSelection(aNATTable, "at")
	 *  => either NATTable must have a method base_at, which is then wrapped
	 */
	public static final NativeClosure upMethodSelection(ATObject atOrigRcvr, String jSelector, ATSymbol origSelector) throws InterpreterException {
		Method m = JavaInterfaceAdaptor.getNativeATMethod(atOrigRcvr.getClass(), atOrigRcvr, jSelector, origSelector);
		return new NativeClosure(atOrigRcvr, new NativeMethod(m, origSelector));
	}
	
	/**
	 * upInstanceCreation takes an explicit AmbientTalk 'new' invocation and turns it into an
	 * implicit Java instance creation by calling a constructor. The initargs are upped as well
	 * and are passed as arguments to the constructor.
	 * 
	 * @param jRcvr the Java object having received the call to new
	 * @param atInitargs the arguments to the constructor
	 * @return a new instance of a Java class
	 * @throws InterpreterException
	 */
	public static final ATObject upInstanceCreation(ATObject jRcvr, ATTable atInitargs) throws InterpreterException {
		ATObject[] args = atInitargs.asNativeTable().elements_;
		return JavaInterfaceAdaptor.createNativeATObject(jRcvr.getClass(), args);
	}
	
	public static final ATObject upExceptionCreation(InterpreterException jRcvr, ATTable atInitargs) throws InterpreterException {
		ATObject[] args = atInitargs.asNativeTable().elements_;
		return Symbiosis.symbioticInstanceCreation(jRcvr.getClass(), args);
	}

	/**
	 * Pass an AmbientTalk meta-level object into the base-level
	 */
	public static final ATObject downObject(ATObject metaObject) throws InterpreterException {
		return metaObject;
		/*if (metaObject.meta_isTaggedAs(NativeTypeTags._MIRROR_).asNativeBoolean().javaValue) {
			return metaObject.meta_select(metaObject, OBJMirrorRoot._BASE_NAME_);
		} else {
			return metaObject; // most native objects represent both the object at the base and at the meta-level
		}*/
	}
	
	/**
	 * Pass an AmbientTalk base-level object to the meta-level
	 */
	public static final ATObject upObject(ATObject baseObject) {
		if (baseObject instanceof NATMirage) {
			return ((NATMirage) baseObject).getMirror();
		} else {
			return baseObject;
		}
	}
	
	/**
	 * Returns, for a given AmbientTalk object atObj, an array of NativeField objects corresponding
	 * to all non-static methods of that object's Java class, where each method's name is prefixed with 'base_get'
	 */
	public static final ATField[] downBaseLevelFields(ATObject atObj) throws InterpreterException {
		Method[] allBaseGetMethods =
			JavaInterfaceAdaptor.allMethodsPrefixed(atObj.getClass(), Reflection._BGET_PREFIX_, false);
		ATField[] fields = new ATField[allBaseGetMethods.length];
		for (int i = 0; i < allBaseGetMethods.length; i++) {
			Method m = allBaseGetMethods[i];
			fields[i] = downBaseLevelField(atObj, downBaseFieldAccessSelector(m.getName()));
		}
		return fields;
	}
	
	/**
	 * Returns, for a given AmbientTalk object atObj, an array of NativeField objects corresponding
	 * to all non-static methods of that object's Java class, where each method's name is prefixed with 'meta_get'
	 */
	public static final ATField[] downMetaLevelFields(ATObject atObj) throws InterpreterException {
		Method[] allMetaGetMethods =
			JavaInterfaceAdaptor.allMethodsPrefixed(atObj.getClass(), Reflection._MGET_PREFIX_, false);
		ATField[] fields = new ATField[allMetaGetMethods.length];
		for (int i = 0; i < allMetaGetMethods.length; i++) {
			Method m = allMetaGetMethods[i];
			fields[i] = downMetaLevelField(atObj, downMetaFieldAccessSelector(m.getName()));
		}
		return fields;
	}
	
	/**
	 * Returns, for a given AmbientTalk object atObj, an array of NativeMethod objects corresponding
	 * to all non-static methods of that object's class, where each method's name:
	 *  - is prefixed with 'base_'
	 *  - is not prefixed with 'base_get'
	 *  - is not prefixed with 'base_set'
	 */
	public static final ATMethod[] downBaseLevelMethods(ATObject atObj) throws InterpreterException {
		Method[] allBaseMethods =
			JavaInterfaceAdaptor.allMethodsPrefixed(atObj.getClass(), Reflection._BASE_PREFIX_, false);
		Vector allNonFieldBaseMethods = new Vector();
		for (int i = 0; i < allBaseMethods.length; i++) {
			Method m = allBaseMethods[i];
			String nam = m.getName();
			if (!((nam.startsWith(Reflection._BGET_PREFIX_)) ||
			      (nam.startsWith(Reflection._BSET_PREFIX_)))) {
				allNonFieldBaseMethods.add(new NativeMethod(m, downBaseLevelSelector(nam)));
			}
		}
		return (ATMethod[]) allNonFieldBaseMethods.toArray(new ATMethod[allNonFieldBaseMethods.size()]);
	}
	
	/**
	 * Returns, for a given AmbientTalk object natObj, an array of NativeMethod objects corresponding
	 * to all non-static methods of that object's class, where each method's name:
	 *  - is prefixed with 'meta_'
	 *  - is not prefixed with 'meta_get'
	 *  - is not prefixed with 'meta_set'
	 */
	public static final ATMethod[] downMetaLevelMethods(ATObject natObj) throws InterpreterException {
		Method[] allMetaMethods =
			JavaInterfaceAdaptor.allMethodsPrefixed(natObj.getClass(), Reflection._META_PREFIX_, false);
		Vector allNonFieldMetaMethods = new Vector();
		for (int i = 0; i < allMetaMethods.length; i++) {
			Method m = allMetaMethods[i];
			String nam = m.getName();
			if (!((nam.startsWith(Reflection._MGET_PREFIX_)) ||
			      (nam.startsWith(Reflection._MSET_PREFIX_)))) {
				allNonFieldMetaMethods.add(new NativeMethod(m, downMetaLevelSelector(nam)));
			}
		}
		return (ATMethod[]) allNonFieldMetaMethods.toArray(new ATMethod[allNonFieldMetaMethods.size()]);
	}
	
	private static final Pattern oprCode = Pattern.compile("_op(\\w\\w\\w)_"); //'_op', 3 chars, '_'
	private static final Pattern symbol = Pattern.compile("\\W"); //any non-word character
	
	private static String stripPrefix(String input, String prefix) {
		// \A matches start of input
	    // Backport from JDK 1.4 to 1.3
        // return input.replaceFirst("\\A"+prefix, "");
		return Pattern.compile("\\A"+prefix).matcher(new StringBuffer(input)).replaceFirst("");
	}
	
	private static final String oprCode2Symbol(String code) {
		switch (code.charAt(0)) {
		  case 'p': if (code.equals("pls")) { return "+"; } else break;
		  case 'm': if (code.equals("mns")) { return "-"; } else break;
		  case 't': if (code.equals("tms")) { return "*"; } else
			        if (code.equals("til")) { return "~"; } else break;
		  case 'd': if (code.equals("div")) { return "/"; } else break;
		  case 'b': if (code.equals("bsl")) { return "\\"; } else break;
		  case 'a': if (code.equals("and")) { return "&"; } else break;
		  case 'n': if (code.equals("not")) { return "!"; } else break;
		  case 'g': if (code.equals("gtx")) { return ">"; } else break;
		  case 'l': if (code.equals("ltx")) { return "<"; } else break;
		  case 'e': if (code.equals("eql")) { return "="; } else break;
		  case 'q': if (code.equals("que")) { return "?"; } else break;
		  case 'r': if (code.equals("rem")) { return "%"; } else break;
		}
		return "_op" + code + "_"; // no match, return original input
	}
	
	private static final String symbol2oprCode(String symbol) {
		switch (symbol.charAt(0)) {
		  case '+': return "pls";
		  case '-': return "mns";
		  case '*': return "tms";
		  case '/': return "div";
		  case '\\': return "bsl";
		  case '&': return "and";
		  case '!': return "not";
		  case '>': return "gtx";
		  case '<': return "ltx";
		  case '=': return "eql";
		  case '~': return "til";
		  case '?': return "que";
		  case '%': return "rem";
		  default: return symbol; // no match, return original input
		}	
	}
	
	private static final String javaToAmbientTalkSelector(String jSelector) {
		// _op{code}_ -> operator symbol
		Matcher m = oprCode.matcher(new StringBuffer(jSelector));
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
             // find every occurence of _op\w\w\w_ and convert it into a symbol
			m.appendReplacement(sb, oprCode2Symbol(m.group(1)));
		}
		m.appendTail(sb);
		
		// _ -> :
        // Backport from JDK 1.4 to 1.3
		// return sb.toString().replaceAll("_", ":");
        return Pattern.compile("_").matcher(sb).replaceAll(":");
	}
	
}
