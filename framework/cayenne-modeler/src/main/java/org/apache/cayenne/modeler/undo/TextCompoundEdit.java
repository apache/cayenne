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

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.CayenneModelerFrame;
import org.apache.cayenne.modeler.editor.EditorView;
import org.apache.cayenne.modeler.util.TextAdapter;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.SQLTemplate;

public class TextCompoundEdit extends CompoundEdit implements DocumentListener {

    private TextAdapter adapter;

    private TreePath treePath;
    private int selectedTabIndex;
    private JTabbedPane tabbedPane;
    
    private Object targetObject;

    public Object getTargetObject() {
        return targetObject;
    }

    public TextCompoundEdit(TextAdapter adapter) {
        this.adapter = adapter;

        EditorView editorView = ((CayenneModelerFrame) Application
                .getInstance()
                .getFrameController()
                .getView()).getView();

        treePath = editorView.getProjectTreeView().getSelectionPath();

        DefaultMutableTreeNode newPath = (DefaultMutableTreeNode) treePath
                .getLastPathComponent();

        targetObject = newPath.getUserObject();

        if (targetObject instanceof ObjEntity) {
            tabbedPane = editorView.getObjDetailView();
        }

        if (targetObject instanceof DbEntity) {
            tabbedPane = editorView.getDbDetailView();
        }

        if (targetObject instanceof Embeddable) {
            tabbedPane = editorView.getEmbeddableView();
        }

        if (targetObject instanceof SQLTemplate) {
            tabbedPane = editorView.getSqlTemplateView();
        }

        if (targetObject instanceof EJBQLQuery) {
            tabbedPane = editorView.getEjbqlQueryView();
        }

        if (targetObject instanceof DataNode) {
            tabbedPane = editorView.getDataNodeView();
        }

        if (targetObject instanceof DataMap) {
            tabbedPane = editorView.getDataMapView();
        }

        if (tabbedPane != null) {
            selectedTabIndex = tabbedPane.getSelectedIndex();
        }
    }

    private void restoreSelections() {

        EditorView editorView = ((CayenneModelerFrame) Application
                .getInstance()
                .getFrameController()
                .getView()).getView();

        editorView.getProjectTreeView().getSelectionModel().setSelectionPath(treePath);

        if (tabbedPane != null) {
            tabbedPane.setSelectedIndex(selectedTabIndex);
        }
    }

    public void insertUpdate(final DocumentEvent e) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                int offset = e.getOffset() + e.getLength();
                offset = Math.min(offset, adapter
                        .getComponent()
                        .getDocument()
                        .getLength());
                adapter.getComponent().setCaretPosition(offset);
            }
        });
    }

    public void removeUpdate(DocumentEvent e) {
        adapter.getComponent().setCaretPosition(e.getOffset());
    }

    public void changedUpdate(DocumentEvent e) {
    }

    public boolean isInProgress() {
        return false;
    }

    @Override
    public void redo() throws CannotRedoException {
        restoreSelections();

        super.redo();

        adapter.getComponent().requestFocusInWindow();
    }

    public void undo() throws CannotUndoException {
        restoreSelections();

        end();

        super.undo();

        adapter.updateModel();

        adapter.getComponent().requestFocusInWindow();
        adapter.getComponent().selectAll();
    }

    @Override
    public synchronized String getRedoPresentationName() {
        return "Redo Text Change";
    }

    @Override
    public synchronized String getUndoPresentationName() {
        return "Undo Text Change";
    }

    public JTextComponent getEditor() {
        return adapter.getComponent();
    }

    public void watchCaretPosition() {
        adapter.getComponent().getDocument().addDocumentListener(this);
    }

    public void stopWatchingCaretPosition() {
        adapter.getComponent().getDocument().removeDocumentListener(this);
    }
}