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

import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;

import org.apache.cayenne.modeler.undo.CayenneUndoManager;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextAreaEditorKit;
import org.fife.ui.rtextarea.RUndoManager;

/**
 * A syntax-highlighting text area that records its edits in the Modeler {@link CayenneUndoManager}, the same as
 * {@link CMUndoableTextPane}, instead of its own undo history.
 */
public class CMUndoableSyntaxTextArea extends RSyntaxTextArea {

    private final UndoableEditListener undoListener;

    public CMUndoableSyntaxTextArea(CayenneUndoManager undoManager) {
        this.undoListener = new TextComponentUndoListener(this, undoManager);
        getDocument().addUndoableEditListener(undoListener);

        // let the undo and redo keys reach the Modeler menu actions, as the own undo history of the text area is off
        InputMap inputMap = getInputMap();
        KeyStroke[] keys = inputMap.allKeys();
        if (keys != null) {
            for (KeyStroke key : keys) {
                Object action = inputMap.get(key);
                if (RTextAreaEditorKit.rtaUndoAction.equals(action)
                        || RTextAreaEditorKit.rtaRedoAction.equals(action)) {
                    inputMap.put(key, "none");
                }
            }
        }
    }

    @Override
    protected RUndoManager createUndoManager() {
        return new RUndoManager(this) {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                // the edits go to the Modeler undo manager
            }
        };
    }

    /**
     * Replaces the text without recording an undoable edit.
     */
    @Override
    public void setText(String text) {
        getDocument().removeUndoableEditListener(undoListener);
        try {
            super.setText(text);
        } finally {
            getDocument().addUndoableEditListener(undoListener);
        }
    }
}
