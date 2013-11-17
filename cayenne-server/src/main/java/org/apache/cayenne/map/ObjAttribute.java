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

import java.util.Iterator;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.commons.collections.IteratorUtils;

/**
 * An ObjAttribute is a mapping descriptor of a Java class property.
 */
public class ObjAttribute extends Attribute implements ConfigurationNode {

    protected String type;
    protected boolean usedForLocking;
    protected String dbAttributePath;

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
        setDbAttributePath(attribute.getDbAttributePath());
        setUsedForLocking(attribute.isUsedForLocking());
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
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<obj-attribute name=\"" + getName() + '\"');

        if (getType() != null) {
            encoder.print(" type=\"");
            encoder.print(Util.encodeXmlAttribute(getType()));
            encoder.print('\"');
        }

        if (isUsedForLocking()) {
            encoder.print(" lock=\"true\"");
        }

        // If this obj attribute is mapped to db attribute
        if (getDbAttribute() != null
                || (((ObjEntity) getEntity()).isAbstract() && !Util.isEmptyString(getDbAttributePath()))) {
            encoder.print(" db-attribute-path=\"");
            encoder.print(Util.encodeXmlAttribute(getDbAttributePath()));
            encoder.print('\"');
        }

        encoder.println("/>");
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
     * Returns a DbAttribute mapped by this ObjAttribute.
     */
    public DbAttribute getDbAttribute() {
        Iterator<CayenneMapEntry> pathIterator = getDbPathIterator((ObjEntity) getEntity());
        CayenneMapEntry o = null;
        while (pathIterator.hasNext()) {
            o = pathIterator.next();
        }
        if (o == null) {
            return getParentDbAttribute((ObjEntity) getEntity());
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
        ObjEntity owningEntity = (ObjEntity) getEntity();
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
        return getDbPathIterator((ObjEntity) getEntity());
    }

    public Iterator<CayenneMapEntry> getDbPathIterator(ObjEntity entity) {
        if (dbAttributePath == null) {
            return IteratorUtils.EMPTY_ITERATOR;
        }

        if (entity == null) {
            return IteratorUtils.EMPTY_ITERATOR;
        }

        DbEntity dbEnt = entity.getDbEntity();
        if (dbEnt == null) {
            return IteratorUtils.EMPTY_ITERATOR;
        }

        int lastPartStart = dbAttributePath.lastIndexOf('.');
        if (lastPartStart < 0) {
            DbAttribute attribute = dbEnt.getAttribute(dbAttributePath);
            if (attribute == null) {
                return IteratorUtils.EMPTY_ITERATOR;
            }
            return IteratorUtils.singletonIterator(attribute);
        }

        return dbEnt.resolvePathComponents(dbAttributePath);
    }

    /**
     * Returns the the name of the mapped DbAttribute. This value is the same as
     * "dbAttributePath" for regular attributes mapped to columns. It is equql
     * to the last path component for the flattened attributes.
     */
    public String getDbAttributeName() {
        if (dbAttributePath == null) {
            return null;
        }

        int lastDot = dbAttributePath.lastIndexOf('.');
        if (lastDot < 0) {
            return dbAttributePath;
        }

        return dbAttributePath.substring(lastDot + 1);
    }

    public void setDbAttributePath(String dbAttributePath) {
        this.dbAttributePath = dbAttributePath;

        if (isInherited()) {
            ((ObjEntity) entity).addAttributeOverride(getName(), dbAttributePath);
        }
    }

    /**
     * Returns a dot-separated path that starts in the root DbEntity that maps
     * to this attribute's ObjEntity and spans zero or more relationships,
     * always ending in a DbAttribute name.
     */
    public String getDbAttributePath() {
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
        return dbAttributePath != null && dbAttributePath.indexOf('.') >= 0;
    }

    /**
     * Returns whether this attribute is mandatory
     * 
     * @see DbAttribute#isMandatory()
     */
    public boolean isMandatory() {
        DbAttribute dbAttribute = getDbAttribute();
        return dbAttribute == null ? false : dbAttribute.isMandatory();
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
     * Returns an ObjAttribute stripped of any server-side information, such as
     * DbAttribute mapping.
     * 
     * @since 1.2
     */
    public ObjAttribute getClientAttribute() {
        ClientObjAttribute attribute = new ClientObjAttribute(getName());
        attribute.setType(getType());

        DbAttribute dbAttribute = getDbAttribute();
        if (dbAttribute != null) {

            // expose PK attribute names - the client may need those to build
            // ObjectIds
            if (dbAttribute.isPrimaryKey()) {
                attribute.setDbAttributePath(dbAttribute.getName());
                attribute.setPrimaryKey(true);
            }

            attribute.setMandatory(isMandatory());
            attribute.setMaxLength(getMaxLength());
        }

        // TODO: will likely need "userForLocking" property as well.

        return attribute;
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
}
