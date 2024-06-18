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

package org.apache.cayenne.map;

import java.util.Collections;
import java.util.Iterator;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;

/**
 * An ObjAttribute is a mapping descriptor of a Java class property.
 */
public class ObjAttribute extends Attribute<ObjEntity, ObjAttribute, ObjRelationship> implements ConfigurationNode {

    protected String type;
    protected boolean usedForLocking;
    /**
     * @since 4.2
     */
    protected boolean lazy;
    protected CayennePath dbAttributePath;

    public ObjAttribute() {
    }

    public ObjAttribute(String name) {
        super(name);
    }

    public ObjAttribute(String name, String type, ObjEntity entity) {
        setName(name);
        setType(type);
        setEntity(entity);
    }

    /**
     * Creates a clone of an ObjAttribute argument.
     * 
     * @since 3.0
     */
    public ObjAttribute(ObjAttribute attribute) {
        setName(attribute.getName());
        setType(attribute.getType());
        setEntity(attribute.getEntity());
        setDbAttributePathDirect(attribute.getDbAttributePath());
        setUsedForLocking(attribute.isUsedForLocking());
        setLazy(attribute.isLazy());
    }

    @Override
    public ObjEntity getEntity() {
        return (ObjEntity) super.getEntity();
    }

    /**
     * @since 3.1
     */
    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return visitor.visitObjAttribute(this);
    }

    /**
     * Returns Java class of an object property described by this attribute.
     * Wraps any thrown exceptions into CayenneRuntimeException.
     */
    public Class<?> getJavaClass() {
        if (this.getType() == null) {
            return null;
        }

        try {
            return Util.getJavaClass(getType());
        } catch (ClassNotFoundException e) {
            throw new CayenneRuntimeException("Failed to load class for name '" + this.getType() + "': "
                    + e.getMessage(), e);
        }
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor<?> delegate) {
        encoder.start("obj-attribute")
                .attribute("name", getName())
                .attribute("type", getType())
                .attribute("lock", isUsedForLocking())
                .attribute("lazy", isLazy())
                .attribute("db-attribute-path", getDbAttributePath() == null
                                ? null
                                : getDbAttributePath().value());

        delegate.visitObjAttribute(this);
        encoder.end();
    }

    /**
     * Returns fully qualified Java class name of the object property
     * represented by this attribute.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the data object property. Type is expected to be a fully
     * qualified Java class name.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @since 3.0
     */
    public boolean isPrimaryKey() {
        if (dbAttributePath == null) {
            return false;
        }

        DbAttribute dbAttribute = getDbAttribute();
        return dbAttribute != null && dbAttribute.isPrimaryKey();
    }

    /**
     * Returns whether this attribute should be used for locking.
     * 
     * @since 1.1
     */
    public boolean isUsedForLocking() {
        return usedForLocking;
    }

    /**
     * Sets whether this attribute should be used for locking.
     * 
     * @since 1.1
     */
    public void setUsedForLocking(boolean usedForLocking) {
        this.usedForLocking = usedForLocking;
    }

    /**
     * @return whether this attribute should be loaded lazily.
     * @since 4.2
     */
    public boolean isLazy() {
        return lazy;
    }

    /**
     * Sets whether this attribute should be loaded lazily.
     * @since 4.2
     */
    public void setLazy(boolean lazy) {
        this.lazy = lazy;
    }

    /**
     * Returns a DbAttribute mapped by this ObjAttribute.
     */
    public DbAttribute getDbAttribute() {
        Iterator<CayenneMapEntry> pathIterator = getDbPathIterator(getEntity());
        CayenneMapEntry o = null;
        while (pathIterator.hasNext()) {
            o = pathIterator.next();
        }
        if (o == null) {
            return getParentDbAttribute(getEntity());
        }
        return (DbAttribute) o;
    }

    private DbAttribute getParentDbAttribute(ObjEntity entity) {
        if (entity != null) {
            ObjEntity parent = entity.getSuperEntity();
            if (parent != null) {
                Iterator<CayenneMapEntry> pathIterator = getDbPathIterator(parent);
                CayenneMapEntry o = null;
                while (pathIterator.hasNext()) {
                    o = pathIterator.next();
                }
                if (o == null) {
                    return getParentDbAttribute(parent);
                }
                return (DbAttribute) o;
            }
        }

        return null;
    }

    /**
     * Returns <code>true</code> if attribute inherited from a super entity.
     * 
     * @since 3.0
     */
    public boolean isInherited() {
        ObjEntity owningEntity = getEntity();
        if (owningEntity == null) {
            return false;
        }

        ObjEntity superEntity = owningEntity.getSuperEntity();

        if (superEntity == null) {
            return false;
        }

        return superEntity.getAttribute(getName()) != null;
    }

    public Iterator<CayenneMapEntry> getDbPathIterator() {
        return getDbPathIterator(getEntity());
    }

    public Iterator<CayenneMapEntry> getDbPathIterator(ObjEntity entity) {
        if (dbAttributePath == null) {
            return Collections.emptyIterator();
        }

        if (entity == null) {
            return Collections.emptyIterator();
        }

        DbEntity dbEnt = entity.getDbEntity();
        if (dbEnt == null) {
            return Collections.emptyIterator();
        }

        if(dbAttributePath.isEmpty()) {
            return Collections.emptyIterator();
        }

        if(dbAttributePath.length() == 1) {
            DbAttribute attribute = dbEnt.getAttribute(dbAttributePath.last().value());
            if (attribute == null) {
                return Collections.emptyIterator();
            }
            return Collections.<CayenneMapEntry>singleton(attribute).iterator();
        }

        return dbEnt.resolvePathComponents(dbAttributePath);
    }

    /**
     * Returns the name of the mapped DbAttribute. This value is the same as
     * "dbAttributePath" for regular attributes mapped to columns.
     * It is equal to the last path component for the flattened attributes.
     */
    public String getDbAttributeName() {
        if (dbAttributePath == null || dbAttributePath.isEmpty()) {
            return null;
        }

        return dbAttributePath.last().value();
    }

    public void setDbAttributePath(String dbAttributePath) {
        setDbAttributePath(CayennePath.of(dbAttributePath));
    }

    public void setDbAttributePath(CayennePath dbAttributePath) {
        setDbAttributePathDirect(dbAttributePath);
        if (isInherited()) {
            getEntity().addAttributeOverride(getName(), dbAttributePath);
        }
    }

    void setDbAttributePathDirect(CayennePath path) {
        this.dbAttributePath = path;
    }

    /**
     * Returns a dot-separated path that starts in the root DbEntity that maps
     * to this attribute's ObjEntity and spans zero or more relationships,
     * always ending in a DbAttribute name.
     */
    public CayennePath getDbAttributePath() {
        return dbAttributePath;
    }

    /**
     * Returns whether this attribute is "flattened", meaning that it points to
     * a column from an entity other than the DbEntity mapped to the parent
     * ObjEntity.
     * 
     * @since 3.0
     */
    public boolean isFlattened() {
        return dbAttributePath != null && dbAttributePath.length() > 1;
    }

    /**
     * Returns whether this attribute is mandatory
     * 
     * @see DbAttribute#isMandatory()
     */
    public boolean isMandatory() {
        DbAttribute dbAttribute = getDbAttribute();
        return dbAttribute != null && dbAttribute.isMandatory();
    }

    /**
     * Returns this attribute's maximum allowed length
     * 
     * @see DbAttribute#getMaxLength()
     */
    public int getMaxLength() {
        DbAttribute dbAttribute = getDbAttribute();
        return dbAttribute == null ? -1 : dbAttribute.getMaxLength();
    }

    /**
     * Updates DbAttributePath for this ObjAttribute
     */
    public void updateDbAttributePath() {

        if (isFlattened()) {
            StringBuilder newDbAttributePath = new StringBuilder();

            Iterator<CayenneMapEntry> dbPathIterator = getDbPathIterator();

            while (dbPathIterator.hasNext()) {
                CayenneMapEntry next = dbPathIterator.next();

                newDbAttributePath.append(next.getName());
                if (next instanceof DbRelationship) {
                    newDbAttributePath.append('.');
                }
            }

            setDbAttributePath(newDbAttributePath.toString());
        }
    }

    @Override
    public String toString() {
        return "ObjAttr: " + type + " " + name + "; DbPath[" + dbAttributePath + "]";
    }
}
