/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.util.XMLEncoder;

/**
 * DbEntity subclass that is based on another DbEntity
 * and allows to define complex database expressions 
 * like GROUP BY and aggregate functions.
 * 
 * @author Andrei Adamchik
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
     * @see org.objectstyle.cayenne.map.DbEntity#getFullyQualifiedName()
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
     * @see org.objectstyle.cayenne.map.Entity#removeAttribute(String)
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
