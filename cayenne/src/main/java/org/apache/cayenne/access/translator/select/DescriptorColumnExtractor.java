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

package org.apache.cayenne.access.translator.select;

import java.util.HashSet;
import java.util.Set;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledAttribute;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.map.compiled.CompiledRelationship;
import org.apache.cayenne.map.compiled.CompiledToOneRelationship;
import org.apache.cayenne.query.EntityResult;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.table;

/**
 * @since 4.2
 */
class DescriptorColumnExtractor extends BaseColumnExtractor {

    private final CompiledEntity entity;
    private final PathTranslator pathTranslator;
    private final Set<String> columnTracker = new HashSet<>();

    private EntityResult entityResult;
    private CayennePath prefix;
    private CayennePath labelPrefix;

    DescriptorColumnExtractor(SelectTranslatorContext context, CompiledEntity entity) {
        super(context);
        this.entity = entity;
        this.pathTranslator = context.getPathTranslator();
    }

    public void extract(CayennePath prefix) {
        this.prefix = prefix;
        boolean newEntityResult = false;
        this.labelPrefix = CayennePath.EMPTY_PATH;
        SelectTranslatorContext.DescriptorType type = SelectTranslatorContext.DescriptorType.OTHER;

        if(prefix != null && prefix.hasMarker(CayennePath.PREFETCH_MARKER)) {
            type = SelectTranslatorContext.DescriptorType.PREFETCH;
            labelPrefix = prefix;
            if(context.needsResultSetMapping()) {
                entityResult = context.getRootEntityResult();
                if (entityResult == null) {
                    throw new CayenneRuntimeException("Can't process prefetch descriptor without root.");
                }
            }
        } else {
            if(context.needsResultSetMapping()) {
                entityResult = new EntityResult(entity.objectClass());
                newEntityResult = true;
            }
            if(entity.table() == context.getRootDbEntity()){
                type = SelectTranslatorContext.DescriptorType.ROOT;
                context.setRootEntityResult(entityResult);
            }
        }

        context.markDescriptorStart(type);

        for (CompiledAttribute attribute : entity.allAttributes()) {
            addAttribute(attribute);
        }
        // a to-many relationship has no column of its own in the result
        for (CompiledRelationship relationship : entity.allRelationships()) {
            if (relationship instanceof CompiledToOneRelationship toOne) {
                addToOne(toOne);
            }
        }

        // add remaining needed attrs from DbEntity
        CompiledTable table = entity.table();
        String alias = context.getTableTree().aliasForPath(this.prefix);
        for (CompiledColumn column : table.primaryKey()) {
            String columnUniqueName = alias + '.' + column.name();
            if(columnTracker.add(columnUniqueName)) {
                addDbAttribute(this.prefix, labelPrefix, column);
                addEntityResultField(column);
            }
        }

        if(newEntityResult) {
            context.getSqlResult().addEntityResult(entityResult);
        }
        context.markDescriptorEnd(type);
    }

    private void addAttribute(CompiledAttribute attribute) {
        if(attribute.isLazy()) {
            return;
        }

        PathTranslationResult result = pathTranslator.translatePath(attribute.entity(), CayennePath.of(attribute.name()), prefix);

        int count = result.getDbAttributes().size();
        for(int i=0; i<count; i++) {
            ResultNodeDescriptor resultNodeDescriptor = processTranslationResult(result, i);
            CompiledColumn column = result.getDbAttributes().get(i);
            if(resultNodeDescriptor != null) {
                // set exact Java type only for the result node directly related to the ObjAttribute we processing
                // for the rest DbAttributes Java type would be calculated based on the DB type
                // see also CAY-2794
                if(column == attribute.column()) {
                    resultNodeDescriptor.setJavaType(attribute.valueClassName());
                }
                if (count > 1) {
                    // it was a flattened attribute, so need to keep full path info
                    CayennePath dataRowKey = result.getAttributePaths().get(i).dot(column.name());
                    resultNodeDescriptor.setDataRowKey(dataRowKey);
                    addEntityResultField(dataRowKey);
                } else {
                    // simple attribute
                    addEntityResultField(column);
                }
            }
        }
    }

    private void addToOne(CompiledToOneRelationship toOne) {
        // a to-one has a column in the result only if the source row holds its FK, or if it is a flattened path
        // whose first hop is to a PK: its key is then fetched through the path joins under a path-qualified name
        // (CAY-2911). An inverse one-to-one has no column of its own here.
        boolean keyInResult = toOne.isFlattened()
                ? toOne.joins().getFirst().isToPrimaryKey()
                : toOne.isForeignKey();
        if(!keyInResult) {
            return;
        }

        PathTranslationResult result = pathTranslator.translatePath(toOne.source(), CayennePath.of(toOne.name()), prefix);

        int count = result.getDbAttributes().size();
        for(int i=0; i<count; i++) {
            if(processTranslationResult(result, i) == null) {
                // the column was already emitted for an earlier property, and its result field with it
                continue;
            }
            CompiledColumn column = result.getDbAttributes().get(i);
            if(count > 1) {
                // See CAY-2911, and CAY-2552 for the same fix in addAttribute().
                addEntityResultField(result.getAttributePaths().get(i).dot(column.name()));
            } else {
                addEntityResultField(column);
            }
        }
    }

    private ResultNodeDescriptor processTranslationResult(PathTranslationResult result, int i) {
        CayennePath path = result.getAttributePaths().get(i);
        String alias = context.getTableTree().aliasForPath(path);
        CompiledColumn column = result.getDbAttributes().get(i);

        String columnUniqueName = alias + '.' + column.name();
        if(columnTracker.add(columnUniqueName)) {
            CayennePath attributePath = path.dot(column.name());
            Node columnNode = table(alias).column(column).build();
            return context.addResultNode(columnNode, attributePath).setDbAttribute(column);
        }

        return null;
    }

    private void addEntityResultField(CompiledColumn column) {
        String name = labelPrefix.dot(column.name()).value();
        if(context.needsResultSetMapping()) {
            entityResult.addDbField(name, name);
        }
    }

    private void addEntityResultField(CayennePath pathForFlattenedAttribute) {
        if (context.needsResultSetMapping()) {
            String nameForFlattenedAttribute = pathForFlattenedAttribute.value();
            entityResult.addDbField(nameForFlattenedAttribute, nameForFlattenedAttribute);
        }
    }
}
