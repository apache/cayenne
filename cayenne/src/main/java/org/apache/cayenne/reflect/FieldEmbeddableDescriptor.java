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
package org.apache.cayenne.reflect;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.map.Embeddable;

/**
 * A default POJO embeddable descriptor.
 * 
 * @since 3.0
 */
public class FieldEmbeddableDescriptor implements EmbeddableDescriptor {

    protected Class<?> embeddableClass;
    protected Embeddable embeddable;
    protected Accessor ownerAccessor;
    protected Accessor embeddedPropertyAccessor;

    public FieldEmbeddableDescriptor(AdhocObjectFactory objectFactory, Embeddable embeddable,
                                     String ownerProperty, String embeddedPropertyProperty) {
        this.embeddable = embeddable;
        this.embeddableClass = objectFactory.getJavaClass(embeddable.getClassName());
        this.ownerAccessor = new FieldAccessor(embeddableClass, ownerProperty, Persistent.class);
        this.embeddedPropertyAccessor = new FieldAccessor(embeddableClass, embeddedPropertyProperty, String.class);
    }

    public Object createObject(Object owner, String embeddedProperty) {
        Object embeddable;
        try {
            embeddable = embeddableClass.getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            throw new PropertyException("Error creating embeddable object of class '" + embeddableClass.getName() + "'", e);
        }

        ownerAccessor.setValue(embeddable, owner);
        embeddedPropertyAccessor.setValue(embeddable, embeddedProperty);
        return embeddable;

    }

    public Embeddable getEmbeddable() {
        return embeddable;
    }

    public Class<?> getObjectClass() {
        return embeddableClass;
    }

}
