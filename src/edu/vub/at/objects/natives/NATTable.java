/**
 * AmbientTalk/2 Project
 * NATTable.java created on 26-jul-2006 at 16:48:34
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
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.exceptions.XIndexOutOfBounds;
import edu.vub.at.objects.ATBoolean;
import edu.vub.at.objects.ATClosure;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATMethod;
import edu.vub.at.objects.ATNil;
import edu.vub.at.objects.ATNumber;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATText;
import edu.vub.at.objects.coercion.NativeTypeTags;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.DirectNativeMethod;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.natives.grammar.AGExpression;
import edu.vub.at.parser.SourceLocation;
import edu.vub.util.TempFieldGenerator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;

/**
 * The native implementation of an AmbientTalk table.
 * A table is implemented by a java array.
 * 
 * An important distinction between AT tables and Java arrays is that
 * ATTable objects are indexed from [1..size] rather than [0..size[
 * 
 * @author tvcutsem
 */
public class NATTable extends AGExpression implements ATTable {

	/**
	 * The empty table. This instance is shared between all actors on this VM,
	 * which is safe since it is an immutable object.
	 */
	public final static NATTable EMPTY = new NATTable(new ATObject[] {}) {
		// since the empty table is shared, its source location is meaningless
	    public SourceLocation impl_getLocation() { return null; }
	    public void impl_setLocation(SourceLocation loc) {}
	    
	    static final long serialVersionUID = 4036096689737987809L;
	};
	
	public final ATObject[] elements_;
	
	/**
	 * Table factory method. Used to enforce that only one empty table
	 * in the system exists.
	 */
	public static final NATTable atValue(ATObject[] array) {
		if (array.length == 0)
			return NATTable.EMPTY;
		else
			return new NATTable(array);
	}
	
	/**
	 * @return a table of the given size, filled with nil
	 */
	public static final NATTable ofSize(int size) {
		ATObject[] array = new ATObject[size];
		for (int i = 0; i < size; i++) {
			array[i] = Evaluator.getNil();
		}
		return atValue(array);
	}
	
	/*
	 * Auxiliary methods to create tables more easily.
	 */
	
	public static final NATTable of(ATObject one) {
		return new NATTable(new ATObject[] { one });
	}
	
	public static final NATTable of(ATObject one, ATObject two) {
		return new NATTable(new ATObject[] { one, two });
	}
	
	public static final NATTable of(ATObject one, ATObject two, ATObject three) {
		return new NATTable(new ATObject[] { one, two, three });
	}
	
	private NATTable(ATObject[] elements) {
		// assert elements.length > 0
		elements_ = elements;
	}
	
    public ATTable asTable() { return this; }
	
    public boolean isTable() { return true; }
    
	public NATTable asNativeTable() { return this; }
	
	/**
	 * To evaluate a table, evaluate all of its constituent expressions, taking
	 * special care to take into account spliced expressions.
	 * 
	 * NATTAB(exps).eval(ctx) = NATTAB(map eval(ctx) over exps)
	 * 
	 * @return a table of evaluated arguments
	 */
	public ATObject meta_eval(ATContext ctx) throws InterpreterException {
		if (this == EMPTY) return EMPTY;
		
		LinkedList result = new LinkedList();
		int siz = elements_.length;
		for (int i = 0; i < elements_.length; i++) {
			if (elements_[i].isSplice()) {
				ATObject[] tbl = elements_[i].asSplice().base_expression().meta_eval(ctx).asNativeTable().elements_;
				for (int j = 0; j < tbl.length; j++) {
					result.add(tbl[j]);
				}
				siz += (tbl.length - 1); // -1 because we replace one element by a table of elements
			} else {
				result.add(elements_[i].meta_eval(ctx));
			}
		}
		return atValue((ATObject[]) result.toArray(new ATObject[siz]));
	}
	
