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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbKeyGenerator;

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

    protected OraclePkGenerator(JdbcAdapter adapter) {
        super(adapter);
    }

    private static final String _SEQUENCE_PREFIX = "pk_";

    @Override
    public void createAutoPk(DataNode node, List dbEntities) throws Exception {
        List sequences = getExistingSequences(node);

        // create needed sequences
        Iterator it = dbEntities.iterator();
        while (it.hasNext()) {
            DbEntity ent = (DbEntity) it.next();
            if (!sequences.contains(sequenceName(ent))) {
                runUpdate(node, createSequenceString(ent));
            }
        }
    }

    @Override
    public List createAutoPkStatements(List dbEntities) {
        List<String> list = new ArrayList<String>();
        Iterator it = dbEntities.iterator();
        while (it.hasNext()) {
            DbEntity ent = (DbEntity) it.next();
            list.add(createSequenceString(ent));
        }

        return list;
    }

    @Override
    public void dropAutoPk(DataNode node, List dbEntities) throws Exception {
        List sequences = getExistingSequences(node);

        // drop obsolete sequences
        Iterator it = dbEntities.iterator();
        while (it.hasNext()) {
            DbEntity ent = (DbEntity) it.next();
            String name;
            if (ent.getDataMap().isQuotingSQLIdentifiers()) {
                DbEntity tempEnt = new DbEntity();
                DataMap dm = new DataMap();
                dm.setQuotingSQLIdentifiers(false);
                tempEnt.setDataMap(dm);
                tempEnt.setName(ent.getName());
                name = stripSchemaName(sequenceName(tempEnt));
            } else {
                name = stripSchemaName(sequenceName(ent));
            }
            if (sequences.contains(name)) {
                runUpdate(node, dropSequenceString(ent));
            }
        }
    }

    @Override
    public List dropAutoPkStatements(List dbEntities) {
        List<String> list = new ArrayList<String>();
        Iterator it = dbEntities.iterator();
        while (it.hasNext()) {
            DbEntity ent = (DbEntity) it.next();
            list.add(dropSequenceString(ent));
        }

        return list;
    }

    protected String createSequenceString(DbEntity ent) {
        StringBuilder buf = new StringBuilder();
        buf.append("CREATE SEQUENCE ").append(sequenceName(ent)).append(" START WITH 200").append(" INCREMENT BY ")
                .append(pkCacheSize(ent));
        return buf.toString();
    }

    /**
     * Returns a SQL string needed to drop any database objects associated with
     * automatic primary key generation process for a specific DbEntity.
     */
    protected String dropSequenceString(DbEntity ent) {

        StringBuilder buf = new StringBuilder();
        buf.append("DROP SEQUENCE ").append(sequenceName(ent));
        return buf.toString();
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
                && pkGenerator.getGeneratorName() != null)
            pkGeneratingSequenceName = pkGenerator.getGeneratorName();
        else
            pkGeneratingSequenceName = sequenceName(entity);

        Connection con = node.getDataSource().getConnection();
        try {
            Statement st = con.createStatement();
            try {
                String sql = "SELECT " + pkGeneratingSequenceName + ".nextval FROM DUAL";
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

    protected int pkCacheSize(DbEntity entity) {
        // use custom generator if possible
        DbKeyGenerator keyGenerator = entity.getPrimaryKeyGenerator();
        if (keyGenerator != null && DbKeyGenerator.ORACLE_TYPE.equals(keyGenerator.getGeneratorType())
                && keyGenerator.getGeneratorName() != null) {

            Integer size = keyGenerator.getKeyCacheSize();
            return (size != null && size.intValue() >= 1) ? size.intValue() : super.getPkCacheSize();
        } else {
            return super.getPkCacheSize();
        }
    }

    /** Returns expected primary key sequence name for a DbEntity. */
    protected String sequenceName(DbEntity entity) {

        // use custom generator if possible
        DbKeyGenerator keyGenerator = entity.getPrimaryKeyGenerator();
        if (keyGenerator != null && DbKeyGenerator.ORACLE_TYPE.equals(keyGenerator.getGeneratorType())
                && keyGenerator.getGeneratorName() != null) {

            return keyGenerator.getGeneratorName().toLowerCase();
        } else {
            String entName = entity.getName();
            String seqName = _SEQUENCE_PREFIX + entName.toLowerCase();

            return adapter.getQuotingStrategy().quotedIdentifier(entity, entity.getCatalog(), entity.getSchema(),
                    seqName);
        }
    }

    protected String stripSchemaName(String sequenceName) {
        int ind = sequenceName.indexOf('.');
        return (ind >= 0) ? sequenceName.substring(ind + 1) : sequenceName;
    }

    /**
     * Fetches a list of existing sequences that might match Cayenne generated
     * ones.
     */
    protected List getExistingSequences(DataNode node) throws SQLException {

        // check existing sequences
        Connection con = node.getDataSource().getConnection();

        try {
            Statement sel = con.createStatement();
            try {
                String sql = "SELECT LOWER(SEQUENCE_NAME) FROM ALL_SEQUENCES";
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
