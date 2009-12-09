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

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.util.TextAdapter;

public class JTextFieldUndoListener implements UndoableEditListener {

    public CompoundEdit compoundEdit;
    
    private TextAdapter adapter;

    private int lastOffset;
    private int lastLength;

    public JTextFieldUndoListener(TextAdapter adapter) {
        this.adapter = adapter;

        this.adapter.getComponent().addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent e) {
                if (compoundEdit != null) {
                    compoundEdit.end();
                    compoundEdit = null;
                }
            }
        });
    }

    public void undoableEditHappened(UndoableEditEvent e) {
        
        JTextComponent editor = adapter.getComponent();
        
        if (compoundEdit == null || !compoundEdit.canUndo()) {
            compoundEdit = startCompoundEdit(e.getEdit());
            lastLength = editor.getDocument().getLength();
            return;
        }

        AbstractDocument.DefaultDocumentEvent event = (AbstractDocument.DefaultDocumentEvent) e
                .getEdit();

        if (event.getType().equals(DocumentEvent.EventType.CHANGE)) {
            compoundEdit.addEdit(e.getEdit());
            return;
        }

        int offsetChange = editor.getCaretPosition() - lastOffset;
        int lengthChange = editor.getDocument().getLength() - lastLength;

        if (Math.abs(offsetChange) == 1 && Math.abs(lengthChange) == 1) {
            compoundEdit.addEdit(e.getEdit());
            lastOffset = editor.getCaretPosition();
            lastLength = editor.getDocument().getLength();
            return;
        }

        compoundEdit.end();
        compoundEdit = startCompoundEdit(e.getEdit());
    }

    private CompoundEdit startCompoundEdit(UndoableEdit anEdit) {
        
        JTextComponent editor = adapter.getComponent();
        
        lastOffset = editor.getCaretPosition();
        lastLength = editor.getDocument().getLength();

        compoundEdit = new TextCompoundEdit(adapter);
        compoundEdit.addEdit(anEdit);

        Application.getInstance().getUndoManager().addEdit(compoundEdit);

        return compoundEdit;
    }
}
