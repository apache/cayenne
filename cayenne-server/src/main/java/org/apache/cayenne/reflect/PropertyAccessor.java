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
 * A utility accessor class that wraps a simple property.
 * 
 * @since 3.0
 */
public class PropertyAccessor implements Accessor {

    protected PropertyDescriptor property;

    public PropertyAccessor(PropertyDescriptor property) {
        if (property == null) {
            throw new NullPointerException("Null property");
        }
        this.property = property;
    }

    public String getName() {
        return property.getName();
    }

    public Object getValue(Object object) throws PropertyException {
        return property.readProperty(object);
    }

    public void setValue(Object object, Object newValue) throws PropertyException {
        property.writeProperty(object, null, newValue);
    }
}
