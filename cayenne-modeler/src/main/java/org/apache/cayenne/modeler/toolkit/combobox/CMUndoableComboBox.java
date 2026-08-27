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

package org.apache.cayenne.modeler.toolkit.combobox;

import org.apache.cayenne.modeler.undo.CayenneUndoManager;

import javax.swing.JComboBox;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * A CMComboBox that records selection changes on the modeler's undo stack.
 */
public class CMUndoableComboBox<T> extends CMComboBox<T> {

    public CMUndoableComboBox(CayenneUndoManager undoManager) {
        addItemListener(new UndoableSelectionListener(undoManager));
    }

    // Tracks selection changes on a JComboBox and pushes them to the undo stack.
    private static class UndoableSelectionListener implements ItemListener {
        private final CayenneUndoManager undoManager;
        private Object deselectedItem;

        UndoableSelectionListener(CayenneUndoManager undoManager) {
            this.undoManager = undoManager;
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            switch (e.getStateChange()) {
                case ItemEvent.DESELECTED:
                    deselectedItem = e.getItem();
                    break;
                case ItemEvent.SELECTED:
                    undoManager.addEdit(new CMComboBoxUndoableEdit(
                            (JComboBox<?>) e.getSource(), deselectedItem, e.getItem(), this));
                    break;
            }
        }
    }
}
