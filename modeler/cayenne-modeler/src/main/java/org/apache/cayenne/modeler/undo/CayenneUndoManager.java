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

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.action.RedoAction;
import org.apache.cayenne.modeler.action.UndoAction;
import org.apache.cayenne.modeler.util.CayenneAction;

public class CayenneUndoManager extends javax.swing.undo.UndoManager {

    private Application application;

    public CayenneUndoManager(Application application) {
        this.application = application;
        setLimit(100);
    }

    @Override
    public void discardAllEdits() {
        super.discardAllEdits();
        updateUI();
    }

    @Override
    public boolean addEdit(UndoableEdit anEdit) {
        boolean result = super.addEdit(anEdit);
        updateUI();
        return result;
    }

    @Override
    public void redo() throws CannotRedoException {
        UndoableEdit e = editToBeRedone();

        if (e instanceof TextCompoundEdit) {
            TextCompoundEdit edit = (TextCompoundEdit) e;

            edit.watchCaretPosition();

            super.redo();

            edit.stopWatchingCaretPosition();
        } else {
            super.redo();
        }

        updateUI();
    }

    @Override
    public void undo() throws CannotUndoException {
        UndoableEdit e = editToBeUndone();

        if (e instanceof TextCompoundEdit) {
            TextCompoundEdit edit = (TextCompoundEdit) e;

            edit.watchCaretPosition();

            super.undo();

            edit.stopWatchingCaretPosition();
        } else {
            super.undo();
        }

        updateUI();
    }

    private void updateUI() {
        CayenneAction undoAction = application.getActionManager().getAction(UndoAction.class);

        CayenneAction redoAction = application.getActionManager().getAction(RedoAction.class);

        undoAction.setEnabled(canUndo());
        redoAction.setEnabled(canRedo());

        undoAction.setName(getUndoPresentationName());
        redoAction.setName(getRedoPresentationName());
    }
}
