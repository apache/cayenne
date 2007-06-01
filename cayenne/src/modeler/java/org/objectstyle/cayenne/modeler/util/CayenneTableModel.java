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
package org.objectstyle.cayenne.modeler.util;

import java.util.Collections;

import javax.swing.table.AbstractTableModel;

import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.util.PropertyComparator;
import org.objectstyle.cayenne.util.Util;

/**
 * Superclass of CayenneModeler table model classes.
 * 
 * @author Andrei Adamchik
 */
public abstract class CayenneTableModel extends AbstractTableModel {
    protected ProjectController mediator;
    protected Object eventSource;
    protected java.util.List objectList;

    /**
     * Constructor for CayenneTableModel.
     */
    public CayenneTableModel(
        ProjectController mediator,
        Object eventSource,
        java.util.List objectList) {
        super();
        this.eventSource = eventSource;
        this.mediator = mediator;
        this.objectList = objectList;

        orderList();
    }

    public void setValueAt(Object newVal, int row, int col) {
        if (!Util.nullSafeEquals(newVal, getValueAt(row, col))) {
            setUpdatedValueAt(newVal, row, col);
        }
    }

    /**
     * Sets a new value after it is already checked by the superclass 
     * and it is determined that the value has changed.
     */
    public abstract void setUpdatedValueAt(Object newVal, int row, int col);

    /**
     * Orders internal object list. Key returned by 
     * <code>getOrderingKey</code> is used for comparison.
     */
    protected void orderList() {
        String key = getOrderingKey();
        if (key != null) {
            Collections.sort(
                objectList,
                new PropertyComparator(getOrderingKey(), getElementsClass()));
        }
    }

    /**
     * Returns Java class of the internal list elements.
     */
    public abstract Class getElementsClass();

    /** 
     * Returns the key by which to order elements
     * in the object list. Default value is "name".
     */
    public String getOrderingKey() {
        return "name";
    }

    /**
     * Returns the number of objects on the list.
     */
    public int getRowCount() {
        return objectList.size();
    }

    /**
     * Returns an object used as an event source.
     */
    public Object getEventSource() {
        return eventSource;
    }

    /** 
     * Returns EventController object. 
     */
    public ProjectController getMediator() {
        return mediator;
    }

    /**
     * Returns internal object list.
     */
    public java.util.List getObjectList() {
        return objectList;
    }

    public void addRow(Object row) {
        objectList.add(row);
        fireTableDataChanged();
    }

    public void removeRow(Object row) {
        objectList.remove(row);
        fireTableDataChanged();
    }

    /**
     * Moves a row up, jumping down if row is already at the top.
     */
    public int moveRowUp(Object row) {
        int len = objectList.size();
        if (len < 2) {
            return -1;
        }

        int ind = objectList.indexOf(row);
        if (ind <= 0) {
            return -1;
        }

        int neighborIndex = ind - 1;
        if (neighborIndex < 0) {
            neighborIndex = len - 1;
        }

        swapRows(ind, neighborIndex);
        return neighborIndex;
    }

    /**
      * Moves a row down, jumping up if row is already at the bottom.
      */
    public int moveRowDown(Object row) {
        int len = objectList.size();
        if (len < 2) {
            return -1;
        }

        int ind = objectList.indexOf(row);
        if (ind <= 0) {
            return -1;
        }

        int neighborIndex = ind + 1;
        if (neighborIndex >= len) {
            neighborIndex = 0;
        }

        swapRows(ind, neighborIndex);
        return neighborIndex;
    }

    protected void swapRows(int i, int j) {
        Collections.swap(objectList, i, j);
        fireTableDataChanged();
    }
}
