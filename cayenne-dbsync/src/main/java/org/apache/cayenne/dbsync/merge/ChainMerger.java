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

import java.util.Collection;
import java.util.List;

import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.map.DataMap;

class ChainMerger<T, M> extends AbstractMerger<T, M> {

    private final AbstractMerger<T, M> merger;

    private final AbstractMerger<?, T> parentMerger;

    ChainMerger(MergerTokenFactory tokenFactory, AbstractMerger<T, M> merger, AbstractMerger<?, T> parentMerger) {
        super(tokenFactory);
        this.merger = merger;
        this.parentMerger = parentMerger;
    }

    @Override
    public List<MergerToken> createMergeTokens() {
        return createMergeTokens(null, null);
    }

    @Override
    MergerDictionaryDiff<M> createDiff(T unused1, T unused2) {
        MergerDictionaryDiff<M> diff = new MergerDictionaryDiff<>();
        MergerDictionaryDiff<T> parentDiff = parentMerger.getDiff();
        merger.setOriginalDictionary(parentMerger.getOriginalDictionary());
        for(MergerDiffPair<T> pair : parentDiff.getSame()) {
            diff.addAll(merger.createDiff(pair.getOriginal(), pair.getImported()));
        }
        return diff;
    }

    @Override
    Collection<MergerToken> createTokensForMissingOriginal(M imported) {
        return merger.createTokensForMissingOriginal(imported);
    }

    @Override
    Collection<MergerToken> createTokensForMissingImported(M original) {
        return merger.createTokensForMissingImported(original);
    }

    @Override
    Collection<MergerToken> createTokensForSame(MergerDiffPair<M> same) {
        return merger.createTokensForSame(same);
    }
}