	/**
	 * To quote a table, quote all elements of the table.
	 * Special care needs to be taken in order to properly deal with unquote-spliced elements.
	 * When one of the direct elements of the table is an unquote-splice element, the resulting
	 * unquotation must result in a table whose elements are directly added to this table's elements.
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		if (this == EMPTY) return EMPTY;
		
		LinkedList result = new LinkedList();
		int siz = elements_.length;
		for (int i = 0; i < elements_.length; i++) {
			if (elements_[i].isUnquoteSplice()) {
				ATObject[] tbl = elements_[i].asUnquoteSplice().base_expression().meta_eval(ctx).asNativeTable().elements_;
				for (int j = 0; j < tbl.length; j++) {
					result.add(tbl[j]);
				}
				siz += (tbl.length - 1); // -1 because we replace one element by a table of elements
			} else {
				result.add(elements_[i].meta_quote(ctx));
			}
		}
		return atValue((ATObject[]) result.toArray(new ATObject[siz]));
	}
	
	public NATText meta_print() throws InterpreterException {
		return Evaluator.printElements(this, "[", ", ","]");
	}
	
	public NATText impl_asCode(TempFieldGenerator objectMap) throws InterpreterException {
		if(objectMap.contains(this)) {
			return objectMap.getName(this);
		}
		
		StringBuffer out = new StringBuffer("[");
		for(int i = 0 ; i < elements_.length ; i++) {
			if(i > 0) { out.append(", "); }
			out.append(elements_[i].impl_asCode(objectMap).javaValue);
		}
		out.append("]");
		NATText name = objectMap.put(this, NATText.atValue(out.toString()));
		return name;
	}
	
    public ATTable meta_typeTags() throws InterpreterException {
    	return NATTable.of(NativeTypeTags._TABLE_);
    }
	
	public ATNumber base_length() { return NATNumber.atValue(elements_.length); }

	public ATObject base_at(ATNumber index) throws InterpreterException {
		return elements_[extractIndex(index)];
	}

	public ATObject base_atPut(ATNumber index, ATObject value) throws InterpreterException {
		elements_[extractIndex(index)] = value;
		return value;
	}
	
	public ATBoolean base_isEmpty() {
		return NATBoolean.atValue(elements_.length == 0);
	}
	
	public ATNil base_each_(ATClosure clo) throws InterpreterException {
		for (int i = 0; i < elements_.length; i++) {
			clo.base_apply(atValue(new ATObject[] { elements_[i] }));
		}
		return Evaluator.getNil();
	}

	public ATTable base_map_(ATClosure clo) throws InterpreterException {
		if (this == EMPTY) return EMPTY;
		
		ATObject[] result = new ATObject[elements_.length];
		for (int i = 0; i < elements_.length; i++) {
			result[i] = clo.base_apply(atValue(new ATObject[] { elements_[i] }));
		}
		return atValue(result);
	}
	
	public ATObject base_inject_into_(ATObject init, ATClosure clo) throws InterpreterException {
		ATObject total = init;
		for (int i = 0; i < elements_.length; i++) {
			total = clo.base_apply(atValue(new ATObject[] { total, elements_[i] }));
		}
		return total;
	}
	
	public ATTable base_filter_(ATClosure clo) throws InterpreterException {
		Vector matchingElements = new Vector(elements_.length);
		for (int i = 0; i < elements_.length; i++) {
			if (clo.base_apply(atValue(new ATObject[] { elements_[i] })).asNativeBoolean().javaValue) {
				matchingElements.add(elements_[i]);
			}
		}
		return atValue((ATObject[]) matchingElements.toArray(new ATObject[matchingElements.size()]));
	}
	
	public ATObject base_find_(ATClosure clo) throws InterpreterException {
		for (int i = 0; i < elements_.length; i++) {
			if (clo.base_apply(atValue(new ATObject[] { elements_[i] })).asNativeBoolean().javaValue) {
				return NATNumber.atValue(i+1);
			}
		}
		return Evaluator.getNil();
	}
	
	public ATBoolean base_contains(ATObject obj) throws InterpreterException {
		for (int i = 0; i < elements_.length; i++) {
			if (obj.equals(elements_[i])) {
				return NATBoolean._TRUE_;
			}
		}
		return NATBoolean._FALSE_;
	}
	
	public ATText base_implode() throws InterpreterException {
		StringBuffer buff = new StringBuffer("");
		for (int i = 0; i < elements_.length; i++) {
			buff.append(elements_[i].asNativeText().javaValue);
		}
		return NATText.atValue(buff.toString());
	}

	public ATText base_join(ATText sep) throws InterpreterException {
		String separator = sep.asNativeText().javaValue;
		StringBuffer buff = new StringBuffer("");
		for (int i = 0; i < elements_.length-1; i++) {
			buff.append(elements_[i].asNativeText().javaValue);
			buff.append(separator);
		}
		if (elements_.length > 0)
			buff.append(elements_[elements_.length-1].asNativeText().javaValue);
		return NATText.atValue(buff.toString());
	}
	
	/**
	 * tab.select(start, stop) == els = [ ] ; start.to: stop do: { |i| els << tab[i] } ; els
	 */
	public ATTable base_select(ATNumber first, ATNumber last) throws InterpreterException {
		final LinkedList selection = new LinkedList();
		first.base_to_do_(last, new NativeClosure(this) {
			public ATObject base_apply(ATTable args) throws InterpreterException {
			    selection.add(base_at(args.base_at(NATNumber.ONE).asNumber()));
			    return Evaluator.getNil();
			}
		});
		return NATTable.atValue((ATObject[]) selection.toArray(new ATObject[selection.size()]));
	}
	
