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

import org.apache.cayenne.project.Project;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Embeddable;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.SQLTemplateDescriptor;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.modeler.ui.project.ProjectView;
import org.apache.cayenne.modeler.ui.project.editor.EditorPanelView;
import org.apache.cayenne.modeler.ui.project.editor.query.sqltemplate.SQLTemplateTabbedView;

import javax.swing.*;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

public class TextCompoundEdit extends CompoundEdit {

    private final JTextComponent editor;
    // the listener that records the edits of the editor, detached while the text is restored
    private final UndoableEditListener recorder;

    private final String textBefore;
    private String textAfter;

    private final TreePath treePath;
    private int selectedTabIndex;
    // the SQL script of a SQLTemplate that was edited
    private String selectedScript;
    private JTabbedPane tabbedPane;

    private Object targetObject;

    private final Runnable onUndoCleanup;

    public TextCompoundEdit(
            JTextComponent editor,
            UndoableEditListener recorder,
            Runnable onUndoCleanup,
            String textBefore) {

        this.editor = editor;
        this.recorder = recorder;
        this.onUndoCleanup = onUndoCleanup;
        this.textBefore = textBefore;
        this.textAfter = editor.getText();

        ProjectView projectView = (ProjectView) SwingUtilities.getAncestorOfClass(ProjectView.class, editor);
        if (projectView == null) {
            // editor not yet mounted; selection capture is best-effort
            this.treePath = null;
            return;
        }

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

        if (targetObject instanceof SelectQueryDescriptor) {
            tabbedPane = editorPanel.getSelectQueryView();
        }

        if (targetObject instanceof SQLTemplateDescriptor) {
            tabbedPane = editorPanel.getSqlTemplateView();

            if (tabbedPane != null) {
                selectedScript = ((SQLTemplateTabbedView) tabbedPane).getSelectedScript();
            }
        }

        if (targetObject instanceof DataMap) {
            tabbedPane = editorPanel.getDataMapView();
        }

        if (targetObject instanceof Project) {
            tabbedPane = editorPanel.getProjectEditorView();
        }

        if (tabbedPane != null) {
            selectedTabIndex = tabbedPane.getSelectedIndex();
        }
    }

    private void restoreSelections() {

        ProjectView projectView = (ProjectView) SwingUtilities.getAncestorOfClass(ProjectView.class, editor);
        if (projectView == null) {
            return;
        }

        projectView.getProjectTreeView().getSelectionModel().setSelectionPath(treePath);

        if (tabbedPane != null) {
            tabbedPane.setSelectedIndex(selectedTabIndex);

            if (tabbedPane instanceof SQLTemplateTabbedView sqlTemplateView && selectedScript != null) {
                sqlTemplateView.showScript(selectedScript);
            }
        }
    }

    /**
     * Takes the latest edit of the editor into this group. The edit itself isn't kept, only the text after it.
     */
    public void editHappened() {
        textAfter = editor.getText();
    }

    /**
     * Doesn't absorb other edits, the edits of the editor are taken with {@link #editHappened()}.
     */
    @Override
    public boolean addEdit(UndoableEdit anEdit) {
        return false;
    }

    // the CompoundEdit is significant only if its child edits are, and there are none here
    @Override
    public boolean isSignificant() {
        return true;
    }

    private void applyText(String text) {
        String current = editor.getText();

        int prefix = 0;
        int maxPrefix = Math.min(current.length(), text.length());
        while (prefix < maxPrefix && current.charAt(prefix) == text.charAt(prefix)) {
            prefix++;
        }

        int suffix = 0;
        int maxSuffix = maxPrefix - prefix;
        while (suffix < maxSuffix
                && current.charAt(current.length() - 1 - suffix) == text.charAt(text.length() - 1 - suffix)) {
            suffix++;
        }

        String replacement = text.substring(prefix, text.length() - suffix);
        Document document = editor.getDocument();
        document.removeUndoableEditListener(recorder);
        try {
            int length = current.length() - suffix - prefix;
            if (document instanceof AbstractDocument abstractDocument) {
                abstractDocument.replace(prefix, length, replacement, null);
            } else {
                document.remove(prefix, length);
                document.insertString(prefix, replacement, null);
            }
        } catch (BadLocationException e) {
            throw new IllegalStateException("Can't restore the text", e);
        } finally {
            document.addUndoableEditListener(recorder);
        }

        editor.setCaretPosition(prefix + replacement.length());
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
            applyText(textAfter);
        } else {
            die();
        }

        editor.requestFocusInWindow();
    }

    @Override
    public void undo() throws CannotUndoException {
        restoreSelections();

        onUndoCleanup.run();

        if (canUndo()) {
            super.undo();
            applyText(textBefore);
        } else {
            die();
        }

        editor.requestFocusInWindow();

        // a single-line field is selected as a whole, a multi-line editor keeps the caret where the undo left it
        if (editor instanceof JTextField) {
            editor.selectAll();
        }
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
}
