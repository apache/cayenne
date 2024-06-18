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

package org.apache.cayenne.modeler.editor;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.editor.wrapper.ObjAttributeWrapper;
import org.apache.cayenne.modeler.event.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CayenneTableModel;
import org.apache.cayenne.modeler.util.CellEditorForAttributeTable;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.project.extension.info.ObjectInfo;
import org.apache.cayenne.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Model for the Object Entity attributes and for Obj to DB Attribute Mapping tables.
 * Allows adding/removing attributes, modifying the types and the names.
 * 
 */
public class ObjAttributeTableModel extends CayenneTableModel<ObjAttributeWrapper> {

    // Columns
    public static final int OBJ_ATTRIBUTE = 0;
    public static final int OBJ_ATTRIBUTE_TYPE = 1;
    public static final int DB_ATTRIBUTE = 2;
    public static final int DB_ATTRIBUTE_TYPE = 3;
    public static final int LOCKING = 4;
    public static final int LAZY = 5;
    public static final int COMMENT = 6;
    public static final int COLUMN_COUNT = 7;

    private final ObjEntity entity;
    private DbEntity dbEntity;
    private CellEditorForAttributeTable cellEditor;
    private CayenneTable table;

    public ObjAttributeTableModel(ObjEntity entity, ProjectController mediator, Object eventSource) {
        super(mediator, eventSource, wrapObjAttributes(entity.getAttributes()));
        // take a copy
        this.entity = entity;
        this.dbEntity = entity.getDbEntity();

        // order using local comparator
        objectList.sort(new AttributeComparator());
    }

    private static List<ObjAttributeWrapper> wrapObjAttributes(Collection<ObjAttribute> attributes) {
        List<ObjAttributeWrapper> wrappedAttributes = new ArrayList<>();
        for (ObjAttribute attr : attributes) {
            wrappedAttributes.add(new ObjAttributeWrapper(attr));
        }
        return wrappedAttributes;
    }

    public CayenneTable getTable() {
        return table;
    }

    public Class<?> getColumnClass(int col) {
        switch (col) {
            case LOCKING:
            case LAZY:
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
        return ObjAttributeWrapper.class;
    }

    public DbEntity getDbEntity() {
        return dbEntity;
    }

    public ObjAttributeWrapper getAttribute(int row) {
        return (row >= 0 && row < objectList.size())
                ? objectList.get(row)
                : null;
    }

    /**
     * Refreshes DbEntity to current db entity within ObjEntity.
     */
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
        return COLUMN_COUNT;
    }

    public String getColumnName(int column) {
        switch (column) {
            case OBJ_ATTRIBUTE:
                return "Name";
            case OBJ_ATTRIBUTE_TYPE:
                return "Java Type";
            case DB_ATTRIBUTE:
                return "DbAttribute Path";
            case DB_ATTRIBUTE_TYPE:
                return "DB Type";
            case LOCKING:
                return "Used for Locking";
            case LAZY:
                return "Lazy loading";
            case COMMENT:
                return "Comment";
            default:
                return "";
        }
    }

    public Object getValueAt(int row, int column) {
        ObjAttributeWrapper attribute = getAttribute(row);
        DbAttribute dbAttribute = attribute.getDbAttribute();

        switch (column) {
            case OBJ_ATTRIBUTE:
                return attribute.getName();
            case OBJ_ATTRIBUTE_TYPE:
                return attribute.getType();
            case DB_ATTRIBUTE:
                return getDBAttribute(attribute, dbAttribute);
            case DB_ATTRIBUTE_TYPE:
                return getDBAttributeType(attribute, dbAttribute);
            case LOCKING:
                return attribute.isUsedForLocking();
            case LAZY:
                return attribute.isLazy();
            case COMMENT:
                return getComment(attribute.getValue());
            default:
                return null;
        }
    }

    private String getDBAttribute(ObjAttributeWrapper attribute, DbAttribute dbAttribute) {
        if (dbAttribute == null) {
            if (!attribute.isInherited() && attribute.getEntity().isAbstract()) {
                return attribute.getDbAttributePath();
            } else {
                return null;
            }
        } else if (attribute.getDbAttributePath() != null && attribute.getDbAttributePath().contains(".")) {
            return attribute.getDbAttributePath();
        }
        return dbAttribute.getName();
    }

