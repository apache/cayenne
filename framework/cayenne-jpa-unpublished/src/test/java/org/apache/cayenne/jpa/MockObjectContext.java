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
package org.apache.cayenne.jpa;

import java.util.Collection;
import java.util.List;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DeleteDenyException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;

public class MockObjectContext implements ObjectContext {

    public void commitChanges() {
    }

    public void commitChangesToParent() {
    }

    public void deleteObject(Object object) throws DeleteDenyException {
    }

    public void deleteObjects(Collection<?> objects) {
    }

    public Collection<?> deletedObjects() {
        return null;
    }

    public DataChannel getChannel() {
        return null;
    }

    public EntityResolver getEntityResolver() {
        return null;
    }

    public GraphManager getGraphManager() {
        return null;
    }

    public Persistent localObject(ObjectId id, Object prototype) {
        return null;
    }

    public Collection<?> modifiedObjects() {
        return null;
    }

    public <T> T newObject(Class<T> persistentClass) {
        return null;
    }

    public Collection<?> newObjects() {
        return null;
    }

    public QueryResponse performGenericQuery(Query query) {
        return null;
    }

    @SuppressWarnings("unchecked")
    public List performQuery(Query query) {
        return null;
    }

    public void prepareForAccess(Persistent object, String property) {
    }

    public void prepareForAccess(Persistent object, String property, boolean lazyFaulting) {
    }

    public void propertyChanged(
            Persistent object,
            String property,
            Object oldValue,
            Object newValue) {
    }

    public void registerNewObject(Object object) {
    }

    public void rollbackChanges() {
    }

    public void rollbackChangesLocally() {
    }

    public Collection<?> uncommittedObjects() {
        return null;
    }

    public ObjectContext createChildContext() {
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
