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

import java.util.List;

import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.QueryMetadata;

/**
 * Builder of an entity segment that has no persistent subclasses or superclasses.
 * 
 * @since 3.0
 */
class EntitySegmentBuilder extends MappedColumnBuilder {

    protected QueryMetadata metadata;
    protected ObjEntity entity;

    EntitySegmentBuilder(QueryMetadata metadata, ExtendedTypeMap extendedTypes,
            ObjEntity entity) {

        super(extendedTypes);
        this.entity = entity;
        this.metadata = metadata;
    }

    List<EntitySelectColumn> buildColumns() {
        if (metadata.getPageSize() > 0) {
            appendId();
        }
        else {
            appendAll();
        }

        return columns;
    }

    EntitySegment buildSegment() {

        buildColumns();

        RowReader<Object> rowReader;
        // read single column ID as scalar
        if (metadata.getPageSize() > 0 && columns.size() == 1) {
            EntitySelectColumn column = columns.get(0);
            rowReader = new ScalarRowReader(column.getConverter(), column.getJdbcType());
        }
        else {
            rowReader = new EntityRowReader(entity.getName(), columns);
        }

        return new EntitySegment(rowReader, columns);
    }

    protected void appendId() {
        // append meaningful attributes prior to any special DbAttributes; this way if
        // there is an overlap between meaningful and Db attributes, the right Java
        // type will be used.
        appendIdObjAttributes();
        appendIdDbAttributes();
    }

    protected void appendAll() {
        // append meaningful attributes prior to any special DbAttributes; this way if
        // there is an overlap between meaningful and Db attributes, the right Java
        // type will be used.
        appendObjAttributes();

        appendIdDbAttributes();
        appendFK();
        appendJointPrefetches();
    }

    protected void appendIdObjAttributes() {
        for (ObjAttribute attribute : entity.getDeclaredAttributes()) {

            if (attribute.isPrimaryKey()) {
                append(attribute);
            }
        }
    }

    protected void appendObjAttributes() {
        for (ObjAttribute attribute : entity.getDeclaredAttributes()) {
            append(attribute);
        }
    }

    protected void appendIdDbAttributes() {

        // if this ObjENtity inherits DbEntity from super, we will rely in super
        // descriptor to map ID columns
        if (entity.getDbEntityName() != null) {
            for (DbAttribute attribute : entity.getDbEntity().getPrimaryKeys()) {
                append(attribute);
            }
        }
    }

    protected void appendFK() {
        for (ObjRelationship relationship : entity.getDeclaredRelationships()) {

            DbRelationship dbRel = relationship.getDbRelationships().get(0);

            List<DbJoin> joins = dbRel.getJoins();
            int len = joins.size();
            for (int i = 0; i < len; i++) {
                append(joins.get(i).getSource());
            }
        }
    }

    protected void appendJointPrefetches() {
        if (metadata.getPrefetchTree() != null) {
            throw new UnsupportedOperationException("TODO: joint prefetches");
        }
    }
}
