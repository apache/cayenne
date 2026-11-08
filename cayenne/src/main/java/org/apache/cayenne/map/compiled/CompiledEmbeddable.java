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

import org.apache.cayenne.Persistent;
import org.apache.cayenne.reflect.Accessor;
import org.apache.cayenne.reflect.DefaultConstructor;
import org.apache.cayenne.reflect.FieldAccessor;

import java.util.Collection;
import java.util.Map;

/**
 * A compiled embeddable class: a persistent class without an identity of its own, embedded in persistent objects.
 *
 * @since 6.0
 */
public final class CompiledEmbeddable {

    private final String name;
    private final Class<?> objectClass;
    private final DefaultConstructor<?> constructor;
    private final Accessor ownerAccessor;
    private final Accessor embeddedPropertyAccessor;
    private final Map<String, CompiledEmbeddableAttribute> attributes;

    CompiledEmbeddable(String name, Class<?> objectClass, Map<String, CompiledEmbeddableAttribute> attributes) {
        this.name = name;
        this.objectClass = objectClass;
        this.attributes = attributes;

        if (objectClass != null) {
            this.constructor = new DefaultConstructor<>(objectClass);
            this.ownerAccessor = new FieldAccessor(objectClass, "owner", Persistent.class);
            this.embeddedPropertyAccessor = new FieldAccessor(objectClass, "embeddedProperty", String.class);
        } else {
            this.constructor = null;
            this.ownerAccessor = null;
            this.embeddedPropertyAccessor = null;
        }
    }

    /**
     * Returns the name of the embeddable, which is the name of its class; {@link #objectClass()} is the class
     * itself, when bound.
     */
    public String name() {
        return name;
    }

    /**
     * Returns the embeddable class, or null if the mapping was compiled without class binding.
     */
    public Class<?> objectClass() {
        return objectClass;
    }

    public Collection<CompiledEmbeddableAttribute> attributes() {
        return attributes.values();
    }

    public CompiledEmbeddableAttribute attribute(String name) {
        return attributes.get(name);
    }

    /**
     * Creates a new instance of the embeddable class for the given owner and its embedded property.
     */
    public Object createObject(Object owner, String embeddedProperty) {
        if (constructor == null) {
            throw new IllegalStateException("Embeddable class is not bound: " + name);
        }
        Object embeddable = constructor.newInstance();
        ownerAccessor.setValue(embeddable, owner);
        embeddedPropertyAccessor.setValue(embeddable, embeddedProperty);
        return embeddable;
    }

    @Override
    public String toString() {
        return "CompiledEmbeddable[" + name + "]";
    }
}
