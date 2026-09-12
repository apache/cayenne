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

package org.apache.cayenne;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;

/**
 * Stores all messages passed via this handler.
 * 
 */
public class MockDataChannel implements DataChannel {

    protected EntityResolver resolver;
    protected List requestObjects = new ArrayList();
    protected GraphDiff commitResponse;
    protected List<QueryResult> response;

    public MockDataChannel() {

    }

    public MockDataChannel(GraphDiff commitResponse) {
        this.commitResponse = commitResponse;
    }

    public MockDataChannel(List selectResponse) {
        this.response = List.of(new QueryResult.Select<>(selectResponse));
    }

    public MockDataChannel(EntityResolver entityResolver, List selectResponse) {
        this(selectResponse);
        this.resolver = entityResolver;
    }

    public MockDataChannel(EntityResolver resolver) {
        this.resolver = resolver;
        this.response = new ArrayList<>();
    }

    public EventManager getEventManager() {
        return null;
    }

    public DataChannel getParent() {
        return null;
    }

    public void reset() {
        requestObjects.clear();
    }

    public List getRequestObjects() {
        return requestObjects;
    }

    public GraphDiff onSync(ObjectContext context, GraphDiff changes, int syncType) {
        requestObjects.add(changes);
        return commitResponse;
    }

    public List<QueryResult> onQuery(ObjectContext context, Query query, boolean iteratedResult) {
        requestObjects.add(query);
        return response;
    }

    public void onInvalidate(ObjectContext context, Collection<ObjectId> objectIds) {
        requestObjects.add(objectIds);
    }

    public Persistent onIdQuery(ObjectContext context, ObjectId id) {
        requestObjects.add(id);
        List<?> objects = response != null ? firstList() : null;
        return objects == null || objects.isEmpty() ? null : (Persistent) objects.getFirst();
    }

    public List<Persistent> onRelationshipQuery(ObjectContext context, ObjectId sourceId,
                                                String relationshipName) {
        requestObjects.add(sourceId);
        return (List<Persistent>) firstList();
    }

    private List<?> firstList() {
        for (QueryResult item : response) {
            if (item instanceof QueryResult.Select<?> select) {
                return select.objects();
            }
        }
        return null;
    }

    public EntityResolver getEntityResolver() {
        return resolver;
    }
    
    public void setEntityResolver(EntityResolver resolver) {
        this.resolver = resolver;
    }
}
