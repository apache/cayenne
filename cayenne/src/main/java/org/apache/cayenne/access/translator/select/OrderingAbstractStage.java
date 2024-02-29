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

package org.apache.cayenne.access.translator.select;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.cayenne.access.sqlbuilder.NodeBuilder;
import org.apache.cayenne.access.sqlbuilder.QuotingAppendable;
import org.apache.cayenne.access.sqlbuilder.SQLGenerationContext;
import org.apache.cayenne.access.sqlbuilder.sqltree.AliasedNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.SimpleNodeTreeVisitor;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTAggregateFunctionCall;
import org.apache.cayenne.query.Ordering;

abstract class OrderingAbstractStage implements TranslationStage {

    protected void processOrdering(QualifierTranslator qualifierTranslator, TranslatorContext context, Ordering ordering) {
        Expression orderExp = ordering.getSortSpec();
        NodeBuilder nodeBuilder = node(qualifierTranslator.translate(orderExp));

        if(ordering.isCaseInsensitive()) {
            nodeBuilder = function("UPPER", nodeBuilder);
        }

        // If query is DISTINCT or GROUPING then we need to add the order column as a result column
        if (orderColumnAbsent(context, nodeBuilder)) {
            // deepCopy as some DB expect exactly the same expression in select and in ordering
            ResultNodeDescriptor descriptor = context.addResultNode(nodeBuilder.build().deepCopy());
            if(orderExp instanceof ASTAggregateFunctionCall) {
                descriptor.setAggregate(true);
            }
        }
    }

    private boolean orderColumnAbsent(TranslatorContext context, NodeBuilder nodeBuilder) 
    {
        OrderNodeVisitor orderVisitor = new OrderNodeVisitor();
        nodeBuilder.build().visit( orderVisitor );
        List<CharSequence> orderParts = orderVisitor.getParts();

        return context.getResultNodeList().stream()
            .noneMatch( result -> {
                ResultNodeVisitor resultVisitor = new ResultNodeVisitor(orderParts);
                // Visitor aborts as soon as there's a mismatch with orderParts
                result.getNode().visit(resultVisitor);
                return resultVisitor.matches();
            });
    }

    private class OrderNodeVisitor extends AppendableVisitor // see below
    {
        @Override
        public boolean onNodeStart(Node node) {
            node.append( this );
            node.appendChildrenStart(this);
            return true;
        }

        @Override
        public void onChildNodeEnd(Node parent, Node child, int index, boolean hasMore) {
            if (hasMore && parent != null) {
                parent.appendChildrenSeparator(this, index);
            }
        }

        @Override
        public void onNodeEnd(Node node) {
            node.appendChildrenEnd(this);
        }

        List<CharSequence> getParts() {
            return Collections.unmodifiableList(partList);
        }
    }

    private class ResultNodeVisitor extends AppendableVisitor // see below
    {
        private List<CharSequence> orderItemParts;
        private boolean itemsMatch = true;
        private int lastIndex = 0;

        ResultNodeVisitor(List<CharSequence> orderParts) {
            orderItemParts = orderParts;
        }

        @Override
        public boolean onNodeStart(Node node) {
            node.append(this);
            if (node instanceof ColumnNode && ((ColumnNode) node).getAlias() != null) {
                partList.removeLast(); // Remove appended alias
            }
            if (!(node.getParent() instanceof AliasedNode)) {
                // Prevent appending opening bracket
                node.appendChildrenStart(this);
            }
            return isEqualSoFar();
        }

        @Override
        public void onChildNodeEnd(Node parent, Node child, int index, boolean hasMore) {
            if (hasMore && parent != null) {
                parent.appendChildrenSeparator(this, index);
                isEqualSoFar();
            }
        }

        @Override
        public void onNodeEnd(Node node) {
            // Prevent appending alias or closing bracket
            if (!(node instanceof AliasedNode || node.getParent() instanceof AliasedNode)) {
                node.appendChildrenEnd(this);
                if (node instanceof FunctionNode && ((FunctionNode) node).getAlias() != null) {
                    if (partList.getLast().equals(((FunctionNode) node).getAlias())) {
                        partList.removeLast(); // Remove appended alias
                    }
                }
                isEqualSoFar();
            }
        }

        private boolean isEqualSoFar() {
            int currentSize = partList.size();
            if (currentSize == lastIndex) return itemsMatch;
            if (currentSize > orderItemParts.size()) itemsMatch = false;
            if (itemsMatch) {
                // In reverse to fail fast by hopefully comparing column names first
                for (int x = currentSize-1; x >= lastIndex; x--) {
                    if (!partList.get(x).equals(orderItemParts.get(x))) {
                        itemsMatch = false;
                        break;
                    }
                }
            }
            lastIndex = partList.size();
            return itemsMatch;
        }

        boolean matches() {
            return isEqualSoFar();
        }
    }

    private class AppendableVisitor extends SimpleNodeTreeVisitor implements QuotingAppendable
    {
        protected final LinkedList<CharSequence> partList = new LinkedList<>();

        @Override
        public QuotingAppendable append(CharSequence csq) {
            partList.add(csq);
            return this;
        }

        @Override
        public QuotingAppendable append(CharSequence csq, int start, int end) {
            return this;
        }

        @Override
        public QuotingAppendable append(char c) {
            if (c != '.' && c != ' ') partList.add(String.valueOf(c));
            return this;
        }

        @Override
        public QuotingAppendable append(int c) {
            return this;
        }

        @Override
        public QuotingAppendable appendQuoted(CharSequence csq) {
            partList.add(csq);
            return this;
        }

        @Override
        public SQLGenerationContext getContext() {
            return null;
        }

        @Override
        public String toString() {
            return partList.stream().collect( Collectors.joining("|") );
        }
    }
}
