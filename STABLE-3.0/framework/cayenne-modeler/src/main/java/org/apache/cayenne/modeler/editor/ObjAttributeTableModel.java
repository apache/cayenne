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

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.event.AttributeEvent;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.AttributeDisplayEvent;
import org.apache.cayenne.modeler.event.EntityDisplayEvent;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CayenneTableModel;
import org.apache.cayenne.modeler.util.CayenneWidgetFactory;
import org.apache.cayenne.modeler.util.CellEditorForAttributeTable;
import org.apache.cayenne.modeler.util.ModelerUtil;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.CayenneRuntimeException;

/**
 * Model for the Object Entity attributes and for Obj to DB Attribute Mapping tables.
 * Allows adding/removing attributes, modifying the types and the names.
 * 
 */
public class ObjAttributeTableModel extends CayenneTableModel {

    // Columns
    static final int INHERITED = 0;
    static final int OBJ_ATTRIBUTE = 1;
    static final int OBJ_ATTRIBUTE_TYPE = 2;
    static final int DB_ATTRIBUTE = 3;
    static final int DB_ATTRIBUTE_TYPE = 4;
    static final int LOCKING = 5;

    protected ObjEntity entity;
    protected DbEntity dbEntity;
    private CellEditorForAttributeTable cellEditor;
    private CayenneTable table;

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
        return 6;
    }

    public String getColumnName(int column) {
        switch (column) {
            case INHERITED:
                return "In";
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
            else if (column == DB_ATTRIBUTE_TYPE) {
                int type;
                if (dbAttribute == null) {
                    if (!(attribute instanceof EmbeddedAttribute)) {
                        try {
                            type = TypesMapping.getSqlTypeByJava(getAttribute(row)
                                    .getJavaClass());
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
            else {
                return null;
            }
        }
    }

    public CellEditorForAttributeTable setCellEditor(Collection<String> nameAttr, CayenneTable table) {
        this.cellEditor = new CellEditorForAttributeTable(table, CayenneWidgetFactory.createComboBox(
                nameAttr,
                true));
        this.table = table;
        return cellEditor;
    }

    public CellEditorForAttributeTable getCellEditor() {
        return cellEditor;
    }

    public void setUpdatedValueAt(Object value, int row, int column) {

        ObjAttribute attribute = getAttribute(row);
        AttributeEvent event = new AttributeEvent(eventSource, attribute, entity);
        String path = null;
        Collection<String> nameAttr = null;

        if (column == OBJ_ATTRIBUTE) {
            event.setOldName(attribute.getName());
            ProjectUtil.setAttributeName(attribute, value != null ? value
                    .toString()
                    .trim() : null);
            fireTableCellUpdated(row, column);
        }
        else if (column == OBJ_ATTRIBUTE_TYPE) {
            String oldType = attribute.getType();
            attribute.setType(value != null ? value.toString() : null);
            String newType = attribute.getType();
            String[] registeredTypes = ModelerUtil.getRegisteredTypeNames();
            Collection<String> registeredTypesList =  Arrays.asList(registeredTypes); ;
            if(oldType!=null && newType!=null && ! (registeredTypesList.contains(oldType) == registeredTypesList.contains(newType))){
                ObjAttribute attributeNew;
                
                ArrayList<Embeddable> embs = mediator.getEmbeddableNamesInCurRentDataDomain();
                ArrayList<String> embNames = new ArrayList<String>();
                Iterator<Embeddable> it = embs.iterator();
                while (it.hasNext()) {
                    embNames.add(it.next().getClassName());
                }
                
                if(registeredTypesList.contains(newType) || !embNames.contains(newType)){
                    attributeNew = new ObjAttribute();
                } else {
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
                
                mediator.fireObjEntityEvent(new EntityEvent(
                        this,
                        ent,
                        MapEvent.CHANGE));

                EntityDisplayEvent ev = new EntityDisplayEvent(this, mediator
                        .getCurrentObjEntity(), mediator.getCurrentDataMap(), mediator
                        .getCurrentDataDomain());

                mediator.fireObjEntityDisplayEvent(ev);

                mediator.fireObjAttributeEvent(new AttributeEvent(this, attributeNew, ent, MapEvent.CHANGE));

                AttributeDisplayEvent eventAttr = new AttributeDisplayEvent(
                        this,
                        attributeNew,
                        mediator.getCurrentObjEntity(),
                        mediator.getCurrentDataMap(),
                        mediator.getCurrentDataDomain());

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

                // If path is flattened attribute, update the JComboBox
                if (path != null && path.split("\\.").length > 1 && dbEntity!=null) {
                    setComboBoxes(nameAttr, column);
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
                attributeComboForRow
                        .add(getAttribute(i).getDbAttributePath());
                JComboBox comboBoxForRow = CayenneWidgetFactory
                        .createComboBox(attributeComboForRow, true);

                cellEditor.setEditorAt(new Integer(i), new DefaultCellEditor(
                        comboBoxForRow));
                
            }
        }
        table.getColumnModel().getColumn(column).setCellEditor(
                cellEditor);
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
