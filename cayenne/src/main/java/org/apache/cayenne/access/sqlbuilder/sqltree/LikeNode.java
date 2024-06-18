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

import org.apache.cayenne.access.sqlbuilder.QuotingAppendable;

import java.util.Objects;

/**
 * expressions: LIKE, ILIKE, NOT LIKE, NOT ILIKE + ESCAPE
 *
 * @since 4.2
 */
public class LikeNode extends ExpressionNode {

    protected final boolean ignoreCase;
    protected final boolean not;
    protected final char escape;

    public LikeNode(boolean ignoreCase, boolean not, char escape) {
        super(NodeType.LIKE);
        this.ignoreCase = ignoreCase;
        this.not = not;
        this.escape = escape;
    }

    @Override
    public void appendChildrenStart(QuotingAppendable buffer) {
        if(ignoreCase) {
            buffer.append(" UPPER(");
        }
    }

    @Override
    public void appendChildrenSeparator(QuotingAppendable buffer, int childIdx) {
        if(ignoreCase) {
            buffer.append(')');
        }
        if(not) {
            buffer.append(" NOT");
        }
        buffer.append(" LIKE");
        if(ignoreCase) {
            buffer.append(" UPPER(");
        }
    }

    @Override
    public void appendChildrenEnd(QuotingAppendable buffer) {
        if(ignoreCase) {
            buffer.append(')');
        }
        if(escape != 0) {
            buffer.append(" ESCAPE '").append(escape).append('\'');
        }
    }

    @Override
    public Node copy() {
        return new LikeNode(ignoreCase, not, escape);
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public boolean isNot() {
        return not;
    }

    public char getEscape() {
        return escape;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        LikeNode likeNode = (LikeNode) o;
        return ignoreCase == likeNode.ignoreCase && not == likeNode.not && escape == likeNode.escape;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ignoreCase, not, escape);
    }
}
