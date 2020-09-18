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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.ASTDbPath;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.PrefetchSelectQuery;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.Select;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.table;

/**
 * @since 4.2
 */
class PrefetchNodeStage implements TranslationStage {

    private static final Logger LOGGER = LoggerFactory.getLogger(SelectTranslator.class);

    @Override
    public void perform(TranslatorContext context) {
        updatePrefetchNodes(context);
        processJoint(context);
        processPrefetchQuery(context);
    }

    private void updatePrefetchNodes(TranslatorContext context) {
        if(context.getMetadata().getPrefetchTree() == null) {
            return;
        }
        // Set entity name, in case MixedConversionStrategy will be used to select objects from this query
        // Note: all prefetch nodes will point to query root, it is not a problem until select query can't
        // perform some sort of union or sub-queries.
        for(PrefetchTreeNode prefetch : context.getMetadata().getPrefetchTree().getChildren()) {
            prefetch.setEntityName(context.getMetadata().getObjEntity().getName());
        }
    }

    private void processJoint(TranslatorContext context) {
        QueryMetadata queryMetadata = context.getMetadata();
        PrefetchTreeNode prefetch = queryMetadata.getPrefetchTree();
        if(prefetch == null) {
            return;
        }

        ObjEntity objEntity = queryMetadata.getObjEntity();
        boolean warnPrefetchWithLimit = false;

        for(PrefetchTreeNode node : prefetch.adjacentJointNodes()) {
            Expression prefetchExp = ExpressionFactory.exp(node.getPath());
            ASTDbPath dbPrefetch = (ASTDbPath) objEntity.translateToDbPath(prefetchExp);
            final String dbPath = dbPrefetch.getPath();
            DbEntity dbEntity = objEntity.getDbEntity();

            PathComponents components = new PathComponents(dbPath);
            StringBuilder fullPath = new StringBuilder();
            for(String c : components.getAll()) {
                DbRelationship rel = dbEntity.getRelationship(c);
                if(rel == null) {
                    throw new CayenneRuntimeException("Unable to resolve path %s for entity %s", dbPath, objEntity.getName());
                }
                if(fullPath.length() > 0) {
                    fullPath.append('.');
                }
                context.getTableTree().addJoinTable("p:" + fullPath.append(c).toString(), rel, JoinType.LEFT_OUTER);
                dbEntity = rel.getTargetEntity();
            }

            ObjRelationship targetRel = (ObjRelationship) prefetchExp.evaluate(objEntity);
            ClassDescriptor prefetchClassDescriptor = context.getResolver().getClassDescriptor(targetRel.getTargetEntityName());

            DescriptorColumnExtractor columnExtractor = new DescriptorColumnExtractor(context, prefetchClassDescriptor);
            columnExtractor.extract("p:" + dbPath);

            if(!warnPrefetchWithLimit && targetRel.isToMany()
                    && (queryMetadata.getFetchLimit() > 0 || queryMetadata.getFetchOffset() > 0)) {
                warnPrefetchWithLimit = true;
            }
        }

        // warn about a potentially faulty joint prefetch + limit combination
        if(warnPrefetchWithLimit) {
            LOGGER.warn("The query uses both limit/offset and a joint prefetch, this most probably will lead to an incorrect result. " +
                    "Either use disjointById prefetch or get a full result set.");
        }
    }

    private void processPrefetchQuery(TranslatorContext context) {
        Select<?> select = context.getQuery().unwrap();
        if(!(select instanceof PrefetchSelectQuery)) {
            return;
        }

        PathTranslator pathTranslator = context.getPathTranslator();
        PrefetchSelectQuery<?> prefetchSelectQuery = (PrefetchSelectQuery<?>) select;
        for(String prefetchPath: prefetchSelectQuery.getResultPaths()) {
            ASTDbPath pathExp = (ASTDbPath) context.getMetadata().getClassDescriptor().getEntity()
                    .translateToDbPath(ExpressionFactory.exp(prefetchPath));

            String path = pathExp.getPath();
            PathTranslationResult result = pathTranslator
                    .translatePath(context.getMetadata().getDbEntity(), path);
            result.getDbRelationship().ifPresent(r -> {
                DbEntity targetEntity = r.getTargetEntity();
                context.getTableTree().addJoinTable(path, r, JoinType.INNER);
                for (DbAttribute pk : targetEntity.getPrimaryKeys()) {
                    // note that we may select a source attribute, but label it as target for simplified snapshot processing
                    String finalPath = path + '.' + pk.getName();
                    String alias = context.getTableTree().aliasForPath(path);
                    Node columnNode = table(alias).column(pk).build();
                    context.addResultNode(columnNode, finalPath).setDbAttribute(pk);
                }
            });
        }
    }
}
