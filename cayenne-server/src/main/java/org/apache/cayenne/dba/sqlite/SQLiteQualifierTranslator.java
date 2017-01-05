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

package org.apache.cayenne.dba.sqlite;

import org.apache.cayenne.access.translator.select.QualifierTranslator;
import org.apache.cayenne.access.translator.select.QueryAssembler;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTFunctionCall;
import org.apache.cayenne.exp.parser.Node;

/**
 * @since 4.0
 */
public class SQLiteQualifierTranslator extends QualifierTranslator {

    public SQLiteQualifierTranslator(QueryAssembler queryAssembler) {
        super(queryAssembler);
    }

    @Override
    public void endNode(Expression node, Expression parentNode) {
        super.endNode(node, parentNode);
        if(node.getType() == Expression.FUNCTION_CALL) {
            if("LOCATE".equals(((ASTFunctionCall)node).getFunctionName())) {
                // order of args in INSTR is different, so swap them back
                swapNodeChildren((ASTFunctionCall)node, 0, 1);
            }
        }
    }

    @Override
    protected void appendFunction(ASTFunctionCall functionExpression) {
        switch (functionExpression.getFunctionName()) {
            case "MOD":
            case "CONCAT":
                // noop
                break;
            case "SUBSTRING":
                out.append("SUBSTR");
                break;
            case "LOCATE":
                // LOCATE(substr, str) -> INSTR(str, substr)
                out.append("INSTR");
                swapNodeChildren(functionExpression, 0, 1);
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
                out.append(" || ");
                break;
            default:
                super.appendFunctionArgDivider(functionExpression);
        }
    }

    @Override
    protected void clearLastFunctionArgDivider(ASTFunctionCall functionExpression) {
        switch (functionExpression.getFunctionName()) {
            case "MOD":
                out.delete(out.length() - 3, out.length());
                break;
            case "CONCAT":
                out.delete(out.length() - 4, out.length());
                break;
            default:
                super.clearLastFunctionArgDivider(functionExpression);
        }
    }

    private void swapNodeChildren(Node node, int i, int j) {
        Node ni = node.jjtGetChild(i);
        Node nj = node.jjtGetChild(j);
        node.jjtAddChild(ni, j);
        node.jjtAddChild(nj, i);
    }
}
