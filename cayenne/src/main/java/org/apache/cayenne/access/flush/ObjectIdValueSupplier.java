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

package org.apache.cayenne.access.flush;

import java.util.Objects;
import java.util.function.Supplier;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.types.InternalUnsupportedTypeFactory;

/**
 * Deferred value extracted from ObjectId
 *
 * @since 4.2
 */
class ObjectIdValueSupplier implements Supplier<Object>, InternalUnsupportedTypeFactory.Marker {

    private final ObjectId id;
    private final String attribute;

    static Object getFor(ObjectId id, String attribute) {
        // resolve eagerly, if value is already present
        // TODO: what if this is a meaningful part of an ID and it will change?
        Object value = id.getIdSnapshot().get(attribute);
        if(value != null) {
            return value;
        }
        return new ObjectIdValueSupplier(id, attribute);
    }

    private ObjectIdValueSupplier(ObjectId id, String attribute) {
        this.id = Objects.requireNonNull(id);
        this.attribute = Objects.requireNonNull(attribute);
    }

    @Override
    public Object get() {
        return id.getIdSnapshot().get(attribute);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ObjectIdValueSupplier that = (ObjectIdValueSupplier) o;
        if (!id.equals(that.id)) {
            return false;
        }
        return attribute.equals(that.attribute);
    }

    @Override
    public int hashCode() {
        return 31 * id.hashCode() + attribute.hashCode();
    }

    @Override
    public String toString() {
        return "{id=" + id + ", attr=" + attribute + '}';
    }

    @Override
    public String errorMessage() {
        return "Value supplier is not resolved before usage.";
    }
}
