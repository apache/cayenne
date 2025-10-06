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

package org.apache.cayenne.modeler.util;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.combo.AutoCompletion;
import org.apache.cayenne.util.Util;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.TableCellEditor;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class used as cell editor, when you need to
 * choose path in comboBox and use autocompletion.
 */
public abstract class PathChooserComboBoxCellEditor<T extends CayenneTableModel<?>> extends AbstractCellEditor implements TableCellEditor, ActionListener, PopupMenuListener {

    protected JComboBox<String> comboBoxPathChooser;
    protected int previousEmbeddedLevel = 0;
    protected EntityTreeModel treeModel;
    protected int row;
    private JTable table;

    protected abstract void enterPressed(JTable table);

    protected abstract EntityTreeModel createTreeModelForComboBox(int indexInTable);

    protected abstract Object getCurrentNodeToInitializeCombo(T model, int row);

    protected abstract String getPathToInitializeCombo(T model, int row);

    protected void initializeCombo(T model, int row, final JTable table) {
        Object currentNode = getCurrentNodeToInitializeCombo(model, row);
        String dbAttributePath = getPathToInitializeCombo(model, row);
        List<String> nodeChildren = getChildren(currentNode, dbAttributePath);
        this.table = table;
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
        comboBoxPathChooser.setRenderer(new PathChooserComboBoxCellRenderer());
        comboBoxPathChooser.addActionListener(this);
        comboBoxPathChooser.addPopupMenuListener(this);
    }

    private void setComboModelAccordingToPath(String pathString) {
        List<String> currentNodeChildren = new ArrayList<>(getChildren(getCurrentNode(pathString), pathString));
        comboBoxPathChooser.setModel(new DefaultComboBoxModel<>(currentNodeChildren.toArray(new String[0])));
        comboBoxPathChooser.setSelectedItem(pathString);
        if(!pathString.isEmpty()) {
            comboBoxPathChooser.showPopup();
        }
    }

    protected void parsePathString(char lastEnteredCharacter) {
        JTextComponent editorComponent = (JTextComponent) (comboBoxPathChooser).getEditor().getEditorComponent();
        String pathString = editorComponent.getText();
        if (pathString != null && pathString.isEmpty()) {
            setComboModelAccordingToPath("");
            previousEmbeddedLevel = 0;
            return;
        }

        if (lastEnteredCharacter == '.') {
            processDotEntered();
            previousEmbeddedLevel = Util.countMatches(pathString, ".");
            return;
        }

        int currentEmbeddedLevel = Util.countMatches(pathString, ".");
        if (previousEmbeddedLevel != currentEmbeddedLevel) {
            previousEmbeddedLevel = currentEmbeddedLevel;
            String[] pathStrings = pathString.split(Pattern.quote("."));
            String lastStringInPath = pathStrings[pathStrings.length - 1];
            String saveDbAttributePath = pathString;
            pathString = pathString.replaceAll(lastStringInPath + "$", "");
            List<String> currentNodeChildren = new ArrayList<>(getChildren(getCurrentNode(pathString), pathString));
            comboBoxPathChooser.setModel(new DefaultComboBoxModel<>(currentNodeChildren.toArray(new String[0])));
            comboBoxPathChooser.setSelectedItem(saveDbAttributePath);
        }
    }

    protected void processDotEntered() {
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
            dbAttributePathForPreviousNode = dbAttributePath.replaceAll('.' + lastStringInPath + ".$", "");
        }
        List<String> potentialVariantsToChoose = getChildren(getCurrentNode(dbAttributePathForPreviousNode), "");
        if (potentialVariantsToChoose.contains(lastStringInPath) &&
                !(getCurrentNode(dbAttributePath) instanceof DbAttribute)) {
            setComboModelAccordingToPath(dbAttributePath);
        } else {
            editorComponent.setText(dbAttributePath.substring(0, dbAttributePath.length() - 1));
        }
    }

    /**
     * find current node by path
     *
     * @param pathString db path
     * @return last node in path which matches DbRelationship or DbAttribute
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

    @Override
    public void actionPerformed(ActionEvent e) {
        //for some reason comboBoxPathChooser don't load selected item text, so we made it by hand
        if (comboBoxPathChooser.getSelectedIndex() != (-1)) {
            ((JTextComponent) (comboBoxPathChooser).
                    getEditor().getEditorComponent()).setText(comboBoxPathChooser.getSelectedItem().toString());
        }
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        if (comboBoxPathChooser.getSelectedIndex() != -1 &&
                !((JTextComponent) (comboBoxPathChooser).
                        getEditor().getEditorComponent()).getText().isEmpty()) {
            enterPressed(table);
        }
    }

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
    }

    private final class PathChooserComboBoxCellRenderer extends DefaultListCellRenderer {

        private  final ImageIcon rightArrow = ModelerUtil.buildIcon("icon-arrow-closed.png");

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {

            JPanel panel = new JPanel(new BorderLayout());
            JLabel label = new JLabel(value.toString());
            panel.add(label);

            Object currentNode = getCurrentNode(value.toString());
            if (treeModel.isLeaf(currentNode)) {
                ListCellRenderer<Object> leafRenderer = CellRenderers.listRenderer();
                return leafRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            } else {
                DefaultListCellRenderer nonLeafTextRenderer = new DefaultListCellRenderer();
                Component text = nonLeafTextRenderer.getListCellRendererComponent(list, value, index, isSelected,
                        cellHasFocus);
                panel.setBackground(text.getBackground());
                panel.add(new JLabel(rightArrow), BorderLayout.EAST);
                return panel;
            }
        }
    }
}
