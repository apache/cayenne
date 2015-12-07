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

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.editor.ObjRelationshipTableModel;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.regex.Pattern;

public class DbRelationshipPathComboBoxEditor extends PathChooserComboBoxCellEditor implements ActionListener {

    private static final int REL_TARGET_PATH_COLUMN = 2;
    private static int enterPressedCount = 0;

    private ObjRelationshipTableModel model;

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.model = (ObjRelationshipTableModel) table.getModel();
        this.row = row;
        treeModel = createTreeModelForComboBox(row);
        if (treeModel == null) {
            return new JLabel("You should select table for this ObjectEntity");
        }
        initializeCombo(model, row, table);

        String dbRelationshipPath = ((JTextComponent) (comboBoxPathChooser).
                getEditor().getEditorComponent()).getText();
        previousEmbeddedLevel = dbRelationshipPath.split(Pattern.quote(".")).length;
        return comboBoxPathChooser;
    }

    @Override
    public Object getCellEditorValue() {
        return model.getValueAt(row, REL_TARGET_PATH_COLUMN);
    }

    @Override
    protected void initializeCombo(CayenneTableModel model, int row, final JTable table) {
        super.initializeCombo(model, row, table);
        ((JTextComponent) (comboBoxPathChooser).
                getEditor().getEditorComponent()).
                setText(((ObjRelationshipTableModel) model).getRelationship(row).getDbRelationshipPath());

        enterPressedCount = 0;
        comboBoxPathChooser.addActionListener(this);
    }

    @Override
    protected void enterPressed(JTable table) {
        String dbRelationshipPath = ((JTextComponent) (comboBoxPathChooser).
                getEditor().getEditorComponent()).getText();
        Object currentNode = getCurrentNode(dbRelationshipPath);
        String[] pathStrings = dbRelationshipPath.split(Pattern.quote("."));
        String lastStringInPath = pathStrings[pathStrings.length - 1];
        if (lastStringInPath.equals(ModelerUtil.getObjectName(currentNode))
                && currentNode instanceof DbRelationship) {
            if (enterPressedCount == 1) {
                //it is second time enter pressed.. so we will save input data
                enterPressedCount = 0;
                if (table.getCellEditor() != null) {

                    table.getCellEditor().stopCellEditing();
                    model.getRelationship(row).setDbRelationshipPath(dbRelationshipPath);

                    //we need object target to save it in model
                    DbEntity lastEntity = ((DbRelationship) currentNode).getTargetEntity();
                    Collection<ObjEntity> objEntities = ((DbRelationship) currentNode).getTargetEntity().
                            getDataMap().getMappedEntities(lastEntity);
                    ObjEntity objectTarget = objEntities.isEmpty() ? null : objEntities.iterator().next();
                    model.getRelationship(row).setTargetEntityName(objectTarget);
                }
                table.repaint();
            } else {
                enterPressedCount = 1;
            }
        }
    }

    @Override
    protected void parsePathString(char lastEnteredCharacter) {
        super.parsePathString(lastEnteredCharacter);
        enterPressedCount = 0;
    }

    @Override
    protected EntityTreeModel createTreeModelForComboBox(int relationshipIndexInTable) {
        if (model.getRelationship(relationshipIndexInTable).
                getSourceEntity().getDbEntity() == null) {
            return null;
        }
        EntityTreeModel treeModel = new EntityTreeModel(model.getRelationship(relationshipIndexInTable).
                getSourceEntity().getDbEntity());
        treeModel.setFilter(new EntityTreeRelationshipFilter());
        return treeModel;
    }

    @Override
    protected Object getCurrentNodeToInitializeCombo(CayenneTableModel model, int row) {
        return getCurrentNode(getPathToInitializeCombo(model, row));
    }

    @Override
    protected String getPathToInitializeCombo(CayenneTableModel model, int row) {
        String pathString = ((ObjRelationshipTableModel) model).getRelationship(row).getDbRelationshipPath();
        if (pathString == null) {
            return "";
        }
        String[] pathStrings = pathString.split(Pattern.quote("."));
        String lastStringInPath = pathStrings[pathStrings.length - 1];
        return pathString.replaceAll(lastStringInPath + '$', "");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        model.getRelationship(row).setMapKey(null);

        //for some reason dbRelationshipPathCombo don't load selected item text, so we made it by hand
        if (comboBoxPathChooser.getSelectedIndex() != (-1)) {
            ((JTextComponent) (comboBoxPathChooser).
                    getEditor().getEditorComponent()).setText(comboBoxPathChooser.getSelectedItem().toString());
        }
    }
}
