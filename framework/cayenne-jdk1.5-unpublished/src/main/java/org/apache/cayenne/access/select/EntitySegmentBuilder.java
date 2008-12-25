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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SQLResultSetMetadata;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * @since 3.0
 */
class EntitySegmentBuilder {

    private QueryMetadata metadata;
    private ExtendedTypeMap extendedTypes;

    EntitySegmentBuilder(ExtendedTypeMap extendedTypes, QueryMetadata metadata) {
        this.extendedTypes = extendedTypes;
        this.metadata = metadata;
    }

    SelectSegment getSegment(int position) {

        ClassDescriptor rootDescriptor;
        EntityResultSegment segmentMetadata;

        SQLResultSetMetadata resultSetMetadata = metadata.getResultSetMapping();
        if (resultSetMetadata != null) {
            segmentMetadata = resultSetMetadata.getEntitySegment(position);
            rootDescriptor = segmentMetadata.getClassDescriptor();
        }
        else {
            segmentMetadata = null;
            rootDescriptor = metadata.getClassDescriptor();
        }

        // no ObjEntity and Java class at the root of the query...
        if (rootDescriptor == null) {
            DbEntity dbEntity = metadata.getDbEntity();
            if (dbEntity == null) {
                throw new CayenneRuntimeException("Invalid entity segment in position "
                        + position
                        + ", no root DbEntity specified");
            }

            return forDbEntity(dbEntity, segmentMetadata);
        }

        return forEntity(rootDescriptor, segmentMetadata);
    }

    private SelectSegment forEntity(
            ClassDescriptor rootDescriptor,
            EntityResultSegment segmentMetadata) {

        Collection<DbEntity> unionRoots = rootDescriptor.getRootDbEntities();
        if (unionRoots.size() == 1) {
            return forSingleSelectEntity(rootDescriptor, segmentMetadata, unionRoots
                    .iterator()
                    .next());
        }
        else {
            return forUnionSelectEntity(rootDescriptor, segmentMetadata, unionRoots);
        }

    }

    private SelectSegment forSingleSelectEntity(
            ClassDescriptor rootDescriptor,
            EntityResultSegment segmentMetadata,
            DbEntity root) {

        EntityColumnAppender appender = new EntityColumnAppender(rootDescriptor, root);

        if (metadata.getPageSize() > 0) {
            appender.appendId();
        }
        else {
            appender.appendAll();
        }

        EntityRowReader rowReader = new EntityRowReader(rootDescriptor
                .getEntity()
                .getName(), appender.columns);

        return new EntitySegment(rowReader, appender.columns);
    }

    private SelectSegment forUnionSelectEntity(
            ClassDescriptor rootDescriptor,
            EntityResultSegment segmentMetadata,
            Collection<DbEntity> unionRoots) {
        // TODO: union query
        throw new UnsupportedOperationException("TODO: union query");
    }

    private SelectSegment forDbEntity(
            DbEntity dbEntity,
            EntityResultSegment segmentMetadata) {
        // TODO - queries with DbEntity root
        throw new UnsupportedOperationException("TODO");
    }

    class EntityColumnAppender {

        List<EntitySelectColumn> columns;
        Map<DbAttribute, Integer> columnMap;
        DbEntity dbEntity;
        ClassDescriptor classDescriptor;

        EntityColumnAppender(ClassDescriptor classDescriptor, DbEntity dbEntity) {
            this.columns = new ArrayList<EntitySelectColumn>();
            this.columnMap = new HashMap<DbAttribute, Integer>();
            this.dbEntity = dbEntity;
            this.classDescriptor = classDescriptor;
        }

        void appendId() {
            // append meaningful attributes prior to any special DbAttributes; this way if
            // there is an overlap between meaningful and Db attributes, the right Java
            // type will be used.
            appendIdObjAttributes();
            appendIdDbAttributes();
        }

        void appendAll() {
            // append meaningful attributes prior to any special DbAttributes; this way if
            // there is an overlap between meaningful and Db attributes, the right Java
            // type will be used.
            appendObjAttributes();

            appendIdDbAttributes();
            appendFK();
            appendJointPrefetches();
        }

        private void appendIdObjAttributes() {
            for (ObjAttribute attribute : classDescriptor
                    .getEntity()
                    .getDeclaredAttributes()) {

                if (attribute.isPrimaryKey()) {
                    appendObjAttribute(attribute);
                }
            }
        }

        private void appendObjAttributes() {
            for (ObjAttribute attribute : classDescriptor
                    .getEntity()
                    .getDeclaredAttributes()) {
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
}
