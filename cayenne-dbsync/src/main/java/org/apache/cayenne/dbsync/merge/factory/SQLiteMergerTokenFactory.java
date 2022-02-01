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

package org.apache.cayenne.dbsync.merge.factory;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.merge.token.ValueForNullProvider;
import org.apache.cayenne.dbsync.merge.token.db.AddRelationshipToDb;
import org.apache.cayenne.dbsync.merge.token.db.DropRelationshipToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetAllowNullToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetColumnTypeToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetGeneratedFlagToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetNotNullToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetPrimaryKeyToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetValueForNullToDb;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * @since 4.3
 */
public class SQLiteMergerTokenFactory extends DefaultMergerTokenFactory {

    @Override
    public MergerToken createSetColumnTypeToDb(
            final DbEntity entity,
            final DbAttribute columnOriginal,
            final DbAttribute columnNew) {
        return new SetColumnTypeToDb(entity, columnOriginal, columnNew) {

            @Override
            public List<String> createSql(DbAdapter adapter) {
                throw new CayenneRuntimeException("SQLite doesn't support altering column type.");
            }

        };
    }

    @Override
    public MergerToken createSetNotNullToDb(DbEntity entity, DbAttribute column) {
        return new SetNotNullToDb(entity, column) {

            @Override
            public List<String> createSql(DbAdapter adapter) {
                throw new CayenneRuntimeException("SQLite doesn't support adding constrains to existing column.");
            }

        };
    }

    @Override
    public MergerToken createSetAllowNullToDb(DbEntity entity, DbAttribute column) {
        return new SetAllowNullToDb(entity, column) {

            @Override
            public List<String> createSql(DbAdapter adapter) {
                throw new CayenneRuntimeException("SQLite doesn't support changing constrains.");
            }

        };
    }

    @Override
    public MergerToken createSetValueForNullToDb(DbEntity entity, DbAttribute column, ValueForNullProvider valueForNullProvider) {
        return new SetValueForNullToDb(entity, column, valueForNullProvider){

            @Override
            public List<String> createSql(DbAdapter adapter) {
                throw new CayenneRuntimeException("SQLite doesn't support adding constrains to existing column.");
            }

        };
    }

    @Override
    public MergerToken createSetPrimaryKeyToDb(
            DbEntity entity,
            Collection<DbAttribute> primaryKeyOriginal,
            Collection<DbAttribute> primaryKeyNew,
            String detectedPrimaryKeyName,
            Function<String, String> nameConverter) {
        return new SetPrimaryKeyToDb(
                entity,
                primaryKeyOriginal,
                primaryKeyNew,
                detectedPrimaryKeyName,
                nameConverter){

            @Override
            public List<String> createSql(DbAdapter adapter) {
                throw new CayenneRuntimeException("SQLite doesn't support adding constrains to existing column.");
            }

        };
    }

    @Override
    public MergerToken createSetGeneratedFlagToDb(DbEntity entity, DbAttribute column, boolean isGenerated) {
        return new SetGeneratedFlagToDb(entity, column, isGenerated){

            @Override
            public List<String> createSql(DbAdapter adapter) {
                throw new CayenneRuntimeException("SQLite doesn't support adding constrains to existing column.");
            }

        };
    }

    @Override
    public MergerToken createDropRelationshipToDb(DbEntity entity, DbRelationship rel) {
        return new DropRelationshipToDb(entity, rel){

            @Override
            public List<String> createSql(DbAdapter adapter) {
                throw new CayenneRuntimeException("SQLite doesn't support removing constrains.");
            }

        };
    }

    @Override
    public MergerToken createAddRelationshipToDb(DbEntity entity, DbRelationship rel) {
        return new AddRelationshipToDb(entity, rel){

            @Override
            public List<String> createSql(DbAdapter adapter) {
                throw new CayenneRuntimeException("SQLite doesn't support adding FK to existing column.");
            }

        };
    }
}