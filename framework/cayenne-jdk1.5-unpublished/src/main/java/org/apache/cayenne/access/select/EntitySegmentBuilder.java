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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 3.0
 */
class EntitySegmentBuilder {

    private QueryMetadata metadata;
    private ExtendedTypeMap extendedTypes;
    private List<EntitySelectColumn> columns;
    private Map<DbAttribute, Integer> columnMap;
    private DbEntity dbEntity;
    private ClassDescriptor classDescriptor;

    EntitySegmentBuilder(QueryMetadata metadata, ExtendedTypeMap extendedTypes,
            ClassDescriptor classDescriptor, DbEntity dbEntity) {

        this.columns = new ArrayList<EntitySelectColumn>();
        this.columnMap = new HashMap<DbAttribute, Integer>();
        this.dbEntity = dbEntity;
        this.classDescriptor = classDescriptor;
        this.metadata = metadata;
        this.extendedTypes = extendedTypes;
    }

    SelectDescriptor<Object> getDescriptor() {
        if (metadata.getPageSize() > 0) {
            appendId();
        }
        else {
            appendAll();
        }

        RowReader<Object> rowReader;
        // read single column ID as scalar
        if (metadata.getPageSize() > 0 && columns.size() == 1) {
            EntitySelectColumn column = columns.get(0);
            rowReader = new ScalarRowReader(column.getConverter(), column.getJdbcType());
        }
        else {
            rowReader = new EntityRowReader(
                    classDescriptor.getEntity().getName(),
                    columns);
        }

        return new EntitySegment(rowReader, columns);
    }

    private void appendId() {
        // append meaningful attributes prior to any special DbAttributes; this way if
        // there is an overlap between meaningful and Db attributes, the right Java
        // type will be used.
        appendIdObjAttributes();
        appendIdDbAttributes();
    }

    private void appendAll() {
        // append meaningful attributes prior to any special DbAttributes; this way if
        // there is an overlap between meaningful and Db attributes, the right Java
        // type will be used.
        appendObjAttributes();

        appendIdDbAttributes();
        appendFK();
        appendJointPrefetches();
    }

    private void appendIdObjAttributes() {
        for (ObjAttribute attribute : classDescriptor.getEntity().getDeclaredAttributes()) {

            if (attribute.isPrimaryKey()) {
                appendObjAttribute(attribute);
            }
        }
    }

    private void appendObjAttributes() {
        for (ObjAttribute attribute : classDescriptor.getEntity().getDeclaredAttributes()) {
            appendObjAttribute(attribute);
        }
    }

    private void appendIdDbAttributes() {
        for (DbAttribute attribute : dbEntity.getPrimaryKeys()) {
            appendDbAttrribute(attribute);
        }
    }

    private void appendFK() {
        for (ObjRelationship relationship : classDescriptor
                .getEntity()
                .getDeclaredRelationships()) {

            DbRelationship dbRel = relationship.getDbRelationships().get(0);

            List<DbJoin> joins = dbRel.getJoins();
            int len = joins.size();
            for (int i = 0; i < len; i++) {
                appendDbAttrribute(joins.get(i).getSource());
            }
        }
    }

    private void appendJointPrefetches() {
        if (metadata.getPrefetchTree() != null) {
            throw new UnsupportedOperationException("TODO: joint prefetches");
        }
    }

    private void appendObjAttribute(ObjAttribute attribute) {
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

        column.setDataRowKey(attribute.getDbAttributePath());
        column.setPath(attribute);
        column.setConverter(extendedTypes.getRegisteredType(attribute.getType()));

        columnMap.put(dbAttribute, columns.size());
        columns.add(column);
    }

    private void appendDbAttrribute(DbAttribute attribute) {
        // skip if already appended via ObjAttributes
        if (!columnMap.containsKey(attribute)) {

            EntitySelectColumn column = new EntitySelectColumn();
            column.setColumnName(attribute.getName());
            column.setJdbcType(attribute.getType());
            column.setDataRowKey(attribute.getName());

            String javaType = TypesMapping.getJavaBySqlType(attribute.getType());
            column.setConverter(extendedTypes.getRegisteredType(javaType));

            columnMap.put(attribute, columns.size());
            columns.add(column);
        }
    }
}