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
package org.apache.cayenne.modeler.util;

import javax.swing.event.UndoableEditListener;

import org.apache.cayenne.modeler.undo.JTextFieldUndoListener;
import org.apache.cayenne.swing.components.textpane.JCayenneTextPane;
import org.apache.cayenne.swing.components.textpane.syntax.SyntaxConstant;

class JCayenneTextPaneUndoable extends JCayenneTextPane {

    private UndoableEditListener undoListener;

    JCayenneTextPaneUndoable(SyntaxConstant syntaxConstant) {
        super(syntaxConstant);

        this.undoListener = new JTextFieldUndoListener(this.getPane());
        getDocument().addUndoableEditListener(this.undoListener);
    }

    @Override
    public void setText(String t) {
        getDocument().removeUndoableEditListener(this.undoListener);

        try {
            super.setText(t);
        }
        finally {
            getDocument().addUndoableEditListener(this.undoListener);
        }
    }
}
