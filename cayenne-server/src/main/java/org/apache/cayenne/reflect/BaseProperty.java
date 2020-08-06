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
 * An abstract property descriptor that delegates property access to an {@link Accessor}.
 * Used as a superclass for other implementations.
 * 
 * @since 3.0
 */
public abstract class BaseProperty implements PropertyDescriptor {

    protected ClassDescriptor owner;
    protected Accessor accessor;

    // name is derived from accessor, cached here for performance
    final String name;

    public BaseProperty(ClassDescriptor owner, Accessor accessor) {

        if (accessor == null) {
            throw new IllegalArgumentException("Null accessor");
        }

        this.accessor = accessor;
        this.owner = owner;
        this.name = accessor.getName();
    }

    public Object readProperty(Object object) throws PropertyException {
        return readPropertyDirectly(object);
    }

    public void writeProperty(Object object, Object oldValue, Object newValue)
            throws PropertyException {
        writePropertyDirectly(object, oldValue, newValue);
    }

    public String getName() {
        return name;
    }

    public abstract boolean visit(PropertyVisitor visitor);

    /**
     * Does nothing.
     */
    public void injectValueHolder(Object object) throws PropertyException {
        // noop
    }

    public Object readPropertyDirectly(Object object) throws PropertyException {
        return accessor.getValue(object);
    }

    public void writePropertyDirectly(Object object, Object oldValue, Object newValue)
            throws PropertyException {
        accessor.setValue(object, newValue);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(getClass().getName()).append('@').append(
                System.identityHashCode(this)).append('[').append(name).append(']');
        return buffer.toString();
    }
}
