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
package org.apache.cayenne.dba.derby;

import java.io.IOException;
import java.sql.Types;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.translator.select.QueryAssembler;
import org.apache.cayenne.access.translator.select.TrimmingQualifierTranslator;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTEqual;
import org.apache.cayenne.exp.parser.ASTExtract;
import org.apache.cayenne.exp.parser.ASTFunctionCall;
import org.apache.cayenne.exp.parser.ASTNotEqual;
import org.apache.cayenne.exp.parser.SimpleNode;
import org.apache.cayenne.map.DbAttribute;

public class DerbyQualifierTranslator extends TrimmingQualifierTranslator {

	public DerbyQualifierTranslator(QueryAssembler queryAssembler, String trimFunction) {
		super(queryAssembler, trimFunction);
	}

	@Override
	protected void processColumnWithQuoteSqlIdentifiers(DbAttribute dbAttr, Expression pathExp) {

		SimpleNode parent = null;
		if (pathExp instanceof SimpleNode) {
			parent = (SimpleNode) ((SimpleNode) pathExp).jjtGetParent();
		}

		// problem in derby : Comparisons between 'CLOB (UCS_BASIC)' and 'CLOB
		// (UCS_BASIC)' are not supported.
		// we need do it by casting the Clob to VARCHAR.
		if (parent != null && (parent instanceof ASTEqual || parent instanceof ASTNotEqual)
				&& dbAttr.getType() == Types.CLOB && parent.getOperandCount() == 2
				&& parent.getOperand(1) instanceof String) {
			Integer size = parent.getOperand(1).toString().length() + 1;

			out.append("CAST(");
			super.processColumnWithQuoteSqlIdentifiers(dbAttr, pathExp);
			out.append(" AS VARCHAR(").append(size).append("))");
		} else {
			super.processColumnWithQuoteSqlIdentifiers(dbAttr, pathExp);
		}
	}

	/**
	 * @since 4.0
	 */
	@Override
	protected void appendFunction(ASTFunctionCall functionExpression) {
		if("SUBSTRING".equals(functionExpression.getFunctionName())) {
			out.append("SUBSTR");
		} else if("CONCAT".equals(functionExpression.getFunctionName())) {
			out.append("");
		} else {
			super.appendFunction(functionExpression);
		}
	}

	/**
	 * A little bit ugly code that wraps String scalars to CAST(? AS VARCHAR(length))
	 * because otherwise derby don't know what type will be at the placeholder and
	 * use LONG VARCHAR that isn't comparable what leads to statement preparation failure.
	 *
	 * @since 4.0
	 */
	protected void appendFunctionArg(Object value, ASTFunctionCall functionExpression) throws IOException {
		if("CONCAT".equals(functionExpression.getFunctionName())) {
			if(value instanceof String) {
				out.append("CAST(");
			}
			super.appendFunctionArg(value, functionExpression);
			if(value instanceof String) {
				clearLastFunctionArgDivider(functionExpression);
				out.append(" AS VARCHAR(").append(((String)value).length()).append("))");
				appendFunctionArgDivider(functionExpression);
			}
		} else {
			super.appendFunctionArg(value, functionExpression);
		}
	}

	/**
	 * @since 4.0
	 */
	@Override
	protected void appendFunctionArgDivider(ASTFunctionCall functionExpression) {
		if("CONCAT".equals(functionExpression.getFunctionName())) {
			out.append(" || ");
		} else {
			super.appendFunctionArgDivider(functionExpression);
		}
	}

	/**
	 * @since 4.0
	 */
	@Override
	protected void clearLastFunctionArgDivider(ASTFunctionCall functionExpression) {
		if("CONCAT".equals(functionExpression.getFunctionName())) {
			out.delete(out.length() - 4, out.length());
		} else {
			super.clearLastFunctionArgDivider(functionExpression);
		}
	}

	@Override
	protected void appendExtractFunction(ASTExtract functionExpression) {
		switch (functionExpression.getPart()) {
			case DAY_OF_MONTH:
				out.append("DAY");
				break;
			case DAY_OF_WEEK:
			case DAY_OF_YEAR:
			case WEEK:
				throw new CayenneRuntimeException("Function %s() is unsupported in Derby."
						, functionExpression.getPartCamelCaseName());
			default:
				super.appendExtractFunction(functionExpression);
		}
	}
}
