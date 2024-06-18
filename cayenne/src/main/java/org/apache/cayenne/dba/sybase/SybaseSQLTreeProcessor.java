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

package org.apache.cayenne.dba.sybase;

import org.apache.cayenne.access.sqlbuilder.sqltree.EmptyNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeType;
import org.apache.cayenne.access.sqlbuilder.sqltree.OpExpressionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.TextNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.TopNode;
import org.apache.cayenne.access.translator.select.BaseSQLTreeProcessor;

/**
 * @since 4.2
 */
public class SybaseSQLTreeProcessor extends BaseSQLTreeProcessor {

    @Override
    protected void onLimitOffsetNode(Node parent, LimitOffsetNode child, int index) {
        // SQLServer uses "SELECT DISTINCT TOP N" or "SELECT TOP N" instead of LIMIT
        // Offset will be calculated in-memory
        replaceChild(parent, index, new EmptyNode(), false);
        if(child.getLimit() > 0) {
            int limit = child.getLimit() + child.getOffset();
            // we have root actually as input for processor, but it's better to keep processor stateless
            // root shouldn't be far from limit's parent (likely it will be parent itself)
            Node root = getRoot(parent);
            int idx = 0;
            if(root.getChild(0).getType() == NodeType.DISTINCT) {
                idx = 1;
            }
            root.addChild(idx, new TopNode(limit));
        }
    }

    private Node getRoot(Node node) {
        while(node.getParent() != null) {
            node = node.getParent();
        }
        return node;
    }

    @Override
    protected void onFunctionNode(Node parent, FunctionNode child, int index) {
        String functionName = child.getFunctionName();
        Node replacement = null;
        switch (functionName) {
            case "LENGTH":
                replacement = new FunctionNode("LEN", child.getAlias(), true);
                break;
            case "LOCATE":
                replacement = new FunctionNode("CHARINDEX", child.getAlias(), true);
                break;
            case "MOD":
                replacement = new OpExpressionNode("%");
                break;
            case "TRIM":
                Node rtrim = new FunctionNode("RTRIM", null, true);
                replacement = new FunctionNode("LTRIM", child.getAlias(), true);
                for(int i=0; i<child.getChildrenCount(); i++) {
                    rtrim.addChild(child.getChild(i));
                }
                replacement.addChild(rtrim);
                parent.replaceChild(index, replacement);
                return;
            case "CURRENT_DATE":
                replacement = new FunctionNode("{fn CURDATE()}", child.getAlias(), false);
                break;
            case "CURRENT_TIME":
                replacement = new FunctionNode("{fn CURTIME()}", child.getAlias(), false);
                break;
            case "CURRENT_TIMESTAMP":
                replacement = new FunctionNode("CURRENT_TIMESTAMP", child.getAlias(), false);
                break;

            case "YEAR":
            case "MONTH":
            case "WEEK":
            case "DAY_OF_YEAR":
            case "DAY":
            case "DAY_OF_MONTH":
            case "DAY_OF_WEEK":
            case "HOUR":
            case "MINUTE":
            case "SECOND":
                replacement = new FunctionNode("DATEPART", child.getAlias(), true);
                switch (functionName) {
                    case "DAY_OF_MONTH":
                        functionName = "DAY";
                        break;
                    case "DAY_OF_WEEK":
                        functionName = "WEEKDAY";
                        break;
                    case "DAY_OF_YEAR":
                        functionName = "DAYOFYEAR";
                        break;
                }
                replacement.addChild(new TextNode(functionName));
                break;
        }

        if(replacement != null) {
            replaceChild(parent, index, replacement);
        }
    }

}
