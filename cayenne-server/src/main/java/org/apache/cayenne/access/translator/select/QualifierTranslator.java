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

package org.apache.cayenne.access.translator.select;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.TraversalHandler;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.parser.ASTExtract;
import org.apache.cayenne.exp.parser.ASTFunctionCall;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.exp.parser.PatternMatchNode;
import org.apache.cayenne.exp.parser.SimpleNode;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * Translates query qualifier to SQL. Used as a helper class by query
 * translators.
 */
public class QualifierTranslator extends QueryAssemblerHelper implements TraversalHandler {

	protected DataObjectMatchTranslator objectMatchTranslator;
	protected boolean matchingObject;
	protected boolean caseInsensitive;

	/**
	 * @since 4.0
	 */
	protected boolean useAliasForExpressions;

	/**
	 * @since 4.0
	 */
	protected Expression waitingForEndNode;

	/**
	 * @since 4.0
	 */
	protected Expression qualifier;

	public QualifierTranslator(QueryAssembler queryAssembler) {
		super(queryAssembler);

		caseInsensitive = false;
	}

	/**
	 * Translates query qualifier to SQL WHERE clause. Qualifier is obtained
	 * from the parent queryAssembler.
	 * 
	 * @since 3.0
	 */
	@Override
	protected void doAppendPart() {
		doAppendPart(extractQualifier());
	}

	public void setCaseInsensitive(boolean caseInsensitive) {
		this.caseInsensitive = caseInsensitive;
	}

	/**
	 * Explicitly set qualifier.
	 * It will be used instead of extracting qualifier from the query itself.
	 * @since 4.0
	 */
	public void setQualifier(Expression qualifier) {
		this.qualifier = qualifier;
	}

	/**
	 * @since 4.0
	 */
	public void setUseAliasForExpressions(boolean useAliasForExpressions) {
		this.useAliasForExpressions = useAliasForExpressions;
	}

	/**
	 * Translates query qualifier to SQL WHERE clause. Qualifier is a method
	 * parameter.
	 * 
	 * @since 3.0
	 */
	protected void doAppendPart(Expression rootNode) {
		if (rootNode == null) {
			return;
		}
		rootNode.traverse(this);
	}

	protected Expression extractQualifier() {
		// if additional qualifier is set, use it
		if(this.qualifier != null) {
			return this.qualifier;
		}

		Query q = queryAssembler.getQuery();

		Expression qualifier = ((SelectQuery<?>) q).getQualifier();

		// append Entity qualifiers, taking inheritance into account
		ObjEntity entity = getObjEntity();

		if (entity != null) {

			ClassDescriptor descriptor = queryAssembler.getEntityResolver().getClassDescriptor(entity.getName());
			Expression entityQualifier = descriptor.getEntityInheritanceTree().qualifierForEntityAndSubclasses();
			if (entityQualifier != null) {
				qualifier = (qualifier != null) ? qualifier.andExp(entityQualifier) : entityQualifier;
			}
		}

		// Attaching root Db entity's qualifier
		if (getDbEntity() != null) {
			Expression dbQualifier = getDbEntity().getQualifier();
			if (dbQualifier != null) {
				dbQualifier = dbQualifier.transform(new DbEntityQualifierTransformer());

				qualifier = qualifier == null ? dbQualifier : qualifier.andExp(dbQualifier);
			}
		}

		return qualifier;
	}

	/**
	 * Called before processing an expression to initialize
	 * objectMatchTranslator if needed.
	 */
	protected void detectObjectMatch(Expression exp) {
		// On demand initialization of
		// objectMatchTranslator is not possible since there may be null
		// object values that would not allow to detect the need for
		// such translator in the right time (e.g.: null = dbpath)

		matchingObject = false;

		if (exp.getOperandCount() != 2) {
			// only binary expressions are supported
			return;
		}

		// check if there are DataObjects among direct children of the
		// Expression
		for (int i = 0; i < 2; i++) {
			Object op = exp.getOperand(i);
			if (op instanceof Persistent || op instanceof ObjectId) {
				matchingObject = true;

				if (objectMatchTranslator == null) {
					objectMatchTranslator = new DataObjectMatchTranslator();
				} else {
					objectMatchTranslator.reset();
				}
				break;
			}
		}
	}

