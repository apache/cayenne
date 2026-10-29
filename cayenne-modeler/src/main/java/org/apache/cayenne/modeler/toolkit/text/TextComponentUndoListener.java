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
package org.apache.cayenne.modeler.toolkit.text;

import org.apache.cayenne.modeler.undo.CayenneUndoManager;
import org.apache.cayenne.modeler.undo.TextCompoundEdit;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

class TextComponentUndoListener implements UndoableEditListener {

    private final JTextComponent editor;
    private final CayenneUndoManager undoManager;
    private TextCompoundEdit compoundEdit;

    private int lastOffset;
    private int lastLength;

    private boolean isKeyEdit;

    // the text before and after the last change of the document, including the changes that aren't recorded
    private String previousText;
    private String currentText;

    public TextComponentUndoListener(JTextComponent editor, CayenneUndoManager undoManager) {
        this.editor = editor;
        this.undoManager = undoManager;
        this.currentText = editor.getText();
        this.previousText = currentText;
        this.editor.getDocument().addDocumentListener(new TextTracker());
        this.editor.addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent e) {
                finishCurrentEdit();
            }
        });
        this.editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                isKeyEdit = true;
            }
        });
    }

    @Override
    public void undoableEditHappened(UndoableEditEvent e) {

        if (compoundEdit == null || !compoundEdit.canUndo()) {
            compoundEdit = startCompoundEdit();
            lastLength = editor.getDocument().getLength();
            return;
        }

        // See AbstractDocument.DefaultDocumentEvent.getPresentationName() method
        if ("AbstractDocument.styleChangeText".equals(e.getEdit().getPresentationName())) {
            compoundEdit.editHappened();
            return;
        }

        int offsetChange = editor.getCaretPosition() - lastOffset;
        int lengthChange = editor.getDocument().getLength() - lastLength;

        if (Math.abs(offsetChange) == 1 && Math.abs(lengthChange) == 1) {
            compoundEdit.editHappened();
            lastOffset = editor.getCaretPosition();
            lastLength = editor.getDocument().getLength();
            return;
        }

        compoundEdit.end();
        compoundEdit = startCompoundEdit();
    }

    private TextCompoundEdit startCompoundEdit() {

        lastOffset = editor.getCaretPosition();
        lastLength = editor.getDocument().getLength();

        compoundEdit = new TextCompoundEdit(editor, this, this::finishCurrentEdit, previousText);

        if (isKeyEdit) {
            compoundEdit.editHappened();
        } else {
            return null;
        }

        undoManager.addEdit(compoundEdit);
        return compoundEdit;
    }

    public void finishCurrentEdit() {
        if (compoundEdit != null) {
            compoundEdit.end();
            compoundEdit = null;
            isKeyEdit = false;
        }
    }

    // document listeners are notified before the undoable edit listeners, so the previous text is the text before the
    // edit being recorded
    private final class TextTracker implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            textChanged();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            textChanged();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            // style changes only
        }

        private void textChanged() {
            previousText = currentText;
            currentText = editor.getText();
        }
    }
}
