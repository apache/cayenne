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
package org.apache.cayenne.modeler.dialog.objentity;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EmbeddableAttribute;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.util.CayenneTable;
import org.apache.cayenne.modeler.util.CayenneTableModel;
import org.apache.cayenne.modeler.util.CellEditorForAttributeTable;

public class OverrideEmbeddableAttributeTableModel extends CayenneTableModel {

    private Map<String, String> overrideAttr;
    private ObjAttribute attr;
    private boolean isAttributeOverrideChange;

    private CellEditorForAttributeTable cellEditor;
    Collection<String> nameAttr;
    private CayenneTable table;

    protected List<EmbeddableAttribute> embeddableList;

    public List<EmbeddableAttribute> getEmbeddableList() {
        return embeddableList;
    }

    public OverrideEmbeddableAttributeTableModel(ProjectController mediator,
            Object eventSource, Collection<EmbeddableAttribute> embAttr, ObjAttribute attr) {

        super(mediator, eventSource, new ArrayList<Object>(embAttr));
        this.embeddableList = new ArrayList<EmbeddableAttribute>(embAttr);
        this.attr = attr;
        this.isAttributeOverrideChange = false;
        if (attr instanceof EmbeddedAttribute) {
            EmbeddedAttribute embeddedAttribute = (EmbeddedAttribute) attr;
            this.overrideAttr = new TreeMap<>(embeddedAttribute.getAttributeOverrides());
        }
        else {
            this.overrideAttr = null;
        }

        Iterator<EmbeddableAttribute> it = embeddableList.iterator();

        while (it.hasNext()) {
            EmbeddableAttribute emb = it.next();
            if (overrideAttr != null) {
                if (overrideAttr.get(emb.getName()) != null) {
                    emb.setDbAttributeName(overrideAttr.get(emb.getName()));
                }
            }
        }
    }

    public Map<String, String> getOverrideAttr() {
        return overrideAttr;
    }

    // Columns
    static final int OBJ_ATTRIBUTE = 0;
    static final int OBJ_ATTRIBUTE_TYPE = 1;
    static final int DB_ATTRIBUTE = 2;
    static final int DB_ATTRIBUTE_TYPE = 3;

    protected void orderList() {
        // NOOP
    }

    /**
     * Returns ObjAttribute class.
     */
    @Override
    public Class<?> getElementsClass() {
        return ObjAttribute.class;
    }

    @Override
    public void setUpdatedValueAt(Object value, int row, int col) {

        EmbeddableAttribute attribute = getEmbeddableAttribute(row);

        if (col == DB_ATTRIBUTE) {

            attribute.setDbAttributeName(value != null ? value.toString() : null);
            fireTableCellUpdated(row, col);
            this.isAttributeOverrideChange = true;
            ((ObjAttributeInfoDialogView) ((ObjAttributeInfoDialog) eventSource)
                    .getView()).getSaveButton().setEnabled(true);

            if (value != null) {
                DbEntity currentEnt = ((ObjEntity) attr.getEntity()).getDbEntity();
                if (currentEnt != null) {
                    DbAttribute dbAttr = (DbAttribute) currentEnt.getAttribute(value
                            .toString());
                    if (dbAttr != null) {
                        fireTableCellUpdated(DB_ATTRIBUTE_TYPE, col);
                    }
                }
            }
            fireTableRowsUpdated(row, row);
        }
    }

    public boolean isAttributeOverrideChange() {
        return isAttributeOverrideChange;
    }

    public CellEditorForAttributeTable setCellEditor(
            Collection<String> nameAttr,
            CayenneTable table) {
        this.table = table;
        this.cellEditor = new CellEditorForAttributeTable(table, Application
                .getWidgetFactory()
                .createComboBox(nameAttr, true));
        return cellEditor;
    }

    public CellEditorForAttributeTable getCellEditor() {
        return cellEditor;
    }

    public boolean isCellEditable(int row, int col) {
        return col == DB_ATTRIBUTE;
    }

    public EmbeddableAttribute getEmbeddableAttribute(int row) {
        return (row >= 0 && row < embeddableList.size())
                ? (EmbeddableAttribute) embeddableList.get(row)
                : null;
    }

    public int getColumnCount() {
        return 4;
    }

    public Object getValueAt(int row, int column) {
        EmbeddableAttribute attribute = getEmbeddableAttribute(row);

        if (column == OBJ_ATTRIBUTE) {
            return attribute.getName();
        }
        else if (column == OBJ_ATTRIBUTE_TYPE) {
            return attribute.getType();
        }
        else {
            String dbAttributeName = attribute.getDbAttributeName();
            if (column == DB_ATTRIBUTE) {
                return dbAttributeName;
            }
            else if (column == DB_ATTRIBUTE_TYPE) {

                return getDBAttrType(dbAttributeName);
            }
            else {
                return null;
            }
        }
    }

