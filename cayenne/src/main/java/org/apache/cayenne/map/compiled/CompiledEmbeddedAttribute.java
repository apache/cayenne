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

import org.apache.cayenne.reflect.Accessor;
import org.apache.cayenne.reflect.PropertyException;

import java.util.List;

/**
 * An attribute of an entity that holds an {@link CompiledEmbeddable embeddable} object. It is not a column-mapped
 * attribute itself: the entity maps the attributes of the embeddable as its own {@link CompiledAttribute
 * attributes}, named "embeddedAttribute.embeddableAttribute", each referring back to this object.
 *
 * @since 6.0
 */
public final class CompiledEmbeddedAttribute {

    private final String name;
    private final CompiledEmbeddable embeddable;
    private final Accessor accessor;

    // written once by the compiler, after the entity attributes are created
    List<CompiledAttribute> attributes;

    /**
     * @param accessor the accessor of the embeddable object on the persistent object, or null if the classes are
     *                 not bound
     */
    CompiledEmbeddedAttribute(String name, CompiledEmbeddable embeddable, Accessor accessor) {
        this.name = name;
        this.embeddable = embeddable;
        this.accessor = accessor;
        this.attributes = List.of();
    }

    public String name() {
        return name;
    }

    public CompiledEmbeddable embeddable() {
        return embeddable;
    }

    /**
     * Returns the attributes of the entity that map the attributes of the embeddable.
     */
    public List<CompiledAttribute> attributes() {
        return attributes;
    }

    /**
     * Creates the embeddable object of the persistent object if it has none, bound to the object. The framework
     * calls this when it prepares an object for access, together with the value holders of the relationships, so
     * that the {@link #attributes() attributes} of the embeddable can be read and written.
     */
    public void injectValueHolder(Object object) throws PropertyException {
        if (accessor == null) {
            throw new IllegalStateException("Entity class is not bound, can't inject '" + name + "'");
        }
        if (accessor.getValue(object) == null) {
            accessor.setValue(object, embeddable.createObject(object, name));
        }
    }

    @Override
    public String toString() {
        return "CompiledEmbeddedAttribute[" + name + "]";
    }
}
