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

import java.util.Iterator;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.ObjEntity;

/**
 * A ClassDescriptor wrapper that compiles decorated descriptor lazily on first access.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class LazyClassDescriptorDecorator implements ClassDescriptor {

    protected ClassDescriptor descriptor;
    protected ClassDescriptorMap descriptorMap;
    protected String entityName;

    public LazyClassDescriptorDecorator(ClassDescriptorMap descriptorMap,
            String entityName) {
        this.descriptorMap = descriptorMap;
        this.entityName = entityName;
    }

    /**
     * Checks whether decorated descriptor is initialized, and if not, creates it using
     * parent {@link ClassDescriptorMap}.
     */
    protected void checkDescriptorInitialized() {
        if (descriptor == null) {
            descriptor = descriptorMap.createDescriptor(entityName);
        }
    }

    /**
     * Returns underlying descriptor used to delegate all processing, resolving it if
     * needed.
     */
    public ClassDescriptor getDescriptor() {
        checkDescriptorInitialized();
        return descriptor;
    }

    public Object createObject() {
        checkDescriptorInitialized();
        return descriptor.createObject();
    }

    public Property getDeclaredProperty(String propertyName) {
        checkDescriptorInitialized();
        return descriptor.getDeclaredProperty(propertyName);
    }

    public ObjEntity getEntity() {
        checkDescriptorInitialized();
        return descriptor.getEntity();
    }

    public Class<?> getObjectClass() {

        // note that we can resolve Object class without triggering descriptor resolution.
        // This is very helpful when compiling POJO relationships
        if (descriptor == null) {

            ObjEntity entity = descriptorMap.getResolver().getObjEntity(entityName);
            if (entity != null) {
                return entity.getJavaClass();
            }
        }

        checkDescriptorInitialized();
        return descriptor.getObjectClass();
    }

    /**
     * @deprecated since 3.0. Use {@link #visitProperties(PropertyVisitor)} method
     *             instead.
     */
    public Iterator<Property> getProperties() {
        checkDescriptorInitialized();
        return descriptor.getProperties();
    }

    public Iterator<Property> getIdProperties() {
        checkDescriptorInitialized();
        return descriptor.getIdProperties();
    }

    public Iterator<DbAttribute> getDiscriminatorColumns() {
        checkDescriptorInitialized();
        return descriptor.getDiscriminatorColumns();
    }

    public Expression getEntityQualifier() {
        checkDescriptorInitialized();
        return descriptor.getEntityQualifier();
    }

    public Iterator<ArcProperty> getMapArcProperties() {
        checkDescriptorInitialized();
        return descriptor.getMapArcProperties();
    }

    public Property getProperty(String propertyName) {
        checkDescriptorInitialized();
        return descriptor.getProperty(propertyName);
    }

    public ClassDescriptor getSubclassDescriptor(Class<?> objectClass) {
        checkDescriptorInitialized();
        return descriptor.getSubclassDescriptor(objectClass);
    }

    public ClassDescriptor getSuperclassDescriptor() {
        checkDescriptorInitialized();
        return descriptor.getSuperclassDescriptor();
    }

    public void injectValueHolders(Object object) throws PropertyException {
        checkDescriptorInitialized();
        descriptor.injectValueHolders(object);
    }

    public boolean isFault(Object object) {
        checkDescriptorInitialized();
        return descriptor.isFault(object);
    }

    public void shallowMerge(Object from, Object to) throws PropertyException {
        checkDescriptorInitialized();
        descriptor.shallowMerge(from, to);
    }

    public boolean visitDeclaredProperties(PropertyVisitor visitor) {
        checkDescriptorInitialized();
        return descriptor.visitDeclaredProperties(visitor);
    }

    public boolean visitProperties(PropertyVisitor visitor) {
        checkDescriptorInitialized();
        return descriptor.visitProperties(visitor);
    }

    public boolean visitAllProperties(PropertyVisitor visitor) {
        checkDescriptorInitialized();
        return descriptor.visitAllProperties(visitor);
    }
}
