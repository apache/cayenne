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

package org.apache.cayenne.dba.ingres;

import org.apache.cayenne.access.sqlbuilder.QuotingAppendable;
import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.OffsetFetchNextNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.OpExpressionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.TrimmingColumnNode;
import org.apache.cayenne.access.translator.select.BaseSQLTreeProcessor;

/**
 * @since 4.2
 */
public class IngressSQLTreeProcessor extends BaseSQLTreeProcessor {

    @Override
    protected void onColumnNode(Node parent, ColumnNode child, int index) {
        replaceChild(parent, index, new TrimmingColumnNode(child));
    }

    @Override
    protected void onLimitOffsetNode(Node parent, LimitOffsetNode child, int index) {
        replaceChild(parent, index, new OffsetFetchNextNode(child) {
            @Override
            public QuotingAppendable append(QuotingAppendable buffer) {
                // OFFSET X FETCH NEXT Y ROWS ONLY
                if(offset > 0) {
                    buffer.append("OFFSET ").append(offset);
                }
                if(limit > 0) {
                    buffer.append("FETCH NEXT ").append(limit).append(" ROWS ONLY");
                }
                return buffer;
            }
        });
    }

    @Override
    protected void onFunctionNode(Node parent, FunctionNode child, int index) {
        switch (child.getFunctionName()) {
            case "CONCAT":
                replaceChild(parent, index, new OpExpressionNode("+"));
                return;
            case "LOCATE":
                Node child0 = child.getChild(0);
                child.replaceChild(0, child.getChild(1));
                child.replaceChild(1, child0);
                return;
            case "SUBSTRING":
                Node replacement = new FunctionNode("SUBSTRING", child.getAlias(), true) {
                    @Override
                    public void appendChildrenSeparator(QuotingAppendable buffer, int childIdx) {
                        // 0, CAST(1 AS INTEGER), CAST(2 AS INTEGER)
                        if(childIdx == 1 || childIdx == 2) {
                            buffer.append(" AS INTEGER)");
                        }
                        if(childIdx == 0 || childIdx == 1) {
                            buffer.append(", CAST(");
                        }
                    }

                    @Override
                    public void appendChildrenEnd(QuotingAppendable buffer) {
                        buffer.append(" AS INTEGER)");
                        super.appendChildrenEnd(buffer);
                    }
                };
                replaceChild(parent, index, replacement);
                return;
            case "TRIM":
                replaceChild(parent, index, new FunctionNode("RTRIM(LTRIM", child.getAlias(), true) {
                    @Override
                    public void appendChildrenEnd(QuotingAppendable buffer) {
                        buffer.append(')');
                        super.appendChildrenEnd(buffer);
                    }
                });
                return;
            case "DAY_OF_WEEK":
            case "DAY_OF_MONTH":
            case "DAY_OF_YEAR":
                // ingres variants are without '_'
                replaceChild(parent, index, new FunctionNode(child.getFunctionName().replace("_", ""), child.getAlias(), true));
                return;
        }
    }
}
