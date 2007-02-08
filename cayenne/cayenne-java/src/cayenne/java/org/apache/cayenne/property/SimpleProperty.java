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

package org.apache.cayenne.property;

/**
 * A property descriptor that provides access to a simple object property, delegating
 * property read/write operations to an accessor.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class SimpleProperty implements Property {

    protected ClassDescriptor owner;
    protected PropertyAccessor accessor;
    
    // name is derived from accessor, cached here for performance
    final String name;

    public SimpleProperty(ClassDescriptor owner, PropertyAccessor accessor) {

        if (accessor == null) {
            throw new IllegalArgumentException("Null accessor");
        }

        this.accessor = accessor;
        this.owner = owner;
        this.name = accessor.getName();
    }

    public Object readProperty(Object object) throws PropertyAccessException {
        return readPropertyDirectly(object);
    }

    public void writeProperty(Object object, Object oldValue, Object newValue)
            throws PropertyAccessException {
        writePropertyDirectly(object, oldValue, newValue);
    }

    public String getName() {
        return name;
    }

    public boolean visit(PropertyVisitor visitor) {
        return visitor.visitProperty(this);
    }

    /**
     * Does nothing.
     */
    public void injectValueHolder(Object object) throws PropertyAccessException {
        // noop
    }

    public void shallowMerge(Object from, Object to) throws PropertyAccessException {
        writePropertyDirectly(to, accessor.readPropertyDirectly(to), accessor
                .readPropertyDirectly(from));
    }

    public Object readPropertyDirectly(Object object) throws PropertyAccessException {
        return accessor.readPropertyDirectly(object);
    }

    public void writePropertyDirectly(Object object, Object oldValue, Object newValue)
            throws PropertyAccessException {
        accessor.writePropertyDirectly(object, oldValue, newValue);
    }
}
