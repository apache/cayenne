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

package org.apache.cayenne.dataview;

import javax.swing.table.AbstractTableModel;

import org.apache.cayenne.DataObject;

/**
 * A Swing TableModel wrapping {@link DataObjectList}.
 * 
 * @since 1.1
 */
public class DOTableModel extends AbstractTableModel implements DataObjectChangeListener {

    private ObjEntityView view;
    private int[] columnMap;
    private String[] columnNames;
    private boolean[] editableColumns;
    private Class[] columnClasses;
    private int columnCount = 0;
    private DataObjectList dataObjects = new DataObjectList(1);

    public DOTableModel() {
        updateModel();
    }

    public ObjEntityView getView() {
        return view;
    }

    public void setView(ObjEntityView view) {
        this.view = view;
        updateModel();
    }

    public void updateModel() {
        if (view != null) {
            int fieldCount = view.getFieldCount();
            columnMap = new int[fieldCount];
            columnNames = new String[fieldCount];
            editableColumns = new boolean[fieldCount];
            columnClasses = new Class[fieldCount];
            columnCount = 0;
            for (int i = 0; i < fieldCount; i++) {
                ObjEntityViewField field = view.getField(i);
                if (!field.isVisible())
                    continue;
                columnMap[columnCount] = i;
                columnNames[columnCount] = field.getCaption();
                editableColumns[columnCount] = field.isEditable();
                columnClasses[columnCount] = field.getJavaClass();
                if (columnClasses[columnCount] == null)
                    columnClasses[columnCount] = String.class;
                columnCount++;
            }
        }
        else {
            columnMap = new int[0];
            columnNames = new String[0];
            editableColumns = new boolean[0];
            columnClasses = new Class[0];
            columnCount = 0;
            dataObjects = new DataObjectList(1);
        }
        fireTableStructureChanged();
    }

    public int getColumnCount() {
        return columnCount;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return editableColumns[columnIndex];
    }

    public String getColumnName(int column) {
        return columnNames[column];
    }

    public Class getColumnClass(int columnIndex) {
        return columnClasses[columnIndex];
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        ObjEntityViewField field = getField(columnIndex);
        DataObject obj = getDataObject(rowIndex);
        Object value = field.getValue(obj);
        return value;
    }

    public int getRowCount() {
        return dataObjects.size();
    }

    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        ObjEntityViewField field = getField(columnIndex);
        DataObject obj = getDataObject(rowIndex);
        field.setValue(obj, value);
    }

    public DataObject getDataObject(int rowIndex) {
        return dataObjects.getDataObject(rowIndex);
    }

    public ObjEntityViewField getField(int columnIndex) {
        return view.getField(columnMap[columnIndex]);
    }

    public DataObjectList getDataObjects() {
        return dataObjects;
    }

    public void setDataObjects(DataObjectList dataObjects) {
        this.dataObjects.removeDataObjectChangeListener(this);
        this.dataObjects = dataObjects;
        this.dataObjects.addDataObjectChangeListener(this);
        fireTableDataChanged();
    }

    public void dataChanged(DataObjectChangeEvent event) {
        if (event.isMultiObjectChange()) {
            fireTableDataChanged();
            return;
        }
        int affectedRow = event.getAffectedDataObjectIndex();
        switch (event.getId()) {
            case DataObjectChangeEvent.DATAOBJECT_ADDED:
                fireTableRowsInserted(affectedRow, affectedRow);
                break;
            case DataObjectChangeEvent.DATAOBJECT_REMOVED:
                fireTableRowsDeleted(affectedRow, affectedRow);
                break;
            case DataObjectChangeEvent.DATAOBJECT_CHANGED:
                fireTableRowsUpdated(affectedRow, affectedRow);
                break;
            default:
                fireTableDataChanged();
        }
    }
}
