/**
 * AmbientTalk/2 Project
 * AGDefType.java created on 18-feb-2007 at 14:09:27
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
package edu.vub.at.objects.natives.grammar;

import edu.vub.at.eval.Evaluator;
import edu.vub.at.exceptions.InterpreterException;
import edu.vub.at.objects.ATContext;
import edu.vub.at.objects.ATObject;
import edu.vub.at.objects.ATTable;
import edu.vub.at.objects.ATTypeTag;
import edu.vub.at.objects.grammar.ATDefType;
import edu.vub.at.objects.grammar.ATDefinition;
import edu.vub.at.objects.grammar.ATSymbol;
import edu.vub.at.objects.mirrors.NativeClosure;
import edu.vub.at.objects.natives.NATTable;
import edu.vub.at.objects.natives.NATText;
import edu.vub.at.objects.natives.NATTypeTag;
import edu.vub.util.TempFieldGenerator;

import java.util.HashSet;
import java.util.Set;

/**
 * The native AST node for the code:
 *
 * deftype a <: b, c;
 *
 * @author tvcutsem
 */
public final class AGDefType extends AGDefinition implements ATDefType {

	private final ATSymbol typeName_;
	private final ATTable parentTypeExpressions_;
	
	public AGDefType(ATSymbol typeName, ATTable parentTypeExpressions) {
		typeName_ = typeName;
		parentTypeExpressions_ = parentTypeExpressions;
	}

	public ATTable base_parentTypeExpressions() {
		return parentTypeExpressions_;
	}

	public ATSymbol base_typeName() {
		return typeName_;
	}
	
	/**
	 * Defines a new type in the current scope. The return value is
	 * the new type.
	 * 
	 * AGDEFTYPE(nam,parentExps).eval(ctx) =
	 *   ctx.scope.addField(nam, TYPETAG(nam, map eval(ctx) over parentExps))
	 */
	public ATObject meta_eval(final ATContext ctx) throws InterpreterException {
		// parentTypeExpressions_.map: { |parent| (reflect: parent).eval(ctx).base.asType() }
		ATTable parentTypes = parentTypeExpressions_.base_map_(new NativeClosure(this) {
			public ATObject base_apply(ATTable args) throws InterpreterException {
				return get(args,1).meta_eval(ctx).asTypeTag();
			}
		});
		
		ATTypeTag type = NATTypeTag.atValue(typeName_, parentTypes);
		ctx.base_lexicalScope().meta_defineField(typeName_, type);
		return type;
	}

	/**
	 * Quoting a type definition results in a new quoted type definition.
	 * 
	 * AGDEFTYPE(nam,parentExps).quote(ctx) = AGDEFTYPE(nam.quote(ctx), parentExps.quote(ctx))
	 */
	public ATObject meta_quote(ATContext ctx) throws InterpreterException {
		return new AGDefType(typeName_.meta_quote(ctx).asSymbol(), parentTypeExpressions_.meta_quote(ctx).asTable());
	}
	
	public NATText meta_print() throws InterpreterException {
		if (parentTypeExpressions_ == NATTable.EMPTY) {
			return NATText.atValue("deftype " + typeName_.meta_print().javaValue);
		} else {
			return NATText.atValue("deftype " + typeName_.meta_print().javaValue + " <: " +
					Evaluator.printElements(parentTypeExpressions_.asNativeTable(), "", ",", "").javaValue);
		}
	}
	
	public NATText impl_asUnquotedCode(TempFieldGenerator objectMap) throws InterpreterException {
		if (parentTypeExpressions_ == NATTable.EMPTY) {
			return NATText.atValue("deftype " + typeName_.impl_asUnquotedCode(objectMap).javaValue);
		} else {
			return NATText.atValue("deftype " + typeName_.impl_asUnquotedCode(objectMap).javaValue + " <: " +
					Evaluator.codeElements(objectMap, parentTypeExpressions_.asNativeTable(), "", ",", "").javaValue);
		}
	}
	
	public ATDefinition asDefinition() { return this; }

	/**
	 * IV(deftype nam <: exps) = { T1 }
	 */
	public Set impl_introducedVariables() throws InterpreterException {
		Set singleton = new HashSet();
		singleton.add(typeName_);
		return singleton;
	}

	/**
	 * FV(deftype nam <: exps) = FV(exps) \ { nam }
	 */
	public Set impl_freeVariables() throws InterpreterException {
		Set fvParentType = parentTypeExpressions_.impl_freeVariables();
		fvParentType.remove(typeName_);
		return fvParentType;
	}
	
	
	public Set impl_quotedFreeVariables() throws InterpreterException {
		Set qfv = parentTypeExpressions_.impl_quotedFreeVariables();
		qfv.addAll(typeName_.impl_quotedFreeVariables());
		return qfv;
	}
	
}
