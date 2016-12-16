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

import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.token.EmptyValueForNullProvider;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.merge.token.ValueForNullProvider;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.dbsync.reverse.filters.PatternFilter;
import org.apache.cayenne.dbsync.reverse.filters.TableFilter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Synchronization of data base store and Cayenne model.
 */
public class DataMapMerger implements Merger<DataMap> {

    private MergerTokenFactory tokenFactory;
    private ValueForNullProvider valueForNull;
    private boolean skipRelationshipsTokens;
    private boolean skipPKTokens;
    private FiltersConfig filters;
    private DbEntityMerger dbEntityMerger;
    private List<AbstractMerger<?, ?>> mergerList = new ArrayList<>();

    private DataMapMerger() {
    }

    /**
     * Create List of MergerToken that represent the difference between two {@link DataMap} objects.
     */
    @Override
    public List<MergerToken> createMergeTokens(DataMap original, DataMap importedFromDb) {
        prepare(original, importedFromDb);

        createDbEntityMerger(original, importedFromDb);
        createRelationshipMerger();
        createAttributeMerger();

        return createTokens();
    }

    private void prepare(DataMap original, DataMap imported) {
        imported.setQuotingSQLIdentifiers(original.isQuotingSQLIdentifiers());
    }

    private List<MergerToken> createTokens() {
        List<MergerToken> tokens = new ArrayList<>();
        for(AbstractMerger<?, ?> merger : mergerList) {
            tokens.addAll(merger.createMergeTokens());
        }
        Collections.sort(tokens);
        return tokens;
    }

    private void createDbEntityMerger(DataMap original, DataMap imported) {
        dbEntityMerger = new DbEntityMerger(tokenFactory, original, imported, filters, skipPKTokens);
        mergerList.add(dbEntityMerger);
    }

    private void createAttributeMerger() {
        ChainMerger<DbEntity, DbAttribute> dbAttributeMerger = new ChainMerger<>(
                tokenFactory,
                new DbAttributeMerger(tokenFactory, valueForNull),
                dbEntityMerger
        );
        mergerList.add(dbAttributeMerger);
    }

    private void createRelationshipMerger() {
        ChainMerger<DbEntity, DbRelationship> dbRelationshipMerger = new ChainMerger<>(
                tokenFactory,
                new DbRelationshipMerger(tokenFactory, skipRelationshipsTokens),
                dbEntityMerger
        );
        mergerList.add(dbRelationshipMerger);
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
