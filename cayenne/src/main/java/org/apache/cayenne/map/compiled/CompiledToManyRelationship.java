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
package org.apache.cayenne.map.compiled;

import org.apache.cayenne.Fault;
import org.apache.cayenne.access.ToManyHolder;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.reflect.Accessor;
import org.apache.cayenne.reflect.PropertyException;

import java.util.List;

/**
 * A compiled relationship whose property holds a collection of related objects: a List, a Set, a Collection or a
 * Map keyed by a property of the target.
 *
 * @since 6.0
 */
public final class CompiledToManyRelationship extends CompiledRelationship {

    private final boolean map;
    private final String mapKey;

    // written once by the compiler, for a map relationship, after the target attributes are created
    Accessor mapKeyAccessor;

    CompiledToManyRelationship(
            CompiledEntity source,
            CompiledEntity target,
            String name,
            DeleteRule deleteRule,
            boolean usedForLocking,
            boolean map,
            String mapKey,
            List<CompiledJoin> joins) {
        super(source, target, name, deleteRule, usedForLocking, joins);
        this.map = map;
        this.mapKey = mapKey;
    }

    @Override
    public boolean isToMany() {
        return true;
    }

    /**
     * Returns true if the collection is a Map, keyed by the {@link #mapKey() map key} of the targets. The kind of
     * any other collection is a concern of the fault that creates it, not of the relationship.
     */
    public boolean isMap() {
        return map;
    }

    /**
     * Returns the name of the target property that keys a Map collection, or null if the target id is the key, or
     * the collection is not a Map.
     */
    public String mapKey() {
        return mapKey;
    }

    /**
     * Extracts the map key of a target object of a Map collection.
     */
    public Object mapKey(Object target) throws PropertyException {
        if (mapKeyAccessor == null) {
            throw new PropertyException("Not a map relationship: " + source.name() + "." + name, this, target,
                    (Throwable) null);
        }
        return mapKeyAccessor.getValue(target);
    }

    public void addTarget(Object source, Object target, boolean setReverse) throws PropertyException {
        try {
            persistent(source).addToManyTarget(name, persistent(target), setReverse);
        } catch (PropertyException e) {
            throw e;
        } catch (Throwable th) {
            throw new PropertyException("Error setting to-many Persistent property: " + name, this, source, th);
        }
    }

    public void removeTarget(Object source, Object target, boolean setReverse) throws PropertyException {
        try {
            persistent(source).removeToManyTarget(name, persistent(target), setReverse);
        } catch (PropertyException e) {
            throw e;
        } catch (Throwable th) {
            throw new PropertyException("Error unsetting to-many Persistent property: " + name, this, source, th);
        }
    }

    @Override
    public void injectValueHolder(Object object) throws PropertyException {
        if (readPropertyDirectly(object) == null) {
            writePropertyDirectly(object, null, fault.resolveFault(persistent(object)));
        }
    }

    @Override
    public boolean isFaultValue(Object value) {
        return value instanceof Fault || value instanceof ToManyHolder<?> holder && holder.isFault();
    }

    @Override
    public void invalidate(Object object) {
        Object value = readPropertyDirectly(object);
        if (value instanceof Fault) {
            // nothing to do
        } else if (value instanceof ToManyHolder<?> holder) {
            holder.invalidate();
        } else {
            writePropertyDirectly(object, null, fault);
        }
    }
}
