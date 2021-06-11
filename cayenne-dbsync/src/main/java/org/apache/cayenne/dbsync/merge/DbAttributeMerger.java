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

import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.cayenne.dbsync.merge.token.ValueForNullProvider;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.dbsync.model.DetectedDbEntity;

class DbAttributeMerger extends AbstractMerger<DbEntity, DbAttribute> {

    static int[] typesWithMaxLength = {
            Types.NCHAR, Types.NVARCHAR,
            Types.CHAR, Types.VARCHAR,
            Types.BINARY, Types.VARBINARY,
            Types.TIME, Types.TIMESTAMP
    };

    private final ValueForNullProvider valueForNull;

    DbAttributeMerger(MergerTokenFactory tokenFactory, ValueForNullProvider valueForNull) {
        super(tokenFactory);
        this.valueForNull = valueForNull;
    }

    @Override
    MergerDictionaryDiff<DbAttribute> createDiff(DbEntity original, DbEntity imported) {
        return new MergerDictionaryDiff.Builder<DbAttribute>()
                .originalDictionary(new DbAttributeDictionary(original))
                .importedDictionary(new DbAttributeDictionary(imported))
                .build();
    }

    /**
     * Add column to db
     * @param original attribute found in model but missing in db
     */
    @Override
    Collection<MergerToken> createTokensForMissingImported(DbAttribute original) {
        DbEntity originalDbEntity = original.getEntity();
        List<MergerToken> tokens = new LinkedList<>();
        tokens.add(getTokenFactory().createAddColumnToDb(originalDbEntity, original));

        // Create not null check
        if (original.isMandatory()) {
            if (valueForNull.hasValueFor(originalDbEntity, original)) {
                tokens.add(getTokenFactory().createSetValueForNullToDb(originalDbEntity, original, valueForNull));
            }
            tokens.add(getTokenFactory().createSetNotNullToDb(originalDbEntity, original));
        }

        if(original.isPrimaryKey()
                && originalDbEntity instanceof DetectedDbEntity
                && "VIEW".equals(((DetectedDbEntity) originalDbEntity).getType())) {
            // Views doesn't has PKs in a database, but if the user selects some PKs in a model, we put these keys.
            return null;
        }
        return tokens;
    }

    /**
     * Drop column in db
     * @param imported attribute found in db but missing in model
     */
    @Override
    Collection<MergerToken> createTokensForMissingOriginal(DbAttribute imported) {
        DbEntity originalDbEntity = getOriginalDictionary().getByName(imported.getEntity().getName().toUpperCase());
        return Collections.singleton(getTokenFactory().createDropColumnToDb(originalDbEntity, imported));
    }

    /**
     * Compare same attributes in model and db
     */
    @Override
    Collection<MergerToken> createTokensForSame(MergerDiffPair<DbAttribute> same) {
        List<MergerToken> tokens = new LinkedList<>();

        // isMandatory flag
        checkMandatory(same.getOriginal(), same.getImported(), tokens);
        // check type (including max length, scale and precision)
        checkType(same.getOriginal(), same.getImported(), tokens);
        // not implemented yet
        // isGenerated flag
        checkIsGenerated(same.getOriginal(), same.getImported(), tokens);

        return tokens;
    }

    private void checkMandatory(DbAttribute original, DbAttribute imported, List<MergerToken> tokens) {
        if(original.isMandatory() == imported.isMandatory()) {
            return;
        }

        DbEntity originalDbEntity = original.getEntity();
        if (original.isMandatory()) {
            if (valueForNull.hasValueFor(originalDbEntity, original)) {
                tokens.add(getTokenFactory().createSetValueForNullToDb(originalDbEntity, original, valueForNull));
            }
            tokens.add(getTokenFactory().createSetNotNullToDb(originalDbEntity, original));
        } else {
            tokens.add(getTokenFactory().createSetAllowNullToDb(originalDbEntity, original));
        }
    }

    /**
     * Check whether attributes have same type, max length, scale and precision
     * @param original attribute in model
     * @param imported attribute from db
     * @return true if attributes not same
     */
    private boolean needUpdateType(DbAttribute original, DbAttribute imported) {
        if(original.getType() != imported.getType()) {
            // Decimal and NUMERIC types are effectively equal so skip their interchange
            if( (original.getType() == Types.DECIMAL || original.getType() == Types.NUMERIC) &&
                (imported.getType() == Types.DECIMAL || imported.getType() == Types.NUMERIC)) {
                return false;
            }
            return getTokenFactory().needUpdateSpecificType(original, imported);
        }

        if(original.getMaxLength() != imported.getMaxLength()) {
            for(int type : typesWithMaxLength) {
                if(original.getType() == type) {
                    return true;
                }
            }
        }

        if(needUpdateScale(original, imported)) {
            return true;
        }

        if(original.getAttributePrecision() != imported.getAttributePrecision()) {
            return true;
        }

        return false;
    }

    private boolean needUpdateScale(DbAttribute original, DbAttribute imported) {
        if(original.getScale() == imported.getScale()) {
            return false;
        }

        // -1 and 0 are actually equal values for scale
        if((original.getScale() == -1 || original.getScale() == 0)
                && (imported.getScale() == -1 || imported.getScale() == 0)) {
            return false;
        }

        return true;
    }

    private void checkType(DbAttribute original, DbAttribute imported, List<MergerToken> tokens) {
        if(!needUpdateType(original, imported)) {
            return;
        }
        DbEntity originalDbEntity = original.getEntity();
        tokens.add(getTokenFactory().createSetColumnTypeToDb(originalDbEntity, imported, original));
    }

    private void checkIsGenerated(DbAttribute original, DbAttribute imported, List<MergerToken> tokens) {
        if(original.isGenerated() == imported.isGenerated()) {
            return;
        }

        tokens.add(getTokenFactory().createSetGeneratedFlagToDb(original.getEntity(), original, original.isGenerated()));
    }

    @Override
    public List<MergerToken> createMergeTokens() {
        throw new UnsupportedOperationException();
    }
}
