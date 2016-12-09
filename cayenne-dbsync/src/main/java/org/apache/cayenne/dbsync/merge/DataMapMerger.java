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

package org.apache.cayenne.dbsync.merge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.cayenne.dbsync.merge.token.EmptyValueForNullProvider;
import org.apache.cayenne.dbsync.merge.token.ValueForNullProvider;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.merge.token.TokenComparator;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.dbsync.reverse.filters.PatternFilter;
import org.apache.cayenne.dbsync.reverse.filters.TableFilter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;

/**
 * Synchronization data base store and Cayenne model.
 */
public class DataMapMerger implements Merger<DataMap> {

    private MergerTokenFactory tokenFactory;
    private ValueForNullProvider valueForNull;
    private boolean skipRelationshipsTokens;
    private boolean skipPKTokens;
    private FiltersConfig filters;

    private DataMapMerger() {
    }

    /**
     * Create List of MergerToken that represent the difference between two {@link DataMap} objects.
     */
    public List<MergerToken> createMergeTokens(DataMap original, DataMap importedFromDb) {
        List<MergerToken> tokens = new ArrayList<>();

        DbEntityMerger dbEntityMerger = mergeDbEntities(tokens, original, importedFromDb);
        mergeAttributes(tokens, dbEntityMerger, original, importedFromDb);
        mergeRelationships(tokens, dbEntityMerger, original, importedFromDb);

        Collections.sort(tokens, new TokenComparator());

        return tokens;
    }

    private DbEntityMerger mergeDbEntities(List<MergerToken> tokens, DataMap original, DataMap importedFromDb) {
        DbEntityMerger dbEntityMerger = new DbEntityMerger(tokenFactory, skipPKTokens, original, importedFromDb);
        tokens.addAll(dbEntityMerger.createMergeTokens(original, importedFromDb));
        return dbEntityMerger;
    }

    private void mergeAttributes(List<MergerToken> tokens, DbEntityMerger dbEntityMerger, DataMap original, DataMap importedFromDb) {
        ChainMerger<DbEntity, DbAttribute> dbAttributeMerger = new ChainMerger<>(
                tokenFactory, original, importedFromDb,
                new DbAttributeMerger(tokenFactory, original, importedFromDb, valueForNull),
                dbEntityMerger
        );
        tokens.addAll(dbAttributeMerger.createMergeTokens(null, null));
    }

    private void mergeRelationships(List<MergerToken> tokens, DbEntityMerger dbEntityMerger, DataMap original, DataMap importedFromDb) {
        ChainMerger<DbEntity, DbRelationship> dbRelationshipMerger = new ChainMerger<>(
                tokenFactory, original, importedFromDb,
                new DbRelationshipMerger(tokenFactory, original, importedFromDb, skipRelationshipsTokens),
                dbEntityMerger
        );
        tokens.addAll(dbRelationshipMerger.createMergeTokens(null, null));
    }


    public static Builder builder(MergerTokenFactory tokenFactory) {
        return new Builder(tokenFactory);
    }

    public static DataMapMerger build(MergerTokenFactory tokenFactory) {
        return builder(tokenFactory).build();
    }

    public static class Builder {
        private DataMapMerger merger;

        private Builder(MergerTokenFactory tokenFactory) {
            this.merger = new DataMapMerger();
            this.merger.tokenFactory = Objects.requireNonNull(tokenFactory);
        }

        public DataMapMerger build() {

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
