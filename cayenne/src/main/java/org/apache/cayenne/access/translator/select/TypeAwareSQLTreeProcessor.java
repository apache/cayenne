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

import java.sql.Types;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.access.sqlbuilder.sqltree.ChildProcessor;
import org.apache.cayenne.access.sqlbuilder.sqltree.ColumnNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.NodeType;
import org.apache.cayenne.access.sqlbuilder.sqltree.PerAttributeChildProcessor;
import org.apache.cayenne.access.sqlbuilder.sqltree.SQLTreeProcessor;
import org.apache.cayenne.access.sqlbuilder.sqltree.SimpleNodeTreeVisitor;
import org.apache.cayenne.access.sqlbuilder.sqltree.ValueNode;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledAttribute;
import org.apache.cayenne.map.compiled.CompiledEntity;

/**
 * @since 4.2
 */
public class TypeAwareSQLTreeProcessor extends SimpleNodeTreeVisitor implements SQLTreeProcessor {

    protected static final Class<?> DEFAULT_TYPE = DefaultColumnTypeMarker.class;
    protected static final String DEFAULT_TYPE_NAME = DEFAULT_TYPE.getName();

    protected final Map<String, ChildProcessor<ColumnNode>> byColumnTypeProcessors = new HashMap<>();
    protected final Map<String, ChildProcessor<ValueNode>> byValueTypeProcessors = new HashMap<>();
    protected final Map<NodeType, ChildProcessor<Node>> byNodeTypeProcessors = new EnumMap<>(NodeType.class);

    public TypeAwareSQLTreeProcessor() {
        registerProcessor(NodeType.COLUMN, new PerAttributeChildProcessor<>(this::getColumnAttribute, this::getColumnProcessor));
        registerProcessor(NodeType.VALUE, new PerAttributeChildProcessor<>(this::getValueAttribute, this::getValueProcessor));
    }

    @Override
    public Node process(Node node) {
        node.visit(this);
        return node;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void registerProcessor(NodeType nodeType, ChildProcessor childProcessor) {
        byNodeTypeProcessors.put(nodeType, childProcessor);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void registerColumnProcessor(Class<?> columnType, ChildProcessor childProcessor) {
        byColumnTypeProcessors.put(columnType.getName(), childProcessor);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void registerValueProcessor(Class<?> columnType, ChildProcessor childProcessor) {
        byValueTypeProcessors.put(columnType.getName(), childProcessor);
    }

    protected Optional<Node> defaultProcess(Node parent, Node child, int index) {
        return Optional.empty();
    }

    @Override
    public boolean onChildNodeStart(Node parent, Node child, int index, boolean hasMore) {
        byNodeTypeProcessors
                .getOrDefault(child.getType(), this::defaultProcess)
                .process(parent, child, index)
                .ifPresent(node -> replaceChild(parent, index, node));
        return true;
    }

    protected CompiledColumn getColumnAttribute(ColumnNode node) {
        CompiledColumn column = node.getAttribute();
        if(column != null
                && column.jdbcType() == Types.OTHER
                && node.getParent() != null
                && node.getParent().getType() == NodeType.RESULT) {
            return column;
        }
        return null;
    }

    protected ChildProcessor<ColumnNode> getColumnProcessor(CompiledColumn column) {
        String type = getObjAttributeFor(column).map(CompiledAttribute::valueClassName).orElse(DEFAULT_TYPE_NAME);
        return byColumnTypeProcessors
                .getOrDefault(type, this::defaultProcess);
    }

    protected CompiledColumn getValueAttribute(ValueNode node) {
        CompiledColumn column = node.getAttribute();
        if(column == null) {
            return null;
        }
        if(column.jdbcType() == Types.OTHER
                && node.getParent() != null
                && (node.getParent().getType() == NodeType.EQUALITY || node.getParent().getType() == NodeType.INSERT_VALUES)) {
            return column;
        }
        return null;
    }

    protected ChildProcessor<ValueNode> getValueProcessor(CompiledColumn column) {
        String type = getObjAttributeFor(column).map(CompiledAttribute::valueClassName).orElse(DEFAULT_TYPE_NAME);
        return byValueTypeProcessors
                .getOrDefault(type, this::defaultProcess);
    }

    protected static void replaceChild(Node parent, int index, Node newChild) {
        Node oldChild = parent.getChild(index);
        for (int i = 0; i < oldChild.getChildrenCount(); i++) {
            newChild.addChild(oldChild.getChild(i));
        }
        parent.replaceChild(index, newChild);
    }

    protected static Node wrapInFunction(Node node, String function) {
        FunctionNode functionNode = new FunctionNode(function, null);
        functionNode.addChild(node);
        return functionNode;
    }

    protected static Optional<CompiledAttribute> getObjAttributeFor(CompiledColumn column) {
        if(column == null) {
            return Optional.empty();
        }
        // only an entity of the column's table maps the column directly
        for (CompiledEntity entity : column.table().entities()) {
            CompiledAttribute attribute = entity.attributeForColumn(column);
            if(attribute != null) {
                return Optional.of(attribute);
            }
        }
        return Optional.empty();
    }

    private static class DefaultColumnTypeMarker {
    }
}