	public ATTable base__oppls_(ATTable other) throws InterpreterException {
		return NATTable.atValue(collate(elements_, other.asNativeTable().elements_));
	}
	
	protected int extractIndex(ATNumber atIndex) throws InterpreterException {
		int javaIndex = atIndex.asNativeNumber().javaValue - 1;
		if ((javaIndex < 0) || (javaIndex >= elements_.length))
			throw new XIndexOutOfBounds(javaIndex + 1, elements_.length);
		else
			return javaIndex;
	}

	/**
	 * Auxiliary method to collate two Java arrays
	 * @return an array containing first the elements of ary1, then the elements of ary2
	 */
	public static final ATObject[] collate(ATObject[] ary1, ATObject[] ary2) {
	    int siz1 = ary1.length;
	    int siz2 = ary2.length;
	    ATObject[] union = new ATObject[siz1 + siz2];
	    System.arraycopy(ary1, 0, union, 0, siz1);
	    System.arraycopy(ary2, 0, union, siz1, siz2);
	    return union;
	}
	
	public ATObject meta_clone() throws InterpreterException {
		ATObject[] clonedArray = new ATObject[elements_.length];
		System.arraycopy(elements_, 0, clonedArray, 0, elements_.length);
		return NATTable.atValue(clonedArray);
	}
	
	public ATObject meta_resolve() throws InterpreterException {
		if (elements_.length == 0)
			return NATTable.EMPTY;
		else
			return this;
	}
	
	/**
	 * FV([exp1, exp2, ...]) = FV(exp1) U FV(exp2) U ...
	 */
	public Set impl_freeVariables() throws InterpreterException {
        HashSet freeVars = new HashSet();
        for (int i = 0; i < elements_.length; i++) {
			freeVars.addAll(elements_[i].asExpression().impl_freeVariables());
		}
        return freeVars;
	}
	
	public Set impl_quotedFreeVariables() throws InterpreterException {
        HashSet freeVars = new HashSet();
        for (int i = 0; i < elements_.length; i++) {
			freeVars.addAll(elements_[i].asExpression().impl_quotedFreeVariables());
		}
        return freeVars;
	}
	
	/**
	 * This hashmap stores all native methods of native AmbientTalk tables.
	 * It is populated when this class is loaded, and shared between all
	 * AmbientTalk actors on this VM. This is safe, since {@link DirectNativeMethod}
	 * instances are all immutable.
	 */
	private static final HashMap<String, ATMethod> _meths = new HashMap<String, ATMethod>();
	
