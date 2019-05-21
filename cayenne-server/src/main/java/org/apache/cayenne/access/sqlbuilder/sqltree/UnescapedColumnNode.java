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
import org.apache.cayenne.map.DbAttribute;

/**
 * @since 4.2
 */
public class UnescapedColumnNode extends ColumnNode {

    public UnescapedColumnNode(String table, String column, String alias, DbAttribute attribute) {
        super(table, column, alias, attribute);
    }

    @Override
    public QuotingAppendable append(QuotingAppendable buffer) {
        buffer.append(' ');
        if (table != null) {
            buffer.append(table).append('.');
        }
        buffer.append(column);
        if (alias != null) {
            buffer.append(' ').appendQuoted(alias);
        }

        return buffer;
    }

    @Override
    public Node copy() {
        return new UnescapedColumnNode(table, column, alias, attribute);
    }
}
