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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.ObjEntityListener;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * ObjEntity is a mapping descriptor for a DataObject Java class. It contains
 * the information about the Java class itself, as well as its mapping to the
 * DbEntity layer.
 */
public class ObjEntity extends Entity implements ObjEntityListener, ConfigurationNode {

    public static final int LOCK_TYPE_NONE = 0;
    public static final int LOCK_TYPE_OPTIMISTIC = 1;

    // do not import CayenneDataObject as it introduces unneeded client
    // dependency
    private static final String CAYENNE_DATA_OBJECT_CLASS = "org.apache.cayenne.CayenneDataObject";
    /**
     * A collection of default "generic" entity classes excluded from class
     * generation.
     * 
     * @since 1.2
     */
    protected static final Collection<String> DEFAULT_GENERIC_CLASSES = Collections.singletonList(CAYENNE_DATA_OBJECT_CLASS);

    protected String superClassName;
    protected String className;
    protected String dbEntityName;
    protected String superEntityName;
    protected Expression qualifier;
    protected boolean readOnly;
    protected int lockType;

    protected boolean _abstract;
    protected boolean serverOnly;
    protected String clientClassName;
    protected String clientSuperClassName;

    protected CallbackMap callbacks;

    protected Map<String, String> attributeOverrides;

    public ObjEntity() {
        this(null);
    }

    public ObjEntity(String name) {
        setName(name);
        this.lockType = LOCK_TYPE_NONE;
        this.callbacks = new CallbackMap();
        this.attributeOverrides = new TreeMap<>();
    }

    /**
     * @since 3.1
     */
    @Override
    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return visitor.visitObjEntity(this);
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
        encoder.start("obj-entity").attribute("name", getName());

        // additionally validate that super entity exists
        if (getSuperEntityName() != null && getSuperEntity() != null) {
            encoder.attribute("superEntityName", getSuperEntityName());
        }

        encoder.attribute("abstract", isAbstract())
                .attribute("serverOnly", isServerOnly())
                .attribute("className", getClassName())
                .attribute("clientClassName", getClientClassName())
                .attribute("readOnly", isReadOnly());

        if (getDeclaredLockType() == LOCK_TYPE_OPTIMISTIC) {
            encoder.attribute("lock-type", "optimistic");
        }

        if (getDbEntityName() != null && getDbEntity() != null) {
            // not writing DbEntity name if sub entity has same DbEntity
            // as super entity, see CAY-1477
            if (!(getSuperEntity() != null && getSuperEntity().getDbEntity() == getDbEntity())) {
                encoder.attribute("dbEntityName", getDbEntityName());
            }
        }

        if (getSuperEntityName() == null && getSuperClassName() != null) {
            encoder.attribute("superClassName", getSuperClassName());
        }

        if (getSuperEntityName() == null && getClientSuperClassName() != null) {
            encoder.attribute("clientSuperClassName", getClientSuperClassName());
        }

        if (qualifier != null) {
            encoder.start("qualifier").nested(qualifier, delegate).end();
        }

        // store attributes
        encoder.nested(getDeclaredAttributes(), delegate);

        for (Map.Entry<String, String> override : attributeOverrides.entrySet()) {
            encoder.start("attribute-override")
                    .attribute("name", override.getKey())
                    .attribute("db-attribute-path", override.getValue())
                    .end();
        }

        // write entity-level callbacks
        getCallbackMap().encodeCallbacksAsXML(encoder);

