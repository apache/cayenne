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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.ObjAttribute;

/**
 * A descriptor of an "attribute" persistent property.
 * 
 * @since 3.0
 */
public class SimpleAttributeProperty extends BaseProperty implements AttributeProperty {

    private ObjAttribute attribute;

    public SimpleAttributeProperty(ClassDescriptor owner, Accessor accessor,
            ObjAttribute attribute) {
        super(owner, accessor);
        this.attribute = attribute;
    }

    @Override
    public boolean visit(PropertyVisitor visitor) {
        return visitor.visitAttribute(this);
    }

    public ObjAttribute getAttribute() {
        return attribute;
    }

    @Override
    public Object readProperty(Object object) throws PropertyException {
        resolveFault(object);
        return super.readProperty(object);
    }

    @Override
    public void writeProperty(Object object, Object oldValue, Object newValue)
            throws PropertyException {
        resolveFault(object);
        super.writeProperty(object, oldValue, newValue);
    }

    protected void resolveFault(Object object) throws PropertyException {
        try {
            Persistent persistent = (Persistent) object;
            ObjectContext context = persistent.getObjectContext();
            if (context != null) {
                context.prepareForAccess(persistent, getName(), false);
            }
        }
        catch (ClassCastException e) {
            throw new PropertyException("Object is not a Persistent: '"
                    + object.getClass().getName()
                    + "'", this, object, e);
        }
    }
}
