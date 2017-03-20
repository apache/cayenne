/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.dba.postgres;

import java.io.IOException;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.translator.select.QueryAssembler;
import org.apache.cayenne.access.translator.select.TrimmingQualifierTranslator;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTExtract;
import org.apache.cayenne.exp.parser.ASTFunctionCall;
import org.apache.cayenne.exp.parser.PatternMatchNode;

/**
 * Uses Postgres extensions to optimize various translations.
 * 
 * @since 1.1
 */
public class PostgresQualifierTranslator extends TrimmingQualifierTranslator {

	public PostgresQualifierTranslator(QueryAssembler queryAssembler) {
		super(queryAssembler, "RTRIM");
	}

	@Override
	public void startNode(Expression node, Expression parentNode) {
		// super implementation has special handling of LIKE_IGNORE_CASE and NOT_LIKE_IGNORE_CASE
		// Postgres uses ILIKE
		boolean likeIgnoreCase = (node.getType() == Expression.LIKE_IGNORE_CASE || node.getType() == Expression.NOT_LIKE_IGNORE_CASE);
		if (likeIgnoreCase) {
			// binary nodes are the only ones that currently require this
			detectObjectMatch(node);
			if (parenthesisNeeded(node, parentNode)) {
				out.append('(');
			}
		} else {
			super.startNode(node, parentNode);
		}
	}

	@Override
	public void endNode(Expression node, Expression parentNode) {
		// super implementation has special handling of LIKE_IGNORE_CASE and NOT_LIKE_IGNORE_CASE
		// Postgres uses ILIKE
		boolean likeIgnoreCase = (node.getType() == Expression.LIKE_IGNORE_CASE || node.getType() == Expression.NOT_LIKE_IGNORE_CASE);

		if (likeIgnoreCase) {
			try {
				if (matchingObject) {
					appendObjectMatch();
				}

				if (PatternMatchNode.class.isAssignableFrom(node.getClass())) {
					appendLikeEscapeCharacter((PatternMatchNode) node);
				}

				if (parenthesisNeeded(node, parentNode)) {
					out.append(')');
				}
			} catch (IOException ioex) {
				throw new CayenneRuntimeException("Error appending content", ioex);
			}
		} else {
			super.endNode(node, parentNode);
		}
	}

	@Override
	public void finishedChild(Expression node, int childIndex, boolean hasMoreChildren) {
		if (!hasMoreChildren) {
			return;
		}

		try {
			// use ILIKE
			switch (node.getType()) {
				case Expression.LIKE_IGNORE_CASE:
					finishedChildNodeAppendExpression(node, " ILIKE ");
					break;
				case Expression.NOT_LIKE_IGNORE_CASE:
					finishedChildNodeAppendExpression(node, " NOT ILIKE ");
					break;
				default:
					super.finishedChild(node, childIndex, hasMoreChildren);
			}
		} catch (IOException ioex) {
			throw new CayenneRuntimeException("Error appending content", ioex);
		}
	}

	private void finishedChildNodeAppendExpression(Expression node, String operation) throws IOException {
		Appendable buf = matchingObject ? new StringBuilder() : this.out;
		buf.append(operation);
		if (matchingObject) {
			objectMatchTranslator.setOperation(buf.toString());
			objectMatchTranslator.setExpression(node);
		}
	}

    /**
     * @since 4.0
     */
	@Override
	protected void appendFunction(ASTFunctionCall functionExpression) {
		if("LOCATE".equals(functionExpression.getFunctionName())) {
			out.append("POSITION");
		} else {
			super.appendFunction(functionExpression);
		}
	}

    /**
     * @since 4.0
     */
	@Override
	protected void appendFunctionArgDivider(ASTFunctionCall functionExpression) {
		if("LOCATE".equals(functionExpression.getFunctionName())) {
			out.append(" in ");
		} else {
			super.appendFunctionArgDivider(functionExpression);
		}
	}

    /**
     * @since 4.0
     */
	@Override
	protected void clearLastFunctionArgDivider(ASTFunctionCall functionExpression) {
		if("LOCATE".equals(functionExpression.getFunctionName())) {
			out.delete(out.length() - " in ".length(), out.length());
		} else {
			super.clearLastFunctionArgDivider(functionExpression);
		}

		if(functionExpression instanceof ASTExtract) {
			out.append(")");
		}
	}

	@Override
	protected boolean parenthesisNeeded(Expression node, Expression parentNode) {
		if (node.getType() == Expression.FUNCTION_CALL) {
			if (node instanceof ASTExtract) {
				return false;
			}
		}

		return super.parenthesisNeeded(node, parentNode);
	}


	@Override
	protected void appendExtractFunction(ASTExtract functionExpression) {
		out.append("EXTRACT(");
		switch (functionExpression.getPart()) {
			case DAY_OF_MONTH:
				out.append("day");
				break;
			case DAY_OF_WEEK:
				out.append("dow");
				break;
			case DAY_OF_YEAR:
				out.append("doy");
				break;
			default:
				out.append(functionExpression.getPartCamelCaseName());
		}

		out.append(" FROM ");
	}
}
