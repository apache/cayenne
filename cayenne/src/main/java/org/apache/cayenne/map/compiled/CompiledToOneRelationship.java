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
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.reflect.PropertyException;

import java.util.List;

/**
 * A compiled relationship whose property holds a single related object.
 *
 * @since 6.0
 */
public final class CompiledToOneRelationship extends CompiledRelationship {

    CompiledToOneRelationship(
            CompiledEntity source,
            CompiledEntity target,
            String name,
            DeleteRule deleteRule,
            boolean usedForLocking,
            List<CompiledJoin> joins) {
        super(source, target, name, deleteRule, usedForLocking, joins);
    }

    @Override
    public boolean isToMany() {
        return false;
    }

    /**
     * Sets the target of the relationship on the source object, optionally setting the reverse relationship on the
     * target.
     */
    public void setTarget(Object source, Object target, boolean setReverse) throws PropertyException {
        try {
            persistent(source).setToOneTarget(name, target != null ? persistent(target) : null, setReverse);
        } catch (PropertyException e) {
            throw e;
        } catch (Throwable th) {
            throw new PropertyException("Error setting to-one Persistent property: " + name, this, source, th);
        }
    }

    @Override
    public void injectValueHolder(Object object) throws PropertyException {
        // a to-one is resolved from the snapshot, no holder needed
    }

    @Override
    public boolean isFaultValue(Object value) {
        return value instanceof Fault;
    }

    @Override
    public void invalidate(Object object) {
        writePropertyDirectly(object, null, fault);
    }
}
