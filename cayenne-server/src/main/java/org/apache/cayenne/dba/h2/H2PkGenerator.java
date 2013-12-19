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

package org.apache.cayenne.dba.h2;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.oracle.OraclePkGenerator;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbKeyGenerator;

/**
 * Default PK generator for H2 that uses sequences for PK generation.
 * 
 * @since 3.2
 */
public class H2PkGenerator extends OraclePkGenerator {

    protected H2PkGenerator(JdbcAdapter adapter) {
        super(adapter);
    }

    @Override
    protected String createSequenceString(DbEntity ent) {
        StringBuilder buf = new StringBuilder();
        buf.append("CREATE SEQUENCE ").append(sequenceName(ent))
        	.append(" START WITH ").append(pkStartValue)
        	.append(" INCREMENT BY ").append(pkCacheSize(ent)).append(" CACHE 1");
        return buf.toString();
    }

    @Override
    protected long longPkFromDatabase(DataNode node, DbEntity entity) throws Exception {

        DbKeyGenerator pkGenerator = entity.getPrimaryKeyGenerator();
        String pkGeneratingSequenceName;
        if (pkGenerator != null && DbKeyGenerator.ORACLE_TYPE.equals(pkGenerator.getGeneratorType())
                && pkGenerator.getGeneratorName() != null)
            pkGeneratingSequenceName = pkGenerator.getGeneratorName();
        else
            pkGeneratingSequenceName = sequenceName(entity);

        Connection con = node.getDataSource().getConnection();
        try {
            Statement st = con.createStatement();
            try {
                String sql = "SELECT NEXT VALUE FOR " + pkGeneratingSequenceName;
                adapter.getJdbcEventLogger().logQuery(sql, Collections.EMPTY_LIST);
                ResultSet rs = st.executeQuery(sql);
                try {
                    // Object pk = null;
                    if (!rs.next()) {
                        throw new CayenneRuntimeException("Error generating pk for DbEntity " + entity.getName());
                    }
                    return rs.getLong(1);
                } finally {
                    rs.close();
                }
            } finally {
                st.close();
            }
        } finally {
            con.close();
        }

    }

    /**
     * Fetches a list of existing sequences that might match Cayenne generated
     * ones.
     */
    @Override
    protected List<String> getExistingSequences(DataNode node) throws SQLException {

        // check existing sequences
        Connection con = node.getDataSource().getConnection();

        try {
            Statement sel = con.createStatement();
            try {
                String sql = "SELECT LOWER(sequence_name) FROM Information_Schema.Sequences";
                adapter.getJdbcEventLogger().logQuery(sql, Collections.EMPTY_LIST);
                ResultSet rs = sel.executeQuery(sql);
                try {
                    List<String> sequenceList = new ArrayList<String>();
                    while (rs.next()) {
                        sequenceList.add(rs.getString(1));
                    }
                    return sequenceList;
                } finally {
                    rs.close();
                }
            } finally {
                sel.close();
            }
        } finally {
            con.close();
        }
    }
}
