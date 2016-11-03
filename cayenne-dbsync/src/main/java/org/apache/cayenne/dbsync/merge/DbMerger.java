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
package org.apache.cayenne.dbsync.merge;

import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.dbsync.reverse.filters.PatternFilter;
import org.apache.cayenne.dbsync.reverse.filters.TableFilter;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.DetectedDbEntity;

import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Wraps an algorithm to traverse a {@link DataMap} and create a group of {@link MergerToken}s that can be used to
 * synchronize data store and Cayenne model.
 */
public class DbMerger {

    private MergerTokenFactory tokenFactory;
    private ValueForNullProvider valueForNull;
    private boolean skipRelationshipsTokens;
    private boolean skipPKTokens;
    private FiltersConfig filters;

    private DbMerger() {
    }

    public static Builder builder(MergerTokenFactory tokenFactory) {
        return new Builder(tokenFactory);
    }

    public static DbMerger build(MergerTokenFactory tokenFactory) {
        return builder(tokenFactory).build();
    }

    /**
     * Return true if the two unordered {@link Collection}s of {@link DbJoin}s
     * are equal. Entity and Attribute names are compared case insensitively.
     * <p>
     * TODO complexity n^2; sort both collection and go through them to compare
     * = 2*n*log(n) + n
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
            if (!isNull(j1.getSource()) && !isNull(j1.getTarget()) && !isNull(j2.getSource())
                    && !isNull(j2.getTarget())
                    && j1.getSource().getEntity().getName().equalsIgnoreCase(j2.getSource().getEntity().getName())
                    && j1.getTarget().getEntity().getName().equalsIgnoreCase(j2.getTarget().getEntity().getName())
                    && j1.getSourceName().equalsIgnoreCase(j2.getSourceName())
                    && j1.getTargetName().equalsIgnoreCase(j2.getTargetName())) {

                return true;
            }
        }
        return false;
    }

    private static boolean isNull(DbAttribute attribute) {
        return attribute == null || attribute.getEntity() == null;
    }

    /**
     * Create MergerTokens that represent the difference between two {@link DataMap} objects.
     */
    public List<MergerToken> createMergeTokens(DataMap dataMap, DataMap dbImport) {

        dbImport.setQuotingSQLIdentifiers(dataMap.isQuotingSQLIdentifiers());

        List<MergerToken> tokens = createMergeTokens(filter(dataMap, filters), dbImport.getDbEntities());

        // sort. use a custom Comparator since only toDb tokens are comparable
        // by now
        Collections.sort(tokens, new Comparator<MergerToken>() {

            public int compare(MergerToken o1, MergerToken o2) {
                if (o1 instanceof AbstractToDbToken && o2 instanceof AbstractToDbToken) {

                    return ((AbstractToDbToken) o1).compareTo(o2);
                }
                return 0;
            }
        });

        return tokens;
    }

    private Collection<DbEntity> filter(DataMap existing, FiltersConfig filtersConfig) {
        Collection<DbEntity> existingFiltered = new LinkedList<>();
        for (DbEntity entity : existing.getDbEntities()) {
            TableFilter tableFilter = filtersConfig.tableFilter(entity.getCatalog(), entity.getSchema());
            if (tableFilter != null && tableFilter.isIncludeTable(entity.getName()) != null) {
                existingFiltered.add(entity);
            }
        }
        return existingFiltered;
    }

    protected List<MergerToken> createMergeTokens(Collection<DbEntity> entities, Collection<DbEntity> dbImportedEntities) {
        Collection<DbEntity> dbEntitiesToDrop = new LinkedList<>(dbImportedEntities);

        List<MergerToken> tokens = new LinkedList<>();
        for (DbEntity dbEntity : entities) {
            String tableName = dbEntity.getName();

            // look for table
            DbEntity detectedEntity = findDbEntity(dbImportedEntities, tableName);
            if (detectedEntity == null) {
                tokens.add(tokenFactory.createCreateTableToDb(dbEntity));
                // TODO: does this work properly with createReverse?
                for (DbRelationship rel : dbEntity.getRelationships()) {
                    tokens.add(tokenFactory.createAddRelationshipToDb(dbEntity, rel));
                }
                continue;
            }

            dbEntitiesToDrop.remove(detectedEntity);

            tokens.addAll(checkRelationshipsToDrop(dbEntity, detectedEntity));
            if (!skipRelationshipsTokens) {
                tokens.addAll(checkRelationshipsToAdd(dbEntity, detectedEntity));
            }
            tokens.addAll(checkRows(dbEntity, detectedEntity));

            if (!skipPKTokens) {
                MergerToken token = checkPrimaryKeyChange(dbEntity, detectedEntity);
                if (token != null) {
                    tokens.add(token);
                }
            }
        }

        // drop table
        // TODO: support drop table. currently, too many tables are marked for
        // drop
        for (DbEntity e : dbEntitiesToDrop) {
            tokens.add(tokenFactory.createDropTableToDb(e));
            for (DbRelationship relationship : e.getRelationships()) {
                DbEntity detectedEntity = findDbEntity(entities, relationship.getTargetEntityName());
                if (detectedEntity != null) {
                    tokens.add(tokenFactory.createDropRelationshipToDb(detectedEntity, relationship.getReverseRelationship()));
                }
            }
        }

        return tokens;
    }

