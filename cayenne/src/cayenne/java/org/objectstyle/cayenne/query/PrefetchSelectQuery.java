/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
 * A SelectQuery to perform a prefetch based on another query. Used internally
 * by Cayenne and is normally never used directly.
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

    /**
     * Creates a prefetch query based on parent query.
     * 
     * @since 1.1
     */
    public PrefetchSelectQuery(
        EntityResolver resolver,
        SelectQuery parentQuery,
        String prefetch) {

        setParentQuery(parentQuery);
        setPrefetchPath(prefetch);

        ObjEntity entity = resolver.lookupObjEntity(parentQuery);
        EntityInheritanceTree inheritanceTree = resolver.lookupInheritanceTree(entity);
        
        Iterator it = entity.resolvePathComponents(prefetch);

        // find root entity

        ObjRelationship r = null;
        while (it.hasNext()) {
            r = (ObjRelationship) it.next();
        }

        if (r == null) {
            throw new CayenneRuntimeException(
                "Invalid prefetch '" + prefetch + "' for entity: " + entity.getName());
        }

        setRoot(r.getTargetEntity());

        // chain query and entity qualifiers
        Expression queryQualifier = parentQuery.getQualifier();

        Expression entityQualifier =
            (inheritanceTree != null)
                ? inheritanceTree.qualifierForEntityAndSubclasses()
                : entity.getDeclaredQualifier();

        if (entityQualifier != null) {
            queryQualifier =
                (queryQualifier != null)
                    ? queryQualifier.andExp(entityQualifier)
                    : entityQualifier;
        }

        setQualifier(entity.translateToRelatedEntity(queryQualifier, prefetchPath));

        if (r.isToMany() && !r.isFlattened()) {
            setLastPrefetchHint(r);
        }
    }

    /**
     * @deprecated Since 1.1 use {@link #PrefetchSelectQuery(EntityResolver,SelectQuery,String)}
     */
    public PrefetchSelectQuery() {
        super();
    }

    /**
     * @deprecated Since 1.1 use {@link #PrefetchSelectQuery(EntityResolver,SelectQuery,String)}
     */
    public PrefetchSelectQuery(ObjEntity root) {
        super(root);
    }

    /**
     * @deprecated Since 1.1 use {@link #PrefetchSelectQuery(EntityResolver,SelectQuery,String)}
     */
    public PrefetchSelectQuery(ObjEntity root, Expression qualifier) {
        super(root, qualifier);
    }

    /**
     * @deprecated Since 1.1 use {@link #PrefetchSelectQuery(EntityResolver,SelectQuery,String)}
     */
    public PrefetchSelectQuery(Class rootClass) {
        super(rootClass);
    }

    /**
     * @deprecated Since 1.1 use {@link #PrefetchSelectQuery(EntityResolver,SelectQuery,String)}
     */
    public PrefetchSelectQuery(Class rootClass, Expression qualifier) {
        super(rootClass, qualifier);
    }

    /**
     * @deprecated Since 1.1 use {@link #PrefetchSelectQuery(EntityResolver,SelectQuery,String)}
     */
    public PrefetchSelectQuery(String objEntityName) {
        super(objEntityName);
    }

    /**
    * @deprecated Since 1.1 use {@link #PrefetchSelectQuery(EntityResolver,SelectQuery,String)}
    */
    public PrefetchSelectQuery(String objEntityName, Expression qualifier) {
        super(objEntityName, qualifier);
    }

    /**
     * Returns the prefetchPath.
     * @return String
     */
    public String getPrefetchPath() {
        return prefetchPath;
    }

    /**
     * Sets the prefetchPath.
     * @param prefetchPath The prefetchPath to set
     */
    public void setPrefetchPath(String prefetchPath) {
        this.prefetchPath = prefetchPath;
    }

    /**
     * @deprecated Since 1.1 use {@link #getParentQuery()}.
     */
    public SelectQuery getRootQuery() {
        return parentQuery;
    }

    /**
     * @deprecated Since 1.1 use setParentQuery(..)
     */
    public void setRootQuery(SelectQuery parentQuery) {
        this.parentQuery = parentQuery;
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
     * @return ObjRelationship
     * @deprecated Since 1.1 replaced with "lastPrefetch" property. 
     */
    public ObjRelationship getSingleStepToManyRelationship() {
        return lastPrefetchHint;
    }

    /**
     * Sets the singleStepToManyRelationship.
     * @param singleStepToManyRelationship The singleStepToManyRelationship to set
     * @deprecated Since 1.1 replaced with "lastPrefetch" property. 
     */
    public void setSingleStepToManyRelationship(ObjRelationship singleStepToManyRelationship) {
        this.lastPrefetchHint = singleStepToManyRelationship;
    }

    /**
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
}