	// initialize NATTable methods
	static {
		_meths.put("length", new DirectNativeMethod("length") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATTable self = ctx.base_receiver().asNativeTable();
				checkArity(args, 0);
				return self.base_length();
			}
		});
		_meths.put("at", new DirectNativeMethod("at") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATTable self = ctx.base_receiver().asNativeTable();
				checkArity(args, 1);
				ATNumber index = get(args, 1).asNumber();
				return self.base_at(index);
			}
		});
		_meths.put("atPut", new DirectNativeMethod("atPut") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATTable self = ctx.base_receiver().asNativeTable();
				checkArity(args, 2);
				ATNumber index = get(args, 1).asNumber();
				ATObject value = get(args, 2);
				return self.base_atPut(index, value);
			}
		});
		_meths.put("isEmpty", new DirectNativeMethod("isEmpty") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATTable self = ctx.base_receiver().asNativeTable();
				checkArity(args, 0);
				return self.base_isEmpty();
			}
		});
		_meths.put("each:", new DirectNativeMethod("each:") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATTable self = ctx.base_receiver().asNativeTable();
				checkArity(args, 1);
				ATClosure clo = get(args, 1).asClosure();
				return self.base_each_(clo);
			}
		});
		_meths.put("map:", new DirectNativeMethod("map:") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATTable self = ctx.base_receiver().asNativeTable();
				checkArity(args, 1);
				ATClosure clo = get(args, 1).asClosure();
				return self.base_map_(clo);
			}
		});
		_meths.put("inject:into:", new DirectNativeMethod("inject:into:") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATTable self = ctx.base_receiver().asNativeTable();
				checkArity(args, 2);
				ATObject init = get(args, 1);
				ATClosure clo = get(args, 2).asClosure();
				return self.base_inject_into_(init, clo);
			}
		});
		_meths.put("filter:", new DirectNativeMethod("filter:") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATTable self = ctx.base_receiver().asNativeTable();
				checkArity(args, 1);
				ATClosure clo = get(args, 1).asClosure();
				return self.base_filter_(clo);
			}
		});
		_meths.put("find:", new DirectNativeMethod("find:") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATTable self = ctx.base_receiver().asNativeTable();
				checkArity(args, 1);
				ATClosure clo = get(args, 1).asClosure();
				return self.base_find_(clo);
			}
		});
		_meths.put("contains", new DirectNativeMethod("contains") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATTable self = ctx.base_receiver().asNativeTable();
				checkArity(args, 1);
				ATObject obj = get(args, 1);
				return self.base_contains(obj);
			}
		});
		_meths.put("implode", new DirectNativeMethod("implode") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATTable self = ctx.base_receiver().asNativeTable();
				checkArity(args, 0);
				return self.base_implode();
			}
		});
		_meths.put("join", new DirectNativeMethod("join") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATTable self = ctx.base_receiver().asNativeTable();
				checkArity(args, 1);
				ATText sep = get(args, 1).asNativeText();
				return self.base_join(sep);
			}
		});
		_meths.put("select", new DirectNativeMethod("select") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATTable self = ctx.base_receiver().asNativeTable();
				checkArity(args, 2);
				ATNumber first = get(args, 1).asNumber();
				ATNumber last = get(args, 2).asNumber();
				return self.base_select(first, last);
			}
		});
		_meths.put("+", new DirectNativeMethod("+") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATTable self = ctx.base_receiver().asNativeTable();
				checkArity(args, 1);
				ATTable other = get(args, 1).asTable();
				return self.base__oppls_(other);
			}
		});
		_meths.put("==", new DirectNativeMethod("==") {
			public ATObject base_apply(ATTable args, ATContext ctx) throws InterpreterException {
				NATTable self = ctx.base_receiver().asNativeTable();
				checkArity(args, 1);
				ATObject other = get(args, 1);
				return self.base__opeql__opeql_(other);
			}
		});
	}
	
	/**
	 * Overrides the default AmbientTalk native object behavior of extracting native
	 * methods based on the 'base_' naming convention. Instead, native AT tables use
	 * an explicit hashmap of native methods. This is much faster than the default
	 * behavior, which requires reflection.
	 */
	protected boolean hasLocalMethod(ATSymbol atSelector) throws InterpreterException {
		if  (_meths.containsKey(atSelector.base_text().asNativeText().javaValue)) {
			return true;
		} else {
			return super.hasLocalMethod(atSelector);
		}
	}
	
	/**
	 * @see NATTable#hasLocalMethod(ATSymbol)
	 */
	protected ATMethod getLocalMethod(ATSymbol selector) throws InterpreterException {
		ATMethod val = _meths.get(selector.base_text().asNativeText().javaValue);
		if (val == null) {
			return super.getLocalMethod(selector);
		}
		return val;
	}
	
}
