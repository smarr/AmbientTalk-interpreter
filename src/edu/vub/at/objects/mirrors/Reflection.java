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

import edu.vub.at.exceptions.NATException;
import edu.vub.at.exceptions.XArityMismatch;
import edu.vub.at.exceptions.XIllegalArgument;
import edu.vub.at.objects.ATField;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATMirror;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.natives.NATBoolean;
import edu.vub.at.objects.natives.NATFraction;
import edu.vub.at.objects.natives.NATNil;
import edu.vub.at.objects.natives.NATNumber;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.grammar.AGSymbol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author tvc
 *
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
 */
public final class Reflection {
	
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
	 *   car -> ^
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
		return AGSymbol.alloc(javaToAmbientTalkSelector(jSelector));
	}
	
	/**
	 * Transforms a Java selector prefixed with base_ into an AmbientTalk selector without the prefix.
	 */
	public static final ATSymbol downBaseLevelSelector(String jSelector) throws NATException {
		if (jSelector.startsWith(JavaInterfaceAdaptor._BASE_PREFIX_)) {
			return downSelector(stripPrefix(jSelector, JavaInterfaceAdaptor._BASE_PREFIX_));
		} else 
			
		// In Exceptional Cases magic_ may be used to invoke special primitives, so this method needs to support them
		// since they are in all other regards equal to base_ methods
		if (jSelector.startsWith(JavaInterfaceAdaptor._MAGET_PREFIX_)) {
			return downFieldName(stripPrefix(jSelector, JavaInterfaceAdaptor._MAGET_PREFIX_));
		} else if (jSelector.startsWith(JavaInterfaceAdaptor._MASET_PREFIX_)) {
			return downFieldName(stripPrefix(jSelector, JavaInterfaceAdaptor._MASET_PREFIX_));
		} else if (jSelector.startsWith(JavaInterfaceAdaptor._MAGIC_PREFIX_)) {
			return downSelector(stripPrefix(jSelector, JavaInterfaceAdaptor._MAGIC_PREFIX_));
		} else
		
		
		if (jSelector.startsWith(JavaInterfaceAdaptor._MGET_PREFIX_)) {
			return downFieldName(stripPrefix(jSelector, JavaInterfaceAdaptor._MGET_PREFIX_));
		} else if (jSelector.startsWith(JavaInterfaceAdaptor._MSET_PREFIX_)) {
			return downFieldName(stripPrefix(jSelector, JavaInterfaceAdaptor._MSET_PREFIX_));
		} else if (jSelector.startsWith(JavaInterfaceAdaptor._META_PREFIX_)) {
			return downSelector(stripPrefix(jSelector, JavaInterfaceAdaptor._META_PREFIX_));
		} else {
			throw new XIllegalArgument("Illegal base level selector to down: " + jSelector);
		}
	}
	
	/**
	 * Transforms a Java selector prefixed with meta_ into an AmbientTalk selector without the prefix.
	 */
	public static final ATSymbol downMetaLevelSelector(String jSelector) throws NATException {
		if (jSelector.startsWith(JavaInterfaceAdaptor._META_PREFIX_)) {
			return downSelector(stripPrefix(jSelector, JavaInterfaceAdaptor._META_PREFIX_));
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
	public static final ATSymbol downBaseFieldAccessSelector(String jSelector) throws NATException {
		if (jSelector.startsWith(JavaInterfaceAdaptor._BGET_PREFIX_)) {
			return downFieldName(stripPrefix(jSelector, JavaInterfaceAdaptor._BGET_PREFIX_));
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
	public static final ATSymbol downBaseFieldMutationSelector(String jSelector) throws NATException {
		if (jSelector.startsWith(JavaInterfaceAdaptor._BSET_PREFIX_)) {
			return downFieldName(stripPrefix(jSelector, JavaInterfaceAdaptor._BSET_PREFIX_));
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
	public static final ATSymbol downMetaFieldAccessSelector(String jSelector) throws NATException {
		if (jSelector.startsWith(JavaInterfaceAdaptor._MGET_PREFIX_)) {
			return downFieldName(stripPrefix(jSelector, JavaInterfaceAdaptor._MGET_PREFIX_));
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
	public static final ATSymbol downMetaFieldMutationSelector(String jSelector) throws NATException {
		if (jSelector.startsWith(JavaInterfaceAdaptor._MSET_PREFIX_)) {
			return downFieldName(stripPrefix(jSelector, JavaInterfaceAdaptor._MSET_PREFIX_));
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
	public static final ATSymbol downFieldName(String jName) throws NATException {
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
	 *   ^ -> car
	 *   ! -> not
	 *   > -> gtx
	 *   < -> ltx
	 *   = -> eql
	 *   ~ -> til
	 *   ? -> que
	 *   % -> rem
	 */
	public static final String upSelector(ATSymbol atSelector) throws NATException {
		// : -> _
		String nam = atSelector.base_getText().asNativeText().javaValue;
		nam = nam.replaceAll(":", "_");
		
		// operator symbol -> _op{code}_
		Matcher m = symbol.matcher(nam);
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
	public static final String upBaseLevelSelector(ATSymbol atSelector) throws NATException {
		return JavaInterfaceAdaptor._BASE_PREFIX_ + upSelector(atSelector);
	}

	/**
	 * Transforms an AmbientTalk selector into a Java-level selector prefixed with meta_.
	 */
	public static final String upMetaLevelSelector(ATSymbol atSelector) throws NATException {
		return JavaInterfaceAdaptor._META_PREFIX_ + upSelector(atSelector);
	}
	
	/**
	 * Transforms an AmbientTalk selector into a Java-level selector prefixed with magic_.
	 */
	public static final String upMagicLevelSelector(ATSymbol atSelector) throws NATException {
		return JavaInterfaceAdaptor._MAGIC_PREFIX_ + upSelector(atSelector);
	}
	
	/**
	 * A field name "field" passed from the AmbientTalk to the Java level undergoes the following transformations:
	 * 
	 *  - the same transformations applicable to upSelector
	 *    @see Reflection#upSelector(ATSymbol)
	 *  - the first letter is transformed into upper case such that it can be accessed using respectively
	 *    "getField" | "setField" methods at the Java level.  
	 */
	public static final String upFieldName(ATSymbol atName) throws NATException {
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
	public static final String upBaseFieldAccessSelector(ATSymbol atName) throws NATException {
		return JavaInterfaceAdaptor._BGET_PREFIX_ + upFieldName(atName);
	}

	/**
	 * Transforms an AmbientTalk selector into an equivalent Java selector uppercased and prefixed with "base_set".
	 * 
	 * Example:
	 *  upBaseFieldMutationSelector(ATSymbol('receiver')) => "base_setReceiver"
	 */
	public static final String upBaseFieldMutationSelector(ATSymbol atName) throws NATException {
		return JavaInterfaceAdaptor._BSET_PREFIX_ + upFieldName(atName);
	}

	/**
	 * Transforms an AmbientTalk selector into an equivalent Java selector uppercased and prefixed with "meta_get".
	 * 
	 * Example:
	 *  upMetaFieldAccessSelector(ATSymbol('receiver')) => "meta_getReceiver"
	 */
	public static final String upMetaFieldAccessSelector(ATSymbol atName) throws NATException {
		return JavaInterfaceAdaptor._MGET_PREFIX_ + upFieldName(atName);
	}
	
	/**
	 * Transforms an AmbientTalk selector into an equivalent Java selector uppercased and prefixed with "meta_set".
	 * 
	 * Example:
	 *  upMetaFieldMutationSelector(ATSymbol('receiver')) => "meta_setReceiver"
	 */
	public static final String upMetaFieldMutationSelector(ATSymbol atName) throws NATException {
		return JavaInterfaceAdaptor._MSET_PREFIX_ + upFieldName(atName);
	}
	
	/**
	 * Transforms an AmbientTalk selector into an equivalent Java selector uppercased and prefixed with "meta_get".
	 * 
	 * Example:
	 *  upMetaFieldAccessSelector(ATSymbol('receiver')) => "meta_getReceiver"
	 */
	public static final String upMagicFieldAccessSelector(ATSymbol atName) throws NATException {
		return JavaInterfaceAdaptor._MAGET_PREFIX_ + upFieldName(atName);
	}
	
	/**
	 * Transforms an AmbientTalk selector into an equivalent Java selector uppercased and prefixed with "meta_set".
	 * 
	 * Example:
	 *  upMetaFieldMutationSelector(ATSymbol('receiver')) => "meta_setReceiver"
	 */
	public static final String upMagicFieldMutationSelector(ATSymbol atName) throws NATException {
		return JavaInterfaceAdaptor._MASET_PREFIX_ + upFieldName(atName);
	}
	/**
	 * Constructs an AmbientTalk ATField from a pair of getter/setter methods of
	 * a Java object. Given an object obj and a String sel, it is checked whether
	 *  a) obj has a method named 'get' + Sel, if so, a field can be created
	 *  b) obj has a method named 'set' + Sel, if so, the field is mutable, otherwise it is read-only
	 *
	 * The getter method cannot take any arguments, the setter method must have a unary arity.
	 *
	 * @param jObject the Java object in which the getter/setter methods should be found
	 * @param jSelector a selector which, when prefixed with 'get' or 'set' should yield a method in jObject
	 * @return a reified field, which may either be read-only or mutable depending on available methods
	 * 
	 * Example:
	 *  eval "(reflect: msg).getField('selector')" where msg is a NATMessage
	 *  => downField(aNATMessage, "selector")
	 *  => NATMessage must have a zero-arg method getSelector and optionally setSelector
	 */
	public static final ATField downField(ATObject jObject, String jSelector) throws NATException {
		return JavaField.createPrimitiveField(jObject, downSelector(jSelector));
	}
	
	/**
	 * Constructs an AmbientTalk ATClosure from a Java method.
	 * Given an object obj and a String sel, it is checked whether obj has a method
	 * named 'base_' + sel. If so, the corresponding Java method is wrapped in a JavaClosure.
	 * If not, the downing fails.
	 *
	 * @param jObject the Java object in which the method should be found
	 * @param jSelector a selector which, when prefixed with 'base_' should yield a method in jObject
	 * @return a reified closure wrapping the Java method
	 * 
	 * Example:
	 *  eval "(reflect: tbl).getMethod('at')" where tbl is a NATTable
	 *  => downMethod(aNATTable, "at")
	 *  => NATTable must have a method named base_at
	 */
	public static final ATMethod downMethod(ATObject jObject, String jSelector) throws NATException {
		return JavaInterfaceAdaptor.getMethod(jObject.getClass(), jObject, jSelector);
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
	 */
	public static final ATObject downInvocation(ATObject atRcvr, String jSelector, Object[] jArgs) throws NATException {
		if (jArgs == null) { jArgs = NATTable.EMPTY.elements_; }
		
		if (jSelector.startsWith(JavaInterfaceAdaptor._BGET_PREFIX_)) {
			// obj.base_getSelector() => obj.meta_select(obj, selector)
			if (jArgs.length != 0) {
				throw new XArityMismatch(downBaseFieldAccessSelector(jSelector).toString(), 0, jArgs.length);
			}
			return atRcvr.meta_select(atRcvr, downBaseFieldAccessSelector(jSelector));
		} else if (jSelector.startsWith(JavaInterfaceAdaptor._BSET_PREFIX_)) {
			// obj.base_setSelector(x) => obj.meta_assignField(selector, x)
			if (jArgs.length != 1) {
				throw new XArityMismatch(downBaseFieldMutationSelector(jSelector).toString(), 1, jArgs.length);
			}
			return atRcvr.meta_assignField(downBaseFieldMutationSelector(jSelector), downObject(jArgs[0]));
		} else if (jSelector.startsWith(JavaInterfaceAdaptor._BASE_PREFIX_)) {
			// obj.base_selector(args) => obj.meta_invoke(obj, selector, args)
			return atRcvr.meta_invoke(atRcvr, downBaseLevelSelector(jSelector), new NATTable(jArgs));
		} else if (jSelector.startsWith(JavaInterfaceAdaptor._META_PREFIX_)) {
			// obj.meta_selector(args) => obj.meta_selector(args)
			return downObject(JavaInterfaceAdaptor.invokeJavaMethod(atRcvr.getClass(), atRcvr, jSelector, jArgs));
		} else {
			throw new XIllegalArgument("invocation downed with inappropriate java selector: " + jSelector);
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
	public static final Object upInvocation(ATObject atOrigRcvr, String jSelector, ATTable atArgs) throws NATException {
		return JavaInterfaceAdaptor.invokeJavaMethod(
				    atOrigRcvr.getClass(),
				    atOrigRcvr,
					jSelector,
					atArgs.asNativeTable().elements_);
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
	public static final boolean upRespondsTo(ATObject jRcvr,String jSelector) throws NATException {
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
	 * @param jSelector the selector of the message to be invoked, converted to a Java selector
	 * @return the return value of the Java getter method invoked via the AmbientTalk selection.
	 * 
	 * Example:
	 *  eval "msg.selector" where msg is a NATMessage
	 *  => upSelection(aNATMessage, "selector")
	 *  => NATMessage must have a zero-argument method named getSelector
	 *  
	 */
	public static final Object upFieldSelection(ATObject atOrigRcvr, String jSelector) throws NATException {
		return JavaInterfaceAdaptor.invokeJavaMethod(
				atOrigRcvr.getClass(),
				atOrigRcvr,
				jSelector,
				NATTable.EMPTY.elements_);		
	}
	
	/**
	 * upMethodSelection takes an explicit AmbientTalk field selection and checks whether 
	 * a Java method exists that matches the selector. If so, this method is wrapped in a 
	 * JavaClosure and returned.
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
	public static final Object upMethodSelection(ATObject atOrigRcvr, String jSelector) throws NATException {
		return JavaInterfaceAdaptor.wrapMethodFor(atOrigRcvr.getClass(), atOrigRcvr, jSelector);
	}
	
	/**
	 * upInstanceCreation takes an explicit AmbientTalk 'new' invocation and turns it into an
	 * implicit Java instance creation by calling a constructor. The initargs are upped as well
	 * and are passed as arguments to the constructor.
	 * 
	 * @param jRcvr the Java object having received the call to new
	 * @param atInitargs the arguments to the constructor
	 * @return a new instance of a Java class
	 * @throws NATException
	 */
	public static final Object upInstanceCreation(ATObject jRcvr, ATTable atInitargs) throws NATException {
		ATObject[] args = atInitargs.asNativeTable().elements_;
		Object[] uppedArgs = new Object[args.length];
		for (int i = 0; i < uppedArgs.length; i++) {
			uppedArgs[i] = upObject(args[i]);
		}
		return JavaInterfaceAdaptor.createClassInstance(jRcvr.getClass(), uppedArgs);
	}

	/**
	 * Convert a Java object into an AmbientTalk object.
	 * This is currently only possible if the Java object is a mirror (i.e. it implements ATObject)
	 * or if it is a 'native type'.
	 * 
	 * @param jObj the Java object representing a mirror or a native type
	 * @return the same object if it implements the ATObject interface
	 */
	public static final ATObject downObject(Object jObj) {
		// Our own "dynamic dispatch"
		// mirror
		if(jObj instanceof ATMirror) {
			return ((ATMirror)jObj).base_getBase();
		// object
		} else if(jObj instanceof ATObject) {
			return (ATObject) jObj;
	    // integer
		} else if (jObj instanceof Integer) {
			return NATNumber.atValue(((Integer) jObj).intValue());
		// double
		} else if (jObj instanceof Double) {
			return NATFraction.atValue(((Double) jObj).doubleValue());
		// float
		} else if (jObj instanceof Float) {
			return NATFraction.atValue(((Float) jObj).floatValue());
		// string
		} else if (jObj instanceof String) {
			return NATText.atValue((String) jObj);
		// char
		} else if (jObj instanceof Character) {
			return NATText.atValue(((Character) jObj).toString());
		// boolean
		} else if (jObj instanceof Boolean) {
			return NATBoolean.atValue(((Boolean) jObj).booleanValue());
		// byte
		} else if (jObj instanceof Byte) {
			return NATNumber.atValue(((Byte) jObj).intValue());
		// short
		} else if (jObj instanceof Short) {
			return NATNumber.atValue(((Short) jObj).intValue());
		// Object[]
		} else if (jObj instanceof Object[]) {
			Object[] jArray = (Object[]) jObj;
			ATObject[] atTable = new ATObject[jArray.length];
			for (int i = 0; i < jArray.length; i++) {
				atTable[i] = downObject(jArray[i]);
			}
			return new NATTable(atTable);
	    // null
		} else if (jObj == null) {
			return NATNil._INSTANCE_;
		} else {
			throw new RuntimeException("Cannot wrap Java objects of type " + jObj.getClass());			
		}
	}

	/**
	 * Convert an AmbientTalk object into its Java equivalent.
	 */
	public static final Object upObject(ATObject atObj) {
		// Our own "dynamic dispatch"
		// mirage
		if(atObj instanceof NATMirage) {
			return ((NATMirage) atObj).mirror_;
	    // integer
		} else if (atObj instanceof NATNumber) {
			return new Integer(((NATNumber) atObj).javaValue);
		// double
		} else if (atObj instanceof NATFraction) {
			return new Double(((NATFraction) atObj).javaValue);
		// string
		} else if (atObj instanceof NATText) {
			return ((NATText) atObj).javaValue;
		// booleans
		} else if (atObj == NATBoolean._TRUE_) {
			return new Boolean(true);
		} else if (atObj == NATBoolean._FALSE_) {
			return new Boolean(false);
	    // nil
		} else if (atObj == NATNil._INSTANCE_) {
			return null;
		// Object[]
		} else if (atObj instanceof NATTable) {
			ATObject[] atArray = ((NATTable) atObj).elements_;
			Object[] jArray = new Object[atArray.length];
			for (int i = 0; i < jArray.length; i++) {
				jArray[i] = upObject(atArray[i]);
			}
			return jArray;
		} else {
			return atObj;	
		}
	}
	
	
	private static final Pattern oprCode = Pattern.compile("_op(\\w\\w\\w)_"); //'_op', 3 chars, '_'
	private static final Pattern symbol = Pattern.compile("\\W"); //any non-word character
	
	private static String stripPrefix(String input, String prefix) {
		// \A matches start of input
		return input.replaceFirst("\\A"+prefix, "");
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
		  case 'c': if (code.equals("car")) { return "^"; } else break;
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
		  case '^': return "car";
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
		Matcher m = oprCode.matcher(jSelector);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
             // find every occurence of _op\w\w\w_ and convert it into a symbol
			m.appendReplacement(sb, oprCode2Symbol(m.group(1)));
		}
		m.appendTail(sb);
		
		// _ -> :
		return sb.toString().replaceAll("_", ":");
	}
	
}
