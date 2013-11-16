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
package org.apache.cayenne.merge;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DbLoader;
import org.apache.cayenne.access.DbLoaderDelegate;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.DetectedDbEntity;
import org.apache.cayenne.map.ObjEntity;

/**
 * Traverse a {@link DataNode} and a {@link DataMap} and create a group of
 * {@link MergerToken}s to alter the {@link DataNode} data store to match the
 * {@link DataMap}.
 * 
 */
public class DbMerger {

    private MergerFactory factory;
    
    private ValueForNullProvider valueForNull = new EmptyValueForNullProvider();

    /**
     * Set a {@link ValueForNullProvider} that will be used to set value for null on not
     * null columns
     */
    public void setValueForNullProvider(ValueForNullProvider valueProvider) {
        valueForNull = valueProvider;
    }

    /**
     * A method that return true if the given table name should be included. The default
     * implementation include all tables.
     */
    public boolean includeTableName(String tableName) {
        return true;
    }

    /**
     * Create and return a {@link List} of {@link MergerToken}s to alter the given
     * {@link DataNode} to match the given {@link DataMap}
     */
    public List<MergerToken> createMergeTokens(DataNode dataNode, DataMap dataMap) {
        return createMergeTokens(dataNode.getAdapter(), dataNode.getDataSource(), dataMap);
    }

