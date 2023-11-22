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
package org.apache.cayenne.dba.sqlite;

import org.apache.cayenne.access.sqlbuilder.QuotingAppendable;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.OpExpressionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.TextNode;
import org.apache.cayenne.access.translator.select.BaseSQLTreeProcessor;
import org.apache.cayenne.dba.mysql.sqltree.MysqlLimitOffsetNode;

/**
 * @since 4.2
 */
public class SQLiteTreeProcessor extends BaseSQLTreeProcessor {

    @Override
    protected void onLimitOffsetNode(Node parent, LimitOffsetNode child, int index) {
        replaceChild(parent, index, new MysqlLimitOffsetNode(child.getLimit(), child.getOffset()), false);
    }

    @Override
    protected void onFunctionNode(Node parent, FunctionNode child, int index) {
        String functionName = child.getFunctionName();
        Node replacement = null;
        switch (functionName) {
            case "LOCATE":
                replacement = new FunctionNode("INSTR", child.getAlias(), true);
                for (int i = 0; i <= 1; i++) {
                    replacement.addChild(child.getChild(1 - i));
                }
                parent.replaceChild(index, replacement);
                return;
            case "DAY_OF_YEAR":
                replaceExtractFunction(parent, child, index, "'%j'");
                return;
            case "DAY_OF_WEEK":
                replaceExtractFunction(parent, child, index, "'%w'");
                return;
            case "WEEK":
                replaceExtractFunction(parent, child, index, "'%W'");
                return;
            case "YEAR":
                replaceExtractFunction(parent, child, index, "'%Y'");
                return;
            case "MONTH":
                replaceExtractFunction(parent, child, index, "'%m'");
                return;
            case "DAY":
            case "DAY_OF_MONTH":
                replaceExtractFunction(parent, child, index, "'%d'");
                return;
            case "HOUR":
                replaceExtractFunction(parent, child, index, "'%H'");
                return;
            case "MINUTE":
                replaceExtractFunction(parent, child, index, "'%M'");
                return;
            case "SECOND":
                replaceExtractFunction(parent, child, index, "'%S'");
                return;

            case "SUBSTRING":
                replacement = new FunctionNode("SUBSTR", child.getAlias(), true);
                break;
            case "CONCAT":
                replacement = new OpExpressionNode("||");
                break;
            case "MOD":
                replacement = new OpExpressionNode("%");
                break;
            case "CURRENT_DATE":
            case "CURRENT_TIMESTAMP":
            case "CURRENT_TIME":
                replacement = new FunctionNode(functionName, child.getAlias(), false);
                break;
        }

        if(replacement != null) {
            replaceChild(parent, index, replacement);
        }
    }

    private void replaceExtractFunction(Node parent, FunctionNode original, int index, String format) {
        Node replacement = new FunctionNode("cast", original.getAlias(), true) {
            @Override
            public void appendChildrenSeparator(QuotingAppendable buffer, int childIdx) {
                buffer.append(" as ");
            }
        };

        FunctionNode strftime = new FunctionNode("strftime", null, true);
        strftime.addChild(new TextNode(format));
        strftime.addChild(original.getChild(0));
        replacement.addChild(strftime);
        replacement.addChild(new TextNode("integer"));

        parent.replaceChild(index, replacement);
    }
}