    private String getDBAttributeType(ObjAttributeWrapper attribute, DbAttribute dbAttribute) {
        int type;
        if (dbAttribute != null) {
            type = dbAttribute.getType();
        } else {
            if (attribute.getValue() instanceof EmbeddedAttribute) {
                return null;
            } else {
                try {
                    Class<?> objAttributeClass;
                    try {
                        objAttributeClass = attribute.getObjAttributeClass();
                    } catch (DIRuntimeException e) {
                        return null;
                    }
                    type = TypesMapping.getSqlTypeByJava(objAttributeClass);
                    // have to catch the exception here to make sure that exceptional situations
                    // (class doesn't exist, for example) don't prevent the gui from properly updating.
                } catch (CayenneRuntimeException cre) {
                    return null;
                }
            }
        }
        return TypesMapping.getSqlNameByType(type);
    }

    public CellEditorForAttributeTable setCellEditor(
            Collection<String> nameAttr,
            CayenneTable table) {
        this.cellEditor = new CellEditorForAttributeTable(table, Application
                .getWidgetFactory()
                .createComboBox(nameAttr, true));
        this.table = table;
        return cellEditor;
    }

    public CellEditorForAttributeTable getCellEditor() {
        return cellEditor;
    }
    
    /**
     * Correct errors that attributes have.
     */
    @Override
    public void resetModel() {
        for (ObjAttributeWrapper attribute : objectList) {
            attribute.resetEdits();
        }
    }    
    
    /**
     * @return false, if one or more attributes in model are not valid. 
     */
    @Override
    public boolean isValid() {
        for (ObjAttributeWrapper attribute : getObjectList()) {
            if (!attribute.isValid()) {
                return false;
            }
        }

        return true;
    }

    private void setObjAttribute(ObjAttributeWrapper attribute, Object value) {
        attribute.setName(value != null ? value.toString().trim() : null);
        if (attribute.isValid()) {
            attribute.commitEdits();
        }
    }

    private void setObjAttributeType(ObjAttributeWrapper attribute, Object value) {
        String newType = value != null ? value.toString() : null;
        attribute.setType(newType);

        if (Arrays.asList(ModelerUtil.getRegisteredTypeNames()).contains(newType) || newType == null) {
            return;
        }

        ObjAttribute attributeNew;
        if (mediator.getEmbeddableNamesInCurrentDataDomain().contains(newType)) {
            attributeNew = new EmbeddedAttribute();
            attributeNew.setDbAttributePath((String)null);
        } else {
            attributeNew = new ObjAttribute();
            attributeNew.setDbAttributePath(attribute.getDbAttributePath());
        }

        ObjEntity entity = attribute.getEntity();
        attributeNew.setName(attribute.getName());
        attributeNew.setEntity(entity);
        attributeNew.setParent(attribute.getParent());
        attributeNew.setType(attribute.getType());
        attributeNew.setUsedForLocking(attribute.isUsedForLocking());
        attributeNew.setLazy(attribute.isLazy());

        entity.updateAttribute(attributeNew);

        mediator.fireObjEntityEvent(new EntityEvent(this, entity, MapEvent.CHANGE));

        mediator.fireObjEntityDisplayEvent(new EntityDisplayEvent(
                this,
                mediator.getCurrentObjEntity(),
                mediator.getCurrentDataMap(),
                (DataChannelDescriptor) mediator.getProject().getRootNode()));

        mediator.fireObjAttributeEvent(new AttributeEvent(
                this,
                attributeNew,
                entity,
                MapEvent.CHANGE));

        mediator.fireObjAttributeDisplayEvent(new AttributeDisplayEvent(
                this,
                attributeNew,
                mediator.getCurrentObjEntity(),
                mediator.getCurrentDataMap(),
                (DataChannelDescriptor) mediator.getProject().getRootNode()));
    }

    private void setColumnLocking(ObjAttributeWrapper attribute, Object value) {
        attribute.setUsedForLocking((value instanceof Boolean) && (Boolean) value);
    }

    private void setColumnLazy(ObjAttributeWrapper attribute, Object value) {
        attribute.setLazy((value instanceof Boolean) && (Boolean) value);
    }

    private void setDbAttribute(ObjAttributeWrapper attribute, Object value) {

        // If db attribute exist, associate it with obj attribute
        if (value != null) {

            if (ProjectUtil.isDbAttributePathCorrect(dbEntity, value.toString())) {
                attribute.setDbAttributePath(value.toString());
            } else {
                attribute.setDbAttributePath(null);
            }
        }
        // If name is erased, remove db attribute from obj attribute.
        else if (attribute.getDbAttribute() != null) {
            attribute.setDbAttributePath(null);
        }
    }

