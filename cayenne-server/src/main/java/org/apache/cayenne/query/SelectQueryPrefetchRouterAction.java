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

package org.apache.cayenne.query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.property.BaseProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.CayenneMapEntry;

import static org.apache.cayenne.exp.ExpressionFactory.dbPathExp;
import static org.apache.cayenne.exp.ExpressionFactory.fullObjectExp;

/**
 * Preprocessor and router of SelectQuery prefetches.
 * 
 * @since 1.2
 */
class SelectQueryPrefetchRouterAction implements PrefetchProcessor {

    SelectQuery<?> query;
    QueryRouter router;
    EntityResolver resolver;
    ClassDescriptor classDescriptor;

    /**
     * Routes query prefetches, but not the query itself.
     */
    void route(SelectQuery query, QueryRouter router, EntityResolver resolver) {
        if (!query.isFetchingDataRows() && query.getPrefetchTree() != null) {

            this.query = query;
            this.router = router;
            this.resolver = resolver;
            this.classDescriptor = query.getMetaData(resolver).getClassDescriptor();

            query.getPrefetchTree().traverse(this);
        }
    }

    public boolean startPhantomPrefetch(PrefetchTreeNode node) {
        return true;
    }

    public boolean startDisjointPrefetch(PrefetchTreeNode node) {
        // don't do anything to root
        if (node == query.getPrefetchTree()) {
            return true;
        }

        String prefetchPath = node.getPath();

        // find last relationship
        Iterator<CayenneMapEntry> it = classDescriptor.getEntity().resolvePathComponents(
                prefetchPath);

        List<ObjRelationship> relationships = new ArrayList<>();
        while (it.hasNext()) {
            relationships.add((ObjRelationship) it.next());
        }

        if (relationships.isEmpty()) {
            throw new CayenneRuntimeException("Invalid prefetch '%s' for entity '%s'"
                    , prefetchPath, classDescriptor.getEntity().getName());
        }

        ObjRelationship firstRelationship = relationships.get(0);
        ObjRelationship lastRelationship = relationships.get(relationships.size() - 1);

        // chain query and entity qualifiers
        Expression queryQualifier = query.getQualifier();

        Expression entityQualifier = classDescriptor
                .getEntityInheritanceTree()
                .qualifierForEntityAndSubclasses();

        if (entityQualifier != null) {
            queryQualifier = (queryQualifier != null)
                    ? queryQualifier.andExp(entityQualifier)
                    : entityQualifier;
        }

        // create and configure PrefetchSelectQuery
        PrefetchSelectQuery<?> prefetchQuery = new PrefetchSelectQuery<>(prefetchPath, lastRelationship);
        prefetchQuery.setStatementFetchSize(query.getStatementFetchSize());

        if(!hasReverseDdRelationship(relationships)) {
            prefetchQuery.setRoot(firstRelationship.getSourceEntity());
            prefetchQuery.setColumns(buildColumn(lastRelationship, prefetchPath));
            prefetchQuery.setQualifier(query.getQualifier());
            if (lastRelationship.isSourceIndependentFromTargetChange()) {
                // setup extra result columns to be able to relate result rows to the parent
                // result objects.
                StringBuilder path = new StringBuilder();
                for(ObjRelationship relationship : relationships) {
                    if(path.length() != 0) {
                        path.append(".");
                    }
                    path.append(relationship.getDbRelationshipPath());
                    prefetchQuery.addResultPath("db:" + path);
                }
            }
        } else {
            prefetchQuery.setQualifier(classDescriptor.getEntity()
                    .translateToRelatedEntity(queryQualifier, prefetchPath));
            if (lastRelationship.isSourceIndependentFromTargetChange()) {
                // setup extra result columns to be able to relate result rows to the parent
                // result objects.
                prefetchQuery.addResultPath("db:" + lastRelationship.getReverseDbRelationshipPath());
            }
        }

        // pass prefetch subtree to enable joint prefetches...
        prefetchQuery.setPrefetchTree(node);

        // route...
        prefetchQuery.route(router, resolver, null);
        return true;
    }

    private BaseProperty<?> buildColumn(ObjRelationship relationship, String path) {
        Expression fullObjectExp = fullObjectExp(dbPathExp(path));
        Class<?> classType = resolver.getClassDescriptor(relationship.getTargetEntityName()).getObjectClass();
        return PropertyFactory.createBase(fullObjectExp, classType);
    }

    private boolean hasReverseDdRelationship(List<ObjRelationship> relationships) {
        for(ObjRelationship relationship : relationships) {
            if(!relationship.hasReverseDdRelationship()) {
                return false;
            }
        }

        return true;
    }

    public boolean startDisjointByIdPrefetch(PrefetchTreeNode prefetchTreeNode) {
        // simply pass through
        return true;
    }

    public boolean startJointPrefetch(PrefetchTreeNode node) {
        // simply pass through
        return true;
    }

    public boolean startUnknownPrefetch(PrefetchTreeNode node) {
        // don't do anything to root
        if (node == query.getPrefetchTree()) {
            return true;
        }

        // route unknown as disjoint...
        return startDisjointPrefetch(node);
    }

    public void finishPrefetch(PrefetchTreeNode node) {
    }
}
