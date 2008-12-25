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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;

public class MockClassDescriptor implements ClassDescriptor {

    public Object createObject() {
        return null;
    }

    public ObjEntity getEntity() {
        return null;
    }

    public EntityInheritanceTree getEntityInheritanceTree() {
        return null;
    }

    /**
     * @since 3.0
     */
    public Collection<DbEntity> getRootDbEntities() {
        return Collections.emptyList();
    }

    public boolean isFault(Object object) {
        return false;
    }

    /**
     * @deprecated since 3.0. Use {@link #visitProperties(PropertyVisitor)} method
     *             instead.
     */
    public Iterator<Property> getProperties() {
        return null;
    }

    public Expression getEntityQualifier() {
        return null;
    }

    public Iterator<Property> getIdProperties() {
        return null;
    }

    public Iterator<ObjAttribute> getDiscriminatorColumns() {
        return null;
    }

    public Iterator<ArcProperty> getMapArcProperties() {
        return null;
    }

    public ClassDescriptor getSubclassDescriptor(Class<?> objectClass) {
        return this;
    }

    public Accessor getObjectContextProperty() {
        return null;
    }

    public Accessor getObjectIdProperty() {
        return null;
    }

    public Accessor getPersistenceStateProperty() {
        return null;
    }

    public void injectValueHolders(Object object) throws PropertyException {
    }

    public void shallowMerge(Object from, Object to) throws PropertyException {
    }

    public Property getDeclaredProperty(String propertyName) {
        return null;
    }

    public Class<?> getObjectClass() {
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

    public boolean visitAllProperties(PropertyVisitor visitor) {
        return true;
    }

    public boolean visitDeclaredProperties(PropertyVisitor visitor) {
        return true;
    }
}
