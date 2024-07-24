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

package org.apache.cayenne.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyMapProperty;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

/**
 * An operation that merges changes from an object graph, whose objects are registered in
 * some ObjectContext, to peer objects in an ObjectConext that is a child of that context.
 * The merge terminates at hollow nodes in the parent context to avoid tripping over
 * unresolved relationships.
 * 
 * @since 1.2
 */
public class DeepMergeOperation {

    private final EntityResolver entityResolver;
    private final ShallowMergeOperation shallowMergeOperation;

    public DeepMergeOperation(ObjectContext context) {
        this.entityResolver = context.getEntityResolver();
        this.shallowMergeOperation = new ShallowMergeOperation(context);
    }

    public <T extends Persistent> T merge(T peerInParentContext) {
        ClassDescriptor descriptor = entityResolver
                .getClassDescriptor(peerInParentContext.getObjectId().getEntityName());
        return merge(peerInParentContext, descriptor, new HashMap<>());
    }

    private <T extends Persistent> T merge(
            final T peerInParentContext,
            ClassDescriptor descriptor,
            final Map<ObjectId, Persistent> seen) {

        ObjectId id = peerInParentContext.getObjectId();

        // sanity check
        if (id == null) {
            throw new CayenneRuntimeException("Server returned an object without an id: %s", peerInParentContext);
        }

        Persistent seenTarget = seen.get(id);
        if (seenTarget != null) {
            @SuppressWarnings("unchecked")
            T castTarget = (T) seenTarget;
            return castTarget;
        }

        final T target = shallowMergeOperation.merge(peerInParentContext);
        seen.put(id, target);

        descriptor = descriptor.getSubclassDescriptor(id.getEntityName());
        descriptor.visitProperties(new PropertyVisitor() {

            public boolean visitToOne(ToOneProperty property) {

                if (!property.isFault(peerInParentContext)) {
                    Persistent destinationSource = (Persistent) property.readProperty(peerInParentContext);
                    Object destinationTarget = destinationSource != null
                            ? merge(destinationSource, property.getTargetDescriptor(), seen)
                            : null;
                    Object oldTarget = property.isFault(target)
                            ? null
                            : property.readProperty(target);
                    property.writePropertyDirectly(target, oldTarget, destinationTarget);
                }

                return true;
            }

            public boolean visitToMany(ToManyProperty property) {
                if (!property.isFault(peerInParentContext)) {
                    Object value = property.readProperty(peerInParentContext);
                    Object targetValue;

                    if (property instanceof ToManyMapProperty) {
                        Map<?, ?> map = (Map<?, ?>) value;
                        Map<Object, Object> targetMap = new HashMap<>();

                        for (Entry<?, ?> entry : map.entrySet()) {
                            Object destinationSource = entry.getValue();
                            Object destinationTarget = destinationSource != null
                                    ? merge((Persistent) destinationSource, property.getTargetDescriptor(), seen)
                                    : null;

                            targetMap.put(entry.getKey(), destinationTarget);
                        }
                        targetValue = targetMap;
                    }
                    else {
                        Collection<?> collection = (Collection<?>) value;
                        Collection<Object> targetCollection = new ArrayList<>(collection.size());

                        for (Object destinationSource : collection) {
                            Object destinationTarget = destinationSource != null
                                    ? merge((Persistent) destinationSource, property.getTargetDescriptor(), seen)
                                    : null;

                            targetCollection.add(destinationTarget);
                        }
                        targetValue = targetCollection;
                    }

                    property.writePropertyDirectly(target, null, targetValue);
                }

                return true;
            }

            public boolean visitAttribute(AttributeProperty property) {
                return true;
            }
        });

        return target;
    }
}