	protected void appendObjectMatch() throws IOException {
		if (!matchingObject || objectMatchTranslator == null) {
			throw new IllegalStateException("An invalid attempt to append object match.");
		}

		// turn off special handling, so that all the methods behave as a
		// superclass's
		// impl.
		matchingObject = false;
		boolean first = true;
		boolean needToProcessLiterals = true;

		DbRelationship relationship = objectMatchTranslator.getRelationship();
		if (relationship != null && !relationship.isToMany() && !relationship.isToPK()) {
			needToProcessLiterals = false;
			queryAssembler.dbRelationshipAdded(relationship, JoinType.INNER, objectMatchTranslator.getJoinSplitAlias());
		}

		Map<String, DbAttribute> attributes = objectMatchTranslator.attributes;
		if(attributes != null) {
			needToProcessLiterals = false;
			Iterator<String> it = objectMatchTranslator.keys();
			while (it.hasNext()) {
				if (first) {
					first = false;
				} else {
					out.append(" AND ");
				}

				String key = it.next();
				DbAttribute attr = objectMatchTranslator.getAttribute(key);
				Object val = objectMatchTranslator.getValue(key);

				processColumn(attr);
				out.append(objectMatchTranslator.getOperation());
				appendLiteral(val, attr, objectMatchTranslator.getExpression());
			}
		}

		if(needToProcessLiterals) {
			DbAttribute attribute = paramsDbType(objectMatchTranslator.getExpression());
			matchingObject = false;
			appendLiteral(
					objectMatchTranslator.getValue(attribute.getName()),
					attribute,
					objectMatchTranslator.getExpression());
		}

		objectMatchTranslator.reset();
	}

