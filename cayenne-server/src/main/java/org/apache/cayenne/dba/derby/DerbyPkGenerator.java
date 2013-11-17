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

package org.apache.cayenne.dba.derby;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DbEntity;

/**
 * Default PK generator for Derby that uses updateable ResultSet to get the next id from
 * the lookup table.
 * 
 * @since 1.2
 */
public class DerbyPkGenerator extends JdbcPkGenerator {

    DerbyPkGenerator(JdbcAdapter adapter) {
        super(adapter);
    }

    static final String SELECT_QUERY = "SELECT NEXT_ID FROM AUTO_PK_SUPPORT"
            + " WHERE TABLE_NAME = ? FOR UPDATE";

    /**
     * @since 3.0
     */
    @Override
    protected long longPkFromDatabase(DataNode node, DbEntity entity) throws Exception {

        JdbcEventLogger logger = adapter.getJdbcEventLogger();
        if (logger.isLoggable()) {
            logger.logQuery(SELECT_QUERY, Collections
                    .singletonList(entity.getName()));
        }

        Connection c = node.getDataSource().getConnection();
        try {
            PreparedStatement select = c.prepareStatement(
                    SELECT_QUERY,
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_UPDATABLE);
            try {
                select.setString(1, entity.getName());
                ResultSet rs = select.executeQuery();
    
                try {
                    if (!rs.next()) {
                        throw new CayenneException("PK lookup failed for table: "
                                + entity.getName());
                    }
        
                    long nextId = rs.getLong(1);
        
                    rs.updateLong(1, nextId + pkCacheSize);
                    rs.updateRow();
        
                    if (rs.next()) {
                        throw new CayenneException("More than one PK record for table: "
                                + entity.getName());
                    }
                    
                    c.commit();

                    return nextId;
                }
                finally {
                    rs.close();
                }
            }
            finally {
                select.close();
            }
        }
        finally {
            c.close();
        }
    }
}
