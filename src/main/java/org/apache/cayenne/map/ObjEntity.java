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

package org.apache.cayenne.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.ObjAttributeListener;
import org.apache.cayenne.map.event.ObjEntityListener;
import org.apache.cayenne.map.event.ObjRelationshipListener;
import org.apache.cayenne.map.event.RelationshipEvent;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.commons.collections.Transformer;

/**
 * ObjEntity is a mapping descriptor for a DataObject Java class. It contains the
 * information about the Java class itself, as well as its mapping to the DbEntity layer.
 * 
 * @author Misha Shengaout
 * @author Andrus Adamchik
 */
public class ObjEntity extends Entity implements ObjEntityListener, ObjAttributeListener,
        ObjRelationshipListener {

    final public static int LOCK_TYPE_NONE = 0;
    final public static int LOCK_TYPE_OPTIMISTIC = 1;

    // do not import CayenneDataObject as it introduces unneeded client dependency
    static final String CAYENNE_DATA_OBJECT_CLASS = "org.apache.cayenne.CayenneDataObject";
    /**
     * A collection of default "generic" entity classes excluded from class generation.
     * 
     * @since 1.2
     */
    protected static final Collection DEFAULT_GENERIC_CLASSES = Arrays
            .asList(new String[] {
                CAYENNE_DATA_OBJECT_CLASS
            });

    protected String superClassName;
    protected String className;
    protected String dbEntityName;
    protected String superEntityName;
    protected Expression qualifier;
    protected boolean readOnly;
    protected int lockType;

    protected boolean serverOnly;
    protected String clientClassName;
    protected String clientSuperClassName;

    protected List entityListeners;
    protected SortedMap callbackMethods;
    protected boolean excludingDefaultListeners;
    protected boolean excludingSuperclassListeners;

    public ObjEntity() {
        this(null);
    }

    public ObjEntity(String name) {
        setName(name);
        this.lockType = LOCK_TYPE_NONE;
        this.callbackMethods = new TreeMap();
        this.entityListeners = new ArrayList(2);
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<obj-entity name=\"");
        encoder.print(getName());

        // additionally validate that superentity exists
        if (getSuperEntityName() != null && getSuperEntity() != null) {
            encoder.print("\" superEntityName=\"");
            encoder.print(getSuperEntityName());
        }

        if (isServerOnly()) {
            encoder.print("\" serverOnly=\"true");
        }

        if (getClassName() != null) {
            encoder.print("\" className=\"");
            encoder.print(getClassName());
        }

        if (getClientClassName() != null) {
            encoder.print("\" clientClassName=\"");
            encoder.print(getClientClassName());
        }

        if (isReadOnly()) {
            encoder.print("\" readOnly=\"true");
        }

        if (getDeclaredLockType() == LOCK_TYPE_OPTIMISTIC) {
            encoder.print("\" lock-type=\"optimistic");
        }

        if (getSuperEntityName() == null && getDbEntity() != null) {
            encoder.print("\" dbEntityName=\"");
            encoder.print(Util.encodeXmlAttribute(getDbEntityName()));
        }

        if (getSuperEntityName() == null && getSuperClassName() != null) {
            encoder.print("\" superClassName=\"");
            encoder.print(getSuperClassName());
        }

        if (getSuperEntityName() == null && getClientSuperClassName() != null) {
            encoder.print("\" clientSuperClassName=\"");
            encoder.print(getClientSuperClassName());
        }

        encoder.println("\">");
        encoder.indent(1);

        if (qualifier != null) {
            encoder.print("<qualifier>");
            qualifier.encodeAsXML(encoder);
            encoder.println("</qualifier>");
        }

        // store attributes
        encoder.print(getDeclaredAttributes());

        encoder.indent(-1);
        encoder.println("</obj-entity>");
    }

    /**
     * Returns an ObjEntity stripped of any server-side information, such as DbEntity
     * mapping. "clientClassName" property of this entity is used to intialize "className"
     * property of returned entity.
     * 
     * @since 1.2
     */
    public ObjEntity getClientEntity() {

        ObjEntity entity = new ObjEntity(getName());
        entity.setClassName(getClientClassName());
        entity.setSuperClassName(getClientSuperClassName());
        entity.setSuperEntityName(getSuperEntityName());

        // TODO: should we also copy lock type?

        // copy attributes
        Iterator attributes = getDeclaredAttributes().iterator();
        while (attributes.hasNext()) {
            ObjAttribute attribute = (ObjAttribute) attributes.next();
            entity.addAttribute(attribute.getClientAttribute());
        }

        // copy relationships
        Iterator relationships = getDeclaredRelationships().iterator();
        while (relationships.hasNext()) {
            ObjRelationship relationship = (ObjRelationship) relationships.next();
            entity.addRelationship(relationship.getClientRelationship());
        }

        // TODO: andrus 2/5/2007 - copy embeddables
        // TODO: andrus 2/5/2007 - copy listeners and callback methods

        return entity;
    }

    /**
     * Returns Java class of persistent objects described by this entity. For generic
     * entities with no class specified explicitly, default DataMap superclass is used,
     * and if it is not set - CayenneDataObject is used. Casts any thrown exceptions into
     * CayenneRuntimeException.
     * 
     * @since 1.2
     */
    public Class getJavaClass() {
        String name = getClassName();

        if (name == null && getDataMap() != null) {
            name = getDataMap().getDefaultSuperclass();
        }

        if (name == null) {
            name = CAYENNE_DATA_OBJECT_CLASS;
        }

        try {
            return Util.getJavaClass(name);
        }
        catch (ClassNotFoundException e) {
            throw new CayenneRuntimeException("Failed to load class "
                    + name
                    + ": "
                    + e.getMessage(), e);
        }
    }

    /**
     * Returns an unmodifiable list of registered {@link EntityListener} objects. Note
     * that since the order of listeners is significant a list, not just a generic
     * Collection is returned.
     * 
     * @since 3.0
     */
    public List getEntityListeners() {
        return Collections.unmodifiableList(entityListeners);
    }

    /**
     * Adds a new EntityListener.
     * 
     * @since 3.0
     * @throws IllegalArgumentException if a listener for the same class name is already
     *             registered.
     */
    public void addEntityListener(EntityListener listener) {
        Iterator it = entityListeners.iterator();
        while (it.hasNext()) {
            EntityListener next = (EntityListener) it.next();
            if (listener.getClassName().equals(next.getClassName())) {
                throw new IllegalArgumentException("Duplicate listener for "
                        + next.getClassName());
            }
        }

        entityListeners.add(listener);
    }

    /**
     * Removes a listener matching class name.
     * 
     * @since 3.0
     */
    public void removeEntityListener(String className) {
        Iterator it = entityListeners.iterator();
        while (it.hasNext()) {
            EntityListener next = (EntityListener) it.next();
            if (className.equals(next.getClassName())) {
                it.remove();
                break;
            }
        }
    }
    
    /**
     * @since 3.0
     */
    public EntityListener getEntityListener(String className) {
        Iterator it = entityListeners.iterator();
        while (it.hasNext()) {
            EntityListener next = (EntityListener) it.next();
            if (className.equals(next.getClassName())) {
                return next;
            }
        }
        
        return null;
    }

    /**
     * Returns an unmodifiable sorted map of listener methods.
     * 
     * @since 3.0
     */
    public SortedMap getCallbackMethodsMap() {
        // create a new instance ... Caching unmodifiable map causes
        // serialization issues (esp. with Hessian).
        return Collections.unmodifiableSortedMap(callbackMethods);
    }

    /**
     * Returns an unmodifiable collection of listener methods.
     * 
     * @since 3.0
     */
    public Collection getCallbackMethods() {
        // create a new instance. Caching unmodifiable collection causes
        // serialization issues (esp. with Hessian).
        return Collections.unmodifiableCollection(callbackMethods.values());
    }

    /**
     * Adds new listener method. If a method has no name, IllegalArgumentException is
     * thrown.
     * 
     * @since 3.0
     */
    public void addCallbackMethod(CallbackMethod method) {

        if (method.getName() == null) {
            throw new IllegalArgumentException("Attempt to insert unnamed method.");
        }

        Object existingMethod = callbackMethods.get(method.getName());
        if (existingMethod != null) {
            if (existingMethod == method) {
                return;
            }
            else {
                throw new IllegalArgumentException("An attempt to override method '"
                        + method.getName()
                        + "'");
            }
        }

        callbackMethods.put(method.getName(), method);
    }

    /**
     * @since 3.0
     */
    public CallbackMethod getCallbackMethod(String name) {
        return (CallbackMethod) callbackMethods.get(name);
    }

    /**
     * @since 3.0
     */
    public void removeCallbackMethod(String name) {
        callbackMethods.remove(name);
    }

    /**
     * Returns the type of lock used by this ObjEntity. If this entity is not locked, this
     * method would look in a super entity recursively, until it finds a lock somewhere in
     * the inheritance hierarchy.
     * 
     * @since 1.1
     */
    public int getLockType() {
        // if this entity has an explicit lock,
        // no need to lookup inheritance hierarchy
        if (lockType != LOCK_TYPE_NONE) {
            return lockType;
        }

        ObjEntity superEntity = getSuperEntity();
        return (superEntity != null) ? superEntity.getLockType() : lockType;
    }

    /**
     * Returns the type of lock used by this ObjEntity, regardless of what locking type is
     * used by super entities.
     * 
     * @since 1.1
     */
    public int getDeclaredLockType() {
        return lockType;
    }

    /**
     * Sets the type of lock used by this ObjEntity.
     * 
     * @since 1.1
     */
    public void setDeclaredLockType(int i) {
        lockType = i;
    }

    /**
     * Returns whether this entity is "generic", meaning it is not mapped to a unique Java
     * class. Criterion for generic entities is that it either has no Java class mapped or
     * its class is the same as DataMap's default superclass, or it is CayenneDataObject.
     * 
     * @since 1.2
     */
    public boolean isGeneric() {
        String className = getClassName();
        return className == null
                || DEFAULT_GENERIC_CLASSES.contains(className)
                || (getDataMap() != null && className.equals(getDataMap()
                        .getDefaultSuperclass()));
    }

    /**
     * Returns true if this entity is allowed to be used on the client. Checks that parent
     * DataMap allows client entities and also that this entity is not explicitly disabled
     * for the client use.
     * 
     * @since 1.2
     */
    public boolean isClientAllowed() {
        return (getDataMap() == null || isServerOnly()) ? false : getDataMap()
                .isClientSupported();
    }

    /**
     * Returns true if this entity is not available on the client.
     * 
     * @since 1.2
     */
    public boolean isServerOnly() {
        return serverOnly;
    }

    /**
     * Sets whether this entity is available on the client.
     * 
     * @since 1.2
     */
    public void setServerOnly(boolean serverOnly) {
        this.serverOnly = serverOnly;
    }

    /**
     * Returns a qualifier that imposes a restriction on what objects belong to this
     * entity. Returned qualifier is the one declared in this entity, and does not include
     * qualifiers declared in super entities.
     * 
     * @since 1.1
     */
    public Expression getDeclaredQualifier() {
        return qualifier;
    }

    /**
     * Returns an entity name for a parent entity in the inheritance hierarchy.
     * 
     * @since 1.1
     */
    public String getSuperEntityName() {
        return superEntityName;
    }

    /**
     * Sets a qualifier that imposes a limit on what objects belong to this entity.
     * 
     * @since 1.1
     */
    public void setDeclaredQualifier(Expression qualifier) {
        this.qualifier = qualifier;
    }

    /**
     * Sets an entity name for a parent entity in the inheritance hierarchy.
     * 
     * @since 1.1
     */
    public void setSuperEntityName(String superEntityName) {
        this.superEntityName = superEntityName;
    }

    /**
     * Returns the name of DataObject class described by this entity.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the name of the DataObject class described by this entity.
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Returns the name of ClientDataObject class described by this entity.
     * 
     * @since 1.2
     */
    public String getClientClassName() {
        return clientClassName;
    }

    /**
     * Sets the name of the ClientDataObject class described by this entity.
     * 
     * @since 1.2
     */
    public void setClientClassName(String clientClassName) {
        this.clientClassName = clientClassName;
    }

    /**
     * Returns a fully-qualified name of the super class of the DataObject class. This
     * value is used as a hint for class generation. If the entity inherits from another
     * entity, a superclass is the class of that entity.
     */
    public String getSuperClassName() {
        ObjEntity superEntity = getSuperEntity();
        return (superEntity != null) ? superEntity.getClassName() : superClassName;
    }

    /**
     * Sets a fully-qualified name of the super class of the DataObject class. This value
     * is used as a hint for class generation.
     * <p>
     * <i>An attempt to set superclass on an inherited entity has no effect, since a class
     * of the super entity is always used as a superclass.</i>
     * </p>
     */
    public void setSuperClassName(String superClassName) {
        this.superClassName = superClassName;
    }

    /**
     * Returns a fully-qualified name of the client-side super class of the DataObject
     * class. This value is used as a hint for class generation. If the entity inherits
     * from another entity, a superclass is the class of that entity.
     * 
     * @since 1.2
     */
    public String getClientSuperClassName() {
        ObjEntity superEntity = getSuperEntity();
        return (superEntity != null)
                ? superEntity.getClientClassName()
                : clientSuperClassName;
    }

    /**
     * Sets a fully-qualified name of the client-side super class of the ClientDataObject
     * class. This value is used as a hint for class generation.
     * <p>
     * <i>An attempt to set superclass on an inherited entity has no effect, since a class
     * of the super entity is always used as a superclass. </i>
     * </p>
     * 
     * @since 1.2
     */
    public void setClientSuperClassName(String clientSuperClassName) {
        this.clientSuperClassName = clientSuperClassName;
    }

    /**
     * Returns a "super" entity in the entity inheritance hierarchy.
     * 
     * @since 1.1
     */
    public ObjEntity getSuperEntity() {
        return (superEntityName != null) ? getNonNullNamespace().getObjEntity(
                superEntityName) : null;
    }

    /**
     * Returns a DbEntity associated with this ObjEntity.
     */
    public DbEntity getDbEntity() {

        // since 1.2 - allow overriding DbEntity in the inheritance hierarchy...
        if (dbEntityName != null) {
            return getNonNullNamespace().getDbEntity(dbEntityName);
        }

        ObjEntity superEntity = getSuperEntity();
        if (superEntity != null) {
            return superEntity.getDbEntity();
        }

        return null;
    }

    /**
     * Sets the DbEntity used by this ObjEntity.
     * <p>
     * <i>Setting DbEntity on an inherited entity has no effect, since a class of the
     * super entity is always used as a superclass. </i>
     * </p>
     */
    public void setDbEntity(DbEntity dbEntity) {
        this.dbEntityName = (dbEntity != null) ? dbEntity.getName() : null;
    }

    /**
     * Returns a named attribute that either belongs to this ObjEntity or is inherited.
     * Returns null if no matching attribute is found.
     */
    public Attribute getAttribute(String name) {
        Attribute attribute = super.getAttribute(name);
        if (attribute != null) {
            return attribute;
        }

        if (superEntityName == null) {
            return null;
        }

        ObjEntity superEntity = getSuperEntity();
        return (superEntity != null) ? superEntity.getAttribute(name) : null;
    }

    /**
     * Returns a SortedMap of all attributes that either belong to this ObjEntity or
     * inherited.
     */
    public SortedMap getAttributeMap() {
        if (superEntityName == null) {
            return super.getAttributeMap();
        }

        SortedMap attributeMap = new TreeMap();
        appendAttributes(attributeMap);
        return attributeMap;
    }

    /**
     * Recursively appends all attributes in the entity inheritance hierarchy.
     */
    final void appendAttributes(Map map) {
        map.putAll(super.getAttributeMap());

        ObjEntity superEntity = getSuperEntity();
        if (superEntity != null) {
            superEntity.appendAttributes(map);
        }
    }

    /**
     * Returns a Collection of all attributes that either belong to this ObjEntity or
     * inherited.
     */
    public Collection getAttributes() {
        if (superEntityName == null) {
            return super.getAttributes();
        }

        return getAttributeMap().values();
    }

    /**
     * Returns a Collection of all attributes that belong to this ObjEntity, excluding
     * inherited attributes.
     * 
     * @since 1.1
     */
    public Collection getDeclaredAttributes() {
        return super.getAttributes();
    }

    /**
     * Returns a named Relationship that either belongs to this ObjEntity or is inherited.
     * Returns null if no matching attribute is found.
     */
    public Relationship getRelationship(String name) {
        Relationship relationship = super.getRelationship(name);
        if (relationship != null) {
            return relationship;
        }

        if (superEntityName == null) {
            return null;
        }

        ObjEntity superEntity = getSuperEntity();
        return (superEntity != null) ? superEntity.getRelationship(name) : null;
    }

    public SortedMap getRelationshipMap() {
        if (superEntityName == null) {
            return super.getRelationshipMap();
        }

        SortedMap relationshipMap = new TreeMap();
        appendRelationships(relationshipMap);
        return relationshipMap;
    }

    /**
     * Recursively appends all relationships in the entity inheritance hierarchy.
     */
    final void appendRelationships(Map map) {
        map.putAll(super.getRelationshipMap());

        ObjEntity superEntity = getSuperEntity();
        if (superEntity != null) {
            superEntity.appendRelationships(map);
        }
    }

    public Collection getRelationships() {
        if (superEntityName == null) {
            return super.getRelationships();
        }

        return getRelationshipMap().values();
    }

    /**
     * Returns a Collection of all relationships that belong to this ObjEntity, excluding
     * inherited attributes.
     * 
     * @since 1.1
     */
    public Collection getDeclaredRelationships() {
        return super.getRelationships();
    }

    /**
     * Returns ObjAttribute of this entity that maps to <code>dbAttribute</code>
     * parameter. Returns null if no such attribute is found.
     */
    public ObjAttribute getAttributeForDbAttribute(DbAttribute dbAttribute) {
        Iterator it = getAttributeMap().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            ObjAttribute objAttr = (ObjAttribute) entry.getValue();
            if (objAttr.getDbAttribute() == dbAttribute)
                return objAttr;
        }
        return null;
    }

    /**
     * Returns ObjRelationship of this entity that maps to <code>dbRelationship</code>
     * parameter. Returns null if no such relationship is found.
     */
    public ObjRelationship getRelationshipForDbRelationship(DbRelationship dbRelationship) {
        Iterator it = getRelationshipMap().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            ObjRelationship objRel = (ObjRelationship) entry.getValue();

            List relList = objRel.getDbRelationships();
            if (relList.size() != 1) {
                continue;
            }

            if (relList.get(0) == dbRelationship) {
                return objRel;
            }
        }
        return null;
    }

    /**
     * Clears all the mapping between this obj entity and its current db entity. Clears
     * mapping between entities, attributes and relationships.
     */
    public void clearDbMapping() {
        if (dbEntityName == null)
            return;

        Iterator it = getAttributeMap().values().iterator();
        while (it.hasNext()) {
            ObjAttribute objAttr = (ObjAttribute) it.next();
            DbAttribute dbAttr = objAttr.getDbAttribute();
            if (null != dbAttr) {
                objAttr.setDbAttribute(null);
            }
        }

        Iterator rels = this.getRelationships().iterator();
        while (rels.hasNext()) {
            ((ObjRelationship) rels.next()).clearDbRelationships();
        }

        dbEntityName = null;
    }

    /**
     * Returns <code>true</code> if this ObjEntity represents a set of read-only
     * objects.
     * 
     * @return boolean
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Returns true if this entity directly or indirectly inherits from a given entity,
     * false otherwise.
     * 
     * @since 1.1
     */
    public boolean isSubentityOf(ObjEntity entity) {
        if (entity == null) {
            return false;
        }

        if (entity == this) {
            return false;
        }

        ObjEntity superEntity = getSuperEntity();
        if (superEntity == entity) {
            return true;
        }

        return (superEntity != null) ? superEntity.isSubentityOf(entity) : false;
    }

    public Iterator resolvePathComponents(Expression pathExp) throws ExpressionException {

        // resolve DB_PATH if we can
        if (pathExp.getType() == Expression.DB_PATH) {
            if (getDbEntity() == null) {
                throw new ExpressionException("Can't resolve DB_PATH '"
                        + pathExp
                        + "', DbEntity is not set.");
            }

            return getDbEntity().resolvePathComponents(pathExp);
        }

        if (pathExp.getType() == Expression.OBJ_PATH) {
            return new PathIterator((String) pathExp.getOperand(0));
        }

        throw new ExpressionException("Invalid expression type: '"
                + pathExp.expName()
                + "',  OBJ_PATH is expected.");
    }

    /**
     * Transforms an Expression to an analogous expression in terms of the underlying
     * DbEntity.
     * 
     * @since 1.1
     */
    public Expression translateToDbPath(Expression expression) {

        if (expression == null) {
            return null;
        }

        if (getDbEntity() == null) {
            throw new CayenneRuntimeException(
                    "Can't translate expression to DB_PATH, no DbEntity for '"
                            + getName()
                            + "'.");
        }

        // converts all OBJ_PATH expressions to DB_PATH expressions
        // and pass control to the DB entity
        return expression.transform(new DBPathConverter());
    }

    /**
     * Transforms an Expression rooted in this entity to an analogous expression rooted in
     * related entity.
     * 
     * @since 1.1
     */
    public Expression translateToRelatedEntity(
            Expression expression,
            String relationshipPath) {

        if (expression == null) {
            return null;
        }

        if (relationshipPath == null) {
            return expression;
        }

        if (getDbEntity() == null) {
            throw new CayenneRuntimeException(
                    "Can't transform expression, no DbEntity for '" + getName() + "'.");
        }

        // converts all OBJ_PATH expressions to DB_PATH expressions
        // and pass control to the DB entity

        DBPathConverter transformer = new DBPathConverter();

        String dbPath = transformer.toDbPath(resolvePathComponents(relationshipPath));
        Expression dbClone = expression.transform(transformer);

        return getDbEntity().translateToRelatedEntity(dbClone, dbPath);
    }

    final class DBPathConverter implements Transformer {

        // TODO: make it a public method - resolveDBPathComponents or something...
        // seems generally useful
        String toDbPath(Iterator objectPathComponents) {
            StringBuffer buf = new StringBuffer();
            while (objectPathComponents.hasNext()) {
                Object component = objectPathComponents.next();

                Iterator dbSubpath;

                if (component instanceof ObjRelationship) {
                    dbSubpath = ((ObjRelationship) component)
                            .getDbRelationships()
                            .iterator();
                }
                else if (component instanceof ObjAttribute) {
                    dbSubpath = ((ObjAttribute) component).getDbPathIterator();
                }
                else {
                    throw new CayenneRuntimeException("Unknown path component: "
                            + component);
                }

                while (dbSubpath.hasNext()) {
                    CayenneMapEntry subComponent = (CayenneMapEntry) dbSubpath.next();
                    if (buf.length() > 0) {
                        buf.append(Entity.PATH_SEPARATOR);
                    }

                    buf.append(subComponent.getName());
                }
            }

            return buf.toString();
        }

        public Object transform(Object input) {

            if (!(input instanceof Expression)) {
                return input;
            }

            Expression expression = (Expression) input;

            if (expression.getType() != Expression.OBJ_PATH) {
                return input;
            }

            // convert obj_path to db_path

            String converted = toDbPath(resolvePathComponents(expression));
            Expression exp = ExpressionFactory.expressionOfType(Expression.DB_PATH);
            exp.setOperand(0, converted);
            return exp;
        }
    }

    /**
     * Returns the name of the underlying DbEntity.
     * 
     * @since 1.1
     */
    public String getDbEntityName() {
        return dbEntityName;
    }

    /**
     * Sets the name of underlying DbEntity.
     * 
     * @since 1.1
     */
    public void setDbEntityName(String string) {
        dbEntityName = string;
    }

    /**
     * ObjEntity property changed. May be name, attribute or relationship added or
     * removed, etc. Attribute and relationship property changes are handled in respective
     * listeners.
     * 
     * @since 1.2
     */
    public void objEntityChanged(EntityEvent e) {
        if ((e == null) || (e.getEntity() != this)) {
            // not our concern
            return;
        }

        // handle entity name changes
        if (e.getId() == EntityEvent.CHANGE && e.isNameChange()) {
            String oldName = e.getOldName();
            String newName = e.getNewName();

            DataMap map = getDataMap();
            if (map != null) {
                ObjEntity oe = (ObjEntity) e.getEntity();
                Iterator rit = oe.getRelationships().iterator();
                while (rit.hasNext()) {
                    ObjRelationship or = (ObjRelationship) rit.next();
                    or = or.getReverseRelationship();
                    if (null != or && or.targetEntityName.equals(oldName)) {
                        or.targetEntityName = newName;
                    }
                }
            }
        }
    }

    /** New entity has been created/added. */
    public void objEntityAdded(EntityEvent e) {
        // does nothing currently
    }

    /** Entity has been removed. */
    public void objEntityRemoved(EntityEvent e) {
        // does nothing currently
    }

    /** Attribute property changed. */
    public void objAttributeChanged(AttributeEvent e) {
        // does nothing currently
    }

    /** New attribute has been created/added. */
    public void objAttributeAdded(AttributeEvent e) {
        // does nothing currently
    }

    /** Attribute has been removed. */
    public void objAttributeRemoved(AttributeEvent e) {
        // does nothing currently
    }

    /** Relationship property changed. */
    public void objRelationshipChanged(RelationshipEvent e) {
        // does nothing currently
    }

    /** Relationship has been created/added. */
    public void objRelationshipAdded(RelationshipEvent e) {
        // does nothing currently
    }

    /** Relationship has been removed. */
    public void objRelationshipRemoved(RelationshipEvent e) {
        // does nothing currently
    }

    /**
     * Returns true if the default lifecycle listeners should not be notified of this
     * entity lifecycle events.
     * 
     * @since 3.0
     */
    public boolean isExcludingDefaultListeners() {
        return excludingDefaultListeners;
    }

    public void setExcludingDefaultListeners(boolean excludingDefaultListeners) {
        this.excludingDefaultListeners = excludingDefaultListeners;
    }

    /**
     * Returns true if the lifeycle listeners defined on the superclasses should not be
     * notified of this entity lifecycle events.
     * 
     * @since 3.0
     */
    public boolean isExcludingSuperclassListeners() {
        return excludingSuperclassListeners;
    }

    public void setExcludingSuperclassListeners(boolean excludingSuperclassListeners) {
        this.excludingSuperclassListeners = excludingSuperclassListeners;
    }

}
