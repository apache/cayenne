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

import javax.swing.AbstractListModel;

import org.apache.cayenne.DataObject;

/**
 * A Swing ListModel wrapping {@link DataObjectList}.
 * 
 * @since 1.1
 */
public class DOListModel extends AbstractListModel implements DataObjectChangeListener,
        FieldValueChangeListener {

    protected ObjEntityViewField viewField;
    protected DataObjectList dataObjects = new DataObjectList(1);

    public DOListModel() {
    }

    public void setViewField(ObjEntityViewField field) {
        if (this.viewField != null) {
            this.viewField.getOwner().getOwner().removeFieldValueChangeListener(this);
        }
        this.viewField = field;
        viewField.getOwner().getOwner().addFieldValueChangeListener(this);
        fireContentsChanged(this, 0, getSize());
    }

    public void setDataObjects(DataObjectList dataObjects) {
        this.dataObjects.removeDataObjectChangeListener(this);
        this.dataObjects = dataObjects;
        this.dataObjects.addDataObjectChangeListener(this);
        fireContentsChanged(this, 0, getSize());
    }

    public int getSize() {
        return dataObjects.size();
    }

    public DataObject getDataObject(int index) {
        return (DataObject) dataObjects.get(index);
    }

    public Object getElementAt(int index) {
        if (viewField == null)
            return getDataObject(index);
        return viewField.getValue(getDataObject(index));
    }

    public void dataChanged(DataObjectChangeEvent event) {
        if (event.isMultiObjectChange()) {
            fireContentsChanged(this, 0, getSize());
            return;
        }
        int affectedRow = event.getAffectedDataObjectIndex();
        switch (event.getId()) {
            case DataObjectChangeEvent.DATAOBJECT_ADDED:
                fireIntervalAdded(this, affectedRow, affectedRow);
                break;
            case DataObjectChangeEvent.DATAOBJECT_REMOVED:
                fireIntervalRemoved(this, affectedRow, affectedRow);
                break;
            case DataObjectChangeEvent.DATAOBJECT_CHANGED:
                fireContentsChanged(this, affectedRow, affectedRow);
                break;
            default:
                fireContentsChanged(this, 0, getSize());
        }
    }

    public ObjEntityViewField getViewField() {
        return viewField;
    }

    public DataObjectList getDataObjects() {
        return dataObjects;
    }

    public void fieldValueChanged(FieldValueChangeEvent event) {
        if (viewField != null && viewField.isSameObjAttribute(event.getField())) {
            int index = dataObjects.indexOf(event.getModifiedObject());
            if (index >= 0)
                fireContentsChanged(this, index, index);
        }
    }
}
