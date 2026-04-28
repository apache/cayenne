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
package org.apache.cayenne.modeler.ui.project.editor.embeddable.attributes;

import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.modeler.event.model.EmbeddableAttributeEvent;
import org.apache.cayenne.modeler.toolkit.table.CMTableModel;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.util.Util;

import java.util.ArrayList;
import java.util.Collection;

public class EmbeddableAttributeTableModel extends CMTableModel {

    private final Embeddable embeddable;

    // Columns
    static final int OBJ_ATTRIBUTE = 0;
    static final int OBJ_ATTRIBUTE_TYPE = 1;
    static final int DB_ATTRIBUTE = 2;

    public EmbeddableAttributeTableModel(Embeddable embeddable, ProjectController mediator, Object eventSource) {
        super(mediator, eventSource, new ArrayList<>(embeddable.getAttributes()));
        this.embeddable = embeddable;
    }

    public EmbeddableAttribute getEmbeddableAttribute(int row) {
        return (row >= 0 && row < objectList.size()) ? (EmbeddableAttribute) objectList
                .get(row) : null;
    }

    @Override
    public Class<?> getElementsClass() {
        return EmbeddableAttribute.class;
    }

    @Override
    public void setUpdatedValueAt(Object value, int row, int col) {
        EmbeddableAttribute attribute = getEmbeddableAttribute(row);
        String renamedFrom = null;
        String path = null;
        Collection<String> nameAttr = null;

        if (col == OBJ_ATTRIBUTE) {
            String oldName = attribute.getName();
            String newName = value != null ? value.toString().trim() : null;
            renamedFrom = oldName;
            if (!Util.nullSafeEquals(oldName, newName)) {
                attribute.setName(newName);
                if (embeddable != null) {
                    embeddable.removeAttribute(oldName);
                    embeddable.addAttribute(attribute);
                }
            }

            fireTableCellUpdated(row, col);
        }
        else if (col == OBJ_ATTRIBUTE_TYPE) {
            attribute.setType(value != null ? value.toString() : null);
            fireTableCellUpdated(row, col);
        }
        else if (col == DB_ATTRIBUTE) {
            attribute.setDbAttributeName(value != null ? value.toString() : null);
            fireTableCellUpdated(row, col);
        }

        controller.fireEmbeddableAttributeEvent(
                EmbeddableAttributeEvent.ofChange(eventSource, attribute, embeddable, renamedFrom));
    }

    public int getColumnCount() {
        return 3;
    }

    public String getColumnName(int column) {
        switch (column) {
            case OBJ_ATTRIBUTE:
                return "ObjAttribute";
            case OBJ_ATTRIBUTE_TYPE:
                return "Java Type";
            case DB_ATTRIBUTE:
                return "DbAttribute";
            default:
                return "";
        }
    }

    public Object getValueAt(int row, int column) {
        EmbeddableAttribute attribute = getEmbeddableAttribute(row);

        if (column == OBJ_ATTRIBUTE) {
            return attribute.getName();
        }
        else if (column == OBJ_ATTRIBUTE_TYPE) {
            return attribute.getType();
        }
        else if (column == DB_ATTRIBUTE) {
            return attribute.getDbAttributeName();
        }
        else {
            return null;
        }
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return true;
    }

    @Override
    public boolean isColumnSortable(int sortCol) {
        return true;
    }

    @Override
    public void sortByColumn(int sortCol, boolean isAscent) {
        switch (sortCol) {
            case OBJ_ATTRIBUTE:
                sortByElementProperty("name", isAscent);
                break;
            case OBJ_ATTRIBUTE_TYPE:
                sortByElementProperty("type", isAscent);
                break;
            case DB_ATTRIBUTE:
                sortByElementProperty("dbAttributeName", isAscent);
                break;
        }
    }
}