	@Override
	public void finishedChild(Expression node, int childIndex, boolean hasMoreChildren) {

		if(waitingForEndNode != null) {
			return;
		}

		if (!hasMoreChildren) {
			return;
		}
		boolean hasObjectsToMatch = objectMatchTranslator != null &&
				(objectMatchTranslator.attributes != null ||
				objectMatchTranslator.relationship != null);
		Appendable out = (matchingObject && hasObjectsToMatch) ? new StringBuilder() : this.out;

		try {
			switch (node.getType()) {
			case Expression.AND:
				out.append(" AND ");
				break;
			case Expression.OR:
				out.append(" OR ");
				break;
			case Expression.EQUAL_TO:
				// translate NULL as IS NULL
				if (childIndex == 0 && node.getOperandCount() == 2 && node.getOperand(1) == null) {
					out.append(" IS ");
				} else {
					out.append(" = ");
				}
				break;
			case Expression.NOT_EQUAL_TO:
				// translate NULL as IS NOT NULL
				if (childIndex == 0 && node.getOperandCount() == 2 && node.getOperand(1) == null) {
					out.append(" IS NOT ");
				} else {
					out.append(" <> ");
				}
				break;
			case Expression.LESS_THAN:
				out.append(" < ");
				break;
			case Expression.GREATER_THAN:
				out.append(" > ");
				break;
			case Expression.LESS_THAN_EQUAL_TO:
				out.append(" <= ");
				break;
			case Expression.GREATER_THAN_EQUAL_TO:
				out.append(" >= ");
				break;
			case Expression.IN:
				out.append(" IN ");
				break;
			case Expression.NOT_IN:
				out.append(" NOT IN ");
				break;
			case Expression.LIKE:
				out.append(" LIKE ");
				break;
			case Expression.NOT_LIKE:
				out.append(" NOT LIKE ");
				break;
			case Expression.LIKE_IGNORE_CASE:
				if (caseInsensitive) {
					out.append(" LIKE ");
				} else {
					out.append(") LIKE UPPER(");
				}
				break;
			case Expression.NOT_LIKE_IGNORE_CASE:
				if (caseInsensitive) {
					out.append(" NOT LIKE ");
				} else {
					out.append(") NOT LIKE UPPER(");
				}
				break;
			case Expression.ADD:
				out.append(" + ");
				break;
			case Expression.SUBTRACT:
				out.append(" - ");
				break;
			case Expression.MULTIPLY:
				out.append(" * ");
				break;
			case Expression.DIVIDE:
				out.append(" / ");
				break;
			case Expression.BETWEEN:
				if (childIndex == 0) {
					out.append(" BETWEEN ");
				} else if (childIndex == 1) {
					out.append(" AND ");
				}
				break;
			case Expression.NOT_BETWEEN:
				if (childIndex == 0) {
					out.append(" NOT BETWEEN ");
				} else if (childIndex == 1) {
					out.append(" AND ");
				}
				break;
			case Expression.BITWISE_OR:
				out.append(" ").append(operandForBitwiseOr()).append(" ");
				break;
			case Expression.BITWISE_AND:
				out.append(" ").append(operandForBitwiseAnd()).append(" ");
				break;
			case Expression.BITWISE_XOR:
				out.append(" ").append(operandForBitwiseXor()).append(" ");
				break;
			case Expression.BITWISE_LEFT_SHIFT:
				out.append(" ").append(operandForBitwiseLeftShift()).append(" ");
				break;
			case Expression.BITWISE_RIGHT_SHIFT:
				out.append(" ").append(operandForBitwiseRightShift()).append("");
				break;
			}
		} catch (IOException ioex) {
			throw new CayenneRuntimeException("Error appending content", ioex);
		}

		if (matchingObject) {
			objectMatchTranslator.setOperation(out.toString());
			objectMatchTranslator.setExpression(node);
		}
	}

	/**
	 * @since 3.1
	 */
	protected String operandForBitwiseNot() {
		return "~";
	}

	/**
	 * @since 3.1
	 */
	protected String operandForBitwiseOr() {
		return "|";
	}

	/**
	 * @since 3.1
	 */
	protected String operandForBitwiseAnd() {
		return "&";
	}

	/**
	 * @since 3.1
	 */
	protected String operandForBitwiseXor() {
		return "^";
	}

	/**
	 * @since 4.0
	 */
	protected String operandForBitwiseLeftShift() {
		return "<<";
	}

	/**
	 * @since 4.0
	 */
	protected String operandForBitwiseRightShift() {
		return ">>";
	}

	@Override
	public void startNode(Expression node, Expression parentNode) {

		if(waitingForEndNode != null) {
			return;
		}

		if(useAliasForExpressions) {
			String alias = queryAssembler.getAliasForExpression(node);
			if(alias != null) {
				out.append(alias);
				waitingForEndNode = node;
				return;
			}
		}

		boolean parenthesisNeeded = parenthesisNeeded(node, parentNode);

		if(node.getType() == Expression.FUNCTION_CALL) {
			if(node instanceof ASTExtract) {
				appendExtractFunction((ASTExtract) node);
			} else {
				appendFunction((ASTFunctionCall) node);
			}
			if(parenthesisNeeded) {
				out.append("(");
			}
			return;
		}

		if(node.getType() == Expression.FULL_OBJECT && parentNode != null) {
			throw new CayenneRuntimeException("Expression is not supported in where clause.");
		}

		int count = node.getOperandCount();

		if (count == 2) {
			// binary nodes are the only ones that currently require this
			detectObjectMatch(node);
		}

		if (parenthesisNeeded) {
			out.append('(');
		}

		if (count == 0) {
			// not all databases handle true/false
			if (node.getType() == Expression.TRUE) {
				out.append("1 = 1");
			} else if (node.getType() == Expression.FALSE) {
				out.append("1 = 0");
			} else if (node.getType() == Expression.ASTERISK) {
				out.append("*");
			}
		}

		if (count == 1) {
			if (node.getType() == Expression.NEGATIVE) {
				out.append('-');
			} else if (node.getType() == Expression.NOT) {
				out.append("NOT ");
			} else if (node.getType() == Expression.BITWISE_NOT) {
				out.append(operandForBitwiseNot());
			}
		} else if ((node.getType() == Expression.LIKE_IGNORE_CASE || node.getType() == Expression.NOT_LIKE_IGNORE_CASE)
				&& !caseInsensitive) {
			out.append("UPPER(");
		}

	}

