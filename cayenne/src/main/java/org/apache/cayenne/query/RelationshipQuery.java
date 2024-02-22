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

package org.apache.cayenne.query;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.util.Util;

/**
 * A query that selects objects related to a given object via a mapped relationship.
 * RelationshipQuery is used by Cayenne internally to resolve relationships, and is rarely
 * executed by the application directly, although this of course is possible too.
 * 
 * @since 1.2
 */
public class RelationshipQuery extends IndirectQuery {

    protected ObjectId objectId;
    protected String relationshipName;
    protected boolean refreshing;
    protected int statementFetchSize;

    protected transient EntityResolver metadataResolver;
    protected transient QueryMetadata metadata;
    protected transient ArcProperty arc;

    // exists for deserialization with Hessian
    @SuppressWarnings("unused")
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
    @Override
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

    @Override
    protected Query createReplacementQuery(EntityResolver resolver) {

        if (objectId.isTemporary() && !objectId.isReplacementIdAttached()) {
            throw new CayenneRuntimeException("Can't build a query for relationship '%s' for temporary id: %s"
                    , relationshipName, objectId);
        }

        ObjRelationship relationship = getRelationship(resolver);

        // build executable select...
        Expression qualifier = ExpressionFactory
                .matchExp(ExpressionFactory.dbPathExp(relationship.getReverseDbRelationshipPath()), objectId);

        return ObjectSelect.query(Persistent.class)
                .entityName(relationship.getTargetEntityName())
                .where(qualifier)
                .statementFetchSize(statementFetchSize);
    }

    /**
     * Returns a non-null relationship object for this query.
     */
    public ObjRelationship getRelationship(EntityResolver resolver) {
        updateMetadata(resolver);
        return arc.getRelationship();
    }

    void updateMetadata(EntityResolver resolver) {
        // caching metadata as it may be accessed multiple times (at a DC and DD level)
        if (metadataResolver != resolver) {

            if (objectId == null) {
                throw new CayenneRuntimeException("Can't resolve query - objectID is null.");
            }

            ClassDescriptor descriptor = resolver.getClassDescriptor(objectId
                    .getEntityName());
            this.arc = (ArcProperty) descriptor.getProperty(relationshipName);

            if (arc == null) {
                throw new CayenneRuntimeException("No relationship named %s found in entity %s; object id: %s"
                        , relationshipName, objectId.getEntityName(), objectId);
            }

            this.metadata = new DefaultQueryMetadata() {

                @Override
                public boolean isRefreshingObjects() {
                    return refreshing;
                }

                @Override
                public ObjEntity getObjEntity() {
                    return arc.getTargetDescriptor().getEntity();
                }

                @Override
                public ClassDescriptor getClassDescriptor() {
                    return arc.getTargetDescriptor();
                }

                @Override
                public int getStatementFetchSize() {
                    return statementFetchSize;
                }
            };

            this.metadataResolver = resolver;
        }
    }

    /**
     * Sets statement's fetch size (0 for no default size)
     * 
     * @since 3.0
     */
    public void setStatementFetchSize(int size) {
        this.statementFetchSize = size;
    }

    /**
     * @return statement's fetch size
     * @since 3.0
     */
    public int getStatementFetchSize() {
        return statementFetchSize;
    }

    /**
     * Overrides toString() outputting a short string with query class and relationship
     * name.
     */
    @Override
    public String toString() {
        return Util.stripPackageName(getClass().getName()) + ":" + getRelationshipName();
    }
}
