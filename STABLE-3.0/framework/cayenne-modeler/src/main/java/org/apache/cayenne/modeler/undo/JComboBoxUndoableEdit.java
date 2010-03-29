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

import java.awt.event.ItemListener;

import javax.swing.JComboBox;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

public class JComboBoxUndoableEdit extends AbstractUndoableEdit {

    
    
    private JComboBox comboBox;
    private Object deselectedItem;
    private Object selectedItem;
    private ItemListener undoItemListener;
    
    public JComboBoxUndoableEdit(JComboBox comboBox, Object deselectedItem,
            Object selectedItem, ItemListener undoItemListener) {
        super();
        this.comboBox = comboBox;
        this.deselectedItem = deselectedItem;
        this.selectedItem = selectedItem;
        this.undoItemListener = undoItemListener;
    }
    
    @Override
    public String getPresentationName() {
        return "Selection";
    }

    @Override
    public boolean canRedo() {
        return true;
    }

    @Override
    public void redo() throws CannotRedoException {
        comboBox.removeItemListener(undoItemListener);

        try {
            comboBox.setSelectedItem(selectedItem);
        }
        finally {
           comboBox.addItemListener(undoItemListener);
        }
    }

    @Override
    public void undo() throws CannotUndoException {
        comboBox.removeItemListener(undoItemListener);
        try {
            comboBox.setSelectedItem(deselectedItem);
        }
        finally {
            comboBox.addItemListener(undoItemListener);
        }
    }
}
