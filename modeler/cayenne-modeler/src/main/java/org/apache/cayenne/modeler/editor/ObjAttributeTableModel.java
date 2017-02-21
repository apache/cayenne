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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.Attribute;
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
    public static final int INHERITED = 0;
    public static final int OBJ_ATTRIBUTE = 1;
    public static final int OBJ_ATTRIBUTE_TYPE = 2;
    public static final int DB_ATTRIBUTE = 3;
    public static final int DB_ATTRIBUTE_TYPE = 4;
    public static final int LOCKING = 5;
    public static final int COLUMN_COUNT = 6;

    private ObjEntity entity;
    private DbEntity dbEntity;
    private CellEditorForAttributeTable cellEditor;
    private CayenneTable table;

    public ObjAttributeTableModel(ObjEntity entity, ProjectController mediator, Object eventSource) {
        super(mediator, eventSource, wrapObjAttributes(entity.getAttributes()));
        // take a copy
        this.entity = entity;
        this.dbEntity = entity.getDbEntity();

        // order using local comparator
        Collections.sort(objectList, new AttributeComparator());
    }

    private static List<ObjAttributeWrapper> wrapObjAttributes(Collection<ObjAttribute> attributes) {
        List<ObjAttributeWrapper>  wrappedAttributes = new ArrayList<ObjAttributeWrapper>();
        for(ObjAttribute attr : attributes) {
            wrappedAttributes.add(new ObjAttributeWrapper(attr));
        }
        return wrappedAttributes;
    }
    
    protected void orderList() {
        // NOOP
    }

    public CayenneTable getTable() {
        return table;
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
        return COLUMN_COUNT;
    }

    public String getColumnName(int column) {
        switch (column) {
            case INHERITED:
                return "In";
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
            default:
                return "";
        }
    }

    public Object getValueAt(int row, int column) {
        ObjAttributeWrapper attribute = getAttribute(row);
        if (column == INHERITED) {
            return attribute.isInherited();
        }
        else if (column == OBJ_ATTRIBUTE) {
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
            if (column == DB_ATTRIBUTE) {
                return getDBAttribute(attribute, dbAttribute);
            }
            else if (column == DB_ATTRIBUTE_TYPE) {
                return getDBAttributeType(attribute, dbAttribute);
            }
            else {
                return null;
            }
        }
    }

    private String getDBAttribute(ObjAttributeWrapper attribute, DbAttribute dbAttribute) {
        if (dbAttribute == null) {
            if (!attribute.isInherited() && attribute.getEntity().isAbstract()) {
                return attribute.getDbAttributePath();
            }
            else {
                return null;
            }
        }
        else if (attribute.getDbAttributePath() != null && attribute.getDbAttributePath().contains(".")) {
            return attribute.getDbAttributePath();
        }
        return dbAttribute.getName();
    }

    private String getDBAttributeType(ObjAttributeWrapper attribute, DbAttribute dbAttribute) {
        int type;
        if (dbAttribute == null) {
            if (!(attribute.getValue() instanceof EmbeddedAttribute)) {
                try {
                    type = TypesMapping.getSqlTypeByJava(attribute.getJavaClass());
                    // have to catch the exception here to make sure that
                    // exceptional situations
                    // (class doesn't exist, for example) don't prevent the gui
                    // from properly updating.
                }
                catch (CayenneRuntimeException cre) {
                    return null;
                }
            }
            else {
                return null;
            }
        }
        else {
            type = dbAttribute.getType();
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
        for(ObjAttributeWrapper attribute : objectList) {
            attribute.resetEdits();
        }
    }    
    
    /**
     * @return false, if one or more attributes in model are not valid. 
     */
    @Override
    public boolean isValid() {
        for(ObjAttributeWrapper attribute : getObjectList()) {
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
        String oldType = attribute.getType();
        String newType = value != null ? value.toString() : null;

        attribute.setType(newType);
        if (oldType == null || newType == null) {
            return;
        }

        String[] registeredTypes = ModelerUtil.getRegisteredTypeNames();
        Collection<String> registeredTypesList = Arrays.asList(registeredTypes);
        if (registeredTypesList.contains(oldType) == registeredTypesList.contains(newType)) {
            return;
        }

        ObjEntity entity = attribute.getEntity();

        ObjAttribute attributeNew;
        if (registeredTypesList.contains(newType) ||
                !mediator.getEmbeddableNamesInCurrentDataDomain().contains(newType)) {
            attributeNew = new ObjAttribute();
            attributeNew.setDbAttributePath(attribute.getDbAttributePath());
        } else {
            attributeNew = new EmbeddedAttribute();
            attributeNew.setDbAttributePath(null);
        }

        attributeNew.setName(attribute.getName());
        attributeNew.setEntity(entity);
        attributeNew.setParent(attribute.getParent());
        attributeNew.setType(attribute.getType());
        attributeNew.setUsedForLocking(attribute.isUsedForLocking());

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

    private void setDbAttribute(ObjAttributeWrapper attribute, Object value) {

        // If db attribute exist, associate it with obj attribute
        if (value != null) {

            if (ProjectUtil.isDbAttributePathCorrect(dbEntity,value.toString())) {
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

        if (column == OBJ_ATTRIBUTE) {
            event.setOldName(attribute.getName());
            setObjAttribute(attribute, value);
            fireTableCellUpdated(row, column);
        } else if (column == OBJ_ATTRIBUTE_TYPE) {
            setObjAttributeType(attribute, value);
            fireTableCellUpdated(row, column);
        } else if (column == LOCKING) {
            setColumnLocking(attribute, value);
            fireTableCellUpdated(row, column);
        } else {
            if (column == DB_ATTRIBUTE) {
                setDbAttribute(attribute, value);
            }
            fireTableRowsUpdated(row, row);
        }
        mediator.fireObjAttributeEvent(event);
    }

    public boolean isCellEditable(int row, int col) {

        if (getAttribute(row).isInherited()) {
            return col == DB_ATTRIBUTE;
        }

        return col != DB_ATTRIBUTE_TYPE && col != INHERITED;
    }

    public ObjEntity getEntity() {
        return entity;
    }

    final class AttributeComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            Attribute a1 = ((ObjAttributeWrapper) o1).getValue();
            Attribute a2 = ((ObjAttributeWrapper) o2).getValue();

            int delta = getWeight(a1) - getWeight(a2);

            return (delta != 0) ? delta : Util.nullSafeCompare(true, a1.getName(), a2
                    .getName());
        }

        private int getWeight(Attribute a) {
            return a.getEntity() == entity ? 1 : -1;
        }
    }

    @Override
    public void sortByColumn(final int sortCol, boolean isAscent) {
        switch (sortCol) {
            case INHERITED:
                sortByElementProperty("inherited", isAscent);
                break;
            case OBJ_ATTRIBUTE:
                sortByElementProperty("name", isAscent);
                break;
            case OBJ_ATTRIBUTE_TYPE:
                sortByElementProperty("type", isAscent);
                break;
            case LOCKING:
                sortByElementProperty("usedForLocking", isAscent);
                break;
            case DB_ATTRIBUTE:
            case DB_ATTRIBUTE_TYPE:
                Collections.sort(objectList, new ObjAttributeTableComparator(sortCol));
                if (!isAscent) {
                    Collections.reverse(objectList);
                }
                break;
            default:
                return;
        }
    }

    private class ObjAttributeTableComparator implements Comparator<ObjAttributeWrapper>{

        private int sortCol;

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
                    valToCompare1 = getDBAttributeType(o1, o1
                            .getDbAttribute());
                    valToCompare2 = getDBAttributeType(o2, o2
                            .getDbAttribute());
                    break;
                default:
                    break;
            }
            return (valToCompare1 == null) ? -1 : (valToCompare2 == null)
                    ? 1
                    : valToCompare1.compareTo(valToCompare2);
        }

        private Integer compareObjAttributes(ObjAttributeWrapper o1, ObjAttributeWrapper o2) {
            if ((o1 == null && o2 == null) || o1 == o2) {
                return 0;
            }
            else if (o1 == null && o2 != null) {
                return -1;
            }
            else if (o1 != null && o2 == null) {
                return 1;
            }
            return null;
        }
    }


    @Override
    public boolean isColumnSortable(int sortCol) {
        return true;
    }

}
