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

package org.apache.cayenne.dba;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbKeyGenerator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * A common superclass for sequence-based primary key generators. Uses database
 * sequences to generate primary key values, which is generally faster than the
 * lookup table approach implemented by {@link JdbcPkGenerator}. Concrete
 * subclasses provide database-specific SQL for selecting the next sequence value
 * and for discovering existing sequences.
 * <p>
 * When using the Cayenne key caching mechanism, make sure that sequences in the
 * database have "INCREMENT BY" greater or equal to the generator "pkCacheSize"
 * property value. If this is not the case, you will need to adjust the
 * PkGenerator value accordingly. For example when a sequence is incremented by 1
 * each time, use the following code:
 * </p>
 *
 * <pre>
 * dataNode.getAdapter().getPkGenerator().setPkCacheSize(1);
 * </pre>
 *
 * @since 5.0
 */
public abstract class SequencePkGenerator extends JdbcPkGenerator {

    private static final String _SEQUENCE_PREFIX = "pk_";

    public SequencePkGenerator() {
        super();
    }

    protected SequencePkGenerator(JdbcAdapter adapter) {
        super(adapter);
    }

    @Override
    public void createAutoPk(DataNode node, List<DbEntity> dbEntities) {
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
    public void dropAutoPk(DataNode node, List<DbEntity> dbEntities) {
        List<String> sequences = getExistingSequences(node);

        // drop obsolete sequences
        for (DbEntity dbEntity : dbEntities) {
            if (sequences.contains(sequenceName(dbEntity))) {
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
        String seqFQN = adapter.getQuotingStrategy(ent).quotedFQN(ent.getCatalog(), ent.getSchema(), sequenceName(ent));
        return "CREATE SEQUENCE " + seqFQN + " START WITH " + pkStartValue + " INCREMENT BY " + pkCacheSize(ent);
    }

    /**
     * Returns a SQL string needed to drop any database objects associated with
     * automatic primary key generation process for a specific DbEntity.
     */
    protected String dropSequenceString(DbEntity ent) {
        String seqFQN = adapter.getQuotingStrategy(ent).quotedFQN(ent.getCatalog(), ent.getSchema(), sequenceName(ent));
        return "DROP SEQUENCE " + seqFQN;
    }

    /**
     * Returns a database-specific query to fetch the next value of the named
     * sequence.
     */
    protected abstract String selectNextValQuery(String pkGeneratingSequenceName);

    /**
     * Returns a database-specific query that lists the names of all existing
     * sequences.
     */
    protected abstract String selectAllSequencesQuery();

    /**
     * Generates a primary key by calling the database sequence corresponding to
     * the <code>dbEntity</code>.
     *
     * @since 3.0
     */
    @Override
    protected long longPkFromDatabase(DataNode node, DbEntity entity) {

        DbKeyGenerator pkGenerator = entity.getPrimaryKeyGenerator();
        String pkGeneratingSequenceName;
        if (pkGenerator != null && DbKeyGenerator.ORACLE_TYPE.equals(pkGenerator.getGeneratorType())
                && pkGenerator.getGeneratorName() != null) {
            pkGeneratingSequenceName = pkGenerator.getGeneratorName();
        } else {
            pkGeneratingSequenceName = adapter.getQuotingStrategy(entity).quotedFQN(entity.getCatalog(),
                    entity.getSchema(), sequenceName(entity));
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
        } catch (SQLException e) {
            throw new CayenneRuntimeException("Error generating pk for DbEntity %s", e, entity.getName());
        }
    }

    protected int pkCacheSize(DbEntity entity) {
        // use custom generator if possible
        DbKeyGenerator keyGenerator = entity.getPrimaryKeyGenerator();
        if (keyGenerator != null && DbKeyGenerator.ORACLE_TYPE.equals(keyGenerator.getGeneratorType())
                && keyGenerator.getGeneratorName() != null) {

            Integer size = keyGenerator.getKeyCacheSize();
            return (size != null && size >= 1) ? size : getPkCacheSize();
        } else {
            return getPkCacheSize();
        }
    }

    protected String sequenceName(DbEntity entity) {

        // use custom generator if possible
        DbKeyGenerator keyGenerator = entity.getPrimaryKeyGenerator();
        if (keyGenerator != null
                && DbKeyGenerator.ORACLE_TYPE.equals(keyGenerator.getGeneratorType())
                && keyGenerator.getGeneratorName() != null) {

            return keyGenerator.getGeneratorName().toLowerCase();
        } else {
            return getSequencePrefix() + entity.getName().toLowerCase();
        }
    }

    protected String getSequencePrefix() {
        return _SEQUENCE_PREFIX;
    }

    /**
     * Fetches a list of existing sequences that might match Cayenne generated
     * ones.
     */
    protected List<String> getExistingSequences(DataNode node) {

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
        } catch (SQLException e) {
            throw new CayenneRuntimeException("Error fetching existing sequences", e);
        }
    }
}
