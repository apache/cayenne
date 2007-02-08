/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.query;

import java.util.Iterator;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.map.EntityInheritanceTree;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;

/**
 * Preprocessor and router of SelectQuery prefetches.
 * 
 * @since 1.2
 * @author Andrus Adamchik
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

        // setup extra result columns to be able to relate result rows to the parent
        // result objects.
        if (relationship.isFlattened()
                || (relationship.isToMany() && relationship.getReverseRelationship() == null)) {

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
