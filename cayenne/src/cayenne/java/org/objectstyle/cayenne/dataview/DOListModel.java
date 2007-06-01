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

import javax.swing.AbstractListModel;

import org.objectstyle.cayenne.DataObject;

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