        delegate.visitObjEntity(this);
        encoder.end();
    }

    /**
     * Returns an ObjEntity stripped of any server-side information, such as
     * DbEntity mapping. "clientClassName" property of this entity is used to
     * initialize "className" property of returned entity.
     * 
     * @since 1.2
     */
    public ObjEntity getClientEntity() {

        ClientObjEntity entity = new ClientObjEntity(getName());
        entity.setClassName(getClientClassName());
        entity.setSuperClassName(getClientSuperClassName());
        entity.setSuperEntityName(getSuperEntityName());
        entity.setDeclaredQualifier(getDeclaredQualifier());

        // TODO: should we also copy lock type?

        Collection<ObjAttribute> primaryKeys = getMutablePrimaryKeys();
        Collection<ObjAttribute> clientPK = new ArrayList<>(primaryKeys.size());

        for (ObjAttribute attribute : getDeclaredAttributes()) {
            ObjAttribute clientAttribute = attribute.getClientAttribute();
            entity.addAttribute(clientAttribute);

            if (primaryKeys.remove(attribute)) {
                clientPK.add(clientAttribute);
            }
        }

        // after all meaningful pks got removed, here we only have synthetic pks
        // left...
        for (ObjAttribute attribute : primaryKeys) {
            ObjAttribute clientAttribute = attribute.getClientAttribute();
            clientPK.add(clientAttribute);
        }

        entity.setPrimaryKeys(clientPK);

        // copy relationships; skip runtime generated relationships
        for (ObjRelationship relationship : getDeclaredRelationships()) {
            if (relationship.isRuntime()) {
                continue;
            }

            ObjEntity targetEntity = relationship.getTargetEntity();
            // note that 'isClientAllowed' also checks parent DataMap client
            // policy
            // that can be handy in case of cross-map relationships
            if (targetEntity == null || !targetEntity.isClientAllowed()) {
                continue;
            }

            entity.addRelationship(relationship.getClientRelationship());
        }

        // TODO: andrus 2/5/2007 - copy embeddables
        // TODO: andrus 2/5/2007 - copy callback methods

        return entity;
    }

    /**
     * Returns a non-null class name. For generic entities with no class
     * specified explicitly, default DataMap superclass is used, and if it is
     * not set - CayenneDataObject is used.
     * 
     * @since 4.0
     */
   public  String getJavaClassName() {
        String name = getClassName();

        if (name == null && getDataMap() != null) {
            name = getDataMap().getDefaultSuperclass();
        }

        if (name == null) {
            name = CAYENNE_DATA_OBJECT_CLASS;
        }

        return name;
    }

    /**
     * Returns Java class of persistent objects described by this entity. For
     * generic entities with no class specified explicitly, default DataMap
     * superclass is used, and if it is not set - CayenneDataObject is used.
     * Casts any thrown exceptions into CayenneRuntimeException.
     * 
     * @since 1.2
     * @deprecated since 4.0 this method based on statically defined class
     *             loading algorithm is not going to work in environments like
     *             OSGi. {@link AdhocObjectFactory} should be used as it can
     *             provide the environment-specific class loading policy. 
     */
    @Deprecated
    public Class<?> getJavaClass() {
        String name = getJavaClassName();

        try {
            return Util.getJavaClass(name);
        } catch (ClassNotFoundException e) {
            throw new CayenneRuntimeException("Failed to doLoad class " + name + ": " + e.getMessage(), e);
        }
    }

    /**
     * Returns an object that stores callback methods of this entity.
     * 
     * @since 3.0
     */
    public CallbackMap getCallbackMap() {
        return callbacks;
    }

    /**
     * Returns the type of lock used by this ObjEntity. If this entity is not
     * locked, this method would look in a super entity recursively, until it
     * finds a lock somewhere in the inheritance hierarchy.
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
     * Returns the type of lock used by this ObjEntity, regardless of what
     * locking type is used by super entities.
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
     * Returns whether this entity is "generic", meaning it is not mapped to a
     * unique Java class. Criterion for generic entities is that it either has
     * no Java class mapped or its class is the same as DataMap's default
     * superclass, or it is CayenneDataObject.
     * 
     * @since 1.2
     */
    public boolean isGeneric() {
        String className = getClassName();
        return className == null || DEFAULT_GENERIC_CLASSES.contains(className)
                || (getDataMap() != null && className.equals(getDataMap().getDefaultSuperclass()));
    }

    /**
     * Returns true if this entity is allowed to be used on the client. Checks
     * that parent DataMap allows client entities and also that this entity is
     * not explicitly disabled for the client use.
     * 
     * @since 1.2
     */
    public boolean isClientAllowed() {
        return getDataMap() != null && !isServerOnly() && getDataMap().isClientSupported();
    }

    public boolean isAbstract() {
        return _abstract;
    }

    /**
     * Sets whether this entity is abstract only.
     */
    public void setAbstract(boolean isAbstract) {
        this._abstract = isAbstract;
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
     * Returns a qualifier that imposes a restriction on what objects belong to
     * this entity. Returned qualifier is the one declared in this entity, and
     * does not include qualifiers declared in super entities.
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
     * Sets a qualifier that imposes a limit on what objects belong to this
     * entity.
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
     * Returns a fully-qualified name of the super class of the DataObject
     * class. This value is used as a hint for class generation. If the entity
     * inherits from another entity, a superclass is the class of that entity.
     */
    public String getSuperClassName() {
        ObjEntity superEntity = getSuperEntity();
        return (superEntity != null) ? superEntity.getClassName() : superClassName;
    }

    /**
     * Sets a fully-qualified name of the super class of the DataObject class.
     * This value is used as a hint for class generation.
     * <p>
     * <i>An attempt to set superclass on an inherited entity has no effect,
     * since a class of the super entity is always used as a superclass.</i>
     * </p>
     */
    public void setSuperClassName(String superClassName) {
        this.superClassName = superClassName;
    }

    /**
     * Returns a fully-qualified name of the client-side super class of the
     * DataObject class. This value is used as a hint for class generation. If
     * the entity inherits from another entity, a superclass is the class of
     * that entity.
     * 
     * @since 1.2
     */
    public String getClientSuperClassName() {
        ObjEntity superEntity = getSuperEntity();
        return (superEntity != null) ? superEntity.getClientClassName() : clientSuperClassName;
    }

    /**
     * Sets a fully-qualified name of the client-side super class of the
     * ClientDataObject class. This value is used as a hint for class
     * generation.
     * <p>
     * <i>An attempt to set superclass on an inherited entity has no effect,
     * since a class of the super entity is always used as a superclass. </i>
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
        return (superEntityName != null) ? getNonNullNamespace().getObjEntity(superEntityName) : null;
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
     * <i>Setting DbEntity on an inherited entity has no effect, since a class
     * of the super entity is always used as a superclass. </i>
     * </p>
     */
    public void setDbEntity(DbEntity dbEntity) {
        this.dbEntityName = (dbEntity != null) ? dbEntity.getName() : null;
    }

    /**
     * Returns an unmodifiable collection of ObjAttributes representing the
     * primary key of the table described by this DbEntity. Note that since PK
     * is very often not an object property, the returned collection may contain
     * "synthetic" ObjAttributes that are created on the fly and are not a part
     * of ObjEntity and will not be a part of entity.getAttributes().
     * 
     * @since 3.0
     */
    public Collection<ObjAttribute> getPrimaryKeys() {
        return Collections.unmodifiableCollection(getMutablePrimaryKeys());
    }

    private Collection<ObjAttribute> getMutablePrimaryKeys() {
        if (getDbEntity() == null) {
            throw new CayenneRuntimeException("No DbEntity for ObjEntity: %s", getName());
        }

        Collection<DbAttribute> pkAttributes = getDbEntity().getPrimaryKeys();
        Collection<ObjAttribute> attributes = new ArrayList<>(pkAttributes.size());

        for (DbAttribute pk : pkAttributes) {
            ObjAttribute attribute = getAttributeForDbAttribute(pk);

            // create synthetic attribute
            if (attribute == null) {
                attribute = new SyntheticPKObjAttribute(Util.underscoredToJava(pk.getName(), false));
                attribute.setDbAttributePath(pk.getName());
                attribute.setType(TypesMapping.getJavaBySqlType(pk.getType()));
            }

            attributes.add(attribute);
        }

        return attributes;
    }

    /**
     * Returns a named attribute that is either declared in this ObjEntity or is
     * inherited. In any case returned attribute 'getEntity' method will return
     * this entity. Returns null if no matching attribute is found.
     */
    @Override
    public ObjAttribute getAttribute(String name) {
        ObjAttribute attribute = (ObjAttribute) super.getAttribute(name);
        if (attribute != null) {
            return attribute;
        }

        // check embedded attribute
        int dot = name.indexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            ObjAttribute embedded = getAttribute(name.substring(0, dot));
            if (embedded instanceof EmbeddedAttribute) {
                return ((EmbeddedAttribute) embedded).getAttribute(name.substring(dot + 1));
            }
        }

        // check super attribute
        ObjEntity superEntity = getSuperEntity();
        if (superEntity != null) {

            ObjAttribute superAttribute = superEntity.getAttribute(name);
            if (superAttribute == null) {
                return null;
            }

            // decorate returned attribute to make it appear as if it belongs to
            // this
            // entity

            ObjAttribute decoratedAttribute = new ObjAttribute(superAttribute);
            decoratedAttribute.setEntity(this);

            String pathOverride = attributeOverrides.get(name);
            if (pathOverride != null) {
                decoratedAttribute.setDbAttributePath(pathOverride);
            }

            return decoratedAttribute;
        }

        return null;
    }

    /**
     * Returns a Map of all attributes that either belong to this
     * ObjEntity or inherited.
     */
    @Override
    public Map<String, ObjAttribute> getAttributeMap() {
        if (superEntityName == null) {
            return getAttributeMapInternal();
        }

        Map<String, ObjAttribute> attributeMap = new HashMap<>();
        appendAttributes(attributeMap);
        return attributeMap;
    }

    /**
     * Recursively appends all attributes in the entity inheritance hierarchy.
     */
    final void appendAttributes(Map<String, ObjAttribute> map) {
        map.putAll(getAttributeMapInternal());

        ObjEntity superEntity = getSuperEntity();
        if (superEntity != null) {
            Map<String, ObjAttribute> attributeMap = new HashMap<>();
            superEntity.appendAttributes(attributeMap);
            for (String attributeName : attributeMap.keySet()) {

                String overridedDbPath = attributeOverrides.get(attributeName);

                ObjAttribute attribute = new ObjAttribute(attributeMap.get(attributeName));
                attribute.setEntity(this);
                if (overridedDbPath != null) {
                    attribute.setDbAttributePath(overridedDbPath);
                }
                map.put(attributeName, attribute);
            }
        }
    }

    @SuppressWarnings("unchecked")
    final Map<String, ObjAttribute> getAttributeMapInternal() {
        return (Map<String, ObjAttribute>) super.getAttributeMap();
    }

    /**
     * @since 3.0
     */
    public void addAttributeOverride(String attributeName, String dbPath) {
        attributeOverrides.put(attributeName, dbPath);
    }

    /**
     * @since 4.0
     */
    public void removeAttributeOverride(String attributeName) {
        attributeOverrides.remove(attributeName);
    }

    /**
     * @since 3.0
     */
    public Map<String, String> getDeclaredAttributeOverrides() {
        return Collections.unmodifiableMap(attributeOverrides);
    }

    /**
     * Returns a Collection of all attributes that either belong to this
     * ObjEntity or inherited.
     */
    @Override
    public Collection<ObjAttribute> getAttributes() {
        return getAttributeMap().values();
    }

    /**
     * Returns a Collection of all attributes that belong to this ObjEntity,
     * excluding inherited attributes.
     * 
     * @since 1.1
     */
    @SuppressWarnings("unchecked")
    public Collection<ObjAttribute> getDeclaredAttributes() {
        return (Collection<ObjAttribute>) super.getAttributes();
    }

    /**
     * Finds attribute declared by this ObjEntity,
     * excluding inherited attributes.
     *
     * @param name of the attribute
     * @return declared attribute or null if no attribute is found
     *
     * @see ObjEntity#getAttribute(String)
     *
     * @since 4.0
     */
    public ObjAttribute getDeclaredAttribute(String name) {
        return (ObjAttribute) super.getAttribute(name);
    }

    /**
     * Returns a named Relationship that either belongs to this ObjEntity or is
     * inherited. Returns null if no matching attribute is found.
     */
    @Override
    public ObjRelationship getRelationship(String name) {
        ObjRelationship relationship = (ObjRelationship) super.getRelationship(name);
        if (relationship != null) {
            return relationship;
        }

        if (superEntityName == null) {
            return null;
        }

        ObjEntity superEntity = getSuperEntity();
        return (superEntity != null) ? superEntity.getRelationship(name) : null;
    }

    @Override
    public Map<String, ObjRelationship> getRelationshipMap() {
        if (superEntityName == null) {
            return getRelationshipMapInternal();
        }

        Map<String, ObjRelationship> relationshipMap = new HashMap<>();
        appendRelationships(relationshipMap);
        return relationshipMap;
    }

    /**
     * Recursively appends all relationships in the entity inheritance
     * hierarchy.
     */
    final void appendRelationships(Map<String, ObjRelationship> map) {
        map.putAll(getRelationshipMapInternal());

        ObjEntity superEntity = getSuperEntity();
        if (superEntity != null) {
            superEntity.appendRelationships(map);
        }
    }

    @Override
    public Collection<ObjRelationship> getRelationships() {
        return getRelationshipMap().values();
    }

    @SuppressWarnings("unchecked")
    final Map<String, ObjRelationship> getRelationshipMapInternal() {
        return (Map<String, ObjRelationship>) super.getRelationshipMap();
    }

    /**
     * Returns a Collection of all relationships that belong to this ObjEntity,
     * excluding inherited attributes.
     * 
     * @since 1.1
     */
    @SuppressWarnings("unchecked")
    public Collection<ObjRelationship> getDeclaredRelationships() {
        return (Collection<ObjRelationship>) super.getRelationships();
    }

    /**
     * Returns ObjAttribute of this entity that maps to <code>dbAttribute</code>
     * parameter. Returns null if no such attribute is found.
     */
    public ObjAttribute getAttributeForDbAttribute(DbAttribute dbAttribute) {

        for (ObjAttribute next : getAttributeMap().values()) {

            if (next instanceof EmbeddedAttribute) {
                ObjAttribute embeddedAttribute = ((EmbeddedAttribute) next)
                        .getAttributeForDbPath(dbAttribute.getName());
                if (embeddedAttribute != null) {
                    return embeddedAttribute;
                }
            } else {
                if (next.getDbAttribute() == dbAttribute) {
                    return next;
                }
            }
        }

        return null;
    }

    /**
     * Returns the names of DbAtributes that comprise the primary key of the
     * parent DbEntity.
     * 
     * @since 3.0
     */
    public Collection<String> getPrimaryKeyNames() {
        DbEntity dbEntity = getDbEntity();

        // abstract entities may have no DbEntity mapping
        if (dbEntity == null) {
            return Collections.emptyList();
        }

        Collection<DbAttribute> pkAttributes = dbEntity.getPrimaryKeys();
        Collection<String> names = new ArrayList<>(pkAttributes.size());

        for (DbAttribute pk : pkAttributes) {
            names.add(pk.getName());
        }

        return Collections.unmodifiableCollection(names);
    }

    /**
     * Returns ObjRelationship of this entity that maps to
     * <code>dbRelationship</code> parameter. Returns null if no such
     * relationship is found.
     */
    public ObjRelationship getRelationshipForDbRelationship(DbRelationship dbRelationship) {

        for (ObjRelationship objRel : getRelationshipMap().values()) {
            List<DbRelationship> relList = objRel.getDbRelationships();
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
     * Clears all the mapping between this obj entity and its current db entity.
     * Clears mapping between entities, attributes and relationships.
     */
    public void clearDbMapping() {
        if (dbEntityName == null) {
            return;
        }

        for (ObjAttribute attribute : getAttributeMap().values()) {
            DbAttribute dbAttr = attribute.getDbAttribute();
            if (dbAttr != null) {
                attribute.setDbAttributePath(null);
            }
        }

        for (ObjRelationship relationship : getRelationships()) {
            relationship.clearDbRelationships();
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
     * Returns true if this entity directly or indirectly inherits from a given
     * entity, false otherwise.
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

    /**
     * @since 3.0
     */
    @Override
    @SuppressWarnings("unchecked")
    public PathComponent<ObjAttribute, ObjRelationship> lastPathComponent(Expression path, Map aliasMap) {
        return super.lastPathComponent(path, aliasMap);
    }

    /**
     * Returns an Iterable instance over expression path components based on
     * this entity.
     * 
     * @since 3.0
     */
    @Override
    @SuppressWarnings("unchecked")
    public Iterable<PathComponent<ObjAttribute, ObjRelationship>> resolvePath(final Expression pathExp,
            final Map aliasMap) {

        if (pathExp.getType() == Expression.OBJ_PATH) {

            return new Iterable<PathComponent<ObjAttribute, ObjRelationship>>() {

                public Iterator iterator() {
                    return new PathComponentIterator(ObjEntity.this, (String) pathExp.getOperand(0), aliasMap);
                }
            };
        }

        throw new ExpressionException("Invalid expression type: '" + pathExp.expName() + "',  OBJ_PATH is expected.");
    }

    @Override
    public Iterator<CayenneMapEntry> resolvePathComponents(Expression pathExp) throws ExpressionException {

        // resolve DB_PATH if we can
        if (pathExp.getType() == Expression.DB_PATH) {
            if (getDbEntity() == null) {
                throw new ExpressionException("Can't resolve DB_PATH '" + pathExp + "', DbEntity is not set.");
            }

            return getDbEntity().resolvePathComponents(pathExp);
        }

        if (pathExp.getType() == Expression.OBJ_PATH) {
            return new PathIterator((String) pathExp.getOperand(0));
        }

        throw new ExpressionException("Invalid expression type: '" + pathExp.expName() + "',  OBJ_PATH is expected.");
    }

    /**
     * Transforms an Expression to an analogous expression in terms of the
     * underlying DbEntity.
     * 
     * @since 1.1
     */
    public Expression translateToDbPath(Expression expression) {

        if (expression == null) {
            return null;
        }

        if (getDbEntity() == null) {
            throw new CayenneRuntimeException("Can't translate expression to DB_PATH, no DbEntity for '%s'.", getName());
        }

        // converts all OBJ_PATH expressions to DB_PATH expressions
        // and pass control to the DB entity
        return expression.transform(new DBPathConverter());
    }

    /**
     * Transforms an Expression rooted in this entity to an analogous expression
     * rooted in related entity.
     * 
     * @since 1.1
     */
    @Override
    public Expression translateToRelatedEntity(Expression expression, String relationshipPath) {

        if (expression == null) {
            return null;
        }

        if (relationshipPath == null) {
            return expression;
        }

        if (getDbEntity() == null) {
            throw new CayenneRuntimeException("Can't transform expression, no DbEntity for '%s'.", getName());
        }

        // converts all OBJ_PATH expressions to DB_PATH expressions
        // and pass control to the DB entity

        DBPathConverter transformer = new DBPathConverter();

        String dbPath = transformer.toDbPath(createPathIterator(relationshipPath));
        Expression dbClone = expression.transform(transformer);

        return getDbEntity().translateToRelatedEntity(dbClone, dbPath);
    }

    private PathComponentIterator createPathIterator(String path) {
        return new PathComponentIterator(ObjEntity.this, path, new HashMap<String, String>());
        // TODO: do we need aliases here?
    }

    /**
     * @since 4.0
     */
    public Set<String> getCallbackMethods() {
        Set<String> res = new LinkedHashSet<>();
        for (CallbackDescriptor descriptor : getCallbackMap().getCallbacks()) {
            res.addAll(descriptor.getCallbackMethods());
        }
        return res;
    }

    final class DBPathConverter implements Function<Object, Object> {

        // TODO: make it a public method - resolveDBPathComponents or
        // something...
        // seems generally useful

        String toDbPath(PathComponentIterator objectPathComponents) {
            StringBuilder buf = new StringBuilder();
            while (objectPathComponents.hasNext()) {
                PathComponent<Attribute, Relationship> component = objectPathComponents.next();

                Iterator<?> dbSubpath;

                if (component.getAttribute() != null) {
                    dbSubpath = ((ObjAttribute) component.getAttribute()).getDbPathIterator();
                } else if (component.getRelationship() != null) {
                    dbSubpath = ((ObjRelationship) component.getRelationship()).getDbRelationships().iterator();
                } else {
                    throw new CayenneRuntimeException("Unknown path component: %s", component);
                }

                while (dbSubpath.hasNext()) {
                    CayenneMapEntry subComponent = (CayenneMapEntry) dbSubpath.next();
                    if (buf.length() > 0) {
                        buf.append(Entity.PATH_SEPARATOR);
                    }

                    buf.append(subComponent.getName());
                    
                    // use OUTER join for all components of the path is Obj path is OUTER
                    if (component.getJoinType() == JoinType.LEFT_OUTER) {
                        buf.append(OUTER_JOIN_INDICATOR);
                    }
                }
            }

            return buf.toString();
        }

        public Object apply(Object input) {

            if (!(input instanceof Expression)) {
                return input;
            }

            Expression expression = (Expression) input;

            if (expression.getType() != Expression.OBJ_PATH) {
                return input;
            }

            // convert obj_path to db_path

            String converted = toDbPath(createPathIterator((String) expression.getOperand(0)));
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
     * ObjEntity property changed. May be name, attribute or relationship added
     * or removed, etc. Attribute and relationship property changes are handled
     * in respective listeners.
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
                for (ObjRelationship relationship : oe.getRelationships()) {
                    relationship = relationship.getReverseRelationship();
                    if (null != relationship && relationship.targetEntityName.equals(oldName)) {
                        relationship.targetEntityName = newName;
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

}
