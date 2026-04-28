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

package org.apache.cayenne.modeler.toolkit.table;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Superclass of CayenneModeler table model classes.
 */
public abstract class CMTableModel<T> extends AbstractTableModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(CMTableModel.class);

    protected ProjectController controller;
    protected Object eventSource;
    protected List<T> objectList;

    protected CMTableModel(ProjectController controller, Object eventSource, List<T> objectList) {
        this.eventSource = eventSource;
        this.controller = controller;
        this.objectList = objectList;
    }

    @Override
    public void setValueAt(Object newVal, int row, int col) {
        try {

            Object oldValue = getValueAt(row, col);
            if (!Util.nullSafeEquals(newVal, oldValue)) {
                setUpdatedValueAt(newVal, row, col);

                this.controller.getApplication()
                        .getUndoManager()
                        .addEdit(new CMTableModelUndoableEdit(this, oldValue, newVal, row, col));
            }
        } catch (IllegalArgumentException e) {
            LOGGER.error("Error setting table model value", e);
            JOptionPane.showMessageDialog(
                    controller.getApplication().getFrameController().getView(),
                    e.getMessage(),
                    "Invalid value",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Sets a new value after it is already checked by the superclass and it is determined
     * that the value has changed.
     */
    public abstract void setUpdatedValueAt(Object newVal, int row, int col);

    /**
     * Returns Java class of the internal list elements.
     */
    public abstract Class<?> getElementsClass();

    /**
     * Returns the number of objects on the list.
     */
    @Override
    public int getRowCount() {
        return objectList.size();
    }

    /**
     * Returns EventController object.
     */
    public ProjectController getController() {
        return controller;
    }

    /**
     * Returns internal object list.
     */
    public List<T> getObjectList() {
        return objectList;
    }

    public void addRow(T row) {
        objectList.add(row);
        fireTableDataChanged();
    }

    public void removeRow(T row) {
        objectList.remove(row);
        fireTableDataChanged();
    }

    /**
     * Moves a row up, jumping down if row is already at the top.
     */
    public int moveRowUp(T row) {
        int len = objectList.size();
        if (len < 2) {
            return -1;
        }

        int ind = objectList.indexOf(row);
        if (ind <= 0) {
            return -1;
        }

        int neighborIndex = ind - 1;
        swapRows(ind, neighborIndex);
        return neighborIndex;
    }

    /**
     * Moves a row down, jumping up if row is already at the bottom.
     */
    public int moveRowDown(T row) {
        int len = objectList.size();
        if (len < 2) {
            return -1;
        }

        int ind = objectList.indexOf(row);
        // not valid if it is not found or it is at the end of the list
        if (ind < 0 || (ind + 1) >= len) {
            return -1;
        }

        int neighborIndex = ind + 1;

        swapRows(ind, neighborIndex);
        return neighborIndex;
    }

    protected void swapRows(int i, int j) {
        Collections.swap(objectList, i, j);
        fireTableDataChanged();
    }

    /**
     * Correct errors that model has.
     */
    public void resetModel() {
        // do nothing by default
    }

    /**
     * @return false, if model is not valid.
     */
    public boolean isValid() {
        return true;
    }

    protected static class PropertyComparator<C> implements Comparator<C> {

        Method getter;

        PropertyComparator(String propertyName, Class<?> beanClass) {
            try {
                getter = findGetter(beanClass, propertyName);
            } catch (IntrospectionException e) {
                throw new CayenneRuntimeException("Introspection error", e);
            }
        }

        Method findGetter(Class<?> beanClass, String propertyName) throws IntrospectionException {
            BeanInfo info = Introspector.getBeanInfo(beanClass);
            PropertyDescriptor[] descriptors = info.getPropertyDescriptors();

            for (PropertyDescriptor descriptor : descriptors) {
                if (propertyName.equals(descriptor.getName())) {
                    return descriptor.getReadMethod();
                }
            }

            throw new IntrospectionException("No getter found for " + propertyName);
        }

        @SuppressWarnings("unchecked")
        public int compare(C o1, C o2) {

            if (o1 == o2) {
                return 0;
            } else if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            }

            try {
                Comparable p1 = (Comparable) getter.invoke(o1);
                Comparable p2 = (Comparable) getter.invoke(o2);
                return (p1 == null) ? -1 : (p2 == null) ? 1 : p1.compareTo(p2);
            } catch (Exception ex) {
                throw new CayenneRuntimeException("Error reading property.", ex);
            }
        }
    }

    public abstract void sortByColumn(int sortCol, boolean isAscent);

    public abstract boolean isColumnSortable(int sortCol);

    public void sortByElementProperty(String string, boolean isAscent) {
        objectList.sort(new PropertyComparator<>(string, getElementsClass()));
        if (!isAscent) {
            Collections.reverse(objectList);
        }
    }

}
