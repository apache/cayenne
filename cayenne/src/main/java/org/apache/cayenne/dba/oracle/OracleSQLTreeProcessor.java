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

package org.apache.cayenne.dba.oracle;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.access.sqlbuilder.ExpressionNodeBuilder;
import org.apache.cayenne.access.sqlbuilder.QuotingAppendable;
import org.apache.cayenne.access.sqlbuilder.SelectBuilder;
import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.EmptyNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.InNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.LimitOffsetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeType;
import org.apache.cayenne.access.sqlbuilder.sqltree.OpExpressionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.TextNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ValueNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.TrimmingColumnNode;
import org.apache.cayenne.access.translator.select.BaseSQLTreeProcessor;
import org.apache.cayenne.util.ArrayUtil;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;

/**
 * @since 4.2
 */
public class OracleSQLTreeProcessor extends BaseSQLTreeProcessor {

    private static final int ORACLE_IN_BATCH_SIZE = 1000;

    private SelectBuilder selectBuilder;

    private Node root;

    @Override
    protected void onResultNode(Node parent, Node child, int index) {
        for(int i=0; i<child.getChildrenCount(); i++) {
            child.replaceChild(i, aliased(child.getChild(i), "c" + i).build());
        }
    }

    @Override
    protected void onColumnNode(Node parent, ColumnNode child, int index) {
        replaceChild(parent, index, new TrimmingColumnNode(child));
    }

    @Override
    protected void onLimitOffsetNode(Node parent, LimitOffsetNode child, int index) {
        if(child.getLimit() > 0 || child.getOffset() > 0) {
            int limit = child.getLimit();
            int offset = child.getOffset();
            int max = (limit <= 0) ? Integer.MAX_VALUE : limit + offset;

            /*
             Transform query with limit/offset into following form:
             SELECT * FROM (
                SELECT tid.*, ROWNUM rnum
                FROM ( MAIN_QUERY ) tid
                WHERE ROWNUM <= OFFSET + LIMIT
             ) WHERE rnum > OFFSET
             */
            selectBuilder = select(all())
                    .from(select(text(" tid.*"), text(" ROWNUM rnum")) // using text not column to avoid quoting
                            .from(aliased(() -> root, "tid"))
                            .where(exp(text(" ROWNUM")).lte(value(max))))
                    .where(exp(text(" rnum")).gt(value(offset)));
        }
        parent.replaceChild(index, new EmptyNode());
    }

    @Override
    protected void onInNode(Node parent, InNode child, int index) {
        boolean not = child.isNot();
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

        // need to slice for batches of 1000 values
        if(value instanceof Object[]) {
            for(Object[] slice : ArrayUtil.sliceArray((Object[])value, ORACLE_IN_BATCH_SIZE)) {
                newChildren.add(newSliceNode(child, arg, valueNode, slice));
            }
        } else if(value instanceof int[]) {
            for(int[] slice : ArrayUtil.sliceArray((int[])value, ORACLE_IN_BATCH_SIZE)) {
                newChildren.add(newSliceNode(child, arg, valueNode, slice));
            }
        } else if(value instanceof long[]) {
            for(long[] slice : ArrayUtil.sliceArray((long[])value, ORACLE_IN_BATCH_SIZE)) {
                newChildren.add(newSliceNode(child, arg, valueNode, slice));
            }
        } else if(value instanceof float[]) {
            for(float[] slice : ArrayUtil.sliceArray((float[])value, ORACLE_IN_BATCH_SIZE)) {
                newChildren.add(newSliceNode(child, arg, valueNode, slice));
            }
        } else if(value instanceof double[]) {
            for(double[] slice : ArrayUtil.sliceArray((double[])value, ORACLE_IN_BATCH_SIZE)) {
                newChildren.add(newSliceNode(child, arg, valueNode, slice));
            }
        } else if(value instanceof short[]) {
            for(short[] slice : ArrayUtil.sliceArray((short[])value, ORACLE_IN_BATCH_SIZE)) {
                newChildren.add(newSliceNode(child, arg, valueNode, slice));
            }
        } else if(value instanceof char[]) {
            for(char[] slice : ArrayUtil.sliceArray((char[])value, ORACLE_IN_BATCH_SIZE)) {
                newChildren.add(newSliceNode(child, arg, valueNode, slice));
            }
        } else if(value instanceof boolean[]) {
            for(boolean[] slice : ArrayUtil.sliceArray((boolean[])value, ORACLE_IN_BATCH_SIZE)) {
                newChildren.add(newSliceNode(child, arg, valueNode, slice));
            }
        } else if(value instanceof byte[]) {
            for(byte[] slice : ArrayUtil.sliceArray((byte[])value, ORACLE_IN_BATCH_SIZE)) {
                newChildren.add(newSliceNode(child, arg, valueNode, slice));
            }
        }

        ExpressionNodeBuilder exp = exp(node(newChildren.get(0)));
        for(int i=1; i<newChildren.size(); i++) {
            if(not) {
                exp = exp.and(node(newChildren.get(i)));
            } else {
                exp = exp.or(node(newChildren.get(i)));
            }
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
        String functionName = child.getFunctionName();
        Node functionReplacement = null;
        switch (functionName) {
            case "LOCATE":
                functionReplacement = new FunctionNode("INSTR", child.getAlias(), true);
                for(int i=0; i<=1; i++) {
                    functionReplacement.addChild(child.getChild(1-i));
                }
                parent.replaceChild(index, functionReplacement);
                return;

            case "DAY_OF_YEAR":
            case "DAY_OF_WEEK":
            case "WEEK":
                functionReplacement = new FunctionNode("TO_CHAR", child.getAlias(), true);
                functionReplacement.addChild(child.getChild(0));
                if("DAY_OF_YEAR".equals(functionName)) {
                    functionName = "'DDD'";
                } else if("DAY_OF_WEEK".equals(functionName)) {
                    functionName = "'D'";
                } else {
                    functionName = "'IW'";
                }
                functionReplacement.addChild(new TextNode(functionName));
                parent.replaceChild(index, functionReplacement);
                return;

            case "SUBSTRING":
                functionReplacement = new FunctionNode("SUBSTR", child.getAlias(), true);
                break;
            case "CONCAT":
                functionReplacement = new OpExpressionNode("||");
                break;
            case "CURRENT_TIMESTAMP":
            case "CURRENT_DATE":
                functionReplacement = new FunctionNode(functionName, child.getAlias(), false);
                break;
            case "CURRENT_TIME":
                functionReplacement = new FunctionNode("{fn CURTIME()}", child.getAlias(), false);
                break;
            case "YEAR":
            case "MONTH":
            case "DAY":
            case "DAY_OF_MONTH":
            case "HOUR":
            case "MINUTE":
            case "SECOND":
                functionReplacement = new FunctionNode("EXTRACT", child.getAlias(), true) {
                    @Override
                    public void appendChildrenSeparator(QuotingAppendable buffer, int childIdx) {
                        buffer.append(' ');
                    }
                };
                if("DAY_OF_MONTH".equals(functionName)) {
                    functionName = "DAY";
                }
                functionReplacement.addChild(new TextNode(functionName + " FROM "));
                break;
        }

        if(functionReplacement != null) {
            replaceChild(parent, index, functionReplacement);
        }
    }

    @Override
    public Node process(Node node) {
        root = node;
        super.process(node);
        if(selectBuilder != null) {
            return selectBuilder.build();
        }
        return node;
    }
}
