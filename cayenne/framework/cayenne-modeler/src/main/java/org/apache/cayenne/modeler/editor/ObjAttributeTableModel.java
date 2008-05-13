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

package org.apache.cayenne.modeler.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneTableModel;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.util.Util;

/**
 * Model for the Object Entity attributes and for Obj to DB Attribute Mapping tables.
 * Allows adding/removing attributes, modifying the types and the names.
 * 
 * @author Michael Misha Shengaout.
 * @author Andrus Adamchik
 */
public class ObjAttributeTableModel extends CayenneTableModel {

    // Columns
    static final int OBJ_ATTRIBUTE = 0;
    static final int OBJ_ATTRIBUTE_TYPE = 1;
    static final int DB_ATTRIBUTE = 2;
    static final int DB_ATTRIBUTE_TYPE = 3;
    static final int LOCKING = 4;

    protected ObjEntity entity;
    protected DbEntity dbEntity;

    public ObjAttributeTableModel(ObjEntity entity, ProjectController mediator,
            Object eventSource) {
        super(mediator, eventSource, new ArrayList<Attribute>(entity.getAttributes()));
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
            case LOCKING:
                return Boolean.class;
            default:
                return String.class;
        }
    }

    /**
     * Returns ObjAttribute class.
     */
    @Override
    public Class<?> getElementsClass() {
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

    /** Refreshes DbEntity to current db entity within ObjEntity. */
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
            case OBJ_ATTRIBUTE:
                return "ObjAttribute";
            case OBJ_ATTRIBUTE_TYPE:
                return "Java Type";
            case DB_ATTRIBUTE:
                return "DbAttribute";
            case DB_ATTRIBUTE_TYPE:
                return "DB Type";
            case LOCKING:
                return "Used for Locking";
            default:
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
            ProjectUtil.setAttributeName(attribute, value != null ? value
                    .toString()
                    .trim() : null);
            fireTableCellUpdated(row, column);
        }
        else if (column == OBJ_ATTRIBUTE_TYPE) {
            attribute.setType(value != null ? value.toString() : null);
            fireTableCellUpdated(row, column);
        }
        else if (column == LOCKING) {
            attribute.setUsedForLocking((value instanceof Boolean)
                    && ((Boolean) value).booleanValue());
            fireTableCellUpdated(row, column);
        }
        else {
            if (column == DB_ATTRIBUTE) {
                // If db attrib exist, associate it with obj attribute
                if (value != null) {
                    DbAttribute dbAttribute = (DbAttribute) dbEntity.getAttribute(value
                            .toString());
                    String path = dbAttribute != null ? dbAttribute.getName() : null;
                    attribute.setDbAttributePath(path);
                }
                // If name is erased, remove db attribute from obj attribute.
                else if (attribute.getDbAttribute() != null) {
                    attribute.setDbAttributePath(null);
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
            Attribute a1 = (Attribute) o1;
            Attribute a2 = (Attribute) o2;

            int delta = getWeight(a1) - getWeight(a2);

            return (delta != 0) ? delta : Util.nullSafeCompare(true, a1.getName(), a2
                    .getName());
        }

        private int getWeight(Attribute a) {
            return a.getEntity() == entity ? 1 : -1;
        }
    }
}
