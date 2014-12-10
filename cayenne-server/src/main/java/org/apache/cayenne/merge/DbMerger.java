/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.apache.cayenne.merge;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DbLoader;
import org.apache.cayenne.access.loader.DbLoaderConfiguration;
import org.apache.cayenne.access.loader.DefaultDbLoaderDelegate;
import org.apache.cayenne.access.loader.NameFilter;
import org.apache.cayenne.access.loader.filters.DbPath;
import org.apache.cayenne.access.loader.filters.FiltersConfig;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.DetectedDbEntity;

/**
 * Traverse a {@link DataNode} and a {@link DataMap} and create a group of
 * {@link MergerToken}s to alter the {@link DataNode} data store to match the
 * {@link DataMap}.
 * 
 */
public class DbMerger {

    private final MergerFactory factory;
    
    private final ValueForNullProvider valueForNull;

    public DbMerger(MergerFactory factory) {
        this(factory, null);
    }

    public DbMerger(MergerFactory factory, ValueForNullProvider valueForNull) {
        this.factory = factory;
        this.valueForNull = valueForNull == null ? new EmptyValueForNullProvider() : valueForNull;
    }

    /**
     * Create and return a {@link List} of {@link MergerToken}s to alter the given
     * {@link DataNode} to match the given {@link DataMap}
     */
    public List<MergerToken> createMergeTokens(DataNode dataNode, DataMap existing, DbLoaderConfiguration config) {
        return createMergeTokens(dataNode.getDataSource(), dataNode.getAdapter(), existing, config);
    }

    /**
     * Create and return a {@link List} of {@link MergerToken}s to alter the given
     * {@link DataNode} to match the given {@link DataMap}
     */
    public List<MergerToken> createMergeTokens(DbLoader dbLoader, DataMap existing, DbLoaderConfiguration config) {
        return createMergeTokens(existing, loadDataMapFromDb(dbLoader, config), config.getFiltersConfig());
    }

    /**
     * Create and return a {@link List} of {@link MergerToken}s to alter the given
     * {@link DataNode} to match the given {@link DataMap}
     */
    public List<MergerToken> createMergeTokens(DataSource dataSource, DbAdapter adapter, DataMap existingDataMap, DbLoaderConfiguration config) {
        return createMergeTokens(existingDataMap, loadDataMapFromDb(dataSource, adapter, config), config.getFiltersConfig());
    }

    /**
     * Create and return a {@link List} of {@link MergerToken}s to alter the given
     * {@link DataNode} to match the given {@link DataMap}
     */
    public List<MergerToken> createMergeTokens(DataMap existing, DataMap loadedFomDb, FiltersConfig filtersConfig) {

        loadedFomDb.setQuotingSQLIdentifiers(existing.isQuotingSQLIdentifiers());

        List<MergerToken> tokens = createMergeTokens(filter(existing, filtersConfig), loadedFomDb.getDbEntities());


        // sort. use a custom Comparator since only toDb tokens are comparable by now
        Collections.sort(tokens, new Comparator<MergerToken>() {

            public int compare(MergerToken o1, MergerToken o2) {
                if (o1 instanceof AbstractToDbToken
                        && o2 instanceof AbstractToDbToken) {
                    AbstractToDbToken d1 = (AbstractToDbToken) o1;
                    AbstractToDbToken d2 = (AbstractToDbToken) o2;
                    return d1.compareTo(d2);
                }
                return 0;
            }
        });

        return tokens;
    }

    private Collection<DbEntity> filter(DataMap existing, FiltersConfig filtersConfig) {
        Collection<DbEntity> existingFiltered = new LinkedList<DbEntity>();
        for (DbEntity entity : existing.getDbEntities()) {
            if (filtersConfig.filter(DbPath.build(entity)).tableFilter().isInclude(entity)) {
                existingFiltered.add(entity);
            }
        }
        return existingFiltered;
    }

