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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.exp.property.Property;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.EmbeddedResult;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.EmbeddableResultSegment;
import org.apache.cayenne.query.EntityResultSegment;
import org.apache.cayenne.query.ResultSegment;
import org.apache.cayenne.query.ScalarResultSegment;
import org.apache.cayenne.reflect.ClassDescriptor;

/**
 * Extracts SQL columns for a query with an explicit column list. Each column is classified by the result segment
 * the query metadata built for it (see {@code ColumnSelectMetadata}), so entity, embeddable and scalar columns and
 * scalar Java types are decided once, against the model, rather than from the column's declared Java type.
 *
 * @since 4.2
 */
class CustomColumnSetExtractor implements ColumnExtractor {

    private final SelectTranslatorContext context;
    private final Collection<Property<?>> columns;

    CustomColumnSetExtractor(SelectTranslatorContext context, Collection<Property<?>> columns) {
        this.context = context;
        this.columns = columns;
    }

    @Override
    public void extract(CayennePath prefix) {
        List<ResultSegment> segments = context.getMetadata().getResultSetMapping();
        if (segments == null || segments.size() != columns.size()) {
            throw new CayenneRuntimeException("Query metadata describes %d result segments for %d columns",
                    segments == null ? 0 : segments.size(), columns.size());
        }

        Iterator<ResultSegment> segmentIterator = segments.iterator();
        for (Property<?> property : columns) {
            switch (segmentIterator.next()) {
                case EntityResultSegment segment -> extractFullObject(prefix, property, segment.classDescriptor());
                case EmbeddableResultSegment ignored -> extractEmbeddedObject(property);
                case ScalarResultSegment segment -> extractSimpleProperty(property, segment);
            }
        }
    }

    private void extractSimpleProperty(Property<?> property, ScalarResultSegment segment) {
        Node sqlNode = context.getQualifierTranslator().translate(property);
        String alias = property.getAlias();
        ResultNodeDescriptor resultNode = context.addResultNode(
                sqlNode, true, property, alias == null ? null : CayennePath.of(alias));
        if (segment.type() != null) {
            resultNode.setJavaType(segment.type().getCanonicalName());
        }
        context.getSqlResult().addColumnResult(segment.column());
    }

    private void extractEmbeddedObject(Property<?> property) {
        Object o = property.getExpression().evaluate(context.getMetadata().getObjEntity());
        if(!(o instanceof EmbeddedAttribute attribute)) {
            throw new CayenneRuntimeException("EmbeddedAttribute expected, %s found", o);
        }
        EmbeddedResult result = new EmbeddedResult(attribute.getEmbeddable(), attribute.getAttributes().size());
        attribute.getAttributes().forEach(attr -> {
            Node sqlNode = context.getQualifierTranslator()
                    .translate(ExpressionFactory.dbPathExp(attr.getDbAttributePath()));
            context.addResultNode(sqlNode, true, null, null);
            result.addAttribute(attr);
        });
        context.getSqlResult().addEmbeddedResult(result);
    }

    private void extractFullObject(CayennePath prefix, Property<?> property, ClassDescriptor descriptor) {
        prefix = calculatePrefix(prefix, property);
        ensureJoin(prefix);

        ObjEntity entity = descriptor.getEntity();

        ColumnExtractor extractor;
        if(context.getMetadata().getPageSize() > 0) {
            extractor = new IdColumnExtractor(context, entity);
        } else {
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
    private CayennePath calculatePrefix(CayennePath prefix, Property<?> property) {
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

    private CayennePath dbPathOrDefault(Expression pathExp, CayennePath defaultPrefix) {
        // normalize to db path first
        if(pathExp.getType() == Expression.OBJ_PATH) {
            pathExp = context.getMetadata().getObjEntity().translateToDbPath(pathExp);
        }

        if(pathExp.getType() != Expression.DB_PATH) {
            return defaultPrefix;
        }

        return ((ASTDbPath)pathExp).getPath();
    }

    private void ensureJoin(CayennePath prefix) {
        // ensure all joins for given property
        if(!prefix.isEmpty()) {
            PathTranslationResult result = context.getPathTranslator()
                    .translatePath(context.getMetadata().getDbEntity(), prefix);
            result.getDbRelationship().ifPresent(relationship
                    -> context.getTableTree().addJoinTable(result.getFinalPath(), relationship, JoinType.LEFT_OUTER));
        }
    }
}
