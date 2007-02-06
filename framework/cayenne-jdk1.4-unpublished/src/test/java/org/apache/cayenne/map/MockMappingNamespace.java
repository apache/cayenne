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

package org.apache.cayenne.map;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.MappingNamespace;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.query.Query;

/**
 * @author Andrus Adamchik
 */
public class MockMappingNamespace implements MappingNamespace {
    private Map dbEntities = new HashMap();
    private Map objEntities = new HashMap();
    private Map queries = new HashMap();
    private Map procedures = new HashMap();
    
    public Embeddable getEmbeddable(String className) {
        return null;
    }
    
    public EntityListener getEntityListener(String className) {
        return null;
    }

    public void addDbEntity(DbEntity entity) {
        dbEntities.put(entity.getName(), entity);
    }

    public void addObjEntity(ObjEntity entity) {
        objEntities.put(entity.getName(), entity);
    }

    public void addQuery(Query query) {
        queries.put(query.getName(), query);
    }

    public void addProcedure(Procedure procedure) {
        procedures.put(procedure.getName(), procedure);
    }

    public DbEntity getDbEntity(String name) {
        return (DbEntity) dbEntities.get(name);
    }

    public ObjEntity getObjEntity(String name) {
        return (ObjEntity) objEntities.get(name);
    }

    public Procedure getProcedure(String name) {
        return (Procedure) procedures.get(name);
    }

    public Query getQuery(String name) {
        return (Query) queries.get(name);
    }

    public Collection getDbEntities() {
        return dbEntities.values();
    }

    public Collection getObjEntities() {
        return objEntities.values();
    }

    public Collection getProcedures() {
        return procedures.values();
    }

    public Collection getQueries() {
        return queries.values();
    }
}
