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

package org.apache.cayenne.dbsync.merge;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.dbsync.model.DetectedDbEntity;

class DbEntityMerger extends AbstractMerger<DataMap, DbEntity> {

    private final FiltersConfig filtersConfig;
    private final boolean skipPKTokens;
    private DataMap originalDataMap;
    private DataMap importedDataMap;

    DbEntityMerger(MergerTokenFactory tokenFactory, DataMap original, DataMap imported,
                   FiltersConfig filtersConfig, boolean skipPKTokens) {
        super(tokenFactory);
        this.filtersConfig = filtersConfig;
        this.skipPKTokens = skipPKTokens;
        originalDataMap = original;
        importedDataMap = imported;
    }

    @Override
    public List<MergerToken> createMergeTokens() {
        return createMergeTokens(originalDataMap, importedDataMap);
    }

    @Override
    MergerDictionaryDiff<DbEntity> createDiff(DataMap original, DataMap imported) {
        DbEntityDictionary dictionary = new DbEntityDictionary(original, filtersConfig);
        MergerDictionaryDiff<DbEntity> diff = new MergerDictionaryDiff.Builder<DbEntity>()
                .originalDictionary(dictionary)
                .importedDictionary(new DbEntityDictionary(imported, null))
                .build();
        setOriginalDictionary(dictionary);
        return diff;
    }

    /**
     * Generate Drop Table in DB token
     * @param imported DbEntity not found in model but found in DB
     */
    @Override
    Collection<MergerToken> createTokensForMissingOriginal(DbEntity imported) {
        return Collections.singleton(getTokenFactory().createDropTableToDb(imported));
    }

    /**
     * Generate Create Table in model token
     * @param original DbEntity found in model but not found in DB
     */
    @Override
    Collection<MergerToken> createTokensForMissingImported(DbEntity original) {
        Collection<MergerToken> tokens = new LinkedList<>();
        // add entity
        tokens.add(getTokenFactory().createCreateTableToDb(original));
        // add it's relationships
        for (DbRelationship rel : original.getRelationships()) {
            tokens.add(getTokenFactory().createAddRelationshipToDb(original, rel));
        }
        return tokens;
    }

    /**
     * Compare same entities. For now we check primary keys.
     * @param same found DbEntities in model and db
     */
    @Override
    Collection<MergerToken> createTokensForSame(MergerDiffPair<DbEntity> same) {
        if(skipPKTokens) {
            return null;
        }
        return checkPrimaryKeyChange(same.getOriginal(), same.getImported());
    }

    private Collection<MergerToken> checkPrimaryKeyChange(DbEntity original, DbEntity imported) {
        Collection<DbAttribute> primaryKeyOriginal = imported.getPrimaryKeys();
        Collection<DbAttribute> primaryKeyNew = original.getPrimaryKeys();

        String primaryKeyName = null;
        if (imported instanceof DetectedDbEntity){
            if("VIEW".equals(((DetectedDbEntity) imported).getType())){
                // Views doesn't has PKs in a database, but if the user selects some PKs in a model, we put these keys.
                return null;
            }
            primaryKeyName = ((DetectedDbEntity) imported).getPrimaryKeyName();
        }

        if (upperCaseEntityNames(primaryKeyOriginal).equals(upperCaseEntityNames(primaryKeyNew))) {
            return null;
        }

        return Collections.singleton(
                getTokenFactory().createSetPrimaryKeyToDb(
                        original, primaryKeyOriginal, primaryKeyNew, primaryKeyName
                )
        );
    }

    private Set<String> upperCaseEntityNames(Collection<? extends Attribute> attributes) {
        Set<String> names = new HashSet<>();
        for (Attribute attr : attributes) {
            names.add(attr.getName().toUpperCase());
        }
        return names;
    }
}
