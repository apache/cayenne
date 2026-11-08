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
package org.apache.cayenne.map.compiled;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;

import java.util.Collection;
import java.util.Map;

/**
 * The runtime view of the mapping: an immutable graph of compiled entities, tables, procedures and queries built once
 * from the authored DataMaps by a {@link MappingCompiler}, with all references resolved and all derived facts
 * precomputed. A DataDomain holds the current mapping and replaces it as a whole when its DataMaps change.
 *
 * @since 6.0
 */
public final class CompiledMapping {

    // marks a class name shared by more than one entity
    private static final CompiledEntity DUPLICATE_CLASS = new CompiledEntity();

    private final CompiledSchema schema;
    private final Map<String, CompiledEntity> entities;
    private final Map<Class<?>, CompiledEntity> entitiesByClass;
    private final Map<String, CompiledEmbeddable> embeddables;
    private final Map<String, CompiledQuery> queries;

    CompiledMapping(
            CompiledSchema schema,
            Map<String, CompiledEntity> entities,
            Map<Class<?>, CompiledEntity> entitiesByClass,
            Map<String, CompiledEmbeddable> embeddables,
            Map<String, CompiledQuery> queries) {

        this.schema = schema;
        this.entities = entities;
        this.entitiesByClass = entitiesByClass;
        this.embeddables = embeddables;
        this.queries = queries;
    }

    /**
     * Returns a mapping with no maps, the mapping of a domain that was given none.
     */
    static CompiledMapping empty() {
        return new CompiledMapping(CompiledSchema.empty(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    static CompiledEntity duplicateClassMarker() {
        return DUPLICATE_CLASS;
    }

    /**
     * Returns the schema this mapping is built on: its maps, tables and procedures.
     */
    public CompiledSchema schema() {
        return schema;
    }

    public Collection<CompiledEntity> entities() {
        return entities.values();
    }

    /**
     * Returns an entity by name, or null if there is none.
     */
    public CompiledEntity entity(String name) {
        return name != null ? entities.get(name) : null;
    }

    /**
     * Returns the entity bound to the class, or null if there is none, which is also the case for an entity whose
     * class could not be loaded when the mapping was compiled. Fails if more than one entity is bound to the class.
     */
    public CompiledEntity entity(Class<?> entityClass) {
        CompiledEntity entity = entitiesByClass.get(entityClass);
        if (entity == DUPLICATE_CLASS) {
            throw new CayenneRuntimeException("Can't perform lookup. There is more than one ObjEntity mapped to %s",
                    entityClass.getName());
        }
        return entity;
    }

    /**
     * Returns the entity of a persistent object: the entity of its id, or, for an object without an id, the entity
     * of its class.
     */
    public CompiledEntity entity(Persistent object) {
        ObjectId id = object.getObjectId();
        return id != null ? entity(id.getEntityName()) : entity(object.getClass());
    }

    public Collection<CompiledEmbeddable> embeddables() {
        return embeddables.values();
    }

    /**
     * Returns an embeddable by name, which is the name of its class, or null if there is none.
     */
    public CompiledEmbeddable embeddable(String name) {
        return name != null ? embeddables.get(name) : null;
    }

    public Collection<CompiledQuery> queries() {
        return queries.values();
    }

    /**
     * Returns a query mapped under the name, or null if there is none.
     */
    public CompiledQuery query(String name) {
        return name != null ? queries.get(name) : null;
    }

    @Override
    public String toString() {
        return "CompiledMapping" + schema.maps().stream().map(CompiledDataMap::name).toList();
    }
}
