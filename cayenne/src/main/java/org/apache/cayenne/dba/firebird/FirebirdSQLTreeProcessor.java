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

package org.apache.cayenne.dba.firebird;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.access.sqlbuilder.ExpressionNodeBuilder;
import org.apache.cayenne.access.sqlbuilder.QuotingAppendable;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.InNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeType;
import org.apache.cayenne.access.sqlbuilder.sqltree.OpExpressionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.TextNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ValueNode;
import org.apache.cayenne.access.translator.select.BaseSQLTreeProcessor;
import org.apache.cayenne.dba.firebird.sqltree.FirebirdLimitNode;
import org.apache.cayenne.dba.firebird.sqltree.FirebirdSubstringFunctionNode;
import org.apache.cayenne.util.ArrayUtil;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.exp;
import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.node;

/**
 * @since 4.2
 */
public class FirebirdSQLTreeProcessor extends BaseSQLTreeProcessor {

    private static final int FIREBIRD_IN_BATCH_SIZE = 1500;

    @Override
    protected void onValueNode(Node parent, ValueNode child, int index) {
        replaceChild(parent, index, new ValueNode(child.getValue(), child.isArray(), child.getAttribute(), child.isNeedBinding()) {
            @Override
            protected void appendStringValue(QuotingAppendable buffer, CharSequence value) {
                buffer.append("CAST(");
                super.appendStringValue(buffer, value);
                buffer.append(" AS VARCHAR(").append(Math.max(1,value.length())).append("))");
            }
        });
    }

    @Override
    protected void onLimitOffsetNode(Node parent, LimitOffsetNode child, int index) {
        if(child.getLimit() == 0 && child.getOffset() == 0) {
            return;
        }
        int from = child.getOffset() + 1;
        int to = child.getLimit() == 0 ? Integer.MAX_VALUE : from + child.getLimit();
        replaceChild(parent, index, new FirebirdLimitNode(from, to));
    }

    @Override
    protected void onInNode(Node parent, InNode child, int index) {
        Node arg = child.getChild(0);
        Node childNode = child.getChild(1);
        if(childNode.getType() != NodeType.VALUE) {
            return;
        }

        ValueNode valueNode = (ValueNode)childNode;
        Object value = valueNode.getValue();
        if(!value.getClass().isArray()) {
            return;
        }

        List<Node> newChildren = new ArrayList<>();

        // need to slice for batches of 1500 values
        if(value instanceof Object[]) {
            for(Object[] slice : ArrayUtil.sliceArray((Object[])value, FIREBIRD_IN_BATCH_SIZE)) {
                newChildren.add(newSliceNode(child, arg, valueNode, slice));
            }
        } else if(value instanceof int[]) {
            for(int[] slice : ArrayUtil.sliceArray((int[])value, FIREBIRD_IN_BATCH_SIZE)) {
                newChildren.add(newSliceNode(child, arg, valueNode, slice));
            }
        } else if(value instanceof long[]) {
            for(long[] slice : ArrayUtil.sliceArray((long[])value, FIREBIRD_IN_BATCH_SIZE)) {
                newChildren.add(newSliceNode(child, arg, valueNode, slice));
            }
        } else if(value instanceof float[]) {
            for(float[] slice : ArrayUtil.sliceArray((float[])value, FIREBIRD_IN_BATCH_SIZE)) {
                newChildren.add(newSliceNode(child, arg, valueNode, slice));
            }
        } else if(value instanceof double[]) {
            for(double[] slice : ArrayUtil.sliceArray((double[])value, FIREBIRD_IN_BATCH_SIZE)) {
                newChildren.add(newSliceNode(child, arg, valueNode, slice));
            }
        } else if(value instanceof short[]) {
            for(short[] slice : ArrayUtil.sliceArray((short[])value, FIREBIRD_IN_BATCH_SIZE)) {
                newChildren.add(newSliceNode(child, arg, valueNode, slice));
            }
        } else if(value instanceof char[]) {
            for(char[] slice : ArrayUtil.sliceArray((char[])value, FIREBIRD_IN_BATCH_SIZE)) {
                newChildren.add(newSliceNode(child, arg, valueNode, slice));
            }
        } else if(value instanceof boolean[]) {
            for(boolean[] slice : ArrayUtil.sliceArray((boolean[])value, FIREBIRD_IN_BATCH_SIZE)) {
                newChildren.add(newSliceNode(child, arg, valueNode, slice));
            }
        } else if(value instanceof byte[]) {
            for(byte[] slice : ArrayUtil.sliceArray((byte[])value, FIREBIRD_IN_BATCH_SIZE)) {
                newChildren.add(newSliceNode(child, arg, valueNode, slice));
            }
        }

        ExpressionNodeBuilder exp = exp(node(newChildren.get(0)));
        for(int i=1; i<newChildren.size(); i++) {
            exp = exp.or(node(newChildren.get(i)));
        }
        parent.replaceChild(index, exp.build());
    }

    private InNode newSliceNode(InNode child, Node arg, ValueNode valueNode, Object slice) {
        InNode nextNode = new InNode(child.isNot());
        nextNode.addChild(arg.deepCopy());
        nextNode.addChild(new ValueNode(slice, valueNode.isArray(), valueNode.getAttribute(), valueNode.isNeedBinding()));
        return nextNode;
    }

    @Override
    protected void onFunctionNode(Node parent, FunctionNode child, int index) {
        switch (child.getFunctionName()) {
            case "LENGTH":
                replaceChild(parent, index, new FunctionNode("CHAR_LENGTH", child.getAlias()));
                break;
            case "LOCATE":
                replaceChild(parent, index, new FunctionNode("POSITION", child.getAlias()));
                break;
            case "CONCAT":
                replaceChild(parent, index, new OpExpressionNode("||"));
                break;

            case "SUBSTRING":
                replaceChild(parent, index, new FirebirdSubstringFunctionNode(child.getAlias()));
                break;

            case "YEAR":
            case "MONTH":
            case "DAY":
            case "DAY_OF_MONTH":
            case "DAY_OF_WEEK":
            case "DAY_OF_YEAR":
            case "WEEK":
            case "HOUR":
            case "MINUTE":
            case "SECOND":
                Node functionReplacement = new FunctionNode("EXTRACT", child.getAlias(), true) {
                    @Override
                    public void appendChildrenSeparator(QuotingAppendable buffer, int childIdx) {
                        buffer.append(' ');
                    }
                };

                String partName = child.getFunctionName();
                if("DAY_OF_MONTH".equals(partName)) {
                    partName = "DAY";
                } else if("DAY_OF_WEEK".equals(partName)) {
                    partName = "WEEKDAY";
                } else if("DAY_OF_YEAR".equals(partName)) {
                    partName = "YEARDAY";
                }
                functionReplacement.addChild(new TextNode(partName + " FROM "));
                replaceChild(parent, index, functionReplacement);
                break;
        }
    }

}
