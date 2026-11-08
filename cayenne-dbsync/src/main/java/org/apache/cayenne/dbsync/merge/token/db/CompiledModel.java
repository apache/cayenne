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

package org.apache.cayenne.dbsync.merge.token.db;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.MappingNamespace;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledJoin;
import org.apache.cayenne.map.compiled.CompiledSchema;
import org.apache.cayenne.map.compiled.SchemaCompiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compiles the authored model held by the "to DB" merge tokens on demand, so that it can be passed to the DDL methods
 * of {@link DbAdapter}, which take the compiled mapping. The merger compares the database with the authored model,
 * and the tokens of one merge keep mutating that model, so nothing is cached here: every call compiles the maps of
 * the entity's namespace afresh and looks the object up by name. Compiling is cheap, and the tokens are run rarely.
 *
 * @since 6.0
 */
public final class CompiledModel {

    private CompiledModel() {
    }

    /**
     * Returns the compiled form of the authored table.
     */
    public static CompiledTable table(DbEntity entity) {
        CompiledTable table = compile(entity).table(entity.getName());
        if (table == null) {
            throw new CayenneRuntimeException("DbEntity '%s' is missing from the compiled mapping", entity.getName());
        }
        return table;
    }

    /**
     * Returns the compiled form of the authored column. The column must belong to a DbEntity.
     */
    public static CompiledColumn column(DbAttribute attribute) {
        DbEntity entity = attribute.getEntity();
        if (entity == null) {
            throw new CayenneRuntimeException("DbAttribute '%s' is not attached to a DbEntity", attribute.getName());
        }
        CompiledColumn column = table(entity).column(attribute.getName());
        if (column == null) {
            throw new CayenneRuntimeException("DbAttribute '%s.%s' is missing from the compiled mapping",
                    entity.getName(), attribute.getName());
        }
        return column;
    }

    /**
     * Returns the compiled form of the authored relationship. The relationship must belong to a DbEntity, and have a
     * resolvable target and at least one join, as the compiler skips the relationships that don't.
     */
    public static CompiledJoin relationship(DbRelationship relationship) {
        DbEntity entity = relationship.getSourceEntity();
        if (entity == null) {
            throw new CayenneRuntimeException("DbRelationship '%s' is not attached to a DbEntity",
                    relationship.getName());
        }
        CompiledJoin compiled = table(entity).join(relationship.getName());
        if (compiled == null) {
            throw new CayenneRuntimeException("DbRelationship '%s.%s' is missing from the compiled mapping: it has no "
                    + "target entity or no joins", entity.getName(), relationship.getName());
        }
        return compiled;
    }

    private static CompiledSchema compile(DbEntity entity) {
        DataMap map = entity.getDataMap();
        if (map == null) {
            // a table being created before it is added to the model: compile it on its own
            return compileDetached(entity);
        }

        // a map in a shared namespace may have relationships to the tables of its sibling maps
        Collection<DataMap> maps = map.getNamespace() instanceof EntityResolver resolver
                ? new ArrayList<>(resolver.getDataMaps())
                : List.of(map);

        // the compiler links the maps through a namespace of its own; keep the authored namespace in place
        Map<DataMap, MappingNamespace> namespaces = new IdentityHashMap<>();
        for (DataMap m : maps) {
            namespaces.put(m, m.getNamespace());
        }
        try {
            return SchemaCompiler.compile(maps);
        } finally {
            namespaces.forEach(DataMap::setNamespace);
        }
    }

    private static CompiledSchema compileDetached(DbEntity entity) {
        DataMap map = new DataMap("__detached__");
        map.addDbEntity(entity);
        try {
            return SchemaCompiler.compile(List.of(map));
        } finally {
            entity.setDataMap(null);
        }
    }
}
