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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfig;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.map.ProcedureParameter;

/**
 * @since 4.2
 */
public class ProcedureMerger extends AbstractMerger<DataMap, Procedure> {

    private final FiltersConfig filtersConfig;
    private DataMap originalDataMap;
    private DataMap importedDataMap;

    ProcedureMerger(MergerTokenFactory tokenFactory, DataMap original, DataMap imported,
                   FiltersConfig filtersConfig) {
        super(tokenFactory);
        this.filtersConfig = filtersConfig;
        originalDataMap = original;
        importedDataMap = imported;
    }

    @Override
    public List<MergerToken> createMergeTokens() {
        return createMergeTokens(originalDataMap, importedDataMap);
    }

    @Override
    MergerDictionaryDiff<Procedure> createDiff(DataMap original, DataMap imported) {
        return new MergerDictionaryDiff.Builder<Procedure>()
                .originalDictionary(new ProcedureDictionary(original, filtersConfig))
                .importedDictionary(new ProcedureDictionary(imported, filtersConfig))
                .build();
    }

    @Override
    Collection<MergerToken> createTokensForMissingOriginal(Procedure imported) {
        return Collections.singletonList(getTokenFactory().createDropProcedureToDb(imported));
    }

    @Override
    Collection<MergerToken> createTokensForMissingImported(Procedure original) {
        return Collections.singletonList(getTokenFactory().createAddProcedureToDb(original));
    }

    @Override
    Collection<MergerToken> createTokensForSame(MergerDiffPair<Procedure> same) {
        Procedure original = same.getOriginal();
        Procedure imported = same.getImported();
        if(needToCreateTokens(original, imported)) {
            List<MergerToken> tokens = new ArrayList<>();
            tokens.add(getTokenFactory().createAddProcedureToDb(original));
            tokens.add(getTokenFactory().createDropProcedureToDb(imported));
            return tokens;
        }
        return null;
    }

    private boolean needToCreateTokens(Procedure original, Procedure imported) {
        List<ProcedureParameter> originalParams = original.getCallParameters();
        List<ProcedureParameter> importedParams = imported.getCallParameters();
        if(originalParams.size() != importedParams.size()) {
            return true;
        }
        for(ProcedureParameter oP : originalParams) {
            boolean found = false;
            for(ProcedureParameter iP : importedParams) {
                if(oP.getName().equals(iP.getName()) &&
                        oP.getType() == iP.getType() &&
                        oP.getPrecision() == iP.getPrecision() &&
                        oP.getMaxLength() == iP.getMaxLength() &&
                        oP.getDirection() == iP.getDirection()) {
                    found = true;
                    break;
                }
            }
            if(!found) {
                return true;
            }
        }
        return false;
    }

}
