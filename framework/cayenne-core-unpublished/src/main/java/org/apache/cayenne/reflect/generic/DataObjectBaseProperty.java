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
package org.apache.cayenne.reflect.generic;

import java.io.Serializable;

import org.apache.cayenne.DataObject;
import org.apache.cayenne.reflect.Property;
import org.apache.cayenne.reflect.PropertyException;
import org.apache.cayenne.reflect.PropertyVisitor;

/**
 * A superclass of DataObject properties that accesses object via DataObject methods.
 * 
 * @since 3.0
 */
abstract class DataObjectBaseProperty implements Property, Serializable {

    public abstract String getName();

    public abstract void injectValueHolder(Object object) throws PropertyException;

    public abstract boolean visit(PropertyVisitor visitor);

    public Object readProperty(Object object) throws PropertyException {
        try {
            return toDataObject(object).readProperty(getName());
        }
        catch (Throwable th) {
            throw new PropertyException(
                    "Error reading DataObject property: " + getName(),
                    this,
                    object,
                    th);
        }
    }

    public void writeProperty(Object object, Object oldValue, Object newValue)
            throws PropertyException {
        try {
            toDataObject(object).writeProperty(getName(), newValue);
        }
        catch (Throwable th) {
            throw new PropertyException(
                    "Error writing DataObject property: " + getName(),
                    this,
                    object,
                    th);
        }
    }

    public Object readPropertyDirectly(Object object) throws PropertyException {
        try {
            return toDataObject(object).readPropertyDirectly(getName());
        }
        catch (Throwable th) {
            throw new PropertyException(
                    "Error reading DataObject property: " + getName(),
                    this,
                    object,
                    th);
        }
    }

    public void writePropertyDirectly(Object object, Object oldValue, Object newValue)
            throws PropertyException {
        try {
            toDataObject(object).writePropertyDirectly(getName(), newValue);
        }
        catch (Throwable th) {
            throw new PropertyException(
                    "Error writing DataObject property: " + getName(),
                    this,
                    object,
                    th);
        }
    }

    protected final DataObject toDataObject(Object object) throws PropertyException {
        try {
            return (DataObject) object;
        }
        catch (ClassCastException e) {
            throw new PropertyException("Object is not a DataObject: '"
                    + object.getClass().getName()
                    + "'", this, object, e);
        }
    }
}
