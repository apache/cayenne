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

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;

import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.CayenneModelerFrame;
import org.apache.cayenne.modeler.ui.project.ProjectView;
import org.apache.cayenne.modeler.ui.project.editor.query.sqltemplate.SQLTemplateTabbedView;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.SQLTemplate;

public class TextCompoundEdit extends CompoundEdit implements DocumentListener {

    private TextAdapter adapter;
    private JTextComponent editor;

    private TreePath treePath;
    private int selectedTabIndex;
    private int selectedItem;
    private JTabbedPane tabbedPane;

    private Object targetObject;

    private JTextFieldUndoListener listener;

    public Object getTargetObject() {
        return targetObject;
    }

    public TextCompoundEdit(TextAdapter adapter, JTextFieldUndoListener listener) {
        this(adapter.getComponent(), listener);
        this.adapter = adapter;
    }

    public TextCompoundEdit(JTextComponent editor, JTextFieldUndoListener listener) {

        this.editor = editor;
        this.listener = listener;

        ProjectView projectView = ((CayenneModelerFrame) Application.getInstance().getFrameController().getView())
                .getEditorPanel();

        treePath = projectView.getProjectTreeView().getSelectionPath();

        if(treePath != null) {
            DefaultMutableTreeNode newPath = (DefaultMutableTreeNode) treePath.getLastPathComponent();
            targetObject = newPath.getUserObject();
        }

        if (targetObject instanceof ObjEntity) {
            tabbedPane = projectView.getObjDetailView();
        }

        if (targetObject instanceof DbEntity) {
            tabbedPane = projectView.getDbDetailView();
        }

        if (targetObject instanceof Embeddable) {
            tabbedPane = projectView.getEmbeddableView();
        }

        if (targetObject instanceof SQLTemplate) {
            tabbedPane = projectView.getSqlTemplateView();

            if (tabbedPane != null) {
                selectedItem = ((SQLTemplateTabbedView) tabbedPane).getScriptsTab().getSelectedIndex();
            }
        }

        if (targetObject instanceof EJBQLQuery) {
            tabbedPane = projectView.getEjbqlQueryView();
        }

        if (targetObject instanceof DataMap) {
            tabbedPane = projectView.getDataMapView();
        }

        if (targetObject instanceof DataChannelDescriptor) {
            tabbedPane = projectView.getDataDomainView();
        }

        if (tabbedPane != null) {
            selectedTabIndex = tabbedPane.getSelectedIndex();
        }
    }

    private void restoreSelections() {

        ProjectView projectView = ((CayenneModelerFrame) Application.getInstance().getFrameController().getView())
                .getEditorPanel();

        projectView.getProjectTreeView().getSelectionModel().setSelectionPath(treePath);

        if (tabbedPane != null) {
            tabbedPane.setSelectedIndex(selectedTabIndex);

            if (tabbedPane instanceof SQLTemplateTabbedView) {
                ((SQLTemplateTabbedView) tabbedPane).getScriptsTab().setSelectedIndex(selectedItem);
            }
        }
    }

    public void insertUpdate(final DocumentEvent e) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                int offset = e.getOffset() + e.getLength();
                offset = Math.min(offset, editor.getDocument().getLength());
                editor.setCaretPosition(offset);
            }
        });
    }

    public void removeUpdate(DocumentEvent e) {
        editor.setCaretPosition(e.getOffset());
    }

    public void changedUpdate(DocumentEvent e) {
    }

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

    public void undo() throws CannotUndoException {
        restoreSelections();

        listener.finishCurrentEdit();

        if (canUndo()) {
            super.undo();
        } else {
            die();
        }

        if (adapter != null) {
            adapter.updateModel();
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