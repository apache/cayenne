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

import org.apache.cayenne.access.sqlbuilder.NodeTreeVisitor;
import org.apache.cayenne.access.sqlbuilder.QuotingAppendable;

import java.util.Objects;

/**
 * @since 4.2
 */
public class AliasedNode extends Node {

    protected final String alias;

    public AliasedNode(String alias) {
        this.alias = alias;
    }

    @Override
    public Node copy() {
        return new AliasedNode(alias);
    }

    @Override
    public QuotingAppendable append(QuotingAppendable buffer) {
        if(skipContent()) {
            buffer.append(' ').append(alias);
        }
        return buffer;
    }

    @Override
    public void visit(NodeTreeVisitor visitor) {
        if(skipContent()) {
            visitor.onNodeStart(this);
            visitor.onNodeEnd(this);
            return;
        }
        super.visit(visitor);
    }

    @Override
    public void appendChildrenEnd(QuotingAppendable buffer) {
        if(skipContent()){
            return;
        }
        buffer.append(" ").append(alias);
    }

    public String getAlias() {
        return alias;
    }

    private boolean skipContent() {
        // check if parent is of type RESULT
        Node parent = getParent();
        while(parent != null) {
            if(parent.getType() == NodeType.RESULT) {
                return false;
            }
            parent = parent.getParent();
        }

        // check if we have subselect as a child
        for(Node child : children) {
            if(child != null && child.getType() == NodeType.SELECT) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AliasedNode that = (AliasedNode) o;
        return Objects.equals(alias, that.alias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), alias);
    }
}
