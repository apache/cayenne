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

/**
 * @since 4.2
 */
public class OffsetFetchNextNode extends LimitOffsetNode {

    public OffsetFetchNextNode(LimitOffsetNode node) {
        super(node.getLimit(), node.getOffset());
    }

    private OffsetFetchNextNode(int limit, int offset) {
        super(limit, offset);
    }

    @Override
    public SQLAppendable append(SQLAppendable buffer) {
        // OFFSET X ROWS FETCH NEXT Y ROWS ONLY
        if(offset > 0) {
            buffer.appendTokenSeparator().append("OFFSET").appendTokenSeparator().append(offset).appendTokenSeparator().append("ROWS");
        }
        if(limit > 0) {
            buffer.appendTokenSeparator().append("FETCH NEXT").appendTokenSeparator().append(limit).appendTokenSeparator().append("ROWS ONLY");
        }
        return buffer;
    }

    @Override
    public Node copy() {
        return new OffsetFetchNextNode(limit, offset);
    }
}
