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

package org.apache.cayenne.dba.oracle;

import java.sql.PreparedStatement;

import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.access.trans.SelectTranslator;

/**
 * Select translator that implements Oracle-specific optimizations.
 * 
 */
class OracleSelectTranslator extends SelectTranslator {

    @Override
    protected void appendLimitAndOffsetClauses(StringBuilder buffer) {
        int offset = queryMetadata.getFetchOffset();
        int limit = queryMetadata.getFetchLimit();

        if (limit > 0 || offset > 0) {
            int max = (limit <= 0) ? Integer.MAX_VALUE : limit + offset;

            buffer.insert(0, "select * from ( select tid.*, ROWNUM rnum from (");
            buffer
                    .append(") tid where ROWNUM <=")
                    .append(max)
                    .append(") where rnum  > ")
                    .append(offset);
        }
    }

    /**
     * Translates internal query into PreparedStatement, applying Oracle optimizations if
     * possible.
     */
    @Override
    public PreparedStatement createStatement() throws Exception {
        String sqlStr = createSqlString();
        QueryLogger.logQuery(sqlStr, values);
        PreparedStatement stmt = connection.prepareStatement(sqlStr);

        initStatement(stmt);

        return stmt;
    }
}
