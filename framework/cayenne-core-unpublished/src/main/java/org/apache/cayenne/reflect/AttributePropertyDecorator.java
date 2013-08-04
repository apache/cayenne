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

import org.apache.cayenne.map.ObjAttribute;

/**
 * A class that presents an AttributeProperty view for an inherited attribute at
 * a given subclass. It applies all needed attribute overrides.
 * 
 * @since 3.0
 */
class AttributePropertyDecorator implements AttributeProperty {

    private AttributeProperty delegate;
    private ObjAttribute attribute;

    AttributePropertyDecorator(ClassDescriptor owningClassDescriptor, AttributeProperty delegate) {

        this.delegate = delegate;
        this.attribute = owningClassDescriptor.getEntity().getAttribute(delegate.getName());
    }

    public ObjAttribute getAttribute() {
        return attribute;
    }

    public boolean visit(PropertyVisitor visitor) {
        return visitor.visitAttribute(this);
    }

    public String getName() {
        return delegate.getName();
    }

    public void injectValueHolder(Object object) throws PropertyException {
        delegate.injectValueHolder(object);
    }

    public Object readProperty(Object object) throws PropertyException {
        return delegate.readProperty(object);
    }

    public Object readPropertyDirectly(Object object) throws PropertyException {
        return delegate.readPropertyDirectly(object);
    }

    public void writeProperty(Object object, Object oldValue, Object newValue) throws PropertyException {
        delegate.writeProperty(object, oldValue, newValue);
    }

    public void writePropertyDirectly(Object object, Object oldValue, Object newValue) throws PropertyException {
        delegate.writePropertyDirectly(object, oldValue, newValue);
    }
}
