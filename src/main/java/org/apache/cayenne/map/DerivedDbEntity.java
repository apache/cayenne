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
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;

/**
 * DbEntity subclass that is based on another DbEntity
 * and allows to define complex database expressions 
 * like GROUP BY and aggregate functions.
 * 
 * @author Andrus Adamchik
 * @deprecated since 3.0M2 (scheduled for removal in 3.0M3) this type of mapping is no longer supported.
 */
public class DerivedDbEntity extends DbEntity {
    protected String parentEntityName;

    /**
     * Constructor for DerivedDbEntity.
     */
    public DerivedDbEntity() {
        super();
    }

    /**
     * Constructor for DerivedDbEntity.
     * @param name
     */
    public DerivedDbEntity(String name) {
        super(name);
    }

    /**
     * Constructor for DerivedDbEntity. Creates
     * a derived entity with the attribute set of a parent entity.
     */
    public DerivedDbEntity(String name, DbEntity parentEntity) {
        super(name);

        this.setParentEntity(parentEntity);
        this.resetToParentView();
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<db-entity name=\"" + Util.encodeXmlAttribute(getName()));

        if (getSchema() != null && getSchema().trim().length() > 0) {
            encoder.print("\" schema=\"");
            encoder.print(Util.encodeXmlAttribute(getSchema().trim()));
        }

        if (getCatalog() != null && getCatalog().trim().length() > 0) {
            encoder.print("\" catalog=\"");
            encoder.print(Util.encodeXmlAttribute(getCatalog().trim()));
        }

        encoder.print("\" parentName=\"");
        encoder.print(Util.encodeXmlAttribute(getParentEntityName()));
        encoder.println("\">");

        encoder.indent(1);
        encoder.print(getAttributeMap());
        encoder.indent(-1);
        encoder.println("</db-entity>");
    }

    /**
     * Removes all attributes and relationships, 
     * and replaces them with the data of the parent entity.
     */
    public void resetToParentView() {
        this.clearAttributes();
        this.clearRelationships();

        // copy attributes
        Iterator it = getParentEntity().getAttributes().iterator();
        while (it.hasNext()) {
            this.addAttribute(new DerivedDbAttribute(this, (DbAttribute) it.next()));
        }

        // copy relationships
        // Iterator rit = new ArrayList(this.getParentEntity().getRelationships()).iterator();
        Iterator rit = this.getParentEntity().getRelationships().iterator();
        while (rit.hasNext()) {
            DbRelationship protoRel = (DbRelationship) rit.next();
            DbRelationship rel = new DbRelationship();
            rel.setName(protoRel.getName());
            rel.setSourceEntity(this);
            rel.setTargetEntity(protoRel.getTargetEntity());

            Iterator joins = protoRel.getJoins().iterator();
            while (joins.hasNext()) {
                DbJoin protoJoin = (DbJoin) joins.next();
                DbJoin join = new DbJoin(rel);
                join.setSourceName(protoJoin.getSourceName());
                join.setTargetName(protoJoin.getTargetName());
                rel.addJoin(join);
            }

            this.addRelationship(rel);
        }
    }

    /**
     * Returns the parentEntity.
     * 
     * @return DbEntity
     */
    public DbEntity getParentEntity() {
        if (parentEntityName == null) {
            return null;
        }

        return getNonNullNamespace().getDbEntity(parentEntityName);
    }

    /**
     * Sets the parent entity of this derived DbEntity.
     */
    public void setParentEntity(DbEntity parentEntity) {
        setParentEntityName(parentEntity != null ? parentEntity.getName() : null);
    }

    /** 
     * Returns attributes used in GROUP BY as an unmodifiable list.
     */
    public List getGroupByAttributes() {
        List list = new ArrayList();
        Iterator it = super.getAttributes().iterator();
        while (it.hasNext()) {
            DerivedDbAttribute attr = (DerivedDbAttribute) it.next();
            if (attr.isGroupBy()) {
                list.add(attr);
            }
        }
        return list;
    }

    /**
     * @see org.apache.cayenne.map.DbEntity#getFullyQualifiedName()
     */
    public String getFullyQualifiedName() {
        return (getParentEntity() != null)
            ? getParentEntity().getFullyQualifiedName()
            : null;
    }

    /** 
     * Returns schema of the parent entity.
     */
    public String getSchema() {
        return (getParentEntity() != null) ? getParentEntity().getSchema() : null;
    }

    /** Throws exception. */
    public void setSchema(String schema) {
        throw new CayenneRuntimeException("Can't change schema of a derived entity.");
    }

    /** 
     * Returns catalog of the parent entity.
     */
    public String getCatalog() {
        return (getParentEntity() != null) ? getParentEntity().getCatalog() : null;
    }

    /** Throws exception. */
    public void setCatalog(String catalog) {
        throw new CayenneRuntimeException("Can't change catalogue of a derived entity.");
    }

    /**
     * @see org.apache.cayenne.map.Entity#removeAttribute(String)
     */
    public void removeAttribute(String attrName) {
        super.removeAttribute(attrName);
    }

    /**
     * Returns the parentEntityName.
     * @return String
     */
    public String getParentEntityName() {
        return parentEntityName;
    }

    /**
     * Sets the parentEntityName.
     * @param parentEntityName The parentEntityName to set
     */
    public void setParentEntityName(String parentEntityName) {
        this.parentEntityName = parentEntityName;
    }
}
