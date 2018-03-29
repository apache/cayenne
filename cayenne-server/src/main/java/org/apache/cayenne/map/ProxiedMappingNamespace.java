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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.cayenne.Persistent;

/**
 * A proxy for lazy on-demand initialization of the mapping cache.
 */
abstract class ProxiedMappingNamespace implements MappingNamespace {

    private volatile MappingNamespace delegate;

    MappingNamespace getDelegate() {

        if (delegate == null) {

            synchronized (this) {
                if (delegate == null) {
                    delegate = createDelegate();
                }
            }
        }

        return delegate;
    }

    protected abstract MappingNamespace createDelegate();

    public Embeddable getEmbeddable(String className) {
        return getDelegate().getEmbeddable(className);
    }

    public SQLResult getResult(String name) {
        return getDelegate().getResult(name);
    }

    public EntityInheritanceTree getInheritanceTree(String entityName) {
        return getDelegate().getInheritanceTree(entityName);
    }

    public Procedure getProcedure(String procedureName) {
        return getDelegate().getProcedure(procedureName);
    }

    public QueryDescriptor getQueryDescriptor(String queryName) {
        return getDelegate().getQueryDescriptor(queryName);
    }

    public DbEntity getDbEntity(String name) {
        return getDelegate().getDbEntity(name);
    }

    public ObjEntity getObjEntity(Class<?> entityClass) {
        return getDelegate().getObjEntity(entityClass);
    }
    
    public ObjEntity getObjEntity(Persistent object) {
        return getDelegate().getObjEntity(object);
    }

    public ObjEntity getObjEntity(String name) {
        return getDelegate().getObjEntity(name);
    }

    public Collection<DbEntity> getDbEntities() {
        return getDelegate().getDbEntities();
    }

    public Collection<ObjEntity> getObjEntities() {
        return getDelegate().getObjEntities();
    }

    public Collection<Procedure> getProcedures() {
        return getDelegate().getProcedures();
    }

    public Collection<QueryDescriptor> getQueryDescriptors() {
        return getDelegate().getQueryDescriptors();
    }

    public Collection<Embeddable> getEmbeddables() {
        return getDelegate().getEmbeddables();
    }
    
    public Collection<SQLResult> getResults() {
        return getDelegate().getResults();
    }

}
