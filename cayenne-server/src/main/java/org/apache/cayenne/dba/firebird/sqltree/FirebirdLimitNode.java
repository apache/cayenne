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

package org.apache.cayenne.dba.firebird.sqltree;

import org.apache.cayenne.access.sqlbuilder.QuotingAppendable;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;

/**
 * @since 4.2
 */
public class FirebirdLimitNode extends Node {
    private final int from;
    private final int to;

    public FirebirdLimitNode(int from, int to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public QuotingAppendable append(QuotingAppendable buffer) {
        buffer.append(" ROWS ");
        if(from > 0) {
            buffer.append(from).append(" TO ");
        }
        return buffer.append(to);
    }

    @Override
    public Node copy() {
        return new FirebirdLimitNode(from, to);
    }
}
