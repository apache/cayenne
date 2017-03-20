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

package org.apache.cayenne.dba.ingres;

import java.io.IOException;

import org.apache.cayenne.access.translator.select.QueryAssembler;
import org.apache.cayenne.access.translator.select.TrimmingQualifierTranslator;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTExtract;
import org.apache.cayenne.exp.parser.ASTFunctionCall;
import org.apache.cayenne.exp.parser.Node;

/**
 * @since 4.0
 */
class IngresQualifierTranslator extends TrimmingQualifierTranslator {

    IngresQualifierTranslator(QueryAssembler queryAssembler) {
        super(queryAssembler, IngresAdapter.TRIM_FUNCTION);
    }

    @Override
    public void endNode(Expression node, Expression parentNode) {
        super.endNode(node, parentNode);
        if(node.getType() == Expression.FUNCTION_CALL) {
            if("LOCATE".equals(((ASTFunctionCall)node).getFunctionName())) {
                // order of args in ingres version of LOCATE is different, so swap them back
                swapNodeChildren((ASTFunctionCall)node, 0, 1);
            }
        }
    }

    @Override
    protected void appendFunction(ASTFunctionCall functionExpression) {
        if("CONCAT".equals(functionExpression.getFunctionName())) {
            // noop
        } else if("LOCATE".equals(functionExpression.getFunctionName())) {
            // order of args in ingres version of LOCATE is different
            // LOCATE(substr, str) -> LOCATE(str, substr)
            out.append("LOCATE");
            swapNodeChildren(functionExpression, 0, 1);
        } else if("TRIM".equals(functionExpression.getFunctionName())) {
            // simple TRIM removes only trailing spaces
            out.append("LTRIM(RTRIM");
        } else {
            super.appendFunction(functionExpression);
        }
    }

    @Override
    protected void appendFunctionArgDivider(ASTFunctionCall functionExpression) {
        if("CONCAT".equals(functionExpression.getFunctionName())) {
            out.append(" + ");
        } else {
            super.appendFunctionArgDivider(functionExpression);
        }
    }

    @Override
    protected void appendFunctionArg(Object value, ASTFunctionCall functionExpression) throws IOException {
        if("SUBSTRING".equals(functionExpression.getFunctionName())) {
            out.append("CAST(");
            super.appendFunctionArg(value, functionExpression);
            clearLastFunctionArgDivider(functionExpression);
            out.append(" AS INTEGER)");
            appendFunctionArgDivider(functionExpression);
        } else {
            super.appendFunctionArg(value, functionExpression);
        }
    }

    @Override
    protected void clearLastFunctionArgDivider(ASTFunctionCall functionExpression) {
        if("CONCAT".equals(functionExpression.getFunctionName())) {
            out.delete(out.length() - " + ".length(), out.length());
        } else {
            super.clearLastFunctionArgDivider(functionExpression);
            if("TRIM".equals(functionExpression.getFunctionName())) {
                out.append(")");
            }
        }
    }

    @Override
    protected void appendExtractFunction(ASTExtract functionExpression) {
        switch (functionExpression.getPart()) {
            case DAY_OF_WEEK:
            case DAY_OF_MONTH:
            case DAY_OF_YEAR:
                // ingres variants are without '_'
                out.append(functionExpression.getPart().name().replace("_", ""));
                break;
            default:
                appendFunction(functionExpression);
        }
    }

    private void swapNodeChildren(Node node, int i, int j) {
        Node ni = node.jjtGetChild(i);
        Node nj = node.jjtGetChild(j);
        node.jjtAddChild(ni, j);
        node.jjtAddChild(nj, i);
    }
}