    private String getDBAttrType(String dbAttributeName) {
        DbEntity currentEnt = ((ObjEntity) attr.getEntity()).getDbEntity();
        if (currentEnt != null
                && currentEnt.getAttributes() != null
                && dbAttributeName != null) {
            DbAttribute dbAttr = (DbAttribute) currentEnt.getAttribute(dbAttributeName);
            if (dbAttr != null) {
                return TypesMapping.getSqlNameByType(dbAttr.getType());
            }
        }
        return null;
    }

    public String getColumnName(int column) {
        switch (column) {
            case OBJ_ATTRIBUTE:
                return "ObjAttribute";
            case OBJ_ATTRIBUTE_TYPE:
                return "Java Type";
            case DB_ATTRIBUTE:
                return "DbAttribute";
            case DB_ATTRIBUTE_TYPE:
                return "DB Type";
            default:
                return "";
        }
    }

    public void setComboBoxes(Collection<String> nameAttr, int column) {

        int count = getRowCount();
        for (int i = 0; i < count; i++) {
            EmbeddableAttribute embAt = getEmbeddableAttribute(i);
            if (!nameAttr.contains(embAt.getDbAttributeName())
                    && embAt.getDbAttributeName() != null) {
                Collection<String> attributeComboForRow = new ArrayList<String>();
                attributeComboForRow.addAll(nameAttr);
                attributeComboForRow.add(embAt.getDbAttributeName());
                JComboBox comboBoxForRow = Application.getWidgetFactory().createComboBox(
                        attributeComboForRow,
                        true);

                cellEditor.setEditorAt(new Integer(i), new DefaultCellEditor(
                        comboBoxForRow));
                BoxCellRenderer renderer = new BoxCellRenderer();
                renderer.setNotActiveColumn(attributeComboForRow.size() - 1);
                comboBoxForRow.setRenderer(renderer);

            }
        }

        table.getColumnModel().getColumn(column).setCellEditor(cellEditor);
    }

    public ObjAttribute getAttribute() {
        return attr;
    }

    @Override
    public boolean isColumnSortable(int sortCol) {
        return true;
    }

    @Override
    public void sortByColumn(final int sortCol, boolean isAscent) {
        Collections.sort(embeddableList, new Comparator<EmbeddableAttribute>() {

            public int compare(EmbeddableAttribute o1, EmbeddableAttribute o2) {
                Integer compareObjAttributesVal = compareObjAttributes(o1, o2);
                if (compareObjAttributesVal != null) {
                    return compareObjAttributesVal;
                }
                String valueToCompare1 = "";
                String valueToCompare2 = "";
                switch (sortCol) {
                    case OBJ_ATTRIBUTE:
                        valueToCompare1 = o1.getName();
                        valueToCompare2 = o2.getName();
                        break;
                    case OBJ_ATTRIBUTE_TYPE:
                        valueToCompare1 = o1.getType();
                        valueToCompare2 = o2.getType();
                        break;
                    case DB_ATTRIBUTE:
                        valueToCompare1 = o1.getDbAttributeName();
                        valueToCompare2 = o2.getDbAttributeName();
                        break;
                    case DB_ATTRIBUTE_TYPE:
                        valueToCompare1 = getDBAttrType(o1.getDbAttributeName());
                        valueToCompare2 = getDBAttrType(o2.getDbAttributeName());
                        break;
                }

                return (valueToCompare1 == null) ? -1 : (valueToCompare2 == null)
                        ? 1
                        : valueToCompare1.compareTo(valueToCompare2);
            }

        });

        if (!isAscent) {
            Collections.reverse(embeddableList);
        }

    }

    private Integer compareObjAttributes(EmbeddableAttribute o1, EmbeddableAttribute o2) {
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

class BoxCellRenderer implements ListCellRenderer {

    protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
    private int notActiveColumn;

    public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        JLabel renderer = (JLabel) defaultRenderer.getListCellRendererComponent(
                list,
                value,
                index,
                isSelected,
                cellHasFocus);

        if (getNotActiveColumn() != 0 && index == getNotActiveColumn()) {
            renderer.setForeground(Color.GRAY);
        }

        return renderer;
    }

    public int getNotActiveColumn() {
        return notActiveColumn;
    }

    public void setNotActiveColumn(int notActiveColumn) {
        this.notActiveColumn = notActiveColumn;
    }

}
