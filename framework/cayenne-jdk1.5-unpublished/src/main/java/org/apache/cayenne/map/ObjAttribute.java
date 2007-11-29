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
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.StringUtils;

/**
 * An ObjAttribute is a mapping descriptor of a Java class property.
 * 
 * @author Misha Shengaout
 * @author Andrus Adamchik
 */
public class ObjAttribute extends Attribute {

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
     * Returns Java class of an object property described by this attribute. Wraps any
     * thrown exceptions into CayenneRuntimeException.
     */
    public Class<?> getJavaClass() {
        if (this.getType() == null) {
            return null;
        }

        try {
            return Util.getJavaClass(getType());
        }
        catch (ClassNotFoundException e) {
            throw new CayenneRuntimeException("Failed to load class for name '"
                    + this.getType()
                    + "': "
                    + e.getMessage(), e);
        }
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<obj-attribute name=\"" + getName() + '\"');

        if (getType() != null) {
            encoder.print(" type=\"");
            encoder.print(getType());
            encoder.print('\"');
        }

        if (isUsedForLocking()) {
            encoder.print(" lock=\"true\"");
        }

        // If this obj attribute is mapped to db attribute
        if (getDbAttribute() != null) {
            encoder.print(" db-attribute-path=\"");
            encoder.print(Util.encodeXmlAttribute(getDbAttributePath()));
            encoder.print('\"');
        }

        encoder.println("/>");
    }

    /**
     * Returns fully qualified Java class name of the object property represented by this
     * attribute.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the data object property. Type is expected to be a fully qualified
     * Java class name.
     */
    public void setType(String type) {
        this.type = type;
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
        Iterator<Object> pathIterator = getDbPathIterator();
        Object o = null;
        while (pathIterator.hasNext()) {
            o = pathIterator.next();
        }
        return (DbAttribute) o;
    }

    public Iterator<Object> getDbPathIterator() {
        if (dbAttributePath == null) {
            return IteratorUtils.EMPTY_ITERATOR;
        }

        ObjEntity ent = (ObjEntity) getEntity();
        if (ent == null) {
            return IteratorUtils.EMPTY_ITERATOR;
        }

        DbEntity dbEnt = ent.getDbEntity();
        if (dbEnt == null) {
            return IteratorUtils.EMPTY_ITERATOR;
        }

        int lastPartStart = dbAttributePath.lastIndexOf('.');
        if (lastPartStart < 0) {
            Attribute attribute = dbEnt.getAttribute(dbAttributePath);
            if (attribute == null) {
                return IteratorUtils.EMPTY_ITERATOR;
            }
            return IteratorUtils.singletonIterator(attribute);
        }

        return dbEnt.resolvePathComponents(dbAttributePath);
    }

    /**
     * Set mapped DbAttribute.
     */
    public void setDbAttribute(DbAttribute dbAttribute) {
        if (dbAttribute == null) {
            this.setDbAttributePath(null);
        }
        else {
            this.setDbAttributePath(dbAttribute.getName());
        }
    }

    /**
     * Returns the dbAttributeName.
     * 
     * @return String
     */
    public String getDbAttributeName() {
        if (dbAttributePath == null)
            return null;
        int lastPartStart = dbAttributePath.lastIndexOf('.');
        String lastPart = StringUtils.substring(
                dbAttributePath,
                lastPartStart + 1,
                dbAttributePath.length());
        return lastPart;
    }

    /**
     * Sets the dbAttributeName.
     * 
     * @param dbAttributeName The dbAttributeName to set
     */
    public void setDbAttributeName(String dbAttributeName) {
        if (dbAttributePath == null || dbAttributeName == null) {
            dbAttributePath = dbAttributeName;
            return;
        }
        int lastPartStart = dbAttributePath.lastIndexOf('.');
        String newPath = (lastPartStart > 0
                ? StringUtils.chomp(dbAttributePath, ".")
                : "");
        newPath += (newPath.length() > 0 ? "." : "") + dbAttributeName;
        this.dbAttributePath = newPath;
    }

    public void setDbAttributePath(String dbAttributePath) {
        this.dbAttributePath = dbAttributePath;
    }

    public String getDbAttributePath() {
        return dbAttributePath;
    }

    public boolean isCompound() {
        return (dbAttributePath != null && dbAttributePath.indexOf('.') >= 0);
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
            attribute.setMandatory(dbAttribute.isMandatory());
            attribute.setMaxLength(dbAttribute.getMaxLength());
        }

        // TODO: will likely need "userForLocking" property as well.

        return attribute;
    }
}
