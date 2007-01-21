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
package org.apache.cayenne.intercept;

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

/**
 * A pass-through decorator of an ObjectContext. Can serve as a superclass of various
 * ObjectContext interceptors.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class ObjectContextDecorator implements ObjectContext {

    protected ObjectContext context;

    public void commitChanges() {
        context.commitChanges();
    }

    public void commitChangesToParent() {
        context.commitChangesToParent();
    }

    public Collection deletedObjects() {
        return context.deletedObjects();
    }

    public void deleteObject(Object object) throws DeleteDenyException {
        context.deleteObject(object);
    }

    public DataChannel getChannel() {
        return context.getChannel();
    }

    public EntityResolver getEntityResolver() {
        return context.getEntityResolver();
    }

    public GraphManager getGraphManager() {
        return context.getGraphManager();
    }

    public Persistent localObject(ObjectId id, Object prototype) {
        return context.localObject(id, prototype);
    }

    public Collection modifiedObjects() {
        return context.modifiedObjects();
    }

    public Persistent newObject(Class persistentClass) {
        return context.newObject(persistentClass);
    }
    
    public void registerNewObject(Object object) {
        context.registerNewObject(object);
    }

    public Collection newObjects() {
        return context.newObjects();
    }

    public QueryResponse performGenericQuery(Query query) {
        return context.performGenericQuery(query);
    }

    public List performQuery(Query query) {
        return context.performQuery(query);
    }

    /**
     * @deprecated since 3.0, use {@link #prepareForAccess(Persistent, String, boolean)}.
     */
    public void prepareForAccess(Persistent object, String property) {
        context.prepareForAccess(object, property);
    }
    
    public void prepareForAccess(Persistent object, String property, boolean lazyFaulting) {
        context.prepareForAccess(object, property, lazyFaulting);
    }

    public void propertyChanged(
            Persistent object,
            String property,
            Object oldValue,
            Object newValue) {
        context.propertyChanged(object, property, oldValue, newValue);
    }

    public void rollbackChanges() {
        context.rollbackChanges();
    }

    public void rollbackChangesLocally() {
        context.rollbackChangesLocally();
    }

    public Collection uncommittedObjects() {
        return context.uncommittedObjects();
    }

    public ObjectContext getContext() {
        return context;
    }

    public void setContext(ObjectContext context) {
        this.context = context;
    }
}