    private List<MergerToken> checkRows(DbEntity existing, DbEntity loadedFromDb) {
        List<MergerToken> tokens = new LinkedList<MergerToken>();

        // columns to drop
        for (DbAttribute detected : loadedFromDb.getAttributes()) {
            if (findDbAttribute(existing, detected.getName()) == null) {
                tokens.add(tokenFactory.createDropColumnToDb(existing, detected));
            }
        }

        // columns to add or modify
        for (DbAttribute attr : existing.getAttributes()) {
            String columnName = attr.getName().toUpperCase();

            DbAttribute detected = findDbAttribute(loadedFromDb, columnName);

            if (detected == null) {
                tokens.add(tokenFactory.createAddColumnToDb(existing, attr));
                if (attr.isMandatory()) {
                    if (valueForNull.hasValueFor(existing, attr)) {
                        tokens.add(tokenFactory.createSetValueForNullToDb(existing, attr, valueForNull));
                    }
                    tokens.add(tokenFactory.createSetNotNullToDb(existing, attr));
                }
                continue;
            }

            // check for not null
            if (attr.isMandatory() != detected.isMandatory()) {
                if (attr.isMandatory()) {
                    if (valueForNull.hasValueFor(existing, attr)) {
                        tokens.add(tokenFactory.createSetValueForNullToDb(existing, attr, valueForNull));
                    }
                    tokens.add(tokenFactory.createSetNotNullToDb(existing, attr));
                } else {
                    tokens.add(tokenFactory.createSetAllowNullToDb(existing, attr));
                }
            }

            // TODO: check more types than char/varchar
            // TODO: psql report VARCHAR for text column, not clob
            switch (detected.getType()) {
                case Types.VARCHAR:
                case Types.CHAR:
                    if (attr.getMaxLength() != detected.getMaxLength()) {
                        tokens.add(tokenFactory.createSetColumnTypeToDb(existing, detected, attr));
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

                // alter detected relationship to match entity and attribute
                // names.
                // (case sensitively)

                DbEntity targetEntity = findDbEntity(dbEntity.getDataMap().getDbEntities(),
                        detected.getTargetEntityName());
                if (targetEntity == null) {
                    continue;
                }

                detected.setSourceEntity(dbEntity);
                detected.setTargetEntityName(targetEntity);

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

                MergerToken token = tokenFactory.createDropRelationshipToDb(dbEntity, detected);
                if (detected.isToMany()) {
                    // default toModel as we can not do drop a toMany in the db.
                    // only
                    // toOne are represented using foreign key
                    token = token.createReverse(tokenFactory);
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
                AddRelationshipToDb token = (AddRelationshipToDb) tokenFactory.createAddRelationshipToDb(dbEntity, rel);

                if (token.shouldGenerateFkConstraint()) {
                    // TODO I guess we should add relationship always; in order
                    // to have ability
                    // TODO generate reverse relationship. If it doesn't have
                    // anything to execute it will be passed
                    // TODO through execution without any affect on db
                    tokens.add(token);
                }
            }
        }

        return tokens;
    }

    private MergerToken checkPrimaryKeyChange(DbEntity dbEntity, DbEntity detectedEntity) {
        Collection<DbAttribute> primaryKeyOriginal = detectedEntity.getPrimaryKeys();
        Collection<DbAttribute> primaryKeyNew = dbEntity.getPrimaryKeys();

        String primaryKeyName = null;
        if (detectedEntity instanceof DetectedDbEntity) {
            primaryKeyName = ((DetectedDbEntity) detectedEntity).getPrimaryKeyName();
        }

        if (upperCaseEntityNames(primaryKeyOriginal).equals(upperCaseEntityNames(primaryKeyNew))) {
            return null;
        }

        return tokenFactory.createSetPrimaryKeyToDb(dbEntity, primaryKeyOriginal, primaryKeyNew, primaryKeyName);
    }

    private Set<String> upperCaseEntityNames(Collection<? extends Attribute> attrs) {
        Set<String> names = new HashSet<String>();
        for (Attribute attr : attrs) {
            names.add(attr.getName().toUpperCase());
        }
        return names;
    }

    /**
     * case insensitive search for a {@link DbEntity} in a {@link DataMap} by
     * name
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
     * case insensitive search for a {@link DbAttribute} in a {@link DbEntity}
     * by name
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
     * search for a {@link DbRelationship} like rel in the given
     * {@link DbEntity}
     */
    private DbRelationship findDbRelationship(DbEntity entity, DbRelationship rel) {
        for (DbRelationship candidate : entity.getRelationships()) {
            if (equalDbJoinCollections(candidate.getJoins(), rel.getJoins())) {
                return candidate;
            }
        }
        return null;
    }

    public static class Builder {
        private DbMerger merger;

        private Builder(MergerTokenFactory tokenFactory) {
            this.merger = new DbMerger();
            this.merger.tokenFactory = Objects.requireNonNull(tokenFactory);
        }

        public DbMerger build() {

            if (merger.valueForNull == null) {
                merger.valueForNull = new EmptyValueForNullProvider();
            }

            if (merger.filters == null) {
                // default: match all tables, no stored procedures
                merger.filters = FiltersConfig.create(null, null, TableFilter.everything(), PatternFilter.INCLUDE_NOTHING);
            }

            return merger;
        }

        public Builder valueForNullProvider(ValueForNullProvider provider) {
            merger.valueForNull = provider;
            return this;
        }

        public Builder skipRelationshipsTokens(boolean flag) {
            merger.skipRelationshipsTokens = flag;
            return this;
        }

        public Builder skipPKTokens(boolean flag) {
            merger.skipPKTokens = flag;
            return this;
        }

        public Builder filters(FiltersConfig filters) {
            merger.filters = Objects.requireNonNull(filters);
            return this;
        }
    }
}
