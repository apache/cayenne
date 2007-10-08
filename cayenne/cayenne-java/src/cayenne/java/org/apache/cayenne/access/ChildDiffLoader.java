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

package org.apache.cayenne.access;

import java.util.Collection;
import java.util.Iterator;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphManager;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.Relationship;

/**
 * A GraphChangeHandler that loads child ObjectContext diffs into a parent DataContext.
 * Graph node ids are expected to be ObjectIds.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
class ChildDiffLoader implements GraphChangeHandler {

    DataContext context;
    GraphManager graphManager;

    ChildDiffLoader(DataContext context) {
        this.context = context;
        this.graphManager = context.getGraphManager();
    }

    public void nodeIdChanged(Object nodeId, Object newId) {
        throw new CayenneRuntimeException("Not supported");
    }

    public void nodeCreated(Object nodeId) {
        ObjectId id = (ObjectId) nodeId;
        if (id.getEntityName() == null) {
            throw new NullPointerException("Null entity name in id " + id);
        }

        ObjEntity entity = context
                .getEntityResolver()
                .lookupObjEntity(id.getEntityName());
        if (entity == null) {
            throw new IllegalArgumentException("Entity not mapped with Cayenne: " + id);
        }

        DataObject dataObject = null;
        try {
            dataObject = (DataObject) entity.getJavaClass().newInstance();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error instantiating object.", ex);
        }

        dataObject.setObjectId(id);
        context.registerNewObject(dataObject);
    }

    public void nodeRemoved(Object nodeId) {
        Persistent object = findObject(nodeId);
        context.deleteObject(object);
    }

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {

        // this change is for simple property, so no need to convert targets to server
        // objects...
        DataObject object = findObject(nodeId);

        try {
            object.writeProperty(property, newValue);
        }
        catch (Exception e) {
            throw new CayenneRuntimeException("Error setting property: " + property, e);
        }
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {

        DataObject source = findObject(nodeId);
        
        if(source == null) {
            return;
        }

        // find whether this is to-one or to-many
        ObjEntity sourceEntity = context.getEntityResolver().lookupObjEntity(source);
        Relationship relationship = sourceEntity.getRelationship(arcId.toString());

        DataObject target = findObject(targetNodeId);
        if (relationship.isToMany()) {
            source.addToManyTarget(relationship.getName(), target, false);
        }
        else {
            source.setToOneTarget(relationship.getName(), target, false);
        }
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        DataObject source = findObject(nodeId);

        // find whether this is to-one or to-many
        ObjEntity sourceEntity = context.getEntityResolver().lookupObjEntity(source);
        Relationship relationship = sourceEntity.getRelationship(arcId.toString());

        DataObject target = findObject(targetNodeId);
        if (relationship.isToMany()) {
            source.removeToManyTarget(relationship.getName(), target, false);
        }
        else {
            source.setToOneTarget(relationship.getName(), null, false);
        }
    }

    DataObject findObject(Object nodeId) {
        return (DataObject) graphManager.getNode(nodeId);
    }
    
    Persistent findObjectInCollection(Object nodeId, Collection toManyHolder) {
        Iterator it = toManyHolder.iterator();
        while (it.hasNext()) {
            Persistent o = (Persistent) it.next();
            if (nodeId.equals(o.getObjectId())) {
                return o;
            }
        }

        return null;
    }
}
