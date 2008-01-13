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
package org.apache.cayenne.dba.db2;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.QueryLogger;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.map.DbEntity;

/**
 * A sequence-based PK generator used by {@link DB2Adapter}.
 * 
 * @author Andrus Adamchik
 */
public class DB2PkGenerator extends JdbcPkGenerator {

    private static final String _SEQUENCE_PREFIX = "S_";

    /**
     * @deprecated since 2.0 - other generators do not expose the default prefix.
     */
    public static final String SEQUENCE_PREFIX = "S_";

    @Override
    protected int pkFromDatabase(DataNode node, DbEntity ent) throws Exception {

        String pkGeneratingSequenceName = sequenceName(ent);

        Connection con = node.getDataSource().getConnection();
        try {
            Statement st = con.createStatement();
            try {
                String sql = "SELECT NEXTVAL FOR "
                        + pkGeneratingSequenceName
                        + " FROM SYSIBM.SYSDUMMY1";
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

    @Override
    public void createAutoPk(DataNode node, List<DbEntity> dbEntities) throws Exception {
        Collection<String> sequences = getExistingSequences(node);
       for (DbEntity entity : dbEntities) {
            if (!sequences.contains(sequenceName(entity))) {
                this.runUpdate(node, createSequenceString(entity));
            }
        }
    }

    /**
     * Creates a list of CREATE SEQUENCE statements for the list of DbEntities.
     */
    @Override
    public List<String> createAutoPkStatements(List<DbEntity> dbEntities) {
        List<String> list = new ArrayList<String>(dbEntities.size());
        for (DbEntity entity : dbEntities) {
            list.add(createSequenceString(entity));
        }
        return list;
    }

    /**
     * Drops PK sequences for all specified DbEntities.
     */
    @Override
    public void dropAutoPk(DataNode node, List<DbEntity> dbEntities) throws Exception {
        Collection<String> sequences = getExistingSequences(node);

        for (DbEntity ent : dbEntities) {
            if (sequences.contains(sequenceName(ent))) {
                runUpdate(node, dropSequenceString(ent));
            }
        }
    }

    /**
     * Creates a list of DROP SEQUENCE statements for the list of DbEntities.
     */
    @Override
    public List<String> dropAutoPkStatements(List<DbEntity> dbEntities) {
        List<String> list = new ArrayList<String>(dbEntities.size());
        for (DbEntity entity : dbEntities) {
            list.add(dropSequenceString(entity));
        }
        return list;
    }

    /**
     * Fetches a list of existing sequences that might match Cayenne generated ones.
     */
    protected List<String> getExistingSequences(DataNode node) throws SQLException {

        // check existing sequences
        Connection con = node.getDataSource().getConnection();

        try {
            Statement sel = con.createStatement();
            try {
                StringBuffer buffer = new StringBuffer();
                buffer.append("SELECT SEQNAME FROM SYSCAT.SEQUENCES ").append(
                        "WHERE SEQNAME LIKE '").append(_SEQUENCE_PREFIX).append("%'");

                String sql = buffer.toString();
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

    /**
     * Returns default sequence name for DbEntity.
     */
    protected String sequenceName(DbEntity entity) {

        String entName = entity.getName();
        String seqName = _SEQUENCE_PREFIX + entName;

        if (entity.getSchema() != null && entity.getSchema().length() > 0) {
            seqName = entity.getSchema() + "." + seqName;
        }
        return seqName;
    }

    /**
     * Returns DROP SEQUENCE statement.
     */
    protected String dropSequenceString(DbEntity entity) {
        return "DROP SEQUENCE " + sequenceName(entity) + " RESTRICT ";
    }

    /**
     * Returns CREATE SEQUENCE statement for entity.
     */
    protected String createSequenceString(DbEntity entity) {
        StringBuffer buf = new StringBuffer();
        buf
                .append("CREATE SEQUENCE ")
                .append(sequenceName(entity))
                .append(" START WITH 200")
                .append(" INCREMENT BY ")
                .append(getPkCacheSize())
                .append(" NO MAXVALUE ")
                .append(" NO CYCLE ")
                .append(" CACHE ")
                .append(getPkCacheSize());
        return buf.toString();
    }
}
