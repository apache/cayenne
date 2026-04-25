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
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.ModelerFrame;
import org.apache.cayenne.modeler.ui.project.ProjectView;
import org.apache.cayenne.modeler.ui.project.editor.EditorPanelView;
import org.apache.cayenne.modeler.ui.project.editor.query.sqltemplate.SQLTemplateTabbedView;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.SQLTemplate;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;

public class TextCompoundEdit extends CompoundEdit implements DocumentListener {

    private final JTextComponent editor;

    private final TreePath treePath;
    private int selectedTabIndex;
    private int selectedItem;
    private JTabbedPane tabbedPane;

    private Object targetObject;

    private final JTextFieldUndoListener listener;

    public TextCompoundEdit(JTextComponent editor, JTextFieldUndoListener listener) {

        this.editor = editor;
        this.listener = listener;

        ProjectView projectView = ((ModelerFrame) Application.getInstance().getFrameController().getView())
                .getProjectView();

        treePath = projectView.getProjectTreeView().getSelectionPath();

        if (treePath != null) {
            DefaultMutableTreeNode newPath = (DefaultMutableTreeNode) treePath.getLastPathComponent();
            targetObject = newPath.getUserObject();
        }

        EditorPanelView editorPanel = projectView.getEditorPanel();

        if (targetObject instanceof ObjEntity) {
            tabbedPane = editorPanel.getObjDetailView();
        }

        if (targetObject instanceof DbEntity) {
            tabbedPane = editorPanel.getDbDetailView();
        }

        if (targetObject instanceof Embeddable) {
            tabbedPane = editorPanel.getEmbeddableView();
        }

        if (targetObject instanceof SQLTemplate) {
            tabbedPane = editorPanel.getSqlTemplateView();

            if (tabbedPane != null) {
                selectedItem = ((SQLTemplateTabbedView) tabbedPane).getScriptsTab().getSelectedIndex();
            }
        }

        if (targetObject instanceof EJBQLQuery) {
            tabbedPane = editorPanel.getEjbqlQueryView();
        }

        if (targetObject instanceof DataMap) {
            tabbedPane = editorPanel.getDataMapView();
        }

        if (targetObject instanceof DataChannelDescriptor) {
            tabbedPane = editorPanel.getDataDomainView();
        }

        if (tabbedPane != null) {
            selectedTabIndex = tabbedPane.getSelectedIndex();
        }
    }

    private void restoreSelections() {

        ProjectView projectView = ((ModelerFrame) Application.getInstance().getFrameController().getView())
                .getProjectView();

        projectView.getProjectTreeView().getSelectionModel().setSelectionPath(treePath);

        if (tabbedPane != null) {
            tabbedPane.setSelectedIndex(selectedTabIndex);

            if (tabbedPane instanceof SQLTemplateTabbedView) {
                ((SQLTemplateTabbedView) tabbedPane).getScriptsTab().setSelectedIndex(selectedItem);
            }
        }
    }

    public void insertUpdate(final DocumentEvent e) {
        SwingUtilities.invokeLater(() -> {
            int offset = e.getOffset() + e.getLength();
            offset = Math.min(offset, editor.getDocument().getLength());
            editor.setCaretPosition(offset);
        });
    }


    @Override
    public void removeUpdate(DocumentEvent e) {
        editor.setCaretPosition(e.getOffset());
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
    }

    @Override
    public boolean isInProgress() {
        return false;
    }

    @Override
    public void redo() throws CannotRedoException {
        restoreSelections();

        if (canRedo()) {
            super.redo();
        } else {
            die();
        }

        editor.requestFocusInWindow();
    }

    @Override
    public void undo() throws CannotUndoException {
        restoreSelections();

        listener.finishCurrentEdit();

        if (canUndo()) {
            super.undo();
        } else {
            die();
        }

        editor.requestFocusInWindow();
        editor.selectAll();
    }

    @Override
    public String getRedoPresentationName() {
        return "Redo Text Change";
    }

    @Override
    public String getUndoPresentationName() {
        return "Undo Text Change";
    }

    public JTextComponent getEditor() {
        return editor;
    }

    public void watchCaretPosition() {
        editor.getDocument().addDocumentListener(this);
    }

    public void stopWatchingCaretPosition() {
        editor.getDocument().removeDocumentListener(this);
    }
}