    @Override
    public void setUpdatedValueAt(Object value, int row, int column) {
        ObjAttributeWrapper attribute = getAttribute(row);
        attribute.resetEdits();
        AttributeEvent event = new AttributeEvent(eventSource, attribute.getValue(), entity);

        switch (column) {
            case OBJ_ATTRIBUTE:
                event.setOldName(attribute.getName());
                setObjAttribute(attribute, value);
                fireTableCellUpdated(row, column);
                break;
            case OBJ_ATTRIBUTE_TYPE:
                setObjAttributeType(attribute, value);
                fireTableCellUpdated(row, column);
                break;
            case DB_ATTRIBUTE:
                setDbAttribute(attribute, value);
                fireTableRowsUpdated(row, row);
                break;
            case LOCKING:
                setColumnLocking(attribute, value);
                fireTableCellUpdated(row, column);
                break;
            case LAZY:
                setColumnLazy(attribute, value);
                fireTableCellUpdated(row, column);
                break;
            case COMMENT:
                setComment((String)value, attribute.getValue());
            default:
                fireTableRowsUpdated(row, row);
                break;
        }
        mediator.fireObjAttributeEvent(event);
    }

    public boolean isCellEditable(int row, int col) {

        if (getAttribute(row).isInherited()) {
            return col == DB_ATTRIBUTE;
        }

        return col != DB_ATTRIBUTE_TYPE;
    }

    public ObjEntity getEntity() {
        return entity;
    }

    final class AttributeComparator implements Comparator<ObjAttributeWrapper> {

        public int compare(ObjAttributeWrapper o1, ObjAttributeWrapper o2) {
            ObjAttribute a1 = o1.getValue();
            ObjAttribute a2 = o2.getValue();

            int delta = getWeight(a1) - getWeight(a2);
            return (delta != 0)
                    ? delta
                    : Util.nullSafeCompare(true, a1.getName(), a2.getName());
        }

        private int getWeight(ObjAttribute a) {
            return a.getEntity() == entity ? 1 : -1;
        }
    }

    @Override
    public void sortByColumn(final int sortCol, boolean isAscent) {
        switch (sortCol) {
            case OBJ_ATTRIBUTE:
                sortByElementProperty("name", isAscent);
                break;
            case OBJ_ATTRIBUTE_TYPE:
                sortByElementProperty("type", isAscent);
                break;
            case LOCKING:
                sortByElementProperty("usedForLocking", isAscent);
                break;
            case LAZY:
                sortByElementProperty("lazy", isAscent);
                break;
            case DB_ATTRIBUTE:
            case DB_ATTRIBUTE_TYPE:
                objectList.sort(new ObjAttributeTableComparator(sortCol));
                if (!isAscent) {
                    Collections.reverse(objectList);
                }
                break;
        }
    }

    private class ObjAttributeTableComparator implements Comparator<ObjAttributeWrapper> {

        private final int sortCol;

        public ObjAttributeTableComparator(int sortCol) {
            this.sortCol = sortCol;
        }

        @Override
        public int compare(ObjAttributeWrapper o1, ObjAttributeWrapper o2) {
            Integer compareObjAttributesVal = compareObjAttributes(o1, o2);
            if (compareObjAttributesVal != null) {
                return compareObjAttributesVal;
            }
            String valToCompare1 = getDBAttribute(o1, o1.getDbAttribute());
            String valToCompare2 = getDBAttribute(o2, o2.getDbAttribute());
            switch (sortCol) {
                case DB_ATTRIBUTE:
                    valToCompare1 = getDBAttribute(o1, o1.getDbAttribute());
                    valToCompare2 = getDBAttribute(o2, o2.getDbAttribute());
                    break;
                case DB_ATTRIBUTE_TYPE:
                    valToCompare1 = getDBAttributeType(o1, o1.getDbAttribute());
                    valToCompare2 = getDBAttributeType(o2, o2.getDbAttribute());
                    break;
            }
            return (valToCompare1 == null)
                    ? -1
                    : (valToCompare2 == null)
                        ? 1
                        : valToCompare1.compareTo(valToCompare2);
        }

        private Integer compareObjAttributes(ObjAttributeWrapper o1, ObjAttributeWrapper o2) {
            if (o1 == o2) {
                return 0;
            } else if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            }
            return null;
        }
    }

    private String getComment(ObjAttribute attr) {
        return ObjectInfo.getFromMetaData(mediator.getApplication().getMetaData(), attr, ObjectInfo.COMMENT);
    }

    private void setComment(String newVal, ObjAttribute attr) {
        ObjectInfo.putToMetaData(mediator.getApplication().getMetaData(), attr, ObjectInfo.COMMENT, newVal);
    }

    @Override
    public boolean isColumnSortable(int sortCol) {
        return true;
    }

}
