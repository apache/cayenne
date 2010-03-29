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
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTTrue;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * Builder of an entity segment that has at least one subclass or superclass or any
 * combination of them.
 * 
 * @since 3.0
 */
class EntityTreeSegmentBuilder {

    private QueryMetadata metadata;
    protected ExtendedTypeMap extendedTypes;

    private List<Expression> entityQualifiers;
    private List<RowReader<?>> entityReaders;

    protected ClassDescriptor classDescriptor;
    protected List<EntitySelectColumn> columns;
    protected Map<String, Integer> columnMap;

    EntityTreeSegmentBuilder(QueryMetadata metadata, ExtendedTypeMap extendedTypes,
            ClassDescriptor classDescriptor) {

        this.metadata = metadata;
        this.extendedTypes = extendedTypes;
        this.classDescriptor = classDescriptor;

        this.entityQualifiers = new ArrayList<Expression>();
        this.entityReaders = new ArrayList<RowReader<?>>();
        this.columns = new ArrayList<EntitySelectColumn>();
        this.columnMap = new HashMap<String, Integer>();
    }

    SelectDescriptor<Object> buildSegment() {

        EntityInheritanceTree inheritanceTree = classDescriptor
                .getEntityInheritanceTree();

        // non-leaf entity
        if (inheritanceTree != null) {
            return buildNonLeafSegment(inheritanceTree);
        }
        // leaf entity
        else {
            return buildLeafSegment();
        }
    }

    SelectDescriptor<Object> buildLeafSegment() {
        EntityRowReader rowReader = processSuperclasses(classDescriptor.getEntity());
        return new EntitySegment(rowReader, columns);
    }

    SelectDescriptor<Object> buildNonLeafSegment(EntityInheritanceTree inheritanceTree) {
        EntityRowReader superRowReader = null;

        ObjEntity superEntity = classDescriptor.getEntity().getSuperEntity();
        if (superEntity != null) {
            superRowReader = processSuperclasses(superEntity);
        }

        processSubclasses(inheritanceTree, superRowReader);

        RowReader<Object> discriminatorReader = mergeColumns(
                null,
                new DiscriminatorBuilder(extendedTypes, inheritanceTree).buildColumns());

        int size = this.entityQualifiers.size();
        Expression[] entityQualifiers = this.entityQualifiers
                .toArray(new Expression[size]);
        RowReader<Object>[] entityReaders = this.entityReaders
                .toArray(new RowReader[size]);

        // if subclasses are involved, change RowReader to a compound RowReader...
        RowReader<Object> rowReader = new EntityTreeRowReader(
                discriminatorReader,
                entityQualifiers,
                entityReaders);
        return new EntitySegment(rowReader, columns);
    }

    /**
     * Merges subsegment columns into the main columns list, generating a RowReader for
     * subsegment.
     */
    private EntityRowReader mergeColumns(
            String entityName,
            List<EntitySelectColumn> columnsToMerge) {

        int[] indexes = new int[columnsToMerge.size()];

        for (int i = 0; i < indexes.length; i++) {

            EntitySelectColumn column = columnsToMerge.get(i);

            Integer columnIndex = columnMap.get(column.getDataRowKey());
            if (columnIndex == null) {
                columnIndex = columns.size();
                columnMap.put(column.getDataRowKey(), columnIndex);
                columns.add(column);
            }

            indexes[i] = columnIndex.intValue() + 1;
        }

        return new EntityRowReader(entityName, columnsToMerge, indexes);
    }

    private EntityRowReader processSuperclasses(ObjEntity entity) {

        List<EntitySelectColumn> entityColumns = new EntitySegmentBuilder(
                metadata,
                extendedTypes,
                entity).buildColumns();

        EntityRowReader rowReader = mergeColumns(entity.getName(), entityColumns);

        ObjEntity superEntity = entity.getSuperEntity();
        if (superEntity != null) {
            rowReader.setSuperReader(processSuperclasses(superEntity));
        }

        return rowReader;
    }

    private void processSubclasses(EntityInheritanceTree node, EntityRowReader superReader) {

        EntityRowReader reader = processSubclass(node);
        reader.setSuperReader(superReader);

        for (EntityInheritanceTree childNode : node.getChildren()) {
            processSubclasses(childNode, reader);
        }
    }

    private EntityRowReader processSubclass(EntityInheritanceTree node) {

        List<EntitySelectColumn> entityColumns = new EntitySegmentBuilder(
                metadata,
                extendedTypes,
                node.getEntity()).buildColumns();

        // merge columns
        EntityRowReader rowReader = mergeColumns(
                node.getEntity().getName(),
                entityColumns);

        // record entity qualifier and row reader...
        if (!node.getEntity().isAbstract()) {

            Expression qualifier = node.getDbQualifier();

            if (qualifier == null) {
                qualifier = new ASTTrue();
            }

            entityQualifiers.add(qualifier);
            entityReaders.add(rowReader);
        }

        return rowReader;
    }
}
