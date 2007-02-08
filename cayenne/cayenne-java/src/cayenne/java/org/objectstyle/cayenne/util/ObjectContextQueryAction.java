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

package org.objectstyle.cayenne.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.property.ArcProperty;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.query.ObjectIdQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.QueryMetadata;
import org.objectstyle.cayenne.query.RelationshipQuery;

/**
 * A helper class that implements
 * {@link org.objectstyle.cayenne.DataChannel#onQuery(ObjectContext, Query)} logic on
 * behalf of an ObjectContext.
 * <p>
 * <i>Intended for internal use only.</i>
 * </p>
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class ObjectContextQueryAction {

    protected static final boolean DONE = true;

    protected ObjectContext targetContext;
    protected ObjectContext actingContext;
    protected Query query;
    protected QueryMetadata metadata;

    protected transient QueryResponse response;

    public ObjectContextQueryAction(ObjectContext actingContext,
            ObjectContext targetContext, Query query) {

        this.actingContext = actingContext;
        this.query = query;

        // no special target context and same target context as acting context mean the
        // same thing. "normalize" the internal state to avoid confusion
        this.targetContext = targetContext != actingContext ? targetContext : null;
        this.metadata = query.getMetaData(actingContext.getEntityResolver());
    }

    /**
     * Worker method that perfomrs internal query.
     */
    public QueryResponse execute() {

        if (interceptOIDQuery() != DONE) {
            if (interceptRelationshipQuery() != DONE) {
                runQuery();
            }
        }

        interceptObjectConversion();

        return response;
    }

    /**
     * Transfers fetched objects into the target context if it is different from "acting"
     * context. Note that when this method is invoked, result objects are already
     * registered with acting context by the parent channel.
     */
    protected void interceptObjectConversion() {

        if (targetContext != null && !metadata.isFetchingDataRows()) {

            // rewrite response to contain objects from the query context

            GenericResponse childResponse = new GenericResponse();

            for (response.reset(); response.next();) {
                if (response.isList()) {

                    List objects = response.currentList();
                    if (objects.isEmpty()) {
                        childResponse.addResultList(objects);
                    }
                    else {

                        // TODO: Andrus 1/31/2006 - IncrementalFaultList is not properly
                        // transferred between contexts....

                        List childObjects = new ArrayList(objects.size());
                        Iterator it = objects.iterator();
                        while (it.hasNext()) {
                            Persistent object = (Persistent) it.next();
                            childObjects.add(targetContext.localObject(object
                                    .getObjectId(), object));
                        }

                        childResponse.addResultList(childObjects);
                    }
                }
                else {
                    childResponse.addBatchUpdateCount(response.currentUpdateCount());
                }
            }

            response = childResponse;
        }

    }

    protected boolean interceptOIDQuery() {
        if (query instanceof ObjectIdQuery) {
            ObjectIdQuery oidQuery = (ObjectIdQuery) query;

            if (!oidQuery.isFetchMandatory() && !oidQuery.isFetchingDataRows()) {
                Object object = actingContext.getGraphManager().getNode(
                        oidQuery.getObjectId());
                if (object != null) {
                    this.response = new ListResponse(object);
                    return DONE;
                }
            }
        }

        return !DONE;
    }

    protected boolean interceptRelationshipQuery() {

        if (query instanceof RelationshipQuery) {
            RelationshipQuery relationshipQuery = (RelationshipQuery) query;
            if (!relationshipQuery.isRefreshing()) {

                // don't intercept to-many relationships if fetch is done to the same
                // context as the root context of this action - this will result in an
                // infinite loop.

                if (targetContext == null
                        && relationshipQuery.getRelationship(
                                actingContext.getEntityResolver()).isToMany()) {
                    return !DONE;
                }

                ObjectId id = relationshipQuery.getObjectId();
                Object object = actingContext.getGraphManager().getNode(id);

                if (object != null) {

                    ClassDescriptor descriptor = actingContext
                            .getEntityResolver()
                            .getClassDescriptor(id.getEntityName());

                    if (!descriptor.isFault(object)) {

                        ArcProperty property = (ArcProperty) descriptor
                                .getProperty(relationshipQuery.getRelationshipName());

                        if (!property.isFault(object)) {

                            Object related = property.readPropertyDirectly(object);

                            List result;

                            // null to-one
                            if (related == null) {
                                result = new ArrayList(1);
                            }
                            // to-many
                            else if (related instanceof List) {
                                result = (List) related;
                            }
                            // non-null to-one
                            else {
                                result = new ArrayList(1);
                                result.add(related);
                            }

                            this.response = new ListResponse(result);
                            return DONE;

                        }
                    }
                }
            }
        }

        return !DONE;
    }

    /**
     * Fetches data from the channel.
     */
    protected void runQuery() {
        this.response = actingContext.getChannel().onQuery(actingContext, query);
    }
}