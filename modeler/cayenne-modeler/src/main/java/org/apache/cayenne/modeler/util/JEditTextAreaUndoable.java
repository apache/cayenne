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

import java.awt.event.FocusListener;

import javax.swing.JTextField;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;

import org.apache.cayenne.modeler.undo.JTextFieldUndoListener;
import org.syntax.jedit.JEditTextArea;

class JEditTextAreaUndoable extends JEditTextArea {

    private UndoableEditListener undoListener;

    JEditTextAreaUndoable() {
        this.undoListener = new JTextFieldUndoListener(new JEditTextAreaUndoableAdapter(
                this));

        this.getDocument().addUndoableEditListener(this.undoListener);
    }

    @Override
    public void setText(String t) {
        this.getDocument().removeUndoableEditListener(this.undoListener);
        try {
            super.setText(t);
        }
        finally {
            this.getDocument().addUndoableEditListener(this.undoListener);
        }
    }

    private static class JEditTextAreaUndoableAdapter extends JTextField {

        @Override
        public synchronized void addFocusListener(FocusListener l) {
            if (textArea != null) {
                textArea.addFocusListener(l);
            }
        }

        private JEditTextArea textArea;

        public JEditTextAreaUndoableAdapter(JEditTextArea scriptArea) {
            this.textArea = scriptArea;
        }

        public int getCaretPosition() {
            if (textArea == null) {
                return 0;
            }

            return textArea.getCaretPosition();
        }

        public Document getDocument() {
            if (textArea == null) {
                return null;
            }

            return textArea.getDocument();
        }

        public boolean requestFocusInWindow() {
            if (textArea == null) {
                return false;
            }

            return textArea.requestFocusInWindow();
        }

        public void selectAll() {
            if (textArea == null) {
                return;
            }

            textArea.selectAll();
        }

        @Override
        public void setText(String t) {
            if (textArea == null) {
                return;
            }

            textArea.setText(t);
        }

        public void setCaretPosition(int position) {
            if (textArea == null) {
                return;
            }

            textArea.setCaretPosition(position);
        }

        @Override
        public void updateUI() {

        }
    }
}
