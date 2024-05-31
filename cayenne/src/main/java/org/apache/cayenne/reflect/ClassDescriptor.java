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

import java.util.Collection;
import java.util.Map;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;

/**
 * A runtime descriptor of an persistent class.
 * 
 * @since 1.2
 */
public interface ClassDescriptor {

    /**
     * Returns an ObjEntity associated with this descriptor.
     * 
     * @since 3.0
     */
    ObjEntity getEntity();

    /**
     * Returns a collection of DbEntities that are the root tables for this descriptor's
     * ObjEntity. Usually such collection would contain only one entity, however in cases
     * involving subclass horizontal inheritance, it will be more than one, and in cases
     * of abstract entities with no subclasses, the collection will be empty.
     * 
     * @since 3.0
     */
    Collection<DbEntity> getRootDbEntities();

    /**
     * Returns information about additional db entities that is used for this ObjEntity (i.e. for flattened attributes).
     * <p>
     * Keys are full paths for corresponding flattened attributes.
     * <p>
     * 
     * @since 5.0
     * @return information about additional db entities
     */
    Map<CayennePath, AdditionalDbEntityDescriptor> getAdditionalDbEntities();

    /**
     * @since 3.0
     */
    EntityInheritanceTree getEntityInheritanceTree();

    /**
     * Returns whether this class has persistent subclasses.
     * 
     * @since 3.1
     */
    boolean hasSubclasses();

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
    PropertyDescriptor getProperty(String propertyName);

    /**
     * Returns a Java Bean property descriptor matching property name or null if no such
     * property is found. Lookup DOES NOT including properties from the superclass
     * descriptors. Returned property can be any one of {@link AttributeProperty},
     * {@link ToManyProperty}, {@link ToOneProperty}.
     */
    PropertyDescriptor getDeclaredProperty(String propertyName);

    /**
     * Returns a collection of the properties mapped to id columns.
     * 
     * @since 3.1
     */
    Collection<AttributeProperty> getIdProperties();

    /**
     * Returns a collection of ObjAttribute for the described class, its superclasses and
     * subclasses, that participate in inheritance qualifier. If a discriminator
     * expression specifies a DbAttribute instead of an ObjAttribute, a synthetic
     * ObjAttribute is created and returned.
     * 
     * @since 3.1
     */
    Collection<ObjAttribute> getDiscriminatorColumns();

    /**
     * Returns entity qualifier as a Cayenne expression that includes qualifiers for this
     * entity and all subentities.
     * 
     * @since 3.0
     */
    Expression getEntityQualifier();

    /**
     * Returns a collection over the arc properties whose reverse arcs are to-many maps.
     * I.e. for each ArcProperty in returned collection, the following is true:
     * 
     * <pre>
     * arc.getComplimentaryReverseArc() instanceof ToManyMapProperty
     * </pre>
     * 
     * @since 3.1
     */
    Collection<ArcProperty> getMapArcProperties();

    /**
     * Passes the visitor to the properties "visit" method for all properties declared in
     * this descriptor and all its super-decsriptors. Properties that are overridden in
     * subdescriptors will include overridden information. Walkthrough is terminated in
     * case one of the properties returns false. Returns true if all visited properties
     * returned true, false - if one property returned false.
     */
    boolean visitProperties(PropertyVisitor visitor);

    /**
     * Passes the visitor to the properties "visit" method for all properties declared in
     * this descriptor. This property set excludes inherited properties, even those that
     * got overridden in this subclass. Walkthrough is terminated in case one of the
     * properties returns false. Returns true if all visited properties returned true,
     * false - if one property returned false.
     * 
     * @since 3.0
     */
    boolean visitDeclaredProperties(PropertyVisitor visitor);

    /**
     * Passes the visitor to the properties "visit" method for a combination of all
     * properties, including properties declared in this descriptor, its super
     * descriptors, and all subdescriptors. Walkthrough is terminated in case one of the
     * properties returns false. Returns true if all visited properties returned true,
     * false - if one property returned false.
     * 
     * @since 3.0
     */
    boolean visitAllProperties(PropertyVisitor visitor);

    /**
     * Returns true if an object is not fully resolved.
     */
    boolean isFault(Object object);
}
