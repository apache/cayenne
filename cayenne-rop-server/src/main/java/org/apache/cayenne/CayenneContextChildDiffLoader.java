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

import org.apache.cayenne.graph.ChildDiffLoader;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

/**
 * Used for loading child's CayenneContext changes to parent context.
 * 
 * @since 3.0
 */
class CayenneContextChildDiffLoader extends ChildDiffLoader {

    public CayenneContextChildDiffLoader(CayenneContext context) {
        super(context);
    }

    @Override
    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {

        super.nodePropertyChanged(nodeId, property, oldValue, newValue);

        Persistent object = (Persistent) context.getGraphManager().getNode(nodeId);
        context.propertyChanged(object, property, oldValue, newValue);
    }

    @Override
    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {

        final Persistent source = findObject(nodeId);
        final Persistent target = findObject(targetNodeId);

        // if a target was later deleted, the diff for arcCreated is still preserved and
        // can result in NULL target here.
        if (target == null) {
            return;
        }

        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(
                ((ObjectId) nodeId).getEntityName());
        ArcProperty property = (ArcProperty) descriptor.getProperty(arcId.toString());

        property.visit(new PropertyVisitor() {

            public boolean visitAttribute(AttributeProperty property) {
                return false;
            }

            public boolean visitToMany(ToManyProperty property) {
                property.addTargetDirectly(source, target);
                return false;
            }

            public boolean visitToOne(ToOneProperty property) {
                property.setTarget(source, target, false);
                return false;
            }
        });
        context.propertyChanged(source, (String) arcId, null, target);
    }

    @Override
    public void arcDeleted(Object nodeId, final Object targetNodeId, Object arcId) {
        final Persistent source = findObject(nodeId);

        // needed as sometime temporary objects are evoked from the context before
        // changing their relationships
        if (source == null) {
            return;
        }

        ClassDescriptor descriptor = context.getEntityResolver().getClassDescriptor(
                ((ObjectId) nodeId).getEntityName());
        PropertyDescriptor property = descriptor.getProperty(arcId.toString());

        final Persistent[] target = new Persistent[1];
        target[0] = findObject(targetNodeId);
        
        property.visit(new PropertyVisitor() {

            public boolean visitAttribute(AttributeProperty property) {
                return false;
            }

            public boolean visitToMany(ToManyProperty property) {
                if (target[0] == null) {

                    // this is usually the case when a NEW object was deleted and then
                    // its relationships were manipulated; so try to locate the object
                    // in the collection ... the performance of this is rather dubious
                    // of course...
                    target[0] = findObjectInCollection(targetNodeId, property
                            .readProperty(source));
                }

                if (target[0] == null) {
                    // ignore?
                }
                else {
                    property.removeTargetDirectly(source, target[0]);
                }

                return false;
            }

            public boolean visitToOne(ToOneProperty property) {
                property.setTarget(source, null, false);
                return false;
            }
        });

        context.propertyChanged(source, (String) arcId, target[0], null);
    }

}