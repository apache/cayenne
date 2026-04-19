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

package org.apache.cayenne.modeler.undo;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerFrame;
import org.apache.cayenne.modeler.editor.EditorPanel;
import org.apache.cayenne.query.SQLTemplate;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.awt.event.ActionListener;

public class JUndoableCheckBoxEdit extends AbstractUndoableEdit {

    private JCheckBox checkBox;
    private ActionListener actionListener;
    private JTabbedPane tabbedPane;
    private TreePath treePath;
    private Object targetObject;
    private EditorPanel editorPanel;

    private int selectedTabIndex;

    private boolean isSelected;


    JUndoableCheckBoxEdit(JCheckBox checkBox, ActionListener actionListener) {

        this.checkBox = checkBox;
        this.actionListener = actionListener;
        this.isSelected = checkBox.isSelected();

        editorPanel = ((CayenneModelerFrame) Application.getInstance().getFrameController().getView()).getEditorPanel();

        treePath = editorPanel.getProjectTreeView().getSelectionPath();

        if (treePath != null) {
            DefaultMutableTreeNode newPath = (DefaultMutableTreeNode) treePath.getLastPathComponent();
            targetObject = newPath.getUserObject();
        }

        if (targetObject instanceof DataChannelDescriptor) {
            tabbedPane = editorPanel.getDataDomainView();
        }

        if (targetObject instanceof DataMap) {
            tabbedPane = editorPanel.getDataMapView();
        }

        if (targetObject instanceof ObjEntity) {
            tabbedPane = editorPanel.getObjDetailView();
        }

        if (targetObject instanceof SQLTemplate) {
            tabbedPane = editorPanel.getSqlTemplateView();
        }

        if (tabbedPane != null) {
            selectedTabIndex = tabbedPane.getSelectedIndex();
        }
    }

    private void restoreSelections() {

        editorPanel.getProjectTreeView().getSelectionModel().setSelectionPath(treePath);

        if (tabbedPane != null) {
            tabbedPane.setSelectedIndex(selectedTabIndex);
        }
    }

    public String getPresentationName() {
        return "CheckBox Change";
    }

    public void redo() throws CannotRedoException {
        super.redo();

        restoreSelections();
        checkBox.removeActionListener(actionListener);
        try {
            checkBox.setSelected(isSelected);
        } finally {
            checkBox.addActionListener(actionListener);
        }
    }

    public void undo() throws CannotUndoException {
        super.undo();

        restoreSelections();
        checkBox.removeActionListener(actionListener);
        try {
            checkBox.setSelected(!isSelected);
        } finally {
            checkBox.addActionListener(actionListener);
        }
    }
}
