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
package org.apache.cayenne.access.select;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.PathComponent;

/**
 * @since 3.0
 */
abstract class MappedColumnBuilder {

    protected List<EntitySelectColumn> columns;
    protected Map<String, Integer> columnMap;
    protected ExtendedTypeMap extendedTypes;

    MappedColumnBuilder(ExtendedTypeMap extendedTypes) {
        this.columns = new ArrayList<EntitySelectColumn>();
        this.columnMap = new HashMap<String, Integer>();
        this.extendedTypes = extendedTypes;
    }

    /**
     * Appends an ObjAttribute belonging to a root ObjEntity.
     */
    protected void append(ObjAttribute attribute) {

        if (!columnMap.containsKey(attribute.getDbAttributePath())) {

            List<DbRelationship> path = new ArrayList<DbRelationship>(2);
            Iterator<?> it = attribute.getDbPathIterator();
            while (it.hasNext()) {
                Object pathComponent = it.next();
                if (!(pathComponent instanceof DbRelationship)) {
                    break;
                }

                path.add((DbRelationship) pathComponent);
            }

            makeColumn(attribute.getDbAttributePath(), attribute, path);
        }
    }

    /**
     * Appends a DbAttribute belonging to a root DbEntity.
     */
    protected void append(DbAttribute attribute) {
        // skip if already appended via ObjAttributes
        if (!columnMap.containsKey(attribute.getName())) {
            makeColumn(attribute.getName(), attribute, null);
        }
    }

    /**
     * Appends a column matching a path Expression rooted in DbEntity.
     */
    protected void append(DbEntity root, Expression dbPath) {

        String pathString = dbPath.getOperand(0).toString();
        if (!columnMap.containsKey(pathString)) {

            List<DbRelationship> relationships = null;

            for (PathComponent<DbAttribute, DbRelationship> c : root.resolvePath(
                    dbPath,
                    Collections.emptyMap())) {

                if (c.isLast()) {
                    makeColumn(pathString, c.getAttribute(), relationships);
                }
                else {

                    if (relationships == null) {
                        relationships = new ArrayList<DbRelationship>(2);
                    }

                    relationships.add(c.getRelationship());
                }
            }
        }
    }

    /**
     * Appends a column matching a path Expression rooted in ObjEntity.
     */
    protected void append(ObjEntity root, Expression objPath) {

        Expression dbPath = root.translateToDbPath(objPath);
        String pathString = dbPath.getOperand(0).toString();
        if (!columnMap.containsKey(pathString)) {

            List<DbRelationship> relationships = null;

            for (PathComponent<ObjAttribute, ObjRelationship> c : root.resolvePath(
                    objPath,
                    Collections.emptyMap())) {

                if (c.isLast()) {
                    makeColumn(pathString, c.getAttribute(), relationships);
                }
                else {

                    if (relationships == null) {
                        relationships = new ArrayList<DbRelationship>(2);
                    }

                    relationships.addAll(c.getRelationship().getDbRelationships());
                }
            }
        }
    }

    private EntitySelectColumn makeColumn(
            String dataRowKey,
            ObjAttribute attribute,
            List<DbRelationship> relationships) {

        EntitySelectColumn column = new EntitySelectColumn();
        DbAttribute dbAttribute = attribute.getDbAttribute();

        // void column
        if (dbAttribute == null) {
            int jdbcType = TypesMapping.getSqlTypeByJava(attribute.getType());
            column.setColumnName(TypesMapping.isNumeric(jdbcType) ? "1" : "'1'");
            column.setJdbcType(jdbcType);
        }
        else {
            column.setColumnName(dbAttribute.getName());
            column.setJdbcType(dbAttribute.getType());
        }

        column.setDataRowKey(dataRowKey);
        column.setConverter(extendedTypes.getRegisteredType(attribute.getType()));
        column.setPath(relationships);

        columnMap.put(dataRowKey, columns.size());
        columns.add(column);

        return column;
    }

    private EntitySelectColumn makeColumn(
            String dataRowKey,
            DbAttribute attribute,
            List<DbRelationship> relationships) {
        EntitySelectColumn column = new EntitySelectColumn();
        column.setColumnName(attribute.getName());
        column.setJdbcType(attribute.getType());
        column.setDataRowKey(dataRowKey);

        String javaType = TypesMapping.getJavaBySqlType(attribute.getType());
        column.setConverter(extendedTypes.getRegisteredType(javaType));
        column.setPath(relationships);

        columnMap.put(dataRowKey, columns.size());
        columns.add(column);

        return column;
    }
}
