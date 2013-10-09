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

package org.apache.cayenne.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyMapProperty;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

/**
 * An operation that creates a subgraph of detached objects, using the PrefetchTree to
 * delineate the graph boundaries. Target objects can be described by a different set of
 * descriptors, thus allowing server-to-client conversion to happen in the process.
 * 
 * @since 1.2
 */
public class ObjectDetachOperation {

    protected EntityResolver targetResolver;
    protected Map<ObjectId, Persistent> seen;

    public ObjectDetachOperation(EntityResolver targetResolver) {
        this.targetResolver = targetResolver;
        this.seen = new HashMap<ObjectId, Persistent>();
    }

    public void reset() {
        seen.clear();
    }

    /**
     * "Detaches" an object from its context by creating an unattached copy. The copy is
     * created using target descriptor of this operation that may be different from the
     * object descriptor passed to this method.
     */
    public Object detach(
            Object object,
            ClassDescriptor descriptor,
            final PrefetchTreeNode prefetchTree) {
        if (!(object instanceof Persistent)) {
            throw new CayenneRuntimeException("Expected Persistent, got: " + object);
        }

        final Persistent source = (Persistent) object;
        ObjectId id = source.getObjectId();

        // sanity check
        if (id == null) {
            throw new CayenneRuntimeException("Server returned an object without an id: "
                    + source);
        }

        Object seenTarget = seen.get(id);
        if (seenTarget != null) {
            return seenTarget;
        }

        descriptor = descriptor.getSubclassDescriptor(source.getClass());

        // presumably id's entity name should be of the right subclass.
        final ClassDescriptor targetDescriptor = targetResolver.getClassDescriptor(id
                .getEntityName());

        final Persistent target = (Persistent) targetDescriptor.createObject();
        target.setObjectId(id);
        seen.put(id, target);

        descriptor.visitProperties(new PropertyVisitor() {

        	private void fillReverseRelationship(Object destinationTarget, ArcProperty property) {
				ArcProperty reverseProperty = property.getComplimentaryReverseArc();

				if (reverseProperty != null && reverseProperty instanceof ToOneProperty) {

					ClassDescriptor desc = targetResolver.getClassDescriptor(
							reverseProperty.getRelationship().getSourceEntity().getName());

					ToOneProperty targetReverseProperty =
							(ToOneProperty) desc.getProperty(reverseProperty.getName());
					targetReverseProperty.writeProperty(destinationTarget, null, target);
				}
        	}

            public boolean visitToOne(ToOneProperty property) {
                if (prefetchTree != null) {

                    PrefetchTreeNode child = prefetchTree.getNode(property.getName());

                    if (child != null) {
                        Object destinationSource = property.readProperty(source);

                        Object destinationTarget = destinationSource != null ? detach(
                                destinationSource,
                                property.getTargetDescriptor(),
                                child) : null;
                                
                        if (destinationTarget != null) {
                            fillReverseRelationship(destinationTarget, property);
                        }

                        ToOneProperty targetProperty = (ToOneProperty) targetDescriptor
                                .getProperty(property.getName());
                        Object oldTarget = targetProperty.isFault(target)
                                ? null
                                : targetProperty.readProperty(target);
                        targetProperty
                                .writeProperty(target, oldTarget, destinationTarget);
                    }
                }

                return true;
            }

            public boolean visitToMany(ToManyProperty property) {
                if (prefetchTree != null) {
                    PrefetchTreeNode child = prefetchTree.getNode(property.getName());

                    if (child != null) {
                        Object value = property.readProperty(source);
                        Object targetValue; 
                        
                        if (property instanceof ToManyMapProperty) {
                            Map<?, ?> map = (Map) value;
                            Map targetMap = new HashMap();
                            
                            for (Entry entry : map.entrySet()) {
                                Object destinationSource = entry.getValue();
                                Object destinationTarget = destinationSource != null
                                    ? detach(destinationSource, property
                                        .getTargetDescriptor(), child)
                                        : null;
                                
                                if (destinationTarget != null) {
                                    fillReverseRelationship(destinationTarget, property);
                                }

                                targetMap.put(entry.getKey(), destinationTarget);
                            }
                            targetValue = targetMap;
                        }
                        else {
                            Collection collection = (Collection) value;
                            Collection targetCollection = new ArrayList(collection.size());
    
                            for (Object destinationSource : collection) {
                                Object destinationTarget = destinationSource != null
                                        ? detach(destinationSource, property
                                                .getTargetDescriptor(), child)
                                        : null;
                                        
                                if (destinationTarget != null) {
                                  	fillReverseRelationship(destinationTarget, property);
                                }
                                
                                targetCollection.add(destinationTarget);
                            }
                            targetValue = targetCollection;
                        }

                        ToManyProperty targetProperty = (ToManyProperty) targetDescriptor
                                .getProperty(property.getName());
                        targetProperty.writeProperty(target, null, targetValue);
                    }
                }

                return true;
            }

            public boolean visitAttribute(AttributeProperty property) {
                PropertyDescriptor targetProperty = targetDescriptor
                        .getProperty(property.getName());
                targetProperty.writeProperty(target, null, property.readProperty(source));
                return true;
            }
        });

        return target;
    }
}
