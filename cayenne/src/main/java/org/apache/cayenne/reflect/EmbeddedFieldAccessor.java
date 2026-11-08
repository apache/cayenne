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

/**
 * An accessor of a field of an embeddable object held by a persistent object: the composition of the accessor of
 * the embedded object on the owner and the accessor of the field on the embeddable. The accessor does not create
 * the embeddable; the embedded attribute of the compiled entity does, when the framework prepares an object for
 * access. Reading a field of a missing embeddable yields null, writing one is an error. Only one level of
 * embedding is supported.
 *
 * @since 3.0
 */
public class EmbeddedFieldAccessor implements Accessor {

    protected String propertyPath;
    protected Accessor embeddedAccessor;
    protected Accessor embeddableAccessor;

    /**
     * @since 6.0
     */
    public EmbeddedFieldAccessor(Accessor embeddedAccessor, Accessor embeddableAccessor) {
        this.propertyPath = embeddedAccessor.getName() + "." + embeddableAccessor.getName();
        this.embeddableAccessor = embeddableAccessor;
        this.embeddedAccessor = embeddedAccessor;
    }

    public String getName() {
        return propertyPath;
    }

    public Object getValue(Object object) throws PropertyException {
        Object embeddable = embeddedAccessor.getValue(object);
        return embeddable != null ? embeddableAccessor.getValue(embeddable) : null;
    }

    public void setValue(Object object, Object newValue) throws PropertyException {
        Object embeddable = embeddedAccessor.getValue(object);
        if (embeddable == null) {
            throw new PropertyException("Can't set '" + propertyPath + "': the embedded object '"
                    + embeddedAccessor.getName() + "' is null", this, object);
        }
        embeddableAccessor.setValue(embeddable, newValue);
    }
}
