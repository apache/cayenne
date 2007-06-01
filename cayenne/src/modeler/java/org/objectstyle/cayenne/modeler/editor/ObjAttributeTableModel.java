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
package org.objectstyle.cayenne.modeler.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.dba.TypesMapping;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.event.AttributeEvent;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.util.CayenneTableModel;
import org.objectstyle.cayenne.modeler.util.ProjectUtil;
import org.objectstyle.cayenne.util.Util;

/** 
 * Model for the Object Entity attributes and for Obj to 
 * DB Attribute Mapping tables. Allows adding/removing attributes,
 * modifying the types and the names.
 * 
 * @author Michael Misha Shengaout. 
 * @author Andrei Adamchik
 */
public class ObjAttributeTableModel extends CayenneTableModel {
    // Columns
    static final int OBJ_ATTRIBUTE = 0;
    static final int OBJ_ATTRIBUTE_TYPE = 1;
    static final int DB_ATTRIBUTE = 2;
    static final int DB_ATTRIBUTE_TYPE = 3;
    static final int LOCKING = 4;

    static final Logger logObj = Logger.getLogger(ObjAttributeTableModel.class);

    protected ObjEntity entity;
    protected DbEntity dbEntity;

    public ObjAttributeTableModel(
        ObjEntity entity,
        ProjectController mediator,
        Object eventSource) {
        super(mediator, eventSource, new ArrayList(entity.getAttributes()));
        // take a copy
        this.entity = entity;
        this.dbEntity = entity.getDbEntity();

        // order using local comparator
        Collections.sort(objectList, new AttributeComparator());
    }

    protected void orderList() {
        // NOOP
    }

    public Class getColumnClass(int col) {
        switch (col) {
            case LOCKING :
                return Boolean.class;
            default :
                return String.class;
        }
    }

    /**
     * Returns ObjAttribute class.
     */
    public Class getElementsClass() {
        return ObjAttribute.class;
    }

    public DbEntity getDbEntity() {
        return dbEntity;
    }

    public ObjAttribute getAttribute(int row) {
        return (row >= 0 && row < objectList.size())
            ? (ObjAttribute) objectList.get(row)
            : null;
    }

    /** Refreshes DbEntity to current db entity within ObjEntity.*/
    public void resetDbEntity() {
        if (dbEntity == entity.getDbEntity()) {
            return;
        }

        boolean wasShowing = isShowingDb();
        dbEntity = entity.getDbEntity();
        boolean isShowing = isShowingDb();

        if (wasShowing != isShowing) {
            fireTableStructureChanged();
        }

        fireTableDataChanged();
    }

    private boolean isShowingDb() {
        return dbEntity != null;
    }

    public int getColumnCount() {
        return 5;
    }

    public String getColumnName(int column) {
        switch (column) {
            case OBJ_ATTRIBUTE :
                return "ObjAttribute";
            case OBJ_ATTRIBUTE_TYPE :
                return "Java Type";
            case DB_ATTRIBUTE :
                return "DbAttribute";
            case DB_ATTRIBUTE_TYPE :
                return "DB Type";
            case LOCKING :
                return "Used for Locking";
            default :
                return "";
        }
    }

    public Object getValueAt(int row, int column) {
        ObjAttribute attribute = getAttribute(row);

        if (column == OBJ_ATTRIBUTE) {
            return attribute.getName();
        }
        else if (column == OBJ_ATTRIBUTE_TYPE) {
            return attribute.getType();
        }
        else if (column == LOCKING) {
            return attribute.isUsedForLocking() ? Boolean.TRUE : Boolean.FALSE;
        }
        else {
            DbAttribute dbAttribute = attribute.getDbAttribute();
            if (dbAttribute == null) {
                return null;
            }
            else if (column == DB_ATTRIBUTE)
                return dbAttribute.getName();
            else if (column == DB_ATTRIBUTE_TYPE) {
                return TypesMapping.getSqlNameByType(dbAttribute.getType());
            }
            else {
                return null;
            }
        }
    }

    public void setUpdatedValueAt(Object value, int row, int column) {

        ObjAttribute attribute = getAttribute(row);
        AttributeEvent event = new AttributeEvent(eventSource, attribute, entity);

        if (column == OBJ_ATTRIBUTE) {
            event.setOldName(attribute.getName());
            ProjectUtil.setAttributeName(
                attribute,
                value != null ? value.toString().trim() : null);
            fireTableCellUpdated(row, column);
        }
        else if (column == OBJ_ATTRIBUTE_TYPE) {
            attribute.setType(value != null ? value.toString() : null);
            fireTableCellUpdated(row, column);
        }
        else if (column == LOCKING) {
            attribute.setUsedForLocking(
                (value instanceof Boolean) && ((Boolean) value).booleanValue());
            fireTableCellUpdated(row, column);
        }
        else {
            DbAttribute dbAttribute = attribute.getDbAttribute();
            if (column == DB_ATTRIBUTE) {
                // If db attrib exist, associate it with obj attribute
                if (value != null) {
                    dbAttribute = (DbAttribute) dbEntity.getAttribute(value.toString());
                    attribute.setDbAttribute(dbAttribute);
                }
                // If name is erased, remove db attribute from obj attribute.
                else if (dbAttribute != null) {
                    attribute.setDbAttribute(null);
                }
            }

            fireTableRowsUpdated(row, row);
        }

        mediator.fireObjAttributeEvent(event);
    }

    private boolean isInherited(int row) {
        ObjAttribute attribute = getAttribute(row);
        return (attribute != null) ? attribute.getEntity() != entity : false;
    }

    public boolean isCellEditable(int row, int col) {
        if (isInherited(row)) {
            return false;
        }

        if (dbEntity == null) {
            return col != DB_ATTRIBUTE_TYPE && col != DB_ATTRIBUTE;
        }

        return col != DB_ATTRIBUTE_TYPE;
    }

    public ObjEntity getEntity() {
        return entity;
    }

    final class AttributeComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            ObjAttribute a1 = (ObjAttribute) o1;
            ObjAttribute a2 = (ObjAttribute) o2;

            int delta = getWeight(a1) - getWeight(a2);

            return (delta != 0)
                ? delta
                : Util.nullSafeCompare(true, a1.getName(), a2.getName());
        }

        private int getWeight(ObjAttribute a) {
            return a.getEntity() == entity ? 1 : -1;
        }
    }
}
