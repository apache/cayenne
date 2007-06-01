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
package org.objectstyle.cayenne.dataview;

import javax.swing.table.AbstractTableModel;

import org.objectstyle.cayenne.DataObject;

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