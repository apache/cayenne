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

package org.apache.cayenne.dba.postgres;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.dba.oracle.OraclePkGenerator;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbKeyGenerator;

/**
 * Default PK generator for PostgreSQL that uses sequences for PK generation.
 */
public class PostgresPkGenerator extends OraclePkGenerator {

    @Override
    protected String createSequenceString(DbEntity ent) {
        // note that PostgreSQL 7.4 and newer supports INCREMENT BY and START WITH
        // however 7.3 doesn't like BY and WITH, so using older more neutral syntax
        // that works with all tested versions.
        StringBuffer buf = new StringBuffer();
        buf
                .append("CREATE SEQUENCE ")
                .append(sequenceName(ent))
                .append(" INCREMENT ")
                .append(pkCacheSize(ent))
                .append(" START 200");
        return buf.toString();
    }

    /**
     * Generates primary key by calling Oracle sequence corresponding to the
     * <code>dbEntity</code>. Executed SQL looks like this:
     * 
     * <pre>
     *     SELECT nextval(pk_table_name)
     * </pre>
     */
    @Override
    protected int pkFromDatabase(DataNode node, DbEntity ent) throws Exception {

        DbKeyGenerator pkGenerator = ent.getPrimaryKeyGenerator();
        String pkGeneratingSequenceName;
        if (pkGenerator != null
                && DbKeyGenerator.ORACLE_TYPE.equals(pkGenerator.getGeneratorType())
                && pkGenerator.getGeneratorName() != null)
            pkGeneratingSequenceName = pkGenerator.getGeneratorName();
        else
            pkGeneratingSequenceName = sequenceName(ent);

        Connection con = node.getDataSource().getConnection();
        try {
            Statement st = con.createStatement();
            try {
                String sql = "SELECT nextval('" + pkGeneratingSequenceName + "')";
                QueryLogger.logQuery(sql, Collections.EMPTY_LIST);
                ResultSet rs = st.executeQuery(sql);
                try {
                    // Object pk = null;
                    if (!rs.next()) {
                        throw new CayenneRuntimeException(
                                "Error generating pk for DbEntity " + ent.getName());
                    }
                    return rs.getInt(1);
                }
                finally {
                    rs.close();
                }
            }
            finally {
                st.close();
            }
        }
        finally {
            con.close();
        }
    }

    /**
     * Fetches a list of existing sequences that might match Cayenne generated ones.
     */
    @Override
    protected List<String> getExistingSequences(DataNode node) throws SQLException {

        // check existing sequences
        Connection con = node.getDataSource().getConnection();

        try {
            Statement sel = con.createStatement();
            try {
                String sql = "SELECT relname FROM pg_class WHERE relkind='S'";
                QueryLogger.logQuery(sql, Collections.EMPTY_LIST);
                ResultSet rs = sel.executeQuery(sql);
                try {
                    List<String> sequenceList = new ArrayList<String>();
                    while (rs.next()) {
                        sequenceList.add(rs.getString(1));
                    }
                    return sequenceList;
                }
                finally {
                    rs.close();
                }
            }
            finally {
                sel.close();
            }
        }
        finally {
            con.close();
        }
    }
}
