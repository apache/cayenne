/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.map;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.ValueHolder;
import org.objectstyle.cayenne.property.BaseClassDescriptor;
import org.objectstyle.cayenne.property.BeanAccessor;
import org.objectstyle.cayenne.property.ClassDescriptor;
import org.objectstyle.cayenne.property.DataObjectAccessor;
import org.objectstyle.cayenne.property.FieldAccessor;
import org.objectstyle.cayenne.property.ListProperty;
import org.objectstyle.cayenne.property.PersistentObjectProperty;
import org.objectstyle.cayenne.property.Property;
import org.objectstyle.cayenne.property.PropertyAccessException;
import org.objectstyle.cayenne.property.PropertyAccessor;
import org.objectstyle.cayenne.property.SimpleProperty;
import org.objectstyle.cayenne.property.ToManyListProperty;
import org.objectstyle.cayenne.property.ValueHolderProperty;

/**
 * A ClassDescriptor describing a persistent bean based on ObjEntity.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class EntityDescriptor extends BaseClassDescriptor {

    protected ObjEntity entity;

    // compiled properties
    protected boolean dataObject;
    boolean persistent;

    /**
     * Creates and compiles a class descriptor for a given entity. A second optional
     * 'superclassDescriptor' parameter should be used if an entity has a super-entity.
     */
    public EntityDescriptor(ObjEntity entity, ClassDescriptor superclassDescriptor) {
        super(superclassDescriptor);
        this.entity = entity;
    }

    public void shallowMerge(Object from, Object to) throws PropertyAccessException {
        super.shallowMerge(from, to);

        if (dataObject && from instanceof DataObject && to instanceof DataObject) {
            ((DataObject) to)
                    .setSnapshotVersion(((DataObject) from).getSnapshotVersion());
        }
    }

    /**
     * Returns ObjEntity described by this object.
     */
    public ObjEntity getEntity() {
        return entity;
    }

    /**
     * Registers a property. This method is useful to customize default ClassDescriptor
     * generated from ObjEntity by adding new properties or overriding the standard ones.
     */
    public void setDeclaredProperty(Property property) {
        declaredProperties.put(property.getName(), property);
    }

    /**
     * Removes declared property. This method can be used to customize default
     * ClassDescriptor generated from ObjEntity.
     */
    public void removeDeclaredProperty(String propertyName) {
        declaredProperties.remove(propertyName);
    }

    /**
     * Prepares the descriptor. A descriptor must be explicitly compiled before it can
     * operate.
     */
    public void compile(EntityResolver resolver) {
        if (entity == null) {
            throw new IllegalStateException(
                    "Entity is not initialized, can't index descriptor.");
        }

        // compile common stuff
        this.objectClass = entity.getJavaClass();
        this.persistent = Persistent.class.isAssignableFrom(objectClass);
        this.dataObject = persistent && DataObject.class.isAssignableFrom(objectClass);

        compileSpecialProperties();

        // init property descriptors...
        Map allDescriptors = new HashMap();

        compileRelationships(resolver, allDescriptors);

        // before we compile attributes, extract all relationship descriptors to a
        // separate value holder map
        if(!allDescriptors.isEmpty()) {
            this.valueHolderProperties = new HashMap(allDescriptors);
        }

        compileAttributes(allDescriptors);

        this.declaredProperties = allDescriptors;

        // init subclass lookup tree
        EntityInheritanceTree inheritanceTree = resolver.lookupInheritanceTree(entity);
        if (inheritanceTree == null) {
            this.subclassDescriptors = null;
        }
        else {
            Map subclassMapping = new HashMap();
            compileSubclassMapping(resolver, subclassMapping, inheritanceTree);
            this.subclassDescriptors = subclassMapping;
        }
    }

    protected void compileSubclassMapping(
            EntityResolver resolver,
            Map subclassDescriptors,
            EntityInheritanceTree treeNode) {
        ObjEntity entity = treeNode.getEntity();
        ClassDescriptor descriptor = resolver.getClassDescriptor(entity.getName());
        subclassDescriptors.put(entity.getClassName(), descriptor);

        Iterator it = treeNode.getChildren().iterator();
        while (it.hasNext()) {
            EntityInheritanceTree child = (EntityInheritanceTree) it.next();
            compileSubclassMapping(resolver, subclassDescriptors, child);
        }
    }

    /**
     * Implements an attributes compilation step. Called internally from "compile".
     */
    protected void compileSpecialProperties() {
        this.persistenceStateProperty = new BeanAccessor(
                objectClass,
                "persistenceState",
                Integer.TYPE);
    }

    /**
     * Implements an attributes compilation step. Called internally from "compile".
     */
    protected void compileAttributes(Map allDescriptors) {

        // only include this entity attributes and skip superclasses...
        Iterator it = entity.getDeclaredAttributes().iterator();
        while (it.hasNext()) {
            ObjAttribute attribute = (ObjAttribute) it.next();

            Class propertyType = attribute.getJavaClass();
            PropertyAccessor accessor = makeAccessor(attribute.getName(), propertyType);
            allDescriptors.put(attribute.getName(), persistent
                    ? new SimplePersistentProperty(this, accessor)
                    : new SimpleProperty(this, accessor));
        }
    }

    /**
     * Implements a relationships compilation step. Maps each to-one relationship to a
     * ValueHolderProperty and to-many - to CollectionProperty. Called internally from
     * "compile" method.
     */
    protected void compileRelationships(EntityResolver resolver, Map allDescriptors) {

        // only include this entity relationships and skip superclasses...
        Iterator it = entity.getDeclaredRelationships().iterator();
        while (it.hasNext()) {

            ObjRelationship relationship = (ObjRelationship) it.next();
            ClassDescriptor targetDescriptor = resolver.getClassDescriptor(relationship
                    .getTargetEntityName());
            String reverseName = relationship.getReverseRelationshipName();

            Property property;
            if (relationship.isToMany()) {

                PropertyAccessor accessor = makeAccessor(
                        relationship.getName(),
                        List.class);

                if (dataObject) {
                    property = new ToManyListProperty(
                            this,
                            targetDescriptor,
                            accessor,
                            reverseName);
                }
                else {
                    property = new ListProperty(
                            this,
                            targetDescriptor,
                            accessor,
                            reverseName);
                }
            }
            else {

                if (dataObject) {
                    ObjEntity targetEntity = (ObjEntity) relationship.getTargetEntity();
                    PropertyAccessor accessor = makeAccessor(
                            relationship.getName(),
                            targetEntity.getJavaClass());
                    property = new PersistentObjectProperty(
                            this,
                            targetDescriptor,
                            accessor,
                            reverseName);
                }
                else {
                    PropertyAccessor accessor = makeAccessor(
                            relationship.getName(),
                            ValueHolder.class);
                    property = new ValueHolderProperty(
                            this,
                            targetDescriptor,
                            accessor,
                            reverseName);
                }
            }

            allDescriptors.put(relationship.getName(), property);
        }
    }

    /*
     * Creates an accessor for the property.
     */
    PropertyAccessor makeAccessor(String propertyName, Class propertyType)
            throws PropertyAccessException {

        if (dataObject) {
            return new DataObjectAccessor(propertyName);
        }

        try {
            return new FieldAccessor(objectClass, propertyName, propertyType);
        }
        catch (Throwable th) {

            throw new PropertyAccessException("Can't create accessor for property '"
                    + propertyName
                    + "' of class '"
                    + objectClass.getName()
                    + "'", null, null);
        }
    }

    /**
     * Overrides toString method of Object to provide a meaningful description.
     */
    public String toString() {
        String entityName = (entity != null) ? entity.getName() : null;
        String className = (objectClass != null) ? objectClass.getName() : null;
        return new ToStringBuilder(this).append("entity", entityName).append(
                "objectClass",
                className).toString();
    }

    // TODO: andrus, 4/26/2006 - this property was added when 1.2 was in public API freeze
    // - move it to the property package past 1.2
    final class SimplePersistentProperty extends SimpleProperty {

        SimplePersistentProperty(ClassDescriptor owner, PropertyAccessor accessor) {
            super(owner, accessor);
        }

        public Object readProperty(Object object) throws PropertyAccessException {
            resolveFault(object);
            return super.readProperty(object);
        }

        public void writeProperty(Object object, Object oldValue, Object newValue)
                throws PropertyAccessException {
            resolveFault(object);
            super.writeProperty(object, oldValue, newValue);
        }

        void resolveFault(Object object) throws PropertyAccessException {
            try {
                Persistent persistent = (Persistent) object;
                ObjectContext context = persistent.getObjectContext();
                if (context != null) {
                    context.prepareForAccess(persistent, getName());
                }
            }
            catch (ClassCastException e) {
                throw new PropertyAccessException("Object is not a Persistent: '"
                        + object.getClass().getName()
                        + "'", this, object, e);
            }
        }
    }
}
