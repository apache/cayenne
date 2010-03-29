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

import java.util.Iterator;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * Preprocessor and router of SelectQuery prefetches.
 * 
 * @since 1.2
 */
class SelectQueryPrefetchRouterAction implements PrefetchProcessor {

    SelectQuery query;
    QueryRouter router;
    EntityResolver resolver;
    ObjEntity entity;
    EntityInheritanceTree inheritanceTree;

    /**
     * Routes query prefetches, but not the query itself.
     */
    void route(SelectQuery query, QueryRouter router, EntityResolver resolver) {
        if (!query.isFetchingDataRows() && query.getPrefetchTree() != null) {

            this.query = query;
            this.router = router;
            this.resolver = resolver;
            this.entity = query.getMetaData(resolver).getObjEntity();
            this.inheritanceTree = resolver.lookupInheritanceTree(entity);

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
        Iterator it = entity.resolvePathComponents(prefetchPath);

        ObjRelationship relationship = null;
        while (it.hasNext()) {
            relationship = (ObjRelationship) it.next();
        }

        if (relationship == null) {
            throw new CayenneRuntimeException("Invalid prefetch '"
                    + prefetchPath
                    + "' for entity: "
                    + entity.getName());
        }

        // chain query and entity qualifiers
        Expression queryQualifier = query.getQualifier();

        Expression entityQualifier = (inheritanceTree != null) ? inheritanceTree
                .qualifierForEntityAndSubclasses() : entity.getDeclaredQualifier();

        if (entityQualifier != null) {
            queryQualifier = (queryQualifier != null) ? queryQualifier
                    .andExp(entityQualifier) : entityQualifier;
        }

        // create and configure PrefetchSelectQuery
        PrefetchSelectQuery prefetchQuery = new PrefetchSelectQuery(
                query,
                prefetchPath,
                relationship);

        prefetchQuery.setQualifier(entity.translateToRelatedEntity(
                queryQualifier,
                prefetchPath));

        if (relationship.isSourceIndependentFromTargetChange()) {
            // setup extra result columns to be able to relate result rows to the parent
            // result objects.
            prefetchQuery.addResultPath("db:"
                    + relationship.getReverseDbRelationshipPath());
        }

        // pass prefetch subtree to enable joint prefetches...
        prefetchQuery.setPrefetchTree(node);

        // route...
        prefetchQuery.route(router, resolver, null);
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
