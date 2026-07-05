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

package org.apache.cayenne.access.sqlbuilder.sqltree;

import org.apache.cayenne.access.sqlbuilder.SQLAppendable;
import org.apache.cayenne.access.sqlbuilder.SQLGenerationContext;

import java.sql.Types;
import java.util.Objects;

/**
 * @since 4.2
 */
public class TrimmingColumnNode extends Node {

    protected final ColumnNode columnNode ;

    public TrimmingColumnNode(ColumnNode columnNode) {
        this.columnNode = columnNode;
    }

    @Override
    public SQLAppendable append(SQLAppendable buffer, SQLGenerationContext context) {
        boolean isResult = isResultNode();
        if(columnNode.getAlias() == null || isResult) {
            if(isCharType() && isAllowedForTrimming()) {
                appendRtrim(buffer, context);
                appendAlias(buffer, isResult);
            } else if(isComparisonWithClob()) {
                appendClobColumnNode(buffer, context);
                appendAlias(buffer, isResult);
            } else {
                columnNode.append(buffer, context);
            }
        } else {
            appendAlias(buffer, false);
        }

        return buffer;
    }

    private boolean isComparisonWithClob() {
        if(isInsertOrUpdateSet()) {
            return false;
        }
        return (getParent().getType() == NodeType.EQUALITY
                || getParent().getType() == NodeType.LIKE)
                && columnNode.getAttribute() != null
                && columnNode.getAttribute().getType() == Types.CLOB;
    }

    protected void appendRtrim(SQLAppendable buffer, SQLGenerationContext context) {
        buffer.appendTokenSeparator().append("RTRIM(");
        appendColumnNode(buffer, context);
        buffer.append(")");
    }

    private boolean isCharType() {
        return columnNode.getAttribute() != null
                && columnNode.getAttribute().getType() == Types.CHAR;
    }

    protected boolean isAllowedForTrimming() {
        Node parent = getParent();
        while(parent != null) {
            if(parent.getType() == NodeType.JOIN
                    || parent.getType() == NodeType.FUNCTION
                    || parent.getType() == NodeType.UPDATE_SET
                    || parent.getType() == NodeType.INSERT_COLUMNS) {
                return false;
            }
            parent = parent.getParent();
        }
        return true;
    }

    protected boolean isResultNode() {
        return isParentOfType(NodeType.RESULT);
    }

    protected boolean isInsertOrUpdateSet() {
        return isParentOfType(NodeType.UPDATE_SET) || isParentOfType(NodeType.INSERT_COLUMNS);
    }

    protected boolean isParentOfType(NodeType nodeType) {
        Node parent = getParent();
        while(parent != null) {
            if(parent.getType() == nodeType) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    protected void appendClobColumnNode(SQLAppendable buffer, SQLGenerationContext context) {
        buffer.appendTokenSeparator().append("CAST(");
        appendColumnNode(buffer, context);
        buffer.appendTokenSeparator().append("AS").appendTokenSeparator().append("VARCHAR(").append(getColumnSize()).append("))");
    }

    protected void appendColumnNode(SQLAppendable buffer, SQLGenerationContext context) {
        // omit the table prefix for a single-table statement
        if (columnNode.getTable() != null && (context == null || !context.isSingleTableSQL())) {
            buffer.appendQuoted(columnNode.getTable()).append('.');
        }
        buffer.appendQuoted(columnNode.getColumn());
    }

    protected void appendAlias(SQLAppendable buffer, boolean isResult) {
        if(!isResult) {
            return;
        }
        if (columnNode.getAlias() != null) {
            buffer.appendTokenSeparator().appendQuoted(columnNode.getAlias());
        }
    }

    protected int getColumnSize() {
        int size = columnNode.getAttribute().getMaxLength();
        if(size > 0) {
            return size;
        }

        int siblings = getParent().getChildrenCount();
        for(int i=0; i<siblings; i++) {
            Node sibling = getParent().getChild(i);
            if(sibling == this) {
                continue;
            }
            if(sibling.getType() == NodeType.VALUE) {
                if(((ValueNode)sibling).getValue() instanceof CharSequence charSequence) {
                    int valLen = charSequence.length();
                    return Math.max(1, valLen);
                }
            }
        }

        return 255;
    }

    @Override
    public Node copy() {
        return new TrimmingColumnNode(columnNode.deepCopy());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TrimmingColumnNode that = (TrimmingColumnNode) o;
        return Objects.equals(columnNode, that.columnNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), columnNode);
    }
}