    /**
     * Create and return a {@link List} of {@link MergerToken}s to alter the given
     * {@link DataNode} to match the given {@link DataMap}
     */
    public List<MergerToken> createMergeTokens(
            DbAdapter adapter,
            DataSource dataSource,
            DataMap dataMap) {
        factory = adapter.mergerFactory();

        List<MergerToken> tokens = new ArrayList<MergerToken>();
        Connection conn = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();

            final DbMerger merger = this;
            DbLoader dbLoader = new DbLoader(conn, adapter, new LoaderDelegate()) {

                @Override
                public boolean includeTableName(String tableName) {
                    return merger.includeTableName(tableName);
                }
            };
            
            DataMap detectedDataMap = dbLoader.loadDataMapFromDB(
                    null,
                    null,
                    new DataMap());
            
            detectedDataMap.setQuotingSQLIdentifiers(dataMap.isQuotingSQLIdentifiers());
            
            Map<String, DbEntity> dbEntityToDropByName = new HashMap<String, DbEntity>(
                    detectedDataMap.getDbEntityMap());

            for (DbEntity dbEntity : dataMap.getDbEntities()) {
                String tableName = dbEntity.getName();
                
                if (!includeTableName(tableName)) {
                    continue;
                }
                
                // look for table
                DbEntity detectedEntity = findDbEntity(detectedDataMap, tableName);
                if (detectedEntity == null) {
                    tokens.add(factory.createCreateTableToDb(dbEntity));
                    // TODO: does this work properly with createReverse?
                    for (DbRelationship rel : dbEntity.getRelationships()) {
                        tokens.add(factory.createAddRelationshipToDb(dbEntity, rel));
                    }
                    continue;
                }
                
                dbEntityToDropByName.remove(detectedEntity.getName());

                checkRelationshipsToDrop(adapter, tokens, dbEntity, detectedEntity);
                checkRows(tokens, dbEntity, detectedEntity);
                checkPrimaryKeyChange(adapter, tokens, dbEntity, detectedEntity);
                checkRelationshipsToAdd(adapter, tokens, dbEntity, detectedEntity);
            }

            // drop table
            // TODO: support drop table. currently, too many tables are marked for drop
            for (DbEntity e : dbEntityToDropByName.values()) {
                
                if (!includeTableName(e.getName())) {
                    continue;
                }
                
                tokens.add(factory.createDropTableToDb(e));
            }

        }
        catch (SQLException e) {
            throw new CayenneRuntimeException("", e);
        }
        finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch (SQLException e) {
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                }
                catch (SQLException e) {
                }
            }
        }
        
        // sort. use a custom Comparator since only toDb tokens
        // are comparable by now
        Collections.sort(tokens, new Comparator<MergerToken>() {

            public int compare(MergerToken o1, MergerToken o2) {
                if ((o1 instanceof AbstractToDbToken)
                        && (o2 instanceof AbstractToDbToken)) {
                    AbstractToDbToken d1 = (AbstractToDbToken) o1;
                    AbstractToDbToken d2 = (AbstractToDbToken) o2;
                    return d1.compareTo(d2);
                }
                return 0;
            }
        });

        return tokens;
    }

    private void checkRows(
            List<MergerToken> tokens,
            DbEntity dbEntity,
            DbEntity detectedEntity) {

        // columns to drop
        for (DbAttribute detected : detectedEntity.getAttributes()) {
            if (findDbAttribute(dbEntity, detected.getName()) == null) {
                tokens.add(factory.createDropColumnToDb(dbEntity, detected));
            }
        }

        // columns to add or modify
        for (DbAttribute attr : dbEntity.getAttributes()) {
            String columnName = attr.getName().toUpperCase();

            DbAttribute detected = findDbAttribute(detectedEntity, columnName);

            if (detected == null) {
                tokens.add(factory.createAddColumnToDb(dbEntity, attr));
                if (attr.isMandatory()) {
                    if (valueForNull.hasValueFor(dbEntity, attr)) {
                        tokens.add(factory.createSetValueForNullToDb(
                                dbEntity,
                                attr,
                                valueForNull));
                    }
                    tokens.add(factory.createSetNotNullToDb(dbEntity, attr));
                }
                continue;
            }

            // check for not null
            if (attr.isMandatory() != detected.isMandatory()) {
                if (attr.isMandatory()) {
                    if (valueForNull.hasValueFor(dbEntity, attr)) {
                        tokens.add(factory.createSetValueForNullToDb(
                                dbEntity,
                                attr,
                                valueForNull));
                    }
                    tokens.add(factory.createSetNotNullToDb(dbEntity, attr));
                }
                else {
                    tokens.add(factory.createSetAllowNullToDb(dbEntity, attr));
                }
            }

            // TODO: check more types than char/varchar
            // TODO: psql report VARCHAR for text column, not clob
            switch (detected.getType()) {
                case Types.VARCHAR:
                case Types.CHAR:
                    if (attr.getMaxLength() != detected.getMaxLength()) {
                        tokens.add(factory.createSetColumnTypeToDb(
                                dbEntity,
                                detected,
                                attr));
                    }
                    break;
            }
        }
    }

    private void checkRelationshipsToDrop(
            DbAdapter adapter,
            List<MergerToken> tokens,
            DbEntity dbEntity,
            DbEntity detectedEntity) {

        // relationships to drop
        for (DbRelationship detected : detectedEntity.getRelationships()) {
            if (findDbRelationship(dbEntity, detected) == null) {

                // alter detected relationship to match entity and attribute names.
                // (case sensitively)

                DbEntity targetEntity = findDbEntity(dbEntity.getDataMap(), detected
                        .getTargetEntityName());
                if (targetEntity == null) {
                    continue;
                }

                detected.setSourceEntity(dbEntity);
                detected.setTargetEntity(targetEntity);

                // manipulate the joins to match the DbAttributes in the model
                for (DbJoin join : detected.getJoins()) {
                    DbAttribute sattr = findDbAttribute(dbEntity, join.getSourceName());
                    if (sattr != null) {
                        join.setSourceName(sattr.getName());
                    }
                    DbAttribute tattr = findDbAttribute(targetEntity, join
                            .getTargetName());
                    if (tattr != null) {
                        join.setTargetName(tattr.getName());
                    }
                }

                MergerToken token = factory
                        .createDropRelationshipToDb(dbEntity, detected);
                if (detected.isToMany()) {
                    // default toModel as we can not do drop a toMany in the db. only
                    // toOne are represented using foreign key
                    token = token.createReverse(factory);
                }
                tokens.add(token);
            }
        }
    }

    private void checkRelationshipsToAdd(
            DbAdapter adapter,
            List<MergerToken> tokens,
            DbEntity dbEntity,
            DbEntity detectedEntity) {
        
        // relationships to add
        for (DbRelationship rel : dbEntity.getRelationships()) {
            
            if (!includeTableName(rel.getTargetEntityName())) {
                continue;
            }
            
            if (findDbRelationship(detectedEntity, rel) == null) {
                // TODO: very ugly. perhaps MergerToken should have a .isNoOp()?
                AbstractToDbToken t = (AbstractToDbToken) factory
                        .createAddRelationshipToDb(dbEntity, rel);
                if (!t.createSql(adapter).isEmpty()) {
                    tokens.add(factory.createAddRelationshipToDb(dbEntity, rel));
                }
            }
        }
    }
    
    private void checkPrimaryKeyChange(
            DbAdapter adapter,
            List<MergerToken> tokens,
            DbEntity dbEntity,
            DbEntity detectedEntity) {
        Collection<DbAttribute> primaryKeyOriginal = detectedEntity.getPrimaryKeys();
        Collection<DbAttribute> primaryKeyNew = dbEntity.getPrimaryKeys();

        String primaryKeyName = null;
        if ((detectedEntity instanceof DetectedDbEntity)) {
            primaryKeyName = ((DetectedDbEntity) detectedEntity).getPrimaryKeyName();
        }

        if (upperCaseEntityNames(primaryKeyOriginal).equals(
                upperCaseEntityNames(primaryKeyNew))) {
            return;
        }

        tokens.add(factory.createSetPrimaryKeyToDb(
                dbEntity,
                primaryKeyOriginal,
                primaryKeyNew,
                primaryKeyName));
    }
    
    private Set<String> upperCaseEntityNames(Collection<? extends Attribute> attrs) {
        Set<String> names = new HashSet<String>();
        for (Attribute attr : attrs) {
            names.add(attr.getName().toUpperCase());
        }
        return names;
    }
    
    /**
     * case insensitive search for a {@link DbEntity} in a {@link DataMap} by name
     */
    private DbEntity findDbEntity(DataMap map, String caseInsensitiveName) {
        // TODO: create a Map with upper case keys?
        for (DbEntity e : map.getDbEntities()) {
            if (e.getName().equalsIgnoreCase(caseInsensitiveName)) {
                return e;
            }
        }
        return null;
    }

    /**
     * case insensitive search for a {@link DbAttribute} in a {@link DbEntity} by name
     */
    private DbAttribute findDbAttribute(DbEntity entity, String caseInsensitiveName) {
        for (DbAttribute a : entity.getAttributes()) {
            if (a.getName().equalsIgnoreCase(caseInsensitiveName)) {
                return a;
            }
        }
        return null;
    }

    /**
     * search for a {@link DbRelationship} like rel in the given {@link DbEntity}
     */
    private DbRelationship findDbRelationship(DbEntity entity, DbRelationship rel) {
        for (DbRelationship candidate : entity.getRelationships()) {
            if (equalDbJoinCollections(candidate.getJoins(), rel.getJoins())) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Return true if the two unordered {@link Collection}s of {@link DbJoin}s are
     * equal. Entity and Attribute names are compared case insensitively.
     */
    private static boolean equalDbJoinCollections(
            Collection<DbJoin> j1s,
            Collection<DbJoin> j2s) {
        if (j1s.size() != j2s.size()) {
            return false;
        }

        for (DbJoin j1 : j1s) {
            boolean foundPair = false;
            for (DbJoin j2 : j2s) {
                if ((j1.getSource() == null) || (j1.getSource().getEntity() == null)) {
                    continue;
                }
                if ((j1.getTarget() == null) || (j1.getTarget().getEntity() == null)) {
                    continue;
                }
                if ((j2.getSource() == null) || (j2.getSource().getEntity() == null)) {
                    continue;
                }
                if ((j2.getTarget() == null) || (j2.getTarget().getEntity() == null)) {
                    continue;
                }

                // check entity name
                if (!j1.getSource().getEntity().getName().equalsIgnoreCase(
                        j2.getSource().getEntity().getName())) {
                    continue;
                }
                if (!j1.getTarget().getEntity().getName().equalsIgnoreCase(
                        j2.getTarget().getEntity().getName())) {
                    continue;
                }
                // check attribute name
                if (!j1.getSourceName().equalsIgnoreCase(j2.getSourceName())) {
                    continue;
                }
                if (!j1.getTargetName().equalsIgnoreCase(j2.getTargetName())) {
                    continue;
                }

                foundPair = true;
                break;
            }

            if (!foundPair) {
                return false;
            }
        }

        return true;
    }

    private static final class LoaderDelegate implements DbLoaderDelegate {

        public void dbEntityAdded(DbEntity ent) {
        }

        public void dbEntityRemoved(DbEntity ent) {
        }

        public void objEntityAdded(ObjEntity ent) {
        }

        public void objEntityRemoved(ObjEntity ent) {
        }

        public boolean overwriteDbEntity(DbEntity ent) throws CayenneException {
            return false;
        }

    }
}
