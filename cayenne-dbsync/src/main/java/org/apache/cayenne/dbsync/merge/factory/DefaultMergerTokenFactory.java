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
package org.apache.cayenne.dbsync.merge.factory;

import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.merge.token.ValueForNullProvider;
import org.apache.cayenne.dbsync.merge.token.db.AddColumnToDb;
import org.apache.cayenne.dbsync.merge.token.db.AddRelationshipToDb;
import org.apache.cayenne.dbsync.merge.token.db.CreateTableToDb;
import org.apache.cayenne.dbsync.merge.token.db.DropColumnToDb;
import org.apache.cayenne.dbsync.merge.token.db.DropRelationshipToDb;
import org.apache.cayenne.dbsync.merge.token.db.DropTableToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetAllowNullToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetColumnTypeToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetNotNullToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetPrimaryKeyToDb;
import org.apache.cayenne.dbsync.merge.token.db.SetValueForNullToDb;
import org.apache.cayenne.dbsync.merge.token.model.AddColumnToModel;
import org.apache.cayenne.dbsync.merge.token.model.AddRelationshipToModel;
import org.apache.cayenne.dbsync.merge.token.model.CreateTableToModel;
import org.apache.cayenne.dbsync.merge.token.model.DropColumnToModel;
import org.apache.cayenne.dbsync.merge.token.model.DropRelationshipToModel;
import org.apache.cayenne.dbsync.merge.token.model.DropTableToModel;
import org.apache.cayenne.dbsync.merge.token.model.SetAllowNullToModel;
import org.apache.cayenne.dbsync.merge.token.model.SetColumnTypeToModel;
import org.apache.cayenne.dbsync.merge.token.model.SetNotNullToModel;
import org.apache.cayenne.dbsync.merge.token.model.SetPrimaryKeyToModel;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;

import java.util.Collection;

/**
 * @since 4.0
 */
public class DefaultMergerTokenFactory implements MergerTokenFactory {

    @Override
    public MergerToken createCreateTableToModel(DbEntity entity) {
        return new CreateTableToModel(entity);
    }

    @Override
    public MergerToken createCreateTableToDb(DbEntity entity) {
        return new CreateTableToDb(entity);
    }

    @Override
    public MergerToken createDropTableToModel(DbEntity entity) {
        return new DropTableToModel(entity);
    }

    @Override
    public MergerToken createDropTableToDb(DbEntity entity) {
        return new DropTableToDb(entity);
    }

    @Override
    public MergerToken createAddColumnToModel(DbEntity entity, DbAttribute column) {
        return new AddColumnToModel(entity, column);
    }

    @Override
    public MergerToken createAddColumnToDb(DbEntity entity, DbAttribute column) {
        return new AddColumnToDb(entity, column);
    }

    @Override
    public MergerToken createDropColumnToModel(DbEntity entity, DbAttribute column) {
        return new DropColumnToModel(entity, column);
    }

    @Override
    public MergerToken createDropColumnToDb(DbEntity entity, DbAttribute column) {
        return new DropColumnToDb(entity, column);
    }

    @Override
    public MergerToken createSetNotNullToModel(DbEntity entity, DbAttribute column) {
        return new SetNotNullToModel(entity, column);
    }

    @Override
    public MergerToken createSetNotNullToDb(DbEntity entity, DbAttribute column) {
        return new SetNotNullToDb(entity, column);
    }

    @Override
    public MergerToken createSetAllowNullToModel(DbEntity entity, DbAttribute column) {
        return new SetAllowNullToModel(entity, column);
    }

    @Override
    public MergerToken createSetAllowNullToDb(DbEntity entity, DbAttribute column) {
        return new SetAllowNullToDb(entity, column);
    }

    @Override
    public MergerToken createSetValueForNullToDb(DbEntity entity, DbAttribute column, ValueForNullProvider valueForNullProvider) {
        return new SetValueForNullToDb(entity, column, valueForNullProvider);
    }

    @Override
    public MergerToken createSetColumnTypeToModel(
            DbEntity entity,
            DbAttribute columnOriginal,
            DbAttribute columnNew) {
        return new SetColumnTypeToModel(entity, columnOriginal, columnNew);
    }

    @Override
    public MergerToken createSetColumnTypeToDb(
            DbEntity entity,
            DbAttribute columnOriginal,
            DbAttribute columnNew) {
        return new SetColumnTypeToDb(entity, columnOriginal, columnNew);
    }

    @Override
    public MergerToken createAddRelationshipToDb(DbEntity entity, DbRelationship rel) {
        return new AddRelationshipToDb(entity, rel);
    }

    @Override
    public MergerToken createAddRelationshipToModel(DbEntity entity, DbRelationship rel) {
        return new AddRelationshipToModel(entity, rel);
    }

    @Override
    public MergerToken createDropRelationshipToDb(DbEntity entity, DbRelationship rel) {
        return new DropRelationshipToDb(entity, rel);
    }

    @Override
    public MergerToken createDropRelationshipToModel(DbEntity entity, DbRelationship rel) {
        return new DropRelationshipToModel(entity, rel);
    }

    @Override
    public MergerToken createSetPrimaryKeyToDb(
            DbEntity entity,
            Collection<DbAttribute> primaryKeyOriginal,
            Collection<DbAttribute> primaryKeyNew,
            String detectedPrimaryKeyName) {
        return new SetPrimaryKeyToDb(
                entity,
                primaryKeyOriginal,
                primaryKeyNew,
                detectedPrimaryKeyName);
    }

    @Override
    public MergerToken createSetPrimaryKeyToModel(
            DbEntity entity,
            Collection<DbAttribute> primaryKeyOriginal,
            Collection<DbAttribute> primaryKeyNew,
            String detectedPrimaryKeyName) {
        return new SetPrimaryKeyToModel(
                entity,
                primaryKeyOriginal,
                primaryKeyNew,
                detectedPrimaryKeyName);
    }
}
