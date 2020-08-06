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

package org.apache.cayenne.dba.oracle;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbKeyGenerator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Sequence-based primary key generator implementation for Oracle. Uses Oracle
 * sequences to generate primary key values. This approach is at least 50%
 * faster when tested with Oracle compared to the lookup table approach.
 * <p>
 * When using Cayenne key caching mechanism, make sure that sequences in the
 * database have "INCREMENT BY" greater or equal to OraclePkGenerator
 * "pkCacheSize" property value. If this is not the case, you will need to
 * adjust PkGenerator value accordingly. For example when sequence is
 * incremented by 1 each time, use the following code:
 * </p>
 *
 * <pre>
 * dataNode.getAdapter().getPkGenerator().setPkCacheSize(1);
 * </pre>
 */
public class OraclePkGenerator extends JdbcPkGenerator {

    /**
     * Used by DI
     * @since 4.1
     */
    public OraclePkGenerator() {
        super();
    }

    protected OraclePkGenerator(JdbcAdapter adapter) {
        super(adapter);
    }

    private static final String _SEQUENCE_PREFIX = "pk_";

    @Override
    public void createAutoPk(DataNode node, List<DbEntity> dbEntities) throws Exception {
        List<String> sequences = getExistingSequences(node);
        // create needed sequences
        for (DbEntity dbEntity : dbEntities) {
            if (!sequences.contains(sequenceName(dbEntity))) {
                runUpdate(node, createSequenceString(dbEntity));
            }
        }
    }

    /**
     * Creates a list of CREATE SEQUENCE statements for the list of DbEntities.
     */
    @Override
    public List<String> createAutoPkStatements(List<DbEntity> dbEntities) {
        List<String> list = new ArrayList<>(dbEntities.size());
        for (DbEntity dbEntity : dbEntities) {
            list.add(createSequenceString(dbEntity));
        }

        return list;
    }

    /**
     * Drops PK sequences for all specified DbEntities.
     */
    @Override
    public void dropAutoPk(DataNode node, List<DbEntity> dbEntities) throws Exception {
        List<String> sequences = getExistingSequences(node);

        // drop obsolete sequences
        for (DbEntity dbEntity : dbEntities) {
            String name;
            if (dbEntity.getDataMap().isQuotingSQLIdentifiers()) {
                DbEntity tempEnt = new DbEntity();
                DataMap dm = new DataMap();
                dm.setQuotingSQLIdentifiers(false);
                tempEnt.setDataMap(dm);
                tempEnt.setName(dbEntity.getName());
                name = stripSchemaName(sequenceName(tempEnt));
            } else {
                name = stripSchemaName(sequenceName(dbEntity));
            }
            if (sequences.contains(name)) {
                runUpdate(node, dropSequenceString(dbEntity));
            }
        }
    }

    /**
     * Creates a list of DROP SEQUENCE statements for the list of DbEntities.
     */
    @Override
    public List<String> dropAutoPkStatements(List<DbEntity> dbEntities) {
        List<String> list = new ArrayList<>(dbEntities.size());
        for (DbEntity dbEntity : dbEntities) {
            list.add(dropSequenceString(dbEntity));
        }

        return list;
    }

    protected String createSequenceString(DbEntity ent) {
        return "CREATE SEQUENCE " + sequenceName(ent) + " START WITH " + pkStartValue + " INCREMENT BY " + pkCacheSize(ent);
    }

    /**
     * Returns a SQL string needed to drop any database objects associated with
     * automatic primary key generation process for a specific DbEntity.
     */
    protected String dropSequenceString(DbEntity ent) {
        return "DROP SEQUENCE " + sequenceName(ent);
    }

    protected String selectNextValQuery(String pkGeneratingSequenceName) {
        return "SELECT " + pkGeneratingSequenceName + ".nextval FROM DUAL";
    }

    protected String selectAllSequencesQuery() {
        return "SELECT LOWER(SEQUENCE_NAME) FROM ALL_SEQUENCES";
    }

    /**
     * Generates primary key by calling Oracle sequence corresponding to the
     * <code>dbEntity</code>. Executed SQL looks like this:
     *
     * <pre>
     *   SELECT pk_table_name.nextval FROM DUAL
     * </pre>
     *
     * @since 3.0
     */
    @Override
    protected long longPkFromDatabase(DataNode node, DbEntity entity) throws Exception {

        DbKeyGenerator pkGenerator = entity.getPrimaryKeyGenerator();
        String pkGeneratingSequenceName;
        if (pkGenerator != null && DbKeyGenerator.ORACLE_TYPE.equals(pkGenerator.getGeneratorType())
                && pkGenerator.getGeneratorName() != null) {
            pkGeneratingSequenceName = pkGenerator.getGeneratorName();
        } else {
            pkGeneratingSequenceName = sequenceName(entity);
        }

        try (Connection con = node.getDataSource().getConnection()) {
            try (Statement st = con.createStatement()) {
                String sql = selectNextValQuery(pkGeneratingSequenceName);
                adapter.getJdbcEventLogger().log(sql);

                try (ResultSet rs = st.executeQuery(sql)) {
                    if (!rs.next()) {
                        throw new CayenneRuntimeException("Error generating pk for DbEntity %s", entity.getName());
                    }
                    return rs.getLong(1);
                }
            }
        }
    }

    protected int pkCacheSize(DbEntity entity) {
        // use custom generator if possible
        DbKeyGenerator keyGenerator = entity.getPrimaryKeyGenerator();
        if (keyGenerator != null && DbKeyGenerator.ORACLE_TYPE.equals(keyGenerator.getGeneratorType())
                && keyGenerator.getGeneratorName() != null) {

            Integer size = keyGenerator.getKeyCacheSize();
            return (size != null && size >= 1) ? size : super.getPkCacheSize();
        } else {
            return super.getPkCacheSize();
        }
    }

    /**
     * Returns expected primary key sequence name for a DbEntity.
     */
    protected String sequenceName(DbEntity entity) {

        // use custom generator if possible
        DbKeyGenerator keyGenerator = entity.getPrimaryKeyGenerator();
        if (keyGenerator != null && DbKeyGenerator.ORACLE_TYPE.equals(keyGenerator.getGeneratorType())
                && keyGenerator.getGeneratorName() != null) {

            return keyGenerator.getGeneratorName().toLowerCase();
        } else {
            String seqName = getSequencePrefix() + entity.getName().toLowerCase();
            return adapter.getQuotingStrategy().quotedIdentifier(entity, entity.getCatalog(), entity.getSchema(), seqName);
        }
    }

    protected String getSequencePrefix() {
        return _SEQUENCE_PREFIX;
    }

    private String stripSchemaName(String sequenceName) {
        int ind = sequenceName.indexOf('.');
        return ind >= 0 ? sequenceName.substring(ind + 1) : sequenceName;
    }

    /**
     * Fetches a list of existing sequences that might match Cayenne generated
     * ones.
     */
    protected List<String> getExistingSequences(DataNode node) throws SQLException {

        // check existing sequences
        try (Connection con = node.getDataSource().getConnection()) {
            try (Statement sel = con.createStatement()) {
                String sql = selectAllSequencesQuery();
                adapter.getJdbcEventLogger().log(sql);

                try (ResultSet rs = sel.executeQuery(sql)) {
                    List<String> sequenceList = new ArrayList<>();
                    while (rs.next()) {
                        String name = rs.getString(1);
                        if (name != null) {
                            sequenceList.add(name.trim());
                        }
                    }
                    return sequenceList;
                }
            }
        }
    }
}
