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

import org.apache.cayenne.DataRowLayout;
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
     * Returns the most "specialized" descriptor for a given class.
     * This method assumes that the following is true:
     * 
     * <pre>
     * this.getObjectClass().isAssignableFrom(objectClass)
     * </pre>
     * @deprecated will throw UnsupportedOperationException on invocation,
     *             use {@link #getSubclassDescriptor(String)}
     */
    @Deprecated(since = "5.0", forRemoval = true)
    default ClassDescriptor getSubclassDescriptor(Class<?> unused) {
        throw new UnsupportedOperationException("This method is deprecated, use getSubclassDescriptor(entityName) instead");
    }

    /**
     * Returns the most "specialized" descriptor for a given entity name.
     * @since 5.0
     */
    ClassDescriptor getSubclassDescriptor(String entityName);

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
     * Sets the attribute values that the entity qualifier implies for a new object of the described class, such as
     * the discriminator of a single-table inheritance subentity. Only the qualifier declared on the entity itself is
     * used, as in query translation, where entity qualifiers are not combined with those of the super entities.
     *
     * @since 5.0
     */
    void injectDiscriminatorValues(Object object) throws PropertyException;

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
     * arc.getComplementaryReverseArc() instanceof ToManyMapProperty
     * </pre>
     * 
     * @since 3.1
     */
    Collection<ArcProperty> getMapArcProperties();

    /**
     * Returns the properties declared in this descriptor and all its super-descriptors. Properties that are
     * overridden in subdescriptors include the overridden information. Each property is one of
     * {@link AttributeProperty}, {@link ToOneProperty}, {@link ToManyProperty}. The returned collection must not be
     * modified.
     *
     * @since 5.0
     */
    Collection<PropertyDescriptor> getProperties();

    /**
     * Returns the properties declared in this descriptor. This property set excludes inherited properties, even
     * those that got overridden in this subclass. Each property is one of {@link AttributeProperty},
     * {@link ToOneProperty}, {@link ToManyProperty}. The returned collection must not be modified.
     *
     * @since 5.0
     */
    Collection<PropertyDescriptor> getDeclaredProperties();

    /**
     * Returns a combination of all properties, including properties declared in this descriptor, its super
     * descriptors, and all subdescriptors. Each property is one of {@link AttributeProperty},
     * {@link ToOneProperty}, {@link ToManyProperty}. The returned collection must not be modified.
     *
     * @since 5.0
     */
    Collection<PropertyDescriptor> getAllProperties();

    /**
     * Returns the layout of a snapshot of an object of the described class: the columns of the attributes, the
     * foreign keys of the to-one relationships whose targets propagate their keys to this entity, the primary key,
     * and the primary keys of the additional DB entities, each column listed once. Snapshots of objects are built
     * over this layout, and rows of any layout are merged into objects through it.
     *
     * @since 5.0
     */
    DataRowLayout getSnapshotLayout();

    /**
     * Passes the visitor to the properties "visit" method for all properties declared in
     * this descriptor and all its super-decsriptors. Properties that are overridden in
     * subdescriptors will include overridden information. Walkthrough is terminated in
     * case one of the properties returns false. Returns true if all visited properties
     * returned true, false - if one property returned false.
     *
     * @deprecated iterate over {@link #getProperties()} instead.
     */
    @Deprecated(since = "5.0", forRemoval = true)
    default boolean visitProperties(PropertyVisitor visitor) {
        for (PropertyDescriptor property : getProperties()) {
            if (!property.visit(visitor)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Passes the visitor to the properties "visit" method for all properties declared in
     * this descriptor. This property set excludes inherited properties, even those that
     * got overridden in this subclass. Walkthrough is terminated in case one of the
     * properties returns false. Returns true if all visited properties returned true,
     * false - if one property returned false.
     * 
     * @since 3.0
     * @deprecated iterate over {@link #getDeclaredProperties()} instead.
     */
    @Deprecated(since = "5.0", forRemoval = true)
    default boolean visitDeclaredProperties(PropertyVisitor visitor) {
        for (PropertyDescriptor property : getDeclaredProperties()) {
            if (!property.visit(visitor)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Passes the visitor to the properties "visit" method for a combination of all
     * properties, including properties declared in this descriptor, its super
     * descriptors, and all subdescriptors. Walkthrough is terminated in case one of the
     * properties returns false. Returns true if all visited properties returned true,
     * false - if one property returned false.
     * 
     * @since 3.0
     * @deprecated iterate over {@link #getAllProperties()} instead.
     */
    @Deprecated(since = "5.0", forRemoval = true)
    default boolean visitAllProperties(PropertyVisitor visitor) {
        for (PropertyDescriptor property : getAllProperties()) {
            if (!property.visit(visitor)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if an object is not fully resolved.
     */
    boolean isFault(Object object);
}
