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

package org.apache.cayenne;

import java.util.ArrayList;
import java.util.List;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.util.GenericResponse;

/**
 * Stores all messages passed via this handler.
 * 
 */
public class MockDataChannel implements DataChannel {

    protected EntityResolver resolver;
    protected List requestObjects = new ArrayList();
    protected GraphDiff commitResponse;
    protected QueryResponse response;

    public MockDataChannel() {

    }

    public MockDataChannel(GraphDiff commitResponse) {
        this.commitResponse = commitResponse;
    }

    public MockDataChannel(List selectResponse) {
        this.response = new GenericResponse(selectResponse);
    }

    public MockDataChannel(EntityResolver entityResolver, List selectResponse) {
        this(selectResponse);
        this.resolver = entityResolver;
    }

    public MockDataChannel(EntityResolver entityResolver, QueryResponse response) {
        this.resolver = entityResolver;
        this.response = response;
    }

    public MockDataChannel(EntityResolver resolver) {
        this(resolver, new GenericResponse());
    }

    public EventManager getEventManager() {
        return null;
    }

    public void reset() {
        requestObjects.clear();
    }

    public List getRequestObjects() {
        return requestObjects;
    }

    public GraphDiff onSync(ObjectContext originatingContext, GraphDiff changes, int syncType) {
        requestObjects.add(changes);
        return commitResponse;
    }

    public QueryResponse onQuery(ObjectContext context, Query query) {
        requestObjects.add(query);
        return response;
    }

    public EntityResolver getEntityResolver() {
        return resolver;
    }
    
    public void setEntityResolver(EntityResolver resolver) {
        this.resolver = resolver;
    }
}
