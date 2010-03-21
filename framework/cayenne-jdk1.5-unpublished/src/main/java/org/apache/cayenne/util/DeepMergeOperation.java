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
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.reflect.AttributeProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyMapProperty;
import org.apache.cayenne.reflect.ToManyProperty;
import org.apache.cayenne.reflect.ToOneProperty;

/**
 * An operation that performs object graph deep merge, terminating merge at unresolved
 * nodes.
 * 
 * @since 1.2
 */
public class DeepMergeOperation {

    protected ObjectContext context;
    protected Map<ObjectId, Persistent> seen;

    public DeepMergeOperation(ObjectContext context) {
        this.context = context;
        this.seen = new HashMap<ObjectId, Persistent>();
    }

    public void reset() {
        seen.clear();
    }

    public Object merge(Object object, ClassDescriptor descriptor) {
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

        final Persistent target = context.localObject(id, source);
        seen.put(id, target);

        descriptor = descriptor.getSubclassDescriptor(source.getClass());
        descriptor.visitProperties(new PropertyVisitor() {

            public boolean visitToOne(ToOneProperty property) {

                if (!property.isFault(source)) {
                    Object destinationSource = property.readProperty(source);

                    Object destinationTarget = destinationSource != null ? merge(
                            destinationSource,
                            property.getTargetDescriptor()) : null;

                    Object oldTarget = property.isFault(target) ? null : property
                            .readProperty(target);
                    property.writePropertyDirectly(target, oldTarget, destinationTarget);
                }

                return true;
            }

            public boolean visitToMany(ToManyProperty property) {
                if (!property.isFault(source)) {
                    Object value = property.readProperty(source);
                    Object targetValue; 
                    
                    if (property instanceof ToManyMapProperty) {
                        Map<?, ?> map = (Map) value;
                        Map targetMap = new HashMap();
                        
                        for (Entry entry : map.entrySet()) {
                            Object destinationSource = entry.getValue();
                            Object destinationTarget = destinationSource != null
                                ? merge(destinationSource, property.getTargetDescriptor())
                                    : null;

                            targetMap.put(entry.getKey(), destinationTarget);
                        }
                        targetValue = targetMap;
                    }
                    else {
                        Collection collection = (Collection) value;
                        Collection targetCollection = new ArrayList(collection.size());

                        for (Object destinationSource : collection) {
                            Object destinationTarget = destinationSource != null
                                    ? merge(destinationSource, property.getTargetDescriptor())
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
