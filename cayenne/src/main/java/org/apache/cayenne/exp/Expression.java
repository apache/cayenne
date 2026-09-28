/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.exp;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.util.ConversionUtil;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;

import java.util.Objects;
import org.apache.cayenne.util.XMLSerializable;

/**
 * Superclass of Cayenne expressions: a tree node holding its operands. An operand is either a nested expression or a
 * plain value. A node does not know its parent, so it can be shared between trees.
 */
public abstract sealed class Expression implements XMLSerializable permits AggregateConditionExp, AsteriskExp,
		CaseWhenExp, ConditionExp, CustomOperatorExp, ElseExp, EnclosingObjectExp, FullObjectExp, ListExp, NegateExp,
		PathExp, ScalarExp, SubqueryExp, ThenExp, ValueExp {

	/**
	 * A value that a Transformer might return to indicate that a node has to be
	 * pruned from the expression during the transformation.
	 * 
	 * @since 1.2
	 */
	public final static Object PRUNED_NODE = new Object();

	protected Object[] operands;

	protected Expression(Object... operands) {
		if (operands != null && operands.length > 0) {
			setOperands(operands);
		}
		// else - a bare node
	}

	/**
	 * Returns a map of path aliases for this expression. It returns a non-empty
	 * map only if this is a path expression and the aliases are known at the
	 * expression creation time. Otherwise an empty map is returned.
	 * 
	 * @since 3.0
	 */
	public Map<String, String> getPathAliases() {
		return Collections.emptyMap();
	}

	/**
	 * Returns String label for this expression: the class name without the "Exp" suffix. Used for debugging.
	 */
	public String expName() {
		String name = getClass().getSimpleName();
		return name.endsWith("Exp") ? name.substring(0, name.length() - 3) : name;
	}

	@Override
	public boolean equals(Object object) {
		if (!(object instanceof Expression)) {
			return false;
		}

		Expression e = (Expression) object;

		if (e.getClass() != getClass() || e.getOperandCount() != getOperandCount()) {
			return false;
		}

		// compare operands
		int len = e.getOperandCount();
		for (int i = 0; i < len; i++) {
			if (!Objects.deepEquals(e.getOperand(i), getOperand(i))) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = getClass().getName().hashCode();
		int opCount = getOperandCount();
		for (int i = 0; i < opCount; i++) {
			result = 31 * result + Objects.hashCode(getOperand(i));
		}
		return result;
	}

	/**
	 * Creates and returns a new Expression instance based on this expression,
	 * but with parameters substituted with provided values. This is a
	 * positional style of binding. If a given parameter name is used more than
	 * once, only the first occurrence is treated as "position", subsequent
	 * occurrences are bound with the same value as the first one. If expression
	 * parameters count is different from the array parameter count, an
	 * exception will be thrown.
	 * <p>
	 * positional style would not allow subexpression pruning.
	 * 
	 * @since 4.0
	 */
	public Expression paramsArray(Object... parameters) {
		Expression clone = deepCopy();
		clone.inPlaceParamsArray(parameters);
		return clone;
	}

	/**
	 * @since 4.0
	 */
	void inPlaceParamsArray(Object... parameters) {

		InPlaceParamReplacer replacer = new InPlaceParamReplacer(parameters == null ? new Object[0] : parameters);
		traverse(replacer);
		replacer.onFinish();
	}

	/**
	 * Creates and returns a new Expression instance based on this expression,
	 * but with named parameters substituted with provided values. Any
	 * subexpressions containing parameters not matching the "name" argument
	 * will be pruned.
	 * <p>
	 * Note that if you want matching against nulls to be preserved, you must
	 * place NULL values for the corresponding keys in the map.
	 * 
	 * @since 4.0
	 */
	public Expression params(Map<String, ?> parameters) {
		return transform(new NamedParamTransformer(parameters, true));
	}

	/**
	 * Creates and returns a new Expression instance based on this expression,
	 * but with named parameters substituted with provided values.If any
	 * subexpressions containing parameters not matching the "name" argument are
	 * found, the behavior depends on "pruneMissing" argument. If it is false an
	 * Exception will be thrown, otherwise subexpressions with missing
	 * parameters will be pruned from the resulting expression.
	 * <p>
	 * Note that if you want matching against nulls to be preserved, you must
	 * place NULL values for the corresponding keys in the map.
	 * 
	 * @since 4.0
	 */
	public Expression params(Map<String, ?> parameters, boolean pruneMissing) {
		return transform(new NamedParamTransformer(parameters, pruneMissing));
	}

	/**
	 * Joins this expression with the others as the operands of the given (empty) AND or OR node.
	 */
	private Expression join(AggregateConditionExp join, Expression exp, Expression... expressions) {
		join.setOperand(0, this);
		join.setOperand(1, exp);
		for (int i = 0; i < expressions.length; i++) {
			join.setOperand(2 + i, expressions[i]);
		}
		join.flattenTree();
		return join;
	}

	/**
	 * Chains this expression with another expression using "and".
	 */
	public Expression andExp(Expression exp) {
		return join(new AndExp(), exp);
	}

	/**
	 * Chains this expression with other expressions using "and".
	 * 
	 * @since 4.0
	 */
	public Expression andExp(Expression exp, Expression... expressions) {
		return join(new AndExp(), exp, expressions);
	}

	/**
	 * Chains this expression with another expression using "or".
	 */
	public Expression orExp(Expression exp) {
		return join(new OrExp(), exp);
	}

	/**
	 * Chains this expression with other expressions using "or".
	 * 
	 * @since 4.0
	 */
	public Expression orExp(Expression exp, Expression... expressions) {
		return join(new OrExp(), exp, expressions);
	}

	/**
	 * Returns a logical NOT of current expression.
	 * 
	 * @since 1.0.6
	 */
	public Expression notExp() {
		return new NotExp(this);
	}

	/**
	 * Returns expression that will be dynamically resolved to proper subqueries based on a relationships used
	 * (if no relationships are present in the original expression no subqueries will be used).
	 *
	 * @return exists expression
	 *
	 * @see ExpressionFactory#exists(Expression)
	 * @since 5.0
	 */
	public Expression exists() {
		throw new UnsupportedOperationException("Can't use exists() operator with this expression");
	}

	/**
	 * Returns expression that will be dynamically resolved to proper subqueries based on a relationships used
	 * (if no relationships are present in the original expression no subqueries will be used).
	 *
	 * @return not exists expression
	 *
	 * @see ExpressionFactory#notExists(Expression)
	 * @since 5.0
	 */
	public Expression notExists() {
		throw new UnsupportedOperationException("Can't use not exists() operator with this expression");
	}

	/**
	 * Returns a count of operands of this expression. In real life there are
	 * unary (count == 1), binary (count == 2) and ternary (count == 3)
	 * expressions.
	 */
	public int getOperandCount() {
		return operands == null ? 0 : operands.length;
	}

	/**
	 * Returns a value of operand at <code>index</code>: a nested expression or a plain value. Operand indexing
	 * starts at 0.
	 */
	public Object getOperand(int index) {
		if (operands == null) {
			throw new ArrayIndexOutOfBoundsException(index);
		}

		Object operand = operands[index];

		// an enum parsed from a String is resolved on access, so that an expression can be parsed and printed
		// without the enum class being available, e.g. in the Modeler
		return operand instanceof EnumRef enumRef ? enumRef.resolve() : operand;
	}

	/**
	 * Sets a value of operand at <code>index</code>, growing the operands array if needed. Operand indexing starts
	 * at 0. A {@link Persistent} is replaced with its ObjectId. A nested expression is asked first whether this node
	 * is a valid parent for it, see {@link #isValidParent(Expression)}.
	 */
	public void setOperand(int index, Object value) {
		Object operand = value instanceof Persistent persistent ? persistent.getObjectId() : value;

		if (operand instanceof Expression node && !node.isValidParent(this)) {
			throw new ExpressionException(node.expName() + ": invalid parent - " + expName());
		}

		if (operands == null) {
			operands = new Object[index + 1];
		} else if (index >= operands.length) {
			Object[] grown = new Object[index + 1];
			System.arraycopy(operands, 0, grown, 0, operands.length);
			operands = grown;
		}
		operands[index] = operand;
	}

	/**
	 * Replaces all the operands of this node with the given ones. Called by the constructor and by the parser once
	 * all the operands of a node are known, so a node that post-processes its operands overrides this method.
	 *
	 * @since 5.0
	 */
	public void setOperands(Object... operands) {
		this.operands = null;
		if (operands != null) {
			for (int i = 0; i < operands.length; i++) {
				setOperand(i, operands[i]);
			}
		}
	}

	/**
	 * Whether this node can be an operand of the given node. Called by the parent when the operand is set, to check
	 * what the grammar can't: e.g. a condition can only be an operand of a condition aggregate. By default any parent
	 * is valid.
	 *
	 * @since 5.0
	 */
	protected boolean isValidParent(Expression parent) {
		return true;
	}

	/**
	 * Calculates expression value with object as a context for path
	 * expressions.
	 * 
	 * @since 1.1
	 */
	public Object evaluate(Object o) {
		// wrap in try/catch to provide unified exception processing
		try {
			return evaluateNode(o);
		} catch (Throwable th) {
			String string = this.toString();
			throw new ExpressionException("Error evaluating expression '%s'",
					string, Util.unwindException(th), string);
		}
	}

	/**
	 * Evaluates itself with object, pushing result on the stack.
	 */
	protected abstract Object evaluateNode(Object o) throws Exception;

	/**
	 * Evaluates the operand at the given index: a nested node is evaluated against the object, a plain value is
	 * returned as is.
	 */
	protected Object evaluateOperand(int index, Object o) throws Exception {
		Object operand = getOperand(index);
		return switch (operand) {
			case Expression node -> node.evaluate(o);
			case ExpressionParameter parameter -> throw new ExpressionException(
					"Uninitialized parameter: " + parameter + ", call 'params' first.");
			case null, default -> operand;
		};
	}

	/**
	 * Calculates expression boolean value with object as a context for path
	 * expressions.
	 * 
	 * @since 1.1
	 */
	public boolean match(Object o) {
		return ConversionUtil.toBoolean(evaluate(o));
	}

	/**
	 * Returns the first object in the list that matches the expression.
	 * 
	 * @since 3.1
	 */
	public <T> T first(List<T> objects) {
		for (T o : objects) {
			if (match(o)) {
				return o;
			}
		}

		return null;
	}

	/**
	 * Returns a list of objects that match the expression.
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> filterObjects(Collection<T> objects) {
		if (objects == null || objects.size() == 0) {
			return new LinkedList<>(); // returning Collections.emptyList() could cause random client exceptions if they try to mutate the resulting list
		}

		return (List<T>) filter(objects, new LinkedList<>());
	}

	/**
	 * Adds objects matching this expression from the source collection to the
	 * target collection.
	 * 
	 * @since 1.1
	 */
	public <T> Collection<?> filter(Collection<T> source, Collection<T> target) {
		for (T o : source) {
			if (match(o)) {
				target.add(o);
			}
		}

		return target;
	}

	/**
	 * Clones this expression.
	 * 
	 * @since 1.1
	 */
	public Expression deepCopy() {
		return transform(null);
	}

	/**
	 * Creates a copy of this expression node, without copying children.
	 * 
	 * @since 1.1
	 */
	public abstract Expression shallowCopy();

	/**
	 * Returns true if this node should be pruned from expression tree in the
	 * event a child is removed.
	 * 
	 * @since 1.1
	 */
	protected boolean pruneNodeForPrunedChild(Object prunedChild) {
		return true;
	}

	/**
	 * Flattens the tree under this node by eliminating any operands that are nodes of the same class as this node
	 * and copying their operands to this node.
	 * 
	 * @since 1.1
	 */
	protected void flattenTree() {
		if (operands == null) {
			return;
		}

		boolean shouldFlatten = false;
		int newSize = 0;

		for (Object operand : operands) {
			if (operand != null && operand.getClass() == getClass()) {
				shouldFlatten = true;
				newSize += ((Expression) operand).getOperandCount();
			} else {
				newSize++;
			}
		}

		if (shouldFlatten) {
			Object[] newOperands = new Object[newSize];
			int j = 0;

			for (Object operand : operands) {
				if (operand != null && operand.getClass() == getClass()) {
					Expression nested = (Expression) operand;
					for (int k = 0; k < nested.getOperandCount(); ++k) {
						newOperands[j++] = nested.operands[k];
					}
				} else {
					newOperands[j++] = operand;
				}
			}

			if (j != newSize) {
				throw new ExpressionException("Assertion error: " + j + " != " + newSize);
			}

			this.operands = newOperands;
		}
	}

	/**
	 * Traverses itself and child expressions, notifying visitor via callback
	 * methods as it goes. This is an Expression-specific implementation of the
	 * "Visitor" design pattern.
	 * 
	 * @since 1.1
	 */
	public void traverse(TraversalHandler visitor) {
		if (visitor == null) {
			throw new NullPointerException("Null Visitor.");
		}

		traverse(null, visitor);
	}

	/**
	 * Traverses itself and child expressions, notifying visitor via callback
	 * methods as it goes.
	 * 
	 * @since 1.1
	 */
	protected void traverse(Expression parentExp, TraversalHandler visitor) {

		visitor.startNode(this, parentExp);

		// recursively traverse each child
		int count = getOperandCount();
		for (int i = 0; i < count; i++) {
			Object child = getOperand(i);

			if (child instanceof Expression childExp) {
				childExp.traverse(this, visitor);
			} else {
				visitor.objectNode(child, this);
			}

			visitor.finishedChild(this, i, i < count - 1);
		}

		visitor.endNode(this, parentExp);
	}

	/**
	 * Creates a transformed copy of this expression, applying transformation
	 * provided by Transformer to all its nodes. Null transformer will result in
	 * an identical deep copy of this expression.
	 * <p>
	 * To force a node and its children to be pruned from the copy, Transformer
	 * should return Expression.PRUNED_NODE. Otherwise an expectation is that if
	 * a node is an Expression it must be transformed to null or another
	 * Expression. Any other object type would result in a ExpressionException.
	 * 
	 * @since 1.1
	 */
	public Expression transform(Function<Object, Object> transformer) {
		Object transformed = transformExpression(transformer);

		if (transformed == PRUNED_NODE || transformed == null) {
			return null;
		} else if (transformed instanceof Expression) {
			return (Expression) transformed;
		}

		throw new ExpressionException("Invalid transformed expression: " + transformed);
	}

	/**
	 * A recursive method called from "transform" to do the actual
	 * transformation.
	 * 
	 * @return null, Expression.PRUNED_NODE or transformed expression.
	 * @since 1.2
	 */
	protected Object transformExpression(Function<Object, Object> transformer) {
		Expression copy = shallowCopy();
		int count = getOperandCount();
		for (int i = 0, j = 0; i < count; i++) {
			Object operand = getOperand(i);
			Object transformedChild;

			if (operand instanceof Expression) {
				transformedChild = ((Expression) operand).transformExpression(transformer);
			} else if (transformer != null) {
				transformedChild = transformer.apply(operand);
			} else {
				transformedChild = operand;
			}

			// prune null children only if there is a transformer and it
			// indicated so
			boolean prune = transformer != null && transformedChild == PRUNED_NODE;

			if (!prune) {
				copy.setOperand(j, transformedChild);
				j++;
			}

			if (prune && pruneNodeForPrunedChild(operand)) {
				// bail out early...
				return PRUNED_NODE;
			}
		}

		// all the children are processed, only now transform this copy
		return (transformer != null) ? transformer.apply(copy) : copy;
	}

	/**
	 * Encodes itself, wrapping the string into XML CDATA section.
	 * 
	 * @since 1.1
	 */
	@Override
	public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
		StringBuilder sb = new StringBuilder();
		try {
			appendAsString(sb);
		} catch (IOException e) {
			throw new CayenneRuntimeException("Unexpected IO exception appending to PrintWriter", e);
		}
		encoder.cdata(sb.toString(), true);
	}

	/**
	 * Appends own content as a String to the provided Appendable: the operands separated by the operator.
	 * 
	 * @since 4.0
	 * @throws IOException
	 */
	public void appendAsString(Appendable out) throws IOException {
		int count = getOperandCount();
		for (int i = 0; i < count; ++i) {
			if (i > 0) {
				out.append(' ');
				out.append(getExpressionOperator(i));
				out.append(' ');
			}

			appendOperandAsString(i, out);
		}
	}

	/**
	 * Returns the operator printed between the operands, before the operand at the given index.
	 */
	protected abstract String getExpressionOperator(int index);

	/**
	 * Whether this node is wrapped in parentheses when printed as an operand of another node. Compound nodes are,
	 * leaves and nodes with their own delimiters (function calls, lists) are not.
	 */
	protected boolean parenthesizeAsOperand() {
		return true;
	}

	/**
	 * Appends the operand at the given index to the output: a nested node as itself, wrapped in parentheses if it
	 * asks for it via {@link #parenthesizeAsOperand()}, a plain value as a literal.
	 */
	protected void appendOperandAsString(int index, Appendable out) throws IOException {
		// print the operand as stored, not via getOperand(..): an unresolved enum prints without loading its class
		Object operand = operands[index];
		if (!(operand instanceof Expression node)) {
			ExpHelper.appendScalarAsString(out, operand, '\"');
			return;
		}

		boolean parenthesize = node.parenthesizeAsOperand();
		if (parenthesize) {
			out.append('(');
		}
		node.appendAsString(out);
		if (parenthesize) {
			out.append(')');
		}
	}

	@Override
	public String toString() {
		StringBuilder out = new StringBuilder();
		try {
			appendAsString(out);
		} catch (IOException e) {
			throw new CayenneRuntimeException("Unexpected IO exception appending to StringBuilder", e);
		}
		return out.toString();
	}

	final class NamedParamTransformer implements Function<Object, Object> {

		private Map<String, ?> parameters;
		private boolean pruneMissing;

		NamedParamTransformer(Map<String, ?> parameters, boolean pruneMissing) {
			this.parameters = parameters;
			this.pruneMissing = pruneMissing;
		}

		@Override
		public Object apply(Object object) {
			if (!(object instanceof ExpressionParameter)) {

				// normally Object[] is a ListExp child
				if (object instanceof Object[]) {

					Object[] source = (Object[]) object;
					int len = source.length;
					Object[] target = new Object[len];

					for (int i = 0; i < len; i++) {
						target[i] = apply(source[i]);
					}

					return target;
				}

				return object;
			}

			String name = ((ExpressionParameter) object).getName();
			if (!parameters.containsKey(name)) {
				if (pruneMissing) {
					return PRUNED_NODE;
				} else {
					throw new ExpressionException("Missing required parameter: $" + name);
				}
			} else {
				// a value takes the place of the parameter as is, a collection as a list node
				return ExpressionFactory.wrapPathOperand(parameters.get(name));
			}
		}

	}

	final class InPlaceParamReplacer implements TraversalHandler {

		private Object[] parameters;
		private int i;
		private Map<String, Object> seen;

		InPlaceParamReplacer(Object[] parameters) {
			this.parameters = parameters;
		}

		void onFinish() {
			if (i < parameters.length) {
				throw new ExpressionException("Too many parameters to bind expression. Expected: " + i + ", actual: "
						+ parameters.length);
			}
		}

		@Override
		public void finishedChild(Expression node, int childIndex, boolean hasMoreChildren) {

			Object child = node.getOperand(childIndex);
			if (child instanceof ExpressionParameter) {
				node.setOperand(childIndex, nextValue(((ExpressionParameter) child).getName()));
			}
			// normally Object[] is a ListExp child
			else if (child instanceof Object[]) {
				Object[] array = (Object[]) child;

				for (int i = 0; i < array.length; i++) {
					if (array[i] instanceof ExpressionParameter) {
						array[i] = nextValue(((ExpressionParameter) array[i]).getName());
					}
				}
			}
		}

		private Object nextValue(String name) {

			if (seen == null) {
				seen = new HashMap<>();
			}

			Object p;
			if (seen.containsKey(name)) {
				p = seen.get(name);
			} else {
				if (i >= parameters.length) {
					throw new ExpressionException("Too few parameters to bind expression: " + parameters.length);
				}

				p = parameters[i++];
				seen.put(name, p);
			}

			// a value takes the place of the parameter as is, a collection as a list node
			return ExpressionFactory.wrapPathOperand(p);
		}

	}
}
