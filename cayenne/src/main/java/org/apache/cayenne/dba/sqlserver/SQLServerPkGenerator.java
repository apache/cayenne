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

package org.apache.cayenne.dba.sqlserver;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.dba.oracle.OraclePkGenerator;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbKeyGenerator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * The default PK generator for MS SQL,
 * which uses sequences to generate a PK for an integer key type
 * and NEWID() for UNIQUEIDENTIFIER key type
 *
 * @since 4.1
 */
public class SQLServerPkGenerator extends OraclePkGenerator {

    //MS SQL function for generating GUID
    private static final String SELECT_NEW_GUID = "SELECT NEWID()";

    private static final String SEQUENCE_PREFIX = "_pk";

    private static final int MAX_LENGTH_GUID = 36;

    public SQLServerPkGenerator() {
        super();
    }

    protected SQLServerPkGenerator(JdbcAdapter adapter) {
        super(adapter);
    }

    @Override
    protected String createSequenceString(DbEntity ent) {
        return "CREATE SEQUENCE " + sequenceName(ent)
                + " AS [bigint] START WITH " + pkStartValue + " INCREMENT BY "
                + pkCacheSize(ent) + " NO CACHE";
    }

    @Override
    protected String getSequencePrefix() {
        return SEQUENCE_PREFIX;
    }

    @Override
    protected String selectNextValQuery(String sequenceName) {
        return "SELECT NEXT VALUE FOR " + sequenceName;
    }

    @Override
    public List<String> createAutoPkStatements(List<DbEntity> dbEntities) {
        List<String> list = new ArrayList<>(dbEntities.size());
        for (DbEntity dbEntity : dbEntities) {
            if (dbEntity.getPrimaryKeys().size() == 1) {
                DbAttribute pk = dbEntity.getPrimaryKeys().iterator().next();
                if (TypesMapping.isNumeric(pk.getType())) {
                    list.add(createSequenceString(dbEntity));
                }
            }
        }
        return list;
    }

    @Override
    public List<String> dropAutoPkStatements(List<DbEntity> dbEntities) {
        List<String> list = new ArrayList<>(dbEntities.size());
        for (DbEntity dbEntity : dbEntities) {
            if (dbEntity.getPrimaryKeys().size() == 1) {
                DbAttribute pk = dbEntity.getPrimaryKeys().iterator().next();
                if (TypesMapping.isNumeric(pk.getType())) {
                    list.add(dropSequenceString(dbEntity));
                }
            }
        }
        return list;
    }

    @Override
    public Object generatePk(DataNode node, DbAttribute pk) throws Exception {
        DbEntity entity = pk.getEntity();

        //check key on UNIQUEIDENTIFIER; UNIQUEIDENTIFIER is a character with a length of 36
        if (TypesMapping.isCharacter(pk.getType()) && pk.getMaxLength() == MAX_LENGTH_GUID) {
            return guidPkFromDatabase(node, entity);
        } else {
            return super.generatePk(node, pk);
        }
    }

    @Override
    protected String selectAllSequencesQuery() {
        return "SELECT seq.name"
                + " FROM sys.sequences AS seq"
                + " JOIN sys.schemas AS sch"
                + " ON seq.schema_id = sch.schema_id";
    }

    @Override
    protected String sequenceName(DbEntity entity) {
        // use custom generator if possible
        DbKeyGenerator keyGenerator = entity.getPrimaryKeyGenerator();
        if (keyGenerator != null && DbKeyGenerator.ORACLE_TYPE.equals(keyGenerator.getGeneratorType())
                && keyGenerator.getGeneratorName() != null) {

            return keyGenerator.getGeneratorName().toLowerCase();
        } else {
            String seqName = entity.getName().toLowerCase() + getSequencePrefix();
            return adapter.getQuotingStrategy().quotedIdentifier(entity, entity.getSchema(), seqName);
        }
    }

    protected String guidPkFromDatabase(DataNode node, DbEntity entity) throws SQLException {
        try (Connection con = node.getDataSource().getConnection()) {
            try (Statement st = con.createStatement()) {
                adapter.getJdbcEventLogger().log(SELECT_NEW_GUID);
                try (ResultSet rs = st.executeQuery(SELECT_NEW_GUID)) {
                    if (!rs.next()) {
                        throw new CayenneRuntimeException("Error generating pk for DbEntity %s", entity.getName());
                    }
                    return rs.getString(1);
                }
            }
        }
    }
}
