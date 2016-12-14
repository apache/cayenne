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
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.map.DataMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

abstract class AbstractMerger<T, M> implements Merger<T> {

    private MergerDictionaryDiff<M> diff;
    private MergerTokenFactory tokenFactory;
    DataMap originalDataMap;
    DataMap importedDataMap;

    AbstractMerger(MergerTokenFactory tokenFactory, DataMap original, DataMap imported) {
        this.tokenFactory = tokenFactory;
        this.originalDataMap = original;
        this.importedDataMap = imported;
    }

    @Override
    public List<MergerToken> createMergeTokens(T original, T imported) {
        diff = createDiff(original, imported);

        List<MergerToken> tokens = new ArrayList<>();

        for(MergerDiffPair<M> pair : diff.getMissing()) {
            Collection<MergerToken> tokensForMissing = createTokensForMissing(pair);
            if(tokensForMissing != null) {
                tokens.addAll(tokensForMissing);
            }
        }

        for(MergerDiffPair<M> pair : diff.getSame()) {
            Collection<MergerToken> tokensForSame = createTokensForSame(pair);
            if(tokensForSame != null) {
                tokens.addAll(tokensForSame);
            }
        }

        return tokens;
    }

    MergerDictionaryDiff<M> getDiff() {
        return diff;
    }

    MergerTokenFactory getTokenFactory() {
        return tokenFactory;
    }

    private Collection<MergerToken> createTokensForMissing(MergerDiffPair<M> missing) {
        if(missing.getOriginal() == null) {
            return createTokensForMissingOriginal(missing.getImported());
        } else {
            return createTokensForMissingImported(missing.getOriginal());
        }
    }

    public abstract List<MergerToken> createMergeTokens();

    abstract MergerDictionaryDiff<M> createDiff(T original, T imported);

    abstract Collection<MergerToken> createTokensForMissingOriginal(M imported);

    abstract Collection<MergerToken> createTokensForMissingImported(M original);

    abstract Collection<MergerToken> createTokensForSame(MergerDiffPair<M> same);

}
