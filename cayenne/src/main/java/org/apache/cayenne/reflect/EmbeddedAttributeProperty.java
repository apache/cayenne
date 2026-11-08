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

import org.apache.cayenne.map.ObjAttribute;

/**
 * An attribute property mapped to a field of an embeddable object held by the persistent object. Its
 * {@link #injectValueHolder(Object) value holder} is the embeddable itself, created when the object is prepared for
 * access, so that the field can be read and written like any other property.
 *
 * @since 6.0
 */
public class EmbeddedAttributeProperty extends SimpleAttributeProperty {

    private final EmbeddableDescriptor embeddableDescriptor;
    private final Accessor embeddedAccessor;

    /**
     * @param embeddableDescriptor the descriptor of the embeddable, creating its objects
     * @param embeddedAccessor     the accessor of the embedded object on the persistent object
     * @param accessor             the accessor of the field of the embedded object
     */
    public EmbeddedAttributeProperty(
            ClassDescriptor owner,
            EmbeddableDescriptor embeddableDescriptor,
            Accessor embeddedAccessor,
            EmbeddedFieldAccessor accessor,
            ObjAttribute attribute) {
        super(owner, accessor, attribute);
        this.embeddableDescriptor = embeddableDescriptor;
        this.embeddedAccessor = embeddedAccessor;
    }

    /**
     * Creates the embeddable object of the persistent object if it has none, bound to the object.
     */
    @Override
    public void injectValueHolder(Object object) throws PropertyException {
        if (embeddedAccessor.getValue(object) == null) {
            embeddedAccessor.setValue(object, embeddableDescriptor.createObject(object, embeddedAccessor.getName()));
        }
    }
}
