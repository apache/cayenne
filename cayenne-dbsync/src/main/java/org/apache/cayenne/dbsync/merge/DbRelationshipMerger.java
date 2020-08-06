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
import java.util.List;

import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;

/**
 * Merger of relationships.
 * Scoped to single DbEntity and it's counterpart.
 */
public class DbRelationshipMerger extends AbstractMerger<DbEntity, DbRelationship> {

    private final boolean skipRelationshipsTokens;
    private final FiltersConfig filtersConfig;

    DbRelationshipMerger(MergerTokenFactory tokenFactory, boolean skipRelationshipsTokens, FiltersConfig filtersConfig) {
        super(tokenFactory);
        this.skipRelationshipsTokens = skipRelationshipsTokens;
        this.filtersConfig = filtersConfig;
    }

    @Override
    MergerDictionaryDiff<DbRelationship> createDiff(DbEntity original, DbEntity imported) {
        return new MergerDictionaryDiff.Builder<DbRelationship>()
                .originalDictionary(new DbRelationshipDictionary(original, filtersConfig))
                .importedDictionary(new DbRelationshipDictionary(imported, filtersConfig))
                .build();
    }

    private DbEntity getOriginalSourceDbEntity(DbRelationship relationship) {
        return getOriginalDictionary().getByName(relationship.getSourceEntity().getName().toUpperCase());
    }

    private DbEntity getOriginalTargetDbEntity(DbRelationship relationship) {
        return getOriginalDictionary().getByName(relationship.getTargetEntityName().toUpperCase());
    }

    /**
     * @param imported DbRelationship that is in db but not in model
     * @return generated tokens
     */
    @Override
    Collection<MergerToken> createTokensForMissingOriginal(DbRelationship imported) {
        DbEntity originalDbEntity = getOriginalSourceDbEntity(imported);
        DbEntity targetEntity = getOriginalTargetDbEntity(imported);

        if (targetEntity != null) {
            imported.setTargetEntityName(targetEntity);
        }

        imported.setSourceEntity(originalDbEntity);

        // manipulate the joins to match the DbAttributes in the model
        for (DbJoin join : imported.getJoins()) {
            DbAttribute sourceAttr = findDbAttribute(originalDbEntity, join.getSourceName());
            if (sourceAttr != null) {
                join.setSourceName(sourceAttr.getName());
            }
            DbAttribute targetAttr = findDbAttribute(targetEntity, join.getTargetName());
            if (targetAttr != null) {
                join.setTargetName(targetAttr.getName());
            }
        }
        // Add all relationships. Tokens will decide whether or not to execute
        MergerToken token = getTokenFactory().createDropRelationshipToDb(originalDbEntity, imported);
        return Collections.singleton(token);
    }

    /**
     * @param original DbRelationship that is in model but not in db
     * @return generated tokens
     */
    @Override
    Collection<MergerToken> createTokensForMissingImported(DbRelationship original) {
        if(skipRelationshipsTokens) {
            return null;
        }
        DbEntity originalDbEntity = getOriginalSourceDbEntity(original);
        MergerToken token = getTokenFactory().createAddRelationshipToDb(originalDbEntity, original);
        return Collections.singleton(token);
    }

    /**
     *
     * @param same pair of found in model and in db DbRelationships
     * @return generated tokens
     */
    @Override
    Collection<MergerToken> createTokensForSame(MergerDiffPair<DbRelationship> same) {
        return null;
    }

    /**
     * case insensitive search for a {@link DbAttribute} in a {@link DbEntity}
     * by name
     */
    private DbAttribute findDbAttribute(DbEntity entity, String caseInsensitiveName) {
        if (entity == null) {
            return null;
        }

        for (DbAttribute a : entity.getAttributes()) {
            if (a.getName().equalsIgnoreCase(caseInsensitiveName)) {
                return a;
            }
        }
        return null;
    }

    @Override
    public List<MergerToken> createMergeTokens() {
        throw new UnsupportedOperationException();
    }
}
