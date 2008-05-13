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
package org.apache.cayenne.merge;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;

/**
 * All {@link MergerToken}s should be created from a {@link MergerFactory} obtained from
 * {@link DbAdapter#mergerFactory()} so that the {@link DbAdapter} are able to provide
 * {@link MergerToken} subclasses.
 * 
 * @see DbAdapter#mergerFactory()
 */
public class MergerFactory {

    public MergerToken createCreateTableToModel(DbEntity entity) {
        return new CreateTableToModel(entity);
    }

    public MergerToken createCreateTableToDb(DbEntity entity) {
        return new CreateTableToDb(entity);
    }

    public MergerToken createDropTableToModel(DbEntity entity) {
        return new DropTableToModel(entity);
    }

    public MergerToken createDropTableToDb(DbEntity entity) {
        return new DropTableToDb(entity);
    }

    public MergerToken createAddColumnToModel(DbEntity entity, DbAttribute column) {
        return new AddColumnToModel(entity, column);
    }

    public MergerToken createAddColumnToDb(DbEntity entity, DbAttribute column) {
        return new AddColumnToDb(entity, column);
    }

    public MergerToken createDropColumToModel(DbEntity entity, DbAttribute column) {
        return new DropColumnToModel(entity, column);
    }

    public MergerToken createDropColumToDb(DbEntity entity, DbAttribute column) {
        return new DropColumnToDb(entity, column);
    }

    public MergerToken createSetNotNullToModel(DbEntity entity, DbAttribute column) {
        return new SetNotNullToModel(entity, column);
    }

    public MergerToken createSetNotNullToDb(DbEntity entity, DbAttribute column) {
        return new SetNotNullToDb(entity, column);
    }

    public MergerToken createSetAllowNullToModel(DbEntity entity, DbAttribute column) {
        return new SetAllowNullToModel(entity, column);
    }

    public MergerToken createSetAllowNullToDb(DbEntity entity, DbAttribute column) {
        return new SetAllowNullToDb(entity, column);
    }

    public MergerToken createSetColumnTypeToModel(
            DbEntity entity,
            DbAttribute columnOriginal,
            DbAttribute columnNew) {
        return new SetColumnTypeToModel(entity, columnOriginal, columnNew);
    }

    public MergerToken createSetColumnTypeToDb(
            DbEntity entity,
            DbAttribute columnOriginal,
            DbAttribute columnNew) {
        return new SetColumnTypeToDb(entity, columnOriginal, columnNew);
    }
    
    public MergerToken createAddRelationshipToDb(DbEntity entity, DbRelationship rel) {
        return new AddRelationshipToDb(entity, rel);
    }

    public MergerToken createAddRelationshipToModel(DbEntity entity, DbRelationship rel) {
        return new AddRelationshipToModel(entity, rel);
    }

    public MergerToken createDropRelationshipToDb(DbEntity entity, DbRelationship rel) {
        return new DropRelationshipToDb(entity, rel);
    }

    public MergerToken createDropRelationshipToModel(DbEntity entity, DbRelationship rel) {
        return new DropRelationshipToModel(entity, rel);
    }
}
