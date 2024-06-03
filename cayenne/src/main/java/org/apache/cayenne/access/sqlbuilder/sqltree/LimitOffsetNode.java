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
 * @since 4.2
 */
public class LimitOffsetNode extends Node {

    protected final int limit;
    protected final int offset;

    public LimitOffsetNode(int limit, int offset) {
        super(NodeType.LIMIT_OFFSET);
        this.limit = limit;
        this.offset = offset;
    }

    @Override
    public QuotingAppendable append(QuotingAppendable buffer) {
        if(limit == 0 && offset == 0) {
            return buffer;
        }
        return buffer.append(" LIMIT ").append(limit).append(" OFFSET ").append(offset);
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public Node copy() {
        return new LimitOffsetNode(limit, offset);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        LimitOffsetNode that = (LimitOffsetNode) o;
        return limit == that.limit && offset == that.offset;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), limit, offset);
    }
}
