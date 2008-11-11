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

import java.util.List;

import org.apache.cayenne.map.CallbackDescriptor;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.CallbackMethodEvent;
import org.apache.cayenne.modeler.util.CayenneTableModel;


/**
 * Table model for displaying methods list for a particular CallbackDescriptor
 *
 * @version 1.0 Oct 23, 2007
 */
public class CallbackDescriptorTableModel extends CayenneTableModel {
    private static final int COLUMN_COUNT = 1;
    public static final int METHOD_NAME = 0;
    protected ObjEntity entity;
    protected CallbackDescriptor callbackDescriptor;

    /**
     * constructor
     *
     * @param mediator mediator instance
     * @param eventSource event source
     * @param objectList default objects list
     * @param callbackDescriptor callback descriptor instance
     */
    public CallbackDescriptorTableModel(
            ProjectController mediator,
            Object eventSource,
            List objectList,
            CallbackDescriptor callbackDescriptor) {
        super(mediator, eventSource, objectList);
        this.callbackDescriptor = callbackDescriptor;
    }

    /**
     * does nothing
     * @param newVal newVal
     * @param row row
     * @param col col
     */
    public void setUpdatedValueAt(Object newVal, int row, int col) {
        //do nothing
    }

    /**
     * Returns Java class of the internal list elements.
     */
    public Class getElementsClass() {
        return String.class;
    }

    /**
     * @param rowIndex method index
     * @return callback method for the specified index
     */
    public String getCallbackMethod(int rowIndex) {
        return (String) objectList.get(rowIndex);
    }

    /**
     * Returns the number of columns in the model. A
     * <code>JTable</code> uses this method to determine how many columns it
     * should create and display by default.
     *
     * @return the number of columns in the model
     * @see #getRowCount
     */
    public int getColumnCount() {
        return COLUMN_COUNT;
    }


    /**
     * Returns the value for the cell at <code>columnIndex</code> and
     * <code>rowIndex</code>.
     *
     * @param	rowIndex	the row whose value is to be queried
     * @param	columnIndex 	the column whose value is to be queried
     * @return	the value Object at the specified cell
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case METHOD_NAME:
                return getCallbackMethod(rowIndex);
        }

        return null;
    }

    /**
     * sorting disabled
     */
    protected void orderList() {
        // NOOP
    }

    /**
     * @param column column index
     * @return column name
     */
    public String getColumnName(int column) {
        switch (column) {
            case METHOD_NAME:
                return "Method";
        }

        return null;
    }

    /**
     * all cells are editable
     *
     * @param rowIndex row index
     * @param columnIndex column index
     * @return true
     */
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    /**
     * stores edited value
     *
     * @param newVal new value
     * @param row row
     * @param col column
     */
    public void setValueAt(Object newVal, int row, int col) {
        String method = (String) newVal;
        if (method != null) {
            method = method.trim();
        }
        String prevMethod = (String) getObjectList().get(row);

        if (method != null && method.length() > 0) {
            //check that method changed and name is not duplicate
            if (!method.equals(prevMethod) &&
                !getCallbackDescriptor().getCallbackMethods().contains(method)) {
                //update model
                getObjectList().set(row, method);

                //update entity
                getCallbackDescriptor().setCallbackMethodAt(row, method);

                fireTableRowsUpdated(row, row);

                mediator.fireCallbackMethodEvent(new CallbackMethodEvent(
                        eventSource,
                        prevMethod,
                        method,
                        MapEvent.CHANGE
                ));
            }
        }
    }

    /**
     * @return CallbackDescriptor of the model
     */
    public CallbackDescriptor getCallbackDescriptor() {
        return callbackDescriptor;
    }
}

