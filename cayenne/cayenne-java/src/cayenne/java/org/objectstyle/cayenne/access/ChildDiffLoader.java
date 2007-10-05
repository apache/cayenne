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
package org.objectstyle.cayenne.access;

import java.util.Collection;
import java.util.Iterator;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.graph.GraphChangeHandler;
import org.objectstyle.cayenne.graph.GraphManager;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.Relationship;

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
