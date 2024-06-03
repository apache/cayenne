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
public class TopNode extends Node {

    private final int count;

    public TopNode(int count) {
        this.count = count;
    }

    @Override
    public QuotingAppendable append(QuotingAppendable buffer) {
        return buffer.append(" TOP ").append(count);
    }

    @Override
    public Node copy() {
        return new TopNode(count);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TopNode topNode = (TopNode) o;
        return count == topNode.count;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), count);
    }
}
