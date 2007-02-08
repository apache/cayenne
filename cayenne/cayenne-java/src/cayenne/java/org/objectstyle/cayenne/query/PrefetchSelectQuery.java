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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.map.EntityInheritanceTree;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.util.Util;

/**
 * A SelectQuery to perform a prefetch based on another query. Used internally by Cayenne
 * and is normally never used directly.
 * 
 * @author Craig Miskell, Andrei Adamchik
 */
public class PrefetchSelectQuery extends SelectQuery {

    protected SelectQuery parentQuery;

    /**
     * The relationship path from root objects to the objects being prefetched.
     */
    protected String prefetchPath;

    /**
     * Stores the last ObjRelationship in the prefetch path.
     */
    protected ObjRelationship lastPrefetchHint;

    // TODO, Andrus 11/17/2005 - i guess we should deprecate
    // SelectQuery.customDbAttribute, replacing it with "resultPaths" mechanism.
    protected Collection resultPaths;

    /**
     * Creates a prefetch query based on parent query.
     * 
     * @since 1.1
     * @deprecated since 1.2 - passing EntityResolver in constructor is no longer needed,
     *             as prefetch query configuration alogrithm is moved out of this class.
     *             In fact this constructor will not correctly configure query to handle
     *             flattened prefetches.
     */
    public PrefetchSelectQuery(EntityResolver resolver, SelectQuery parentQuery,
            String prefetch) {

        setParentQuery(parentQuery);
        setPrefetchPath(prefetch);

        ObjEntity entity = parentQuery.getMetaData(resolver).getObjEntity();
        EntityInheritanceTree inheritanceTree = resolver.lookupInheritanceTree(entity);

        Iterator it = entity.resolvePathComponents(prefetch);

        // find root entity

        ObjRelationship r = null;
        while (it.hasNext()) {
            r = (ObjRelationship) it.next();
        }

        if (r == null) {
            throw new CayenneRuntimeException("Invalid prefetch '"
                    + prefetch
                    + "' for entity: "
                    + entity.getName());
        }

        setRoot(r.getTargetEntity());

        // chain query and entity qualifiers
        Expression queryQualifier = parentQuery.getQualifier();

        Expression entityQualifier = (inheritanceTree != null) ? inheritanceTree
                .qualifierForEntityAndSubclasses() : entity.getDeclaredQualifier();

        if (entityQualifier != null) {
            queryQualifier = (queryQualifier != null) ? queryQualifier
                    .andExp(entityQualifier) : entityQualifier;
        }

        setQualifier(entity.translateToRelatedEntity(queryQualifier, prefetchPath));

        if (r.isToMany() && !r.isFlattened()) {
            setLastPrefetchHint(r);
        }
    }

    /**
     * Creates a new disjoint prefetch select query.
     * 
     * @since 1.2
     */
    public PrefetchSelectQuery(SelectQuery parentQuery, String prefetchPath,
            ObjRelationship lastPrefetchHint) {

        setRoot(lastPrefetchHint.getTargetEntity());
        this.parentQuery = parentQuery;
        this.prefetchPath = prefetchPath;
        this.lastPrefetchHint = lastPrefetchHint;
    }

    /**
     * Overrides super implementation to suppress disjoint prefetch routing, as the parent
     * query should take care of that.
     * 
     * @since 1.2
     */
    void routePrefetches(QueryRouter router, EntityResolver resolver) {
        // noop - intentional.
    }

    /**
     * Returns the prefetchPath.
     * 
     * @return String
     */
    public String getPrefetchPath() {
        return prefetchPath;
    }

    /**
     * Sets the prefetchPath.
     * 
     * @param prefetchPath The prefetchPath to set
     */
    public void setPrefetchPath(String prefetchPath) {
        this.prefetchPath = prefetchPath;
    }

    /**
     * @since 1.1
     */
    public SelectQuery getParentQuery() {
        return parentQuery;
    }

    /**
     * @since 1.1
     */
    public void setParentQuery(SelectQuery parentQuery) {
        this.parentQuery = parentQuery;
    }

    /**
     * Retunrs last incoming ObjRelationship in the prefetch relationship chain.
     * 
     * @since 1.1
     */
    public ObjRelationship getLastPrefetchHint() {
        return lastPrefetchHint;
    }

    /**
     * @since 1.1
     */
    public void setLastPrefetchHint(ObjRelationship relationship) {
        lastPrefetchHint = relationship;
    }

    /**
     * Configures an "extra" path that will resolve to an extra column (or columns) in the
     * result set.
     * 
     * @param path A valid path expression. E.g. "abc" or "db:ABC" or "abc.xyz".
     * @since 1.2
     */
    public void addResultPath(String path) {
        if (Util.isEmptyString(path)) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }

        nonNullResultPaths().add(path);
    }

    /**
     * Removes an extra result path. Note that this method doesn't check for expression
     * invariants, as it doesn't have a proper context to do so. E.g. for the purspose of
     * this method "db:ARTIST_NAME" and "obj:artistName" are not the same, though both
     * will resolve to the same column name.
     */
    public void removeResultPath(String path) {
        if (resultPaths != null) {
            resultPaths.remove(path);
        }
    }

    /**
     * Returns extra result paths.
     * 
     * @since 1.2
     */
    public Collection getResultPaths() {
        return resultPaths != null
                ? Collections.unmodifiableCollection(resultPaths)
                : Collections.EMPTY_SET;
    }

    /**
     * Returns a Collection that internally stores extra result paths, creating it on
     * demand.
     * 
     * @since 1.2
     */
    Collection nonNullResultPaths() {
        if (resultPaths == null) {
            resultPaths = new HashSet();
        }

        return resultPaths;
    }
}
