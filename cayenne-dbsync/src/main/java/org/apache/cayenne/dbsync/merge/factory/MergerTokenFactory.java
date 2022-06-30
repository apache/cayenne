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

import java.util.Collection;
import java.util.function.Function;

import org.apache.cayenne.dbsync.merge.token.MergerToken;
import org.apache.cayenne.dbsync.merge.token.ValueForNullProvider;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Procedure;

public interface MergerTokenFactory {

    MergerToken createCreateTableToModel(DbEntity entity);

    MergerToken createCreateTableToDb(DbEntity entity);

    MergerToken createDropTableToModel(DbEntity entity);

    MergerToken createDropTableToDb(DbEntity entity);

    MergerToken createAddColumnToModel(DbEntity entity, DbAttribute column);

    MergerToken createAddColumnToDb(DbEntity entity, DbAttribute column);

    MergerToken createDropColumnToModel(DbEntity entity, DbAttribute column);

    MergerToken createDropColumnToDb(DbEntity entity, DbAttribute column);

    MergerToken createSetNotNullToModel(DbEntity entity, DbAttribute column);

    MergerToken createSetNotNullToDb(DbEntity entity, DbAttribute column);

    MergerToken createSetAllowNullToModel(DbEntity entity, DbAttribute column);

    MergerToken createSetAllowNullToDb(DbEntity entity, DbAttribute column);

    MergerToken createSetValueForNullToDb(DbEntity entity,
                                          DbAttribute column,
                                          ValueForNullProvider valueForNullProvider);

    MergerToken createSetColumnTypeToModel(
            DbEntity entity,
            DbAttribute columnOriginal,
            DbAttribute columnNew);

    MergerToken createSetColumnTypeToDb(
            DbEntity entity,
            DbAttribute columnOriginal,
            DbAttribute columnNew);

    MergerToken createAddRelationshipToDb(DbEntity entity, DbRelationship rel);

    MergerToken createAddRelationshipToModel(DbEntity entity, DbRelationship rel);

    MergerToken createDropRelationshipToDb(DbEntity entity, DbRelationship rel);

    MergerToken createDropRelationshipToModel(DbEntity entity, DbRelationship rel);

    MergerToken createAddProcedureToDb(Procedure procedure);

    MergerToken createAddProcedureToModel(Procedure procedure);

    MergerToken createDropProcedureToDb(Procedure procedure);

    MergerToken createDropProcedureToModel(Procedure procedure);

    MergerToken createSetPrimaryKeyToDb(
            DbEntity entity,
            Collection<DbAttribute> primaryKeyOriginal,
            Collection<DbAttribute> primaryKeyNew,
            String detectedPrimaryKeyName,
            Function<String, String> nameConverter);

    MergerToken createSetPrimaryKeyToModel(
            DbEntity entity,
            Collection<DbAttribute> primaryKeyOriginal,
            Collection<DbAttribute> primaryKeyNew,
            String detectedPrimaryKeyName,
            Function<String, String> nameConverter);

    MergerToken createSetGeneratedFlagToDb(DbEntity entity, DbAttribute column, boolean isGenerated);

    MergerToken createSetGeneratedFlagToModel(DbEntity entity, DbAttribute column, boolean isGenerated);

    boolean needUpdateSpecificType(DbAttribute columnOriginal, DbAttribute columnNew);
}