    private DataMap loadDataMapFromDb(DataSource dataSource, DbAdapter adapter, DbLoaderConfiguration config) {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();

            // TODO pass naming strategy
            DbLoader dbLoader = new DbLoader(conn, adapter, new DefaultDbLoaderDelegate());
            return loadDataMapFromDb(dbLoader, config);
        }
        catch (SQLException e) {
            throw new CayenneRuntimeException("Can't doLoad dataMap from db.", e);
        }
        finally {
            if (conn != null) {
                try {
                    conn.close();
                }
                catch (SQLException e) {
                    // Do nothing.
                }
            }
        }
    }

    private DataMap loadDataMapFromDb(DbLoader dbLoader, DbLoaderConfiguration config) {
        try {
            return dbLoader.load(config);
        } catch (SQLException e) {
            // TODO log
        }

        return new DataMap();
    }

    /**
     *
     *
     * @param existing
     * @param loadedFromDb
     * @return
     */
    public List<MergerToken> createMergeTokens(Collection<DbEntity> existing, Collection<DbEntity> loadedFromDb) {
        Collection<DbEntity> dbEntitiesToDrop = new LinkedList<DbEntity>(loadedFromDb);

        List<MergerToken> tokens = new LinkedList<MergerToken>();
        for (DbEntity dbEntity : existing) {
            String tableName = dbEntity.getName();

            // look for table
            DbEntity detectedEntity = findDbEntity(loadedFromDb, tableName);
            if (detectedEntity == null) {
                tokens.add(factory.createCreateTableToDb(dbEntity));
                // TODO: does this work properly with createReverse?
                for (DbRelationship rel : dbEntity.getRelationships()) {
                    tokens.add(factory.createAddRelationshipToDb(dbEntity, rel));
                }
                continue;
            }

            dbEntitiesToDrop.remove(detectedEntity);

            tokens.addAll(checkRelationshipsToDrop(dbEntity, detectedEntity));
            tokens.addAll(checkRelationshipsToAdd(dbEntity, detectedEntity));
            tokens.addAll(checkRows(dbEntity, detectedEntity));

            MergerToken token = checkPrimaryKeyChange(dbEntity, detectedEntity);
            if (token != null) {
                tokens.add(token);
            }
        }

        // drop table
        // TODO: support drop table. currently, too many tables are marked for drop
        for (DbEntity e : dbEntitiesToDrop) {
            tokens.add(factory.createDropTableToDb(e));
        }

        return tokens;
    }

    private List<MergerToken> checkRows(DbEntity existing, DbEntity loadedFromDb) {
        List<MergerToken> tokens = new LinkedList<MergerToken>();

        // columns to drop
        for (DbAttribute detected : loadedFromDb.getAttributes()) {
            if (findDbAttribute(existing, detected.getName()) == null) {
                tokens.add(factory.createDropColumnToDb(existing, detected));
            }
        }

        // columns to add or modify
        for (DbAttribute attr : existing.getAttributes()) {
            String columnName = attr.getName().toUpperCase();

            DbAttribute detected = findDbAttribute(loadedFromDb, columnName);

            if (detected == null) {
                tokens.add(factory.createAddColumnToDb(existing, attr));
                if (attr.isMandatory()) {
                    if (valueForNull.hasValueFor(existing, attr)) {
                        tokens.add(factory.createSetValueForNullToDb(existing, attr, valueForNull));
                    }
                    tokens.add(factory.createSetNotNullToDb(existing, attr));
                }
                continue;
            }

            // check for not null
            if (attr.isMandatory() != detected.isMandatory()) {
                if (attr.isMandatory()) {
                    if (valueForNull.hasValueFor(existing, attr)) {
                        tokens.add(factory.createSetValueForNullToDb(existing, attr, valueForNull));
                    }
                    tokens.add(factory.createSetNotNullToDb(existing, attr));
                }
                else {
                    tokens.add(factory.createSetAllowNullToDb(existing, attr));
                }
            }

            // TODO: check more types than char/varchar
            // TODO: psql report VARCHAR for text column, not clob
            switch (detected.getType()) {
                case Types.VARCHAR:
                case Types.CHAR:
                    if (attr.getMaxLength() != detected.getMaxLength()) {
                        tokens.add(factory.createSetColumnTypeToDb(existing, detected, attr));
                    }
                    break;
            }
        }

        return tokens;
    }

    private List<MergerToken> checkRelationshipsToDrop(DbEntity dbEntity, DbEntity detectedEntity) {
        List<MergerToken> tokens = new LinkedList<MergerToken>();

        // relationships to drop
        for (DbRelationship detected : detectedEntity.getRelationships()) {
            if (findDbRelationship(dbEntity, detected) == null) {

                // alter detected relationship to match entity and attribute names.
                // (case sensitively)

                DbEntity targetEntity = findDbEntity(dbEntity.getDataMap().getDbEntities(), detected.getTargetEntityName());
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
                    DbAttribute tattr = findDbAttribute(targetEntity, join.getTargetName());
                    if (tattr != null) {
                        join.setTargetName(tattr.getName());
                    }
                }

                MergerToken token = factory.createDropRelationshipToDb(dbEntity, detected);
                if (detected.isToMany()) {
                    // default toModel as we can not do drop a toMany in the db. only
                    // toOne are represented using foreign key
                    token = token.createReverse(factory);
                }
                tokens.add(token);
            }
        }

        return tokens;
    }

    private List<MergerToken> checkRelationshipsToAdd(DbEntity dbEntity, DbEntity detectedEntity) {

        List<MergerToken> tokens = new LinkedList<MergerToken>();

        for (DbRelationship rel : dbEntity.getRelationships()) {
            if (findDbRelationship(detectedEntity, rel) == null) {
                AddRelationshipToDb token = (AddRelationshipToDb)
                        factory.createAddRelationshipToDb(dbEntity, rel);

                if (token.shouldGenerateFkConstraint()) {
                    // TODO I guess we should add relationship always; in order to have ability
                    // TODO generate reverse relationship. If it doesn't have anything to execute it will be passed
                    // TODO through execution without any affect on db
                    tokens.add(token);
                }
            }
        }

        return tokens;
    }
    
    private MergerToken checkPrimaryKeyChange(
            DbEntity dbEntity,
            DbEntity detectedEntity) {
        Collection<DbAttribute> primaryKeyOriginal = detectedEntity.getPrimaryKeys();
        Collection<DbAttribute> primaryKeyNew = dbEntity.getPrimaryKeys();

        String primaryKeyName = null;
        if (detectedEntity instanceof DetectedDbEntity) {
            primaryKeyName = ((DetectedDbEntity) detectedEntity).getPrimaryKeyName();
        }

        if (upperCaseEntityNames(primaryKeyOriginal).equals(upperCaseEntityNames(primaryKeyNew))) {
            return null;
        }

        return factory.createSetPrimaryKeyToDb(dbEntity, primaryKeyOriginal, primaryKeyNew, primaryKeyName);
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
    private DbEntity findDbEntity(Collection<DbEntity> dbEntities, String caseInsensitiveName) {
        // TODO: create a Map with upper case keys?
        for (DbEntity e : dbEntities) {
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
     *
     * TODO complexity n^2; sort both collection and go through them to compare = 2*n*log(n) + n
     */
    private static boolean equalDbJoinCollections(Collection<DbJoin> j1s, Collection<DbJoin> j2s) {
        if (j1s.size() != j2s.size()) {
            return false;
        }

        for (DbJoin j1 : j1s) {
            if (!havePair(j2s, j1)) {
                return false;
            }
        }

        return true;
    }

    private static boolean havePair(Collection<DbJoin> j2s, DbJoin j1) {
        for (DbJoin j2 : j2s) {
            if (!isNull(j1.getSource()) && !isNull(j1.getTarget()) &&
                !isNull(j2.getSource()) && !isNull(j2.getTarget()) &&
                j1.getSource().getEntity().getName().equalsIgnoreCase(j2.getSource().getEntity().getName()) &&
                j1.getTarget().getEntity().getName().equalsIgnoreCase(j2.getTarget().getEntity().getName()) &&
                j1.getSourceName().equalsIgnoreCase(j2.getSourceName()) &&
                j1.getTargetName().equalsIgnoreCase(j2.getTargetName())) {

                return true;
            }
        }
        return false;
    }

    private static boolean isNull(DbAttribute attribute) {
        return attribute == null || attribute.getEntity() == null;
    }
}
