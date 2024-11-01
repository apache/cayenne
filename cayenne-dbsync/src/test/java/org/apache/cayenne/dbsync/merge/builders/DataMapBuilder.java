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
package org.apache.cayenne.dbsync.merge.builders;

import java.util.Collections;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;

/**
 * @since 4.0.
 */
public class DataMapBuilder extends DefaultBuilder<DataMap> {

    public DataMapBuilder() {
        this(new DataMap());
    }

    public DataMapBuilder(DataMap dataMap) {
        super(dataMap);
    }

    public DataMapBuilder with(DbEntity ... entities) {
        for (DbEntity entity : entities) {
            obj.addDbEntity(entity);
        }

        return this;
    }

    public DataMapBuilder with(DbEntityBuilder ... entities) {
        for (DbEntityBuilder entity : entities) {
            obj.addDbEntity(entity.build());
        }

        return this;
    }

    public DataMapBuilder withDbEntities(int count) {
        for (int i = 0; i < count; i++) {
            obj.addDbEntity(ObjectMother.dbEntity().random());
        }

        return this;
    }

    public DataMapBuilder with(ObjEntity... entities) {
        for (ObjEntity entity : entities) {
            obj.addObjEntity(entity);
        }

        return this;
    }

    public DataMapBuilder with(ObjEntityBuilder ... entities) {
        for (ObjEntityBuilder entity : entities) {
            obj.addObjEntity(entity.build());
        }

        return this;
    }

    public DataMapBuilder withObjEntities(int count) {
        for (int i = 0; i < count; i++) {
            obj.addObjEntity(ObjectMother.objEntity().random());
        }

        return this;
    }

    public DataMapBuilder join(String from, String to, boolean isFK) {
        return join(null, from, to,isFK);
    }

    public DataMapBuilder join(String name, String from, String to, boolean isFK) {
        String[] fromSplit = from.split("\\.");
        DbEntity fromEntity = obj.getDbEntity(fromSplit[0]);
        if (fromEntity == null) {
            throw new IllegalArgumentException("Entity '" + fromSplit[0] + "' is undefined");
        }

        String[] toSplit = to.split("\\.");

        fromEntity.addRelationship(new DbRelationshipBuilder(name)
                .from(fromEntity, fromSplit[1])
                .to(toSplit[0], toSplit[1])
                .fK(isFK)
                .build());

        return this;
    }

    public DataMapBuilder with(ProcedureBuilder... procedures) {
        for(ProcedureBuilder builder : procedures) {
            obj.addProcedure(builder.build());
        }

        return this;
    }

    public DataMapBuilder with(Procedure... procedures) {
        for(Procedure procedure : procedures) {
            obj.addProcedure(procedure);
        }

        return this;
    }

    public DataMap build() {
        if (obj.getNamespace() == null) {
            obj.setNamespace(new EntityResolver(Collections.singleton(obj)));
        }

        return obj;
    }

    @Override
    public DataMap random() {
        if (dataFactory.chance(90)) {
            withDbEntities(dataFactory.getNumberUpTo(10));
        }


        return build();
    }
}
