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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.Entity;
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
import org.apache.cayenne.util.Util;

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

    protected ObjEntity entity;
    protected DbEntity dbEntity;
    private CellEditorForAttributeTable cellEditor;
    private CayenneTable table;

    private static List<ObjAttributeWrapper> wrapObjAttributes(Collection<ObjAttribute> attributes) {
        List<ObjAttributeWrapper>  wrappedAttributes = new ArrayList<ObjAttributeWrapper>();
        for(ObjAttribute attr : attributes) {
            wrappedAttributes.add(new ObjAttributeWrapper(attr));
        }
        return wrappedAttributes;
    }

    public ObjAttributeTableModel(ObjEntity entity, ProjectController mediator,
            Object eventSource) {
        super(mediator, eventSource, wrapObjAttributes(entity.getAttributes()));
        // take a copy
        this.entity = entity;
        this.dbEntity = entity.getDbEntity();

        // order using local comparator
        Collections.sort(objectList, new AttributeComparator());
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
        return 6;
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
                return "DbAttributePath";
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
            if (!attribute.isInherited()
                    && ((ObjEntity) attribute.getEntity()).isAbstract()) {
                return attribute.getDbAttributePath();
            }
            else {
                return null;
            }
        }
        else if (attribute.getDbAttributePath() != null
                && attribute.getDbAttributePath().contains(".")) {
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

    @Override
    public void setUpdatedValueAt(Object value, int row, int column) {
        ObjAttributeWrapper attribute = getAttribute(row);
        attribute.resetEdits();
        AttributeEvent event = new AttributeEvent(eventSource, attribute.getValue(), entity);
        String path = null;
        Collection<String> nameAttr = null;

        if (column == OBJ_ATTRIBUTE) {
            event.setOldName(attribute.getName());

            attribute.setName(value != null ? value.toString().trim() : null);

            if (attribute.isValid()) {
                attribute.commitEdits();
            }
            fireTableCellUpdated(row, column);
        }
        else if (column == OBJ_ATTRIBUTE_TYPE) {
            String oldType = attribute.getType();
            attribute.setType(value != null ? value.toString() : null);
            String newType = attribute.getType();
            String[] registeredTypes = ModelerUtil.getRegisteredTypeNames();
            Collection<String> registeredTypesList = Arrays.asList(registeredTypes);

            if (oldType != null
                    && newType != null
                    && !(registeredTypesList.contains(oldType) == registeredTypesList
                            .contains(newType))) {
                ObjAttribute attributeNew;

                ArrayList<Embeddable> embs = mediator
                        .getEmbeddableNamesInCurRentDataDomain();
                ArrayList<String> embNames = new ArrayList<String>();
                Iterator<Embeddable> it = embs.iterator();
                while (it.hasNext()) {
                    embNames.add(it.next().getClassName());
                }

                if (registeredTypesList.contains(newType) || !embNames.contains(newType)) {
                    attributeNew = new ObjAttribute();
                }
                else {
                    attributeNew = new EmbeddedAttribute();
                    attribute.setDbAttributePath(null);
                }

                attributeNew.setDbAttributePath(attribute.getDbAttributePath());
                attributeNew.setName(attribute.getName());
                attributeNew.setEntity(attribute.getEntity());
                attributeNew.setParent(attribute.getParent());
                attributeNew.setType(attribute.getType());
                attributeNew.setUsedForLocking(attribute.isUsedForLocking());
                Entity ent = attribute.getEntity();
                ent.removeAttribute(attribute.getName());
                ent.addAttribute(attributeNew);

                mediator.fireObjEntityEvent(new EntityEvent(this, ent, MapEvent.CHANGE));

                EntityDisplayEvent ev = new EntityDisplayEvent(
                        this,
                        mediator.getCurrentObjEntity(),
                        mediator.getCurrentDataMap(),
                        (DataChannelDescriptor) mediator.getProject().getRootNode());

                mediator.fireObjEntityDisplayEvent(ev);

                mediator.fireObjAttributeEvent(new AttributeEvent(
                        this,
                        attributeNew,
                        ent,
                        MapEvent.CHANGE));

                AttributeDisplayEvent eventAttr = new AttributeDisplayEvent(
                        this,
                        attributeNew,
                        mediator.getCurrentObjEntity(),
                        mediator.getCurrentDataMap(),
                        (DataChannelDescriptor) mediator.getProject().getRootNode());

                mediator.fireObjAttributeDisplayEvent(eventAttr);
            }

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
                    path = value.toString();

                    String[] pathSplit = path.split("\\.");

                    // If flattened attribute
                    if (pathSplit.length > 1) {

                        DbEntity currentEnt = dbEntity;
                        StringBuilder pathBuf = new StringBuilder();
                        boolean isTruePath = true;

                        if (dbEntity != null) {

                            nameAttr = ModelerUtil
                                    .getDbAttributeNames(mediator, dbEntity);

                            for (int j = 0; j < pathSplit.length; j++) {

                                if (j == pathSplit.length - 1 && isTruePath) {
                                    DbAttribute dbAttribute = (DbAttribute) currentEnt
                                            .getAttribute(pathSplit[j]);
                                    if (dbAttribute != null) {
                                        pathBuf.append(dbAttribute.getName());
                                    }
                                    else {
                                        isTruePath = false;
                                    }
                                }
                                else if (isTruePath) {
                                    DbRelationship dbRelationship = (DbRelationship) currentEnt
                                            .getRelationship(pathSplit[j]);
                                    if (dbRelationship != null) {
                                        currentEnt = (DbEntity) dbRelationship
                                                .getTargetEntity();
                                        pathBuf.append(dbRelationship.getName());
                                        pathBuf.append(".");
                                    }
                                    else {
                                        isTruePath = false;
                                    }
                                }
                            }
                        }
                        path = isTruePath ? pathBuf.toString() : null;

                    }
                    else {

                        if (dbEntity != null) {
                            DbAttribute dbAttribute = (DbAttribute) dbEntity
                                    .getAttribute(value.toString());
                            path = dbAttribute != null ? dbAttribute.getName() : null;
                        }
                    }
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

    public void setComboBoxes(Collection<String> nameAttr, int column) {
        int count = getRowCount();
        for (int i = 0; i < count; i++) {
            if (getAttribute(i).getDbAttributePath() != null
                    && getAttribute(i).getDbAttributePath().contains(".")) {
                Collection<String> attributeComboForRow = new ArrayList<String>();
                attributeComboForRow.addAll(nameAttr);
                attributeComboForRow.add(getAttribute(i).getDbAttributePath());
                JComboBox comboBoxForRow = Application.getWidgetFactory().createComboBox(
                        attributeComboForRow,
                        true);

                cellEditor.setEditorAt(new Integer(i), new DefaultCellEditor(
                        comboBoxForRow));

            }
        }
        table.getColumnModel().getColumn(column).setCellEditor(cellEditor);
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
                Collections.sort(objectList, new Comparator<ObjAttributeWrapper>() {

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
                        }
                        return (valToCompare1 == null) ? -1 : (valToCompare2 == null)
                                ? 1
                                : valToCompare1.compareTo(valToCompare2);
                    }

                });
                if (!isAscent) {
                    Collections.reverse(objectList);
                }
                break;

        }
    }

    @Override
    public boolean isColumnSortable(int sortCol) {
        return true;
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
