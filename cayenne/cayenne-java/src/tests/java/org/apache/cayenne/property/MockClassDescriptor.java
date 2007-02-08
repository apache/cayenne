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

import java.util.Iterator;

public class MockClassDescriptor implements ClassDescriptor {

    public Object createObject() {
        return null;
    }

    public boolean isFault(Object object) {
        return false;
    }

    public Iterator getProperties() {
        return null;
    }

    public ClassDescriptor getSubclassDescriptor(Class objectClass) {
        return this;
    }

    public PropertyAccessor getObjectContextProperty() {
        return null;
    }

    public PropertyAccessor getObjectIdProperty() {
        return null;
    }

    public PropertyAccessor getPersistenceStateProperty() {
        return null;
    }

    public void injectValueHolders(Object object) throws PropertyAccessException {
    }

    public void shallowMerge(Object from, Object to) throws PropertyAccessException {
    }

    public Property getDeclaredProperty(String propertyName) {
        return null;
    }

    public Class getObjectClass() {
        return null;
    }

    public Property getProperty(String propertyName) {
        return null;
    }

    public ClassDescriptor getSuperclassDescriptor() {
        return null;
    }

    public boolean visitProperties(PropertyVisitor visitor) {
        return true;
    }
}
