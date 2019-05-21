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

package org.apache.cayenne.dba.openbase;

import org.apache.cayenne.access.sqlbuilder.QuotingAppendable;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LikeNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeType;
import org.apache.cayenne.access.sqlbuilder.sqltree.ValueNode;
import org.apache.cayenne.access.translator.select.BaseSQLTreeProcessor;

/**
 * @since 4.2
 * @deprecated since 4.2
 */
@Deprecated
public class OpenBaseSQLTreeProcessor extends BaseSQLTreeProcessor {

    @Override
    protected void onLikeNode(Node parent, LikeNode child, int index) {
        // OpenBase is case-insensitive by default
        if(child.isIgnoreCase()) {
            replaceChild(parent, index, new LikeNode(false, child.isNot(), child.getEscape()));
        }
    }

    @Override
    protected void onValueNode(Node parent, ValueNode child, int index) {
        // Special handling of string matching is needed:
        // Case-sensitive LIKE must be converted to [x][Y][z] format
        if(parent.getType() == NodeType.LIKE) {
            if(!((LikeNode)parent).isIgnoreCase() && child.getValue() instanceof CharSequence) {
                replaceChild(parent, index,
                        new ValueNode(caseSensitiveLikePattern((CharSequence)child.getValue()), child.isArray(), child.getAttribute()));
            }
        }
    }

    @Override
    protected void onLimitOffsetNode(Node parent, LimitOffsetNode child, int index) {
        replaceChild(parent, index, new OpenBaseLimitNode(child));
    }

    @Override
    protected void onFunctionNode(Node parent, FunctionNode child, int index) {
        switch (child.getFunctionName()) {
            case "DAY_OF_WEEK":
            case "DAY_OF_MONTH":
            case "DAY_OF_YEAR":
                replaceChild(parent, index, new FunctionNode(child.getFunctionName().replace("_", ""), child.getAlias()));
                break;
        }
    }

    private String caseSensitiveLikePattern(CharSequence pattern) {
        int len = pattern.length();
        StringBuilder buffer = new StringBuilder(len * 3);

        for (int i = 0; i < len; i++) {
            char c = pattern.charAt(i);
            if (c == '%' || c == '?') {
                buffer.append(c);
            } else {
                buffer.append("[").append(c).append("]");
            }
        }

        return buffer.toString();
    }

    private static class OpenBaseLimitNode extends Node {

        private final LimitOffsetNode child;

        public OpenBaseLimitNode(LimitOffsetNode child) {
            this.child = child;
        }

        @Override
        public QuotingAppendable append(QuotingAppendable buffer) {
            if(child.getLimit() > 0) {
                buffer.append(" RETURN RESULTS ").append(child.getLimit());
            }
            return buffer;
        }

        @Override
        public Node copy() {
            return new OpenBaseLimitNode(child);
        }
    }
}
