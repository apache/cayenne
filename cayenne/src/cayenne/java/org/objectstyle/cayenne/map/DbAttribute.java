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

import java.util.Iterator;

import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.event.AttributeEvent;
import org.objectstyle.cayenne.map.event.DbAttributeListener;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.util.XMLEncoder;

/** 
 * A DbAttribute defines a descriptor for a single database table column.
 * 
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class DbAttribute extends Attribute {
    /** 
     * Defines JDBC type of the column. 
     */
    protected int type = TypesMapping.NOT_DEFINED;

    /**
     * Defines whether the attribute allows nulls.
     */
    protected boolean mandatory;

    /** 
     * Defines whether the attribute is a part of the table primary key.
     */
    protected boolean primaryKey;

    // The length of CHAR or VARCHAr or max num of digits for DECIMAL.
    protected int maxLength = -1;

    // The number of digits after period for DECIMAL.
    protected int precision = -1;

    public DbAttribute() {
        super();
    }

    public DbAttribute(String name) {
        super(name);
    }

    public DbAttribute(String name, int type, DbEntity entity) {
        this.setName(name);
        this.setType(type);
        this.setEntity(entity);
    }

    /**
     * Prints itself as XML to the provided XMLEncoder.
     * 
     * @since 1.1
     */
    public void encodeAsXML(XMLEncoder encoder) {

        encoder.print("<db-attribute name=\"");
        encoder.print(Util.encodeXmlAttribute(getName()));
        encoder.print('\"');

        String type = TypesMapping.getSqlNameByType(getType());
        if (type != null) {
            encoder.print(" type=\"" + type + '\"');
        }

        // If attribute is part of primary key
        if (isPrimaryKey()) {
            encoder.print(" isPrimaryKey=\"true\"");
        }

        if (isMandatory()) {
            encoder.print(" isMandatory=\"true\"");
        }

        if (getMaxLength() > 0) {
            encoder.print(" length=\"");
            encoder.print(getMaxLength());
            encoder.print('\"');
        }

        if (getPrecision() > 0) {
            encoder.print(" precision=\"");
            encoder.print(getPrecision());
            encoder.print('\"');
        }

        encoder.println("/>");
    }

    public String getAliasedName(String alias) {
        return (alias != null) ? alias + '.' + this.getName() : this.getName();
    }

    /** 
     * Returns the SQL type of the column.
     * 
     * @see java.sql.Types
     */
    public int getType() {
        return type;
    }

    /** 
     * Sets the SQL type for the column.
     * 
     * @see java.sql.Types
     */
    public void setType(int type) {
        this.type = type;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    /**
     * Returns <code>true</code> if the DB column represented by this
     * attribute is a foreign key, referencing another table.
     * 
     * @since 1.1
     */
    public boolean isForeignKey() {
        String name = getName();
        if (name == null) {
            // won't be able to match joins...
            return false;
        }

        Iterator relationships = getEntity().getRelationships().iterator();
        while (relationships.hasNext()) {
            DbRelationship relationship = (DbRelationship) relationships.next();
            Iterator joins = relationship.getJoins().iterator();

            while (joins.hasNext()) {
                DbJoin join = (DbJoin) joins.next();
                if (name.equals(join.getSourceName())) {
                    DbAttribute target = join.getTarget();
                    if (target != null && target.isPrimaryKey()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
        Entity e = this.getEntity();
        if (e instanceof DbAttributeListener) {
            ((DbAttributeListener) e).dbAttributeChanged(
                new AttributeEvent(this, this, e));
        }
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    /** 
     * Returns the length of database column described by this attribute. 
     */
    public int getMaxLength() {
        return maxLength;
    }

    /** 
     * Sets the length of character or binary type or max num of digits for DECIMAL.
     */
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    /** 
     * Returns the number of digits after period for DECIMAL.
     */
    public int getPrecision() {
        return precision;
    }

    /** Sets the number of digits after period for DECIMAL.*/
    public void setPrecision(int precision) {
        this.precision = precision;
    }

    /** Appends string representation of attribute to a provided buffer.
     *  This is a variation of "toString" method. It may be more
     *  efficient in some cases. For example, when printing all
     *  attributes of a single entity together. 
     * 
     * @deprecated since 1.1
     */
    public StringBuffer toStringBuffer(StringBuffer buf) {
        buf.append("   Column name: " + this.getName() + "\n");
        buf.append("   Column type: " + type + "\n");
        return buf;
    }
}
