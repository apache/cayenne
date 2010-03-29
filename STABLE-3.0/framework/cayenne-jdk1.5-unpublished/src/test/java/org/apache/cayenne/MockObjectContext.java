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

import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;

/**
 * A noop ObjectContext used for unit testing.
 *
 */
public class MockObjectContext implements ObjectContext {

    protected GraphManager graphManager;

    public MockObjectContext() {
        super();
    }

    public MockObjectContext(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    public EntityResolver getEntityResolver() {
        return null;
    }

    public DataChannel getChannel() {
        return null;
    }

    public GraphManager getGraphManager() {
        return graphManager;
    }

    public Persistent localObject(ObjectId id, Object prototype) {
        return null;
    }

    public void commitChangesToParent() {
    }

    public void rollbackChangesLocally() {
    }

    public void rollbackChanges() {
    }

    public Collection newObjects() {
        return null;
    }

    public Collection deletedObjects() {
        return null;
    }

    public Collection modifiedObjects() {
        return null;
    }

    public List performQuery(Query query) {
        return null;
    }

    public int[] performNonSelectingQuery(Query query) {
        return null;
    }

    public void commitChanges() {

    }

    public void deleteObject(Object object) {
    }

    public void deleteObjects(Collection<?> objects) {
    }

    public void registerNewObject(Object object) {
    }

    /**
     * @deprecated since 3.0
     */
    @Deprecated
    public void prepareForAccess(Persistent persistent, String property) {
    }

    public void prepareForAccess(Persistent object, String property, boolean lazyFaulting) {
    }

    public void propertyChanged(
            Persistent persistent,
            String property,
            Object oldValue,
            Object newValue) {
    }

    public void addedToCollectionProperty(
            Persistent object,
            String property,
            Persistent added) {
    }

    public void removedFromCollectionProperty(
            Persistent object,
            String property,
            Persistent removed) {
    }

    public Collection uncommittedObjects() {
        return null;
    }

    public QueryResponse performGenericQuery(Query queryPlan) {
        return null;
    }

    public ObjectContext createChildContext() {
        return null;
    }

    public <T> T newObject(Class<T> persistentClass) {
        return null;
    }

    public boolean hasChanges() {
        return false;
    }

    public void invalidateObjects(Collection objects) {
    }

    public Object getUserProperty(String key) {
        return null;
    }

    public void setUserProperty(String key, Object value) {
    }
}
