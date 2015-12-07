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

package org.apache.cayenne.modeler.util;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.editor.ObjAttributeTableModel;
import org.apache.cayenne.util.CayenneMapEntry;
import org.apache.commons.lang.StringUtils;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class DbAttributePathComboBoxEditor extends PathChooserComboBoxCellEditor {

    private static final int DB_ATTRIBUTE_PATH_COLUMN = 3;

    private ObjAttributeTableModel model;

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.model = (ObjAttributeTableModel) table.getModel();
        this.row = row;
        treeModel = createTreeModelForComboBox(row);
        if (treeModel == null) {
            return new JLabel("You should select table for this ObjectEntity");
        }
        initializeCombo(model, row, table);

        String dbAttributePath = ((JTextComponent) (comboBoxPathChooser).getEditor().getEditorComponent()).getText();
        previousEmbeddedLevel = StringUtils.countMatches(dbAttributePath, ".");
        return comboBoxPathChooser;
    }

    @Override
    public Object getCellEditorValue() {
        return model.getValueAt(row, DB_ATTRIBUTE_PATH_COLUMN);
    }

    @Override
    protected void initializeCombo(CayenneTableModel model, int row, final JTable table) {
        super.initializeCombo(model, row, table);
        ((JTextComponent) (comboBoxPathChooser).
                getEditor().getEditorComponent()).
                setText(((ObjAttributeTableModel) model).getAttribute(row).getValue().getDbAttributePath());
        comboBoxPathChooser.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Object currentNode = getCurrentNode((String) value);
                JLabel jLabel = new JLabel();
                jLabel.setFont(new Font("Verdana", Font.PLAIN, 13));
                if (isSelected) {
                    jLabel.setOpaque(true);
                    jLabel.setBackground(new Color(0xB4B4B4));
                }
                if (currentNode instanceof DbRelationship) {
                    if (((String) value).charAt(((String) value).length() - 1) != '.') {
                        jLabel.setText(ModelerUtil.getObjectName(value) + " ->");
                    }
                    return jLabel;
                }
                jLabel.setText(ModelerUtil.getObjectName(value));
                return jLabel;
            }
        });
    }


    @Override
    protected Object getCurrentNodeToInitializeCombo(CayenneTableModel model, int row) {
        return getCurrentNode(getPathToInitializeCombo(model, row));
    }

    @Override
    protected String getPathToInitializeCombo(CayenneTableModel model, int row) {
        String pathString = ((ObjAttributeTableModel) model).getAttribute(row).getValue().getDbAttributePath();
        if (pathString == null) {
            return "";
        }
        String[] pathStrings = pathString.split(Pattern.quote("."));
        String lastStringInPath = pathStrings[pathStrings.length - 1];
        return pathString.replaceAll(lastStringInPath + '$', "");
    }

    @Override
    protected void enterPressed(JTable table){
        String dbAttributePath =((JTextComponent) comboBoxPathChooser.
                getEditor().getEditorComponent()).getText();
        Object currentNode = getCurrentNode(dbAttributePath);

        String[] pathStrings = dbAttributePath.split(Pattern.quote("."));
        String lastStringInPath = pathStrings[pathStrings.length - 1];
        if (ModelerUtil.getObjectName(currentNode).equals(lastStringInPath) &&
                currentNode instanceof DbAttribute) {
            // in this case choose is made.. we save data

            if (table.getCellEditor() != null) {
                table.getCellEditor().stopCellEditing();
                model.getAttribute(row).setDbAttributePath(dbAttributePath);
                model.setUpdatedValueAt(dbAttributePath, row, DB_ATTRIBUTE_PATH_COLUMN);
            }
        }else if (ModelerUtil.getObjectName(currentNode).equals(lastStringInPath) &&
                currentNode instanceof DbRelationship) {
            // in this case we add dot  to pathString (if it is missing) and show variants for currentNode

            if (dbAttributePath.charAt(dbAttributePath.length()-1) != '.') {
                dbAttributePath = dbAttributePath + '.';
                previousEmbeddedLevel =  StringUtils.countMatches(dbAttributePath,".");
                ((JTextComponent) (comboBoxPathChooser).
                        getEditor().getEditorComponent()).setText(dbAttributePath);
            }
            List<String> currentNodeChildren = new ArrayList<>();
            currentNodeChildren.add(dbAttributePath);
            currentNodeChildren.addAll(getChildren(getCurrentNode(dbAttributePath), dbAttributePath));
            comboBoxPathChooser.setModel(new DefaultComboBoxModel(currentNodeChildren.toArray()));
            comboBoxPathChooser.showPopup();
            comboBoxPathChooser.setPopupVisible(true);
        }
    }

    @Override
    protected EntityTreeModel createTreeModelForComboBox(int attributeIndexInTable) {
        ObjAttribute attribute = model.getAttribute(attributeIndexInTable).getValue();
        Entity firstEntity = null;
        if (attribute.getDbAttribute() == null) {

            if (attribute.getParent() instanceof ObjEntity) {
                DbEntity dbEnt = ((ObjEntity) attribute.getParent()).getDbEntity();

                if (dbEnt != null) {
                    Collection<DbAttribute> attributes = dbEnt.getAttributes();
                    Collection<DbRelationship> rel = dbEnt.getRelationships();

                    if (!attributes.isEmpty()) {
                        Iterator<DbAttribute> iterator = attributes.iterator();
                        firstEntity = iterator.next().getEntity();
                    } else if (!rel.isEmpty()) {
                        Iterator<DbRelationship> iterator = rel.iterator();
                        firstEntity = iterator.next().getSourceEntity();
                    }
                }
            }
        } else {
            firstEntity = getFirstEntity(attribute);
        }

        if (firstEntity != null) {
            EntityTreeModel treeModel = new EntityTreeModel(firstEntity);
            treeModel.setFilter(new EntityTreeAttributeRelationshipFilter());
            return treeModel;
        }
        return null;
    }

    private Entity getFirstEntity(ObjAttribute attribute) {
        Iterator<CayenneMapEntry> it = attribute.getDbPathIterator();
        Entity firstEnt = attribute.getDbAttribute().getEntity();
        boolean setEnt = false;

        while (it.hasNext()) {
            Object ob = it.next();
            if (ob instanceof DbRelationship) {
                if (!setEnt) {
                    firstEnt = ((DbRelationship) ob).getSourceEntity();
                    setEnt = true;
                }
            } else if (ob instanceof DbAttribute) {
                if (!setEnt) {
                    firstEnt = ((DbAttribute) ob).getEntity();
                }
            }
        }
        return firstEnt;
    }
}
