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

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;
import org.apache.commons.lang.StringUtils;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.text.JTextComponent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class used as cell editor, when you need to
 * choose path in comboBox and use autocompletion.
 */
public abstract class PathChooserComboBoxCellEditor extends AbstractCellEditor implements TableCellEditor {

    protected JComboBox comboBoxPathChooser;
    protected int previousEmbeddedLevel = 0;
    protected EntityTreeModel treeModel;
    protected int row;

    protected abstract void enterPressed(JTable table);

    protected abstract EntityTreeModel createTreeModelForComboBox(int indexInTable);

    protected abstract Object getCurrentNodeToInitializeCombo(CayenneTableModel model, int row);

    protected abstract String getPathToInitializeCombo(CayenneTableModel model, int row);

    protected void initializeCombo(CayenneTableModel model, int row, final JTable table) {
        Object currentNode = getCurrentNodeToInitializeCombo(model, row);
        String dbAttributePath = getPathToInitializeCombo(model, row);
        List<String> nodeChildren = getChildren(currentNode, dbAttributePath);
        comboBoxPathChooser = Application.getWidgetFactory().createComboBox(
                nodeChildren,
                false);
        comboBoxPathChooser.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.VK_ENTER) {
                    enterPressed(table);
                    return;
                }
                parsePathString(event.getKeyChar());
            }
        });
        AutoCompletion.enable(comboBoxPathChooser, true, true);
        ((JComponent) comboBoxPathChooser.getEditor().getEditorComponent()).setBorder(null);
        comboBoxPathChooser.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));
    }

    private void setComboModelAccordingToPath(String pathString) {
        List<String> currentNodeChildren = new ArrayList<>();
        currentNodeChildren.add(pathString);
        currentNodeChildren.addAll(getChildren(getCurrentNode(pathString), pathString));
        comboBoxPathChooser.setModel(new DefaultComboBoxModel(currentNodeChildren.toArray()));
        comboBoxPathChooser.showPopup();
        comboBoxPathChooser.setPopupVisible(true);
    }

    protected void parsePathString(char lastEnteredCharacter) {
        JTextComponent editorComponent = (JTextComponent) (comboBoxPathChooser).getEditor().getEditorComponent();

        String pathString = editorComponent.getText();
        if (pathString != null && pathString.isEmpty()) {
            setComboModelAccordingToPath("");
            previousEmbeddedLevel = StringUtils.countMatches(pathString, ".");
            return;
        }

        if (lastEnteredCharacter == '.') {
            processDotEntered();
            previousEmbeddedLevel = StringUtils.countMatches(pathString, ".");
            return;
        }

        int currentEmbeddedLevel = StringUtils.countMatches(pathString, ".");
        if (previousEmbeddedLevel != currentEmbeddedLevel) {
            previousEmbeddedLevel = currentEmbeddedLevel;
            List<String> currentNodeChildren = new ArrayList<>();
            String[] pathStrings = pathString.split(Pattern.quote("."));
            String lastStringInPath = pathStrings[pathStrings.length - 1];
            String saveDbAttributePath = pathString;
            pathString = pathString.replaceAll(lastStringInPath + "$", "");
            currentNodeChildren.addAll(getChildren(getCurrentNode(pathString), pathString));
            comboBoxPathChooser.setModel(new DefaultComboBoxModel(currentNodeChildren.toArray()));
            editorComponent.setText(saveDbAttributePath);
            return;
        }
    }

    private void processDotEntered() {
        JTextComponent editorComponent = (JTextComponent) (comboBoxPathChooser).getEditor().getEditorComponent();

        String dbAttributePath = editorComponent.getText();
        if (".".equals(dbAttributePath)) {
            setComboModelAccordingToPath("");
            return;
        }
        char secondFromEndCharacter = dbAttributePath.charAt(dbAttributePath.length() - 2);
        if (secondFromEndCharacter == '.') {
            // two dots entered one by one , we replace it by one dot
            editorComponent.setText(dbAttributePath.substring(0, dbAttributePath.length() - 1));
            return;
        }
        String[] pathStrings = dbAttributePath.split(Pattern.quote("."));
        String lastStringInPath = pathStrings[pathStrings.length - 1];

        //we will check if lastStringInPath is correct name of DbAttribute or DbRelationship
        //for appropriate previous node in path. if it is not we won't add entered dot to dbAttributePath
        String dbAttributePathForPreviousNode;
        if (pathStrings.length == 1) {
            //previous root is treeModel.getRoot()
            dbAttributePathForPreviousNode = "";
        } else {
            dbAttributePathForPreviousNode = dbAttributePath.replace('.' + lastStringInPath, "");
        }
        List<String> potentialVariantsToChoose = getChildren(getCurrentNode(dbAttributePathForPreviousNode), "");
        if (potentialVariantsToChoose.contains(lastStringInPath)) {
            setComboModelAccordingToPath(dbAttributePath);
        } else {
            editorComponent.setText(dbAttributePath.substring(0, dbAttributePath.length() - 1));
        }
        previousEmbeddedLevel = StringUtils.countMatches(dbAttributePath, ".");
    }

    /**
     * find current node by dbAttributePath
     *
     * @param pathString
     * @return last node in dbAttributePath which matches DbRelationship or DbAttribute
     */
    protected Object getCurrentNode(String pathString) {
        //case for new attribute
        if (pathString == null || pathString.isEmpty()) {
            return treeModel.getRoot();
        }
        String[] pathStrings = pathString.split(Pattern.quote("."));
        Object root = treeModel.getRoot();
        for (String rootChildText : pathStrings) {
            for (int j = 0; j < treeModel.getChildCount(root); j++) {
                Object child = treeModel.getChild(root, j);
                String objectName = ModelerUtil.getObjectName(child);
                if (objectName.equals(rootChildText)) {
                    root = child;
                    break;
                }
            }
        }
        return root;
    }

    /**
     * @param node       for which we will find children
     * @param pathString string which will be added to each child to make right autocomplete
     * @return list with children , which will be used to autocomplete
     */
    protected List<String> getChildren(Object node, String pathString) {
        List<String> currentNodeChildren = new ArrayList<>();
        for (int j = 0; j < treeModel.getChildCount(node); j++) {
            Object child = treeModel.getChild(node, j);
            String relationshipName = ModelerUtil.getObjectName(child);
            currentNodeChildren.add(pathString + relationshipName);
        }
        return currentNodeChildren;
    }
}
