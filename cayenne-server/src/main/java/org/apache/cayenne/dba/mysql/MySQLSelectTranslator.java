/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.dba.mysql;

import java.sql.Connection;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.trans.SelectTranslator;
import org.apache.cayenne.query.Query;

/**
 * @since 1.2
 */
class MySQLSelectTranslator extends SelectTranslator {

    /**
     * @since 3.2
     */
    public MySQLSelectTranslator(Query query, DataNode dataNode, Connection connection) {
        super(query, dataNode, connection);
    }

    @Override
    protected void appendLimitAndOffsetClauses(StringBuilder buffer) {
        int offset = queryMetadata.getFetchOffset();
        int limit = queryMetadata.getFetchLimit();

        if (offset > 0 || limit > 0) {
            buffer.append(" LIMIT ");

            // both OFFSET and LIMIT must be present, so come up with defaults
            // if one of
            // them is not set by the user
            if (limit == 0) {
                limit = Integer.MAX_VALUE;
            }

            buffer.append(limit).append(" OFFSET ").append(offset);
        }
    }
}