	/**
	 * @since 1.1
	 */
	@Override
	public void endNode(Expression node, Expression parentNode) {

		if(waitingForEndNode != null) {
			if(node == waitingForEndNode) {
				waitingForEndNode = null;
			}
			return;
		}

		try {
			// check if we need to use objectMatchTranslator to finish building the expression
			if (node.getOperandCount() == 2 && matchingObject) {
				appendObjectMatch();
			}

			boolean parenthesisNeeded = parenthesisNeeded(node, parentNode);
			boolean likeIgnoreCase = (node.getType() == Expression.LIKE_IGNORE_CASE || node.getType() == Expression.NOT_LIKE_IGNORE_CASE);
			boolean isPatternMatchNode = PatternMatchNode.class.isAssignableFrom(node.getClass());

			// closing UPPER parenthesis
			if (likeIgnoreCase && !caseInsensitive) {
				out.append(')');
			}

			if (isPatternMatchNode) {
				appendLikeEscapeCharacter((PatternMatchNode) node);
			}

			// clean up trailing comma in function argument list
			if(node.getType() == Expression.FUNCTION_CALL) {
				clearLastFunctionArgDivider((ASTFunctionCall)node);
			}

			// closing LIKE parenthesis
			if (parenthesisNeeded) {
				out.append(')');
			}

			// if inside function call, put comma between arguments
			if(parentNode != null && parentNode.getType() == Expression.FUNCTION_CALL) {
				appendFunctionArgDivider((ASTFunctionCall) parentNode);
			}
		} catch (IOException ioex) {
			throw new CayenneRuntimeException("Error appending content", ioex);
		}
	}

	@Override
	public void objectNode(Object leaf, Expression parentNode) {
		if(waitingForEndNode != null) {
			return;
		}

		try {
			switch (parentNode.getType()) {
				case Expression.OBJ_PATH:
					appendObjPath(parentNode);
					break;
				case Expression.DB_PATH:
					appendDbPath(parentNode);
					break;
				case Expression.LIST:
					appendList(parentNode, paramsDbType(parentNode));
					break;
				case Expression.FUNCTION_CALL:
					appendFunctionArg(leaf, (ASTFunctionCall)parentNode);
					break;
				default:
					appendLiteral(leaf, paramsDbType(parentNode), parentNode);
			}
		} catch (IOException ioex) {
			throw new CayenneRuntimeException("Error appending content", ioex);
		}
	}

	protected boolean parenthesisNeeded(Expression node, Expression parentNode) {
		if (node.getType() == Expression.FUNCTION_CALL) {
			return ((ASTFunctionCall)node).needParenthesis();
		}

		if (parentNode == null) {
			return false;
		}

		// only unary expressions can go w/o parenthesis
		if (node.getOperandCount() > 1) {
			return true;
		}

		if (node.getType() == Expression.OBJ_PATH
				|| node.getType() == Expression.DB_PATH
				|| node.getType() == Expression.ASTERISK) {
			return false;
		}

		return true;
	}

