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

package org.apache.cayenne.dba.sybase;

import org.apache.cayenne.access.translator.select.QualifierTranslator;
import org.apache.cayenne.access.translator.select.QueryAssembler;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTExtract;
import org.apache.cayenne.exp.parser.ASTFunctionCall;

/**
 * @since 4.0
 */
public class SybaseQualifierTranslator extends QualifierTranslator {

    public SybaseQualifierTranslator(QueryAssembler queryAssembler) {
        super(queryAssembler);
    }

    @Override
    protected void appendFunction(ASTFunctionCall functionExpression) {
        switch (functionExpression.getFunctionName()) {
            case "MOD":
            case "CONCAT":
                // noop
                break;
            case "LENGTH":
                out.append("LEN");
                break;
            case "LOCATE":
                out.append("CHARINDEX");
                break;
            default:
                super.appendFunction(functionExpression);
        }
    }

    @Override
    protected void appendFunctionArgDivider(ASTFunctionCall functionExpression) {
        switch (functionExpression.getFunctionName()) {
            case "MOD":
                out.append(" % ");
                break;
            case "CONCAT":
                out.append(" + ");
                break;
            default:
                super.appendFunctionArgDivider(functionExpression);
        }
    }

    @Override
    protected void clearLastFunctionArgDivider(ASTFunctionCall functionExpression) {
        switch (functionExpression.getFunctionName()) {
            case "MOD":
            case "CONCAT":
                out.delete(out.length() - 3, out.length());
                break;
            default:
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
        out.append("datepart(");
        switch (functionExpression.getPart()) {
            case DAY_OF_MONTH:
                out.append("day");
                break;
            case DAY_OF_WEEK:
                out.append("weekday");
                break;
            case DAY_OF_YEAR:
                out.append("dayofyear");
                break;
            default:
                out.append(functionExpression.getPart().name().toLowerCase());
        }
        out.append(" , ");
    }
}
