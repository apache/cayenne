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

package org.apache.cayenne.dba.postgres;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.jdbc.BatchAction;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

public class PostgresBatchAction extends BatchAction {

    /**
     * @since 4.0
     */
    public PostgresBatchAction(BatchQuery query, DataNode dataNode, boolean runningAsBatch) {
        super(query, dataNode, runningAsBatch);
    }

    @Override
    protected PreparedStatement prepareStatement(Connection connection, String queryStr,
                                                 DbAdapter adapter, boolean generatedKeys) throws SQLException {
        if (generatedKeys) {
            Collection<DbAttribute> generatedAttributes = query.getDbEntity().getGeneratedAttributes();
            String[] generatedPKColumns = new String[generatedAttributes.size()];

            int i = 0;
            for (DbAttribute generatedAttribute : generatedAttributes) {
                if (generatedAttribute.isPrimaryKey()) {
                    generatedPKColumns[i++] = generatedAttribute.getName().toLowerCase();
                }
            }

            return connection.prepareStatement(queryStr, Arrays.copyOf(generatedPKColumns, i));
        }

        return connection.prepareStatement(queryStr);
    }

}
