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

import org.apache.cayenne.map.ObjEntity;

/**
 * A runtime descriptor of an persistent class.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public interface ClassDescriptor {

    /**
     * Returns an ObjEntity associated with this descriptor.
     * 
     * @since 3.0
     */
    ObjEntity getEntity();

    /**
     * Returns a class mapped by this descriptor.
     */
    Class<?> getObjectClass();

    /**
     * Returns a descriptor of the mapped superclass or null if the descriptor's entity
     * sits at the top of inheritance hierarchy or no inheritance is mapped.
     */
    ClassDescriptor getSuperclassDescriptor();

    /**
     * Returns the most "specialized" descriptor for a given class. This method assumes
     * that the following is true:
     * 
     * <pre>
     * this.getObjectClass().isAssignableFrom(objectClass)
     * </pre>
     */
    ClassDescriptor getSubclassDescriptor(Class<?> objectClass);

    /**
     * Creates a new instance of a class described by this object.
     */
    Object createObject();

    /**
     * Prepares object properties for access. This may include injection of value holders
     * into the object and such.
     */
    void injectValueHolders(Object object) throws PropertyException;

    /**
     * Merges object properties from one object to another, avoiding traversal of the
     * ArcProperties.
     */
    void shallowMerge(Object from, Object to) throws PropertyException;

    /**
     * Returns a property descriptor matching property name, or null if no such property
     * is found. Lookup includes properties from this descriptor and all its superclass
     * descriptors. Returned property can be any one of {@link AttributeProperty},
     * {@link ToManyProperty}, {@link ToOneProperty}.
     */
    Property getProperty(String propertyName);

    /**
     * Returns a Java Bean property descriptor matching property name or null if no such
     * property is found. Lookup DOES NOT including properties from the superclass
     * descriptors. Returned property can be any one of {@link AttributeProperty},
     * {@link ToManyProperty}, {@link ToOneProperty}.
     */
    Property getDeclaredProperty(String propertyName);

    /**
     * Returns an Iterator over descriptor properties.
     * 
     * @deprecated since 3.0. Use {@link #visitProperties(PropertyVisitor)} method
     *             instead.
     */
    Iterator<Property> getProperties();

    /**
     * Returns an iterator over the properties mapped to id columns.
     * 
     * @since 3.0
     */
    Iterator<Property> getIdProperties();

    /**
     * Returns an iterator over the arc properties whose reverse arcs are to-many maps.
     * I.e. for each ArcProperty in the iterator, the following is true:
     * 
     * <pre>
     * arc.getComplimentaryReverseArc() instanceof ToManyMapProperty
     * </pre>
     * 
     * @since 3.0
     */
    Iterator<ArcProperty> getMapArcProperties();

    /**
     * Passes the visitor to all properties "visit" method, terminating properties
     * walkthrough in case one of the properties returns false. Returns true if all
     * visited properties returned true, false - if one property returned false.
     */
    boolean visitProperties(PropertyVisitor visitor);

    /**
     * Passes the visitor to the properties "visit" method for all properties declared in
     * this descriptor, terminating properties walkthrough in case one of the properties
     * returns false. Returns true if all visited properties returned true, false - if one
     * property returned false.
     * 
     * @since 3.0
     */
    boolean visitDeclaredProperties(PropertyVisitor visitor);

    /**
     * Passes the visitor to the properties "visit" method for all properties declared in
     * this descriptor, its super and subdescriptors, terminating properties walkthrough
     * in case one of the properties returns false. Returns true if all visited properties
     * returned true, false - if one property returned false.
     * 
     * @since 3.0
     */
    boolean visitAllProperties(PropertyVisitor visitor);

    /**
     * Returns true if an object is not fully resolved.
     */
    boolean isFault(Object object);
}
