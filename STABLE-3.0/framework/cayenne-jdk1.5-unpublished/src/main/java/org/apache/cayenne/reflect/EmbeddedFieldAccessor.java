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
package org.apache.cayenne.reflect;

/**
 * An accessor for fields storing embedded objects. This accessor will initialize null
 * fields with appropriate embeddable objects when needed during get and set calls.
 * 
 * @since 3.0
 */
public class EmbeddedFieldAccessor implements Accessor {

    protected String propertyPath;
    protected Accessor embeddedAccessor;
    protected Accessor embeddableAccessor;
    protected EmbeddableDescriptor embeddableDescriptor;

    public EmbeddedFieldAccessor(EmbeddableDescriptor embeddableDescriptor,
            Accessor embeddedAccessor, Accessor embeddableAccessor) {
        this.propertyPath = embeddedAccessor.getName()
                + "."
                + embeddableAccessor.getName();
        this.embeddableDescriptor = embeddableDescriptor;
        this.embeddableAccessor = embeddableAccessor;
        this.embeddedAccessor = embeddedAccessor;
    }

    public String getName() {
        return propertyPath;
    }

    public Object getValue(Object object) throws PropertyException {
        return embeddableAccessor.getValue(getEmbeddable(object));
    }

    public void setValue(Object object, Object newValue) throws PropertyException {
        embeddableAccessor.setValue(getEmbeddable(object), newValue);
    }

    /**
     * Returns an embeddable object for the owner object, initializing embeddable if it is
     * null. Currently supports only one level of embedding.
     */
    protected Object getEmbeddable(Object owner) {
        Object embeddable = embeddedAccessor.getValue(owner);
        if (embeddable == null) {
            embeddable = embeddableDescriptor.createObject(owner, embeddedAccessor
                    .getName());
            embeddedAccessor.setValue(owner, embeddable);
        }

        return embeddable;
    }
}
