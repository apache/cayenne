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
import org.apache.cayenne.access.translator.SelectTranslator;
import org.apache.cayenne.exp.PathExp;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.DbPathExp;
import org.apache.cayenne.exp.path.CayennePathSegment;
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledJoin;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.map.compiled.CompiledRelationship;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.resolved.ResolvedSelect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.table;

/**
 * @since 4.2
 */
class PrefetchNodeStage implements TranslationStage {

    private static final Logger LOGGER = LoggerFactory.getLogger(SelectTranslator.class);

    @Override
    public void perform(SelectTranslatorContext context) {
        updatePrefetchNodes(context);
        processJoint(context);
        processPrefetchQuery(context);
    }

    private void updatePrefetchNodes(SelectTranslatorContext context) {
        if(context.getSelect().prefetchTree() == null) {
            return;
        }
        // Set entity name, in case MixedConversionStrategy will be used to select objects from this query
        // Note: all prefetch nodes will point to query root, it is not a problem until select query can't
        // perform some sort of union or sub-queries.
        for(PrefetchTreeNode prefetch : context.getSelect().prefetchTree().getChildren()) {
            prefetch.setEntityName(context.getSelect().entity().name());
        }
    }

    private void processJoint(SelectTranslatorContext context) {
        ResolvedSelect select = context.getSelect();
        PrefetchTreeNode prefetch = select.prefetchTree();
        if(prefetch == null) {
            return;
        }

        // a paginated query only selects root ids; a joint to-many prefetch would multiply the id rows by the number
        // of joined children. Prefetches are applied later to the per-page fetch queries (see IncrementalFaultList).
        if(select.pageSize() > 0) {
            return;
        }

        CompiledEntity entity = select.entity();
        boolean warnPrefetchWithLimit = false;

        for(PrefetchTreeNode node : prefetch.adjacentJointNodes()) {
            Expression prefetchExp = ExpressionFactory.pathExp(node.getPath());
            CompiledRelationship targetRel = (CompiledRelationship) prefetchExp.evaluate(entity);
            DbPathExp dbPrefetch = (DbPathExp) entity.translateToDbPath(prefetchExp);
            CayennePath dbPath = dbPrefetch.getPath();
            CompiledTable table = entity.table();
            Expression targetQualifier = targetRel.target().qualifier();

            CayennePath fullPath = CayennePath.EMPTY_PATH.withMarker(CayennePath.PREFETCH_MARKER);

            for(CayennePathSegment c : dbPath) {
                CompiledJoin join = table.join(c.value());
                if(join == null) {
                    throw new CayenneRuntimeException("Unable to resolve path %s for entity %s", dbPath, entity.name());
                }

                fullPath = fullPath.dot(c);
                if(targetQualifier != null && c == dbPath.last()) {
                    targetQualifier = translateToPrefetchQualifier(targetRel.target(), targetQualifier);
                    context.getTableTree().addJoinTable(fullPath.withMarker(CayennePath.PREFETCH_MARKER), join, JoinType.LEFT_OUTER, targetQualifier);
                } else {
                    context.getTableTree().addJoinTable(fullPath.withMarker(CayennePath.PREFETCH_MARKER), join, JoinType.LEFT_OUTER);
                }
                table = join.target();
            }

            DescriptorColumnExtractor columnExtractor = new DescriptorColumnExtractor(context, targetRel.target());
            columnExtractor.extract(dbPath.withMarker(CayennePath.PREFETCH_MARKER));

            if(!warnPrefetchWithLimit && targetRel.isToMany()
                    && (select.fetchLimit() > 0 || select.fetchOffset() > 0)) {
                warnPrefetchWithLimit = true;
            }
        }

        // warn about a potentially faulty joint prefetch + limit combination
        if(warnPrefetchWithLimit) {
            LOGGER.warn("The query uses both limit/offset and a joint prefetch, this most probably will lead to an incorrect result. " +
                    "Either use disjointById prefetch or get a full result set.");
        }
    }

    Expression translateToPrefetchQualifier(CompiledEntity entity, Expression targetQualifier) {
        Expression expression = entity.translateToDbPath(targetQualifier);
        return expression.transform(o -> o instanceof DbPathExp
                ? ExpressionFactory.dbPathExp(((DbPathExp) o).getPath().withMarker(CayennePath.PREFETCH_MARKER))
                : o);
    }

    private void processPrefetchQuery(SelectTranslatorContext context) {
        ResolvedSelect select = context.getSelect();
        if (select.resultPaths().isEmpty()) {
            return;
        }

        PathTranslator pathTranslator = context.getPathTranslator();
        for(PathExp prefetchPath: select.resultPaths()) {
            DbPathExp pathExp = (DbPathExp) select.entity().translateToDbPath(prefetchPath);
            CayennePath path = pathExp.getPath();

            PathTranslationResult result = pathTranslator.translatePath(select.table(), path);
            CayennePath resultPath = result.getFinalPath();
            result.getDbRelationship().ifPresent(r -> {
                CompiledTable targetTable = r.target();
                context.getTableTree().addJoinTable(resultPath, r, JoinType.INNER);
                for (CompiledColumn pk : targetTable.primaryKey()) {
                    // note that we may select a source attribute, but label it as target for simplified snapshot processing
                    CayennePath finalPath = resultPath.dot(pk.name());
                    String alias = context.getTableTree().aliasForPath(resultPath);
                    Node columnNode = table(alias).column(pk).build();
                    context.addResultNode(columnNode, finalPath).setDbAttribute(pk);
                }
            });
        }
    }
}
