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

import java.util.Collection;
import java.util.List;

import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.Select;

public class MockBaseContext extends BaseContext {

    @Override
    public void commitChanges() {
    }

    @Override
    public void commitChangesToParent() {
    }

    @Override
    public Collection<?> deletedObjects() {
        return null;
    }

    @Override
    public EntityResolver getEntityResolver() {
        return null;
    }

    @Override
    public GraphManager getGraphManager() {
        return null;
    }

    @Override
    public Collection<?> modifiedObjects() {
        return null;
    }

    @Override
    public <T> T newObject(Class<T> persistentClass) {
        return null;
    }

    @Override
    public Collection<?> newObjects() {
        return null;
    }

    @Override
    protected GraphDiff onContextFlush(ObjectContext originatingContext, GraphDiff changes, boolean cascade) {
        return null;
    }

    @Override
    public QueryResponse performGenericQuery(Query query) {
        return null;
    }

    @Override
    public List performQuery(Query query) {
        return null;
    }

    @Override
    public void registerNewObject(Object object) {
    }

    @Override
    public void rollbackChanges() {
    }

    @Override
    public void rollbackChangesLocally() {
    }

    @Override
    public Collection<?> uncommittedObjects() {
        return null;
    }

    public ObjectContext createChildContext() {
        return null;
    }

    public boolean hasChanges() {
        return false;
    }

    public QueryResponse onQuery(ObjectContext originatingContext, Query query) {
        return null;
    }

    @Override
    public <T> ResultIterator<T> iterator(Select<T> query) {
        return null;
    }

}
