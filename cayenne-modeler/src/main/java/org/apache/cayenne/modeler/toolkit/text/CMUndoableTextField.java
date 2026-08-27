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

import javax.swing.event.UndoableEditListener;

public class CMUndoableTextField extends CMTextField {

    private final UndoableEditListener undoListener;

    public CMUndoableTextField(CayenneUndoManager undoManager) {
        this.undoListener = new TextComponentUndoListener(this, undoManager);
        this.getDocument().addUndoableEditListener(this.undoListener);
    }

    public CMUndoableTextField(CayenneUndoManager undoManager, int columns) {
        super(columns);
        this.undoListener = new TextComponentUndoListener(this, undoManager);
        this.getDocument().addUndoableEditListener(this.undoListener);
    }

    @Override
    public void setText(String t) {
        this.getDocument().removeUndoableEditListener(this.undoListener);
        try {
            super.setText(t);
        } finally {
            this.getDocument().addUndoableEditListener(this.undoListener);
        }
    }
}
