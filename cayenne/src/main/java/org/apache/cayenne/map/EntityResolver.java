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
package org.apache.cayenne.map;

import org.apache.cayenne.Persistent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Represents a virtual shared namespace for zero or more DataMaps, resolving the cross-references of the authored
 * mapping: an entity of one map may refer to a table or an entity of another. The runtime does not use it: it works
 * with the {@link org.apache.cayenne.map.compiled.CompiledMapping} compiled from the DataMaps.
 * <p>
 * EntityResolver is thread-safe.
 * </p>
 *
 * @since 1.1
 */
public class EntityResolver implements MappingNamespace {

    protected Collection<DataMap> maps;
    protected MappingNamespace mappingCache;

    /**
     * Creates new empty EntityResolver.
     */
    public EntityResolver() {
        this(Collections.emptyList());
    }

    /**
     * Creates new EntityResolver that indexes a collection of DataMaps.
     */
    public EntityResolver(Collection<DataMap> dataMaps) {
        this.maps = new ArrayList<>(dataMaps);
        refreshMappingCache();
    }

    /**
     * Returns all DbEntities.
     */
    public Collection<DbEntity> getDbEntities() {
        checkMappingCache();
        return mappingCache.getDbEntities();
    }

    public Collection<ObjEntity> getObjEntities() {
        checkMappingCache();
        return mappingCache.getObjEntities();
    }

    /**
     * @since 3.0
     */
    public Collection<Embeddable> getEmbeddables() {
        checkMappingCache();
        return mappingCache.getEmbeddables();
    }

    public Collection<Procedure> getProcedures() {
        checkMappingCache();
        return mappingCache.getProcedures();
    }

    public Collection<NamedQuery> getNamedQueries() {
        checkMappingCache();
        return mappingCache.getNamedQueries();
    }

    public DbEntity getDbEntity(String name) {
        checkMappingCache();
        DbEntity result = mappingCache.getDbEntity(name);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            refreshMappingCache();
            result = mappingCache.getDbEntity(name);
        }
        return result;
    }

    public ObjEntity getObjEntity(String name) {
        checkMappingCache();
        ObjEntity result = mappingCache.getObjEntity(name);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            refreshMappingCache();
            result = mappingCache.getObjEntity(name);
        }
        return result;
    }

    public Procedure getProcedure(String procedureName) {
        checkMappingCache();
        Procedure result = mappingCache.getProcedure(procedureName);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            refreshMappingCache();
            result = mappingCache.getProcedure(procedureName);
        }
        return result;
    }

    /**
     * Returns a named query or null if no query exists for a given name.
     */
    public NamedQuery getNamedQuery(String name) {
        checkMappingCache();
        NamedQuery result = mappingCache.getNamedQuery(name);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            refreshMappingCache();
            result = mappingCache.getNamedQuery(name);
        }
        return result;
    }

    /**
     * @since 3.0
     */
    public Embeddable getEmbeddable(String className) {
        checkMappingCache();
        Embeddable result = mappingCache.getEmbeddable(className);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            refreshMappingCache();
            result = mappingCache.getEmbeddable(className);
        }
        return result;
    }

    public synchronized void addDataMap(DataMap map) {
        if (!maps.contains(map)) {
            maps.add(map);
            map.setNamespace(this);
            refreshMappingCache();
        }
    }

    private void checkMappingCache() {
        if (mappingCache == null) {
            refreshMappingCache();
        }
    }

    /**
     * Refreshes entity cache to reflect the current state of the DataMaps in
     * the EntityResolver.
     *
     * @since 4.0
     */
    public void refreshMappingCache() {
        mappingCache = new ProxiedMappingNamespace() {

            @Override
            protected MappingCache createDelegate() {
                return new MappingCache(maps);
            }
        };
    }

    /**
     * Returns a DataMap matching the name.
     */
    public DataMap getDataMap(String mapName) {
        if (mapName == null) {
            return null;
        }

        for (DataMap map : maps) {
            if (mapName.equals(map.getName())) {
                return map;
            }
        }

        return null;
    }

    public synchronized void setDataMaps(Collection<DataMap> maps) {
        this.maps.clear();
        this.maps.addAll(maps);
        refreshMappingCache();
    }

    /**
     * Returns an unmodifiable collection of DataMaps.
     */
    public Collection<DataMap> getDataMaps() {
        return Collections.unmodifiableCollection(maps);
    }

    /**
     * @since 4.0
     */
    public EntityInheritanceTree getInheritanceTree(String entityName) {
        checkMappingCache();
        EntityInheritanceTree tree = mappingCache.getInheritanceTree(entityName);
        if (tree == null) {
            // since we keep inheritance trees for all entities, null means
            // unknown entity...
            // rebuild cache just in case some of the datamaps
            // have changed and now contain the required information
            refreshMappingCache();
            tree = mappingCache.getInheritanceTree(entityName);
        }
        return tree;
    }

    /**
     * Looks in the DataMap's that this object was created with for the
     * ObjEntity that maps to the services the specified class
     *
     * @return the required ObjEntity or null if there is none that matches the
     * specifier
     * @since 4.0
     */
    public ObjEntity getObjEntity(Class<?> entityClass) {
        checkMappingCache();
        ObjEntity result = mappingCache.getObjEntity(entityClass);
        if (result == null) {
            // reconstruct cache just in case some of the datamaps
            // have changed and now contain the required information
            refreshMappingCache();
            result = mappingCache.getObjEntity(entityClass);
        }
        return result;
    }

    public ObjEntity getObjEntity(Persistent object) {
        checkMappingCache();
        return mappingCache.getObjEntity(object);
    }

    public synchronized void removeDataMap(DataMap map) {
        if (maps.remove(map)) {
            refreshMappingCache();
        }
    }
}
