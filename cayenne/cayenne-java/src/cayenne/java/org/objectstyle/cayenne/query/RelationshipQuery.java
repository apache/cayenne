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

import org.apache.commons.lang.StringUtils;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;

/**
 * A query that selects objects related to a given object. It is intended for fetching
 * objects related to a given object using a mapped relationship. Cayenne uses it for this
 * purpose internally. RelationshipQuery works with either an ObjectId or a GlobalID for a
 * root object.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class RelationshipQuery extends IndirectQuery {

    protected ObjectId objectId;
    protected String relationshipName;
    protected boolean refreshing;

    protected transient EntityResolver metadataResolver;
    protected transient QueryMetadata metadata;
    protected transient ObjRelationship relationship;

    // exists for deserialization with Hessian
    private RelationshipQuery() {

    }

    /**
     * Creates a RelationshipQuery. Same as
     * <em>new RelationshipQuery(objectID, relationshipName, true)</em>.
     * 
     * @param objectID ObjectId of a root object of the relationship.
     * @param relationshipName The name of the relationship.
     */
    public RelationshipQuery(ObjectId objectID, String relationshipName) {
        this(objectID, relationshipName, true);
    }

    /**
     * Creates a RelationshipQuery.
     * 
     * @param objectID ObjectId of a root object of the relationship.
     * @param relationshipName The name of the relationship.
     * @param refreshing whether objects should be refreshed
     */
    public RelationshipQuery(ObjectId objectID, String relationshipName,
            boolean refreshing) {
        if (objectID == null) {
            throw new CayenneRuntimeException("Null objectID");
        }

        this.objectId = objectID;
        this.relationshipName = relationshipName;
        this.refreshing = refreshing;
    }

    /**
     * Returns query metadata object.
     */
    // return metadata without creating replacement, as this is not always possible to
    // create replacement (one-way relationships, etc.)
    public QueryMetadata getMetaData(final EntityResolver resolver) {
        updateMetadata(resolver);
        return metadata;
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    public boolean isRefreshing() {
        return refreshing;
    }

    public String getRelationshipName() {
        return relationshipName;
    }

    protected Query createReplacementQuery(EntityResolver resolver) {

        if (objectId.isTemporary() && !objectId.isReplacementIdAttached()) {
            throw new CayenneRuntimeException("Can't build a query for relationship '"
                    + relationshipName
                    + "' for temporary id: "
                    + objectId);
        }

        ObjRelationship relationship = getRelationship(resolver);

        // build executable select...
        Expression qualifier = ExpressionFactory.matchDbExp(relationship
                .getReverseDbRelationshipPath(), objectId);

        SelectQuery query = new SelectQuery(
                (ObjEntity) relationship.getTargetEntity(),
                qualifier);
        query.setRefreshingObjects(refreshing);
        return query;
    }

    /**
     * Returns a non-null relationship object for this query.
     */
    public ObjRelationship getRelationship(EntityResolver resolver) {
        updateMetadata(resolver);
        return relationship;
    }

    void updateMetadata(EntityResolver resolver) {
        // caching metadata as it may be accessed multiple times (at a DC and DD level)
        if (metadataResolver != resolver) {

            if (objectId == null) {
                throw new CayenneRuntimeException(
                        "Can't resolve query - objectID is null.");
            }

            ObjEntity entity = resolver.lookupObjEntity(objectId.getEntityName());
            this.relationship = (ObjRelationship) entity
                    .getRelationship(relationshipName);

            if (relationship == null) {
                throw new CayenneRuntimeException("No relationship named "
                        + relationshipName
                        + " found in entity "
                        + entity.getName()
                        + "; object id: "
                        + objectId);
            }

            this.metadata = new DefaultQueryMetadata() {

                public boolean isRefreshingObjects() {
                    return refreshing;
                }

                public ObjEntity getObjEntity() {
                    return (ObjEntity) relationship.getTargetEntity();
                }
            };

            this.metadataResolver = resolver;
        }
    }

    /**
     * Overrides toString() outputting a short string with query class and relationship
     * name.
     */
    public String toString() {
        return StringUtils.substringAfterLast(getClass().getName(), ".")
                + ":"
                + getRelationshipName();
    }
}