	private final void appendList(Expression listExpr, DbAttribute paramDesc) throws IOException {
		Iterator<?> it;
		Object list = listExpr.getOperand(0);
		if (list instanceof List) {
			it = ((List<?>) list).iterator();
		} else if (list instanceof Object[]) {
			it = Arrays.asList((Object[]) list).iterator();
		} else {
			String className = (list != null) ? list.getClass().getName() : "<null>";
			throw new IllegalArgumentException("Unsupported type for the list expressions: " + className);
		}

		// process first element outside the loop
		// (unroll loop to avoid condition checking
		if (it.hasNext()) {
			appendLiteral(it.next(), paramDesc, listExpr);
		} else {
			return;
		}

		while (it.hasNext()) {
			out.append(", ");
			appendLiteral(it.next(), paramDesc, listExpr);
		}
	}

	@Override
	protected void appendLiteral(Object val, DbAttribute attr, Expression parentExpression) throws IOException {

		if (!matchingObject) {
			super.appendLiteral(val, attr, parentExpression);
		} else if (val == null || (val instanceof Persistent)) {
			objectMatchTranslator.setDataObject((Persistent) val);
		} else if (val instanceof ObjectId) {
			objectMatchTranslator.setObjectId((ObjectId) val);
		} else {
			throw new IllegalArgumentException("Attempt to use literal other than DataObject during object match.");
		}
	}

	@Override
	protected void processRelTermination(DbRelationship rel, JoinType joinType, String joinSplitAlias) {

		if (!matchingObject) {
			super.processRelTermination(rel, joinType, joinSplitAlias);
		} else {
			if (rel.isToMany()) {
				// append joins
				queryAssembler.dbRelationshipAdded(rel, joinType, joinSplitAlias);
			}
			objectMatchTranslator.setRelationship(rel, joinSplitAlias);
		}
	}

	/**
	 * Append function name to result SQL
	 * Override this method to rename or skip function if generic name isn't supported on target DB.
	 * @since 4.0
	 */
	protected void appendFunction(ASTFunctionCall functionExpression) {
		out.append(functionExpression.getFunctionName());
	}

	/**
	 * Special case for extract date/time parts functions as they have many variants
	 * @since 4.0
	 */
	protected void appendExtractFunction(ASTExtract functionExpression) {
		appendFunction(functionExpression);
	}

	/**
	 * Append scalar argument of a function call
	 * Used only for values stored in ASTScalar other
	 * expressions appended in objectNode() method
	 *
	 * @since 4.0
	 */
	protected void appendFunctionArg(Object value, ASTFunctionCall functionExpression) throws IOException {
		// Create fake DbAttribute to pass argument info down to bind it to SQL prepared statement
		DbAttribute dbAttrForArg = new DbAttribute();
		dbAttrForArg.setType(TypesMapping.getSqlTypeByJava(value.getClass()));
		super.appendLiteral(value, dbAttrForArg, functionExpression);
		appendFunctionArgDivider(functionExpression);
	}

	/**
	 * Append divider between function arguments.
	 * In overriding methods can be replaced e.g. for " || " for CONCAT operation
	 * @since 4.0
	 */
	protected void appendFunctionArgDivider(ASTFunctionCall functionExpression) {
		out.append(", ");
	}

	/**
	 * Clear last divider as we currently don't now position of argument until parent element is ended.
	 * @since 4.0
	 */
	protected void clearLastFunctionArgDivider(ASTFunctionCall functionExpression) {
		if(functionExpression.getOperandCount() > 0) {
			out.delete(out.length() - 2, out.length());
		}
	}

	/**
	 * Class to translate DB Entity qualifiers annotation to Obj-entity
	 * qualifiers annotation This is done by changing all Obj-paths to Db-paths
	 * and rejecting all original Db-paths
	 */
	class DbEntityQualifierTransformer implements Function<Object, Object> {

		public Object apply(Object input) {
			if (input instanceof ASTObjPath) {
				return new ASTDbPath(((SimpleNode) input).getOperand(0));
			}
			return input;
		}
	}
}
