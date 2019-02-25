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

package org.apache.cayenne.access.translator.select;

import java.util.Collection;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.Util;

/**
 * @since 4.2
 */
class CustomColumnSetExtractor implements ColumnExtractor {

    private final TranslatorContext context;
    private final Collection<BaseProperty<?>> columns;

    CustomColumnSetExtractor(TranslatorContext context, Collection<BaseProperty<?>> columns) {
        this.context = context;
        this.columns = columns;
    }

    @Override
    public void extract(String prefix) {
        for (BaseProperty<?> property : columns) {
            if (isFullObjectProp(property)) {
                extractFullObject(prefix, property);
            } else {
                extractSimpleProperty(property);
            }
        }
    }

    private void extractSimpleProperty(BaseProperty<?> property) {
        Node sqlNode = context.getQualifierTranslator().translate(property);
        context.addResultNode(sqlNode, true, property, property.getAlias());
        String name = property.getName() == null ? property.getExpression().expName() : property.getName();
        context.getSqlResult().addColumnResult(name);
    }

    private boolean isFullObjectProp(BaseProperty<?> property) {
        int expressionType = property.getExpression().getType();

        // forbid direct selection of toMany relationships columns
        if(property.getType() != null && (expressionType == Expression.OBJ_PATH || expressionType == Expression.DB_PATH)
                && (Collection.class.isAssignableFrom(property.getType())
                || Map.class.isAssignableFrom(property.getType()))) {
            throw new CayenneRuntimeException("Can't directly select toMany relationship columns. " +
                    "Either select it with aggregate functions like count() or with flat() function to select full related objects.");
        }

        // evaluate ObjPath with Persistent type as toOne relations and use it as full object
        return expressionType == Expression.FULL_OBJECT
                || (property.getType() != null
                    && expressionType == Expression.OBJ_PATH
                    && Persistent.class.isAssignableFrom(property.getType()));
    }

    private void extractFullObject(String prefix, BaseProperty<?> property) {
        prefix = calculatePrefix(prefix, property);
        ensureJoin(prefix);

        ObjEntity entity = context.getResolver().getObjEntity(property.getType());

        ColumnExtractor extractor;
        if(context.getMetadata().getPageSize() > 0) {
            extractor = new IdColumnExtractor(context, entity);
        } else {
            ClassDescriptor descriptor = context.getResolver().getClassDescriptor(entity.getName());
            extractor = new DescriptorColumnExtractor(context, descriptor);
        }

        int index = context.getResultNodeList().size();

        // extract required columns of entity
        extractor.extract(prefix);

        // Reset data row key as ObjectResolver expects it to match attribute name.
        // Maybe we should change resolver, as it seems cleaner to have path from root as prefix in data row key.
        for(int i=index; i<context.getResultNodeList().size(); i++) {
            context.getResultNodeList().get(i).setDataRowKey(null);
        }
    }

    /**
     * Extracts prefix for this extractor from property.
     * This will be just a db path for this property, if any exists.
     */
    private String calculatePrefix(String prefix, BaseProperty<?> property) {
        Expression exp = property.getExpression();
        int expressionType = exp.getType();
        if(expressionType == Expression.FULL_OBJECT && exp.getOperandCount() > 0) {
            Object op = exp.getOperand(0);
            if(op instanceof Expression) {
                exp = (Expression)op;
            }
        }
        return dbPathOrDefault(exp, prefix);
    }

    private String dbPathOrDefault(Expression pathExp, String defaultPrefix) {
        // normalize to db path first
        if(pathExp.getType() == Expression.OBJ_PATH) {
            pathExp = context.getMetadata().getObjEntity().translateToDbPath(pathExp);
        }

        if(pathExp.getType() != Expression.DB_PATH) {
            return defaultPrefix;
        }

        return ((ASTDbPath)pathExp).getPath();
    }

    private void ensureJoin(String prefix) {
        // ensure all joins for given property
        if(!Util.isEmptyString(prefix)) {
            PathTranslationResult result = context.getPathTranslator().translatePath(context.getMetadata().getDbEntity(), prefix);
            result.getDbRelationship().ifPresent(relationship
                    -> context.getTableTree().addJoinTable(result.getFinalPath(), relationship, JoinType.LEFT_OUTER));
        }
    }
}
